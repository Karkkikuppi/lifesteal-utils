package dev.candycup.lifestealutils.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.candycup.lifestealutils.ItemClusterRenderStateDuck;
import dev.candycup.lifestealutils.event.EventBus;
import dev.candycup.lifestealutils.event.events.ItemRenderEvent;
//? if > 1.21.8
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
//? if > 1.21.8
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemRendererMixin {
   /**
    * Dispatches an ItemRenderEvent containing the item's ItemStack, the active PoseStack, and the item's rarity to the global EventBus during item submission.
    *
    * @param state        the render state providing the item stack and rarity
    * @param poseStack    the current pose stack used for rendering transforms
    * @param collector    the submit node collector in the render pipeline
    * @param cameraState  the current camera render state
    * @param ci           callback info for the injected method
    */

   @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
   private void dispatchItemRenderEvent(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
      ItemClusterRenderStateDuck duck = (ItemClusterRenderStateDuck) state;
      ItemStack itemStack = duck.lifestealutils$getItemStack();
      boolean isRare = duck.lifestealutils$isRare();
      
      ItemRenderEvent event = new ItemRenderEvent(itemStack, poseStack, isRare);
      EventBus.getInstance().post(event);
   }

   //?} else {
   
   /*@Unique
   private ItemEntity entity;

   @Inject(method = {"extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V"}, at = {@At("TAIL")})
   public void updateRenderState(ItemEntity itemEntity, ItemEntityRenderState itemEntityRenderState, float f, CallbackInfo ci) {
      this.entity = itemEntity;
   }

   @Inject(method = {"render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"}, at = {@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V")})
   private void dispatchItemRenderEvent(ItemEntityRenderState state, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
      ItemClusterRenderStateDuck duck = (ItemClusterRenderStateDuck) state;
      ItemStack itemStack = duck.lifestealutils$getItemStack();
      boolean isRare = duck.lifestealutils$isRare();
      
      ItemRenderEvent event = new ItemRenderEvent(itemStack, poseStack, isRare);
      EventBus.getInstance().post(event);
   }
   *///?}
}