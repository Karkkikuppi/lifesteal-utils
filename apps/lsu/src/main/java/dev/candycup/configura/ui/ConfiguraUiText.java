package dev.candycup.configura.ui;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.network.chat.Component;

public final class ConfiguraUiText {
   private ConfiguraUiText() {
   }

   public static ConfiguraTextPreview previewMiniMessage(String miniMessage) {
      try {
         return new ConfiguraTextPreview(MessagingUtils.miniMessage(miniMessage), true);
      } catch (Exception ignored) {
         return new ConfiguraTextPreview(Component.literal(miniMessage), false);
      }
   }
}
