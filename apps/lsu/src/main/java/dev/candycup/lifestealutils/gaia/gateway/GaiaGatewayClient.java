package dev.candycup.lifestealutils.gaia.gateway;

import com.google.gson.JsonObject;
import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ServerChangeEvent;
import dev.candycup.lifestealutils.gaia.GaiaApiClient;
import dev.candycup.lifestealutils.gaia.GaiaAuthClient;
import dev.candycup.lifestealutils.gaia.GaiaAuthTokenStore;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * manages the websocket connection to the Gaia Gateway.
 * automatically connects when joining lifesteal.net and disconnects when leaving.
 * implements exponential backoff reconnection strategy (5s, 10s, 20s, max 60s).
 */
public class GaiaGatewayClient {
   private static final Logger LOGGER = LoggerFactory.getLogger(GaiaGatewayClient.class);
   private static final String GATEWAY_COLOR = "#5DADE2";

   // reconnection backoff settings
   private static final int INITIAL_BACKOFF_SECONDS = 5;
   private static final int MAX_BACKOFF_SECONDS = 60;
   private static final int PING_INTERVAL_SECONDS = 30;

   private WebSocket websocket;
   private GatewayConnectionState state = GatewayConnectionState.DISCONNECTED;
   private final HttpClient httpClient;
   private final ScheduledExecutorService scheduler;
   private final GatewayMessageHandler messageHandler;

   private int currentBackoffSeconds = INITIAL_BACKOFF_SECONDS;
   private int ticksSinceLastPing = 0;
   private String currentUsername;
   private String currentUuid;

