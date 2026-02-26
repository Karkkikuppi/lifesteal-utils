package dev.candycup.lifestealutils;

import dev.candycup.lifestealutils.config.ConfigContainerRegistry;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.ConfigSerializer;
import dev.isxander.yacl3.config.v2.api.FieldAccess;
import dev.isxander.yacl3.config.v2.api.ReadOnlyFieldAccess;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.SerialField;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGenField;
import dev.isxander.yacl3.config.v2.impl.autogen.YACLAutoGenException;
import dev.isxander.yacl3.impl.utils.YACLConstants;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This is largely based on YACL's own config class implementation, but also dynamically discovers fields from
 * multiple classes, for the purposes of LSU's multi-container config system.
 *
 * @param <T>
 */
public class LifestealUtilsConfigClassHandler<T> implements ConfigClassHandler<T> {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils");
   private static final Map<String, DynamicSerialDefinition<?>> DYNAMIC_SERIAL_DEFINITIONS = new LinkedHashMap<>();
   private static final Set<LifestealUtilsConfigClassHandler<?>> ACTIVE_HANDLERS = new LinkedHashSet<>();

   private final Class<T> configClass;
   private final Identifier id;
   private ConfigField<?>[] fields;
   private final ConfigSerializer<T> serializer;
   private final Constructor<T> noArgsConstructor;
   private T configInstance;
   private final T defaults;
   private boolean fieldsDirty;

   private LifestealUtilsConfigClassHandler(Class<T> configClass, Identifier id, Function<ConfigClassHandler<T>, ConfigSerializer<T>> serializerFactory) {
      this.configClass = configClass;
      this.id = id;

      try {
         noArgsConstructor = configClass.getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
         throw new YACLAutoGenException("Failed to find no-args constructor for config class %s.".formatted(configClass.getName()), e);
      }

      this.configInstance = createNewObject();
      this.defaults = createNewObject();
      this.fieldsDirty = true;
      this.fields = discoverFields();
      this.fieldsDirty = false;
      this.serializer = serializerFactory.apply(this);

      synchronized (LifestealUtilsConfigClassHandler.class) {
         ACTIVE_HANDLERS.add(this);
      }
   }

   public static <T> Builder<T> createBuilder(Class<T> configClass) {
      return new BuilderImpl<>(configClass);
   }

   public static synchronized <T> void registerDynamicSerial(DynamicSerialDefinition<T> definition) {
      DynamicSerialDefinition<?> existing = DYNAMIC_SERIAL_DEFINITIONS.putIfAbsent(definition.serialName(), definition);
      if (existing != null) {
         LOGGER.warn("dynamic serial key '{}' is already registered. keeping original registration", definition.serialName());
         return;
      }

      for (LifestealUtilsConfigClassHandler<?> handler : ACTIVE_HANDLERS) {
         handler.fieldsDirty = true;
      }
   }

   public static synchronized void clearDynamicSerials() {
      DYNAMIC_SERIAL_DEFINITIONS.clear();
      for (LifestealUtilsConfigClassHandler<?> handler : ACTIVE_HANDLERS) {
         handler.fieldsDirty = true;
      }
   }

   private static synchronized List<DynamicSerialDefinition<?>> getDynamicSerialDefinitions() {
      return new ArrayList<>(DYNAMIC_SERIAL_DEFINITIONS.values());
   }

   private ConfigField<?>[] discoverFields() {
      ConfigContainerRegistry.initializeGeneratedIndex();
      List<Class<?>> containers = new ArrayList<>(ConfigContainerRegistry.getRegisteredContainers());
      if (!containers.contains(configClass)) {
         containers.add(0, configClass);
      }

      Map<String, String> claimedSerialNames = new LinkedHashMap<>();
      List<ReloadableConfigField<?>> discovered = new ArrayList<>();

      for (Class<?> containerClass : new LinkedHashSet<>(containers)) {
         SerialEntry classSerialEntry = getSerialEntry(containerClass);

         for (Field field : containerClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (!Modifier.isStatic(field.getModifiers())) {
               continue;
            }

            SerialEntry fieldSerialEntry = field.getAnnotation(SerialEntry.class);
            if (classSerialEntry == null && fieldSerialEntry == null) {
               continue;
            }

            SerialField serialField = createSerialField(field, fieldSerialEntry, classSerialEntry);
            if (serialField == null) {
               continue;
            }

            String serialName = serialField.serialName();
            String owner = containerClass.getName() + "#" + field.getName();
            String previousOwner = claimedSerialNames.putIfAbsent(serialName, owner);
            if (previousOwner != null) {
               LOGGER.warn("duplicate serial key '{}' detected on {}. keeping first declaration on {}", serialName, owner, previousOwner);
               continue;
            }

            discovered.add(new MultiClassConfigField<>(
                    new ReflectiveFieldAccess<>(field, configInstance),
                    new ReflectiveFieldAccess<>(field, defaults),
                    this,
                    Optional.of(serialField)
            ));
         }
      }

      for (DynamicSerialDefinition<?> definition : getDynamicSerialDefinitions()) {
         String previousOwner = claimedSerialNames.putIfAbsent(definition.serialName(), "dynamic:" + definition.serialName());
         if (previousOwner != null) {
            LOGGER.warn("duplicate serial key '{}' detected on dynamic definition. keeping first declaration on {}", definition.serialName(), previousOwner);
            continue;
         }

         discovered.add(definition.toConfigField(this));
      }

      return discovered.toArray(ConfigField[]::new);
   }

