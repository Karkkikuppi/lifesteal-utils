package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.SplashTextRequestEvent;
import dev.candycup.lifestealutils.event.events.TitleScreenInitEvent;

/**
 * listener interface for UI-related events.
 */
public interface UIEventListener extends LifestealEventListener {

    /**
     * Invoked when the title screen is initialized so listeners may modify the screen or add controls.
     *
     * @param event the title screen initialization event, providing access to the screen and UI elements
     */
    default void onTitleScreenInit(TitleScreenInitEvent event) {
    }

    /**
     * Invoked when a splash text is requested for the title screen.
     *
     * Allows a listener to provide or modify the splash text through the event.
     *
     * @param event the splash text request event; use it to set or replace the splash text
     */
    default void onSplashTextRequest(SplashTextRequestEvent event) {
    }
}