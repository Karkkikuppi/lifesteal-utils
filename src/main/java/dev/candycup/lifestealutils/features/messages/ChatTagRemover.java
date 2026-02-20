package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * removes chat tags (e.g., [No-Life]) from player messages.
 * example: "[LEGEND+] [No-Life] Player: msg" -> "[LEGEND+] Player: msg"
 */
public class ChatTagRemover {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/chattag");
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   public ChatTagRemover() {
      LifestealUtilsEvents.CHAT_MESSAGE_RECEIVED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onChatMessageReceived(event);
      });
   }

   public boolean isEnabled() {
      return Config.isDisableChatTags();
   }

   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      Component original = event.getModifiedMessage();
      String serialized = MINI_MESSAGE.serialize(
              MinecraftClientAudiences.of().asAdventure(original)
      );

      String filtered = removeChatTag(serialized);

      if (!filtered.equals(serialized)) {
         Component modified = MessagingUtils.miniMessage(filtered);
         event.setModifiedMessage(modified);
         LOGGER.debug("[lsu-chattag] removed chat tag from message");
      }
   }

   /**
    * remove the chat tag (second bracket) from messages.
    */
   private String removeChatTag(String message) {
      if (message == null || message.isEmpty()) {
         return message;
      }

      StringBuilder visible = new StringBuilder();
      List<BracketSpan> spans = new ArrayList<>();

      boolean inTag = false;
      int bracketStartVisible = -1;
      int bracketStartRaw = -1;

      for (int i = 0; i < message.length(); i++) {
         char c = message.charAt(i);
         if (inTag) {
            if (c == '>') inTag = false;
            continue;
         }

         if (c == '<') {
            inTag = true;
            continue;
         }

         int visibleIndex = visible.length();
         visible.append(c);

         if (c == '[') {
            bracketStartVisible = visibleIndex;
            bracketStartRaw = i;
         } else if (c == ']' && bracketStartVisible >= 0) {
            spans.add(new BracketSpan(bracketStartVisible, visibleIndex + 1, bracketStartRaw, i + 1));
            bracketStartVisible = -1;
            bracketStartRaw = -1;
         }
      }

      String visibleString = visible.toString();
      if (spans.size() < 2) {
         return message;
      }

      int colonIndex = visibleString.indexOf(':', spans.get(1).visibleEnd);
      if (colonIndex < 0 || colonIndex - spans.get(1).visibleEnd > 50) {
         return message;
      }

      BracketSpan secondBracket = spans.get(1);
      String result = message.substring(0, secondBracket.rawStart)
              + message.substring(secondBracket.rawEnd);

      // normalize whitespace
      result = result.replaceAll("(<dark_gray>\\]</dark_gray>)\\s+", "$1 ");
      result = result.replaceAll("\\]\\s+", "] ");
      result = result.replaceAll("[\\s\\u00A0]+", " ").trim();

      return result;
   }

   private record BracketSpan(int visibleStart, int visibleEnd, int rawStart, int rawEnd) {
   }
}
