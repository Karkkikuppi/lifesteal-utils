package dev.candycup.lifestealutils.gaia.gateway;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.candycup.lifestealutils.LifestealUtils;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.features.alliances.AllianceNameRenderHandler;
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
   private static final String GATEWAY_COLOR = "#5DADE2"; // light blue/cyan color for gateway messages

   /**
    * processes a text message received from the websocket.
    *
    * @param message the raw JSON message text
    */
   public static void handleMessage(String message) {
      try {
         JsonObject json = JsonParser.parseString(message).getAsJsonObject();

         if (!json.has("op")) {
            LOGGER.warn("Received message without 'op' field: {}", message);
            return;
         }

         String op = json.get("op").getAsString();

         switch (op) {
            case "ready" -> handleReady(json.getAsJsonObject("data"));
            case "event" -> handleEvent(json);
            case "pong" -> handlePong(json.getAsJsonObject("data"));
            case "error" -> handleError(json.getAsJsonObject("error"));
            default -> LOGGER.warn("Unknown opcode received: {}", op);
         }
      } catch (Exception e) {
         LOGGER.error("Failed to parse gateway message: {}", message, e);
      }
   }

   /**
    * handles the 'ready' opcode - sent when websocket connection is established.
    */
   private static void handleReady(JsonObject data) {
      LOGGER.info("Gateway ready: {}", data);

      // notify the gateway client that we're fully connected
      GaiaGatewayClient client = LifestealUtils.getGaiaGatewayClient();
      if (client != null) {
         client.onReady();
      }

      if (data.has("user")) {
         JsonObject user = data.getAsJsonObject("user");
         String username = user.get("name").getAsString();

         // show connection message
         Component msg = Component.translatable("lsu.gaia.gateway.connected");
         MessagingUtils.showMiniMessage(
                 String.format("<color:%s><bold>[LSU]</bold> %s</color>",
                         GATEWAY_COLOR,
                         msg.getString())
         );
      }

      AllianceNameRenderHandler.refreshPrefixCandidatesNow();
   }

   /**
    * handles the 'event' opcode - real-time events from the gateway.
    */
   private static void handleEvent(JsonObject json) {
      String type = json.get("type").getAsString();
      JsonObject data = json.getAsJsonObject("data");

      LOGGER.debug("Received gateway event: {} - {}", type, data);

      // fire event for other systems to handle
      LifestealUtilsEvents.GATEWAY_MESSAGE.invoker().onGatewayMessage(new LifestealUtilsEvents.GatewayMessageEvent(type, data));

      if (type.startsWith("alliance.")) {
         AllianceNameRenderHandler.refreshPrefixCandidatesNow();
      }

      // display alliance events in chat
      displayAllianceEvent(type, data);
   }

   /**
    * displays alliance-related events as chat messages.
    */
   private static void displayAllianceEvent(String type, JsonObject data) {
      try {
         String allianceName = data.has("allianceName") ? data.get("allianceName").getAsString() : "Unknown";
         String username = data.has("username") ? data.get("username").getAsString() : "Unknown";

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
      } catch (Exception e) {
         LOGGER.error("Failed to display alliance event: {} - {}", type, data, e);
      }
   }

   /**
    * handles the 'pong' opcode - response to ping keep-alive.
    */
   private static void handlePong(JsonObject data) {
      LOGGER.debug("Received pong from gateway: {}", data);
      // pong received - connection is alive
   }

   /**
    * handles the 'error' opcode - error messages from the gateway.
    */
   private static void handleError(JsonObject error) {
      String code = error.has("code") ? error.get("code").getAsString() : "UNKNOWN";
      String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";

      LOGGER.error("Gateway error: {} - {}", code, message);

      // fire error event
      LifestealUtilsEvents.GATEWAY_ERROR.invoker().onGatewayError(new LifestealUtilsEvents.GatewayErrorEvent(code, message));

      // show error message in chat
      Component msg = Component.translatable("lsu.gaia.gateway.error", message);
      MessagingUtils.showMiniMessage(
              String.format("<color:%s><bold>[LSU]</bold> <red>%s</red></color>",
                      GATEWAY_COLOR,
                      msg.getString())
      );
   }
}
