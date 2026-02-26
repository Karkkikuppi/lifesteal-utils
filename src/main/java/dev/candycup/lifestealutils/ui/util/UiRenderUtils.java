package dev.candycup.lifestealutils.ui.util;

import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class UiRenderUtils {
   private UiRenderUtils() {
   }

   public static int centeredTextX(Font font, Component text, UiBounds bounds) {
      return bounds.x() + (bounds.width() - font.width(text)) / 2;
   }

   public static int centeredTextY(Font font, UiBounds bounds) {
      return bounds.y() + (bounds.height() - font.lineHeight) / 2;
   }
}