package dev.candycup.ui;

import net.minecraft.client.gui.GuiGraphics;

public final class RenderPrimitives {
    private RenderPrimitives() {
    }

    public static void stroke(GuiGraphics graphics, Bounds bounds, int color) {
        stroke(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), color);
    }

    public static void stroke(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
