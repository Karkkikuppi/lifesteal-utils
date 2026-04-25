package dev.candycup.lifestealutils.features.prestige;

import dev.candycup.lifestealutils.api.PersistentKnowledgeController;
import dev.candycup.lifestealutils.api.PrestigeUtils;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PrestigeMenuListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/prestige");
   private static final String PRESTIGE_MENU_TITLE = "Heart Prestige";
   private static final String ENHANCEMENTS_BOOK_NAME = "Current Enhancements";

   public PrestigeMenuListener() {
      LifestealUtilsEvents.CONTAINER_CONTENT_SET.register(event -> {
         String title = event.getScreenTitle();
         LOGGER.debug("CONTAINER_CONTENT_SET fired: title='{}', slots={}", title, event.getMenu().slots.size());

         if (!PRESTIGE_MENU_TITLE.equals(title)) return;

         LOGGER.info("Heart Prestige menu detected! Saving enhancements from book...");

         for (Slot slot : event.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            LOGGER.debug("  slot {}: item={}", slot.index, stack.getItem());

            if (stack.getItem() != Items.BOOK) continue;

            Component customName = stack.get(DataComponents.CUSTOM_NAME);
            if (customName == null) {
               LOGGER.debug("  slot {}: book has no custom name, skipping", slot.index);
               continue;
            }

            String name = customName.getString();
            LOGGER.debug("  slot {}: book custom name='{}'", slot.index, name);

            if (!ENHANCEMENTS_BOOK_NAME.equals(name)) continue;

            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null || lore.lines().isEmpty()) {
               LOGGER.warn("Found '{}' book but it has no lore — nothing to parse", ENHANCEMENTS_BOOK_NAME);
               return;
            }

            LOGGER.debug("  book has {} lore lines", lore.lines().size());
            for (Component line : lore.lines()) {
               LOGGER.debug("    lore: '{}'", line.getString());
            }

            Map<String, Float> enhancements = PrestigeUtils.parseLoreLines(lore.lines());
            if (enhancements.isEmpty()) {
               LOGGER.warn("Parsed '{}' lore but found no ∙-prefixed enhancement lines", ENHANCEMENTS_BOOK_NAME);
            } else {
               LOGGER.info("Parsed {} prestige enhancements: {}", enhancements.size(), enhancements);
            }

            PersistentKnowledgeController.setPrestigeEnhancements(enhancements);
            return;
         }

         LOGGER.warn("Heart Prestige menu had no book named '{}' in any slot", ENHANCEMENTS_BOOK_NAME);
      });
   }
}
