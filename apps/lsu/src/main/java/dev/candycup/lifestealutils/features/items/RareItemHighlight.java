package dev.candycup.lifestealutils.features.items;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.ConfigurableFloat;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ItemRenderEvent;
import lombok.Getter;
import lombok.Setter;

/**
 * highlights rare items (netherite, custom enchants, artifacts) with increased scale.
 * <p>
 * performance: this feature is called on every item render. the isRare check
 * is done in the mixin to avoid overhead in the event system hot path.
 */
public final class RareItemHighlight {
    @Getter
    @Setter
    @SerialEntry(comment = "Enable increased scale for rare items such as neth and custom enchants.")
    @ConfigurableBoolean(location = "qol.scaling.rareitemscaleenabled")
    private static boolean rareItemScaleEnabled = true;

    @Getter
    @Setter
    @SerialEntry(comment = "Increased scale of the rare items.")
    @ConfigurableFloat(location = "qol.scaling.rareitemscale", min = 1.0f, max = 5.0f)
    private static float rareItemScale = 2.0f;

   public RareItemHighlight() {
      LifestealUtilsEvents.ITEM_RENDER.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onItemRender(event);
      });
   }

   public boolean isEnabled() {
       return rareItemScaleEnabled;
   }

   public void onItemRender(ItemRenderEvent event) {
      // only scale if the item is marked as rare by the mixin
      if (!event.isRare()) return;

       float scale = rareItemScale;
      event.getPoseStack().scale(scale, scale, scale);
   }
}
