package dev.candycup.lifestealutils.features.alliances.ui;

import net.minecraft.network.chat.Component;

/**
 * mutable status text/color pair for UI flows.
 */
final class AllianceStatusState {
   private final int defaultColor;
   private Component message = Component.empty();
   private int color;

   AllianceStatusState(int defaultColor) {
      this.defaultColor = defaultColor;
      this.color = defaultColor;
   }

   Component message() {
      return message;
   }

   int color() {
      return color;
   }

   void set(Component message, int color) {
      this.message = message;
      this.color = color;
   }

   void clear() {
      this.message = Component.empty();
      this.color = defaultColor;
   }
}