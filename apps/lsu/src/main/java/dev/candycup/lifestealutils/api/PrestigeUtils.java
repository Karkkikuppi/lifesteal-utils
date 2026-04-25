package dev.candycup.lifestealutils.api;

import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PrestigeUtils {
   public static final String POTION_DURATION_KEY = "Increased Splash Potion Duration";

   private static final String PERK_LINE_PREFIX = "\u2022 ";
   // matches "• Key Name: 6.0% → 12.0%" or "• Key Name: 6.0%"
   private static final Pattern PERK_PATTERN = Pattern.compile("^\u2022 (.+): ([\\d.]+)%.*$");

   private PrestigeUtils() {
   }

   /**
    * Parses prestige enhancement lore lines from the 'Current Enhancements' book.
    * Lines starting with '∙ ' are perk entries in the format:
    * "∙ Key Name: value% → nextLevel%" (next level indicator is optional)
    */
   public static Map<String, Float> parseLoreLines(List<Component> loreComponents) {
      Map<String, Float> result = new HashMap<>();
      for (Component component : loreComponents) {
         String text = component.getString();
         if (!text.startsWith(PERK_LINE_PREFIX)) continue;
         Matcher matcher = PERK_PATTERN.matcher(text);
         if (!matcher.matches()) continue;
         String key = matcher.group(1).trim();
         String valueStr = matcher.group(2);
         try {
            result.put(key, Float.parseFloat(valueStr));
         } catch (NumberFormatException ignored) {
         }
      }
      return result;
   }

   /**
    * Returns the prestige potion duration boost as a percentage (e.g. 25.0 for 25%).
    * Returns 0.0 if no boost is stored.
    */
   public static float getPotionDurationBoostPercent() {
      return PersistentKnowledgeController.getPrestigeEnhancement(POTION_DURATION_KEY);
   }
}