   private static @Nullable SerialEntry getSerialEntry(Class<?> containerClass) {
      SerialEntry classSerialEntry = containerClass.getAnnotation(SerialEntry.class);
      if (classSerialEntry != null) {
         if (!"".equals(classSerialEntry.value())) {
            throw new IllegalArgumentException("SerialEntry on class '%s' must not have a value. Only `required` and `nullable` are permitted parameters on classes.".formatted(containerClass.getName()));
         }
         if (!"".equals(classSerialEntry.comment())) {
            throw new IllegalArgumentException("SerialEntry on class '%s' must not have a comment. Only `required` and `nullable` are permitted parameters on classes.".formatted(containerClass.getName()));
         }
      }
      return classSerialEntry;
   }

   private SerialField createSerialField(Field field, SerialEntry fieldSerialEntry, SerialEntry inheritedClassSerialEntry) {
      if (fieldSerialEntry != null) {
         String serialName = "".equals(fieldSerialEntry.value()) ? field.getName() : fieldSerialEntry.value();
         Optional<String> comment = "".equals(fieldSerialEntry.comment())
                 ? Optional.empty()
                 : Optional.of(fieldSerialEntry.comment());
         return new SerialFieldImpl(serialName, comment, fieldSerialEntry.required(), fieldSerialEntry.nullable());
      }

      if (inheritedClassSerialEntry != null) {
         return new SerialFieldImpl(field.getName(), Optional.empty(), inheritedClassSerialEntry.required(), inheritedClassSerialEntry.nullable());
      }

      return null;
   }

   @Override
   public T instance() {
      return configInstance;
   }

   @Override
   public T defaults() {
      return defaults;
   }

   @Override
   public Class<T> configClass() {
      return configClass;
   }

   @Override
   public ConfigField<?>[] fields() {
      ensureFieldsUpToDate();
      return fields;
   }

   @Override
   public boolean load() {
      ensureFieldsUpToDate();
      T newInstance = createNewObject();

      Map<ReloadableConfigField<?>, FieldAccess<?>> internalAccessBuffer = Arrays.stream(fields)
              .map(field -> (ReloadableConfigField<?>) field)
              .map(field -> new AbstractMap.SimpleImmutableEntry<>(field, field.createBufferAccess(newInstance)))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      Map<ConfigField<?>, FieldAccess<?>> accessBuffer = internalAccessBuffer.entrySet().stream()
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      ConfigSerializer.LoadResult loadResult = ConfigSerializer.LoadResult.FAILURE;
      Throwable error = null;
      try {
         loadResult = serializer.loadSafely(accessBuffer);
      } catch (Throwable throwable) {
         error = throwable;
      }

      switch (loadResult) {
         case DIRTY:
         case SUCCESS:
            this.configInstance = newInstance;
            internalAccessBuffer.forEach((field, buffer) -> field.commitBufferAccess(buffer));

            if (loadResult == ConfigSerializer.LoadResult.DIRTY) {
               save();
            }
            return true;
         case NO_CHANGE:
            return true;
         case FAILURE:
            YACLConstants.LOGGER.error(
                    "Unsuccessful load of config class '{}'. The load will be abandoned and config remains unchanged.",
                    configClass.getSimpleName(), error
            );
            return false;
      }

      return false;
   }

   @Override
   public void save() {
      serializer.save();
   }

