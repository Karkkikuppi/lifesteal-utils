package dev.candycup.lifestealutils.features.items;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ItemRenderEvent;

/**
 * highlights rare items (netherite, custom enchants, artifacts) with increased scale.
 * <p>
 * performance: this feature is called on every item render. the isRare check
 * is done in the mixin to avoid overhead in the event system hot path.
 */
public final class RareItemHighlight {

   public RareItemHighlight() {
      LifestealUtilsEvents.ITEM_RENDER.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onItemRender(event);
      });
   }

   public boolean isEnabled() {
      return Config.isRareItemScaleEnabled();
   }

   public void onItemRender(ItemRenderEvent event) {
      // only scale if the item is marked as rare by the mixin
      if (!event.isRare()) return;

      float scale = Config.getRareItemScale();
      event.getPoseStack().scale(scale, scale, scale);
   }
}
