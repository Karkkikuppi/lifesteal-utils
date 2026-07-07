package dev.candycup.lifestealutils.features.qol;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientTickEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.LifestealShardSwapEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * automatically joins the Lifesteal gamemode when connecting to a hub shard.
 * triggered by detecting shard names starting with 'hub-' via the lifesteal API.
 */
public class AutoJoinLifesteal {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/autojoin");
   private static final int HUB_CHECK_INTERVAL = 100; // check every 5 seconds
   private static final int JOIN_COOLDOWN = 100; // 5 second cooldown after executing command

   private int pendingJoinTicks = -1;
   private int hubCheckTicks = 0;
   private int joinCooldownTicks = 0;
   private String previousShard = null;
   private boolean wasConnected = false;
   private int disconnectTicks = 0;
   private static final int DISCONNECT_THRESHOLD_TICKS = 100;
   private boolean shouldAutoRejoin = false;
   private boolean pendingManualHubCommand = false;
   private boolean manualHubSwapActive = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Automatically join the Lifesteal gamemode when connecting to the lifesteal.net hub")
   @ConfigurableBoolean(location = "qol.autojoin.autojoinlifestealonhub")
   private static boolean autoJoinLifestealOnHub = false;

   public AutoJoinLifesteal() {

      LifestealUtilsEvents.SHARD_SWAP.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onShardSwap(event);
      });
      LifestealUtilsEvents.CLIENT_TICK.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onClientTick(event);
      });
      LifestealUtilsEvents.SERVER_CHANGE.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onServerChange(event);
      });
      LifestealUtilsEvents.COMMAND_SENT.register(command -> {
         if (!isEnabled()) {
            return;
         }
         onCommandSent(command);
      });
   }

   public boolean isEnabled() {
      return autoJoinLifestealOnHub;
   }

   // tracks server changes
   public void onServerChange(LifestealUtilsEvents.ServerChangeEvent event) {
      previousShard = null;
      pendingJoinTicks = -1;
      wasConnected = false;
      disconnectTicks = 0;
      shouldAutoRejoin = false;
      pendingManualHubCommand = false;
      manualHubSwapActive = false;
   }

   public void onCommandSent(String command) {
      if (!LifestealAPI.isOnLifestealNetwork() || command == null) {
         return;
      }

      String normalized = command.trim().toLowerCase();
      if (normalized.startsWith("/")) {
         normalized = normalized.substring(1).trim();
      }

      int firstSpaceIndex = normalized.indexOf(' ');
      String commandRoot = firstSpaceIndex >= 0 ? normalized.substring(0, firstSpaceIndex) : normalized;
      if (commandRoot.equals("hub") || commandRoot.equals("lobby") || commandRoot.equals("safelogout")) {
         pendingManualHubCommand = true;
      }
   }

   public void onShardSwap(LifestealShardSwapEvent event) {
      String shardName = event.getShardName();
      if (shardName == null || shardName.isBlank()) {
         previousShard = null;
         return;
      }

      String fromShard = event.getFromShard();
      boolean isHubShard = isHubShard(shardName);
      boolean wasHubShard = isHubShard(fromShard);

      if (pendingManualHubCommand && !wasHubShard && isHubShard) {
         manualHubSwapActive = true;
         pendingManualHubCommand = false;
         shouldAutoRejoin = false;
         pendingJoinTicks = -1;
         previousShard = shardName;
         LOGGER.debug("[lsu-autojoin] detected manual transfer to hub shard '{}', skipping auto-rejoin", shardName);
         return;
      }

      if (!isHubShard) {
         pendingManualHubCommand = false;
         manualHubSwapActive = false;
      }

      // if the player is in a lifesteal- shard  it resets the tracking 
      if (shardName.startsWith("lifesteal-")) {
         shouldAutoRejoin = false;
         previousShard = shardName;
         return;
      }

      // checks if you joined the first time or if you were on a lifesteal- shard before
      if (shardName.startsWith("hub-")) {
         boolean wasOnLifesteal = previousShard != null && previousShard.startsWith("lifesteal-");
         boolean isFirstJoin = previousShard == null;

         if (wasOnLifesteal) {
            if (manualHubSwapActive) {
               LOGGER.debug("[lsu-autojoin] detected manual swap to hub shard '{}', skipping auto-rejoin", shardName);
               shouldAutoRejoin = false;
               previousShard = shardName;
               return;
            }

            shouldAutoRejoin = true;
            pendingJoinTicks = 20;
         } else if (isFirstJoin) {
            shouldAutoRejoin = true;
            pendingJoinTicks = 20;
         } else {
         }
      }

      previousShard = shardName;
   }

   public void onClientTick(ClientTickEvent event) {
      Minecraft client = Minecraft.getInstance();

      if (client.player == null) {
         if (wasConnected) {
            disconnectTicks++;

            if (disconnectTicks >= DISCONNECT_THRESHOLD_TICKS) {
               previousShard = null;
               pendingJoinTicks = -1;
               shouldAutoRejoin = false;
               pendingManualHubCommand = false;
               manualHubSwapActive = false;
               wasConnected = false;
               disconnectTicks = 0;
            }
         }
         return;
      }

      disconnectTicks = 0;

      if (!wasConnected) {
         wasConnected = true;
      }

      // decrement cooldown
      if (joinCooldownTicks > 0) {
         joinCooldownTicks--;
      }

      if (pendingJoinTicks < 0) {
         // no pending join, perform periodic hub check
         hubCheckTicks++;
         if (hubCheckTicks >= HUB_CHECK_INTERVAL) {
            hubCheckTicks = 0;
            performHubCheck();
         }
         return;
      }

      if (pendingJoinTicks == 0) {
         executeJoinCommand();
         pendingJoinTicks = -1;
      } else {
         pendingJoinTicks--;
      }
   }

   private void performHubCheck() {
      if (joinCooldownTicks > 0) {
         return;
      }

      if (manualHubSwapActive) {
         LOGGER.debug("[lsu-autojoin] failsafe: skipping hub check due to recent manual swap");
         shouldAutoRejoin = false;
         return;
      }

      String currentShard = LifestealAPI.getCurrentShard();

      if (currentShard != null && currentShard.startsWith("hub-")) {
         if (shouldAutoRejoin) {
            LOGGER.debug("[lsu-autojoin] periodic retry: still in hub shard '{}', executing /joinlifesteal", currentShard);
            executeJoinCommand();
         }
      }
   }

   private void executeJoinCommand() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         client.player.connection.sendCommand("joinlifesteal");
          MessagingUtils.showTranslated("lsu.autojoin.forwarding");
         LOGGER.info("[lsu-autojoin] executed /joinlifesteal command");
         joinCooldownTicks = JOIN_COOLDOWN;
      }
   }

   private static boolean isHubShard(String shardName) {
      return shardName != null && shardName.startsWith("hub");
   }
}
