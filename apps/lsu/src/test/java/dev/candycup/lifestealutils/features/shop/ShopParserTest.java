package dev.candycup.lifestealutils.features.shop;

import dev.candycup.ui.lsu.SlotSnapshot;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShopParserTest {
   @Test
   void categoryItemsRequireBoldSingleColorNames() {
      SlotSnapshot valid = item(0, "Blocks", Component.literal("Blocks").withStyle(style -> style.withBold(true).withColor(0x44AAFF)), List.of(), false, false, false);

      ShopParseResult result = ShopParser.parseCategories(ShopParser.CATEGORIES_TITLE, List.of(valid));

      assertInstanceOf(ShopParseResult.Valid.class, result);
      ShopView.Categories categories = (ShopView.Categories) ((ShopParseResult.Valid) result).view();
      assertEquals(1, categories.categories().size());
      assertEquals("Blocks", categories.categories().getFirst().name());
   }

   @Test
   void categoryImpostorsFallBackWithHomeCode() {
      SlotSnapshot impostor = item(0, "Plain", Component.literal("Plain"), List.of(), false, false, false);

      ShopParseResult result = ShopParser.parseCategories(ShopParser.CATEGORIES_TITLE, List.of(impostor));

      assertFallback(result, ShopStabilityCode.SHOP_HOME_IMPOSTORS_1);
   }

   @Test
   void listingLoreParsesCoinsAndXp() {
      SlotSnapshot back = item(0, "Go Back", Component.literal("Go Back"), List.of(), false, true, false);
      SlotSnapshot listing = item(1, "Diamond", Component.literal("Diamond"), List.of(
              Component.literal("Click to buy item"),
              Component.literal("Buy:"),
              Component.literal("⬝ 2,000 Coins"),
              Component.literal("⬝ 1,000 XP")
      ), false, false, false);

      ShopParseResult result = ShopParser.parseListings("Shop | Blocks", "Blocks", List.of(back, listing));

      assertInstanceOf(ShopParseResult.Valid.class, result);
      ShopView.Listings listings = (ShopView.Listings) ((ShopParseResult.Valid) result).view();
      assertEquals("2,000", listings.listings().getFirst().coinsPer());
      assertEquals("1,000", listings.listings().getFirst().xpPer());
   }

   @Test
   void listingLoreAcceptsLiveServerBuyHeaderAndPrefixedClickMarker() {
      SlotSnapshot back = item(0, "Go Back", Component.literal("Go Back"), List.of(), false, true, false);
      SlotSnapshot listing = item(10, "Black Concrete", Component.literal("Black Concrete"), List.of(
              Component.literal(""),
              Component.literal("Buy"),
              Component.literal("ÔÇó 100 Coins"),
              Component.literal(""),
              Component.literal("ÔåÆ Click to buy item")
      ), false, false, false);

      ShopParseResult result = ShopParser.parseListings("Shop | Decorations", "Decorations", List.of(back, listing));

      assertInstanceOf(ShopParseResult.Valid.class, result);
      ShopView.Listings listings = (ShopView.Listings) ((ShopParseResult.Valid) result).view();
      assertEquals("100", listings.listings().getFirst().coinsPer());
      assertEquals("-", listings.listings().getFirst().xpPer());
   }

   @Test
   void questionableListingLoreFallsBack() {
      SlotSnapshot back = item(0, "Go Back", Component.literal("Go Back"), List.of(), false, true, false);
      SlotSnapshot listing = item(1, "Diamond", Component.literal("Diamond"), List.of(
              Component.literal("Click to buy item"),
              Component.literal("Limited time"),
              Component.literal("Buy:"),
              Component.literal("⬝ 2,000 Coins")
      ), false, false, false);

      ShopParseResult result = ShopParser.parseListings("Shop | Blocks", "Blocks", List.of(back, listing));

      assertFallback(result, ShopStabilityCode.LISTING_META_QUESTIONABLE);
      assertTrue(((ShopParseResult.Fallback) result).detail().contains("Limited time"));
      assertTrue(((ShopParseResult.Fallback) result).detail().contains("lore=["));
   }

   @Test
   void noListingsFallsBack() {
      SlotSnapshot back = item(0, "Go Back", Component.literal("Go Back"), List.of(), false, true, false);

      ShopParseResult result = ShopParser.parseListings("Shop | Blocks", "Blocks", List.of(back));

      assertFallback(result, ShopStabilityCode.NO_LISTINGS_FOUND);
   }

   @Test
   void buyAmountsParseFromAmountPrefix() {
      SlotSnapshot back = item(0, "Go Back", Component.literal("Go Back"), List.of(), false, true, false);
      SlotSnapshot amount = item(1, "64x Enchanted Book", Component.literal("64x Enchanted Book"), List.of(), false, false, false);

      ShopParseResult result = ShopParser.parseSelectAmount(ShopParser.SELECT_AMOUNT_TITLE, List.of(back, amount), null);

      assertInstanceOf(ShopParseResult.Valid.class, result);
      ShopView.SelectAmount selectAmount = (ShopView.SelectAmount) ((ShopParseResult.Valid) result).view();
      assertEquals("Enchanted Book", selectAmount.selectedItemName());
      assertEquals("64x", selectAmount.amounts().getFirst().label());
   }

   @Test
   void missingBuyAmountsFallsBack() {
      SlotSnapshot back = item(0, "Go Back", Component.literal("Go Back"), List.of(), false, true, false);

      ShopParseResult result = ShopParser.parseSelectAmount(ShopParser.SELECT_AMOUNT_TITLE, List.of(back), null);

      assertFallback(result, ShopStabilityCode.NO_BUY_AMOUNTS);
   }

   @Test
   void selectAmountDoesNotRequireGoBackBarrier() {
      SlotSnapshot amount = item(1, "1x Black Concrete", Component.literal("1x Black Concrete"), List.of(), false, false, false);

      ShopParseResult result = ShopParser.parseSelectAmount(ShopParser.SELECT_AMOUNT_TITLE, List.of(amount), null);

      assertInstanceOf(ShopParseResult.Valid.class, result);
      ShopView.SelectAmount selectAmount = (ShopView.SelectAmount) ((ShopParseResult.Valid) result).view();
      assertEquals(-1, selectAmount.goBackSlot());
      assertEquals("1x", selectAmount.amounts().getFirst().label());
   }

   @Test
   void selectAmountCarriesPreviousListingsAndCategoryName() {
      ShopView.ListingRow previousRow = new ShopView.ListingRow("Diamond Helmet", "1,000", "-", 3, item(3, "Diamond Helmet", Component.literal("Diamond Helmet"), List.of(), false, false, false));
      ShopView.Listings previousListings = new ShopView.Listings("Shop | Armor", "Armor", List.of(previousRow), new ShopView.NavButtons(0, -1, -1));
      SlotSnapshot amount = item(1, "1x Diamond Helmet", Component.literal("1x Diamond Helmet"), List.of(), false, false, false);

      ShopParseResult result = ShopParser.parseSelectAmount(ShopParser.SELECT_AMOUNT_TITLE, List.of(amount), previousListings);

      assertInstanceOf(ShopParseResult.Valid.class, result);
      ShopView.SelectAmount selectAmount = (ShopView.SelectAmount) ((ShopParseResult.Valid) result).view();
      assertEquals("Armor", selectAmount.categoryName());
      assertEquals(1, selectAmount.previousListings().size());
      assertEquals("Diamond Helmet", selectAmount.previousListings().getFirst().name());
   }

   private static void assertFallback(ShopParseResult result, ShopStabilityCode code) {
      assertInstanceOf(ShopParseResult.Fallback.class, result);
      assertEquals(code, ((ShopParseResult.Fallback) result).code());
   }

   private static SlotSnapshot item(int slot, String plainName, Component displayName, List<Component> lore, boolean grayPane, boolean barrier, boolean redstoneTorch) {
      return new SlotSnapshot(slot, null, displayName, plainName, lore, false, grayPane, barrier, redstoneTorch);
   }
}
