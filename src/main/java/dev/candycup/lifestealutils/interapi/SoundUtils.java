package dev.candycup.lifestealutils.interapi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * shared sound helpers for ui interactions.
 */
public final class SoundUtils {
   private SoundUtils() {
   }

   /**
    * plays the standard ui button click sound.
    */
   public static void playUiClick() {
      Minecraft client = Minecraft.getInstance();
      if (client == null || client.getSoundManager() == null) {
         return;
      }
      client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
   }
}
