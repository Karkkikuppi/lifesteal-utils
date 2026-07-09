package dev.candycup.ui.lsu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record SlotSnapshot(
        int slotIndex,
        ItemStack stack,
        Component displayName,
        String plainName,
        List<Component> lore,
        boolean empty,
        boolean grayStainedGlassPane,
        boolean barrier,
        boolean redstoneTorch
) {
   public SlotSnapshot visualCopy() {
      return new SlotSnapshot(
              slotIndex,
              stack == null ? null : stack.copy(),
              displayName,
              plainName,
              List.copyOf(lore),
              empty,
              grayStainedGlassPane,
              barrier,
              redstoneTorch
      );
   }
}
