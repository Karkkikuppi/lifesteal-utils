package dev.candycup.configura.core;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfiguraDynamicEntryDefinition<T> {
   private final String key;
   private final Optional<String> comment;
   private final boolean required;
   private final boolean nullable;
   private final Type type;
   private final Class<T> typeClass;
   private final Supplier<T> valueGetter;
   private final Consumer<T> valueSetter;
   private final Supplier<T> defaultValueSupplier;

   private ConfiguraDynamicEntryDefinition(
           String key,
           Optional<String> comment,
           boolean required,
           boolean nullable,
           Type type,
           Class<T> typeClass,
           Supplier<T> valueGetter,
           Consumer<T> valueSetter,
           Supplier<T> defaultValueSupplier
   ) {
      this.key = key;
      this.comment = comment;
      this.required = required;
      this.nullable = nullable;
      this.type = type;
      this.typeClass = typeClass;
      this.valueGetter = valueGetter;
      this.valueSetter = valueSetter;
      this.defaultValueSupplier = defaultValueSupplier;
   }

   public static <T> ConfiguraDynamicEntryDefinition<T> create(
           String key,
           Optional<String> comment,
           boolean required,
           boolean nullable,
           Type type,
           Class<T> typeClass,
           Supplier<T> valueGetter,
           Consumer<T> valueSetter,
           Supplier<T> defaultValueSupplier
   ) {
      return new ConfiguraDynamicEntryDefinition<>(
              key,
              comment,
              required,
              nullable,
              type,
              typeClass,
              valueGetter,
              valueSetter,
              defaultValueSupplier
      );
   }

   public ConfiguraEntry<T> toEntry() {
      return new ConfiguraEntry<>(
              key,
              type,
              typeClass,
              valueGetter,
              valueSetter,
              defaultValueSupplier,
              required,
              nullable,
              comment
      );
   }

   public String key() {
      return key;
   }
}
