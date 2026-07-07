package dev.candycup.lifestealutils.features.messages;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.ConfigurableMinimessage;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

/**
 * formats private messages with custom styling.
 * replaces "(MSG From/To Username) message" with a customizable format.
 */
public class PrivateMessageFormatter {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/pm");
    public static final String DEFAULT_FORMAT = "<light_purple><bold>{{direction}}</bold> {{sender}}</light_purple> <white>\u27A1 {{message}}</white>";

    @Getter
    @Setter
    @SerialEntry(comment = "Whether to enable custom private message formatting")
    @ConfigurableBoolean(location = "customization.messages.pmformatenabled")
    private static boolean enablePmFormat = false;

    @Getter
    @Setter
    @SerialEntry(comment = "Customize the format of private messages (/msg, /r)")
    @ConfigurableMinimessage(location = "customization.messages.pmformat")
    private static String pmFormat = DEFAULT_FORMAT;

   public PrivateMessageFormatter() {
      LifestealUtilsEvents.CHAT_MESSAGE_RECEIVED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onChatMessageReceived(event);
      });
   }

   public boolean isEnabled() {
       return enablePmFormat;
   }

   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      String rawMessage = event.getMessage().getString();
      Matcher matcher = MessagePatterns.PRIVATE_MESSAGE_PATTERN.matcher(rawMessage);

      if (!matcher.find()) {
         return;
      }

      String direction = capitalizeFirst(matcher.group(1));
      String sender = MessagingUtils.escapeMiniMessageTags(matcher.group(2));
      String message = MessagingUtils.escapeMiniMessageTags(matcher.group(3));

       String format = pmFormat != null && !pmFormat.isBlank() ? pmFormat : DEFAULT_FORMAT;

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
