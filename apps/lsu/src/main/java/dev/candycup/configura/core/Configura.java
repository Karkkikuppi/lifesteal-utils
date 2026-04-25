package dev.candycup.configura.core;

import dev.candycup.configura.serial.SerialEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Configura<T> {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/configura");
   private static final String VERSION_KEY = "_configVersion";
   private static final Map<String, ConfiguraDynamicEntryDefinition<?>> DYNAMIC_ENTRY_DEFINITIONS = new LinkedHashMap<>();
   private static final Set<Configura<?>> ACTIVE_CONFIGS = new LinkedHashSet<>();

   private final Class<T> rootClass;
   private final List<Class<?>> containers;
   private final Path path;
   private final ConfiguraCodec codec;
   private final List<ConfiguraMigration> migrations;
   private final int targetVersion;
   private final T defaultsInstance;
   private ConfiguraEntry<?>[] entries;
   private boolean entriesDirty;

   private Configura(Builder<T> builder) {
      this.rootClass = builder.rootClass;
      this.containers = new ArrayList<>(builder.containers);
      this.path = builder.path;
      this.codec = builder.codec;
      this.migrations = List.copyOf(builder.migrations);
      this.targetVersion = this.migrations.size();
      this.defaultsInstance = instantiateNoArgs(rootClass);
      this.entries = discoverEntries();
      this.entriesDirty = false;

      synchronized (Configura.class) {
         ACTIVE_CONFIGS.add(this);
      }
   }

   public static <T> Builder<T> builder(Class<T> rootClass) {
      return new Builder<>(rootClass);
   }

   public static synchronized <T> void registerDynamicEntry(ConfiguraDynamicEntryDefinition<T> definition) {
      ConfiguraDynamicEntryDefinition<?> existing = DYNAMIC_ENTRY_DEFINITIONS.putIfAbsent(definition.key(), definition);
      if (existing != null) {
         LOGGER.warn("dynamic entry '{}' already registered, keeping original", definition.key());
         return;
      }

      for (Configura<?> configura : ACTIVE_CONFIGS) {
         configura.entriesDirty = true;
      }
   }

   public static synchronized void clearDynamicEntries() {
      DYNAMIC_ENTRY_DEFINITIONS.clear();
      for (Configura<?> configura : ACTIVE_CONFIGS) {
         configura.entriesDirty = true;
      }
   }

   private static synchronized List<ConfiguraDynamicEntryDefinition<?>> dynamicEntriesSnapshot() {
      return new ArrayList<>(DYNAMIC_ENTRY_DEFINITIONS.values());
   }

   public synchronized boolean load() {
      ensureEntriesUpToDate();

      if (!Files.exists(path)) {
         save();
         return true;
      }

      String raw;
      try {
         raw = Files.readString(path, StandardCharsets.UTF_8);
      } catch (IOException exception) {
         LOGGER.error("failed reading config '{}': {}", path, exception.getMessage());
         return false;
      }

      Map<String, Object> decoded;
      try {
         decoded = codec.decode(raw);
      } catch (Exception exception) {
         LOGGER.error("failed parsing config '{}': {}", path, exception.getMessage());
         return false;
      }

      boolean dirty = false;

      if (targetVersion > 0) {
         int fileVersion = 0;
         Object rawVersion = decoded.remove(VERSION_KEY);
         if (rawVersion instanceof Number n) {
            fileVersion = n.intValue();
         }

         if (fileVersion < targetVersion) {
            for (int v = fileVersion + 1; v <= targetVersion; v++) {
               LOGGER.info("migrating config '{}' from version {} to {}", path.getFileName(), v - 1, v);
               migrations.get(v - 1).migrate(decoded);
            }
            dirty = true;
         }
      }

      for (ConfiguraEntry<?> entry : entries) {
         Object rawValue = decoded.get(entry.key());
         if (rawValue == null) {
            writeEntryValue(entry, entry.defaultSupplier().get());
            dirty = true;
            continue;
         }

         Object coerced = coerceValue(entry, rawValue);
         if (coerced == null) {
            writeEntryValue(entry, entry.defaultSupplier().get());
            dirty = true;
            continue;
         }

         writeEntryValue(entry, coerced);
      }

      if (dirty) {
         save();
      }
      return true;
   }

   public synchronized void save() {
      ensureEntriesUpToDate();

      Map<String, Object> values = new LinkedHashMap<>();
      if (targetVersion > 0) {
         values.put(VERSION_KEY, targetVersion);
      }
      for (ConfiguraEntry<?> entry : entries) {
         Object entryValue = readEntryValue(entry);
         if (entryValue instanceof ToggleGroup tg) {
            entryValue = tg.toRawMap();
         }
         values.put(entry.key(), deepCopy(entryValue));
      }

      String encoded;
      try {
         encoded = codec.encode(values);
      } catch (Exception exception) {
         LOGGER.error("failed encoding config '{}': {}", path, exception.getMessage());
         return;
      }

      try {
         Path parent = path.getParent();
         if (parent != null) {
            Files.createDirectories(parent);
         }
         Files.writeString(path, encoded, StandardCharsets.UTF_8);
      } catch (IOException exception) {
         LOGGER.error("failed writing config '{}': {}", path, exception.getMessage());
      }
   }

   public synchronized void resetToDefaults() {
      ensureEntriesUpToDate();
      for (ConfiguraEntry<?> entry : entries) {
         writeEntryValue(entry, entry.defaultSupplier().get());
      }
      save();
   }

   public synchronized List<ConfiguraEntry<?>> entries() {
      ensureEntriesUpToDate();
      return Arrays.asList(entries);
   }

   public synchronized Map<String, ConfiguraEntry<?>> entriesByKey() {
      ensureEntriesUpToDate();
      return Arrays.stream(entries)
              .collect(Collectors.toMap(ConfiguraEntry::key, Function.identity(), (first, second) -> first, LinkedHashMap::new));
   }

   private void ensureEntriesUpToDate() {
      if (!entriesDirty) {
         return;
      }
      this.entries = discoverEntries();
      this.entriesDirty = false;
   }

   private ConfiguraEntry<?>[] discoverEntries() {
      List<Class<?>> orderedContainers = new ArrayList<>();
      orderedContainers.add(rootClass);
      for (Class<?> container : containers) {
         if (!orderedContainers.contains(container)) {
            orderedContainers.add(container);
         }
      }

      Map<String, String> claimedKeys = new LinkedHashMap<>();
      List<ConfiguraEntry<?>> discovered = new ArrayList<>();

      for (Class<?> container : new LinkedHashSet<>(orderedContainers)) {
         SerialEntry classEntry = validateClassSerialEntry(container);
         for (Field field : container.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
               continue;
            }
            field.setAccessible(true);

            SerialEntry fieldEntry = field.getAnnotation(SerialEntry.class);
            if (classEntry == null && fieldEntry == null) {
               continue;
            }

            String key = resolveKey(field, fieldEntry);
            String owner = container.getName() + "#" + field.getName();
            String previous = claimedKeys.putIfAbsent(key, owner);
            if (previous != null) {
               LOGGER.warn("duplicate serial key '{}' on {}, keeping first from {}", key, owner, previous);
               continue;
            }

            Optional<String> comment = resolveComment(fieldEntry);
            boolean required = fieldEntry != null ? fieldEntry.required() : classEntry.required();
            boolean nullable = fieldEntry != null ? fieldEntry.nullable() : classEntry.nullable();

            Object defaultValue = readStaticField(field, defaultsInstance);
            discovered.add(new ConfiguraEntry<>(
                    key,
                    field.getGenericType(),
                    (Class<Object>) field.getType(),
                    () -> readStaticField(field, null),
                    value -> writeStaticField(field, null, value),
                    () -> deepCopy(defaultValue),
                    required,
                    nullable,
                    comment
            ));
         }
      }

      for (ConfiguraDynamicEntryDefinition<?> definition : dynamicEntriesSnapshot()) {
         String previous = claimedKeys.putIfAbsent(definition.key(), "dynamic:" + definition.key());
         if (previous != null) {
            LOGGER.warn("duplicate serial key '{}' on dynamic definition, keeping first from {}", definition.key(), previous);
            continue;
         }
         discovered.add(definition.toEntry());
      }

      return discovered.toArray(ConfiguraEntry[]::new);
   }

   private SerialEntry validateClassSerialEntry(Class<?> container) {
      SerialEntry classEntry = container.getAnnotation(SerialEntry.class);
      if (classEntry == null) {
         return null;
      }
      if (!classEntry.value().isBlank()) {
         throw new IllegalArgumentException("SerialEntry on class '%s' must not set value".formatted(container.getName()));
      }
      if (!classEntry.comment().isBlank()) {
         throw new IllegalArgumentException("SerialEntry on class '%s' must not set comment".formatted(container.getName()));
      }
      return classEntry;
   }

   private static String resolveKey(Field field, SerialEntry fieldEntry) {
      if (fieldEntry == null || fieldEntry.value().isBlank()) {
         return field.getName();
      }
      return fieldEntry.value();
   }

   private static Optional<String> resolveComment(SerialEntry fieldEntry) {
      if (fieldEntry == null || fieldEntry.comment().isBlank()) {
         return Optional.empty();
      }
      return Optional.of(fieldEntry.comment());
   }

   private static Object readStaticField(Field field, Object instance) {
      try {
         return field.get(instance);
      } catch (IllegalAccessException exception) {
         throw new IllegalStateException("failed to read field '%s'".formatted(field.getName()), exception);
      }
   }

   private static void writeStaticField(Field field, Object instance, Object value) {
      try {
         field.set(instance, value);
      } catch (IllegalAccessException exception) {
         throw new IllegalStateException("failed to write field '%s'".formatted(field.getName()), exception);
      }
   }

   private static Object coerceValue(ConfiguraEntry<?> entry, Object rawValue) {
      Class<?> typeClass = entry.typeClass();
      if (typeClass == Boolean.class || typeClass == boolean.class) {
         return rawValue instanceof Boolean b ? b : null;
      }
      if (typeClass == String.class) {
         return rawValue instanceof String s ? s : null;
      }
      if (typeClass == Float.class || typeClass == float.class) {
         if (rawValue instanceof Number number) {
            return number.floatValue();
         }
         if (rawValue instanceof String value) {
            try {
               return Float.parseFloat(value);
            } catch (NumberFormatException ignored) {
               return null;
            }
         }
         return null;
      }
      if (typeClass.isEnum()) {
         if (!(rawValue instanceof String enumName)) {
            return null;
         }
         Object[] constants = typeClass.getEnumConstants();
         if (constants == null) {
            return null;
         }
         for (Object constant : constants) {
            Enum<?> enumValue = (Enum<?>) constant;
            if (enumValue.name().equalsIgnoreCase(enumName)) {
               return enumValue;
            }
         }
         return null;
      }
      if (List.class.isAssignableFrom(typeClass)) {
         if (!(rawValue instanceof List<?> list)) {
            return null;
         }
         List<String> values = new ArrayList<>();
         for (Object item : list) {
            if (!(item instanceof String text)) {
               return null;
            }
            values.add(text);
         }
         return values;
      }
      if (typeClass == ToggleGroup.class) {
         if (!(rawValue instanceof Map<?, ?> rawMap)) {
            return null;
         }
         Object defaultValue = entry.defaultSupplier().get();
         if (!(defaultValue instanceof ToggleGroup defaultGroup)) {
            return null;
         }
         return defaultGroup.withValues(rawMap);
      }
      if (Map.class.isAssignableFrom(typeClass)) {
         if (!(rawValue instanceof Map<?, ?> map)) {
            return null;
         }
         Map<Object, Object> copy = new LinkedHashMap<>();
         copy.putAll(map);
         return copy;
      }

      return rawValue;
   }

   private static Object deepCopy(Object value) {
      if (value == null) {
         return null;
      }
      if (value instanceof List<?> list) {
         return new ArrayList<>(list);
      }
      if (value instanceof ToggleGroup tg) {
         return tg.copy();
      }
      if (value instanceof Map<?, ?> map) {
         return new LinkedHashMap<>(map);
      }
      return value;
   }

   private static <T> T instantiateNoArgs(Class<T> type) {
      try {
         var constructor = type.getDeclaredConstructor();
         constructor.setAccessible(true);
         return constructor.newInstance();
      } catch (Exception exception) {
         throw new IllegalStateException("failed to instantiate config class '%s'".formatted(type.getName()), exception);
      }
   }

   private static <T> void writeEntryValue(ConfiguraEntry<T> entry, Object value) {
      entry.setter().accept((T) deepCopy(value));
   }

   private static <T> Object readEntryValue(ConfiguraEntry<T> entry) {
      return entry.getter().get();
   }

   public static final class Builder<T> {
      private final Class<T> rootClass;
      private final List<Class<?>> containers = new ArrayList<>();
      private final List<ConfiguraMigration> migrations = new ArrayList<>();
      private Path path;
      private ConfiguraCodec codec;

      private Builder(Class<T> rootClass) {
         this.rootClass = Objects.requireNonNull(rootClass, "rootClass");
      }

      public Builder<T> container(Class<?> containerClass) {
         this.containers.add(Objects.requireNonNull(containerClass, "containerClass"));
         return this;
      }

      public Builder<T> containers(List<Class<?>> containerClasses) {
         this.containers.clear();
         this.containers.addAll(containerClasses);
         return this;
      }

      public Builder<T> path(Path path) {
         this.path = path;
         return this;
      }

      public Builder<T> codec(ConfiguraCodec codec) {
         this.codec = codec;
         return this;
      }

      public Builder<T> migration(int toVersion, ConfiguraMigration migration) {
         int expected = this.migrations.size() + 1;
         if (toVersion != expected) {
            throw new IllegalArgumentException("migration versions must be consecutive starting from 1, expected %d but got %d".formatted(expected, toVersion));
         }
         this.migrations.add(migration);
         return this;
      }

      public Configura<T> build() {
         Objects.requireNonNull(path, "path");
         Objects.requireNonNull(codec, "codec");
         return new Configura<>(this);
      }
   }
}
