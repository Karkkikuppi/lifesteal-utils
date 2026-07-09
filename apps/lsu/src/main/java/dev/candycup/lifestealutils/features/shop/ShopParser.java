package dev.candycup.lifestealutils.features.shop;

import dev.candycup.ui.lsu.SlotSnapshot;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShopParser {
   public static final String CATEGORIES_TITLE = "Shop | Categories";
   public static final String SELECT_AMOUNT_TITLE = "Shop | Select amount";

   private static final String SHOP_PREFIX = "Shop | ";
   private static final String CLICK_TO_BUY = "Click to buy item";
   private static final String BUY_HEADER = "Buy:";
   private static final String GO_BACK = "Go Back";
   private static final String NEXT_PAGE = "Next Page";
   private static final String PREVIOUS_PAGE = "Previous Page";
   private static final Pattern CURRENCY_LINE = Pattern.compile(".*?([0-9][0-9,]*)\\s+(Coins|XP)\\s*$", Pattern.CASE_INSENSITIVE);
   private static final Pattern AMOUNT_NAME = Pattern.compile("^([0-9][0-9,]*)x\\s+(.+)$");

   private ShopParser() {
   }

   public static ShopParseResult parse(String title, List<SlotSnapshot> shopItems, ShopView.Listings previousListings, String expectedCategoryTitle) {
      if (CATEGORIES_TITLE.equals(title)) {
         return parseCategories(title, shopItems);
      }
      if (SELECT_AMOUNT_TITLE.equals(title)) {
         return parseSelectAmount(title, shopItems, previousListings);
      }
      if (title != null && title.startsWith(SHOP_PREFIX)) {
         if (expectedCategoryTitle != null && !expectedCategoryTitle.equals(title)) {
            return fallback(ShopStabilityCode.SHOP_TITLE_MISMATCH, "expected " + expectedCategoryTitle + " but got " + title);
         }
         return parseListings(title, title.substring(SHOP_PREFIX.length()), shopItems);
      }
      return null;
   }

   public static ShopParseResult parseCategories(String title, List<SlotSnapshot> shopItems) {
      List<ShopView.CategoryButton> categories = new ArrayList<>();
      for (SlotSnapshot item : shopItems) {
         if (item.empty() || item.grayStainedGlassPane()) {
            continue;
         }

         NameStyle nameStyle = singleBoldColor(item.displayName());
         if (nameStyle == null || item.plainName().isBlank()) {
            return fallback(ShopStabilityCode.SHOP_HOME_IMPOSTORS_1, "slot " + item.slotIndex() + " had invalid category item " + item.plainName());
         }
         categories.add(new ShopView.CategoryButton(item.plainName(), nameStyle.color(), item.slotIndex(), item));
      }

      return new ShopParseResult.Valid(new ShopView.Categories(title, List.copyOf(categories)));
   }

   public static ShopParseResult parseListings(String title, String categoryName, List<SlotSnapshot> shopItems) {
      NavParse navParse = parseNavButtons(shopItems, true);
      if (navParse.fallback() != null) {
         return navParse.fallback();
      }

      List<ShopView.ListingRow> listings = new ArrayList<>();
      for (SlotSnapshot item : shopItems) {
         if (item.empty() || !containsLore(item, CLICK_TO_BUY)) {
            continue;
         }

         ListingMeta meta = parseListingMeta(item);
         if (meta.fallback() != null) {
            return meta.fallback();
         }
         listings.add(new ShopView.ListingRow(item.plainName(), meta.coinsPer(), meta.xpPer(), item.slotIndex(), item));
      }

      if (listings.isEmpty()) {
         return fallback(ShopStabilityCode.NO_LISTINGS_FOUND, "no listing items in " + title);
      }
      return new ShopParseResult.Valid(new ShopView.Listings(title, categoryName, List.copyOf(listings), navParse.navButtons()));
   }

   public static ShopParseResult parseSelectAmount(String title, List<SlotSnapshot> shopItems, ShopView.Listings previousListings) {
      int goBackSlot = uniqueNamedSlot(shopItems, GO_BACK, true, false);
      if (goBackSlot == SlotSearch.DUPLICATE) {
         return fallback(ShopStabilityCode.SHOP_DUPLICATE_NAV, "duplicate buy amount go back buttons");
      }

      List<ShopView.BuyAmount> amounts = new ArrayList<>();
      String selectedName = null;
      for (SlotSnapshot item : shopItems) {
         if (item.empty()) {
            continue;
         }
         Matcher matcher = AMOUNT_NAME.matcher(item.plainName());
         if (!matcher.matches()) {
            continue;
         }

         String itemName = matcher.group(2).trim();
         if (selectedName == null) {
            selectedName = itemName;
         }
         if (!selectedName.equals(itemName)) {
            continue;
         }
         amounts.add(new ShopView.BuyAmount(matcher.group(1) + "x", itemName, item.slotIndex(), item));
      }

      if (amounts.isEmpty()) {
         return fallback(ShopStabilityCode.NO_BUY_AMOUNTS, "select amount screen has no amount buttons");
      }

      List<ShopView.ListingRow> previousRows = previousListings == null ? List.of() : previousListings.listings();
      String categoryName = previousListings == null ? "" : previousListings.categoryName();
      return new ShopParseResult.Valid(new ShopView.SelectAmount(title, categoryName, selectedName, previousRows, List.copyOf(amounts), goBackSlot));
   }

   public static NameStyle singleBoldColor(Component component) {
      if (component == null || component.getString().isBlank()) {
         return null;
      }
      StyleAccumulator accumulator = new StyleAccumulator();
      collectVisibleStyles(component, accumulator);
      if (!accumulator.valid || !accumulator.sawText || accumulator.color == null) {
         return null;
      }
      return new NameStyle(accumulator.color);
   }

   private static void collectVisibleStyles(Component component, StyleAccumulator accumulator) {
      if (!component.getString().isBlank() && component.getSiblings().isEmpty()) {
         Style style = component.getStyle();
         TextColor textColor = style.getColor();
         if (!style.isBold() || textColor == null) {
            accumulator.valid = false;
            return;
         }
         int color = textColor.getValue();
         if (accumulator.color == null) {
            accumulator.color = color;
         } else if (accumulator.color != color) {
            accumulator.valid = false;
            return;
         }
         accumulator.sawText = true;
      }

      for (Component sibling : component.getSiblings()) {
         collectVisibleStyles(sibling, accumulator);
         if (!accumulator.valid) {
            return;
         }
      }
   }

   private static ListingMeta parseListingMeta(SlotSnapshot item) {
      boolean sawClick = false;
      boolean sawBuy = false;
      String coins = "-";
      String xp = "-";
      boolean sawCurrency = false;

      for (Component component : item.lore()) {
         String line = component.getString().trim();
         if (line.isEmpty()) {
            continue;
         }
         if (line.contains(CLICK_TO_BUY)) {
            sawClick = true;
            continue;
         }
         if (line.equals(BUY_HEADER) || line.equals("Buy")) {
            sawBuy = true;
            continue;
         }

         Matcher matcher = CURRENCY_LINE.matcher(line);
         if (matcher.matches()) {
            if (!sawBuy) {
               return new ListingMeta(null, null, fallbackWithLore(ShopStabilityCode.LISTING_META_QUESTIONABLE, "currency before Buy: in slot " + item.slotIndex(), item));
            }
            String value = matcher.group(1);
            String currency = matcher.group(2).toLowerCase(Locale.ROOT);
            if ("coins".equals(currency)) {
               coins = value;
            } else if ("xp".equals(currency)) {
               xp = value;
            }
            sawCurrency = true;
            continue;
         }

         return new ListingMeta(null, null, fallbackWithLore(ShopStabilityCode.LISTING_META_QUESTIONABLE, "unexpected lore line in slot " + item.slotIndex() + ": " + line, item));
      }

      if (!sawClick) {
         return new ListingMeta(null, null, fallbackWithLore(ShopStabilityCode.LISTING_META_QUESTIONABLE, "listing without click marker in slot " + item.slotIndex(), item));
      }
      if (!sawBuy || !sawCurrency) {
         return new ListingMeta(null, null, fallbackWithLore(ShopStabilityCode.SHOP_PRICE_PARSE_FAILED, "listing has no parseable Buy: currencies in slot " + item.slotIndex(), item));
      }
      return new ListingMeta(coins, xp, null);
   }

   private static boolean containsLore(SlotSnapshot item, String needle) {
      for (Component line : item.lore()) {
         if (line.getString().contains(needle)) {
            return true;
         }
      }
      return false;
   }

   private static NavParse parseNavButtons(List<SlotSnapshot> shopItems, boolean requireBack) {
      int goBackSlot = uniqueNamedSlot(shopItems, GO_BACK, true, false);
      int previousPageSlot = uniqueNamedSlot(shopItems, PREVIOUS_PAGE, false, true);
      int nextPageSlot = uniqueNamedSlot(shopItems, NEXT_PAGE, false, true);
      if (goBackSlot == SlotSearch.DUPLICATE || previousPageSlot == SlotSearch.DUPLICATE || nextPageSlot == SlotSearch.DUPLICATE) {
         return new NavParse(null, fallback(ShopStabilityCode.SHOP_DUPLICATE_NAV, "duplicate shop navigation button"));
      }
      if (requireBack && goBackSlot < 0) {
         return new NavParse(null, fallback(ShopStabilityCode.SHOP_MISSING_BACK_BUTTON, "listing screen has no barrier go back button"));
      }
      return new NavParse(new ShopView.NavButtons(goBackSlot, previousPageSlot, nextPageSlot), null);
   }

   private static int uniqueNamedSlot(List<SlotSnapshot> shopItems, String name, boolean barrier, boolean redstoneTorch) {
      int found = SlotSearch.NOT_FOUND;
      for (SlotSnapshot item : shopItems) {
         if (item.empty() || !name.equalsIgnoreCase(item.plainName())) {
            continue;
         }
         if (barrier && !item.barrier()) {
            continue;
         }
         if (redstoneTorch && !item.redstoneTorch()) {
            continue;
         }
         if (found >= 0) {
            return SlotSearch.DUPLICATE;
         }
         found = item.slotIndex();
      }
      return found;
   }

   private static ShopParseResult.Fallback fallback(ShopStabilityCode code, String detail) {
      return new ShopParseResult.Fallback(code, detail);
   }

   private static ShopParseResult.Fallback fallbackWithLore(ShopStabilityCode code, String detail, SlotSnapshot item) {
      return fallback(code, detail + "; item='" + item.plainName() + "'; lore=" + loreDebugString(item));
   }

   private static String loreDebugString(SlotSnapshot item) {
      if (item.lore().isEmpty()) {
         return "[]";
      }

      List<String> lines = new ArrayList<>();
      for (Component component : item.lore()) {
         lines.add(component.getString());
      }
      return lines.toString();
   }

   public record NameStyle(int color) {
   }

   private record ListingMeta(String coinsPer, String xpPer, ShopParseResult.Fallback fallback) {
   }

   private record NavParse(ShopView.NavButtons navButtons, ShopParseResult.Fallback fallback) {
   }

   private static final class StyleAccumulator {
      private boolean valid = true;
      private boolean sawText;
      private Integer color;
   }

   private static final class SlotSearch {
      private static final int NOT_FOUND = -1;
      private static final int DUPLICATE = -2;
   }
}
