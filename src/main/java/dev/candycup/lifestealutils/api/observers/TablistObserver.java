package dev.candycup.lifestealutils.api.observers;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.TablistDataController;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;

public class TablistObserver {
   public TablistObserver() {
      LifestealUtilsEvents.PACKET_RECEIVED.register((packet, ci) -> {
         if (packet instanceof ClientboundTabListPacket tabListPacket) {
            if (LifestealAPI.isOnLifestealNetwork()) {
               TablistDataController.updateFromFooter(tabListPacket.footer());
            } else {
               TablistDataController.reset();
            }
         }
      });
   }
}
