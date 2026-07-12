package dev.candycup.lifestealutils.features.ah;

import dev.candycup.ui.lsu.SlotSnapshot;

import java.util.List;

public sealed interface AhView permits AhView.Items, AhView.FilterEdit {
    String title();

    AhState state();

    List<String> warnings();

    record Items(String title, AhState state, List<AuctionListing> listings, List<String> warnings) implements AhView {
    }

    record FilterEdit(String title, AhState state, List<String> warnings) implements AhView {
    }

    record AhState(AhControls controls, AhSortState sortState, AhFilterState filterState, AhSearchState searchState,
                   AhSidebarActions sidebarActions) {
    }

    record AuctionListing(String name, AuctionMeta meta, int slotIndex, SlotSnapshot item) {
    }

    record AuctionMeta(String seller, String timeRemaining, String compactTime, String price, boolean bidAuction,
                       boolean missingMeta) {
    }

    record AhControls(int nextPageSlot, int previousPageSlot) {
        public boolean hasNextPage() {
            return nextPageSlot >= 0;
        }

        public boolean hasPreviousPage() {
            return previousPageSlot >= 0;
        }
    }

    record AhSortState(int sortSlot, List<String> options, int selectedIndex) {
    }

    record AhFilterState(int editSlot, int confirmSlot, int goBackSlot, List<AhFilterOption> options,
                         boolean anySelected) {
    }

    record AhFilterOption(String label, int slotIndex, boolean selected) {
    }

    record AhSearchState(int searchSlot, String activeQuery, boolean clearable) {
        public boolean active() {
            return clearable && activeQuery != null && !activeQuery.isBlank();
        }
    }

    record AhSidebarActions(int claimItemsSlot, int yourListingsSlot) {
    }
}
