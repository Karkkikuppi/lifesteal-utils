package dev.candycup.ui;

import net.minecraft.client.gui.Font;

import java.util.Locale;

public final class Text {
   private static final String[] SMALL_CAPS = {
           "ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ꜰ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ",
           "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "ѕ", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "х", "ʏ", "ᴢ"
   };

   private Text() {
   }

   public static String smallCaps(String text) {
      StringBuilder builder = new StringBuilder(text.length());
      for (int i = 0; i < text.length(); i++) {
         char character = text.charAt(i);
         if (character >= 'A' && character <= 'Z') {
            builder.append(SMALL_CAPS[character - 'A']);
         } else if (character >= 'a' && character <= 'z') {
            builder.append(SMALL_CAPS[character - 'a']);
         } else {
            builder.append(character);
         }
      }
      return builder.toString();
   }

   public static String trim(Font font, String text, int width) {
      if (font.width(text) <= width) {
         return text;
      }
      String suffix = "...";
      int max = Math.max(0, width - font.width(suffix));
      String trimmed = text;
      while (!trimmed.isEmpty() && font.width(trimmed) > max) {
         trimmed = trimmed.substring(0, trimmed.length() - 1);
      }
      return trimmed + suffix;
   }

   public static String compactAmount(double amount) {
      if (amount < 1_000D) {
         return String.format(Locale.ROOT, "%.0f", amount);
      }
      String suffix;
      double scaled;
      if (amount >= 1_000_000_000D) {
         suffix = "B";
         scaled = amount / 1_000_000_000D;
      } else if (amount >= 1_000_000D) {
         suffix = "M";
         scaled = amount / 1_000_000D;
      } else {
         suffix = "K";
         scaled = amount / 1_000D;
      }
      if (scaled >= 100D || Math.abs(scaled - Math.rint(scaled)) < 0.05D) {
         return String.valueOf((long) Math.rint(scaled)) + suffix;
      }
      return String.format(Locale.ROOT, "%.1f%s", scaled, suffix);
   }
}
