package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.features.ah.AhOverlaySearchInput;
import net.minecraft.client.Minecraft;
//? if >1.21.8 {
import net.minecraft.client.input.CharacterEvent;
//?}
//? if >=26.1 {
/*import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?} else {
import net.minecraft.client.gui.screens.Screen;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=26.1 {
/*@Mixin(KeyboardHandler.class)
*///?} else {
@Mixin(Screen.class)
//?}
public abstract class AhOverlaySearchInputMixin {
   //? if >=26.1 {
   /*@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleAhOverlayChar(long window, CharacterEvent characterEvent, CallbackInfo ci) {
      if (Minecraft.getInstance().screen instanceof AhOverlaySearchInput input && input.lifestealutils$handleAhOverlayCharacter(characterEvent.codepoint())) {
         ci.cancel();
      }
   }
   *///?} else {
   //? if >1.21.8 {
   @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleAhOverlayChar(CharacterEvent characterEvent, CallbackInfoReturnable<Boolean> cir) {
      if ((Object) this instanceof AhOverlaySearchInput input && input.lifestealutils$handleAhOverlayCharacter(characterEvent.codepoint())) {
         cir.setReturnValue(true);
      }
   }
   //?} else {
   /*@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleAhOverlayChar(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
      if ((Object) this instanceof AhOverlaySearchInput input && input.lifestealutils$handleAhOverlayCharacter(chr)) {
         cir.setReturnValue(true);
      }
   }
   *///?}
   //?}
}
