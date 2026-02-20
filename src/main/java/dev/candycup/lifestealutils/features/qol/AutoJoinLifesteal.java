package dev.candycup.lifestealutils.features.qol;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealTablistAPI;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.LifestealShardSwapEvent;
import dev.candycup.lifestealutils.event.events.ServerChangeEvent;
import dev.candycup.lifestealutils.event.events.ClientTickEvent;
import dev.candycup.lifestealutils.event.listener.ServerEventListener;
import dev.candycup.lifestealutils.event.listener.TickEventListener;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.TablistDataController;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientTickEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.LifestealShardSwapEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
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

   private final ManualShardSwapTracker manualSwapTracker;
   private int pendingJoinTicks = -1;
   private int hubCheckTicks = 0;
   private int joinCooldownTicks = 0;
   private String previousShard = null;
   private boolean wasConnected = false;
   private int disconnectTicks = 0;
   private static final int DISCONNECT_THRESHOLD_TICKS = 100;
   private boolean shouldAutoRejoin = false;


   
   public AutoJoinLifesteal(ManualShardSwapTracker manualSwapTracker) {
      this.manualSwapTracker = manualSwapTracker;
   }

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
   }

   public boolean isEnabled() {
      return Config.isAutoJoinLifestealOnHub();
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   // tracks server changes
   @Override
   public void onServerChange(ServerChangeEvent event) {
      previousShard = null;
      pendingJoinTicks = -1;
      wasConnected = false;
      disconnectTicks = 0;
      shouldAutoRejoin = false;
      manualSwapTracker.resetTracking();
   }

   // it tracks if the shard gets changed
   @Override
   public void onShardSwap(LifestealShardSwapEvent event) {
      String shardName = event.getShardName();
      if (shardName == null || shardName.isBlank()) {
         previousShard = null;
         return;
      }

      // if the player is in a lifesteal- shard  it resets the tracking 
      if (shardName.startsWith("lifesteal-")) {
         shouldAutoRejoin = false;
         manualSwapTracker.resetTracking();
         LOGGER.debug("[lsu-autojoin] returned to lifesteal shard '{}', reset manual swap tracking", shardName);
         previousShard = shardName;
         return;
      }

      // checks if you joined the first time or if you were on a lifesteal- shard before
      if (shardName.startsWith("hub-")) {
         boolean wasOnLifesteal = previousShard != null && previousShard.startsWith("lifesteal-");
         boolean isFirstJoin = previousShard == null;
         
         if (wasOnLifesteal) {
            if (manualSwapTracker.wasRecentManualSwap()) {
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
               manualSwapTracker.clearOnDisconnect();
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

      if (manualSwapTracker.wasRecentManualSwap()) {
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
         MessagingUtils.showMiniMessage("<gray><italic>[Lifesteal Utils Join Macro] Forwarding you to the lifesteal gamemode... this can be disabled in /lsu config!</italic></gray>");
         LOGGER.info("[lsu-autojoin] executed /joinlifesteal command");
         joinCooldownTicks = JOIN_COOLDOWN;
      }
   }
}
