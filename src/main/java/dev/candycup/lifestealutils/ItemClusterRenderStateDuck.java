package dev.candycup.lifestealutils;

import net.minecraft.world.item.ItemStack;

public interface ItemClusterRenderStateDuck {
   /**
 * Indicates whether the associated item cluster render state is flagged as rare.
 *
 * @return `true` if the associated item cluster render state is rare, `false` otherwise.
 */
boolean lifestealutils$isRare();

   /**
 * Sets whether the associated item cluster render state is considered rare.
 *
 * @param rare true to mark the render state as rare, false to mark it as not rare
 */
void lifestealutils$setRare(boolean rare);

   /**
 * Retrieves the ItemStack associated with the item cluster render state.
 *
 * @return the associated ItemStack
 */
ItemStack lifestealutils$getItemStack();

   /**
 * Sets the ItemStack associated with this item cluster render state.
 *
 * @param stack the ItemStack to associate with the render state
 */
void lifestealutils$setItemStack(ItemStack stack);
}