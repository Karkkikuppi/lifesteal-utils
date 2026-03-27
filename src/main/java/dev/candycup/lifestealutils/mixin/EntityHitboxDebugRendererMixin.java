package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.features.alliances.Alliances;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {
   @ModifyVariable(method = "showHitboxes", at = @At("STORE"), ordinal = 0)
   private int lsu$colorAllianceHitboxes(int color, Entity entity, float tickDelta, boolean serverEntity) {
      if (serverEntity || !Config.isEnableAlliances()) {
         return color;
      }

      if (lsu$shouldColorOwnHitbox(entity)) {
         return Config.getAllianceNameColor();
      }

      if (!(entity instanceof Player player) || !Alliances.isAllied(player)) {
         return color;
      }

      return Config.getAllianceNameColor();
   }

   private static boolean lsu$shouldColorOwnHitbox(Entity entity) {
      if (!Config.isShowOwnAllianceHitboxInThirdPerson()) {
         return false;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || entity != client.player) {
         return false;
      }

      CameraType cameraType = client.options.getCameraType();
      return !cameraType.isFirstPerson();
   }
}
