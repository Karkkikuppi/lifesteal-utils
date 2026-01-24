package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;

/**
 * fired when the game requests a splash text for the title screen.
 * features can provide a custom splash text by setting the value.
 */
public class SplashTextRequestEvent extends LSUEvent {
    private String splashText;

    /**
     * Creates a new SplashTextRequestEvent with no splash text set.
     */
    public SplashTextRequestEvent() {
        this.splashText = null;
    }

    /**
     * Gets the current custom splash text for the title screen.
     *
     * @return the custom splash text, or `null` if none was set
     */
    public String getSplashText() {
        return splashText;
    }

    /**
     * Set the custom splash text for the title screen.
     *
     * If multiple features set a value, the last one wins.
     *
     * @param splashText the splash text to display, or `null` to clear any custom splash text
     */
    public void setSplashText(String splashText) {
        this.splashText = splashText;
    }
}