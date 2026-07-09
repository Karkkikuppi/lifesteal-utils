package dev.candycup.ui;

import net.minecraft.client.gui.GuiGraphics;

public final class Scrollbar {
   private Scrollbar() {
   }

   public static void vertical(GuiGraphics graphics, int x, int y, int height, int offset, int maxOffset, int trackColor, int thumbColor) {
      if (maxOffset <= 0) {
         return;
      }
      graphics.fill(x, y, x + 2, y + height, trackColor);
      int thumbHeight = Math.max(16, height / 4);
      int travel = Math.max(1, height - thumbHeight);
      int thumbY = y + Math.round((travel * offset) / (float) maxOffset);
      graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, thumbColor);
   }
}
