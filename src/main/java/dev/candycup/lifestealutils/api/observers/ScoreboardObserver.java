package dev.candycup.lifestealutils.api.observers;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import net.minecraft.network.protocol.game.*;

public class ScoreboardObserver {
   public ScoreboardObserver() {
      LifestealUtilsEvents.PACKET_RECEIVED.register((packet, ci) -> {
         if (!LifestealAPI.isOnLifestealNetwork()) return;

         if (packet instanceof ClientboundSetPlayerTeamPacket teamPacket) {
            // LSN prefixes scoreboard objective team names with a §
            if (teamPacket.getName().startsWith("§")) {
               teamPacket.getParameters().ifPresent(params -> {
                  String value = params.getPlayerPrefix().getString();
                  // value examples:
                  // • Gems: 6,681
                  // • Coins: 13,019
                  // • Kills: 2
               });
            }
            /*
            System.out.println("ClientboundSetPlayerTeamPacket: " + teamPacket.getName() + " " + teamPacket.getPlayers());
            teamPacket.getParameters().ifPresent(params -> {
               System.out.println("\tparams: "
                       + params.getPlayerPrefix().getString() + " "
                       + params.getPlayerSuffix().getString() + " "
                       + params.getDisplayName().getString()
               );
            });
             */
         }
      });
   }
}
