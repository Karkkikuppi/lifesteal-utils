package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.features.ah.AhOverlaySearchInput;
import net.minecraft.client.Minecraft;
//? if >1.21.8 {
import net.minecraft.client.input.CharacterEvent;
//?}
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(KeyboardHandler.class)
public abstract class AhOverlaySearchInputMixin {
   //? if >1.21.8 {
   @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleAhOverlayChar(long window, CharacterEvent characterEvent, CallbackInfo ci) {
      if (Minecraft.getInstance().screen instanceof AhOverlaySearchInput input && input.lifestealutils$handleAhOverlayCharacter(characterEvent.codepoint())) {
         ci.cancel();
      }
   }
   //?} else {
   /*@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleAhOverlayChar(long window, int codepoint, int modifiers, CallbackInfo ci) {
      if (Minecraft.getInstance().screen instanceof AhOverlaySearchInput input && input.lifestealutils$handleAhOverlayCharacter(codepoint)) {
         ci.cancel();
      }
   }
   *///?}
}
