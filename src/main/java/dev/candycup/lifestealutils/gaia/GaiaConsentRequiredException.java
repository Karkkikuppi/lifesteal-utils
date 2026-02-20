package dev.candycup.lifestealutils.gaia;

public final class GaiaConsentRequiredException extends RuntimeException {
   public GaiaConsentRequiredException() {
      super("Gaia consent is required before making Gaia requests");
   }
}