   public GaiaGatewayClient() {
      this.httpClient = HttpClient.newHttpClient();
      this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread thread = new Thread(r, "GaiaGateway-Scheduler");
         thread.setDaemon(true);
         return thread;
      });
      this.messageHandler = new GatewayMessageHandler(this);

      LifestealUtilsEvents.SERVER_CHANGE.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onServerChange(event);
      });
   }

   public boolean isEnabled() {
      return Config.isGaiaAdvancedFeaturesEnabled();
   }

   public void onServerChange(ServerChangeEvent event) {
      LOGGER.info("ServerChangeEvent received: type={}, server={}, enabled={}, isLifesteal={}",
              event.getType(),
              event.getServerAddress(),
              Config.isGaiaAdvancedFeaturesEnabled(),
              LifestealAPI.isOnLifestealNetwork());

      if (event.isConnected() && LifestealAPI.isOnLifestealNetwork()) {
         LOGGER.info("Connecting to Gaia Gateway (joined lifesteal.net)");
         connect();
      } else if (event.isDisconnected()) {
         LOGGER.info("Disconnecting from Gaia Gateway (left server)");
         disconnect(false);
      }
   }

   /**
    * initiates connection to the Gaia Gateway websocket.
    */
   private void connect() {
      if (!isEnabled()) {
         return;
      }

      if (state == GatewayConnectionState.CONNECTED || state == GatewayConnectionState.CONNECTING) {
         LOGGER.debug("Already connected or connecting to gateway");
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null) {
         LOGGER.warn("Cannot connect to gateway: player is null");
         return;
      }

      currentUsername = minecraft.player.getName().getString();
      currentUuid = minecraft.player.getUUID().toString();

      UUID playerId = minecraft.player.getUUID();

      // validate cached token before use - avoids infinite reconnect loop with expired tokens
      if (!GaiaAuthTokenStore.hasValidToken(currentUsername, playerId)) {
         LOGGER.warn("No valid Gaia auth token, attempting to authenticate...");
         authenticateAndConnect(currentUsername, playerId);
         return;
      }

      String token = GaiaAuthTokenStore.readToken(currentUsername);
      connectWithToken(token);
   }

   /**
    * authenticates with Gaia and then connects to the gateway.
    * username and uuid are captured before the async call to avoid a race where
    * a subsequent connect() overwrites the instance fields before this completes.
    */
   private void authenticateAndConnect(String username, UUID playerId) {
      GaiaAuthClient.confirmHandshakeOnStartup(username, playerId)
              .thenAccept(success -> {
                 if (success) {
                    LOGGER.info("Authentication successful, connecting to gateway");
                    String token = GaiaAuthTokenStore.readToken(username);
                    if (token != null) {
                       connectWithToken(token);
                    } else {
                       LOGGER.error("Authentication succeeded but token not found for {}", username);
                    }
                 } else {
                    LOGGER.error("Failed to authenticate with Gaia, cannot connect to gateway");
                    LifestealUtilsEvents.GATEWAY_ERROR.invoker().onGatewayError(
                            new LifestealUtilsEvents.GatewayErrorEvent("AUTH_FAILED", "Failed to authenticate with Gaia")
                    );
                 }
              });
   }

   /**
    * establishes websocket connection using the provided JWT token.
    */
   private void connectWithToken(String token) {
      try {
         state = GatewayConnectionState.CONNECTING;

         String gatewayBase = GaiaApiClient.GAIA_GATEWAY_ROOT + "/v1/gateway/connect";
         LOGGER.info("Connecting to Gaia Gateway: {}", gatewayBase);

         GaiaApiClient.getInstance().gateway().connectWithToken(httpClient, new GatewayWebSocketListener(), token)
                 .thenAccept(ws -> {
                    this.websocket = ws;
                    // state will be set to CONNECTED when we receive the "ready" message
                    LOGGER.debug("WebSocket opened, waiting for ready message");
                 })
                 .exceptionally(e -> {
                    LOGGER.error("Failed to connect to Gaia Gateway", e);
                    state = GatewayConnectionState.DISCONNECTED;
                    scheduleReconnect();
                    return null;
                 });
      } catch (Exception e) {
         LOGGER.error("Exception while connecting to gateway", e);
         state = GatewayConnectionState.DISCONNECTED;
         scheduleReconnect();
      }
   }

   /**
    * called by GatewayMessageHandler when the "ready" message is received.
    * marks the connection as fully established.
    */
   public void onReady() {
      this.state = GatewayConnectionState.CONNECTED;
      this.currentBackoffSeconds = INITIAL_BACKOFF_SECONDS; // reset backoff on successful connection
      this.ticksSinceLastPing = 0; // reset ping timer

      LOGGER.info("Gateway ready - connection fully established");
      LifestealUtilsEvents.GATEWAY_CONNECTED.invoker().onGatewayConnected(
              new LifestealUtilsEvents.GatewayConnectedEvent(currentUsername, currentUuid)
      );
   }

   /**
    * schedules a reconnection attempt with exponential backoff.
    */
   private void scheduleReconnect() {
      if (!isEnabled() || !LifestealAPI.isOnLifestealNetwork()) {
         LOGGER.debug("Not scheduling reconnect (disabled or not on lifesteal.net)");
         return;
      }

      state = GatewayConnectionState.RECONNECTING;

      LOGGER.info("Scheduling reconnect in {} seconds", currentBackoffSeconds);

      scheduler.schedule(() -> {
         LOGGER.info("Attempting to reconnect to gateway...");
         connect();
      }, currentBackoffSeconds, TimeUnit.SECONDS);

      // exponential backoff: double the wait time, cap at MAX_BACKOFF_SECONDS
      currentBackoffSeconds = Math.min(currentBackoffSeconds * 2, MAX_BACKOFF_SECONDS);
   }

   /**
    * disconnects from the Gaia Gateway websocket.
    *
    * @param reconnect whether to schedule a reconnection attempt
    */
   private void disconnect(boolean reconnect) {
      if (websocket != null) {
         try {
            websocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
            websocket = null;
         } catch (Exception e) {
            LOGGER.error("Error while closing websocket", e);
         }
      }

      GatewayConnectionState previousState = state;
      state = GatewayConnectionState.DISCONNECTED;
      ticksSinceLastPing = 0;

      if (previousState != GatewayConnectionState.DISCONNECTED) {
         LOGGER.info("Disconnected from Gaia Gateway");
         LifestealUtilsEvents.GATEWAY_DISCONNECTED.invoker().onGatewayDisconnected(
                 new LifestealUtilsEvents.GatewayDisconnectedEvent("Disconnected", reconnect)
         );

         // show disconnection message
         Component msg = Component.translatable("lsu.gaia.gateway.disconnected");
         MessagingUtils.showMiniMessage(
                 String.format("<color:%s><bold>[LSU]</bold> %s</color>",
                         GATEWAY_COLOR,
                         msg.getString())
         );
      }

      if (reconnect) {
         scheduleReconnect();
      }
   }

   /**
    * disables the gateway, closing any active connection and cancelling pending reconnects.
    * called when the user opts out of Gaia. Safe to call at any time.
    */
   public void disable() {
      LOGGER.info("Gaia gateway disabled by user");
      if (websocket != null) {
         try {
            websocket.sendClose(WebSocket.NORMAL_CLOSURE, "Gaia disabled by user");
         } catch (Exception e) {
            LOGGER.debug("Exception while closing websocket on disable", e);
         }
         websocket = null;
      }
      state = GatewayConnectionState.DISCONNECTED;
      currentBackoffSeconds = INITIAL_BACKOFF_SECONDS;
      ticksSinceLastPing = 0;
   }

   /**
    * sends a ping message to keep the connection alive.
    * called periodically from the client tick event.
    */
   public void tick() {
      if (state != GatewayConnectionState.CONNECTED || websocket == null) {
         return;
      }

      ticksSinceLastPing++;
      if (ticksSinceLastPing >= PING_INTERVAL_SECONDS * 20) { // 20 ticks per second
         sendPing();
         ticksSinceLastPing = 0;
      }
   }

   /**
    * sends a ping message to the gateway.
    */
   private void sendPing() {
      try {
         JsonObject ping = new JsonObject();
         ping.addProperty("op", "ping");

         String message = ping.toString();
         websocket.sendText(message, true);
         LOGGER.debug("Sent ping to gateway");
      } catch (Exception e) {
         LOGGER.error("Failed to send ping", e);
      }
   }

   /**
    * returns the current connection state.
    */
   public GatewayConnectionState getState() {
      return state;
   }

   /**
    * checks if the gateway is currently connected.
    */
   public boolean isConnected() {
      return state == GatewayConnectionState.CONNECTED;
   }

   /**
    * websocket listener implementation that handles incoming messages and connection events.
    */
   private class GatewayWebSocketListener implements WebSocket.Listener {
      private final StringBuilder messageBuffer = new StringBuilder();

      @Override
      public void onOpen(WebSocket webSocket) {
         LOGGER.debug("WebSocket opened");
         webSocket.request(1);
      }

      @Override
      public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
         messageBuffer.append(data);

         if (last) {
            String message = messageBuffer.toString();
            messageBuffer.setLength(0);

            // process message on main thread to avoid threading issues with Minecraft
            Minecraft.getInstance().execute(() -> messageHandler.handleMessage(message));
         }

         webSocket.request(1);
         return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
         LOGGER.warn("Received unexpected binary message from gateway");
         webSocket.request(1);
         return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
         LOGGER.debug("Received ping from gateway");
         webSocket.request(1);
         return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
         LOGGER.debug("Received pong from gateway");
         webSocket.request(1);
         return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
         LOGGER.info("WebSocket closed: {} - {}", statusCode, reason);

         Minecraft.getInstance().execute(() -> {
            // reconnect if we're still on lifesteal.net and enabled
            boolean shouldReconnect = isEnabled()
                    && LifestealAPI.isOnLifestealNetwork()
                    && statusCode != WebSocket.NORMAL_CLOSURE;

            disconnect(shouldReconnect);
         });

         return CompletableFuture.completedFuture(null);
      }

      @Override
      public void onError(WebSocket webSocket, Throwable error) {
         LOGGER.error("WebSocket error", error);

         Minecraft.getInstance().execute(() -> {
            LifestealUtilsEvents.GATEWAY_ERROR.invoker().onGatewayError(
                    new LifestealUtilsEvents.GatewayErrorEvent("WEBSOCKET_ERROR", error.getMessage(), error)
            );

            // attempt to reconnect on error
            disconnect(true);
         });
      }
   }
}
