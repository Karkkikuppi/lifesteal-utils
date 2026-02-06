package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.LifestealShardSwapEvent;
import dev.candycup.lifestealutils.event.events.ServerChangeEvent;

/**
 * listener interface for server connection events.
 * override methods to handle server lifecycle.
 */
public interface ServerEventListener extends LifestealEventListener {

   /**
    * called when the player connects to or disconnects from a server.
    * use for feature lifecycle management and state cleanup.
    *
    * @param event the server change event
    */
   default void onServerChange(ServerChangeEvent event) {
   }

   /**
    * called when the player swaps to a different shard/lobby on lifesteal.net.
    *
    * @param event the shard swap event
    */
   default void onShardSwap(LifestealShardSwapEvent event) {
   }
}
