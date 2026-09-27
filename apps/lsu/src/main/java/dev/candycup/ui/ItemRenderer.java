package dev.candycup.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ItemRenderer {
    private ItemRenderer() {
    }

    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popMatrix();
    }
}
