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
    * called when an item entity is about to be rendered.
    * features can modify the poseStack to apply transforms (e.g., scaling).
    *
    * @param event the item render event
    */
   default void onItemRender(ItemRenderEvent event) {
   }

   /**
    * called when a player name tag is rendered.
    * features can modify the display name for alliance colors, etc.
    *
    * @param event the player name render event
    */
   default void onPlayerNameRender(PlayerNameRenderEvent event) {
   }
}
