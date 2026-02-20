package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.util.UiInteractionUtils;
import dev.candycup.lifestealutils.ui.util.UiRenderUtils;
import net.minecraft.network.chat.Component;

/**
 * shared rendering and interaction helpers for alliance ui buttons.
 */
final class AllianceButtonUiSupport {
   private AllianceButtonUiSupport() {
   }

   static int resolveTextColor(boolean enabled, boolean hovered, int textColor, int hoverColor, int disabledColor) {
      if (!enabled) {
         return disabledColor;
      }
      return hovered ? hoverColor : textColor;
   }

   static void drawCenteredLabel(UiContext context, UiBounds bounds, Component label, int color) {
      int textX = UiRenderUtils.centeredTextX(context.minecraft().font, label, bounds);
      int textY = UiRenderUtils.centeredTextY(context.minecraft().font, bounds);
      context.graphics().drawString(context.minecraft().font, label, textX, textY, color, true);
   }

   static boolean resolveHovered(UiInputState input, UiBounds bounds, boolean enabled) {
      return UiInteractionUtils.isHovered(input, bounds, enabled);
   }

   static boolean resolvePressed(UiInputState input, boolean pressed, boolean hovered, Runnable onClick) {
      boolean wasPressed = pressed;
      boolean nextPressed = UiInteractionUtils.nextPressedState(pressed, hovered, input);
      if (UiInteractionUtils.shouldClick(wasPressed, hovered, input) && onClick != null) {
         onClick.run();
      }
      return nextPressed;
   }
}
