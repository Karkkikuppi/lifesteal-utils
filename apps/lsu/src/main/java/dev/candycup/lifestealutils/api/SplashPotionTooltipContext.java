package dev.candycup.lifestealutils.api;

public final class SplashPotionTooltipContext {
   public static final ThreadLocal<Boolean> IS_SPLASH_POTION = ThreadLocal.withInitial(() -> false);

   private SplashPotionTooltipContext() {
   }
}
