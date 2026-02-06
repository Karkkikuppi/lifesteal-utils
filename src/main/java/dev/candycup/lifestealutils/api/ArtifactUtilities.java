package dev.candycup.lifestealutils.api;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class ArtifactUtilities {
   private ArtifactUtilities() {
   }

   public static String getArtifactName(ItemStack stack) {
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

      Tag artifactTag = pbv.get().get("lifesteal:artifact");
      if (!(artifactTag instanceof StringTag(String value))) {
         return null;
      }

      return value != null && !value.isBlank() ? value : null;
   }

   public static boolean hasArtifact(ItemStack stack) {
      return getArtifactName(stack) != null;
   }

   private static CompoundTag encodeStack(ItemStack stack, DynamicOps<Tag> ops) {
      DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(ops, stack.getComponentsPatch());
      Tag nbtElement = result.getOrThrow();
      return (CompoundTag) nbtElement;
   }
}
