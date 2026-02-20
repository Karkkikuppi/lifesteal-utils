package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.SplashTextRequestEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashManager.class)
public class SplashManagerMixin {
   @Inject(method = "getSplash", at = @At("HEAD"), cancellable = true)
   private void getSplashHead(CallbackInfoReturnable<SplashRenderer> cir) {
      SplashTextRequestEvent event = new SplashTextRequestEvent();
      LifestealUtilsEvents.SPLASH_TEXT_REQUEST.invoker().onSplashTextRequest(event);

      if (event.getSplashText() != null) {
         cir.setReturnValue(new SplashRenderer(
                 //? if > 1.21.10 {
                 MessagingUtils.miniMessage(
                         "<yellow>" + event.getSplashText() + "</yellow>"
                 )
                 //? } else {
                 /*event.getSplashText()
                  *///? }
         ));
      }
   }
}
