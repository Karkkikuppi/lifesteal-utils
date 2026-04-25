package dev.candycup.lifestealutils.features.messages;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.features.alliances.AllianceModels;
import dev.candycup.lifestealutils.features.alliances.AllianceProfileCacheManager;
import dev.candycup.lifestealutils.features.alliances.AllianceService;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * dims public chat messages that match configured text patterns.
 */
public class GhostedChatMessageFilter {
   private static final int DEFAULT_COLOR_RGB = 0xFFFFFF;
   private static final int RGB_MASK = 0xFFFFFF;
   private static final float GHOST_TARGET_BRIGHTNESS = 0.35f;
   private static final float GHOST_SATURATION_MULTIPLIER = 0.4f;
   private static final float GHOST_MAX_SATURATION = 0.25f;

   private List<String> cachedPatterns = Collections.emptyList();
   private List<String> normalizedPatterns = Collections.emptyList();

   public GhostedChatMessageFilter() {
      LifestealUtilsEvents.CHAT_MESSAGE_RECEIVED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onChatMessageReceived(event);
      });
   }

   public boolean isEnabled() {
      return Config.isDesloppifierEnabled();
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

      List<String> patterns = getNormalizedPatterns();
      if (patterns.isEmpty()) {
         return;
      }

      String matchText = event.getModifiedMessage().getString();
      if (!matchesAny(patterns, matchText)) {
         return;
      }

      if (shouldBypassAllianceMember(rawMessage)) {
         return;
      }

      Component ghosted = applyGhosting(MessagingUtils.asMiniMessage(event.getModifiedMessage()));
      event.setModifiedMessage(MessagingUtils.miniMessage(ghosted));
   }

   /**
    * rebuilds normalized text patterns if the config list has changed.
    *
    * @return normalized patterns
    */
   private List<String> getNormalizedPatterns() {
      List<String> patterns = Config.getDesloppifiedPatterns();
      List<String> safePatterns = patterns != null ? patterns : Collections.emptyList();
      if (safePatterns.equals(cachedPatterns)) {
         return normalizedPatterns;
      }

      List<String> snapshot = new ArrayList<>(safePatterns);
      List<String> normalized = new ArrayList<>();
      for (String entry : snapshot) {
         if (entry == null || entry.isBlank()) {
            continue;
         }
         normalized.add(entry.toLowerCase(Locale.ROOT));
      }

      cachedPatterns = snapshot;
      normalizedPatterns = normalized;
      return normalizedPatterns;
   }

   private boolean matchesAny(List<String> patterns, String message) {
      if (message == null || message.isBlank()) {
         return false;
      }
      String lowered = message.toLowerCase(Locale.ROOT);
      for (String pattern : patterns) {
         if (lowered.contains(pattern)) {
            return true;
         }
      }
      return false;
   }

   private boolean shouldBypassAllianceMember(String rawMessage) {
      if (!Config.isAllowAllianceBypassGhostedChat()) {
         return false;
      }

      String senderName = extractSenderName(rawMessage);
      if (senderName == null || senderName.isBlank()) {
         return false;
      }

      AllianceProfileCacheManager.initialize();
      AllianceProfileCacheManager.observeWorldPlayers();

      String senderUuid = AllianceProfileCacheManager.getCachedUuidByName(senderName);
      if (senderUuid == null) {
         AllianceProfileCacheManager.queueUuidLookupForName(senderName);
         return false;
      }

      return isAllianceMemberUuid(senderUuid);
   }

   private boolean isAllianceMemberUuid(String uuid) {
      String normalizedUuid = AllianceProfileCacheManager.normalizeUuid(uuid);
      if (normalizedUuid == null) {
         return false;
      }

      for (AllianceModels.AllianceRecord alliance : AllianceService.listAll()) {
         if (alliance == null || alliance.data == null || alliance.data.lists == null) {
            continue;
         }
         for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
            if (list == null || list.members == null) {
               continue;
            }
            for (AllianceModels.AllianceMember member : list.members) {
               String memberUuid = AllianceProfileCacheManager.normalizeUuid(member == null ? null : member.uuid);
               if (normalizedUuid.equals(memberUuid)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private String extractSenderName(String rawMessage) {
      if (rawMessage == null || rawMessage.isBlank()) {
         return null;
      }

      int colonIndex = rawMessage.indexOf(':');
      if (colonIndex <= 0) {
         return null;
      }

      String prefix = rawMessage.substring(0, colonIndex).trim();
      while (prefix.startsWith("[") && prefix.contains("]")) {
         int end = prefix.indexOf(']');
         if (end < 0) {
            break;
         }
         prefix = prefix.substring(end + 1).trim();
      }

      if (prefix.isBlank()) {
         return null;
      }

      String[] tokens = prefix.split("\\s+");
      if (tokens.length == 0) {
         return null;
      }

      String candidate = tokens[tokens.length - 1]
              .replaceAll("^[^A-Za-z0-9_]+", "")
              .replaceAll("[^A-Za-z0-9_]+$", "");
      return candidate.isBlank() ? null : candidate;
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
