package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.PlayerNameRenderEvent;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public abstract class TabListMixin {
   @Shadow
   protected abstract List<PlayerInfo> getPlayerInfos();

   @Shadow
   public abstract Component getNameForDisplay(PlayerInfo playerInfo);

   @Inject(method = "render", at = @At("HEAD"))
   private void renderHead(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
      List<PlayerInfo> list = getPlayerInfos();


      for (PlayerInfo playerInfo : list) {
         if (playerInfo == null) continue;
         Component component = getNameForDisplay(playerInfo);
      }
   }

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
