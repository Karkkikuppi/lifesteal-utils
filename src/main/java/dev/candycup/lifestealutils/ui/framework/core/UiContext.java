package dev.candycup.lifestealutils.ui.framework.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * provides render-time context for ui components.
 */
public record UiContext(
        Minecraft minecraft,
        GuiGraphics graphics,
        UiInputState input,
        int width,
        int height,
        float partialTick
) {
}
