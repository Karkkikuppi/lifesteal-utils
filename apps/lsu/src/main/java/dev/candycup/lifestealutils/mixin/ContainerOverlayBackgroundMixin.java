package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.features.ContainerOverlayBackgroundState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces container backgrounds at the render dispatch call site on versions where concrete
 * container screens own background extraction.
 */
@Mixin(Screen.class)
public abstract class ContainerOverlayBackgroundMixin {
    //? if >=26.1 {
    /*@Redirect(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void lifestealutils$replaceContainerBackground(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!(screen instanceof ContainerOverlayBackgroundState state)
                || !state.lifestealutils$shouldReplaceContainerBackground()) {
            screen.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        // Calling the concrete override would append its container texture after the vanilla blur.
        ((ScreenAccessor) screen).invokeRenderBlurredBackground(guiGraphics);
    }
    *///?}
}
