package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * fired when the title screen is initialized.
 * features can use this to add custom buttons or modify the screen.
 */
public class TitleScreenInitEvent extends LSUEvent {
    private final TitleScreen titleScreen;

    /**
     * Creates an event instance representing the initialization of the title screen.
     *
     * @param titleScreen the TitleScreen instance associated with this event
     */
    public TitleScreenInitEvent(TitleScreen titleScreen) {
        this.titleScreen = titleScreen;
    }

    /**
     * Gets the title screen associated with this event.
     *
     * @return the {@link TitleScreen} instance provided when the event was created
     */
    public TitleScreen getTitleScreen() {
        return titleScreen;
    }
}