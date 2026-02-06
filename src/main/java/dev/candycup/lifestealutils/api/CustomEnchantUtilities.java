package dev.candycup.lifestealutils.api;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class CustomEnchantUtilities {
   private CustomEnchantUtilities() {
   }

   public static CompoundTag getCustomEnchantsFrom(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return null;
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null) {
         return null;
      }

      Tag tag = encodeStack(stack, minecraft.player.registryAccess().createSerializationContext(NbtOps.INSTANCE));
      if (!(tag instanceof CompoundTag nbt)) {
         return null;
      }

      Optional<CompoundTag> customData = nbt.getCompound("minecraft:custom_data");
      if (customData.isEmpty()) {
         return null;
      }

      Optional<CompoundTag> pbv = customData.get().getCompound("PublicBukkitValues");
      if (pbv.isEmpty()) {
         return null;
      }

      CompoundTag enchants = new CompoundTag();
      for (String key : pbv.get().keySet()) {
         if (key.startsWith("enchants:")) {
            Tag value = pbv.get().get(key);
            if (value != null) {
               enchants.put(key, value.copy());
            }
         }
      }

      return enchants.isEmpty() ? null : enchants;
   }

   public static boolean hasCustomEnchants(ItemStack stack) {
      return getCustomEnchantsFrom(stack) != null;
   }

   public static boolean hasCustomEnchant(ItemStack stack, String key) {
      CompoundTag enchants = getCustomEnchantsFrom(stack);
      if (enchants == null || key == null || key.isBlank()) {
         return false;
      }
      return enchants.contains(key);
   }

   private static CompoundTag encodeStack(ItemStack stack, DynamicOps<Tag> ops) {
      DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(ops, stack.getComponentsPatch());
      Tag nbtElement = result.getOrThrow();
      return (CompoundTag) nbtElement;
   }
}
