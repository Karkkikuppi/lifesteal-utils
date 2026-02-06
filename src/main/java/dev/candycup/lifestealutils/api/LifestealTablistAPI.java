package dev.candycup.lifestealutils.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * read-only API for accessing lifesteal server information.
 * provides utilities for extracting data from the tab list footer.
 */
public final class LifestealTablistAPI {
   private static final Pattern FOOTER_PATTERN = Pattern.compile("Online:\\s*(\\d+)\\s*\\|\\s*([a-zA-Z0-9-]+)");

   private static String currentShard = null;
   private static int currentPlayerCount = 0;
   private static Component lastFooter = null;

   private LifestealTablistAPI() {
   }

   /**
    * updates the internal state from the tab footer component.
    * this should be called whenever the tab footer changes.
    *
    * @param footer the tab footer component, or null if no footer
    */
   public static void updateFromFooter(@Nullable Component footer) {
      lastFooter = footer;

      if (footer == null) {
         currentShard = null;
         currentPlayerCount = 0;
         return;
      }

      String footerText = footer.getString();
      Matcher matcher = FOOTER_PATTERN.matcher(footerText);

      if (matcher.find()) {
         String newShard = matcher.group(2);
         int newPlayerCount = Integer.parseInt(matcher.group(1));

         // check if shard changed
         boolean shardChanged = !newShard.equals(currentShard);

         currentShard = newShard;
         currentPlayerCount = newPlayerCount;

         if (shardChanged && currentShard != null) {
            // fire shard swap event
            dev.candycup.lifestealutils.event.EventBus.getInstance()
                    .post(new dev.candycup.lifestealutils.event.events.LifestealShardSwapEvent(currentShard));
         }
      }
   }

   /**
    * gets the name of the current shard/lobby the player is in.
    * example: "lifesteal-spawn-79dll"
    *
    * @return the shard name, or null if not on lifesteal or unable to parse
    */
   @Nullable
   public static String getCurrentShard() {
      return currentShard;
   }

   /**
    * gets the total player count visible in the tab footer.
    *
    * @return the player count, or 0 if not available
    */
   public static int getCurrentPlayerCount() {
      return currentPlayerCount;
   }

   /**
    * checks if we are currently tracking a shard.
    *
    * @return true if we have parsed valid shard information
    */
   public static boolean hasShardInfo() {
      return currentShard != null;
   }

   /**
    * resets all tracked data. should be called when disconnecting from server.
    */
   public static void reset() {
      currentShard = null;
      currentPlayerCount = 0;
      lastFooter = null;
   }
}
