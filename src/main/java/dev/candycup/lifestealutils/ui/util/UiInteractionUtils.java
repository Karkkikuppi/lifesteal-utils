package dev.candycup.lifestealutils.ui.util;

import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;

public final class UiInteractionUtils {
   private UiInteractionUtils() {
   }

   public static boolean isHovered(UiInputState input, UiBounds bounds) {
      return input.isHovering(bounds);
   }

   public static boolean isHovered(UiInputState input, UiBounds bounds, boolean enabled) {
      return enabled && isHovered(input, bounds);
   }

   public static boolean shouldClick(boolean wasPressed, boolean hovered, UiInputState input) {
      return wasPressed && input.leftReleased() && hovered;
   }

   public static boolean nextPressedState(boolean currentPressed, boolean hovered, UiInputState input) {
      if (!input.leftDown()) {
         return false;
      }
      if (hovered && input.leftClicked()) {
         return true;
      }
      if (currentPressed && input.leftReleased()) {
         return false;
      }
      return currentPressed;
   }
}