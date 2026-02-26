package dev.candycup.lifestealutils.features.titlescreen;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.FeatureFlagController;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.SplashTextRequestEvent;
import dev.candycup.lifestealutils.interapi.MessagingUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * provides custom splash texts for the title screen.
 * reads splashes from the global feature flag payload.
 */
public final class CustomSplashes {
   private static final List<String> FALLBACK_SPLASHES = new ArrayList<>() {{
      add("<yellow>uhoh...</yellow>");
   }};

   public CustomSplashes() {
      LifestealUtilsEvents.SPLASH_TEXT_REQUEST.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onSplashTextRequest(event);
      });
   }

   public boolean isEnabled() {
      return Config.isCustomSplashesEnabled();
   }

   public void onSplashTextRequest(SplashTextRequestEvent event) {
      List<String> splashes = FeatureFlagController.getSplashes();
      if (splashes.isEmpty()) {
         splashes = FALLBACK_SPLASHES;
      }
      String splash = splashes.get((int) (Math.random() * splashes.size()));

      event.setSplashText(MessagingUtils.miniMessageToSplashSafe(splash));
   }
}

