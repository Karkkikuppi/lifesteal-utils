package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.listener.ChatEventListener;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * normalizes rank plus coloring by merging the colored plus into the rank's color.
 * example: "<bold><#FF7200>HEROIC</#FF7200></bold><green>+</green>"
 * -> "<bold><#FF7200>HEROIC+</#FF7200></bold>"
 */
public class RankPlusColorNormalizer implements ChatEventListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/rankplus");
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   @Override
   public boolean isEnabled() {
      return Config.getRemoveUniquePlusColor();
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   @Override
   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      Component original = event.getModifiedMessage();
      String serialized = MINI_MESSAGE.serialize(
              MinecraftClientAudiences.of().asAdventure(original)
      );

      String filtered = normalizePlusColor(serialized);

      if (!filtered.equals(serialized)) {
         Component modified = MessagingUtils.miniMessage(filtered);
         event.setModifiedMessage(modified);
         LOGGER.debug("[lsu-rankplus] normalized plus color");
      }
   }

   /**
    * merge the colored plus into the rank color.
    */
   private String normalizePlusColor(String message) {
      if (message == null || message.isEmpty()) {
         return message;
      }

      String pattern = "(<bold>\\s*<([#A-Za-z0-9_]+)>)([^<>]+)(</[A-Za-z0-9_#]+>\\s*</bold>)(\\s*)<[^>]*>\\+(?:</[^>]*>)?";
      String result = message.replaceAll(pattern, "$1$3+$4");

      // normalize whitespace
      result = result.replaceAll("(<dark_gray>\\]</dark_gray>)\\s+", "$1 ");
      result = result.replaceAll("\\]\\s+", "] ");
      result = result.replaceAll("[\\s\\u00A0]+", " ").trim();

      return result;
   }
}
