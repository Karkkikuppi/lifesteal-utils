package dev.candycup.lifestealutils.features.shop;

import dev.candycup.ui.lsu.SlotSnapshot;

import java.util.List;

public sealed interface ShopView permits ShopView.Categories, ShopView.Listings, ShopView.SelectAmount {
   String title();

   record Categories(String title, List<CategoryButton> categories) implements ShopView {
   }

   record Listings(String title, String categoryName, List<ListingRow> listings, NavButtons navButtons) implements ShopView {
   }

   record SelectAmount(String title, String categoryName, String selectedItemName, List<ListingRow> previousListings, List<BuyAmount> amounts, int goBackSlot) implements ShopView {
   }

   record CategoryButton(String name, int color, int slotIndex, SlotSnapshot item) {
   }

   record ListingRow(String name, String coinsPer, String xpPer, int slotIndex, SlotSnapshot item) {
   }

   record NavButtons(int goBackSlot, int previousPageSlot, int nextPageSlot) {
      public boolean hasPreviousPage() {
         return previousPageSlot >= 0;
      }

      public boolean hasNextPage() {
         return nextPageSlot >= 0;
      }
   }

   record BuyAmount(String label, String itemName, int slotIndex, SlotSnapshot item) {
   }
}
