package dev.candycup.lifestealutils.config;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigOptionDescriptor<T> {
   private final Kind kind;
   private final String category;
   private final String group;
   private final String name;
   private final Supplier<T> defaultSupplier;
   private final Supplier<T> valueSupplier;
   private final Consumer<T> valueConsumer;
   private final Class<T> enumClass;
   private final Optional<String> hardName;
   private final Optional<String> hardDescription;

   private ConfigOptionDescriptor(Kind kind, String category, String group, String name, Supplier<T> defaultSupplier, Supplier<T> valueSupplier, Consumer<T> valueConsumer, Class<T> enumClass, Optional<String> hardName, Optional<String> hardDescription) {
      this.kind = kind;
      this.category = category;
      this.group = group;
      this.name = name;
      this.defaultSupplier = defaultSupplier;
      this.valueSupplier = valueSupplier;
      this.valueConsumer = valueConsumer;
      this.enumClass = enumClass;
      this.hardName = hardName;
      this.hardDescription = hardDescription;
   }

   public static ConfigOptionDescriptor<Boolean> bool(String category, String group, String name, Supplier<Boolean> defaultSupplier, Supplier<Boolean> valueSupplier, Consumer<Boolean> valueConsumer) {
      return new ConfigOptionDescriptor<>(Kind.BOOLEAN, category, group, name, defaultSupplier, valueSupplier, valueConsumer, null, Optional.empty(), Optional.empty());
   }

   public static ConfigOptionDescriptor<String> string(String category, String group, String name, Supplier<String> defaultSupplier, Supplier<String> valueSupplier, Consumer<String> valueConsumer) {
      return new ConfigOptionDescriptor<>(Kind.STRING, category, group, name, defaultSupplier, valueSupplier, valueConsumer, null, Optional.empty(), Optional.empty());
   }

   public static ConfigOptionDescriptor<String> minimessage(String category, String group, String name, Supplier<String> defaultSupplier, Supplier<String> valueSupplier, Consumer<String> valueConsumer) {
      return new ConfigOptionDescriptor<>(Kind.MINIMESSAGE, category, group, name, defaultSupplier, valueSupplier, valueConsumer, null, Optional.empty(), Optional.empty());
   }

   public static ConfigOptionDescriptor<Float> floating(String category, String group, String name, Supplier<Float> defaultSupplier, Supplier<Float> valueSupplier, Consumer<Float> valueConsumer) {
      return new ConfigOptionDescriptor<>(Kind.FLOAT, category, group, name, defaultSupplier, valueSupplier, valueConsumer, null, Optional.empty(), Optional.empty());
   }

   public static <T extends Enum<T>> ConfigOptionDescriptor<T> enumeration(String category, String group, String name, Class<T> enumClass, Supplier<T> defaultSupplier, Supplier<T> valueSupplier, Consumer<T> valueConsumer) {
      return new ConfigOptionDescriptor<>(Kind.ENUM, category, group, name, defaultSupplier, valueSupplier, valueConsumer, enumClass, Optional.empty(), Optional.empty());
   }

   public static ConfigOptionDescriptor<List<String>> stringList(String category, String group, Supplier<List<String>> defaultSupplier, Supplier<List<String>> valueSupplier, Consumer<List<String>> valueConsumer) {
      return new ConfigOptionDescriptor<>(Kind.LIST, category, group, group, defaultSupplier, valueSupplier, valueConsumer, null, Optional.empty(), Optional.empty());
   }

   public ConfigOptionDescriptor<T> hardTranslation(String name, String description) {
      return new ConfigOptionDescriptor<>(
              kind,
              category,
              group,
              this.name,
              defaultSupplier,
              valueSupplier,
              valueConsumer,
              enumClass,
              Optional.ofNullable(name),
              Optional.ofNullable(description)
      );
   }

   public ConfigOptionDescriptor<T> hardName(String name) {
      return new ConfigOptionDescriptor<>(
              kind,
              category,
              group,
              this.name,
              defaultSupplier,
              valueSupplier,
              valueConsumer,
              enumClass,
              Optional.ofNullable(name),
              hardDescription
      );
   }

   public ConfigOptionDescriptor<T> hardDescription(String description) {
      return new ConfigOptionDescriptor<>(
              kind,
              category,
              group,
              this.name,
              defaultSupplier,
              valueSupplier,
              valueConsumer,
              enumClass,
              hardName,
              Optional.ofNullable(description)
      );
   }

   public Kind kind() {
      return kind;
   }

   public String category() {
      return category;
   }

   public String group() {
      return group;
   }

   public String name() {
      return name;
   }

   public Supplier<T> defaultSupplier() {
      return defaultSupplier;
   }

   public Supplier<T> valueSupplier() {
      return valueSupplier;
   }

   public Consumer<T> valueConsumer() {
      return valueConsumer;
   }

   public Class<T> enumClass() {
      return enumClass;
   }

   public Optional<String> hardName() {
      return hardName;
   }

   public Optional<String> hardDescription() {
      return hardDescription;
   }

   public enum Kind {
      BOOLEAN,
      STRING,
      MINIMESSAGE,
      FLOAT,
      ENUM,
      LIST
   }
}
