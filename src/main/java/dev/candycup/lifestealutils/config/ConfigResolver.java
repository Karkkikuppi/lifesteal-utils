package dev.candycup.lifestealutils.config;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.config.configurables.*;
import dev.candycup.lifestealutils.config.controllers.MinimessageController;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfigResolver {
   public static YetAnotherConfigLib resolve() {
      ConfigDescriptorRegistry.registerDefaultProviders();
      return resolve(ConfigContainerRegistry.getRegisteredContainers());
   }

   public static YetAnotherConfigLib resolve(Class<?> configClass) {
      return resolve(List.of(configClass));
   }

   private static YetAnotherConfigLib resolve(List<Class<?>> configClasses) {
      ConfigurationTree tree = new ConfigurationTree();
      Map<String, ConfigurationCategory> categoriesByName = new LinkedHashMap<>();

      for (Class<?> configClass : configClasses) {
         collectStaticFieldOptions(categoriesByName, configClass);
      }

      List<ConfigOptionDescriptor<?>> dynamicOptions = new ArrayList<>();
      ConfigOptionCollector collector = dynamicOptions::add;
      for (ConfigOptionProvider provider : ConfigDescriptorRegistry.getProviders()) {
         provider.registerOptions(collector);
      }

      for (ConfigOptionDescriptor<?> descriptor : dynamicOptions) {
         ConfigurationOption option = descriptorToOption(descriptor);
         addOption(categoriesByName, option);
      }

      tree.categories = sortedCategories(categoriesByName);

      return tree.toYACL();
   }

   private static void collectStaticFieldOptions(Map<String, ConfigurationCategory> categoriesByName, Class<?> configClass) {
      for (Field field : configClass.getDeclaredFields()) {
         ConfigurationOption option = optionFromStaticField(field);
         if (option == null) {
            continue;
         }
         addOption(categoriesByName, option);
      }
   }

   private static ConfigurationOption optionFromStaticField(Field field) {
      if (!Modifier.isStatic(field.getModifiers())) {
         return null;
      }

      ConfigurableBoolean configurableBoolean = field.getAnnotation(ConfigurableBoolean.class);
      ConfigurableString configurableString = field.getAnnotation(ConfigurableString.class);
      ConfigurableMinimessage configurableMinimessage = field.getAnnotation(ConfigurableMinimessage.class);
      ConfigurableFloat configurableFloat = field.getAnnotation(ConfigurableFloat.class);
      ConfigurableEnum configurableEnum = field.getAnnotation(ConfigurableEnum.class);
      ConfigurableList configurableList = field.getAnnotation(ConfigurableList.class);
      if (configurableBoolean == null && configurableString == null && configurableMinimessage == null
              && configurableFloat == null && configurableEnum == null && configurableList == null) {
         return null;
      }

      String location =
              configurableBoolean != null ? configurableBoolean.location() :
                      configurableString != null ? configurableString.location() :
                              configurableMinimessage != null ? configurableMinimessage.location() :
                                      configurableFloat != null ? configurableFloat.location() :
                                              configurableEnum != null ? configurableEnum.location() :
                                                      configurableList.location();
      OptionType optionType =
              configurableBoolean != null ? OptionType.BOOLEAN :
                      configurableString != null ? OptionType.STRING :
                              configurableMinimessage != null ? OptionType.MINIMESSAGE :
                                      configurableFloat != null ? OptionType.FLOAT :
                                              configurableEnum != null ? OptionType.ENUM :
                                                      OptionType.LIST;

      String[] segments = location.split("\\.");
      boolean isListType = optionType == OptionType.LIST;
      if (isListType && segments.length != 2) {
         throw new IllegalArgumentException("invalid list location '%s' on field '%s', expected category.group".formatted(location, field.getName()));
      }
      if (!isListType && segments.length != 3) {
         throw new IllegalArgumentException("invalid configurable location '%s' on field '%s', expected category.group.entry".formatted(location, field.getName()));
      }

      try {
         field.setAccessible(true);
         Object defaultValue = field.get(null);

         ConfigurationOption option = new ConfigurationOption();
         option.category = segments[0];
         option.group = segments[1];
         option.name = isListType ? segments[1] : segments[2];
         option.listEntry = isListType;
         option.defaultValue = optionType == OptionType.LIST
                 ? new ArrayList<>((List<String>) defaultValue)
                 : defaultValue;
         option.type = optionType;
         option.enumClass = field.getType().isEnum() ? (Class<? extends Enum<?>>) field.getType() : null;
         option.valueSupplier = () -> readStaticValue(field);
         option.valueConsumer = value -> writeStaticValue(field, value);
         return option;
      } catch (IllegalAccessException e) {
         throw new IllegalStateException("failed to read configurable field '%s'".formatted(field.getName()), e);
      }
   }

   private static List<ConfigurationCategory> sortedCategories(Map<String, ConfigurationCategory> categoriesByName) {
      List<ConfigurationCategory> categories = new ArrayList<>(categoriesByName.values());
      categories.sort(Comparator.comparing(configurationCategory -> configurationCategory.name));
      categories.forEach(category -> category.groups.sort(Comparator
              .comparing((ConfigurationGroup configurationGroup) -> configurationGroup.name)
              .thenComparing(configurationGroup -> configurationGroup.listGroup ? configurationGroup.listOption.name : "")));
      return categories;
   }

   private static ConfigurationOption descriptorToOption(ConfigOptionDescriptor<?> descriptor) {
      ConfigurationOption option = new ConfigurationOption();
      option.category = descriptor.category();
      option.group = descriptor.group();
      option.name = descriptor.name();
      option.defaultValue = descriptor.kind() == ConfigOptionDescriptor.Kind.LIST
              ? new ArrayList<>((List<String>) descriptor.defaultSupplier().get())
              : descriptor.defaultSupplier().get();
      option.type = switch (descriptor.kind()) {
         case BOOLEAN -> OptionType.BOOLEAN;
         case STRING -> OptionType.STRING;
         case MINIMESSAGE -> OptionType.MINIMESSAGE;
         case FLOAT -> OptionType.FLOAT;
         case ENUM -> OptionType.ENUM;
         case LIST -> OptionType.LIST;
      };
      option.listEntry = descriptor.kind() == ConfigOptionDescriptor.Kind.LIST;
      option.enumClass = descriptor.kind() == ConfigOptionDescriptor.Kind.ENUM
              ? (Class<? extends Enum<?>>) descriptor.enumClass()
              : null;
      option.valueSupplier = descriptor.valueSupplier();
      option.valueConsumer = descriptor.valueConsumer();
      option.hardName = descriptor.hardName().orElse(null);
      option.hardDescription = descriptor.hardDescription().orElse(null);
      return option;
   }

   private static void addOption(Map<String, ConfigurationCategory> categoriesByName, ConfigurationOption option) {
      ConfigurationCategory category = categoriesByName.computeIfAbsent(option.category, name -> {
         ConfigurationCategory newCategory = new ConfigurationCategory();
         newCategory.name = name;
         newCategory.groups = new ArrayList<>();
         return newCategory;
      });

      if (option.type == OptionType.LIST) {
         ConfigurationGroup listGroup = category.groups.stream()
                 .filter(existing -> existing.listGroup && Objects.equals(existing.name, option.group))
                 .findFirst()
                 .orElseGet(() -> {
                    ConfigurationGroup newGroup = new ConfigurationGroup();
                    newGroup.category = category.name;
                    newGroup.name = option.group;
                    newGroup.listGroup = true;
                    category.groups.add(newGroup);
                    return newGroup;
                 });

         listGroup.listOption = option;
         return;
      }

      ConfigurationGroup group = category.groups.stream()
              .filter(existing -> !existing.listGroup && Objects.equals(existing.name, option.group))
              .findFirst()
              .orElseGet(() -> {
                 ConfigurationGroup newGroup = new ConfigurationGroup();
                 newGroup.category = category.name;
                 newGroup.name = option.group;
                 newGroup.optionsByName = new LinkedHashMap<>();
                 category.groups.add(newGroup);
                 return newGroup;
              });

      group.optionsByName.put(option.name, option);
   }

   private static Object readStaticValue(Field field) {
      try {
         return field.get(null);
      } catch (IllegalAccessException e) {
         throw new IllegalStateException("failed to read configurable field '%s'".formatted(field.getName()), e);
      }
   }

   private static void writeStaticValue(Field field, Object value) {
      try {
         field.set(null, value);
      } catch (IllegalAccessException e) {
         throw new IllegalStateException("failed to write configurable field '%s'".formatted(field.getName()), e);
      }
   }

   enum OptionType {
      BOOLEAN,
      STRING,
      MINIMESSAGE,
      FLOAT,
      ENUM,
      LIST,
   }

   static class ConfigurationTree {
      List<ConfigurationCategory> categories;

      public YetAnotherConfigLib toYACL() {
         List<ConfigCategory> yaclCategories = categories.stream()
                 .map(ConfigurationCategory::toYACLCategory)
                 .toList();

         return YetAnotherConfigLib.createBuilder()
                 .title(Component.translatable("lsu.config.title"))
                 .categories(yaclCategories)
                 .save(() -> Config.HANDLER.save())
                 .build();
      }
   }

   static class ConfigurationCategory {
      List<ConfigurationGroup> groups;
      String name;

      public ConfigCategory toYACLCategory() {
         return ConfigCategory.createBuilder()
                 .name(Component.translatable("lsu.config.%s".formatted(name.toLowerCase())))
                 .groups(groups.stream().map(ConfigurationGroup::toYACLGroup).collect(Collectors.toCollection(
                         ArrayList::new
                 )))
                 .build();
      }
   }

   static class ConfigurationGroup {
      Map<String, ConfigurationOption> optionsByName;
      String category;
      String name;
      boolean listGroup;
      ConfigurationOption listOption;

      public OptionGroup toYACLGroup() {
         if (listGroup) {
            return listOption.toYACLListGroup();
         }

         return OptionGroup.createBuilder()
                 .name(Component.translatable("lsu.config.%s.%s".formatted(category.toLowerCase(), name.toLowerCase())))
                 .options(optionsByName.values().stream()
                         .sorted(Comparator.comparing(configurationOption -> configurationOption.name))
                         .map(ConfigurationOption::toYACLOption).collect(Collectors.toCollection(
                                 ArrayList::new
                         )))
                 .build();
      }
   }

   static class ConfigurationOption {
      String category;
      String group;
      String name;
      Object defaultValue;
      OptionType type;
      Class<? extends Enum<?>> enumClass;
      boolean listEntry;
      String hardName;
      String hardDescription;

      Supplier<?> valueSupplier;
      Consumer<?> valueConsumer;

      private Component resolveName() {
         if (hardName != null && !hardName.isBlank()) {
            return Component.literal(hardName);
         }
         return Component.translatable("lsu.config.%s.%s.%s".formatted(category.toLowerCase(), group.toLowerCase(), name.toLowerCase()));
      }

      public Option<?> toYACLOption() {
         return switch (type) {
            case BOOLEAN -> Option.<Boolean>createBuilder()
                    .name(resolveName())
                    .description(resolveDescription())
                    .binding((Boolean) defaultValue, (Supplier<Boolean>) valueSupplier, (Consumer<Boolean>) valueConsumer)
                    .controller(TickBoxControllerBuilder::create)
                    .build();
            case STRING -> Option.<String>createBuilder()
                    .name(resolveName())
                    .description(resolveDescription())
                    .binding((String) defaultValue, (Supplier<String>) valueSupplier, (Consumer<String>) valueConsumer)
                    .controller(StringControllerBuilder::create)
                    .build();
            case MINIMESSAGE -> Option.<String>createBuilder()
                    .name(resolveName())
                    .description(resolveDescription())
                    .binding((String) defaultValue, (Supplier<String>) valueSupplier, (Consumer<String>) valueConsumer)
                    .customController(MinimessageController::new)
                    .build();
            case FLOAT -> Option.<Float>createBuilder()
                    .name(resolveName())
                    .description(resolveDescription())
                    .binding((Float) defaultValue, (Supplier<Float>) valueSupplier, (Consumer<Float>) valueConsumer)
                    .controller(FloatFieldControllerBuilder::create)
                    .build();
            case ENUM -> createEnumOption();
            case LIST ->
                    throw new IllegalStateException("list options are represented as groups and cannot be built as plain options");
         };
      }

      private OptionDescription resolveDescription() {
         if (hardDescription != null && !hardDescription.isBlank()) {
            return OptionDescription.createBuilder()
                    .text(Component.literal(hardDescription))
                    .build();
         }

         if (listEntry) {
            return OptionDescription.createBuilder()
                    .text(Component.translatable("lsu.config.%s.%s.desc".formatted(category.toLowerCase(), group.toLowerCase())))
                    .build();
         }

         return OptionDescription.createBuilder()
                 .text(Component.translatable("lsu.config.%s.%s.%s.desc".formatted(category.toLowerCase(), group.toLowerCase(), name.toLowerCase())))
                 .build();
      }

      private Option<?> createEnumOption() {
         Class<?> enumClassRaw = enumClass;
         if (enumClassRaw == null) {
            throw new IllegalStateException("enum option '%s' is missing enum class information".formatted(name));
         }
         if (!enumClassRaw.isEnum()) {
            throw new IllegalStateException("option '%s' is marked as enum configurable, but does not have an enum type".formatted(name));
         }

         return createEnumOptionTyped((Class<? extends Enum<?>>) enumClassRaw);
      }

      private <T extends Enum<T>> Option<?> createEnumOptionTyped(Class<? extends Enum<?>> enumClassRaw) {
         Class<T> enumClass = (Class<T>) enumClassRaw;
         return Option.<T>createBuilder()
                 .name(resolveName())
                 .description(resolveDescription())
                 .binding((T) defaultValue, (Supplier<T>) valueSupplier, (Consumer<T>) valueConsumer)
                 .controller(option -> EnumControllerBuilder.create(option)
                         .enumClass(enumClass)
                         .formatValue(enumValue -> resolveEnumLabel(enumValue, category, group, name)))
                 .build();
      }

      private OptionGroup toYACLListGroup() {
         return ListOption.<String>createBuilder()
                 .name(Component.translatable("lsu.config.%s.%s".formatted(category.toLowerCase(), group.toLowerCase())))
                 .description(resolveDescription())
                 .binding((List<String>) defaultValue, (Supplier<List<String>>) valueSupplier, (Consumer<List<String>>) valueConsumer)
                 .controller(StringControllerBuilder::create)
                 .initial("")
                 .build();
      }

      private Component resolveEnumLabel(Enum<?> enumValue, String category, String group, String name) {
         try {
            Method getTranslationKey = enumValue.getClass().getMethod("getTranslationKey");
            if (getTranslationKey.getReturnType() == String.class) {
               String translationKey = (String) getTranslationKey.invoke(enumValue);
               if (translationKey != null && !translationKey.isBlank()) {
                  return Component.translatable(translationKey);
               }
            }
         } catch (Exception ignored) {
         }

         String valueKey = "lsu.config.%s.%s.%s.%s"
                 .formatted(category.toLowerCase(), group.toLowerCase(), name.toLowerCase(), enumValue.name().toLowerCase());
         return Component.translatable(valueKey);
      }
   }
}
