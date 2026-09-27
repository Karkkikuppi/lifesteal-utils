package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.gaia.GaiaConsentController;
import dev.candycup.lifestealutils.gaia.GaiaConsentScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens Gaia consent one tick after the multiplayer screen is shown.
 */
@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin extends Screen {
    @Unique
    private boolean lsu$openLsuOverlayNextTick;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        if (GaiaConsentController.shouldShowConsent()) {
            lsu$openLsuOverlayNextTick = true;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (!lsu$openLsuOverlayNextTick) {
            return;
        }
        lsu$openLsuOverlayNextTick = false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != this) {
            return;
        }

        if (GaiaConsentController.shouldShowConsent()) {
            minecraft.setScreen(new GaiaConsentScreen(this, true));
        }
    }
}
