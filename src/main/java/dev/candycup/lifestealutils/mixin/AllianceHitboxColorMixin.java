package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.features.alliances.AllianceHitboxColorResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >1.21.10 {
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
//?} else {
/*import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

//? if >1.21.10 {
@Mixin(EntityHitboxDebugRenderer.class)
public abstract class AllianceHitboxColorMixin {
   @Redirect(method = "emitGizmos", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInvisible()Z"))
   private boolean lsu$showInvisibleAllianceHitboxes(Entity entity) {
      boolean invisible = entity.isInvisible();
      if (!invisible || !(entity instanceof Player player)) {
         return invisible;
      }

      return !AllianceHitboxColorResolver.shouldRenderInvisibleHitbox(player);
   }

   @ModifyVariable(method = "showHitboxes", at = @At("STORE"), ordinal = 0)
   private int lsu$colorAllianceHitboxes(int color, Entity entity, float tickDelta, boolean serverEntity) {
      if (serverEntity || !(entity instanceof Player player)) {
         return color;
      }

      try {
         Integer resolvedColor = AllianceHitboxColorResolver.resolveColor(player);
         return resolvedColor != null ? resolvedColor | 0xFF000000 : color;
      } catch (RuntimeException ignored) {
         return color;
      }
   }
}
//?} else {
/*@Mixin(EntityRenderer.class)
public abstract class AllianceHitboxColorMixin {
   //? if >1.21.8 {
   @Redirect(
           method = "finalizeRenderState",
           at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;isInvisible:Z")
   )
   private boolean lsu$showInvisibleAllianceHitboxes(EntityRenderState renderState, Entity entity, EntityRenderState ignoredState) {
   //?} else {
   @Redirect(
           method = "extractRenderState",
           at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;isInvisible:Z")
   )
   private boolean lsu$showInvisibleAllianceHitboxes(EntityRenderState renderState, Entity entity, EntityRenderState ignoredState, float tickDelta) {
   //?}
      boolean invisible = entity.isInvisible();
      if (!invisible || !(entity instanceof Player player)) {
         return invisible;
      }

      return !AllianceHitboxColorResolver.shouldRenderInvisibleHitbox(player);
   }

   @Inject(
            method = "extractHitboxes(Lnet/minecraft/world/entity/Entity;FZ)Lnet/minecraft/client/renderer/entity/state/HitboxesRenderState;",
            at = @At("RETURN"),
            cancellable = true
   )
   private void lsu$colorAllianceHitboxes(Entity entity, float tickDelta, boolean serverEntity,
                                          CallbackInfoReturnable<HitboxesRenderState> cir) {
      if (serverEntity || !(entity instanceof Player player)) {
         return;
      }

      try {
         Integer resolvedColor = AllianceHitboxColorResolver.resolveColor(player);
         if (resolvedColor == null) {
            return;
         }

         HitboxesRenderState state = cir.getReturnValue();
         if (state == null || state.hitboxes().isEmpty()) {
            return;
         }

         HitboxRenderState firstHitbox = state.hitboxes().get(0);
         if (firstHitbox == null) {
            return;
         }

         HitboxRenderState recoloredHitbox = new HitboxRenderState(
                 firstHitbox.x0(),
                 firstHitbox.y0(),
                 firstHitbox.z0(),
                 firstHitbox.x1(),
                 firstHitbox.y1(),
                 firstHitbox.z1(),
                 firstHitbox.offsetX(),
                 firstHitbox.offsetY(),
                 firstHitbox.offsetZ(),
                 colorComponent(resolvedColor, 16),
                 colorComponent(resolvedColor, 8),
                 colorComponent(resolvedColor, 0)
         );

         ImmutableList.Builder<HitboxRenderState> hitboxes = ImmutableList.builder();
         hitboxes.add(recoloredHitbox);
         for (int index = 1; index < state.hitboxes().size(); index++) {
            hitboxes.add(state.hitboxes().get(index));
         }

         cir.setReturnValue(new HitboxesRenderState(
                 state.viewX(),
                 state.viewY(),
                 state.viewZ(),
                 hitboxes.build()
         ));
      } catch (RuntimeException ignored) {
         return;
      }
   }

   private static float colorComponent(int color, int shift) {
      return ((color >> shift) & 0xFF) / 255.0f;
   }
}
*///?}
