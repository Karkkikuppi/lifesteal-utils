package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.events.PlayerNameRenderEvent;
import dev.candycup.lifestealutils.event.listener.ChatEventListener;
import dev.candycup.lifestealutils.event.listener.RenderEventListener;
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
 * 
 * also works for player nametags, not just chat messages.
 */
public class RankPlusColorNormalizer implements ChatEventListener, RenderEventListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/rankplus");
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   @Override
   public boolean isEnabled() {
      // Always enabled
      return true;
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.HIGH; // HIGH priority so this runs before Alliances (NORMAL priority)
   }

   @Override
   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      Component original = event.getModifiedMessage();
      String serialized = MINI_MESSAGE.serialize(
              MinecraftClientAudiences.of().asAdventure(original)
      );

      // Fix literal color tags that appear from Lifesteal Plus
      String cleaned = fixLiteralColorTags(serialized);
      
      // Then normalize plus color
      String filtered = normalizePlusColor(cleaned);

      if (!filtered.equals(serialized)) {
         Component modified = MessagingUtils.miniMessage(filtered);
         event.setModifiedMessage(modified);
         LOGGER.debug("[lsu-rankplus] cleaned chat message");
      }
   }

   @Override
   public void onPlayerNameRender(PlayerNameRenderEvent event) {
      Component original = event.getModifiedDisplayName();
      String serialized = MINI_MESSAGE.serialize(
              MinecraftClientAudiences.of().asAdventure(original)
      );

      // Fix literal color tags that appear from Lifesteal Plus
      String cleaned = fixLiteralColorTags(serialized);
      
      // Then normalize plus color
      String filtered = normalizePlusColor(cleaned);

      if (!filtered.equals(serialized)) {
         Component modified = MessagingUtils.miniMessage(filtered);
         event.setModifiedDisplayName(modified);
         LOGGER.debug("[lsu-rankplus] cleaned display name");
      }
   }

   /**
    * merge the colored plus into the rank color.
    * Only runs if Config.getRemoveUniquePlusColor() is enabled.
    */
   private String normalizePlusColor(String message) {
      if (message == null || message.isEmpty()) {
         return message;
      }
      
      // Only normalize if the config option is enabled
      if (!Config.getRemoveUniquePlusColor()) {
         return message;
      }

      // Pattern to match rnk + colored plus symbol
      // This captures: <bold><color>RANK</color></bold> <green>+</green>
      // And converts to: <bold><color>RANK+</color></bold>
      String pattern = "(<bold>\\s*<([#A-Za-z0-9_]+)>)([^<>]+)(</[A-Za-z0-9_#]+>\\s*</bold>)(\\s*)<[^>]*>\\+(?:</[^>]*>)?";
      String result = message.replaceAll(pattern, "$1$3+$4");

      // Also ensure any remaining open green tags after plus are properly closed
      // This handles cases where the green tag wasn't caught by the pattern above
      result = result.replaceAll("<green>\\+</green>", "+");
      result = result.replaceAll("<green>\\+", "+</green>");

      // normalize whitespace
      result = result.replaceAll("(<dark_gray>\\]</dark_gray>)\\s+", "$1 ");
      result = result.replaceAll("\\]\\s+", "] ");
      result = result.replaceAll("[\\s\\u00A0]+", " ").trim();

      return result;
   }
   
   private String fixLiteralColorTags(String message) {
      if (message == null || message.isEmpty()) {
         return message;
      }
      
      String result = message;
      boolean shouldNormalize = Config.getRemoveUniquePlusColor();
      
      if (shouldNormalize) {
         // When normalization is ON, remove the literal tags but ensure proper closure
         // First, remove any literal closing tags
         result = result.replaceAll("(?i)<\\s*/\\s*green\\s*>", "");
         result = result.replaceAll("(?i)<\\\\/\\s*green\\s*>", "");
         result = result.replaceAll("(?i)&lt;\\s*/\\s*green\\s*&gt;", "");
         
         // Then ensure any <green> tags around the + are properly closed
         // This prevents green from bleeding into the username
         result = result.replaceAll("(<green>\\+)(?!</green>)", "$1</green>");
         
         // Also handle other color tags
         String[] colors = {"red", "blue", "yellow", "white", "gray", "dark_gray", "aqua", "dark_aqua"};
         for (String color : colors) {
            result = result.replaceAll("(?i)<\\s*/\\s*" + color + "\\s*>", "");
            result = result.replaceAll("(?i)<\\\\/\\s*" + color + "\\s*>", "");
            result = result.replaceAll("(?i)&lt;\\s*/\\s*" + color + "\\s*&gt;", "");
         }
      } else {
         // When normalization is OFF, unescape the tags so they work properly
         result = result.replaceAll("&lt;\\s*/\\s*green\\s*&gt;", "</green>");
         result = result.replaceAll("&lt;\\s*/\\s*red\\s*&gt;", "</red>");
         result = result.replaceAll("&lt;\\s*/\\s*blue\\s*&gt;", "</blue>");
         result = result.replaceAll("&lt;\\s*/\\s*yellow\\s*&gt;", "</yellow>");
         result = result.replaceAll("&lt;\\s*/\\s*white\\s*&gt;", "</white>");
         result = result.replaceAll("&lt;\\s*/\\s*gray\\s*&gt;", "</gray>");
         result = result.replaceAll("&lt;\\s*/\\s*dark_gray\\s*&gt;", "</dark_gray>");
         result = result.replaceAll("&lt;\\s*/\\s*aqua\\s*&gt;", "</aqua>");
         result = result.replaceAll("&lt;\\s*/\\s*dark_aqua\\s*&gt;", "</dark_aqua>");
      }
      
      // Clean up any double spaces that might result from tag removal
      result = result.replaceAll("  +", " ");
      result = result.trim();
      
      if (!result.equals(message)) {
         LOGGER.debug("[lsu-rankplus] fixed literal color tags: '{}' -> '{}'", message, result);
      }
      
      return result;
   }
}
