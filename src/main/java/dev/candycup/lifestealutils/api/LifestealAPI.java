package dev.candycup.lifestealutils.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LifestealAPI {
   /**
    * gets the name of the current shard/lobby the player is in.
    * example: "lifesteal-spawn-79dll"
    *
    * @return the shard name, or null if not on lifesteal or unable to parse
    */
   public static String getCurrentShard() {
      return TablistDataController.currentShard;
   }

   /**
    * gets the current network playercount, as reported by LSN
    *
    * @return the network playercount, or 0 if not on lifesteal or unable to parse
    */
   public static int getNetworkPlayerCount() {
      return TablistDataController.currentPlayerCount;
   }

   /**
    * retrieves the name of the artifact attached to the itemstack, if applicable.
    *
    * @param stack the itemstack to check for an artifact
    * @return the name of the attached artifact, or null if no artifact is attached
    */
   @Nullable
   public static String getArtifactNameFromStack(ItemStack stack) {
      return ArtifactParsingUtils.getArtifactName(stack);
   }

   /**
    * checks if the given itemstack has an artifact attached.
    *
    * @param stack the itemstack to check for an artifact
    * @return true if the stack has an artifact attached, false otherwise
    */
   public static boolean isArtifact(ItemStack stack) {
      return ArtifactParsingUtils.hasArtifact(stack);
   }

   /**
    * checks if the player is currently anywhere on the Lifesteal Network
    *
    * @return true if the player is literally anywhere under lifesteal.net.
    */
   public static boolean isOnLifestealNetwork() {
      return LifestealServerDetector.isOnLifestealServer();
   }

   /**
    * checks if the given itemstack has any custom enchants on it.
    *
    * @param stack the itemstack to check for custom enchants
    * @return true if the stack has any custom enchants, false otherwise
    */
   public static boolean hasAnyCustomEnchants(ItemStack stack) {
      return CustomEnchantParsingUtilities.hasCustomEnchants(stack);
   }

   /**
    * checks if the given itemstack has a specific custom enchant on it.
    *
    * @param stack      the itemstack to check for the custom enchant
    * @param enchantKey the key of the custom enchant to check for, e.g. "enchants:lifesteal_soulbound"
    * @return true if the stack has the specified custom enchant, false otherwise
    */
   public static boolean hasSpecificCustomEnchant(ItemStack stack, String enchantKey) {
      return CustomEnchantParsingUtilities.hasCustomEnchant(stack, enchantKey);
   }

   /**
    * retrieves all custom enchants from the given itemstack as a CompoundTag.
    *
    * @param stack the itemstack to retrieve custom enchants from
    * @return a CompoundTag containing all custom enchants on the stack, or null if no custom enchants are present
    */
   public static CompoundTag getCustomEnchants(ItemStack stack) {
      return CustomEnchantParsingUtilities.getCustomEnchantsFrom(stack);
   }

   /**
    * gets the active user's coin balance as reported by the sidebar, if available.
    *
    * @return the user's coin balance
    */
   public static int getUserCoinBalance() {
      return SidebarInfoUtils.coinBalance;
   }

   /**
    * gets the active user's gem balance as reported by the sidebar, if available.
    *
    * @return the user's gem balance
    */
   public static int getUserGemBalance() {
      return SidebarInfoUtils.gemBalance;
   }

   /**
    * gets the active user's kill count as reported by the sidebar, if available.
    *
    * @return the user's kill count
    */
   public static int getUserKillCount() {
      return SidebarInfoUtils.kills;
   }
}
