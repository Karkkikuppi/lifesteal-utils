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

   @Override
   public boolean isEnabled() {
      return Config.isRareItemScaling();
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   @Override
   public void onItemRender(ItemRenderEvent event) {
      // only scale if the item is marked as rare by the mixin
      if (!event.isRare()) return;

      float scale = Config.getRareItemScaling();
      event.getPoseStack().scale(scale, scale, scale);
   }
}
