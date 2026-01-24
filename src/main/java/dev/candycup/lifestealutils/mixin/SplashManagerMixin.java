package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.event.EventBus;
import dev.candycup.lifestealutils.event.events.SplashTextRequestEvent;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashManager.class)
public class SplashManagerMixin {
   /**
    * Intercepts the splash retrieval to allow an event-driven override of the splash renderer.
    *
    * Posts a SplashTextRequestEvent to the global EventBus; if the event supplies a non-null
    * splash text, sets the method's return value to a SplashRenderer rendering that text
    * (wrapped in yellow formatting when MessagingUtils.miniMessage is available), preventing
    * the original splash selection from running.
    *
    * @param cir callback used to set the SplashRenderer return value for the intercepted method
    */
   @Inject(method = "getSplash", at = @At("HEAD"), cancellable = true)
   private void getSplashHead(CallbackInfoReturnable<SplashRenderer> cir) {
      SplashTextRequestEvent event = new SplashTextRequestEvent();
      EventBus.getInstance().post(event);

      if (event.getSplashText() != null) {
         cir.setReturnValue(new SplashRenderer(
                 //? if > 1.21.10 {
                 dev.candycup.lifestealutils.interapi.MessagingUtils.miniMessage(
                         "<yellow>" + event.getSplashText() + "</yellow>"
                 )
                 //? } else {
                 /*event.getSplashText()
                  *///? }
         ));
      }
   }
}