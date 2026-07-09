package dev.candycup.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class Button {
   private Button() {
   }

   public static void pill(GuiGraphics graphics, Font font, Bounds bounds, String label, int textColor, int accentColor, boolean hovered) {
      String display = Text.trim(font, label, bounds.width() - 8);
      int bg = hovered ? RenderPrimitives.withAlpha(accentColor, 0x66) : RenderPrimitives.withAlpha(accentColor, 0x3C);
      graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), bg);
      RenderPrimitives.stroke(graphics, bounds, hovered ? RenderPrimitives.withAlpha(accentColor, 0xFF) : RenderPrimitives.withAlpha(accentColor, 0x88));
      int textX = bounds.x() + (bounds.width() - font.width(display)) / 2;
      int textY = bounds.y() + (bounds.height() - font.lineHeight) / 2;
      graphics.drawString(font, Component.literal(display), textX, textY, textColor, false);
   }
}
