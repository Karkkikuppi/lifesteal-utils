package dev.candycup.lifestealutils.features.ah;

import dev.candycup.ui.Text;
import dev.candycup.ui.lsu.SlotSnapshot;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AhParser {
   public static final String AUCTION_ITEMS_TITLE = "Auction | Items";
   public static final String FILTER_TITLE = "Filter";

   private static final String SORT_ITEM_NAME = "Sort Items";
   private static final String FILTER_ITEM_NAME = "Filter Items";
   private static final String SEARCH_ITEM_NAME = "Search Items";
   private static final String CONFIRM_BUTTON_NAME = "Confirm";
   private static final String GO_BACK_BUTTON_NAME = "Go Back";
   private static final String NEXT_PAGE_NAME = "Next Page";
   private static final String PREVIOUS_PAGE_NAME = "Previous Page";
   private static final String ALT_PREVIOUS_PAGE_NAME = "Last Page";
   private static final String CLAIM_ITEMS_NAME = "Claim Items";
   private static final String YOUR_LISTINGS_NAME = "Your Listings";

   private AhParser() {
   }

   public static AhParseResult parse(String title, List<SlotSnapshot> items, AhView.AhState previousState, boolean allowFilterView) {
      if (AUCTION_ITEMS_TITLE.equals(title)) {
         return parseItems(title, items, previousState);
      }
      if (FILTER_TITLE.equals(title) && allowFilterView) {
         return parseFilter(title, items, previousState);
      }
      if (title != null && (title.startsWith("Auction") || FILTER_TITLE.equals(title))) {
         return fallback(AhStabilityCode.AH_TITLE_UNSUPPORTED, "unsupported auction title " + title);
      }
      return null;
   }

   public static AhParseResult parseItems(String title, List<SlotSnapshot> items, AhView.AhState previousState) {
      AhView.AhState state = parseState(items, previousState, false);
      if (state.searchState().searchSlot() < 0) {
         return fallback(AhStabilityCode.AH_SEARCH_CONTROL_MISSING, "auction items screen has no Search Items control");
      }

      List<String> warnings = new ArrayList<>();
      List<AhView.AuctionListing> listings = new ArrayList<>();
      for (int slot : renderableAuctionSlots(items.size())) {
         if (slot < 0 || slot >= items.size()) {
            continue;
         }
         SlotSnapshot item = items.get(slot);
         if (item.empty()) {
            continue;
         }
         AhView.AuctionMeta meta = parseMeta(item, warnings);
         listings.add(new AhView.AuctionListing(item.plainName(), meta, item.slotIndex(), item));
      }

      if (listings.isEmpty()) {
         return fallback(AhStabilityCode.AH_NO_LISTINGS_FOUND, "auction items screen has no renderable listings");
      }
      return new AhParseResult.Valid(new AhView.Items(title, state, List.copyOf(listings), List.copyOf(warnings)));
   }

   public static AhParseResult parseFilter(String title, List<SlotSnapshot> items, AhView.AhState previousState) {
      AhView.AhState state = parseState(items, previousState, true);
      if (state.filterState().options().isEmpty()) {
         return fallback(AhStabilityCode.AH_FILTER_OPTIONS_MISSING, "filter screen has no selectable options");
      }
      return new AhParseResult.Valid(new AhView.FilterEdit(title, state, List.of()));
   }

   public static List<Integer> renderableAuctionSlots(int topContainerSlotCount) {
      List<Integer> slots = new ArrayList<>();
      int rows = topContainerSlotCount / 9;
      if (rows <= 2) {
         return slots;
      }
      for (int row = 1; row < rows - 1; row++) {
         for (int col = 1; col <= 7; col++) {
            int slot = row * 9 + col;
            if (slot < topContainerSlotCount) {
               slots.add(slot);
            }
         }
      }
      return slots;
   }

   private static AhView.AhState parseState(List<SlotSnapshot> items, AhView.AhState previousState, boolean filterEdit) {
      AhView.AhControls controls = parseControls(items);
      AhView.AhSortState sortState = parseSort(items, previousState == null ? null : previousState.sortState(), filterEdit);
      AhView.AhFilterState filterState = parseFilters(items, previousState == null ? null : previousState.filterState(), filterEdit);
      AhView.AhSearchState searchState = parseSearch(items);
      AhView.AhSidebarActions actions = new AhView.AhSidebarActions(findNamedControlSlot(items, CLAIM_ITEMS_NAME), findNamedControlSlot(items, YOUR_LISTINGS_NAME));
      return new AhView.AhState(controls, sortState, filterState, searchState, actions);
   }

   private static AhView.AhControls parseControls(List<SlotSnapshot> items) {
      int next = uniqueNamedTorch(items, NEXT_PAGE_NAME);
      int previous = uniqueNamedTorch(items, PREVIOUS_PAGE_NAME, ALT_PREVIOUS_PAGE_NAME);
      return new AhView.AhControls(next, previous);
   }

   private static AhView.AhSortState parseSort(List<SlotSnapshot> items, AhView.AhSortState previousState, boolean filterEdit) {
      int slot = findNamedControlSlot(items, SORT_ITEM_NAME);
      List<String> options = new ArrayList<>();
      int selectedIndex = -1;
      if (slot >= 0) {
         SlotSnapshot item = itemAt(items, slot);
         for (Component lineComponent : item.lore()) {
            String line = lineComponent.getString().trim();
            if (line.isEmpty()) {
               continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("click to toggle") || lower.contains("click to cycle")) {
               continue;
            }
            boolean selected = hasSelectedPrefix(line);
            if (!selected && !hasOptionPrefix(line)) {
               continue;
            }
            String label = stripPrefix(line);
            if (label.isBlank()) {
               continue;
            }
            options.add(label);
            if (selected) {
               selectedIndex = options.size() - 1;
            }
         }
      }
      if (options.isEmpty() && previousState != null) {
         return new AhView.AhSortState(slot, previousState.options(), previousState.selectedIndex());
      }
      if (selectedIndex < 0 && previousState != null && previousState.selectedIndex() >= 0 && previousState.selectedIndex() < options.size()) {
         selectedIndex = previousState.selectedIndex();
      }
      return new AhView.AhSortState(slot, List.copyOf(options), selectedIndex);
   }

   private static AhView.AhSearchState parseSearch(List<SlotSnapshot> items) {
      int slot = findNamedControlSlot(items, SEARCH_ITEM_NAME);
      String query = null;
      boolean clearable = false;
      if (slot >= 0) {
         SlotSnapshot item = itemAt(items, slot);
         for (Component lineComponent : item.lore()) {
            String line = lineComponent.getString().trim();
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("searching for:")) {
               query = line.substring("searching for:".length()).trim();
            }
            if (lower.contains("click to clear search")) {
               clearable = true;
            }
         }
      }
      if (query != null && query.equalsIgnoreCase("nothing")) {
         query = "";
      }
      if (!clearable || query == null || query.isBlank()) {
         query = null;
      }
      return new AhView.AhSearchState(slot, query, clearable && query != null);
   }

   private static AhView.AhFilterState parseFilters(List<SlotSnapshot> items, AhView.AhFilterState previousState, boolean filterEdit) {
      int editSlot = findNamedControlSlot(items, FILTER_ITEM_NAME);
      int confirmSlot = findNamedTopSlot(items, CONFIRM_BUTTON_NAME);
      int goBackSlot = findNamedTopSlot(items, GO_BACK_BUTTON_NAME);
      if (!filterEdit) {
         List<AhView.AhFilterOption> options = previousState == null ? List.of() : previousState.options();
         return new AhView.AhFilterState(editSlot, confirmSlot, goBackSlot, options, anySelected(options));
      }

      Map<String, AhView.AhFilterOption> parsed = new LinkedHashMap<>();
      for (SlotSnapshot item : items) {
         if (item.empty()) {
            continue;
         }
         boolean selectable = false;
         boolean selected = false;
         for (Component lineComponent : item.lore()) {
            String lower = lineComponent.getString().trim().toLowerCase(Locale.ROOT);
            if (lower.contains("click to select this option")) {
               selectable = true;
            }
            if (lower.contains("click to un-select this option")) {
               selectable = true;
               selected = true;
            }
         }
         if (selectable && !item.plainName().isBlank()) {
            parsed.put(item.plainName(), new AhView.AhFilterOption(item.plainName(), item.slotIndex(), selected));
         }
      }
      if (parsed.isEmpty() && previousState != null) {
         return new AhView.AhFilterState(editSlot, confirmSlot, goBackSlot, previousState.options(), previousState.anySelected());
      }
      List<AhView.AhFilterOption> options = List.copyOf(parsed.values());
      return new AhView.AhFilterState(editSlot, confirmSlot, goBackSlot, options, anySelected(options));
   }

   private static AhView.AuctionMeta parseMeta(SlotSnapshot item, List<String> warnings) {
      String seller = null;
      String timeRemaining = null;
      String price = null;
      boolean bidAuction = false;
      for (Component lineComponent : item.lore()) {
         String line = lineComponent.getString();
         String trimmed = line == null ? "" : line.trim();
         String lower = trimmed.toLowerCase(Locale.ROOT);
         if (lower.startsWith("seller:")) {
            seller = trimmed.substring("seller:".length()).trim();
         } else if (lower.startsWith("price:")) {
            price = trimmed.substring("price:".length()).trim();
         } else if (lower.startsWith("highest bid:")) {
            bidAuction = true;
            price = trimmed.substring("highest bid:".length()).trim();
         } else if (lower.startsWith("time remaining:")) {
            timeRemaining = trimmed.substring("time remaining:".length()).trim();
         }
      }
      boolean missing = seller == null || seller.isBlank() || timeRemaining == null || timeRemaining.isBlank() || price == null || price.isBlank();
      if (missing) {
         warnings.add("slot " + item.slotIndex() + " item='" + item.plainName() + "' lore=" + loreDebugString(item));
      }
      String safeSeller = seller == null || seller.isBlank() ? "Unknown Seller" : seller;
      String safeTime = timeRemaining == null || timeRemaining.isBlank() ? "Unknown" : timeRemaining;
      String safePrice = price == null || price.isBlank() ? "Unknown Price" : compactCurrency(price);
      return new AhView.AuctionMeta(safeSeller, safeTime, compactTime(safeTime), safePrice, bidAuction, missing);
   }

   public static String compactCurrency(String raw) {
      if (raw == null || raw.isBlank()) {
         return raw;
      }
      String value = raw.trim();
      int start = -1;
      int end = -1;
      for (int i = 0; i < value.length(); i++) {
         char character = value.charAt(i);
         if (Character.isDigit(character)) {
            start = i;
            end = i + 1;
            break;
         }
      }
      if (start < 0) {
         return value;
      }
      while (end < value.length()) {
         char character = value.charAt(end);
         if (!Character.isDigit(character) && character != ',' && character != '.') {
            break;
         }
         end++;
      }
      String numericText = value.substring(start, end).replace(",", "");
      double amount;
      try {
         amount = Double.parseDouble(numericText);
      } catch (NumberFormatException ignored) {
         return value;
      }
      if (amount < 1_000D) {
         return value;
      }

      String compact = Text.compactAmount(amount);
      return value.substring(0, start) + compact + value.substring(end);
   }

   public static String compactTime(String raw) {
      if (raw == null || raw.isBlank()) {
         return "Unknown";
      }
      int days = -1;
      int hours = -1;
      int minutes = -1;
      int seconds = -1;
      for (String part : raw.toLowerCase(Locale.ROOT).split(",")) {
         String token = part.trim();
         int value = 0;
         int idx = 0;
         while (idx < token.length() && Character.isDigit(token.charAt(idx))) {
            value = value * 10 + token.charAt(idx) - '0';
            idx++;
         }
         if (idx == 0 || idx >= token.length()) {
            continue;
         }
         switch (token.charAt(idx)) {
            case 'd' -> days = value;
            case 'h' -> hours = value;
            case 'm' -> minutes = value;
            case 's' -> seconds = value;
            default -> {
            }
         }
      }
      if (days >= 0) {
         return days + "d " + Math.max(0, hours) + "h";
      }
      if (hours >= 0) {
         return hours + "h " + Math.max(0, minutes) + "m";
      }
      if (minutes >= 0) {
         return minutes + "m " + Math.max(0, seconds) + "s";
      }
      if (seconds >= 0) {
         return "0m " + seconds + "s";
      }
      String[] fallback = raw.replace(',', ' ').trim().split("\\s+");
      return fallback.length >= 2 ? fallback[0] + " " + fallback[1] : raw.trim();
   }

   private static int findNamedControlSlot(List<SlotSnapshot> items, String name) {
      int rows = items.size() / 9;
      if (rows <= 0) {
         return -1;
      }
      int[] candidates = rows == 1 ? new int[]{0} : new int[]{0, rows - 1};
      for (int row : candidates) {
         int start = row * 9;
         for (int col = 0; col < 9 && start + col < items.size(); col++) {
            SlotSnapshot item = items.get(start + col);
            if (!item.empty() && name.equals(item.plainName())) {
               return item.slotIndex();
            }
         }
      }
      return -1;
   }

   private static int findNamedTopSlot(List<SlotSnapshot> items, String name) {
      for (SlotSnapshot item : items) {
         if (!item.empty() && name.equals(item.plainName())) {
            return item.slotIndex();
         }
      }
      return -1;
   }

   private static int uniqueNamedTorch(List<SlotSnapshot> items, String... names) {
      int found = -1;
      for (SlotSnapshot item : items) {
         if (item.empty() || !item.redstoneTorch()) {
            continue;
         }
         for (String name : names) {
            if (!name.equals(item.plainName())) {
               continue;
            }
            if (found >= 0) {
               return -2;
            }
            found = item.slotIndex();
         }
      }
      return found;
   }

   private static SlotSnapshot itemAt(List<SlotSnapshot> items, int slot) {
      for (SlotSnapshot item : items) {
         if (item.slotIndex() == slot) {
            return item;
         }
      }
      return new SlotSnapshot(slot, null, Component.empty(), "", List.of(), true, false, false, false);
   }

   private static boolean anySelected(List<AhView.AhFilterOption> options) {
      for (AhView.AhFilterOption option : options) {
         if (option.selected()) {
            return true;
         }
      }
      return false;
   }

   private static boolean hasSelectedPrefix(String value) {
      return value.startsWith("→") || value.startsWith("➔") || value.startsWith("➜") || value.startsWith(">");
   }

   private static boolean hasOptionPrefix(String value) {
      return value.startsWith("∙") || value.startsWith("•") || value.startsWith("●") || value.startsWith("·") || value.startsWith("-");
   }

   private static String stripPrefix(String value) {
      return value.isEmpty() ? value : value.substring(1).trim();
   }

   private static String loreDebugString(SlotSnapshot item) {
      List<String> lines = new ArrayList<>();
      for (Component component : item.lore()) {
         lines.add(component.getString());
      }
      return lines.toString();
   }

   private static AhParseResult.Fallback fallback(AhStabilityCode code, String detail) {
      return new AhParseResult.Fallback(code, detail);
   }
}
