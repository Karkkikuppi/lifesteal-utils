package dev.candycup.lifestealutils.features.ah;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.ui.ActionLayer;
import dev.candycup.ui.ActionRegion;
import dev.candycup.ui.Bounds;
import dev.candycup.ui.Button;
import dev.candycup.ui.ItemRenderer;
import dev.candycup.ui.Panel;
import dev.candycup.ui.RenderPrimitives;
import dev.candycup.ui.Scrollbar;
import dev.candycup.ui.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class AhOverlayRenderer {
   private static final int PANEL = 0xD9151A24;
   private static final int PANEL_STROKE = 0x88515E78;
   private static final int ROW = 0x50212A3A;
   private static final int ROW_HOVER = 0x80475A78;
   private static final int TEXT = 0xFFF4F7FB;
   private static final int MUTED_TEXT = 0xFF9EA8B8;
   private static final int DIM_TEXT = 0xFF646C78;
   private static final int AUCTION = 0xFFFFB6A3;
   private static final int ACTIVE_SECTION = 0xFF9AC7FF;
   private static final int BUY = 0xFF8FE6C6;
   private static final int DANGER = 0xFFFF8D8D;
   private static final int COINS = 0xFFFFD76A;

   private final ActionLayer<ActionKind> actions = new ActionLayer<>();
   private Slot hoveredTooltipSlot;
   private int scrollOffset;
   private int maxScrollOffset;
   private Bounds mainViewport = Bounds.empty();
   private Bounds searchInput = Bounds.empty();
   private int screenWidth;
   private int screenHeight;
   private boolean filterPopoverAnchored;
   private int filterPopoverAnchorX;
   private int filterPopoverAnchorY;

   public void render(AhView view, AhView.Items previousItems, List<Slot> playerSlots, GuiGraphics graphics, int width, int height, int mouseX, int mouseY, String searchText, boolean searchFocused) {
      actions.clear();
      hoveredTooltipSlot = null;
      maxScrollOffset = 0;
      screenWidth = width;
      screenHeight = height;

      Minecraft client = Minecraft.getInstance();
      Font font = client.font;
      int outerW = Math.min(width - 20, Math.max(500, Math.round(width * 0.86F)));
      int outerH = Math.min(height - 20, Math.max(280, Math.round(height * 0.84F)));
      int sectionHeight = 19;
      int gap = 10;
      int sidebarWidth = Math.min(190, Math.max(156, outerW / 5));
      int mainX = (width - outerW) / 2;
      int topY = (height - outerH) / 2;
      int mainW = Math.max(260, outerW - sidebarWidth - gap);
      int contentH = Math.max(160, outerH - sectionHeight - 8);
      int sidebarX = mainX + mainW + gap;
      int panelY = topY + sectionHeight + 8;

      renderSectionSwitcher(graphics, font, mainX, topY, mainW, sectionHeight, mouseX, mouseY);
      drawPanel(graphics, mainX, panelY, mainW, contentH);
      drawHeader(graphics, font, view, previousItems, mainX, panelY, mainW, 38);
      renderMain(view, previousItems, graphics, font, mainX + 12, panelY + 46, mainW - 24, contentH - 58, mouseX, mouseY);
      renderSidebar(view, playerSlots, graphics, font, sidebarX, panelY, sidebarWidth, contentH, mouseX, mouseY, searchText, searchFocused);
      scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
   }

   public boolean scroll(double mouseX, double mouseY, double verticalAmount) {
      if (maxScrollOffset <= 0 || !mainViewport.contains(mouseX, mouseY)) {
         return false;
      }
      int direction = verticalAmount > 0 ? -1 : 1;
      int next = Math.max(0, Math.min(maxScrollOffset, scrollOffset + direction));
      if (next == scrollOffset) {
         return false;
      }
      scrollOffset = next;
      return true;
   }

   public ActionRegion<ActionKind> actionAt(double mouseX, double mouseY) {
      return actions.at(mouseX, mouseY);
   }

   public Slot hoveredTooltipSlot() {
      return hoveredTooltipSlot;
   }

   public Bounds searchInput() {
      return searchInput;
   }

   public void openFilterPopoverAt(double mouseX, double mouseY) {
      filterPopoverAnchored = true;
      filterPopoverAnchorX = (int) mouseX;
      filterPopoverAnchorY = (int) mouseY;
   }

   private void renderSectionSwitcher(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
      String[] labels = {"Shops", "Auctions", "Baltop"};
      int currentX = x;
      for (String label : labels) {
         boolean active = "Auctions".equals(label);
         int buttonW = Math.max(58, font.width(label) + 24);
         Bounds bounds = new Bounds(currentX, y, buttonW, height);
         drawPill(graphics, font, bounds, label, active ? ACTIVE_SECTION : MUTED_TEXT, active || bounds.contains(mouseX, mouseY));
         currentX += buttonW + 6;
      }
   }

   private void drawHeader(GuiGraphics graphics, Font font, AhView view, AhView.Items previousItems, int x, int y, int width, int height) {
      graphics.fill(x, y + height - 1, x + width, y + height, 0x554A5870);
      graphics.drawString(font, Component.literal(uppercaseTitle("AUCTION HOUSE")), x + 14, y + 8, MUTED_TEXT, false);
      String title = view instanceof AhView.FilterEdit && previousItems != null ? "Browse Listings" : view instanceof AhView.FilterEdit ? "Filter Auctions" : "Browse Listings";
      graphics.drawString(font, Component.literal(title), x + 14, y + 20, TEXT, false);
      AhView.Items items = view instanceof AhView.Items itemView ? itemView : previousItems;
      if (items != null) {
         String count = items.listings().size() + " shown";
         graphics.drawString(font, Component.literal(count), x + width - 14 - font.width(count), y + 20, MUTED_TEXT, false);
      }
   }

   private void renderMain(AhView view, AhView.Items previousItems, GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
      if (view instanceof AhView.Items items) {
         renderAuctionCards(items, graphics, font, x, y, width, height, mouseX, mouseY, false, -1);
      } else if (view instanceof AhView.FilterEdit filterEdit && previousItems != null) {
         renderAuctionCards(previousItems, graphics, font, x, y, width, height, mouseX, mouseY, true, filterEdit.state().filterState().goBackSlot());
      } else {
         mainViewport = new Bounds(x, y, width, height);
         graphics.drawString(font, Component.literal("Tune filters in the sidebar, then save or cancel."), x + 4, y + 4, MUTED_TEXT, false);
      }
   }

   private void renderAuctionCards(AhView.Items view, GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY, boolean disabled, int disabledClickSlot) {
      int footerHeight = 28;
      int cardHeight = 48;
      int gap = 6;
      int minCardWidth = 138;
      int columns = Math.max(1, Math.min(4, (width + gap) / (minCardWidth + gap)));
      int cardWidth = Math.max(108, (width - gap * (columns - 1)) / columns);
      int rowStep = cardHeight + gap;
      int rows = (view.listings().size() + columns - 1) / columns;
      int visibleRows = Math.max(1, (height - footerHeight) / rowStep);
      maxScrollOffset = Math.max(0, rows - visibleRows);
      int startRow = Math.min(scrollOffset, maxScrollOffset);
      int viewportBottom = y + height - footerHeight;
      mainViewport = new Bounds(x, y, width, height - footerHeight);

      int renderedRows = 0;
      for (int row = startRow; row < rows && y + renderedRows * rowStep + cardHeight <= viewportBottom; row++) {
         int rowY = y + renderedRows * rowStep;
         for (int col = 0; col < columns; col++) {
            int index = row * columns + col;
            if (index >= view.listings().size()) {
               break;
            }
            renderAuctionCard(view.listings().get(index), graphics, font, x + col * (cardWidth + gap), rowY, cardWidth, cardHeight, mouseX, mouseY, disabled, disabledClickSlot);
         }
         renderedRows++;
      }

      if (!disabled) {
         renderPageControls(view.state().controls(), graphics, font, x, y + height - 22, width, mouseX, mouseY);
      }
      renderScrollbar(graphics, x + width - 3, y, height - footerHeight);
   }

   private void renderAuctionCard(AhView.AuctionListing listing, GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY, boolean disabled, int disabledClickSlot) {
      Bounds bounds = new Bounds(x, y, width, height);
      boolean hovered = bounds.contains(mouseX, mouseY);
      graphics.fill(x, y, x + width, y + height, disabled ? 0xA80B0D12 : hovered ? ROW_HOVER : ROW);
      stroke(graphics, x, y, width, height, disabled ? 0x334A5870 : hovered ? 0xAAFFB6A3 : 0x334A5870);
      int iconSize = Math.round(16 * 1.35F);
      drawItem(graphics, listing.item().stack(), x + 5, y + Math.max(2, (height - iconSize) / 2), 1.35F);

      int textX = x + 35;
      int lineGap = 4;
      int textBlockHeight = font.lineHeight * 3 + lineGap * 2;
      int nameY = y + Math.max(2, (height - textBlockHeight) / 2);
      int metaY = nameY + font.lineHeight + lineGap;
      int priceY = metaY + font.lineHeight + lineGap;
      graphics.drawString(font, Component.literal(trim(font, listing.name(), width - 40)), textX, nameY, disabled ? DIM_TEXT : TEXT, false);
      AhView.AuctionMeta meta = listing.meta();
      if (meta.bidAuction()) {
         String bid = "Bid";
         int bidW = font.width(bid) + 8;
         graphics.fill(textX, metaY - 2, textX + bidW, metaY + font.lineHeight + 1, 0x664F6BFF);
         graphics.drawString(font, Component.literal(bid), textX + 4, metaY, disabled ? DIM_TEXT : 0xFFDDE4FF, false);
         textX += bidW + 5;
      }
      graphics.drawString(font, Component.literal(sellerTimeLine(font, meta.seller(), meta.compactTime(), width - (textX - x) - 6)), textX, metaY, disabled ? DIM_TEXT : MUTED_TEXT, false);
      graphics.drawString(font, Component.literal(trim(font, meta.price(), width - 40)), x + 35, priceY, disabled || meta.missingMeta() ? DIM_TEXT : COINS, false);
      actions.add(bounds, disabled ? disabledClickSlot : listing.slotIndex(), listing.item(), disabled && disabledClickSlot < 0 ? ActionKind.CONSUME_UI : ActionKind.SLOT_CLICK);
      if (hovered) {
         hoveredTooltipSlot = findSlot(listing.slotIndex());
      }
   }

   private void renderPageControls(AhView.AhControls controls, GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY) {
      if (controls.hasPreviousPage()) {
         Bounds previous = new Bounds(x, y, 82, 20);
         drawPill(graphics, font, previous, "Previous", MUTED_TEXT, previous.contains(mouseX, mouseY));
         actions.add(previous, controls.previousPageSlot(), null, ActionKind.SLOT_CLICK);
      }
      if (controls.hasNextPage()) {
         Bounds next = new Bounds(x + width - 88, y, 88, 20);
         drawPill(graphics, font, next, "Next Page", BUY, next.contains(mouseX, mouseY));
         actions.add(next, controls.nextPageSlot(), null, ActionKind.SLOT_CLICK);
      }
   }

   private void renderSidebar(AhView view, List<Slot> playerSlots, GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY, String searchText, boolean searchFocused) {
      drawPanel(graphics, x, y, width, height);
      int pad = 10;
      int currentY = y + 10;
      graphics.drawString(font, Component.literal(uppercaseTitle("MARKET TOOLS")), x + pad, currentY, MUTED_TEXT, false);
      currentY += 15;
      currentY = renderSearch(view.state().searchState(), graphics, font, x + pad, currentY, width - pad * 2, mouseX, mouseY, searchText, searchFocused);
      currentY += 8;
      currentY = renderSort(view.state().sortState(), graphics, font, x + pad, currentY, width - pad * 2, mouseX, mouseY);
      currentY += 8;
      currentY = renderFilters(view.state().filterState(), view instanceof AhView.FilterEdit, graphics, font, x + pad, currentY, width - pad * 2, mouseX, mouseY);
      currentY += 8;
      currentY = renderActions(view.state().sidebarActions(), graphics, font, x + pad, currentY, width - pad * 2, mouseX, mouseY);
      renderBalancesAndInventory(playerSlots, graphics, font, x + pad, Math.max(currentY + 8, y + height - 96), width - pad * 2, y + height - 10);
      if (view instanceof AhView.FilterEdit) {
         renderFilterPopover(view.state().filterState(), graphics, font, x + pad, y + 42, width - pad * 2, mouseX, mouseY);
      }
   }

   private int renderSearch(AhView.AhSearchState state, GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, String searchText, boolean focused) {
      drawSectionTitle(graphics, font, "Search", x, y, width);
      y += 16;
      int buttonW = 50;
      searchInput = new Bounds(x, y, width - buttonW - 5, 20);
      graphics.fill(searchInput.x(), searchInput.y(), searchInput.x() + searchInput.width(), searchInput.y() + searchInput.height(), 0x76313A4A);
      stroke(graphics, searchInput.x(), searchInput.y(), searchInput.width(), searchInput.height(), focused ? 0xCC9AC7FF : 0x553F4C60);
      String text = searchText == null || searchText.isEmpty() ? "Search" : searchText;
      graphics.drawString(font, Component.literal(trim(font, text + (focused && (System.currentTimeMillis() / 500L) % 2L == 0L ? "|" : ""), searchInput.width() - 8)), searchInput.x() + 4, searchInput.y() + 6, searchText == null || searchText.isEmpty() ? DIM_TEXT : TEXT, false);
      actions.add(searchInput, -1, null, ActionKind.FOCUS_TEXT);

      boolean reset = state.active();
      Bounds button = new Bounds(x + width - buttonW, y, buttonW, 20);
      drawPill(graphics, font, button, reset ? "Reset" : "Go", reset ? DANGER : BUY, button.contains(mouseX, mouseY));
      actions.add(button, state.searchSlot(), null, reset ? ActionKind.SEARCH_RESET : ActionKind.SEARCH_SUBMIT);
      if (state.active()) {
         y += 25;
         graphics.drawString(font, Component.literal(trim(font, state.activeQuery(), width)), x, y, MUTED_TEXT, false);
         return y + 12;
      }
      return y + 23;
   }

   private int renderSort(AhView.AhSortState sort, GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY) {
      drawSectionTitle(graphics, font, "Sort", x, y, width);
      y += 16;
      for (int i = 0; i < sort.options().size(); i++) {
         Bounds row = new Bounds(x, y, width, 18);
         boolean selected = i == sort.selectedIndex();
         drawSidebarRow(graphics, font, row, sort.options().get(i), selected, row.contains(mouseX, mouseY));
         actions.add(row, sort.sortSlot(), null, ActionKind.SLOT_CLICK);
         y += 18;
      }
      return y;
   }

   private int renderFilters(AhView.AhFilterState filters, boolean editMode, GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY) {
      drawSectionTitle(graphics, font, editMode ? "Edit Filters" : "Filters", x, y, width);
      if (editMode) {
         Bounds active = new Bounds(x + width - 54, y - 2, 54, 16);
         drawPill(graphics, font, active, "Editing", ACTIVE_SECTION, true);
      } else {
         Bounds edit = new Bounds(x + width - 38, y - 2, 38, 16);
         drawPill(graphics, font, edit, "Edit", MUTED_TEXT, edit.contains(mouseX, mouseY));
         actions.add(edit, filters.editSlot(), null, ActionKind.OPEN_FILTERS);
      }
      y += 16;
      int rendered = 0;
      for (AhView.AhFilterOption option : filters.options()) {
         if (!option.selected() && !editMode) {
            continue;
         }
         if (rendered >= 3 && editMode) {
            break;
         }
         Bounds row = new Bounds(x, y, width, 18);
         drawSidebarRow(graphics, font, row, option.label(), option.selected(), row.contains(mouseX, mouseY));
         y += 18;
         rendered++;
      }
      if (rendered == 0) {
         graphics.drawString(font, Component.literal(editMode ? "Choose filters below" : "No filters selected"), x, y + 3, DIM_TEXT, false);
         y += 18;
      }
      return y;
   }

   private void renderFilterPopover(AhView.AhFilterState filters, GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY) {
      int gap = 4;
      int buttonHeight = 18;
      int optionCount = Math.max(1, filters.options().size());
      int height = 8 + optionCount * buttonHeight + Math.max(0, optionCount - 1) * gap + gap + 22;
      int popoverWidth = width + 4;
      int startX = filterPopoverAnchored ? filterPopoverAnchorX : x - 2;
      int startY = filterPopoverAnchored ? filterPopoverAnchorY : y;
      startX = Math.max(8, Math.min(startX, screenWidth - popoverWidth - 8));
      startY = Math.max(8, Math.min(startY, screenHeight - height - 8));
      Bounds popover = new Bounds(startX, startY, popoverWidth, height);
      graphics.fill(popover.x(), popover.y(), popover.x() + popover.width(), popover.y() + popover.height(), 0xFF111822);
      stroke(graphics, popover.x(), popover.y(), popover.width(), popover.height(), 0xAA9AC7FF);
      actions.add(popover, -1, null, ActionKind.CONSUME_UI);
      if (popover.contains(mouseX, mouseY)) {
         hoveredTooltipSlot = null;
      }

      int contentX = startX + 4;
      int contentWidth = popoverWidth - 8;
      int currentY = startY + 4;
      if (filters.options().isEmpty()) {
         graphics.drawString(font, Component.literal("No filter options"), contentX + 2, currentY + 5, DIM_TEXT, false);
         currentY += buttonHeight + gap;
      } else {
         for (AhView.AhFilterOption option : filters.options()) {
            Bounds optionButton = new Bounds(contentX, currentY, contentWidth, buttonHeight);
            drawPill(graphics, font, optionButton, option.label(), option.selected() ? ACTIVE_SECTION : MUTED_TEXT, optionButton.contains(mouseX, mouseY));
            actions.add(optionButton, option.slotIndex(), null, ActionKind.SLOT_CLICK);
            currentY += buttonHeight + gap;
         }
      }

      int half = (contentWidth - gap) / 2;
      Bounds save = new Bounds(contentX, currentY, half, 20);
      Bounds back = new Bounds(contentX + half + gap, currentY, contentWidth - half - gap, 20);
      drawPill(graphics, font, save, "Save", BUY, save.contains(mouseX, mouseY));
      drawPill(graphics, font, back, "Back", DANGER, back.contains(mouseX, mouseY));
      actions.add(save, filters.anySelected() && filters.confirmSlot() >= 0 ? filters.confirmSlot() : filters.goBackSlot(), null, ActionKind.SLOT_CLICK);
      actions.add(back, filters.goBackSlot(), null, ActionKind.SLOT_CLICK);
   }

   private int renderActions(AhView.AhSidebarActions sidebarActions, GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY) {
      drawSectionTitle(graphics, font, "Actions", x, y, width);
      y += 16;
      Bounds claim = new Bounds(x, y, width, 20);
      drawPill(graphics, font, claim, "Claim Items", sidebarActions.claimItemsSlot() >= 0 ? AUCTION : DIM_TEXT, claim.contains(mouseX, mouseY));
      actions.add(claim, sidebarActions.claimItemsSlot(), null, ActionKind.SLOT_CLICK);
      y += 23;
      Bounds listings = new Bounds(x, y, width, 20);
      drawPill(graphics, font, listings, "Your Listings", sidebarActions.yourListingsSlot() >= 0 ? AUCTION : DIM_TEXT, listings.contains(mouseX, mouseY));
      actions.add(listings, sidebarActions.yourListingsSlot(), null, ActionKind.SLOT_CLICK);
      return y + 23;
   }

   private void renderBalancesAndInventory(List<Slot> playerSlots, GuiGraphics graphics, Font font, int x, int y, int width, int bottom) {
      if (y + 40 > bottom) {
         return;
      }
      graphics.fill(x, y, x + width, y + 1, 0x554A5870);
      graphics.drawString(font, Component.literal(uppercaseTitle("BALANCES")), x, y + 8, MUTED_TEXT, false);
      String coins = String.format("%,d", LifestealAPI.getUserCoinBalance());
      graphics.drawString(font, Component.literal("Coins"), x, y + 22, MUTED_TEXT, false);
      graphics.drawString(font, Component.literal(coins), x + width - font.width(coins), y + 22, COINS, false);
      int gridY = y + 40;
      int cell = 18;
      int count = Math.min(18, playerSlots.size());
      for (int i = 0; i < count && gridY + cell <= bottom; i++) {
         int col = i % 9;
         int row = i / 9;
         int sx = x + col * cell;
         int sy = gridY + row * cell;
         graphics.fill(sx, sy, sx + 17, sy + 17, 0x76313A4A);
         ItemStack stack = playerSlots.get(i).getItem();
         if (!stack.isEmpty()) {
            drawItem(graphics, stack, sx + 1, sy + 1, 0.9F);
         }
      }
   }

   private void drawSectionTitle(GuiGraphics graphics, Font font, String label, int x, int y, int width) {
      graphics.drawString(font, Component.literal(uppercaseTitle(label)), x, y, MUTED_TEXT, false);
      graphics.fill(x, y + 12, x + width, y + 13, 0x334A5870);
   }

   private void drawSidebarRow(GuiGraphics graphics, Font font, Bounds row, String label, boolean selected, boolean hovered) {
      graphics.fill(row.x(), row.y(), row.x() + row.width(), row.y() + row.height(), selected ? 0x444F6BFF : hovered ? ROW_HOVER : 0x2A212A3A);
      if (selected) {
         graphics.fill(row.x(), row.y(), row.x() + 2, row.y() + row.height(), ACTIVE_SECTION);
      }
      graphics.drawString(font, Component.literal(trim(font, label, row.width() - 10)), row.x() + 6, row.y() + 5, selected ? TEXT : MUTED_TEXT, false);
   }

   private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
      Panel.fillWithTopGradient(graphics, new Bounds(x, y, width, height), PANEL, 0x66414B60, 0x00202A38, PANEL_STROKE);
   }

   private void drawPill(GuiGraphics graphics, Font font, Bounds bounds, String label, int color, boolean hovered) {
      Button.pill(graphics, font, bounds, label, TEXT, color, hovered);
   }

   private void renderScrollbar(GuiGraphics graphics, int x, int y, int height) {
      Scrollbar.vertical(graphics, x, y, height, scrollOffset, maxScrollOffset, 0x442C3444, 0xAAFFB6A3);
   }

   private void drawItem(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
      ItemRenderer.draw(graphics, stack, x, y, scale);
   }

   private void stroke(GuiGraphics graphics, int x, int y, int width, int height, int color) {
      RenderPrimitives.stroke(graphics, x, y, width, height, color);
   }

   private int withAlpha(int color, int alpha) {
      return RenderPrimitives.withAlpha(color, alpha);
   }

   private String trim(Font font, String text, int width) {
      return Text.trim(font, text, width);
   }

   private String sellerTimeLine(Font font, String seller, String time, int width) {
      String separator = " | ";
      String safeTime = time == null || time.isBlank() ? "Unknown" : time;
      String safeSeller = seller == null || seller.isBlank() ? "Unknown Seller" : seller;
      int sellerWidth = Math.max(12, width - font.width(safeTime) - font.width(separator));
      return safeTime + separator + trim(font, safeSeller, sellerWidth);
   }

   private String uppercaseTitle(String text) {
      return Text.smallCaps(text);
   }

   private Slot findSlot(int slotIndex) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return null;
      }
      if (slotIndex >= 0 && slotIndex < client.player.containerMenu.slots.size()) {
         return client.player.containerMenu.slots.get(slotIndex);
      }
      return null;
   }

   public enum ActionKind {
      SLOT_CLICK,
      FOCUS_TEXT,
      SEARCH_SUBMIT,
      SEARCH_RESET,
      OPEN_FILTERS,
      CONSUME_UI
   }
}
