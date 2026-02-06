package dev.candycup.lifestealutils;

import net.minecraft.world.item.ItemStack;

public interface ItemClusterRenderStateDuck {
   boolean lifestealutils$isRare();

   void lifestealutils$setRare(boolean rare);

   ItemStack lifestealutils$getItemStack();

   void lifestealutils$setItemStack(ItemStack stack);
}