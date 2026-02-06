package dev.candycup.lifestealutils.mixin.afk;

import dev.candycup.lifestealutils.features.afk.AfkMode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   public void lifestealutils$onRenderLevel(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (!AfkMode.isEnabled()) {
         return;
      }
      ci.cancel();
   }
}
