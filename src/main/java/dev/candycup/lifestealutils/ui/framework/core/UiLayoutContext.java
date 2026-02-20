package dev.candycup.lifestealutils.ui.framework.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * provides layout-time context for ui components.
 */
public record UiLayoutContext(Minecraft minecraft, Font font, UiBounds availableBounds) {
   public UiLayoutContext withBounds(UiBounds bounds) {
      return new UiLayoutContext(minecraft, font, bounds);
   }
}
