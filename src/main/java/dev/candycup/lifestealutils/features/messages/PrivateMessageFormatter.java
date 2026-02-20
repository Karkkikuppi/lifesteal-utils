package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

/**
 * formats private messages with custom styling.
 * replaces "(MSG From/To Username) message" with a customizable format.
 */
public class PrivateMessageFormatter {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/pm");
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   public PrivateMessageFormatter() {
      LifestealUtilsEvents.CHAT_MESSAGE_RECEIVED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onChatMessageReceived(event);
      });
   }

   public boolean isEnabled() {
      return Config.isEnablePmFormat();
   }

   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      String rawMessage = event.getMessage().getString();
      Matcher matcher = MessagePatterns.PRIVATE_MESSAGE_PATTERN.matcher(rawMessage);

      if (!matcher.find()) {
         return;
      }

      String direction = capitalizeFirst(matcher.group(1));
      String sender = MINI_MESSAGE.escapeTags(matcher.group(2));
      String message = MINI_MESSAGE.escapeTags(matcher.group(3));

      String format = Config.getPmFormat() != null && !Config.getPmFormat().isBlank()
              ? Config.getPmFormat()
              : "<light_purple><bold>{{direction}}</bold> {{sender}}</light_purple> <white>➡ {{message}}</white>";

      String formatted = format
              .replace("{{direction}}", direction)
              .replace("{{sender}}", sender)
              .replace("{{message}}", message);

      MessagingUtils.showMiniMessage(formatted);
      event.setCancelled(true); // prevent original message from showing

      LOGGER.debug("[lsu-pm] formatted PM: {} -> {}", direction, sender);
   }

   private String capitalizeFirst(String value) {
      if (value == null || value.isEmpty()) {
         return value;
      }
      return Character.toUpperCase(value.charAt(0)) + value.substring(1);
   }
}
