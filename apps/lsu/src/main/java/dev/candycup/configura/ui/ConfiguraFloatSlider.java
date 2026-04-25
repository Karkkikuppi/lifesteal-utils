package dev.candycup.configura.ui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ConfiguraFloatSlider extends AbstractSliderButton {
   private final float min;
   private final float max;
   private final Consumer<Float> onChange;

   public ConfiguraFloatSlider(int x, int y, int width, int height, float currentValue, float min, float max, Consumer<Float> onChange) {
      super(x, y, width, height, Component.empty(), toNormalized(currentValue, min, max));
      this.min = min;
      this.max = max;
      this.onChange = onChange;
      updateMessage();
   }

   private static double toNormalized(float value, float min, float max) {
      if (max <= min) return 0.0;
      return Math.max(0.0, Math.min(1.0, (double) (value - min) / (max - min)));
   }

   public float getFloatValue() {
      float raw = min + (float) this.value * (max - min);
      return Math.round(raw * 10.0f) / 10.0f;
   }

   @Override
   protected void updateMessage() {
      setMessage(Component.literal(String.format("%.1f", getFloatValue())));
   }

   @Override
   protected void applyValue() {
      onChange.accept(getFloatValue());
   }
}
