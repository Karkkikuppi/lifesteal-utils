package dev.candycup.lifestealutils.gaia.gateway;

/**
 * represents the current state of the Gaia Gateway websocket connection.
 */
public enum GatewayConnectionState {
   /**
    * websocket is not connected and not attempting to connect.
    */
   DISCONNECTED,

   /**
    * websocket is currently attempting to establish a connection.
    */
   CONNECTING,

   /**
    * websocket is successfully connected and active.
    */
   CONNECTED,

   /**
    * websocket lost connection and is attempting to reconnect with backoff.
    */
   RECONNECTING
}
