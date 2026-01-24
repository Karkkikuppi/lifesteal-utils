package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.ItemRenderEvent;
import dev.candycup.lifestealutils.event.events.PlayerNameRenderEvent;

/**
 * listener interface for render-related events.
 * <p>
 * performance warning: render events fire extremely frequently (60-144+ fps).
 * implementations must be highly optimized:
 * - avoid allocations in hot paths
 * - cache expensive computations
 * - check isEnabled() first thing to short-circuit
 * - keep logic minimal and fast
 */
public interface RenderEventListener extends LifestealEventListener {

    /**
     * Invoked when an item entity is about to be rendered, allowing modifications to the render transform.
     *
     * @param event the item render event containing the pose stack and render context; handlers may modify the pose stack (for example, to apply translations, rotations, or scaling)
     */
    default void onItemRender(ItemRenderEvent event) {
    }

    /**
     * Invoked when a player's name tag is about to be rendered, allowing modification of the display name (for example, to apply alliance colors).
     *
     * @param event the PlayerNameRenderEvent providing the player, current display name, and methods to change the name that will be rendered
     */
    default void onPlayerNameRender(PlayerNameRenderEvent event) {
    }
}