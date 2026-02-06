package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;

/**
 * fired when the game requests a splash text for the title screen.
 * features can provide a custom splash text by setting the value.
 */
public class SplashTextRequestEvent extends LSUEvent {
   private String splashText;

   public SplashTextRequestEvent() {
      this.splashText = null;
   }

   /**
    * @return the custom splash text, or null if no feature provided one
    */
   public String getSplashText() {
      return splashText;
   }

   /**
    * set a custom splash text. if multiple features set this, the last one wins.
    *
    * @param splashText the splash text to display
    */
   public void setSplashText(String splashText) {
      this.splashText = splashText;
   }
}
