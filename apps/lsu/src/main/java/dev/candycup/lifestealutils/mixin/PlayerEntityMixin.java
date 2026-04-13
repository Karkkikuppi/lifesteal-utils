package dev.candycup.lifestealutils.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.PlayerNameRenderEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
   @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
   public Component lsu$modifyDisplayName(Component original) {
      if (!LifestealAPI.isOnLifestealNetwork()) return original;

      if (original == null) return null;

      Component nameComponent = ((Player) (Object) this).getName();
      String plainName = nameComponent != null ? nameComponent.getString() : null;
      if (plainName == null || plainName.isBlank()) return original;

      PlayerNameRenderEvent event = new PlayerNameRenderEvent(plainName, PlayerNameRenderEvent.RenderContext.NAMETAG, original);
      LifestealUtilsEvents.PLAYER_NAME_RENDER.invoker().onPlayerNameRender(event);

      return event.getModifiedDisplayName();
   }

}
