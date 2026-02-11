package dev.candycup.lifestealutils.features.qol;

import dev.candycup.lifestealutils.api.LifestealServerDetector;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.CommandSentEvent;
import dev.candycup.lifestealutils.event.listener.CommandEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// It tracks shard swaps (hub- and  lifesteal-) when /hub, /safelogout or /lobby are getting used
 
public class ManualShardSwapTracker implements CommandEventListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/manualswap");
   private static final long TRACKING_WINDOW_MS = 60000;

   private long lastManualSwapTime = 0;

   @Override
   public boolean isEnabled() {
      return true;
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   @Override
   public void onCommandSent(CommandSentEvent event) {
      if (!LifestealServerDetector.isOnLifestealServer()) {
         return;
      }

      String command = event.getCommand().toLowerCase();
      if (command.equals("hub") || command.equals("safelogout") || command.equals("lobby")) {
         lastManualSwapTime = System.currentTimeMillis();
         LOGGER.debug("[lsu-manualswap] tracked manual command: /{}", command);
      }
   }

   public boolean wasRecentManualSwap() {
      long timeSinceSwap = System.currentTimeMillis() - lastManualSwapTime;
      boolean isRecent = timeSinceSwap <= TRACKING_WINDOW_MS;
      
      if (isRecent) {
         LOGGER.debug("[lsu-manualswap] recent manual swap detected ({}ms ago)", timeSinceSwap);
      }
      
      return isRecent;
   }

   public void resetTracking() {
      lastManualSwapTime = 0;
      LOGGER.debug("[lsu-manualswap] reset tracking");
   }

   public void clearOnDisconnect() {
      lastManualSwapTime = 0;
      LOGGER.debug("[lsu-manualswap] cleared on disconnect");
   }
}
