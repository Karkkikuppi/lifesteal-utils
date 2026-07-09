package dev.candycup.lifestealutils.features.shop;

import dev.candycup.ui.lsu.SlotGraceCache;
import dev.candycup.ui.lsu.SlotSnapshot;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SlotGraceCacheTest {
   @Test
   void usesCachedSnapshotWhenLiveSlotIsEmptyBeforeReplacementArrives() {
      SlotGraceCache cache = new SlotGraceCache();
      SlotSnapshot clicked = item(3, "Diamond");
      SlotSnapshot empty = empty(3);

      cache.capture(12, "Shop | Armor", clicked);

      assertEquals(clicked.plainName(), cache.resolve(12, "Shop | Armor", empty).plainName());
   }

   @Test
   void keepsCachedSnapshotUntilReplacementArrives() {
      SlotGraceCache cache = new SlotGraceCache();
      SlotSnapshot clicked = item(3, "Diamond");
      SlotSnapshot empty = empty(3);

      cache.capture(12, "Shop | Armor", clicked);

      assertEquals(clicked.plainName(), cache.resolve(12, "Shop | Armor", empty).plainName());
   }

   @Test
   void clearsCachedSnapshotWhenLiveSlotReceivesReplacement() {
      SlotGraceCache cache = new SlotGraceCache();
      SlotSnapshot clicked = item(3, "Diamond");
      SlotSnapshot replacement = item(3, "Netherite");
      SlotSnapshot empty = empty(3);

      cache.capture(12, "Shop | Armor", clicked);

      assertSame(replacement, cache.resolve(12, "Shop | Armor", replacement));
      assertSame(empty, cache.resolve(12, "Shop | Armor", empty));
   }

   @Test
   void ignoresCachedSnapshotWhenScopeChanges() {
      SlotGraceCache cache = new SlotGraceCache();
      SlotSnapshot clicked = item(3, "Diamond");
      SlotSnapshot empty = empty(3);

      cache.capture(12, "Shop | Armor", clicked);

      assertSame(empty, cache.resolve(12, "Shop | Blocks", empty));
   }

   private static SlotSnapshot item(int slot, String plainName) {
      return new SlotSnapshot(slot, null, Component.literal(plainName), plainName, List.of(), false, false, false, false);
   }

   private static SlotSnapshot empty(int slot) {
      return new SlotSnapshot(slot, null, Component.empty(), "", List.of(), true, false, false, false);
   }
}
