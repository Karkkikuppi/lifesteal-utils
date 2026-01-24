package dev.candycup.lifestealutils.mixin;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.candycup.lifestealutils.ItemClusterRenderStateDuck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ItemClusterRenderState.class)
public class ItemClusterMixin implements ItemClusterRenderStateDuck {

   @Unique
   private boolean lifestealutils$isRare = false;

   @Inject(method = "extractItemGroupRenderState", at = @At("HEAD"))

   private void lifestealutils$captureRare(Entity entity, ItemStack stack, ItemModelResolver resolver, CallbackInfo ci) {
      if (stack.isEmpty()) {
         lifestealutils$setRare(false);
         return;
      }

      Item item = stack.getItem();

      if (item == Items.NETHERITE_HELMET ||
              item == Items.NETHERITE_CHESTPLATE ||
              item == Items.NETHERITE_LEGGINGS ||
              item == Items.NETHERITE_BOOTS ||
              item == Items.NETHERITE_SWORD ||
              item == Items.NETHERITE_AXE ||
              item == Items.NETHERITE_PICKAXE ||
              item == Items.NETHERITE_SHOVEL ||
              item == Items.NETHERITE_HOE ||
              item == Items.ANCIENT_DEBRIS ||
              item == Items.NETHERITE_SCRAP ||
              item == Items.NETHERITE_BLOCK ||
              item == Items.NETHERITE_INGOT) {
         lifestealutils$setRare(true);
      }

      Tag tag = encodeStack(stack, Minecraft.getInstance().player.registryAccess().createSerializationContext(NbtOps.INSTANCE));

      if (tag instanceof CompoundTag nbt) {
         nbt.getCompound("minecraft:custom_data").ifPresent(custom -> {
            custom.getCompound("PublicBukkitValues").ifPresent(pbv -> {
               if (pbv.contains("lifesteal:artifact")) {
                  lifestealutils$setRare(true);
                  return;
               }
               for (String key : pbv.keySet()) {
                  if (key.startsWith("enchants:")) {
                     lifestealutils$setRare(true);
                     return;
                  }
               }
            });
         });
      }
   }

   private static CompoundTag encodeStack(ItemStack stack, DynamicOps<Tag> ops) {
      DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(ops, stack.getComponentsPatch());
      result.ifError((e) -> {
      });
      Tag nbtElement = result.getOrThrow();
      return (CompoundTag) nbtElement;
   }


   @Override
   public boolean lifestealutils$isRare() {
      return lifestealutils$isRare;
   }

   @Override
   public void lifestealutils$setRare(boolean rare) {
      this.lifestealutils$isRare = rare;
   }
}
