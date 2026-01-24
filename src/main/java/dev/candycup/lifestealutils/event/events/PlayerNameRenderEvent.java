package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.network.chat.Component;

/**
 * fired when a player's display name is rendered (in-world name tag or tab list).
 * <p>
 * performance note: this event fires frequently during rendering.
 * listeners should cache results where possible and avoid expensive operations.
 * <p>
 * features can modify the display name by setting a new value.
 */
public class PlayerNameRenderEvent extends LSUEvent {
    private final String playerName;
    private final Component originalDisplayName;
    private Component modifiedDisplayName;

    /**
     * Creates a PlayerNameRenderEvent for a player's display name rendering.
     *
     * @param playerName the player's plain username
     * @param originalDisplayName the original, unmodified display name; the event's modified display name is initialized to this value
     */
    public PlayerNameRenderEvent(String playerName, Component originalDisplayName) {
        this.playerName = playerName;
        this.originalDisplayName = originalDisplayName;
        this.modifiedDisplayName = originalDisplayName;
    }

    /**
     * Retrieve the player's plain username.
     *
     * @return the player's plain username
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Original display name before any listener modifications.
     *
     * @return the original, unmodified display name
     */
    public Component getOriginalDisplayName() {
        return originalDisplayName;
    }

    /**
     * Gets the display name currently used for the player.
     *
     * @return the display name currently in use; may reflect modifications applied by previous listeners
     */
    public Component getModifiedDisplayName() {
        return modifiedDisplayName;
    }

    /**
     * Set the display name to be used by subsequent listeners.
     *
     * @param displayName the new display name Component to present to downstream listeners
     */
    public void setModifiedDisplayName(Component displayName) {
        this.modifiedDisplayName = displayName;
    }
}