   @Override
   public ConfigSerializer<T> serializer() {
      return serializer;
   }

   @Override
   public Identifier id() {
      return id;
   }

   @Override
   public YetAnotherConfigLib generateGui() {
      throw new UnsupportedOperationException("LifestealUtilsConfigClassHandler does not support generateGui()");
   }

   @Override
   public boolean supportsAutoGen() {
      return false;
   }

   private T createNewObject() {
      try {
         return noArgsConstructor.newInstance();
      } catch (Exception e) {
         throw new YACLAutoGenException("Failed to create instance of config class '%s' with no-args constructor.".formatted(configClass.getName()), e);
      }
   }

   private void ensureFieldsUpToDate() {
      if (!fieldsDirty) {
         return;
      }

      this.fields = discoverFields();
      this.fieldsDirty = false;
   }

   private static final class BuilderImpl<T> implements Builder<T> {
      private final Class<T> configClass;
      private Identifier id;
      private Function<ConfigClassHandler<T>, ConfigSerializer<T>> serializerFactory;

      private BuilderImpl(Class<T> configClass) {
         this.configClass = configClass;
      }

      @Override
      public Builder<T> id(Identifier id) {
         this.id = id;
         return this;
      }

      @Override
      public Builder<T> serializer(Function<ConfigClassHandler<T>, ConfigSerializer<T>> serializerFactory) {
         this.serializerFactory = serializerFactory;
         return this;
      }

      @Override
      public ConfigClassHandler<T> build() {
         Validate.notNull(serializerFactory, "serializerFactory must not be null");
         Validate.notNull(configClass, "configClass must not be null");
         return new LifestealUtilsConfigClassHandler<>(configClass, id, serializerFactory);
      }
   }

   private record SerialFieldImpl(String serialName, Optional<String> comment, boolean required,
                                  boolean nullable) implements SerialField {
   }

   private static final class MultiClassConfigField<T> implements ReloadableConfigField<T> {
      private ReflectiveFieldAccess<T> access;
      private final ReflectiveFieldAccess<T> defaultAccess;
      private final ConfigClassHandler<?> parent;
      private final Optional<SerialField> serial;

      private MultiClassConfigField(ReflectiveFieldAccess<T> access, ReflectiveFieldAccess<T> defaultAccess, ConfigClassHandler<?> parent, Optional<SerialField> serial) {
         this.access = access;
         this.defaultAccess = defaultAccess;
         this.parent = parent;
         this.serial = serial;
      }

      @Override
      public FieldAccess<T> access() {
         return access;
      }

      private void setFieldAccess(ReflectiveFieldAccess<?> newAccess) {
         this.access = (ReflectiveFieldAccess<T>) newAccess;
      }

      private ReflectiveFieldAccess<T> createAccessFor(Object object) {
         return new ReflectiveFieldAccess<>(access.field, object);
      }

      @Override
      public FieldAccess<T> createBufferAccess(Object object) {
         return createAccessFor(object);
      }

      @Override
      public void commitBufferAccess(FieldAccess<?> bufferAccess) {
         setFieldAccess((ReflectiveFieldAccess<?>) bufferAccess);
      }

      @Override
      public ReadOnlyFieldAccess<T> defaultAccess() {
         return defaultAccess;
      }

      @Override
      public ConfigClassHandler<?> parent() {
         return parent;
      }

      @Override
      public Optional<SerialField> serial() {
         return serial;
      }

      @Override
      public Optional<dev.isxander.yacl3.config.v2.api.autogen.AutoGenField> autoGen() {
         return Optional.empty();
      }
   }

   private interface ReloadableConfigField<T> extends ConfigField<T> {
      FieldAccess<T> createBufferAccess(Object object);

      void commitBufferAccess(FieldAccess<?> bufferAccess);
   }

   public static final class DynamicSerialDefinition<T> {
      private final String serialName;
      private final Optional<String> comment;
      private final boolean required;
      private final boolean nullable;
      private final Type type;
      private final Class<T> typeClass;
      private final Supplier<T> valueGetter;
      private final Consumer<T> valueSetter;
      private final Supplier<T> defaultValueSupplier;

      public DynamicSerialDefinition(String serialName, Optional<String> comment, boolean required, boolean nullable, Type type, Class<T> typeClass, Supplier<T> valueGetter, Consumer<T> valueSetter, Supplier<T> defaultValueSupplier) {
         this.serialName = serialName;
         this.comment = comment;
         this.required = required;
         this.nullable = nullable;
         this.type = type;
         this.typeClass = typeClass;
         this.valueGetter = valueGetter;
         this.valueSetter = valueSetter;
         this.defaultValueSupplier = defaultValueSupplier;
      }

