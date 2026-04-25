package dev.candycup.configura.ui;

import dev.candycup.configura.core.ToggleGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ConfiguraConfigModel {
   private ConfiguraConfigModel() {
   }

   public enum OptionType {
      BOOLEAN,
      STRING,
      MINIMESSAGE,
      FLOAT,
      ENUM,
      LIST,
      TOGGLE_GROUP
   }

   public record ResolvedConfig(Component title, List<UiCategory> categories, Runnable onSave,
                                Runnable onSavedFeedback, Runnable onResetToDefaults,
                                Runnable onResetFeedback) {
   }

   public record UiCategory(String id, Component displayName, List<UiFeature> features) {
   }

   public sealed interface UiFeatureItem permits UiFeatureConfigurable, UiFeatureAccordion {
   }

   public record UiFeatureConfigurable(UiConfigurable configurable) implements UiFeatureItem {
   }

   public record UiFeatureAccordion(UiAccordion accordion) implements UiFeatureItem {
   }

   public record UiAccordion(String id, Component displayName, List<UiConfigurable> configurables) {
   }

   public record UiFeature(String id, Component displayName, List<UiFeatureItem> items) {
      public List<UiConfigurable> allConfigurables() {
         List<UiConfigurable> result = new ArrayList<>();
         for (UiFeatureItem item : items) {
            switch (item) {
               case UiFeatureConfigurable c -> result.add(c.configurable());
               case UiFeatureAccordion a -> result.addAll(a.accordion().configurables());
            }
         }
         return result;
      }
   }

   public record UiToggleEntry(
           String key,
           Component displayName,
           Supplier<ItemStack> iconSupplier
   ) {
      public ItemStack icon() {
         if (iconSupplier == null) return ItemStack.EMPTY;
         ItemStack stack = iconSupplier.get();
         return stack == null ? ItemStack.EMPTY : stack;
      }
   }

   public record UiConfigurable(
           String key,
           String id,
           OptionType type,
           float min,
           float max,
           boolean hasFloatBounds,
           Supplier<?> valueSupplier,
           Consumer<Object> valueConsumer,
           Supplier<?> defaultSupplier,
           Component displayName,
           Component description,
           Component disabledMessage,
           boolean remotelyForced,
           List<? extends Enum<?>> enumValues,
           Function<Enum<?>, Component> enumLabeler,
           Supplier<ItemStack> iconSupplier,
           List<UiToggleEntry> toggleEntries
   ) {
      public Object readValue() {
         Object value = valueSupplier.get();
         if (value instanceof List<?> list) {
            return new ArrayList<>(list);
         }
         if (value instanceof ToggleGroup tg) {
            return tg.copy();
         }
         return value;
      }

      public void writeValue(Object value) {
         valueConsumer.accept(value);
      }

      public Component enumLabel(Enum<?> enumValue) {
         return enumLabeler.apply(enumValue);
      }

      public ItemStack icon() {
         if (iconSupplier == null) {
            return ItemStack.EMPTY;
         }
         ItemStack stack = iconSupplier.get();
         return stack == null ? ItemStack.EMPTY : stack;
      }
   }
}
