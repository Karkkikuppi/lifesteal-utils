package dev.candycup.lifestealutils.features.shop;

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
import dev.candycup.ui.lsu.SlotSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ShopOverlayRenderer {
   private static final int PANEL = 0xD9151A24;
   private static final int PANEL_SOFT = 0xAA202838;
   private static final int PANEL_STROKE = 0x88515E78;
   private static final int ROW = 0x50212A3A;
   private static final int ROW_HOVER = 0x80475A78;
   private static final int DISABLED_ROW = 0x48272A31;
   private static final int TEXT = 0xFFF4F7FB;
   private static final int MUTED_TEXT = 0xFF9EA8B8;
   private static final int DIM_TEXT = 0xFF646C78;
   private static final int COINS = 0xFFFFD76A;
   private static final int XP = 0xFF8BEF8B;
   private static final int DANGER = 0xFFFF8D8D;
   private static final int BUY = 0xFF8FE6C6;
   private static final int ACTIVE_SECTION = 0xFF9AC7FF;
   private static final int MAX_ROWS = 28;

   private final ActionLayer<ActionKind> actions = new ActionLayer<>();
   private Slot hoveredTooltipSlot;
   private int scrollOffset;
   private int maxScrollOffset;
   private static LayoutMode layoutMode = LayoutMode.TABLE;
   private boolean layoutDropdownOpen;
   private Bounds amountPopoverBounds = Bounds.empty();
   private String amountPopoverAnchorKey = "";
   private int amountPopoverAnchorX;
   private int amountPopoverAnchorY;

   public void render(ShopView view, List<Slot> playerSlots, GuiGraphics graphics, int width, int height, int mouseX, int mouseY) {
      actions.clear();
      hoveredTooltipSlot = null;
      amountPopoverBounds = Bounds.empty();
      maxScrollOffset = 0;

      Minecraft client = Minecraft.getInstance();
      Font font = client.font;
      int outerW = Math.min(width - 20, Math.max(440, Math.round(width * 0.84F)));
      int outerH = Math.min(height - 20, Math.max(260, Math.round(height * 0.82F)));
      int headerHeight = 38;
      int sectionHeight = 19;
      int gap = 10;
      int inventoryWidth = Math.min(176, Math.max(136, outerW / 5));
      int contentX = (width - outerW) / 2;
      int contentY = (height - outerH) / 2;
      int contentW = Math.max(220, outerW - inventoryWidth - gap);
      int contentH = Math.max(140, outerH - sectionHeight - 8);
      int inventoryX = contentX + contentW + gap;
      int panelY = contentY + sectionHeight + 8;

      renderSectionSwitcher(graphics, font, contentX, contentY, contentW, sectionHeight, mouseX, mouseY);
      drawPanel(graphics, contentX, panelY, contentW, contentH);
      drawHeader(graphics, font, view, contentX, panelY, contentW, headerHeight);

      if (view instanceof ShopView.Categories categories) {
         renderCategories(categories, graphics, font, contentX + 12, panelY + headerHeight + 8, contentW - 24, contentH - headerHeight - 20, mouseX, mouseY);
      } else if (view instanceof ShopView.Listings listings) {
         renderListings(listings, graphics, font, contentX + 12, panelY + headerHeight + 6, contentW - 24, contentH - headerHeight - 18, mouseX, mouseY, false, null);
      } else if (view instanceof ShopView.SelectAmount selectAmount) {
         renderSelectAmount(selectAmount, graphics, font, contentX + 12, panelY + headerHeight + 6, contentW - 24, contentH - headerHeight - 18, mouseX, mouseY);
      }
      if (view instanceof ShopView.SelectAmount selectAmount) {
         updateAmountPopoverAnchor(selectAmount, mouseX, mouseY);
         renderAmountPopover(selectAmount, graphics, font, contentX + 12, panelY + headerHeight + 6, contentW - 24, contentH - headerHeight - 18, mouseX, mouseY);
      } else {
         amountPopoverAnchorKey = "";
      }
      if (!(view instanceof ShopView.Categories)) {
         renderLayoutDropdownMenu(graphics, font, contentX + contentW - 100, panelY + 8, 86, 20, mouseX, mouseY);
      }
      scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

      renderInventoryPreview(playerSlots, graphics, font, inventoryX, panelY, inventoryWidth, contentH);
   }

   public boolean scroll(double verticalAmount) {
      if (maxScrollOffset <= 0) {
         return false;
      }
      int direction = verticalAmount > 0 ? -1 : 1;
      int nextOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + direction * 3));
      if (nextOffset == scrollOffset) {
         return false;
      }
      scrollOffset = nextOffset;
      return true;
   }

   public ActionRegion<ActionKind> actionAt(double mouseX, double mouseY) {
      return actions.at(mouseX, mouseY);
   }

   public Slot hoveredTooltipSlot() {
      return hoveredTooltipSlot;
   }

   public boolean handleUiAction(ActionRegion<ActionKind> action) {
      if (action == null) {
         return false;
      }
      if (action.kind() == ActionKind.TOGGLE_LAYOUT_DROPDOWN) {
         layoutDropdownOpen = !layoutDropdownOpen;
         return true;
      }
      if (action.kind() == ActionKind.SET_LAYOUT_TABLE) {
         layoutMode = LayoutMode.TABLE;
         layoutDropdownOpen = false;
         scrollOffset = 0;
         return true;
      }
      if (action.kind() == ActionKind.SET_LAYOUT_CARDS) {
         layoutMode = LayoutMode.CARDS;
         layoutDropdownOpen = false;
         scrollOffset = 0;
         return true;
      }
      if (action.kind() == ActionKind.CONSUME_UI) {
         return true;
      }
      return false;
   }

   private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
      Panel.fillWithTopGradient(graphics, new Bounds(x, y, width, height), PANEL, 0x66414B60, 0x00202A38, PANEL_STROKE);
   }

   private void renderSectionSwitcher(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
      String[] labels = {"Shops", "Auctions", "Baltop"};
      int currentX = x;
      int gap = 6;
      for (String label : labels) {
         boolean active = "Shops".equals(label);
         int buttonW = Math.max(58, font.width(label) + 24);
         Bounds bounds = new Bounds(currentX, y, buttonW, height);
         int color = active ? ACTIVE_SECTION : MUTED_TEXT;
         drawPill(graphics, font, bounds, label, color, active || bounds.contains(mouseX, mouseY));
         currentX += buttonW + gap;
      }
   }

   private void drawHeader(GuiGraphics graphics, Font font, ShopView view, int x, int y, int width, int height) {
      graphics.fill(x, y + height - 1, x + width, y + height, 0x554A5870);
      String eyebrow = view instanceof ShopView.Categories ? "SHOP DIRECTORY" : "NPC SHOP";
      graphics.drawString(font, Component.literal(uppercaseTitle(eyebrow)), x + 14, y + 8, MUTED_TEXT, false);
      graphics.drawString(font, Component.literal(titleFor(view)), x + 14, y + 20, TEXT, false);
      if (!(view instanceof ShopView.Categories)) {
         renderLayoutDropdownButton(graphics, font, x + width - 100, y + 8, 86, 20);
      }
   }

   private void renderLayoutDropdownButton(GuiGraphics graphics, Font font, int x, int y, int width, int height) {
      String label = layoutMode == LayoutMode.TABLE ? "Table" : "Cards";
      Bounds button = new Bounds(x, y, width, height);
      drawPill(graphics, font, button, label + " v", ACTIVE_SECTION, layoutDropdownOpen);
      actions.add(button, -1, null, ActionKind.TOGGLE_LAYOUT_DROPDOWN);
   }

   private void renderLayoutDropdownMenu(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
      if (!layoutDropdownOpen) {
         return;
      }

      Bounds table = new Bounds(x, y + height + 3, width, height);
      Bounds cards = new Bounds(x, y + (height + 3) * 2, width, height);
      actions.add(new Bounds(x - 3, y + height, width + 6, height * 2 + 9), -1, null, ActionKind.CONSUME_UI);
      drawPill(graphics, font, table, "Table", layoutMode == LayoutMode.TABLE ? ACTIVE_SECTION : MUTED_TEXT, table.contains(mouseX, mouseY));
      drawPill(graphics, font, cards, "Cards", layoutMode == LayoutMode.CARDS ? ACTIVE_SECTION : MUTED_TEXT, cards.contains(mouseX, mouseY));
      actions.add(table, -1, null, ActionKind.SET_LAYOUT_TABLE);
      actions.add(cards, -1, null, ActionKind.SET_LAYOUT_CARDS);
   }

   private String titleFor(ShopView view) {
      if (view instanceof ShopView.Categories) {
         return "Choose a Category";
      }
      if (view instanceof ShopView.Listings listings) {
         return listings.categoryName();
      }
      if (view instanceof ShopView.SelectAmount selectAmount) {
         return selectAmount.categoryName().isBlank() ? "Shop" : selectAmount.categoryName();
      }
      return view.title();
   }

   private void renderCategories(ShopView.Categories categories, GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
      int buttonHeight = 34;
      int gap = 8;
      int minButtonWidth = 148;
      int columns = Math.max(1, Math.min(4, (width + gap) / (minButtonWidth + gap)));
      int buttonWidth = Math.max(92, (width - gap * (columns - 1)) / columns);
      int rowStep = buttonHeight + gap;
      int totalRows = (categories.categories().size() + columns - 1) / columns;
      int visibleRows = Math.max(1, height / rowStep);
      maxScrollOffset = Math.max(0, totalRows - visibleRows);
      int start = Math.min(scrollOffset, maxScrollOffset);
      int firstIndex = start * columns;
      int visibleSlots = visibleRows * columns;
      int count = Math.min(categories.categories().size() - firstIndex, visibleSlots);
      if (count <= 0) {
         graphics.drawString(font, Component.literal("No categories exposed by the server."), x, y, MUTED_TEXT, false);
         return;
      }

      for (int i = 0; i < count; i++) {
         int index = firstIndex + i;
         ShopView.CategoryButton category = categories.categories().get(index);
         int col = i % columns;
         int row = i / columns;
         int rowY = y + row * rowStep;
         int buttonX = x + col * (buttonWidth + gap);
         Bounds bounds = new Bounds(buttonX, rowY, buttonWidth, buttonHeight);
         boolean hovered = bounds.contains(mouseX, mouseY);
         int[] gradient = cappedGradient(category.color(), hovered);
         graphics.fillGradient(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), gradient[0], gradient[1]);
         stroke(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), hovered ? 0xCCFFFFFF : 0x55FFFFFF);
         drawItem(graphics, category.item().stack(), buttonX + 9, rowY + 9, 1.0F);
         graphics.drawString(font, Component.literal(trim(font, category.name(), buttonWidth - 38)), buttonX + 33, rowY + 13, TEXT, false);
         actions.add(bounds, category.slotIndex(), category.item(), ActionKind.SLOT_CLICK);
      }
      renderScrollbar(graphics, x + width - 3, y, height);
   }

   private void renderListings(ShopView.Listings listings, GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY, boolean disabled, ShopView.SelectAmount amountView) {
      if (layoutMode == LayoutMode.CARDS) {
         renderListingCards(listings, graphics, font, x, y, width, height, mouseX, mouseY, disabled, amountView);
         return;
      }

      int footerHeight = 30;
      int headerHeight = 18;
      int tableHeight = Math.max(70, height - footerHeight);
      int rowHeight = Math.max(15, Math.min(22, (tableHeight - headerHeight) / MAX_ROWS));
      int nameW = Math.max(110, width - 160);
      int coinsX = x + nameW + 10;
      int xpX = coinsX + 72;

      graphics.drawString(font, Component.literal("Item"), x + 22, y + 4, MUTED_TEXT, false);
      graphics.drawString(font, Component.literal("Coins"), coinsX, y + 4, MUTED_TEXT, false);
      graphics.drawString(font, Component.literal("XP"), xpX, y + 4, MUTED_TEXT, false);
      graphics.fill(x, y + headerHeight - 1, x + width, y + headerHeight, 0x554A5870);

      List<ShopView.ListingRow> rows = listings.listings();
      int selectedIndex = selectedIndex(rows, amountView);
      int totalUnits = rows.size();
      maxScrollOffset = Math.max(0, totalUnits - Math.max(1, (tableHeight - headerHeight) / rowHeight));
      int skippedUnits = Math.min(scrollOffset, maxScrollOffset);
      int maxTableBottom = y + tableHeight;
      int currentY = y + headerHeight;
      for (int i = 0; i < rows.size() && currentY + rowHeight <= maxTableBottom; i++) {
         ShopView.ListingRow row = rows.get(i);
         boolean selected = amountView != null && amountView.selectedItemName().equals(row.name());
         if (skippedUnits >= 1) {
            skippedUnits--;
            continue;
         }
         if (skippedUnits > 0) {
            skippedUnits = 0;
            continue;
         }
         int rowY = currentY;
         Bounds rowBounds = new Bounds(x, rowY, width, rowHeight - 1);
         boolean hovered = rowBounds.contains(mouseX, mouseY);
         int rowColor = disabled && !selected ? 0xA80B0D12 : hovered ? ROW_HOVER : ROW;
         graphics.fill(rowBounds.x(), rowBounds.y(), rowBounds.x() + rowBounds.width(), rowBounds.y() + rowBounds.height(), rowColor);
         drawItem(graphics, row.item().stack(), x + 3, rowY + 1, 0.75F);
         int textColor = disabled && !selected ? DIM_TEXT : TEXT;
         graphics.drawString(font, Component.literal(trim(font, row.name(), nameW - 28)), x + 22, rowY + 4, textColor, false);
         graphics.drawString(font, Component.literal(row.coinsPer()), coinsX, rowY + 4, disabled && !selected ? DIM_TEXT : COINS, false);
         graphics.drawString(font, Component.literal(row.xpPer()), xpX, rowY + 4, disabled && !selected ? DIM_TEXT : XP, false);

         if (!disabled && hovered) {
            graphics.fill(rowBounds.x(), rowBounds.y(), rowBounds.x() + 3, rowBounds.y() + rowBounds.height(), BUY);
         }

         if (!disabled) {
            actions.add(rowBounds, row.slotIndex(), row.item(), ActionKind.SLOT_CLICK);
         } else if (!selected && amountView != null && amountView.goBackSlot() >= 0) {
            actions.add(rowBounds, amountView.goBackSlot(), null, ActionKind.SLOT_CLICK);
         }
         if (hovered) {
            hoveredTooltipSlot = findSlot(row.slotIndex());
         }
         currentY += rowHeight;
      }

      renderListingFooter(listings.navButtons(), graphics, font, x, y + height - footerHeight + 5, width, mouseX, mouseY);
      renderScrollbar(graphics, x + width - 3, y + headerHeight, tableHeight - headerHeight);
   }

   private void renderListingCards(ShopView.Listings listings, GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY, boolean disabled, ShopView.SelectAmount amountView) {
      int footerHeight = 30;
      int cardHeight = 34;
      int gap = 6;
      int minCardWidth = 112;
      int columns = Math.max(1, Math.min(5, (width + gap) / (minCardWidth + gap)));
      int cardWidth = Math.max(92, (width - gap * (columns - 1)) / columns);
      List<ShopView.ListingRow> rows = listings.listings();
      int sourceRows = (rows.size() + columns - 1) / columns;
      int totalRows = sourceRows;
      int rowStep = cardHeight + gap;
      int visibleRows = Math.max(1, (height - footerHeight) / rowStep);
      maxScrollOffset = Math.max(0, totalRows - visibleRows);
      int startRow = Math.min(scrollOffset, maxScrollOffset);
      int currentY = y;
      int logicalRow = 0;
      int sourceRow = 0;

      while (sourceRow < sourceRows && currentY + cardHeight <= y + height - footerHeight) {
         if (logicalRow >= startRow) {
            renderCardRow(rows, sourceRow, columns, cardWidth, cardHeight, gap, graphics, font, x, currentY, mouseX, mouseY, disabled, amountView);
            currentY += rowStep;
         }
         logicalRow++;

         sourceRow++;
      }

      renderListingFooter(listings.navButtons(), graphics, font, x, y + height - footerHeight + 5, width, mouseX, mouseY);
      renderScrollbar(graphics, x + width - 3, y, height - footerHeight);
   }

   private void renderCardRow(List<ShopView.ListingRow> rows, int sourceRow, int columns, int cardWidth, int cardHeight, int gap, GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY, boolean disabled, ShopView.SelectAmount amountView) {
      int startIndex = sourceRow * columns;
      for (int col = 0; col < columns; col++) {
         int index = startIndex + col;
         if (index >= rows.size()) {
            return;
         }
         ShopView.ListingRow row = rows.get(index);
         int cardX = x + col * (cardWidth + gap);
         Bounds bounds = new Bounds(cardX, y, cardWidth, cardHeight);
         boolean selected = amountView != null && amountView.selectedItemName().equals(row.name());
         boolean hovered = bounds.contains(mouseX, mouseY);
         int color = disabled && !selected ? 0xA80B0D12 : hovered ? ROW_HOVER : ROW;
         graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), color);
         stroke(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), selected ? withAlpha(BUY, 0xAA) : 0x334A5870);
         drawItem(graphics, row.item().stack(), cardX + 4, y + 4, 1.6F);
         int textColor = disabled && !selected ? DIM_TEXT : TEXT;
         int textX = cardX + 34;
         graphics.drawString(font, Component.literal(trim(font, row.name(), cardWidth - 38)), textX, y + 5, textColor, false);
         List<MetaLine> meta = metaLines(row);
         for (int line = 0; line < Math.min(2, meta.size()); line++) {
            MetaLine metaLine = meta.get(line);
            graphics.drawString(font, Component.literal(metaLine.text()), textX, y + 18 + line * 10, disabled && !selected ? DIM_TEXT : metaLine.color(), false);
         }

         if (!disabled) {
            actions.add(bounds, row.slotIndex(), row.item(), ActionKind.SLOT_CLICK);
         } else if (!selected && amountView != null && amountView.goBackSlot() >= 0) {
            actions.add(bounds, amountView.goBackSlot(), null, ActionKind.SLOT_CLICK);
         }
         if (hovered) {
            hoveredTooltipSlot = findSlot(row.slotIndex());
         }
      }
   }

   private void renderSelectAmount(ShopView.SelectAmount selectAmount, GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
      String categoryName = selectAmount.categoryName().isBlank() ? "Shop" : selectAmount.categoryName();
      ShopView.Listings synthetic = new ShopView.Listings("Shop | " + categoryName, categoryName, selectAmount.previousListings(), new ShopView.NavButtons(-1, -1, -1));
      renderListings(synthetic, graphics, font, x, y, width, height, mouseX, mouseY, true, selectAmount);
   }

   private void renderAmountPopover(ShopView.SelectAmount amountView, GuiGraphics graphics, Font font, int contentX, int contentY, int contentWidth, int contentHeight, int mouseX, int mouseY) {
      int gap = 4;
      int columns = 4;
      int rows = 2;
      int buttonHeight = 22;
      int buttonWidth = amountPopoverButtonWidth(font, amountView);
      int buttonCount = Math.min(7, amountView.amounts().size()) + (amountView.goBackSlot() >= 0 ? 1 : 0);
      int usedColumns = Math.max(1, Math.min(columns, buttonCount));
      int usedRows = Math.max(1, Math.min(rows, (buttonCount + columns - 1) / columns));
      int dropdownWidth = buttonWidth * usedColumns + gap * (usedColumns - 1) + 8;
      int dropdownHeight = buttonHeight * usedRows + gap * (usedRows - 1) + 8;
      int startX = Math.max(contentX, Math.min(amountPopoverAnchorX, contentX + contentWidth - dropdownWidth));
      int startY = Math.max(contentY, Math.min(amountPopoverAnchorY, contentY + contentHeight - dropdownHeight));
      int index = 0;
      amountPopoverBounds = new Bounds(startX, startY, dropdownWidth, dropdownHeight);
      graphics.fill(amountPopoverBounds.x(), amountPopoverBounds.y(), amountPopoverBounds.x() + amountPopoverBounds.width(), amountPopoverBounds.y() + amountPopoverBounds.height(), 0xF0161B25);
      stroke(graphics, amountPopoverBounds.x(), amountPopoverBounds.y(), amountPopoverBounds.width(), amountPopoverBounds.height(), 0xAA8FE6C6);
      actions.add(amountPopoverBounds, -1, null, ActionKind.CONSUME_UI);
      for (ShopView.BuyAmount amount : amountView.amounts()) {
         if (index >= 7) {
            break;
         }
         int col = index % columns;
         int row = index / columns;
         Bounds bounds = new Bounds(startX + 4 + col * (buttonWidth + gap), startY + 4 + row * (buttonHeight + gap), buttonWidth, buttonHeight);
         drawPill(graphics, font, bounds, amount.label(), BUY, bounds.contains(mouseX, mouseY));
         actions.add(bounds, amount.slotIndex(), amount.item(), ActionKind.SLOT_CLICK);
         if (bounds.contains(mouseX, mouseY)) {
            hoveredTooltipSlot = findSlot(amount.slotIndex());
         }
         index++;
      }
      if (amountView.goBackSlot() >= 0) {
         int col = index % columns;
         int row = Math.min(1, index / columns);
         if (row == 0 && index < columns) {
            row = 1;
            col = 0;
         }
         Bounds back = new Bounds(startX + 4 + col * (buttonWidth + gap), startY + 4 + row * (buttonHeight + gap), buttonWidth, buttonHeight);
         drawPill(graphics, font, back, "Cancel", DANGER, back.contains(mouseX, mouseY));
         actions.add(back, amountView.goBackSlot(), null, ActionKind.SLOT_CLICK);
         if (back.contains(mouseX, mouseY)) {
            hoveredTooltipSlot = findSlot(amountView.goBackSlot());
         }
      }
   }

   private void updateAmountPopoverAnchor(ShopView.SelectAmount amountView, int mouseX, int mouseY) {
      String key = amountView.categoryName() + "\n" + amountView.selectedItemName();
      if (key.equals(amountPopoverAnchorKey)) {
         return;
      }
      amountPopoverAnchorKey = key;
      amountPopoverAnchorX = mouseX;
      amountPopoverAnchorY = mouseY;
   }

   private int amountPopoverButtonWidth(Font font, ShopView.SelectAmount amountView) {
      int width = amountView.goBackSlot() >= 0 ? font.width("Cancel") : 0;
      for (ShopView.BuyAmount amount : amountView.amounts()) {
         width = Math.max(width, font.width(amount.label()));
      }
      return Math.max(48, width + 18);
   }

   private void renderListingFooter(ShopView.NavButtons navButtons, GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY) {
      if (navButtons.goBackSlot() >= 0) {
         Bounds back = new Bounds(x, y, 82, 20);
         drawPill(graphics, font, back, "Go Back", DANGER, back.contains(mouseX, mouseY));
         actions.add(back, navButtons.goBackSlot(), null, ActionKind.SLOT_CLICK);
      }

      if (navButtons.hasPreviousPage()) {
         Bounds previous = new Bounds(x + width - 184, y, 86, 20);
         drawPill(graphics, font, previous, "Previous", MUTED_TEXT, previous.contains(mouseX, mouseY));
         actions.add(previous, navButtons.previousPageSlot(), null, ActionKind.SLOT_CLICK);
      }
      if (navButtons.hasNextPage()) {
         Bounds next = new Bounds(x + width - 92, y, 92, 20);
         drawPill(graphics, font, next, "Next Page", BUY, next.contains(mouseX, mouseY));
         actions.add(next, navButtons.nextPageSlot(), null, ActionKind.SLOT_CLICK);
      }
   }

   private void renderInventoryPreview(List<Slot> playerSlots, GuiGraphics graphics, Font font, int x, int y, int width, int height) {
      drawPanel(graphics, x, y, width, height);
      graphics.drawString(font, Component.literal("YOUR INVENTORY"), x + 10, y + 10, MUTED_TEXT, false);

      int cell = 18;
      int cols = 9;
      int gridW = cols * cell;
      int startX = x + Math.max(8, (width - gridW) / 2);
      int startY = y + 32;
      int count = Math.min(27, playerSlots.size());
      for (int i = 0; i < count; i++) {
         int row = i / cols;
         int col = i % cols;
         int sx = startX + col * cell;
         int sy = startY + row * cell;
         graphics.fill(sx, sy, sx + 17, sy + 17, 0x76313A4A);
         stroke(graphics, sx, sy, 17, 17, 0x443F4C60);
         ItemStack stack = playerSlots.get(i).getItem();
         if (!stack.isEmpty()) {
            drawItem(graphics, stack, sx + 1, sy + 1, 0.9F);
         }
      }

      int balancesY = startY + 4 * cell + 10;
      graphics.fill(x + 10, balancesY, x + width - 10, balancesY + 1, 0x554A5870);
      graphics.drawString(font, Component.literal(uppercaseTitle("BALANCES")), x + 10, balancesY + 10, MUTED_TEXT, false);
      drawKeyValue(graphics, font, "Coins", formatNumber(LifestealAPI.getUserCoinBalance()), x + 10, balancesY + 25, width - 20, COINS);
   }

   private void drawKeyValue(GuiGraphics graphics, Font font, String key, String value, int x, int y, int width, int valueColor) {
      graphics.drawString(font, Component.literal(key), x, y, MUTED_TEXT, false);
      graphics.drawString(font, Component.literal(value), x + width - font.width(value), y, valueColor, false);
   }

   private List<MetaLine> metaLines(ShopView.ListingRow row) {
      List<MetaLine> lines = new ArrayList<>();
      if (!row.coinsPer().equals("-")) {
         lines.add(new MetaLine(row.coinsPer() + " Coins", COINS));
      }
      if (!row.xpPer().equals("-")) {
         lines.add(new MetaLine(row.xpPer() + " XP", XP));
      }
      return lines;
   }

   private int selectedIndex(List<ShopView.ListingRow> rows, ShopView.SelectAmount amountView) {
      if (amountView == null) {
         return -1;
      }
      for (int i = 0; i < rows.size(); i++) {
         if (amountView.selectedItemName().equals(rows.get(i).name())) {
            return i;
         }
      }
      return -1;
   }

   private void renderScrollbar(GuiGraphics graphics, int x, int y, int height) {
      Scrollbar.vertical(graphics, x, y, height, scrollOffset, maxScrollOffset, 0x442C3444, 0xAA8FE6C6);
   }

   private void drawPill(GuiGraphics graphics, Font font, Bounds bounds, String label, int color, boolean hovered) {
      Button.pill(graphics, font, bounds, label, TEXT, color, hovered);
   }

   private void drawItem(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
      ItemRenderer.draw(graphics, stack, x, y, scale);
   }

   private int[] cappedGradient(int color, boolean hovered) {
      float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
      float saturation = Math.min(0.58F, Math.max(0.34F, hsb[1]));
      float topBrightness = hovered ? 0.42F : 0.34F;
      float bottomBrightness = hovered ? 0.24F : 0.19F;
      int top = Color.HSBtoRGB(hsb[0], saturation, topBrightness) & 0x00FFFFFF;
      int bottom = Color.HSBtoRGB(hsb[0], saturation, bottomBrightness) & 0x00FFFFFF;
      return new int[]{0xDD000000 | top, 0xDD000000 | bottom};
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

   private String uppercaseTitle(String text) {
      return Text.smallCaps(text);
   }

   private String formatNumber(int value) {
      return String.format("%,d", value);
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
      TOGGLE_LAYOUT_DROPDOWN,
      SET_LAYOUT_TABLE,
      SET_LAYOUT_CARDS,
      CONSUME_UI
   }

   private enum LayoutMode {
      TABLE,
      CARDS
   }

   private record MetaLine(String text, int color) {
   }
}
