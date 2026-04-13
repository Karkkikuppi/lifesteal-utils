package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.features.ah.AhOverlaySearchInput;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
//? if >1.21.8 {
import net.minecraft.client.input.CharacterEvent;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerSearchInputMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   //? if >1.21.8 {
   @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$forwardOverlaySearchInput(long window, CharacterEvent characterEvent, CallbackInfo ci) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      if (!(this.minecraft.screen instanceof AhOverlaySearchInput overlaySearchInput)) {
         return;
      }
      if (overlaySearchInput.lifestealutils$handleOverlayCharTyped(characterEvent)) {
         ci.cancel();
      }
   }
   //?} else {
   /*@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$forwardOverlaySearchInput(long window, char chr, int modifiers, CallbackInfo ci) {
      if (!(this.minecraft.screen instanceof AhOverlaySearchInput overlaySearchInput)) {
         return;
      }
      if (overlaySearchInput.lifestealutils$handleOverlayCharTyped(chr, modifiers)) {
         ci.cancel();
      }
   }
   *///?}
}
