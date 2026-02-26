package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.LifestealServerDetector;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.PlayerNameRenderEvent;
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
public class RankPlusColorNormalizer {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/rankplus");
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   public RankPlusColorNormalizer() {
      LifestealUtilsEvents.CHAT_MESSAGE_RECEIVED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onChatMessageReceived(event);
      });
      LifestealUtilsEvents.PLAYER_NAME_RENDER.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onPlayerNameRender(event);
      });
   }

   public boolean isEnabled() {
      return Config.isRemoveUniquePlusColor();
   }

   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      Component normalized = normalizeComponent(event.getModifiedMessage());
      if (normalized != event.getModifiedMessage()) {
         event.setModifiedMessage(normalized);
         LOGGER.debug("[lsu-rankplus] normalized plus color in chat");
      }
   }

   public void onPlayerNameRender(PlayerNameRenderEvent event) {
      if (event.getRenderContext() != PlayerNameRenderEvent.RenderContext.TABLIST) {
         return;
      }

      if (!LifestealAPI.isOnLifestealNetwork()) {
         return;
      }

      Component normalized = normalizeComponent(event.getModifiedDisplayName());
      if (normalized != event.getModifiedDisplayName()) {
         event.setModifiedDisplayName(normalized);
         LOGGER.debug("[lsu-rankplus] normalized plus color in tablist");
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

   private Component normalizeComponent(Component component) {
      if (component == null) {
         return null;
      }

      String serialized = MINI_MESSAGE.serialize(
              MinecraftClientAudiences.of().asAdventure(component)
      );

      String filtered = normalizePlusColor(serialized);
      if (filtered.equals(serialized)) {
         return component;
      }

      return MessagingUtils.miniMessage(filtered);
   }
}
