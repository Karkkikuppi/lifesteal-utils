package dev.candycup.lifestealutils.gaia;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages Gaia consent state and remote notice content.
 */
public final class GaiaConsentController {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/gaia");
   private static final String CONSENT_URL = "https://gist.githubusercontent.com/Karkkikuppi/a0362d69a4fa39007cbdcff362713bd1/raw/Gaia%20Consent%20Notice.md";
   private static final String CACHE_BUSTER_PARAM = "?v=";

   private static final AtomicInteger CONTENT_VERSION = new AtomicInteger(0);
   private static volatile String consentMiniMessage;

   private GaiaConsentController() {
   }

   /**
    * Initializes the Gaia consent system by fetching the remote notice content.
    */
   public static void initialize() {
      NetworkUtilsController.getAsync(withCacheBuster(CONSENT_URL)).thenAccept(result -> {
         if (result.success() && result.body() != null && !result.body().isBlank()) {
            consentMiniMessage = result.body();
            CONTENT_VERSION.incrementAndGet();
            return;
         }
         LOGGER.debug("failed to fetch gaia consent notice: {}", result.error());
      });
   }

   private static String withCacheBuster(String url) {
      return url + CACHE_BUSTER_PARAM + System.currentTimeMillis();
   }

   /**
    * Returns the current consent notice content version.
    *
    * @return the content version counter
    */
   public static int getConsentContentVersion() {
      return CONTENT_VERSION.get();
   }

   /**
    * Returns the current consent notice as a MiniMessage string.
    *
    * @return the MiniMessage content, or null if not loaded yet
    */
   public static String getConsentMiniMessage() {
      return consentMiniMessage;
   }

   /**
    * Checks if the consent screen should be shown.
    *
    * @return true if the user has not yet seen the consent screen
    */
   public static boolean shouldShowConsent() {
      return !Config.isGaiaConsentSeen();
   }

   /**
    * Records the user's consent decision and persists it.
    *
    * @param enabled whether advanced features are enabled
    */
   public static void recordConsentDecision(boolean enabled) {
      Config.setGaiaAdvancedFeaturesEnabled(enabled);
      Config.setGaiaConsentSeen(true);
   }
}
