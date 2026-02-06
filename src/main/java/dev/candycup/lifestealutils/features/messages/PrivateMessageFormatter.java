package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.listener.ChatEventListener;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * formats private messages with custom styling.
 * replaces "(MSG From/To Username) message" with a customizable format.
 */
public class PrivateMessageFormatter implements ChatEventListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/pm");
   private static final Pattern PRIVATE_MESSAGE_PATTERN = Pattern.compile(
           "^\\(MSG\\s+(From|To)\\s+([^)]+)\\)\\s+(.*)$",
           Pattern.CASE_INSENSITIVE
   );
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   @Override
   public boolean isEnabled() {
      return Config.getEnablePmFormat();
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.HIGH; // process early, cancels original message
   }

   @Override
   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      String rawMessage = event.getMessage().getString();
      Matcher matcher = PRIVATE_MESSAGE_PATTERN.matcher(rawMessage);

      if (!matcher.find()) {
         return;
      }

      String direction = capitalizeFirst(matcher.group(1));
      String sender = MINI_MESSAGE.escapeTags(matcher.group(2));
      String message = MINI_MESSAGE.escapeTags(matcher.group(3));

      String format = Config.pmFormat != null && !Config.pmFormat.isBlank()
              ? Config.pmFormat
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
