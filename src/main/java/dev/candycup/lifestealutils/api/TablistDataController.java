package dev.candycup.lifestealutils.api;

import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.LifestealShardSwapEvent;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * read-only API for accessing lifesteal server information.
 * provides utilities for extracting data from the tab list footer.
 */
public final class TablistDataController {
   private static final Pattern FOOTER_PATTERN = Pattern.compile("Online:\\s*(\\d+)\\s*\\|\\s*([a-zA-Z0-9-]+)");

   static String currentShard = null;
   static int currentPlayerCount = 0;
   private static Component lastFooter = null;

   private TablistDataController() {
   }

   /**
    * updates the internal state from the tab footer component.
    * this should be called whenever the tab footer changes.
    *
    * @param footer the tab footer component, or null if no footer
    */
   public static void updateFromFooter(@Nullable Component footer) {
      lastFooter = footer;
      String oldShard = currentShard;

      if (footer == null) {
         // some transitions briefly send a null footer before the next shard footer arrives.
         // keep previous shard so fromShard stays accurate for the upcoming swap event.
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

         if (shardChanged) {
            // fire shard swap event
            LifestealUtilsEvents.SHARD_SWAP
                    .invoker()
                    .onShardSwap(new LifestealShardSwapEvent(currentShard, oldShard));
         }
      }
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
