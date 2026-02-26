package dev.candycup.lifestealutils.features.alliances;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.LifestealShardSwapEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ServerChangeEvent;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.features.alliances.service.AllianceManagers;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * parses and renders alliance motd messages sent after lobby to shard transfer.
 */
public final class AllianceMotdListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/alliance-motd");
   private static final Pattern CLICK_TAG_PATTERN = Pattern.compile("(?i)</?\\s*click(?:\\s*:[^>]+)?>");

   private String previousShard;
   private String lastMotdShard;

   public AllianceMotdListener() {
      LifestealUtilsEvents.SERVER_CHANGE.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onServerChange(event);
      });
      LifestealUtilsEvents.SHARD_SWAP.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onShardSwap(event);
      });
   }

   public boolean isEnabled() {
      return Config.isEnableAlliances();
   }

   public void onServerChange(ServerChangeEvent event) {
      LOGGER.debug("alliance motd server change: {}", event.getType());

      if (event.getType() == ServerChangeEvent.Type.DISCONNECTED || event.getType() == ServerChangeEvent.Type.CONNECTED) {
         resetSession();
      }
   }

   public void onShardSwap(LifestealShardSwapEvent event) {
      String shard = event.getShardName();
      if (shard == null || shard.isBlank()) {
         return;
      }

      LOGGER.debug("alliance motd shard swap: {} -> {}", previousShard, shard);

      if (isHub(shard)) {
         AllianceNameRenderHandler.refreshPrefixCandidatesNow();
         LOGGER.debug("alliance motd prefetch triggered on hub shard {}", shard);
      }

      boolean firstGameShardJoin = previousShard == null && !isHub(shard);
      boolean hubToGameTransfer = isHub(previousShard) && !isHub(shard);
      if (firstGameShardJoin || hubToGameTransfer) {
         showAllianceMotdsForShard(shard);
      }

      previousShard = shard;
   }

   private static boolean isHub(String shard) {
      return shard != null && shard.startsWith("hub-");
   }

   private void showAllianceMotdsForShard(String shard) {
      if (shard == null || shard.isBlank()) {
         return;
      }

      if (shard.equals(lastMotdShard)) {
         return;
      }

      CompletableFuture<List<Alliance>> future = AllianceManagers.fetchPlayerAlliances();
      future.thenAccept(alliances -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player == null) {
            return;
         }

         if (!shard.equals(previousShard)) {
            return;
         }

         String playerUuid = minecraft.player.getStringUUID();
         List<Alliance> motdAlliances = alliances.stream()
                 .filter(alliance -> alliance != null && alliance.motd() != null && !alliance.motd().isBlank())
                 .filter(alliance -> isJoinedMember(alliance, playerUuid))
                 .toList();

         if (motdAlliances.isEmpty()) {
            return;
         }

         minecraft.execute(() -> {
            if (minecraft.player == null) {
               return;
            }

            if (!shard.equals(previousShard)) {
               return;
            }

            for (Alliance alliance : motdAlliances) {
               MessagingUtils.showMessage(Component.empty(), 0xFFFFFF);
               MessagingUtils.showMiniMessage(
                       "<#ffffff>   <gray>Alliance:</gray> " + escapeMiniMessageText(alliance.getDisplayName()) + "</#ffffff>",
                       0xFFFFFF
               );
               MessagingUtils.showMessage(Component.empty(), 0xFFFFFF);

               String sanitizedMotd = stripClickTags(alliance.motd().trim());
               try {
                  MessagingUtils.showMiniMessage("<#ffffff>   " + sanitizedMotd + "</#ffffff>", 0xFFFFFF);
               } catch (RuntimeException ignored) {
                  MessagingUtils.showMessage(Component.literal("   " + alliance.motd().trim()), 0xFFFFFF);
               }

               MessagingUtils.showMessage(Component.empty(), 0xFFFFFF);
            }

            lastMotdShard = shard;
            LOGGER.debug("alliance motd emitted from alliance data for shard {}", shard);
         });
      }).exceptionally(error -> {
         LOGGER.debug("failed to fetch alliance motds", error);
         return null;
      });
   }

   private static boolean isJoinedMember(Alliance alliance, String playerUuid) {
      String normalizedPlayerUuid = normalizeUuid(playerUuid);
      if (normalizedPlayerUuid.isEmpty()) {
         return false;
      }

      return alliance.members().stream()
              .filter(member -> member != null && member.isJoined())
              .anyMatch(member -> normalizeUuid(member.uuid()).equals(normalizedPlayerUuid));
   }

   private static String normalizeUuid(String uuid) {
      if (uuid == null || uuid.isBlank()) {
         return "";
      }
      return uuid.replace("-", "").toLowerCase(Locale.ROOT);
   }

   private static String stripClickTags(String miniMessage) {
      return CLICK_TAG_PATTERN.matcher(miniMessage).replaceAll("");
   }

   private static String escapeMiniMessageText(String text) {
      if (text == null || text.isEmpty()) {
         return "";
      }
      return text.replace("\\", "\\\\").replace("<", "\\<");
   }

   private void resetSession() {
      LOGGER.debug("alliance motd session reset");
      previousShard = null;
      lastMotdShard = null;
   }
}
