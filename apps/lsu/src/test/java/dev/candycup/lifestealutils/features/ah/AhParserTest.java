package dev.candycup.lifestealutils.features.ah;

import dev.candycup.ui.lsu.SlotSnapshot;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AhParserTest {
   @Test
   void auctionListingMetaParsesPriceSellerTimeAndBidState() {
      List<SlotSnapshot> items = baseAuctionItems();
      items.set(10, item(10, "Diamond Sword", List.of(
              Component.literal("Seller: Candycup"),
              Component.literal("Highest Bid: 25,000 Coins"),
              Component.literal("Time Remaining: 1d, 2h, 3m")
      )));

      AhParseResult result = AhParser.parseItems(AhParser.AUCTION_ITEMS_TITLE, items, null);

      assertInstanceOf(AhParseResult.Valid.class, result);
      AhView.Items view = (AhView.Items) ((AhParseResult.Valid) result).view();
      assertEquals(1, view.listings().size());
      AhView.AuctionMeta meta = view.listings().getFirst().meta();
      assertTrue(meta.bidAuction());
      assertEquals("Candycup", meta.seller());
      assertEquals("25K Coins", meta.price());
      assertEquals("1d 2h", meta.compactTime());
   }

   @Test
   void currencyAmountsCompactThousandsMillionsAndBillions() {
      assertEquals("999 Coins", AhParser.compactCurrency("999 Coins"));
      assertEquals("10K Coins", AhParser.compactCurrency("10,000 Coins"));
      assertEquals("7.5M Coins", AhParser.compactCurrency("7,500,000 Coins"));
      assertEquals("100M Coins", AhParser.compactCurrency("100,000,000 Coins"));
      assertEquals("1.2B Coins", AhParser.compactCurrency("1,200,000,000 Coins"));
   }

   @Test
   void missingListingMetaUsesPlaceholdersAndWarning() {
      List<SlotSnapshot> items = baseAuctionItems();
      items.set(10, item(10, "Mystery Box", List.of(Component.literal("Odd lore"))));

      AhParseResult result = AhParser.parseItems(AhParser.AUCTION_ITEMS_TITLE, items, null);

      assertInstanceOf(AhParseResult.Valid.class, result);
      AhView.Items view = (AhView.Items) ((AhParseResult.Valid) result).view();
      assertEquals("Unknown Seller", view.listings().getFirst().meta().seller());
      assertEquals("Unknown Price", view.listings().getFirst().meta().price());
      assertFalse(view.warnings().isEmpty());
   }

   @Test
   void renderableSlotsExcludeControlBorderRows() {
      List<Integer> slots = AhParser.renderableAuctionSlots(54);

      assertFalse(slots.contains(0));
      assertFalse(slots.contains(8));
      assertFalse(slots.contains(45));
      assertTrue(slots.contains(10));
      assertTrue(slots.contains(43));
   }

   @Test
   void sortLoreParsesOptionsAndSelectedOption() {
      List<SlotSnapshot> items = baseAuctionItems();
      items.set(0, item(0, "Sort Items", List.of(
              Component.literal("→ Recently Listed"),
              Component.literal("∙ Lowest Price"),
              Component.literal("Click to cycle")
      )));
      items.set(10, listing(10));

      AhView.Items view = validItems(items);

      assertEquals(List.of("Recently Listed", "Lowest Price"), view.state().sortState().options());
      assertEquals(0, view.state().sortState().selectedIndex());
   }

   @Test
   void searchStateParsesActiveClearableQuery() {
      List<SlotSnapshot> items = baseAuctionItems();
      items.set(2, item(2, "Search Items", List.of(
              Component.literal("Searching for: diamond"),
              Component.literal("Click to clear search")
      )));
      items.set(10, listing(10));

      AhView.Items view = validItems(items);

      assertEquals("diamond", view.state().searchState().activeQuery());
      assertTrue(view.state().searchState().active());
   }

   @Test
   void filterEditParsesSelectableOptions() {
      List<SlotSnapshot> items = baseAuctionItems();
      items.set(10, item(10, "Weapons", List.of(Component.literal("Click to select this option"))));
      items.set(11, item(11, "Armor", List.of(Component.literal("Click to un-select this option"))));

      AhParseResult result = AhParser.parseFilter(AhParser.FILTER_TITLE, items, null);

      assertInstanceOf(AhParseResult.Valid.class, result);
      AhView.FilterEdit view = (AhView.FilterEdit) ((AhParseResult.Valid) result).view();
      assertEquals(2, view.state().filterState().options().size());
      assertTrue(view.state().filterState().options().get(1).selected());
   }

   @Test
   void noListingsFallsBack() {
      AhParseResult result = AhParser.parseItems(AhParser.AUCTION_ITEMS_TITLE, baseAuctionItems(), null);

      assertInstanceOf(AhParseResult.Fallback.class, result);
      assertEquals(AhStabilityCode.AH_NO_LISTINGS_FOUND, ((AhParseResult.Fallback) result).code());
   }

   private static AhView.Items validItems(List<SlotSnapshot> items) {
      AhParseResult result = AhParser.parseItems(AhParser.AUCTION_ITEMS_TITLE, items, null);
      assertInstanceOf(AhParseResult.Valid.class, result);
      return (AhView.Items) ((AhParseResult.Valid) result).view();
   }

   private static List<SlotSnapshot> baseAuctionItems() {
      List<SlotSnapshot> items = new ArrayList<>();
      for (int i = 0; i < 54; i++) {
         items.add(empty(i));
      }
      items.set(2, item(2, "Search Items", List.of()));
      return items;
   }

   private static SlotSnapshot listing(int slot) {
      return item(slot, "Diamond", List.of(
              Component.literal("Seller: Candycup"),
              Component.literal("Price: 1,000 Coins"),
              Component.literal("Time Remaining: 3h, 2m")
      ));
   }

   private static SlotSnapshot item(int slot, String name, List<Component> lore) {
      return new SlotSnapshot(slot, null, Component.literal(name), name, lore, false, false, false, false);
   }

   private static SlotSnapshot empty(int slot) {
      return new SlotSnapshot(slot, null, Component.empty(), "", List.of(), true, false, false, false);
   }
}
