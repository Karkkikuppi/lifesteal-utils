package dev.candycup.lifestealutils.gaia.gateway;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.candycup.lifestealutils.features.alliances.AllianceSyncManager;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handles incoming messages from the Gaia Gateway websocket.
 * parses JSON messages, fires events to LifestealUtilsEvents, and displays alliance event messages in chat.
 */
public class GatewayMessageHandler {
   private static final Logger LOGGER = LoggerFactory.getLogger(GatewayMessageHandler.class);
   private static final String GATEWAY_COLOR = "#5DADE2";

   private final GaiaGatewayClient gatewayClient;

   public GatewayMessageHandler(GaiaGatewayClient gatewayClient) {
      this.gatewayClient = gatewayClient;
   }

   /**
    * processes a text message received from the websocket.
    *
    * @param message the raw JSON message text
    */
   public void handleMessage(String message) {
      try {
         JsonObject json = JsonParser.parseString(message).getAsJsonObject();

         if (!json.has("op")) {
            LOGGER.warn("Received gateway message without 'op' field: {}", message);
            return;
         }

         String op = json.get("op").getAsString();

         switch (op) {
            case "ready" ->
                    handleReady(json.has("data") && json.get("data").isJsonObject() ? json.getAsJsonObject("data") : new JsonObject());
            case "event" -> handleEvent(json);
            case "pong" ->
                    handlePong(json.has("data") && json.get("data").isJsonObject() ? json.getAsJsonObject("data") : new JsonObject());
            case "error" ->
                    handleError(json.has("error") && json.get("error").isJsonObject() ? json.getAsJsonObject("error") : new JsonObject());
            default -> LOGGER.warn("Unknown gateway opcode received: {}", op);
         }
      } catch (Exception e) {
         LOGGER.error("Failed to parse gateway message: {}", message, e);
      }
   }

   /**
    * handles the 'ready' opcode - sent when websocket connection is established.
    */
   private void handleReady(JsonObject data) {
      LOGGER.info("Gateway ready: {}", data);

      gatewayClient.onReady();

      AllianceSyncManager.syncSubscriptionsAsync();

      if (data.has("user") && data.get("user").isJsonObject()) {
         JsonObject user = data.getAsJsonObject("user");
         if (user.has("name") && !user.get("name").isJsonNull()) {
            Component msg = Component.translatable("lsu.gaia.gateway.connected");
            MessagingUtils.showMiniMessage(
                    String.format("<color:%s><bold>[LSU]</bold> %s</color>",
                            GATEWAY_COLOR,
                            msg.getString())
            );
         }
      }
   }

   /**
    * handles the 'event' opcode - real-time events from the gateway.
    */
   private void handleEvent(JsonObject json) {
      if (!json.has("type") || json.get("type").isJsonNull()) {
         LOGGER.warn("Received gateway event without 'type' field: {}", json);
         return;
      }
      String type = json.get("type").getAsString();
      JsonObject data = json.has("data") && json.get("data").isJsonObject()
              ? json.getAsJsonObject("data")
              : new JsonObject();

      LOGGER.debug("Received gateway event: {} - {}", type, data);

      LifestealUtilsEvents.GATEWAY_MESSAGE.invoker().onGatewayMessage(new LifestealUtilsEvents.GatewayMessageEvent(type, data));

      displayAllianceEvent(type, data);
   }

   /**
    * displays alliance-related events as chat messages.
    */
   private void displayAllianceEvent(String type, JsonObject data) {
      try {
         String allianceName = data.has("allianceName") && !data.get("allianceName").isJsonNull()
                 ? data.get("allianceName").getAsString() : "Unknown";
         String username = data.has("username") && !data.get("username").isJsonNull()
                 ? data.get("username").getAsString() : "Unknown";

         String messageKey = switch (type) {
            case "alliance.invite" -> "lsu.gaia.gateway.event.alliance.invite";
            case "alliance.join" -> "lsu.gaia.gateway.event.alliance.join";
            case "alliance.leave" -> "lsu.gaia.gateway.event.alliance.leave";
            case "alliance.kick" -> "lsu.gaia.gateway.event.alliance.kick";
            default -> null;
         };

         if (messageKey != null) {
            Component msg = Component.translatable(messageKey, allianceName, username);
            MessagingUtils.showMiniMessage(
                    String.format("<color:%s><bold>[LSU]</bold> %s</color>",
                            GATEWAY_COLOR,
                            msg.getString())
            );
         }

         if (type.equals("alliance.updated")
                 || type.equals("alliance.subscription.revoked")
                 || type.equals("alliance.deleted")) {
            data.addProperty("eventType", type);
            AllianceSyncManager.applyGatewayUpdate(data);
         }
      } catch (Exception e) {
         LOGGER.error("Failed to display alliance event: {} - {}", type, data, e);
      }
   }

   /**
    * handles the 'pong' opcode - response to ping keep-alive.
    */
   private void handlePong(JsonObject data) {
      LOGGER.debug("Received pong from gateway: {}", data);
   }

   /**
    * handles the 'error' opcode - error messages from the gateway.
    */
   private void handleError(JsonObject error) {
      String code = error.has("code") && !error.get("code").isJsonNull() ? error.get("code").getAsString() : "UNKNOWN";
      String message = error.has("message") && !error.get("message").isJsonNull() ? error.get("message").getAsString() : "Unknown error";

      LOGGER.error("Gateway error: {} - {}", code, message);

      LifestealUtilsEvents.GATEWAY_ERROR.invoker().onGatewayError(new LifestealUtilsEvents.GatewayErrorEvent(code, message));

      Component msg = Component.translatable("lsu.gaia.gateway.error", message);
      MessagingUtils.showMiniMessage(
              String.format("<color:%s><bold>[LSU]</bold> <red>%s</red></color>",
                      GATEWAY_COLOR,
                      msg.getString())
      );
   }
}
