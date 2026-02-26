package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * dims public chat messages that match configured regex patterns.
 */
public class GhostedChatMessageFilter {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/ghosted-chat");
   private static final int DEFAULT_COLOR_RGB = 0xFFFFFF;
   private static final int RGB_MASK = 0xFFFFFF;
   private static final float GHOST_TARGET_BRIGHTNESS = 0.35f;
   private static final float GHOST_SATURATION_MULTIPLIER = 0.4f;
   private static final float GHOST_MAX_SATURATION = 0.25f;

   private List<String> cachedPatterns = Collections.emptyList();
   private List<Pattern> compiledPatterns = Collections.emptyList();

   public GhostedChatMessageFilter() {
      LifestealUtilsEvents.CHAT_MESSAGE_RECEIVED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onChatMessageReceived(event);
      });
   }

   public boolean isEnabled() {
      return Config.isGhostedChatEnabled();
   }

   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      String rawMessage = event.getMessage().getString();
      if (rawMessage == null || !rawMessage.startsWith("[")) {
         return;
      }

      Matcher matcher = MessagePatterns.PRIVATE_MESSAGE_PATTERN.matcher(rawMessage);
      if (matcher.find()) {
         return;
      }

      List<Pattern> patterns = getCompiledPatterns();
      if (patterns.isEmpty()) {
         return;
      }

      String matchText = event.getModifiedMessage().getString();
      if (!matchesAny(patterns, matchText)) {
         return;
      }

      Component ghosted = applyGhosting(MessagingUtils.asMiniMessage(event.getModifiedMessage()));
      event.setModifiedMessage(MessagingUtils.miniMessage(ghosted));
   }

   /**
    * rebuilds compiled regex patterns if the config list has changed.
    * invalid patterns are ignored and logged.
    *
    * @return compiled regex patterns
    */
   private List<Pattern> getCompiledPatterns() {
      List<String> patterns = Config.getGhostedChatPatterns();
      List<String> safePatterns = patterns != null ? patterns : Collections.emptyList();
      if (safePatterns.equals(cachedPatterns)) {
         return compiledPatterns;
      }

      List<String> snapshot = new ArrayList<>(safePatterns);
      List<Pattern> compiled = new ArrayList<>();
      for (String entry : snapshot) {
         if (entry == null || entry.isBlank()) {
            continue;
         }
         try {
            compiled.add(Pattern.compile(entry));
         } catch (PatternSyntaxException e) {
            LOGGER.warn("[lsu-ghosted-chat] invalid regex ignored: {}", entry);
         }
      }

      cachedPatterns = snapshot;
      compiledPatterns = compiled;
      return compiledPatterns;
   }

   private boolean matchesAny(List<Pattern> patterns, String message) {
      if (message == null || message.isBlank()) {
         return false;
      }
      for (Pattern pattern : patterns) {
         if (pattern.matcher(message).find()) {
            return true;
         }
      }
      return false;
   }

   private Component applyGhosting(Component component) {
      List<Component> children = component.children();
      List<Component> updatedChildren = new ArrayList<>(children.size());
      for (Component child : children) {
         updatedChildren.add(applyGhosting(child));
      }

      Style style = component.style();
      TextColor ghostColor = ghostColor(style.color());
      Component updated = component.style(style.color(ghostColor));
      if (!children.equals(updatedChildren)) {
         updated = updated.children(updatedChildren);
      }
      return updated;
   }

   private TextColor ghostColor(TextColor originalColor) {
      int rgb = originalColor != null ? originalColor.value() : DEFAULT_COLOR_RGB;
      int r = (rgb >> 16) & 0xFF;
      int g = (rgb >> 8) & 0xFF;
      int b = rgb & 0xFF;

      float[] hsb = Color.RGBtoHSB(r, g, b, null);
      float saturation = Math.min(hsb[1] * GHOST_SATURATION_MULTIPLIER, GHOST_MAX_SATURATION);
      int ghostRgb = Color.HSBtoRGB(hsb[0], saturation, GHOST_TARGET_BRIGHTNESS) & RGB_MASK;
      return TextColor.color(ghostRgb);
   }
}
