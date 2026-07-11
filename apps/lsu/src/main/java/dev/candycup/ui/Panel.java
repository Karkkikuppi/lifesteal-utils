package dev.candycup.ui;

import net.minecraft.client.gui.GuiGraphics;

public final class Panel {
    private Panel() {
    }

    public static void fill(GuiGraphics graphics, Bounds bounds, int color, int strokeColor) {
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), color);
        RenderPrimitives.stroke(graphics, bounds, strokeColor);
    }

    public static void fillWithTopGradient(GuiGraphics graphics, Bounds bounds, int color, int gradientTop, int gradientBottom, int strokeColor) {
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), color);
        graphics.fillGradient(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + Math.min(34, bounds.height()), gradientTop, gradientBottom);
        RenderPrimitives.stroke(graphics, bounds, strokeColor);
    }
}