      public static <T> DynamicSerialDefinition<T> create(String serialName, Optional<String> comment, boolean required, boolean nullable, Type type, Class<T> typeClass, Supplier<T> valueGetter, Consumer<T> valueSetter, Supplier<T> defaultValueSupplier) {
         return new DynamicSerialDefinition<>(serialName, comment, required, nullable, type, typeClass, valueGetter, valueSetter, defaultValueSupplier);
      }

      private String serialName() {
         return serialName;
      }

      private DynamicConfigField<T> toConfigField(ConfigClassHandler<?> parent) {
         SerialField serialField = new SerialFieldImpl(serialName, comment, required, nullable);
         return new DynamicConfigField<>(
                 new DynamicValueFieldAccess<>(serialName, type, typeClass, valueGetter, valueSetter),
                 new DynamicReadOnlyFieldAccess<>(serialName, type, typeClass, defaultValueSupplier),
                 parent,
                 Optional.of(serialField),
                 defaultValueSupplier
         );
      }
   }

   private record DynamicConfigField<T>(DynamicValueFieldAccess<T> liveAccess, ReadOnlyFieldAccess<T> defaultAccess,
                                        ConfigClassHandler<?> parent, Optional<SerialField> serial,
                                        Supplier<T> defaultValueSupplier) implements ReloadableConfigField<T> {

      @Override
      public FieldAccess<T> access() {
         return liveAccess;
      }

      @Override
      public Optional<AutoGenField> autoGen() {
         return Optional.empty();
      }

      @Override
      public FieldAccess<T> createBufferAccess(Object object) {
         return new BufferedFieldAccess<>(
                 liveAccess.name(),
                 liveAccess.type(),
                 liveAccess.typeClass(),
                 defaultValueSupplier.get()
         );
      }

      @Override
      public void commitBufferAccess(FieldAccess<?> bufferAccess) {
         liveAccess.set(((FieldAccess<T>) bufferAccess).get());
      }
   }

   private record DynamicReadOnlyFieldAccess<T>(String name, Type type, Class<T> typeClass,
                                                Supplier<T> getter) implements ReadOnlyFieldAccess<T> {

      @Override
      public T get() {
         return getter.get();
      }

      @Override
      public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
         return Optional.empty();
      }
   }

   private record DynamicValueFieldAccess<T>(String name, Type type, Class<T> typeClass, Supplier<T> getter,
                                             Consumer<T> setter) implements FieldAccess<T> {
      @Override
      public T get() {
         return getter.get();
      }

      @Override
      public void set(T value) {
         setter.accept(value);
      }

      @Override
      public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
         return Optional.empty();
      }
   }

   private static final class BufferedFieldAccess<T> implements FieldAccess<T> {
      private final String name;
      private final Type type;
      private final Class<T> typeClass;
      private T value;

      private BufferedFieldAccess(String name, Type type, Class<T> typeClass, T value) {
         this.name = name;
         this.type = type;
         this.typeClass = typeClass;
         this.value = value;
      }

      @Override
      public T get() {
         return value;
      }

      @Override
      public void set(T value) {
         this.value = value;
      }

      @Override
      public String name() {
         return name;
      }

      @Override
      public Type type() {
         return type;
      }

      @Override
      public Class<T> typeClass() {
         return typeClass;
      }

      @Override
      public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
         return Optional.empty();
      }
   }

   private record ReflectiveFieldAccess<T>(Field field, Object instance) implements FieldAccess<T> {

      @Override
      public T get() {
         try {
            return (T) field.get(instance);
         } catch (IllegalAccessException e) {
            throw new YACLAutoGenException("Failed to access field '%s'".formatted(name()), e);
         }
      }

      @Override
      public void set(T value) {
         try {
            field.set(instance, value);
         } catch (IllegalAccessException e) {
            throw new YACLAutoGenException("Failed to set field '%s'".formatted(name()), e);
         }
      }

      @Override
      public String name() {
         return field.getName();
      }

      @Override
      public Type type() {
         return field.getGenericType();
      }

      @Override
      public Class<T> typeClass() {
         return (Class<T>) field.getType();
      }

      @Override
      public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
         return Optional.ofNullable(field.getAnnotation(annotationClass));
      }
   }
}
