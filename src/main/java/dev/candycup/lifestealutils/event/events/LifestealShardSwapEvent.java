package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;

/**
 * fired when the player swaps to a different shard/lobby on lifesteal.net.
 * <p>
 * this event is triggered by changes in the tab footer which displays
 * the current shard name (e.g., "lifesteal-spawn-79dll").
 */
public class LifestealShardSwapEvent extends LSUEvent {
   private final String shardName;

   public LifestealShardSwapEvent(String shardName) {
      this.shardName = shardName;
   }

   /**
    * gets the name of the new shard the player is now in.
    *
    * @return the shard name (e.g., "lifesteal-spawn-79dll")
    */
   public String getShardName() {
      return shardName;
   }

   @Override
   public boolean isCancellable() {
      return false;
   }
}
