package dev.candycup.lifestealutils.features.afk;

import net.minecraft.client.Minecraft;

/**
 * manages local afk mode state and applies a low fps cap while enabled.
 */
public final class AfkMode {
   private static final int AFK_FPS_LIMIT = 2;
   private static final int UNSET_FPS_LIMIT = -1;
   private static boolean enabled = false;
   private static int previousFpsLimit = UNSET_FPS_LIMIT;

   private AfkMode() {
   }

   /**
    * @return true when afk mode is enabled
    */
   public static boolean isEnabled() {
      return enabled;
   }

   /**
    * toggles the current afk mode state.
    *
    * @return true when afk mode is enabled after toggling
    */
   public static boolean toggle() {
      if (enabled) {
         disable();
         return false;
      }
      enable();
      return true;
   }

   /**
    * enables afk mode and caps fps.
    */
   public static void enable() {
      if (enabled) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null) {
         previousFpsLimit = minecraft.options.framerateLimit().get();
         minecraft.getFramerateLimitTracker().setFramerateLimit(AFK_FPS_LIMIT);
      }
      enabled = true;
   }

   /**
    * disables afk mode and restores the previous fps cap.
    */
   public static void disable() {
      if (!enabled) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null && previousFpsLimit != UNSET_FPS_LIMIT) {
         minecraft.getFramerateLimitTracker().setFramerateLimit(previousFpsLimit);
      }
      previousFpsLimit = UNSET_FPS_LIMIT;
      enabled = false;
   }
}
