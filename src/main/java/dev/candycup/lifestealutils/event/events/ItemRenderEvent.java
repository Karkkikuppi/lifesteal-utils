package dev.candycup.lifestealutils.event.events;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.world.item.ItemStack;

/**
 * fired when an item entity is about to be rendered.
 * <p>
 * performance note: this event fires extremely frequently (60-144+ times per second per item).
 * listeners should be highly optimized and avoid allocations in this hot path.
 * <p>
 * can be cancelled to prevent rendering.
 * features can modify the poseStack to apply transforms (e.g., scaling).
 */
public class ItemRenderEvent extends LSUEvent {
   private final ItemStack itemStack;
   private final PoseStack poseStack;
   private final boolean isRare;

   public ItemRenderEvent(ItemStack itemStack, PoseStack poseStack, boolean isRare) {
      this.itemStack = itemStack;
      this.poseStack = poseStack;
      this.isRare = isRare;
   }

   @Override
   public boolean isCancellable() {
      return true;
   }

   public ItemStack getItemStack() {
      return itemStack;
   }

   public PoseStack getPoseStack() {
      return poseStack;
   }

   public boolean isRare() {
      return isRare;
   }
}
