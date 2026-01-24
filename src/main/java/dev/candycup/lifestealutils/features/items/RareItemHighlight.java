package dev.candycup.lifestealutils.features.items;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.ItemRenderEvent;
import dev.candycup.lifestealutils.event.listener.RenderEventListener;

/**
 * highlights rare items (netherite, custom enchants, artifacts) with increased scale.
 * <p>
 * performance: this feature is called on every item render. the isRare check
 * is done in the mixin to avoid overhead in the event system hot path.
 */
public final class RareItemHighlight implements RenderEventListener {

    /**
     * Indicates whether the rare-item scaling feature is enabled.
     *
     * @return true if rare item scaling is enabled, false otherwise.
     */
    @Override
    public boolean isEnabled() {
        return Config.isRareItemScaling();
    }

    /**
     * Specifies the event handling priority for this listener.
     *
     * @return `EventPriority.NORMAL` indicating normal ordering among event listeners.
     */
    @Override
    public EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
     * Scales the rendered item when it is marked as rare.
     *
     * @param event the item render event; if {@code event.isRare()} is true this method applies a uniform scale to the event's pose stack using the configured rare-item scale factor
     */
    @Override
    public void onItemRender(ItemRenderEvent event) {
        // only scale if the item is marked as rare by the mixin
        if (!event.isRare()) return;

        float scale = Config.getRareItemScaling();
        event.getPoseStack().scale(scale, scale, scale);
    }
}