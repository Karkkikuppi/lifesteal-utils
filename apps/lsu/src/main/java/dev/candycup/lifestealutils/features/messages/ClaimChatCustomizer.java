package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
public final class ClaimChatCustomizer {
   private static final Pattern CLAIM_CHAT_PATTERN = Pattern.compile("^([^|]+?)\\s*\\|\\s*([^:]+):\\s+(.*)$", Pattern.CASE_INSENSITIVE);
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   private ClaimChatCustomizer() {
   }

   public static boolean tryHandle(String messageContents) {
      Matcher matcher = CLAIM_CHAT_PATTERN.matcher(messageContents);
      if (!matcher.find()) {
         return false;
      }

      String claim = MINI_MESSAGE.escapeTags(matcher.group(1));
      String username = MINI_MESSAGE.escapeTags(matcher.group(2));
      String message = MINI_MESSAGE.escapeTags(matcher.group(3));

      String format = getFormat();

      String formatted = format
              .replace("{{claim}}", claim)
              .replace("{{username}}", username)
              .replace("{{message}}", message);

      MessagingUtils.showMiniMessage(formatted);
      return true;
   }

   private static String getFormat() {
      try {
         return ConfigUtils.claimChatFormat;
      } catch (Exception e) {
         return "<gold><bold>{{claim}}</bold></gold> <dark_gray>|</dark_gray> <aqua>{{username}}</aqua><gray>:</gray> <white>{{message}}</white>";
      }
   }
}
*/
