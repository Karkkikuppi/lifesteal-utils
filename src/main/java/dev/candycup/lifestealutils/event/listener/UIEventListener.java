package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.SplashTextRequestEvent;
import dev.candycup.lifestealutils.event.events.TitleScreenInitEvent;

/**
 * listener interface for UI-related events.
 */
public interface UIEventListener extends LifestealEventListener {

   /**
    * called when the title screen is initialized.
    * features can add custom buttons or modify the screen.
    *
    * @param event the title screen init event
    */
   default void onTitleScreenInit(TitleScreenInitEvent event) {
   }

   /**
    * called when a splash text is requested for the title screen.
    * features can provide custom splash texts.
    *
    * @param event the splash text request event
    */
   default void onSplashTextRequest(SplashTextRequestEvent event) {
   }
}
