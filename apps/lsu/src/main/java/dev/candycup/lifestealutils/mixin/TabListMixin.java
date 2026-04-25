package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.PlayerNameRenderEvent;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class TabListMixin {
   @Inject(method = "decorateName", at = @At("HEAD"), cancellable = true)
   private void decorateNameHead(PlayerInfo playerInfo, MutableComponent mutableComponent, CallbackInfoReturnable<Component> cir) {
      Component result = mutableComponent;

      //? if > 1.21.8 {
      String plainName = playerInfo.getProfile().name();
      //?} else {
      /*String plainName = playerInfo.getProfile().getName();
       *///?}
      if (plainName != null && !plainName.isBlank()) {
         PlayerNameRenderEvent event = new PlayerNameRenderEvent(plainName, PlayerNameRenderEvent.RenderContext.TABLIST, result);
         LifestealUtilsEvents.PLAYER_NAME_RENDER.invoker().onPlayerNameRender(event);
         result = event.getModifiedDisplayName();
      }

      cir.setReturnValue(result);
   }
}
