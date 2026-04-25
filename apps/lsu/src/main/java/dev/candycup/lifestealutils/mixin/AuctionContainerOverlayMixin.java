package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.features.ah.AhOverlaySearchState;
import dev.candycup.lifestealutils.features.ah.AhSearchAutomation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
//? if >1.21.8 {
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
//?}
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class AuctionContainerOverlayMixin<T extends AbstractContainerMenu> implements AhOverlaySearchState {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/ah-overlay");
   private static final String AUCTION_ITEMS_TITLE = "Auction | Items";
   private static final String FILTER_TITLE = "Filter";
   private static final boolean DEBUG_RENDER_UNDERLYING_GUI = false;

   private static final int IDX_CONTENT_X = 0;
   private static final int IDX_CONTENT_Y = 1;
   private static final int IDX_CONTENT_HEIGHT = 2;
   private static final int IDX_SIDEBAR_X = 3;
   private static final int IDX_SIDEBAR_WIDTH = 4;
   private static final int IDX_MAIN_X = 5;
   private static final int IDX_MAIN_WIDTH = 6;

   private static final int MIN_OUTER_MARGIN_X = 10;
   private static final int MIN_OUTER_MARGIN_Y = 8;
   private static final int MAX_OUTER_MARGIN_X = 28;
   private static final int MAX_OUTER_MARGIN_Y = 22;
   private static final int PANEL_GAP = 18;
   private static final int INNER_PADDING = 10;
   private static final int SEARCH_HEIGHT = 20;
   private static final int SECTION_GAP = 12;
   private static final int HEADER_HEIGHT = 20;
   private static final int SIDEBAR_STROKE_WIDTH = 4;

   private static final int ACCENT_STROKE = 0xFFFF989A;
   private static final int HEADER_GRADIENT_TOP = 0x99FF989A;
   private static final int HEADER_GRADIENT_BOTTOM = 0x73FF989A;

   private static final int ENABLED_TEXT = 0xFFFF989A;
   private static final int ENABLED_BACKGROUND = 0x30873737;
   private static final int DISABLED_TEXT = 0xFFFFFFFF;
   private static final int DISABLED_BACKGROUND = 0x54000000;

   private static final int MAIN_BACKGROUND = 0x9A000000;
   private static final int CARD_HEIGHT = 52;
   private static final int CARD_GAP = 8;
   private static final int CARD_ROW_SIDE_MARGIN = 10;
   private static final int MAX_CARD_COLUMNS = 3;
   private static final int MID_CARD_COLUMNS = 2;
   private static final int MIN_CARD_WIDTH = 138;
   private static final int CARD_CONTENT_PADDING_X = 12;
   private static final float CARD_ICON_SCALE = 1.25F;
   private static final int FOOTER_GAP = 10;
   private static final int FOOTER_HEIGHT = 20;
   private static final int FOOTER_BUTTON_WIDTH = 96;
   private static final int SCROLLBAR_WIDTH = 4;
   private static final int SCROLLBAR_MARGIN = 3;
   private static final int VIEWPORT_WALL_DEPTH = 4;
   private static final int VIEWPORT_BOTTOM_EDGE = 0x33000000;
   private static final int VIEWPORT_BOTTOM_SHADOW = 0x30000000;
   private static final int SEARCH_BUTTON_WIDTH = 70;
   private static final int FILTER_HEADER_BUTTON_WIDTH = 56;
   private static final int FILTER_HEADER_BUTTON_GAP = 4;
   private static final int SEARCH_TEXT_LEFT_PAD = 6;
   private static final int SEARCH_TITLE_HEIGHT = 16;
   private static final int BASELINE_TOP_PADDING = 18;
   private static final int BID_BADGE_HORIZONTAL_PADDING = 4;
   private static final int BID_BADGE_TEXT_COLOR = 0xFFFFCFCF;
   private static final int BID_BADGE_BACKGROUND = 0x66A33434;
   private static final String SIDEBAR_OPTION_PREFIX = "∙ ";

   private static final String SORT_ITEM_NAME = "Sort Items";
   private static final String FILTER_ITEM_NAME = "Filter Items";
   private static final String SEARCH_ITEM_NAME = "Search Items";
   private static final String CONFIRM_BUTTON_NAME = "Confirm";
   private static final String GO_BACK_BUTTON_NAME = "Go Back";
   private static final String RESET_SEARCH_NAME = "Reset Search";
   private static final String NEXT_PAGE_NAME = "Next Page";
   private static final String PREV_PAGE_NAME = "Previous Page";
   private static final String ALT_PREV_PAGE_NAME = "Last Page";
   private static final String CLAIM_ITEMS_NAME = "Claim Items";
   private static final String YOUR_LISTINGS_NAME = "Your Listings";

   private static final String AUCTION_CLAIM_ITEMS_BUTTON_KEY = "lsu.auction.sidebar.claim_items";
   private static final String AUCTION_YOUR_LISTINGS_BUTTON_KEY = "lsu.auction.sidebar.your_listings";
   private static final String AUCTION_CLAIM_ITEMS_UNAVAILABLE_KEY = "lsu.auction.sidebar.claim_items.unavailable";
   private static final String AUCTION_YOUR_LISTINGS_UNAVAILABLE_KEY = "lsu.auction.sidebar.your_listings.unavailable";

   private static final int MODE_NONE = 0;
   private static final int MODE_ITEMS = 1;
   private static final int MODE_FILTER_EDIT = 2;

   @Unique
   private final List<String> lifestealutils$sortOptionLabels = new ArrayList<>();

   @Unique
   private static final List<String> lifestealutils$cachedSortOptionLabels = new ArrayList<>();

   @Unique
   private static int lifestealutils$cachedSelectedSortIndex = -1;

   @Unique
   private int lifestealutils$selectedSortIndex = -1;

   @Unique
   private int lifestealutils$sortItemSlotIndex = -1;

   @Unique
   private int lifestealutils$searchItemSlotIndex = -1;

   @Unique
   private int lifestealutils$filterItemSlotIndex = -1;

   @Unique
   private int lifestealutils$filterConfirmSlotIndex = -1;

   @Unique
   private int lifestealutils$filterGoBackSlotIndex = -1;

   @Unique
   private final List<String> lifestealutils$filterOptionLabels = new ArrayList<>();

   @Unique
   private final List<Integer> lifestealutils$filterOptionSlotIndices = new ArrayList<>();

   @Unique
   private final List<Boolean> lifestealutils$filterOptionSelectedStates = new ArrayList<>();

   @Unique
   private static final List<String> lifestealutils$cachedFilterOptionLabels = new ArrayList<>();

   @Unique
   private static final List<Boolean> lifestealutils$cachedFilterOptionSelectedStates = new ArrayList<>();

   @Unique
   private static boolean lifestealutils$allowFilterOverlayFromAuctionItems = false;

   @Unique
   private boolean lifestealutils$hasAnyFilterSelected = false;

   @Unique
   private final Set<String> lifestealutils$missingMetaWarnings = new HashSet<>();

   @Unique
   private final List<Integer> lifestealutils$visibleAuctionSlots = new ArrayList<>();

   @Unique
   private Button lifestealutils$previousPageButton;

   @Unique
   private Button lifestealutils$nextPageButton;

   @Unique
   private EditBox lifestealutils$searchBox;

   @Unique
   private Button lifestealutils$searchButton;

   @Unique
   private Button lifestealutils$filterEditButton;

   @Unique
   private Button lifestealutils$filterSaveButton;

   @Unique
   private Button lifestealutils$filterCancelButton;

   @Unique
   private Button lifestealutils$claimItemsButton;

   @Unique
   private Button lifestealutils$yourListingsButton;

   @Unique
   private int lifestealutils$claimItemsSlotIndex = -1;

   @Unique
   private int lifestealutils$yourListingsSlotIndex = -1;

   @Unique
   private long lifestealutils$lastPageKeyClickMs = 0L;

   @Unique
   private int lifestealutils$lastNextClickStateId = Integer.MIN_VALUE;

   @Unique
   private int lifestealutils$lastPrevClickStateId = Integer.MIN_VALUE;

   @Unique
   private boolean lifestealutils$suppressNextSearchDialog = false;

   @Unique
   private String lifestealutils$visibleSearchQuery;

   @Unique
   private boolean lifestealutils$visibleSearchClearable;

   @Unique
   private int lifestealutils$cardScrollOffset = 0;

   @Unique
   private float lifestealutils$animatedCardScrollOffset = 0.0F;

   @Unique
   private boolean lifestealutils$cardScrollAnimationInitialized = false;

   @Shadow
   @Final
   protected T menu;

   @Shadow
   protected Slot hoveredSlot;

   @Shadow
   protected abstract void renderTooltip(GuiGraphics guiGraphics, int i, int j);

   @Unique
   private void lifestealutils$setScreenFocused(GuiEventListener element) {
      ((ContainerEventHandler) this).setFocused(element);
   }

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$renderAuctionOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      int mode = lifestealutils$getWrappedMode(self);
      if (mode == MODE_NONE) {
         return;
      }

      if (DEBUG_RENDER_UNDERLYING_GUI) {
         return;
      }

      lifestealutils$renderOverlayContent(self, mode, guiGraphics, mouseX, mouseY);
      ci.cancel();
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void lifestealutils$renderAuctionOverlayTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      if (!DEBUG_RENDER_UNDERLYING_GUI) {
         return;
      }

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      int mode = lifestealutils$getWrappedMode(self);
      if (mode == MODE_NONE) {
         return;
      }

      lifestealutils$renderOverlayContent(self, mode, guiGraphics, mouseX, mouseY);
   }

   @Unique
   private void lifestealutils$renderOverlayContent(AbstractContainerScreen<?> self, int mode, GuiGraphics guiGraphics, int mouseX, int mouseY) {

      Font font = Minecraft.getInstance().font;
      int screenWidth = self.width;
      int screenHeight = self.height;

      int[] metrics = lifestealutils$computeLayout(screenWidth, screenHeight);
      lifestealutils$refreshSortState(mode);
      lifestealutils$refreshSearchState(mode);
      lifestealutils$refreshFilterState(mode);
      ((ScreenAccessor) self).invokeRenderBlurredBackground(guiGraphics);

      lifestealutils$ensureFooterButtons(self);
      lifestealutils$ensureSearchControls(self);
      lifestealutils$ensureFilterHeaderButtons(self);
      lifestealutils$ensureSidebarActionButtons(self);
      lifestealutils$renderSidebar(guiGraphics, font, metrics, mode, mouseX, mouseY);
      Slot hoveredCardSlot = lifestealutils$renderMainContentCards(guiGraphics, font, metrics, mode, mouseX, mouseY);
      lifestealutils$layoutAndRenderFooter(guiGraphics, metrics, mode, mouseX, mouseY);
      this.hoveredSlot = hoveredCardSlot;
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$cancelVanillaBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      if (!DEBUG_RENDER_UNDERLYING_GUI && lifestealutils$getWrappedMode(self) != MODE_NONE) {
         ci.cancel();
      }
   }

   //? if >1.21.8 {
   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleArrowPaging(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      int mode = lifestealutils$getWrappedMode(self);
      if (mode == MODE_NONE) {
         return;
      }

      if (mode == MODE_ITEMS && keyEvent.isEscape() && lifestealutils$isSearchActiveInUi()) {
         cir.setReturnValue(lifestealutils$resetSearch());
         return;
      }

      if (mode == MODE_FILTER_EDIT && keyEvent.isEscape()) {
         cir.setReturnValue(lifestealutils$cancelFilterChanges());
         return;
      }

      if (mode == MODE_ITEMS && this.lifestealutils$searchBox != null && this.lifestealutils$searchBox.isFocused()) {
         if (keyEvent.key() == GLFW.GLFW_KEY_ENTER || keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
            cir.setReturnValue(lifestealutils$isSearchActiveInUi() ? lifestealutils$resetSearch() : lifestealutils$triggerSearch());
            return;
         }
         if (keyEvent.isEscape()) {
            lifestealutils$setScreenFocused(null);
            cir.setReturnValue(true);
            return;
         }
         // Consume all key events while the search box is focused — prevents every keybind
         // (inventory close, hotbar selection, etc.) from firing. Characters reach the
         // EditBox through Screen.charTyped → ContainerEventHandler naturally.
         this.lifestealutils$searchBox.keyPressed(keyEvent);
         cir.setReturnValue(true);
         return;
      }

      int keyCode = keyEvent.key();
      if (keyCode != GLFW.GLFW_KEY_RIGHT && keyCode != GLFW.GLFW_KEY_LEFT) {
         return;
      }

      if (mode != MODE_ITEMS) {
         cir.setReturnValue(true);
         return;
      }

      boolean forward = keyCode == GLFW.GLFW_KEY_RIGHT;
      if (lifestealutils$clickPageButton(forward)) {
         cir.setReturnValue(true);
      } else {
         cir.setReturnValue(true);
      }
   }
   //?}

   //? if >1.21.8 {
   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleOverlayClick(MouseButtonEvent mouseButtonEvent, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      int mode = lifestealutils$getWrappedMode(self);
      if (mode == MODE_NONE) {
         return;
      }

      if (this.lifestealutils$searchBox != null) {
         lifestealutils$setScreenFocused(null);
      }
      if (mouseButtonEvent.button() != 0) {
         cir.setReturnValue(true);
         return;
      }

      int[] metrics = lifestealutils$computeLayout(self.width, self.height);
      if (lifestealutils$trySearchControlClick(mode, mouseButtonEvent.x(), mouseButtonEvent.y())) {
         cir.setReturnValue(true);
         return;
      }
      if (lifestealutils$tryFooterButtonClick(metrics, mode, mouseButtonEvent.x(), mouseButtonEvent.y())) {
         cir.setReturnValue(true);
         return;
      }
      if (lifestealutils$tryFilterHeaderButtonClick(mode, mouseButtonEvent.x(), mouseButtonEvent.y())) {
         cir.setReturnValue(true);
         return;
      }
      if (lifestealutils$trySidebarActionButtonClick(mode, mouseButtonEvent.x(), mouseButtonEvent.y())) {
         cir.setReturnValue(true);
         return;
      }

      boolean consumed = lifestealutils$handleSidebarButtonClick(metrics, mode, mouseButtonEvent.x(), mouseButtonEvent.y());
      if (!consumed) {
         consumed = lifestealutils$handleCardClick(metrics, mode, mouseButtonEvent.x(), mouseButtonEvent.y());
      }
      cir.setReturnValue(true);
   }

   @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleCardScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      int mode = lifestealutils$getWrappedMode(self);
      if (mode == MODE_NONE) {
         return;
      }
      if (mode != MODE_ITEMS) {
         cir.setReturnValue(true);
         return;
      }
      int[] metrics = lifestealutils$computeLayout(self.width, self.height);
      if (!lifestealutils$isInsideMainContent(metrics, mode, mouseX, mouseY)) {
         return;
      }

      int step = CARD_HEIGHT + CARD_GAP;
      int maxScroll = Math.max(0, lifestealutils$getMaxCardScroll(metrics));
      int delta = (int) Math.round(-verticalAmount * step);
      this.lifestealutils$cardScrollOffset = Math.max(0, Math.min(maxScroll, this.lifestealutils$cardScrollOffset + delta));
      if (!this.lifestealutils$cardScrollAnimationInitialized) {
         this.lifestealutils$animatedCardScrollOffset = this.lifestealutils$cardScrollOffset;
         this.lifestealutils$cardScrollAnimationInitialized = true;
      }
      cir.setReturnValue(true);
   }
   //?} else {
   /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleOverlayClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      int mode = lifestealutils$getWrappedMode(self);
      if (mode == MODE_NONE) {
         return;
      }
      if (button != 0) {
         cir.setReturnValue(true);
         return;
      }
      int[] metrics = lifestealutils$computeLayout(self.width, self.height);
      boolean consumed = lifestealutils$handleSidebarButtonClick(metrics, mode, mouseX, mouseY);
      if (!consumed) {
         consumed = lifestealutils$handleCardClick(metrics, mode, mouseX, mouseY);
      }
      cir.setReturnValue(consumed || true);
   }
   *///?}

   @Unique
   private int lifestealutils$getWrappedMode(AbstractContainerScreen<?> self) {
      if (!Config.isCustomAhInterfaceEnabled()) {
         lifestealutils$allowFilterOverlayFromAuctionItems = false;
         return MODE_NONE;
      }
      String title = self.getTitle().getString();
      if (AUCTION_ITEMS_TITLE.equals(title)) {
         lifestealutils$allowFilterOverlayFromAuctionItems = true;
         return MODE_ITEMS;
      }
      if (FILTER_TITLE.equals(title) && lifestealutils$allowFilterOverlayFromAuctionItems) {
         return MODE_FILTER_EDIT;
      }
      lifestealutils$allowFilterOverlayFromAuctionItems = false;
      return MODE_NONE;
   }

   @Unique
   private void lifestealutils$renderSidebar(GuiGraphics guiGraphics, Font font, int[] metrics, int mode, int mouseX, int mouseY) {
      int contentY = metrics[IDX_CONTENT_Y];
      int sidebarX = metrics[IDX_SIDEBAR_X];
      int sidebarWidth = metrics[IDX_SIDEBAR_WIDTH];
      int sectionX = sidebarX + INNER_PADDING;
      int sectionWidth = sidebarWidth - INNER_PADDING * 2;
      int currentY = contentY + BASELINE_TOP_PADDING;

      if (mode == MODE_ITEMS && lifestealutils$isSearchActiveInUi()) {
         Component title = Component.literal("Searching for '" + this.lifestealutils$visibleSearchQuery + "'");
         guiGraphics.drawString(font, title, sectionX + 2, currentY - SEARCH_TITLE_HEIGHT, 0xFFE2E2E2);
      }

      guiGraphics.fill(sectionX, currentY, sectionX + sectionWidth, currentY + SEARCH_HEIGHT, DISABLED_BACKGROUND);
      lifestealutils$layoutSearchControls(sectionX, currentY, sectionWidth, mode);
      if (this.lifestealutils$searchBox != null) {
         lifestealutils$renderSearchText(guiGraphics, font);
      }
      if (this.lifestealutils$searchButton != null) {
         this.lifestealutils$searchButton.render(guiGraphics, mouseX, mouseY, 0.0f);
      }

      currentY += SEARCH_HEIGHT + SECTION_GAP;
      currentY = lifestealutils$renderSortContainer(guiGraphics, font, sectionX, currentY, sectionWidth, mode);
      currentY += SECTION_GAP;
      currentY = lifestealutils$renderFiltersContainer(guiGraphics, font, sectionX, currentY, sectionWidth, mode, mouseX, mouseY);
      lifestealutils$layoutAndRenderSidebarActionButtons(guiGraphics, sectionX, currentY + SECTION_GAP, sectionWidth, mode, mouseX, mouseY);
   }

   @Unique
   private void lifestealutils$ensureSearchControls(AbstractContainerScreen<?> self) {
      if (this.lifestealutils$searchBox != null && this.lifestealutils$searchButton != null) {
         return;
      }

      this.lifestealutils$searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, SEARCH_HEIGHT, Component.literal("Search"));
      this.lifestealutils$searchBox.setHint(Component.literal("Search"));
      this.lifestealutils$searchBox.setBordered(false);
      this.lifestealutils$searchBox.setTextColor(0xFFECECEC);
      this.lifestealutils$searchBox.setTextColorUneditable(0xFFB5B5B5);

      this.lifestealutils$searchButton = Button.builder(Component.literal("Search"), button -> {
      }).width(SEARCH_BUTTON_WIDTH).build();

      ScreenAccessor accessor = (ScreenAccessor) self;
      accessor.invokeAddRenderableWidget(this.lifestealutils$searchBox);
      accessor.invokeAddRenderableWidget(this.lifestealutils$searchButton);
   }

   @Unique
   private void lifestealutils$ensureFilterHeaderButtons(AbstractContainerScreen<?> self) {
      if (this.lifestealutils$filterEditButton != null && this.lifestealutils$filterSaveButton != null && this.lifestealutils$filterCancelButton != null) {
         return;
      }

      this.lifestealutils$filterEditButton = Button.builder(Component.literal("Edit"), button -> {
      }).width(FILTER_HEADER_BUTTON_WIDTH).build();
      this.lifestealutils$filterSaveButton = Button.builder(Component.literal("Save"), button -> {
      }).width(FILTER_HEADER_BUTTON_WIDTH).build();
      this.lifestealutils$filterCancelButton = Button.builder(Component.literal("Cancel"), button -> {
      }).width(FILTER_HEADER_BUTTON_WIDTH).build();

      ScreenAccessor accessor = (ScreenAccessor) self;
      accessor.invokeAddRenderableWidget(this.lifestealutils$filterEditButton);
      accessor.invokeAddRenderableWidget(this.lifestealutils$filterSaveButton);
      accessor.invokeAddRenderableWidget(this.lifestealutils$filterCancelButton);
   }

   @Unique
   private void lifestealutils$ensureSidebarActionButtons(AbstractContainerScreen<?> self) {
      if (this.lifestealutils$claimItemsButton != null && this.lifestealutils$yourListingsButton != null) {
         return;
      }

      this.lifestealutils$claimItemsButton = Button.builder(Component.translatable(AUCTION_CLAIM_ITEMS_BUTTON_KEY), button -> {
      }).width(100).build();
      this.lifestealutils$yourListingsButton = Button.builder(Component.translatable(AUCTION_YOUR_LISTINGS_BUTTON_KEY), button -> {
      }).width(100).build();

      ScreenAccessor accessor = (ScreenAccessor) self;
      accessor.invokeAddRenderableWidget(this.lifestealutils$claimItemsButton);
      accessor.invokeAddRenderableWidget(this.lifestealutils$yourListingsButton);
   }

   @Unique
   private void lifestealutils$layoutAndRenderSidebarActionButtons(GuiGraphics guiGraphics, int x, int y, int width, int mode, int mouseX, int mouseY) {
      if (this.lifestealutils$claimItemsButton == null || this.lifestealutils$yourListingsButton == null) {
         return;
      }

      boolean visible = mode != MODE_NONE;
      this.lifestealutils$claimItemsButton.visible = visible;
      this.lifestealutils$yourListingsButton.visible = visible;
      if (!visible) {
         return;
      }

      this.lifestealutils$claimItemsButton.setX(x);
      this.lifestealutils$claimItemsButton.setY(y);
      this.lifestealutils$claimItemsButton.setWidth(width);
      this.lifestealutils$claimItemsButton.setHeight(HEADER_HEIGHT);
      this.lifestealutils$claimItemsButton.active = this.lifestealutils$claimItemsSlotIndex >= 0;
      this.lifestealutils$claimItemsButton.setTooltip(this.lifestealutils$claimItemsButton.active ? null : Tooltip.create(Component.translatable(AUCTION_CLAIM_ITEMS_UNAVAILABLE_KEY)));
      this.lifestealutils$claimItemsButton.render(guiGraphics, mouseX, mouseY, 0.0F);

      int listingsY = y + HEADER_HEIGHT;
      this.lifestealutils$yourListingsButton.setX(x);
      this.lifestealutils$yourListingsButton.setY(listingsY);
      this.lifestealutils$yourListingsButton.setWidth(width);
      this.lifestealutils$yourListingsButton.setHeight(HEADER_HEIGHT);
      this.lifestealutils$yourListingsButton.active = this.lifestealutils$yourListingsSlotIndex >= 0;
      this.lifestealutils$yourListingsButton.setTooltip(this.lifestealutils$yourListingsButton.active ? null : Tooltip.create(Component.translatable(AUCTION_YOUR_LISTINGS_UNAVAILABLE_KEY)));
      this.lifestealutils$yourListingsButton.render(guiGraphics, mouseX, mouseY, 0.0F);
   }

   @Unique
   private boolean lifestealutils$trySidebarActionButtonClick(int mode, double mouseX, double mouseY) {
      if (mode == MODE_NONE) {
         return false;
      }
      if (this.lifestealutils$claimItemsButton == null || this.lifestealutils$yourListingsButton == null) {
         return false;
      }

      if (this.lifestealutils$claimItemsButton.visible
              && lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$claimItemsButton.getX(), this.lifestealutils$claimItemsButton.getY(), this.lifestealutils$claimItemsButton.getWidth(), this.lifestealutils$claimItemsButton.getHeight())) {
         if (!this.lifestealutils$claimItemsButton.active) {
            return true;
         }
         return lifestealutils$clickClaimItemsButton();
      }

      if (this.lifestealutils$yourListingsButton.visible
              && lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$yourListingsButton.getX(), this.lifestealutils$yourListingsButton.getY(), this.lifestealutils$yourListingsButton.getWidth(), this.lifestealutils$yourListingsButton.getHeight())) {
         if (!this.lifestealutils$yourListingsButton.active) {
            return true;
         }
         return lifestealutils$clickYourListingsButton();
      }

      return false;
   }

   @Unique
   private void lifestealutils$layoutSearchControls(int sectionX, int currentY, int sectionWidth, int mode) {
      if (this.lifestealutils$searchBox == null || this.lifestealutils$searchButton == null) {
         return;
      }

      int buttonWidth = SEARCH_BUTTON_WIDTH;
      int inputWidth = Math.max(30, sectionWidth - buttonWidth - 6);
      this.lifestealutils$searchBox.setX(sectionX + SEARCH_TEXT_LEFT_PAD);
      this.lifestealutils$searchBox.setY(currentY);
      this.lifestealutils$searchBox.setWidth(inputWidth - SEARCH_TEXT_LEFT_PAD - 1);
      this.lifestealutils$searchBox.setHeight(SEARCH_HEIGHT);

      this.lifestealutils$searchButton.setX(sectionX + sectionWidth - buttonWidth);
      this.lifestealutils$searchButton.setY(currentY);
      this.lifestealutils$searchButton.setWidth(buttonWidth);
      this.lifestealutils$searchButton.setHeight(SEARCH_HEIGHT);
      this.lifestealutils$searchButton.visible = true;
      this.lifestealutils$searchButton.active = mode == MODE_ITEMS;
      this.lifestealutils$searchButton.setMessage(Component.literal(lifestealutils$isSearchActiveInUi() ? "Reset" : "Search"));
   }

   @Unique
   private void lifestealutils$renderSearchText(GuiGraphics guiGraphics, Font font) {
      if (this.lifestealutils$searchBox == null) {
         return;
      }

      String value = this.lifestealutils$searchBox.getValue();
      boolean focused = this.lifestealutils$searchBox.isFocused();
      String display = value;
      int color = 0xFFECECEC;
      if (!focused && (display == null || display.isEmpty())) {
         display = "Search";
         color = 0xFFB5B5B5;
      }

      if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
         display = display + "|";
      }

      int textX = this.lifestealutils$searchBox.getX() + 2;
      int textY = this.lifestealutils$searchBox.getY() + (this.lifestealutils$searchBox.getHeight() - font.lineHeight) / 2;
      guiGraphics.drawString(font, display, textX, textY, color);
   }

   @Unique
   private int lifestealutils$renderSortContainer(GuiGraphics guiGraphics, Font font, int x, int y, int width, int mode) {
      guiGraphics.fillGradient(x, y, x + width, y + HEADER_HEIGHT, HEADER_GRADIENT_TOP, HEADER_GRADIENT_BOTTOM);
      guiGraphics.fill(x, y, x + SIDEBAR_STROKE_WIDTH, y + HEADER_HEIGHT, ACCENT_STROKE);

      int textStartX = x + SIDEBAR_STROKE_WIDTH + 8;
      int titleY = y + (HEADER_HEIGHT - font.lineHeight) / 2;
      guiGraphics.drawString(font, Component.literal("Sort By").withStyle(style -> style.withBold(true)), textStartX, titleY, 0xFFFFFFFF);

      List<String> options = this.lifestealutils$sortOptionLabels;
      int buttonY = y + HEADER_HEIGHT;
      boolean sortEnabled = mode == MODE_ITEMS;
      for (int i = 0; i < options.size(); i++) {
         boolean enabled = sortEnabled && this.lifestealutils$selectedSortIndex == i;
         int background = enabled ? ENABLED_BACKGROUND : DISABLED_BACKGROUND;
         int textColor = enabled ? ENABLED_TEXT : DISABLED_TEXT;

         guiGraphics.fill(x, buttonY, x + width, buttonY + HEADER_HEIGHT, background);
         if (enabled) {
            guiGraphics.fill(x, buttonY, x + SIDEBAR_STROKE_WIDTH, buttonY + HEADER_HEIGHT, ACCENT_STROKE);
         }

         int textY = buttonY + (HEADER_HEIGHT - font.lineHeight) / 2;
         Component prefixedLabel = Component.literal(SIDEBAR_OPTION_PREFIX + options.get(i));
         guiGraphics.drawString(font, prefixedLabel, textStartX, textY, textColor);
         buttonY += HEADER_HEIGHT;
      }

      return buttonY;
   }

   @Unique
   private int lifestealutils$renderFiltersContainer(GuiGraphics guiGraphics, Font font, int x, int y, int width, int mode, int mouseX, int mouseY) {
      guiGraphics.fillGradient(x, y, x + width, y + HEADER_HEIGHT, HEADER_GRADIENT_TOP, HEADER_GRADIENT_BOTTOM);
      guiGraphics.fill(x, y, x + SIDEBAR_STROKE_WIDTH, y + HEADER_HEIGHT, ACCENT_STROKE);

      int textStartX = x + SIDEBAR_STROKE_WIDTH + 8;
      int titleY = y + (HEADER_HEIGHT - font.lineHeight) / 2;
      String title = mode == MODE_FILTER_EDIT ? "Filter" : "Filters";
      guiGraphics.drawString(font, Component.literal(title).withStyle(style -> style.withBold(true)), textStartX, titleY, 0xFFFFFFFF);
      lifestealutils$layoutAndRenderFilterHeaderButtons(guiGraphics, x, y, width, mode, mouseX, mouseY);

      int rowY = y + HEADER_HEIGHT;
      if (mode == MODE_ITEMS) {
         for (int optionIndex = 0; optionIndex < this.lifestealutils$filterOptionLabels.size(); optionIndex++) {
            boolean selected = this.lifestealutils$filterOptionSelectedStates.get(optionIndex);
            int background = selected ? ENABLED_BACKGROUND : DISABLED_BACKGROUND;
            int textColor = selected ? ENABLED_TEXT : DISABLED_TEXT;
            guiGraphics.fill(x, rowY, x + width, rowY + HEADER_HEIGHT, background);
            if (selected) {
               guiGraphics.fill(x, rowY, x + SIDEBAR_STROKE_WIDTH, rowY + HEADER_HEIGHT, ACCENT_STROKE);
            }
            int textY = rowY + (HEADER_HEIGHT - font.lineHeight) / 2;
            guiGraphics.drawString(font, Component.literal(SIDEBAR_OPTION_PREFIX + this.lifestealutils$filterOptionLabels.get(optionIndex)), textStartX, textY, textColor);
            rowY += HEADER_HEIGHT;
         }
      } else if (mode == MODE_FILTER_EDIT) {
         for (int optionIndex = 0; optionIndex < this.lifestealutils$filterOptionLabels.size(); optionIndex++) {
            boolean selected = this.lifestealutils$filterOptionSelectedStates.get(optionIndex);
            int background = selected ? ENABLED_BACKGROUND : DISABLED_BACKGROUND;
            int textColor = selected ? ENABLED_TEXT : DISABLED_TEXT;
            guiGraphics.fill(x, rowY, x + width, rowY + HEADER_HEIGHT, background);
            if (selected) {
               guiGraphics.fill(x, rowY, x + SIDEBAR_STROKE_WIDTH, rowY + HEADER_HEIGHT, ACCENT_STROKE);
            }
            int textY = rowY + (HEADER_HEIGHT - font.lineHeight) / 2;
            guiGraphics.drawString(font, Component.literal(SIDEBAR_OPTION_PREFIX + this.lifestealutils$filterOptionLabels.get(optionIndex)), textStartX, textY, textColor);
            rowY += HEADER_HEIGHT;
         }
      } else {
         guiGraphics.fill(x, rowY, x + width, rowY + HEADER_HEIGHT, DISABLED_BACKGROUND);
         int textY = rowY + (HEADER_HEIGHT - font.lineHeight) / 2;
         guiGraphics.drawString(font, Component.literal(SIDEBAR_OPTION_PREFIX + "Unavailable"), textStartX, textY, DISABLED_TEXT);
         rowY += HEADER_HEIGHT;
      }

      return rowY;
   }

   @Unique
   private void lifestealutils$layoutAndRenderFilterHeaderButtons(GuiGraphics guiGraphics, int x, int y, int width, int mode, int mouseX, int mouseY) {
      if (this.lifestealutils$filterEditButton == null || this.lifestealutils$filterSaveButton == null || this.lifestealutils$filterCancelButton == null) {
         return;
      }

      this.lifestealutils$filterEditButton.visible = false;
      this.lifestealutils$filterSaveButton.visible = false;
      this.lifestealutils$filterCancelButton.visible = false;

      int buttonY = y;
      if (mode == MODE_ITEMS) {
         this.lifestealutils$filterEditButton.setX(x + width - FILTER_HEADER_BUTTON_WIDTH);
         this.lifestealutils$filterEditButton.setY(buttonY);
         this.lifestealutils$filterEditButton.setWidth(FILTER_HEADER_BUTTON_WIDTH);
         this.lifestealutils$filterEditButton.setHeight(HEADER_HEIGHT);
         this.lifestealutils$filterEditButton.visible = true;
         this.lifestealutils$filterEditButton.active = this.lifestealutils$filterItemSlotIndex >= 0;
         this.lifestealutils$filterEditButton.render(guiGraphics, mouseX, mouseY, 0.0f);
         return;
      }

      if (mode == MODE_FILTER_EDIT) {
         int cancelX = x + width - FILTER_HEADER_BUTTON_WIDTH;
         int saveX = cancelX - FILTER_HEADER_BUTTON_GAP - FILTER_HEADER_BUTTON_WIDTH;

         this.lifestealutils$filterSaveButton.setX(saveX);
         this.lifestealutils$filterSaveButton.setY(buttonY);
         this.lifestealutils$filterSaveButton.setWidth(FILTER_HEADER_BUTTON_WIDTH);
         this.lifestealutils$filterSaveButton.setHeight(HEADER_HEIGHT);
         this.lifestealutils$filterSaveButton.visible = true;
         this.lifestealutils$filterSaveButton.active = this.lifestealutils$filterConfirmSlotIndex >= 0 || this.lifestealutils$filterGoBackSlotIndex >= 0;
         this.lifestealutils$filterSaveButton.render(guiGraphics, mouseX, mouseY, 0.0f);

         this.lifestealutils$filterCancelButton.setX(cancelX);
         this.lifestealutils$filterCancelButton.setY(buttonY);
         this.lifestealutils$filterCancelButton.setWidth(FILTER_HEADER_BUTTON_WIDTH);
         this.lifestealutils$filterCancelButton.setHeight(HEADER_HEIGHT);
         this.lifestealutils$filterCancelButton.visible = true;
         this.lifestealutils$filterCancelButton.active = this.lifestealutils$filterGoBackSlotIndex >= 0;
         this.lifestealutils$filterCancelButton.render(guiGraphics, mouseX, mouseY, 0.0f);
      }
   }

   @Unique
   private Slot lifestealutils$renderMainContentCards(GuiGraphics guiGraphics, Font font, int[] metrics, int mode, int mouseX, int mouseY) {
      int contentY = metrics[IDX_CONTENT_Y];
      int contentHeight = metrics[IDX_CONTENT_HEIGHT];
      int mainX = metrics[IDX_MAIN_X];
      int mainWidth = metrics[IDX_MAIN_WIDTH];
      int panelLeft = mainX;
      int panelTop = contentY + BASELINE_TOP_PADDING;
      int panelRight = mainX + mainWidth;
      int panelBottom = contentY + contentHeight;
      guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, MAIN_BACKGROUND);

      int cardsLeft = panelLeft + INNER_PADDING;
      int cardsTop = panelTop + INNER_PADDING;
      int cardsRight = panelRight - INNER_PADDING;
      int cardsBottom = panelBottom - INNER_PADDING - FOOTER_GAP - FOOTER_HEIGHT;

      if (mode != MODE_ITEMS) {
         this.lifestealutils$visibleAuctionSlots.clear();
         this.lifestealutils$cardScrollOffset = 0;
         this.lifestealutils$animatedCardScrollOffset = 0.0F;
         this.lifestealutils$cardScrollAnimationInitialized = false;
         Component empty = Component.literal("No auctions in this view");
         int textX = cardsLeft + 6;
         int textY = cardsTop + 6;
         guiGraphics.drawString(font, empty, textX, textY, 0xFFC9C9C9);
         return null;
      }

      int iconXOffset = CARD_CONTENT_PADDING_X;
      int iconCenterYOffset = CARD_HEIGHT / 2;
      int textXOffset = iconXOffset + 16 + 10;
      int nameYInset = 7;
      int metaYInset = 20;
      int priceYInset = 32;

      int cardsWidth = cardsRight - cardsLeft;
      int columns = lifestealutils$getCardColumns(cardsWidth);
      int totalGapWidth = (columns - 1) * CARD_GAP;
      int availableForCards = Math.max(1, cardsWidth - (CARD_ROW_SIDE_MARGIN * 2) - totalGapWidth);
      int cardWidth = Math.max(MIN_CARD_WIDTH, availableForCards / columns);
      int rowStartX = cardsLeft + CARD_ROW_SIDE_MARGIN;
      int topContainerSlotCount = Math.max(0, this.menu.slots.size() - 36);
      lifestealutils$visibleAuctionSlots.clear();
      lifestealutils$visibleAuctionSlots.addAll(lifestealutils$getRenderableAuctionSlots(topContainerSlotCount));
      CardViewportLayout cardLayout = lifestealutils$computeCardViewportLayout(metrics);
      lifestealutils$clampCardScroll(cardLayout.maxScroll());
      lifestealutils$updateAnimatedCardScroll();
      int renderedScrollOffset = lifestealutils$getRenderedCardScrollOffset();

      Slot hovered = null;
      boolean clipApplied = lifestealutils$pushCardViewportClip(guiGraphics, cardLayout.viewportLeft(), cardLayout.viewportTop(), cardLayout.viewportRight(), cardLayout.viewportBottom());
      try {
         for (int visualIndex = 0; visualIndex < lifestealutils$visibleAuctionSlots.size(); visualIndex++) {
            int i = lifestealutils$visibleAuctionSlots.get(visualIndex);
            int col = visualIndex % columns;
            int row = visualIndex / columns;
            int cardX = rowStartX + col * (cardWidth + CARD_GAP);
            int cardY = cardsTop + row * (CARD_HEIGHT + CARD_GAP) - renderedScrollOffset;

            if (cardY + CARD_HEIGHT <= cardLayout.viewportTop()) {
               continue;
            }
            if (cardY >= cardLayout.viewportBottom()) {
               break;
            }

            ItemStack stack = this.menu.slots.get(i).getItem();
            if (stack.isEmpty()) {
               continue;
            }

            guiGraphics.fill(cardX, cardY, cardX + cardWidth, cardY + CARD_HEIGHT, 0x33000000);
            guiGraphics.fill(cardX, cardY, cardX + cardWidth, cardY + 1, 0x22FFFFFF);
            guiGraphics.fill(cardX, cardY + CARD_HEIGHT - 1, cardX + cardWidth, cardY + CARD_HEIGHT, 0x22000000);

            int iconCenterX = cardX + iconXOffset + 8;
            int iconCenterY = cardY + iconCenterYOffset;
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(iconCenterX, iconCenterY);
            guiGraphics.pose().scale(CARD_ICON_SCALE, CARD_ICON_SCALE);
            guiGraphics.renderItem(stack, -8, -8);
            guiGraphics.pose().popMatrix();

            int textX = cardX + textXOffset;
            Component nameComponent = stack.getHoverName();
            guiGraphics.drawString(font, nameComponent, textX, cardY + nameYInset, 0xFFFFFFFF);

            AuctionMeta meta = lifestealutils$extractAuctionMeta(stack, i);
            String seller = meta.seller();
            String timeRemaining = lifestealutils$formatCompactTime(meta.timeRemaining());
            String price = meta.price();
            String timePrefix = "⌛ " + timeRemaining;
            int metaY = cardY + metaYInset;
            int timeTextX = textX;
            if (meta.bidAuction()) {
               String badgeLabel = "Bid";
               int badgeWidth = font.width(badgeLabel) + (BID_BADGE_HORIZONTAL_PADDING * 2);
               int badgeHeight = font.lineHeight + 2;
               int badgeTop = metaY - 1;
               int badgeBottom = badgeTop + badgeHeight;
               guiGraphics.fill(timeTextX, badgeTop, timeTextX + badgeWidth, badgeBottom, BID_BADGE_BACKGROUND);
               guiGraphics.drawString(font, badgeLabel, timeTextX + BID_BADGE_HORIZONTAL_PADDING, metaY, BID_BADGE_TEXT_COLOR);
               timeTextX += badgeWidth + 6;
            }
            guiGraphics.drawString(font, timePrefix, timeTextX, metaY, 0xFFB6B6B6);
            int afterTimeX = timeTextX + font.width(timePrefix);
            String dot = " ● ";
            guiGraphics.drawString(font, dot, afterTimeX, metaY, 0xFF7A7A7A);
            int sellerX = afterTimeX + font.width(dot);
            guiGraphics.drawString(font, seller, sellerX, metaY, 0xFFFFFFFF);
            guiGraphics.drawString(font, price, textX, cardY + priceYInset, 0xFFD8D8D8);

            if (lifestealutils$isMouseInsideVisibleCardSlice(mouseX, mouseY, cardX, cardY, cardWidth, CARD_HEIGHT, cardLayout.viewportTop(), cardLayout.viewportBottom())) {
               hovered = this.menu.slots.get(i);
            }
         }
      } finally {
         lifestealutils$popCardViewportClip(guiGraphics, clipApplied);
      }

      guiGraphics.fill(cardLayout.viewportLeft(), cardLayout.viewportBottom() - 1, cardLayout.viewportRight(), cardLayout.viewportBottom(), VIEWPORT_BOTTOM_EDGE);
      int bottomShadowTop = Math.max(cardLayout.viewportTop(), cardLayout.viewportBottom() - VIEWPORT_WALL_DEPTH);
      guiGraphics.fill(cardLayout.viewportLeft(), bottomShadowTop, cardLayout.viewportRight(), cardLayout.viewportBottom(), VIEWPORT_BOTTOM_SHADOW);

      int maxScroll = Math.max(0, cardLayout.maxScroll());
      if (maxScroll > 0) {
         int trackX = cardLayout.viewportRight() - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
         int trackY = cardLayout.viewportTop();
         int trackHeight = Math.max(1, cardLayout.viewportBottom() - cardLayout.viewportTop());
         guiGraphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0x22000000);

         float visibleRatio = (float) trackHeight / (float) (trackHeight + maxScroll);
         int thumbHeight = Math.max(14, Math.round(trackHeight * visibleRatio));
         float scrollProgress = (float) renderedScrollOffset / (float) maxScroll;
         int thumbTravel = Math.max(0, trackHeight - thumbHeight);
         int thumbY = trackY + Math.round(scrollProgress * thumbTravel);
         guiGraphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0x88FF989A);
      }

      return hovered;
   }

   @Unique
   private void lifestealutils$ensureFooterButtons(AbstractContainerScreen<?> self) {
      if (this.lifestealutils$previousPageButton != null && this.lifestealutils$nextPageButton != null) {
         return;
      }

      this.lifestealutils$previousPageButton = Button.builder(Component.literal("Last Page"), button -> {
      }).width(FOOTER_BUTTON_WIDTH).build();
      this.lifestealutils$nextPageButton = Button.builder(Component.literal("Next Page"), button -> {
      }).width(FOOTER_BUTTON_WIDTH).build();

      ScreenAccessor accessor = (ScreenAccessor) self;
      accessor.invokeAddRenderableWidget(this.lifestealutils$previousPageButton);
      accessor.invokeAddRenderableWidget(this.lifestealutils$nextPageButton);
   }

   @Unique
   private void lifestealutils$layoutAndRenderFooter(GuiGraphics guiGraphics, int[] metrics, int mode, int mouseX, int mouseY) {
      if (this.lifestealutils$previousPageButton == null || this.lifestealutils$nextPageButton == null) {
         return;
      }

      int contentY = metrics[IDX_CONTENT_Y];
      int contentHeight = metrics[IDX_CONTENT_HEIGHT];
      int mainX = metrics[IDX_MAIN_X];
      int mainWidth = metrics[IDX_MAIN_WIDTH];
      int panelBottom = contentY + contentHeight;

      int buttonY = panelBottom - INNER_PADDING - FOOTER_HEIGHT;
      int leftX = mainX + INNER_PADDING;
      int rightX = (mainX + mainWidth) - INNER_PADDING - FOOTER_BUTTON_WIDTH;

      this.lifestealutils$previousPageButton.setX(leftX);
      this.lifestealutils$previousPageButton.setY(buttonY);
      this.lifestealutils$nextPageButton.setX(rightX);
      this.lifestealutils$nextPageButton.setY(buttonY);

      boolean hasPrevious = mode == MODE_ITEMS && lifestealutils$findPageSlot(PREV_PAGE_NAME, ALT_PREV_PAGE_NAME) >= 0;
      boolean hasNext = mode == MODE_ITEMS && lifestealutils$findPageSlot(NEXT_PAGE_NAME) >= 0;

      this.lifestealutils$previousPageButton.visible = hasPrevious;
      this.lifestealutils$previousPageButton.active = hasPrevious;
      this.lifestealutils$nextPageButton.visible = hasNext;
      this.lifestealutils$nextPageButton.active = hasNext;

      if (hasPrevious) {
         this.lifestealutils$previousPageButton.render(guiGraphics, mouseX, mouseY, 0.0f);
      }
      if (hasNext) {
         this.lifestealutils$nextPageButton.render(guiGraphics, mouseX, mouseY, 0.0f);
      }
   }

   @Unique
   private boolean lifestealutils$tryFooterButtonClick(int[] metrics, int mode, double mouseX, double mouseY) {
      if (metrics == null) {
         return false;
      }
      if (mode != MODE_ITEMS) {
         return false;
      }
      if (this.lifestealutils$previousPageButton == null || this.lifestealutils$nextPageButton == null) {
         return false;
      }

      if (this.lifestealutils$previousPageButton.visible
              && lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$previousPageButton.getX(), this.lifestealutils$previousPageButton.getY(), this.lifestealutils$previousPageButton.getWidth(), this.lifestealutils$previousPageButton.getHeight())) {
         return lifestealutils$clickPageButton(false);
      }
      if (this.lifestealutils$nextPageButton.visible
              && lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$nextPageButton.getX(), this.lifestealutils$nextPageButton.getY(), this.lifestealutils$nextPageButton.getWidth(), this.lifestealutils$nextPageButton.getHeight())) {
         return lifestealutils$clickPageButton(true);
      }
      return false;
   }

   @Unique
   private AuctionMeta lifestealutils$extractAuctionMeta(ItemStack stack, int slotIndex) {
      String seller = null;
      String timeRemaining = null;
      String price = null;
      boolean bidAuction = false;

      ItemLore lore = stack.get(DataComponents.LORE);
      if (lore != null) {
         for (Component lineComponent : lore.lines()) {
            String line = lineComponent.getString();
            if (line == null) {
               continue;
            }
            String trimmed = line.trim();
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
      }

      if (seller == null || seller.isBlank() || timeRemaining == null || timeRemaining.isBlank() || price == null || price.isBlank()) {
         String key = slotIndex + "|" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getHoverName().getString();
         if (this.lifestealutils$missingMetaWarnings.add(key)) {
            StringBuilder loreDump = new StringBuilder();
            if (lore == null || lore.lines().isEmpty()) {
               loreDump.append("<no lore>");
            } else {
               for (int i = 0; i < lore.lines().size(); i++) {
                  if (i > 0) {
                     loreDump.append(" | ");
                  }
                  loreDump.append(lore.lines().get(i).getString());
               }
            }
            LOGGER.warn("[ah-overlay] missing auction meta: slot={} itemId={} name='{}' seller='{}' time='{}' price='{}' lore='{}'", slotIndex, BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getHoverName().getString(), seller, timeRemaining, price, loreDump);
         }
      }

      String safeSeller = (seller == null || seller.isBlank()) ? "Unknown Seller" : seller;
      String safeTime = (timeRemaining == null || timeRemaining.isBlank()) ? "Unknown" : timeRemaining;
      String safePrice = (price == null || price.isBlank()) ? "Unknown Price" : price;
      return new AuctionMeta(safeSeller, safeTime, safePrice, bidAuction);
   }

   private record AuctionMeta(String seller, String timeRemaining, String price, boolean bidAuction) {
   }

   @Unique
   private String lifestealutils$formatCompactTime(String raw) {
      if (raw == null || raw.isBlank()) {
         return "Unknown";
      }

      int days = -1;
      int hours = -1;
      int minutes = -1;
      int seconds = -1;

      String[] parts = raw.toLowerCase(Locale.ROOT).split(",");
      for (String part : parts) {
         String token = part.trim();
         if (token.isEmpty()) {
            continue;
         }
         int value = 0;
         int idx = 0;
         while (idx < token.length() && Character.isDigit(token.charAt(idx))) {
            value = (value * 10) + (token.charAt(idx) - '0');
            idx++;
         }
         if (idx == 0 || idx >= token.length()) {
            continue;
         }
         char unit = token.charAt(idx);
         switch (unit) {
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

      String normalized = raw.replace(',', ' ').trim();
      String[] fallbackParts = normalized.split("\\s+");
      if (fallbackParts.length >= 2) {
         return fallbackParts[0] + " " + fallbackParts[1];
      }
      return normalized;
   }

   @Unique
   private boolean lifestealutils$handleSidebarButtonClick(int[] metrics, int mode, double mouseX, double mouseY) {
      int contentY = metrics[IDX_CONTENT_Y];
      int sidebarX = metrics[IDX_SIDEBAR_X];
      int sidebarWidth = metrics[IDX_SIDEBAR_WIDTH];
      int sectionX = sidebarX + INNER_PADDING;
      int sectionWidth = sidebarWidth - INNER_PADDING * 2;

      int currentY = contentY + BASELINE_TOP_PADDING;
      int sortContainerY = currentY + SEARCH_HEIGHT + SECTION_GAP;
      int sortButtonsTop = sortContainerY + HEADER_HEIGHT;
      int sortButtonCount = this.lifestealutils$sortOptionLabels.size();
      int sortButtonBlockHeight = sortButtonCount * HEADER_HEIGHT;
      if (mode == MODE_ITEMS && sortButtonBlockHeight > 0 && lifestealutils$isInside(mouseX, mouseY, sectionX, sortButtonsTop, sectionWidth, sortButtonBlockHeight)) {
         return lifestealutils$clickSortItem();
      }

      int filterContainerY = sortButtonsTop + sortButtonBlockHeight + SECTION_GAP;
      int filterButtonsTop = filterContainerY + HEADER_HEIGHT;
      if (mode == MODE_ITEMS) {
         return false;
      }

      if (mode == MODE_FILTER_EDIT) {
         int rowCount = this.lifestealutils$filterOptionLabels.size();
         int blockHeight = rowCount * HEADER_HEIGHT;
         if (!lifestealutils$isInside(mouseX, mouseY, sectionX, filterButtonsTop, sectionWidth, blockHeight)) {
            return false;
         }
         int rowIndex = (int) ((mouseY - filterButtonsTop) / HEADER_HEIGHT);
         if (rowIndex < this.lifestealutils$filterOptionLabels.size()) {
            return lifestealutils$clickFilterOption(rowIndex);
         }
         return false;
      }

      return false;
   }

   @Unique
   private boolean lifestealutils$tryFilterHeaderButtonClick(int mode, double mouseX, double mouseY) {
      if (mode == MODE_ITEMS) {
         if (this.lifestealutils$filterEditButton != null
                 && this.lifestealutils$filterEditButton.visible
                 && lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$filterEditButton.getX(), this.lifestealutils$filterEditButton.getY(), this.lifestealutils$filterEditButton.getWidth(), this.lifestealutils$filterEditButton.getHeight())) {
            return lifestealutils$clickFilterEdit();
         }
         return false;
      }

      if (mode == MODE_FILTER_EDIT) {
         if (this.lifestealutils$filterSaveButton != null
                 && this.lifestealutils$filterSaveButton.visible
                 && lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$filterSaveButton.getX(), this.lifestealutils$filterSaveButton.getY(), this.lifestealutils$filterSaveButton.getWidth(), this.lifestealutils$filterSaveButton.getHeight())) {
            return lifestealutils$saveFilterChanges();
         }
         if (this.lifestealutils$filterCancelButton != null
                 && this.lifestealutils$filterCancelButton.visible
                 && lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$filterCancelButton.getX(), this.lifestealutils$filterCancelButton.getY(), this.lifestealutils$filterCancelButton.getWidth(), this.lifestealutils$filterCancelButton.getHeight())) {
            return lifestealutils$cancelFilterChanges();
         }
      }

      return false;
   }

   @Unique
   private boolean lifestealutils$trySearchControlClick(int mode, double mouseX, double mouseY) {
      if (mode != MODE_ITEMS) {
         return false;
      }
      if (this.lifestealutils$searchBox == null || this.lifestealutils$searchButton == null) {
         return false;
      }

      if (lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$searchBox.getX(), this.lifestealutils$searchBox.getY(), this.lifestealutils$searchBox.getWidth(), this.lifestealutils$searchBox.getHeight())) {
         lifestealutils$setScreenFocused(this.lifestealutils$searchBox);
         return true;
      }

      lifestealutils$setScreenFocused(null);
      if (lifestealutils$isInside(mouseX, mouseY, this.lifestealutils$searchButton.getX(), this.lifestealutils$searchButton.getY(), this.lifestealutils$searchButton.getWidth(), this.lifestealutils$searchButton.getHeight())) {
         return lifestealutils$isSearchActiveInUi() ? lifestealutils$resetSearch() : lifestealutils$triggerSearch();
      }

      return false;
   }

   @Unique
   private boolean lifestealutils$triggerSearch() {
      if (lifestealutils$isSearchActiveInUi()) {
         return true;
      }
      int searchSlot = lifestealutils$findSearchSlot();
      if (searchSlot < 0) {
         return true;
      }

      String query = this.lifestealutils$searchBox == null ? "" : this.lifestealutils$searchBox.getValue();
      if (query.isBlank()) {
         return true;
      }
      AhSearchAutomation.queueSearch(query);
      this.lifestealutils$suppressNextSearchDialog = true;

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.gameMode == null) {
         this.lifestealutils$suppressNextSearchDialog = false;
         return true;
      }

      client.gameMode.handleInventoryMouseClick(this.menu.containerId, searchSlot, 0, ClickType.PICKUP, client.player);
      return true;
   }

   @Unique
   private boolean lifestealutils$resetSearch() {
      int searchSlot = lifestealutils$findSearchSlot();
      if (searchSlot >= 0) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && client.gameMode != null) {
            client.gameMode.handleInventoryMouseClick(this.menu.containerId, searchSlot, 0, ClickType.PICKUP, client.player);
         }
      }
      AhSearchAutomation.setActiveQuery(null);
      AhSearchAutomation.queueSearch(null);
      this.lifestealutils$visibleSearchQuery = null;
      this.lifestealutils$visibleSearchClearable = false;
      if (this.lifestealutils$searchBox != null) {
         this.lifestealutils$searchBox.setValue("");
      }
      return true;
   }

   @Unique
   private int lifestealutils$findSearchSlot() {
      if (this.lifestealutils$searchItemSlotIndex >= 0 && this.lifestealutils$searchItemSlotIndex < this.menu.slots.size()) {
         ItemStack stack = this.menu.slots.get(this.lifestealutils$searchItemSlotIndex).getItem();
         if (!stack.isEmpty() && SEARCH_ITEM_NAME.equals(stack.getHoverName().getString())) {
            return this.lifestealutils$searchItemSlotIndex;
         }
      }
      return lifestealutils$findNamedControlSlot(SEARCH_ITEM_NAME);
   }

   @Unique
   public boolean lifestealutils$consumeSuppressNextSearchDialogFlag() {
      boolean value = this.lifestealutils$suppressNextSearchDialog;
      this.lifestealutils$suppressNextSearchDialog = false;
      return value;
   }

   @Unique
   private boolean lifestealutils$handleCardClick(int[] metrics, int mode, double mouseX, double mouseY) {
      if (mode != MODE_ITEMS) {
         return false;
      }
      int contentY = metrics[IDX_CONTENT_Y];
      int contentHeight = metrics[IDX_CONTENT_HEIGHT];
      int mainX = metrics[IDX_MAIN_X];
      int mainWidth = metrics[IDX_MAIN_WIDTH];

      int panelLeft = mainX;
      int panelTop = contentY + BASELINE_TOP_PADDING;
      int panelRight = mainX + mainWidth;
      int panelBottom = contentY + contentHeight;

      int cardsLeft = panelLeft + INNER_PADDING;
      int cardsTop = panelTop + INNER_PADDING;
      int cardsRight = panelRight - INNER_PADDING;
      int cardsBottom = panelBottom - INNER_PADDING - FOOTER_GAP - FOOTER_HEIGHT;

      int cardsWidth = cardsRight - cardsLeft;
      int columns = lifestealutils$getCardColumns(cardsWidth);
      int totalGapWidth = (columns - 1) * CARD_GAP;
      int availableForCards = Math.max(1, cardsWidth - (CARD_ROW_SIDE_MARGIN * 2) - totalGapWidth);
      int cardWidth = Math.max(MIN_CARD_WIDTH, availableForCards / columns);
      int rowStartX = cardsLeft + CARD_ROW_SIDE_MARGIN;
      int renderedScrollOffset = lifestealutils$getRenderedCardScrollOffset();
      CardViewportLayout cardLayout = lifestealutils$computeCardViewportLayout(metrics);

      for (int visualIndex = 0; visualIndex < this.lifestealutils$visibleAuctionSlots.size(); visualIndex++) {
         int slotIndex = this.lifestealutils$visibleAuctionSlots.get(visualIndex);
         int col = visualIndex % columns;
         int row = visualIndex / columns;
         int cardX = rowStartX + col * (cardWidth + CARD_GAP);
         int cardY = cardsTop + row * (CARD_HEIGHT + CARD_GAP) - renderedScrollOffset;
         if (cardY + CARD_HEIGHT <= cardLayout.viewportTop()) {
            continue;
         }
         if (cardY >= cardLayout.viewportBottom()) {
            break;
         }
         if (!lifestealutils$isMouseInsideVisibleCardSlice(mouseX, mouseY, cardX, cardY, cardWidth, CARD_HEIGHT, cardLayout.viewportTop(), cardLayout.viewportBottom())) {
            continue;
         }

         Minecraft client = Minecraft.getInstance();
         if (client.player == null || client.gameMode == null) {
            return true;
         }
         client.gameMode.handleInventoryMouseClick(this.menu.containerId, slotIndex, 0, ClickType.PICKUP, client.player);
         return true;
      }

      return false;
   }

   @Unique
   private boolean lifestealutils$isInsideMainContent(int[] metrics, int mode, double mouseX, double mouseY) {
      if (mode != MODE_ITEMS) {
         return false;
      }
      CardViewportLayout cardLayout = lifestealutils$computeCardViewportLayout(metrics);
      return lifestealutils$isInside(mouseX, mouseY, cardLayout.viewportLeft(), cardLayout.viewportTop(), cardLayout.viewportRight() - cardLayout.viewportLeft(), cardLayout.viewportBottom() - cardLayout.viewportTop());
   }

   @Unique
   private int lifestealutils$getCardColumns(int availableCardsWidth) {
      int widthForThree = (MIN_CARD_WIDTH * MAX_CARD_COLUMNS) + (CARD_GAP * (MAX_CARD_COLUMNS - 1)) + (CARD_ROW_SIDE_MARGIN * 2);
      if (availableCardsWidth >= widthForThree) {
         return MAX_CARD_COLUMNS;
      }
      int widthForTwo = (MIN_CARD_WIDTH * MID_CARD_COLUMNS) + (CARD_GAP * (MID_CARD_COLUMNS - 1)) + (CARD_ROW_SIDE_MARGIN * 2);
      if (availableCardsWidth >= widthForTwo) {
         return MID_CARD_COLUMNS;
      }
      return 1;
   }

   @Unique
   private int lifestealutils$getMaxCardScroll(int[] metrics) {
      return lifestealutils$computeCardViewportLayout(metrics).maxScroll();
   }

   @Unique
   private void lifestealutils$updateAnimatedCardScroll() {
      if (!this.lifestealutils$cardScrollAnimationInitialized) {
         this.lifestealutils$animatedCardScrollOffset = this.lifestealutils$cardScrollOffset;
         this.lifestealutils$cardScrollAnimationInitialized = true;
         return;
      }
      float delta = this.lifestealutils$cardScrollOffset - this.lifestealutils$animatedCardScrollOffset;
      if (Math.abs(delta) < 0.35F) {
         this.lifestealutils$animatedCardScrollOffset = this.lifestealutils$cardScrollOffset;
         return;
      }
      this.lifestealutils$animatedCardScrollOffset += delta * 0.35F;
   }

   @Unique
   private int lifestealutils$getRenderedCardScrollOffset() {
      return Math.round(this.lifestealutils$animatedCardScrollOffset);
   }

   @Unique
   private void lifestealutils$clampCardScroll(int maxScroll) {
      this.lifestealutils$cardScrollOffset = Math.max(0, Math.min(maxScroll, this.lifestealutils$cardScrollOffset));
      if (!this.lifestealutils$cardScrollAnimationInitialized) {
         return;
      }
      this.lifestealutils$animatedCardScrollOffset = Math.max(0.0F, Math.min((float) maxScroll, this.lifestealutils$animatedCardScrollOffset));
   }

   @Unique
   private boolean lifestealutils$pushCardViewportClip(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
      if (right <= left || bottom <= top) {
         return false;
      }
      guiGraphics.enableScissor(left, top, right, bottom);
      return true;
   }

   @Unique
   private void lifestealutils$popCardViewportClip(GuiGraphics guiGraphics, boolean clipApplied) {
      if (!clipApplied) {
         return;
      }
      guiGraphics.disableScissor();
   }

   @Unique
   private boolean lifestealutils$isMouseInsideVisibleCardSlice(double mouseX, double mouseY, int cardX, int cardY, int cardWidth, int cardHeight, int viewportTop, int viewportBottom) {
      int visibleTop = Math.max(cardY, viewportTop);
      int visibleBottom = Math.min(cardY + cardHeight, viewportBottom);
      if (visibleBottom <= visibleTop) {
         return false;
      }
      return lifestealutils$isInside(mouseX, mouseY, cardX, visibleTop, cardWidth, visibleBottom - visibleTop);
   }

   @Unique
   private CardViewportLayout lifestealutils$computeCardViewportLayout(int[] metrics) {
      int contentY = metrics[IDX_CONTENT_Y];
      int contentHeight = metrics[IDX_CONTENT_HEIGHT];
      int mainX = metrics[IDX_MAIN_X];
      int mainWidth = metrics[IDX_MAIN_WIDTH];
      int panelTop = contentY + BASELINE_TOP_PADDING;
      int panelBottom = contentY + contentHeight;
      int viewportLeft = mainX + INNER_PADDING;
      int viewportTop = panelTop + INNER_PADDING;
      int viewportRight = mainX + mainWidth - INNER_PADDING;
      int viewportBottom = panelBottom - INNER_PADDING - FOOTER_GAP - FOOTER_HEIGHT;
      int viewportWidth = Math.max(0, viewportRight - viewportLeft);
      int viewportHeight = Math.max(0, viewportBottom - viewportTop);
      int columns = lifestealutils$getCardColumns(viewportWidth);
      int totalCards = this.lifestealutils$visibleAuctionSlots.size();
      int rows = (int) Math.ceil(totalCards / (double) columns);
      int contentPixelHeight = Math.max(0, rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP);
      int maxScroll = Math.max(0, contentPixelHeight - viewportHeight);
      return new CardViewportLayout(viewportLeft, viewportTop, viewportRight, viewportBottom, maxScroll);
   }

   @Unique
   private record CardViewportLayout(int viewportLeft, int viewportTop, int viewportRight, int viewportBottom,
                                     int maxScroll) {
   }

   @Unique
   private List<Integer> lifestealutils$getRenderableAuctionSlots(int topContainerSlotCount) {
      ArrayList<Integer> slots = new ArrayList<>();
      int rows = topContainerSlotCount / 9;
      if (rows <= 2) {
         return slots;
      }

      for (int row = 1; row < rows - 1; row++) {
         for (int col = 1; col <= 7; col++) {
            int slotIndex = row * 9 + col;
            if (slotIndex < topContainerSlotCount) {
               slots.add(slotIndex);
            }
         }
      }
      return slots;
   }

   @Unique
   private boolean lifestealutils$clickPageButton(boolean forward) {
      long now = System.currentTimeMillis();
      if (now - this.lifestealutils$lastPageKeyClickMs < 100L) {
         return false;
      }

      int stateId = lifestealutils$getMenuStateId();
      if (forward && this.lifestealutils$lastNextClickStateId == stateId) {
         return false;
      }
      if (!forward && this.lifestealutils$lastPrevClickStateId == stateId) {
         return false;
      }

      int slotIndex = forward
              ? lifestealutils$findPageSlot(NEXT_PAGE_NAME)
              : lifestealutils$findPageSlot(PREV_PAGE_NAME, ALT_PREV_PAGE_NAME);
      if (slotIndex < 0) {
         return false;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.gameMode == null) {
         return false;
      }

      client.gameMode.handleInventoryMouseClick(this.menu.containerId, slotIndex, 0, ClickType.PICKUP, client.player);
      this.lifestealutils$lastPageKeyClickMs = now;
      if (forward) {
         this.lifestealutils$lastNextClickStateId = stateId;
      } else {
         this.lifestealutils$lastPrevClickStateId = stateId;
      }
      return true;
   }

   @Unique
   private int lifestealutils$getMenuStateId() {
      return this.menu.getStateId();
   }

   @Unique
   private boolean lifestealutils$clickSortItem() {
      if (this.lifestealutils$sortItemSlotIndex < 0 || this.lifestealutils$sortItemSlotIndex >= this.menu.slots.size()) {
         return true;
      }
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.gameMode == null) {
         return true;
      }
      client.gameMode.handleInventoryMouseClick(this.menu.containerId, this.lifestealutils$sortItemSlotIndex, 0, ClickType.PICKUP, client.player);
      return true;
   }

   @Unique
   private int lifestealutils$findPageSlot(String... targetNames) {
      for (int i = 0; i < this.menu.slots.size(); i++) {
         ItemStack stack = this.menu.slots.get(i).getItem();
         if (stack.isEmpty()) {
            continue;
         }
         if (!stack.is(Items.REDSTONE_TORCH)) {
            continue;
         }
         String hoverName = stack.getHoverName().getString();
         for (String targetName : targetNames) {
            if (targetName.equals(hoverName)) {
               return i;
            }
         }
      }
      return -1;
   }

   @Unique
   private void lifestealutils$refreshSortState(int mode) {
      this.lifestealutils$sortItemSlotIndex = -1;
      int topContainerSlotCount = Math.max(0, this.menu.slots.size() - 36);
      int[] controlRows = lifestealutils$getCandidateControlRows(topContainerSlotCount);
      boolean foundSortItem = false;
      for (int row : controlRows) {
         int rowStart = row * 9;
         for (int col = 0; col < 9; col++) {
            int i = rowStart + col;
            if (i >= topContainerSlotCount) {
               break;
            }

            ItemStack stack = this.menu.slots.get(i).getItem();
            if (stack.isEmpty()) {
               continue;
            }
            if (!SORT_ITEM_NAME.equals(stack.getHoverName().getString())) {
               continue;
            }

            foundSortItem = true;

            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null || lore.lines().isEmpty()) {
               this.lifestealutils$sortItemSlotIndex = i;
               break;
            }

            ArrayList<String> parsedOptions = new ArrayList<>();
            int selectedIndex = -1;
            for (Component lineComponent : lore.lines()) {
               String trimmed = lineComponent.getString().trim();
               if (trimmed.isEmpty()) {
                  continue;
               }

               String lowered = trimmed.toLowerCase(Locale.ROOT);
               if (lowered.contains("click to toggle") || lowered.contains("click to cycle")) {
                  continue;
               }

               boolean selected = lifestealutils$hasSelectedPrefix(trimmed);
               boolean option = selected || lifestealutils$hasOptionPrefix(trimmed);
               if (!option) {
                  continue;
               }

               String optionText = lifestealutils$stripPrefix(trimmed);
               if (optionText.isBlank()) {
                  continue;
               }

               parsedOptions.add(optionText);
               if (selected) {
                  selectedIndex = parsedOptions.size() - 1;
               }
            }

            // keep previous visual state unless we got a valid parsed set.
            if (!parsedOptions.isEmpty()) {
               int resolvedSelectedIndex = selectedIndex;
               if (resolvedSelectedIndex < 0) {
                  if (this.lifestealutils$selectedSortIndex >= 0 && this.lifestealutils$selectedSortIndex < parsedOptions.size()) {
                     resolvedSelectedIndex = this.lifestealutils$selectedSortIndex;
                  } else if (lifestealutils$cachedSelectedSortIndex >= 0 && lifestealutils$cachedSelectedSortIndex < parsedOptions.size()) {
                     resolvedSelectedIndex = lifestealutils$cachedSelectedSortIndex;
                  }
               }
               this.lifestealutils$sortOptionLabels.clear();
               this.lifestealutils$sortOptionLabels.addAll(parsedOptions);
               this.lifestealutils$selectedSortIndex = resolvedSelectedIndex;
               lifestealutils$cachedSortOptionLabels.clear();
               lifestealutils$cachedSortOptionLabels.addAll(parsedOptions);
               lifestealutils$cachedSelectedSortIndex = resolvedSelectedIndex;
            }
            this.lifestealutils$sortItemSlotIndex = i;
            break;
         }
         if (this.lifestealutils$sortItemSlotIndex >= 0) {
            break;
         }
      }

      if (!foundSortItem && mode != MODE_ITEMS) {
         if (this.lifestealutils$sortOptionLabels.isEmpty() && !lifestealutils$cachedSortOptionLabels.isEmpty()) {
            this.lifestealutils$sortOptionLabels.addAll(lifestealutils$cachedSortOptionLabels);
            this.lifestealutils$selectedSortIndex = lifestealutils$cachedSelectedSortIndex;
         }
      }
   }

   @Unique
   private void lifestealutils$refreshSearchState(int mode) {
      this.lifestealutils$searchItemSlotIndex = -1;
      if (mode != MODE_ITEMS) {
         return;
      }
      String parsedQuery = null;
      boolean clearable = false;

      int topContainerSlotCount = Math.max(0, this.menu.slots.size() - 36);
      int[] controlRows = lifestealutils$getCandidateControlRows(topContainerSlotCount);
      for (int row : controlRows) {
         int rowStart = row * 9;
         for (int col = 0; col < 9; col++) {
            int i = rowStart + col;
            if (i >= topContainerSlotCount) {
               break;
            }

            ItemStack stack = this.menu.slots.get(i).getItem();
            if (stack.isEmpty()) {
               continue;
            }
            if (!SEARCH_ITEM_NAME.equals(stack.getHoverName().getString())) {
               continue;
            }

            this.lifestealutils$searchItemSlotIndex = i;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
               for (Component lineComponent : lore.lines()) {
                  String trimmed = lineComponent.getString().trim();
                  if (trimmed.isEmpty()) {
                     continue;
                  }
                  String lowered = trimmed.toLowerCase(Locale.ROOT);
                  if (lowered.startsWith("searching for:")) {
                     parsedQuery = trimmed.substring("searching for:".length()).trim();
                  }
                  if (lowered.contains("click to clear search")) {
                     clearable = true;
                  }
               }
            }
            break;
         }
         if (this.lifestealutils$searchItemSlotIndex >= 0) {
            break;
         }
      }

      if (parsedQuery != null && parsedQuery.equalsIgnoreCase("nothing")) {
         parsedQuery = "";
      }
      if (!clearable) {
         parsedQuery = "";
      }

      if (parsedQuery == null || parsedQuery.isBlank()) {
         this.lifestealutils$visibleSearchQuery = null;
      } else {
         this.lifestealutils$visibleSearchQuery = parsedQuery;
      }
      this.lifestealutils$visibleSearchClearable = clearable && this.lifestealutils$visibleSearchQuery != null;
      AhSearchAutomation.setActiveQuery(this.lifestealutils$visibleSearchQuery);

      if (this.lifestealutils$searchBox != null) {
         String uiValue = this.lifestealutils$visibleSearchQuery == null ? "" : this.lifestealutils$visibleSearchQuery;
         if (!this.lifestealutils$searchBox.isFocused() && !uiValue.equals(this.lifestealutils$searchBox.getValue())) {
            this.lifestealutils$searchBox.setValue(uiValue);
         }
      }
   }

   @Unique
   private void lifestealutils$refreshFilterState(int mode) {
      this.lifestealutils$filterItemSlotIndex = lifestealutils$findNamedControlSlot(FILTER_ITEM_NAME);
      this.lifestealutils$filterConfirmSlotIndex = lifestealutils$findNamedTopSlot(CONFIRM_BUTTON_NAME);
      this.lifestealutils$filterGoBackSlotIndex = lifestealutils$findNamedTopSlot(GO_BACK_BUTTON_NAME);
      if (mode != MODE_FILTER_EDIT) {
         if (this.lifestealutils$filterOptionLabels.isEmpty() && !lifestealutils$cachedFilterOptionLabels.isEmpty()) {
            this.lifestealutils$filterOptionLabels.addAll(lifestealutils$cachedFilterOptionLabels);
            this.lifestealutils$filterOptionSelectedStates.addAll(lifestealutils$cachedFilterOptionSelectedStates);
            for (int i = 0; i < this.lifestealutils$filterOptionLabels.size(); i++) {
               this.lifestealutils$filterOptionSlotIndices.add(-1);
            }
         }
         this.lifestealutils$hasAnyFilterSelected = false;
         for (boolean selected : this.lifestealutils$filterOptionSelectedStates) {
            if (selected) {
               this.lifestealutils$hasAnyFilterSelected = true;
               break;
            }
         }
         return;
      }

      int topContainerSlotCount = Math.max(0, this.menu.slots.size() - 36);
      Map<String, Integer> parsedSlots = new LinkedHashMap<>();
      Map<String, Boolean> parsedSelected = new LinkedHashMap<>();
      for (int slotIndex = 0; slotIndex < topContainerSlotCount; slotIndex++) {
         ItemStack stack = this.menu.slots.get(slotIndex).getItem();
         if (stack.isEmpty()) {
            continue;
         }

         ItemLore lore = stack.get(DataComponents.LORE);
         if (lore == null || lore.lines().isEmpty()) {
            continue;
         }

         boolean selectable = false;
         boolean selected = false;
         for (Component lineComponent : lore.lines()) {
            String lowered = lineComponent.getString().trim().toLowerCase(Locale.ROOT);
            if (lowered.contains("click to select this option")) {
               selectable = true;
            }
            if (lowered.contains("click to un-select this option")) {
               selectable = true;
               selected = true;
            }
         }
         if (!selectable) {
            continue;
         }

         String label = stack.getHoverName().getString();
         if (label == null || label.isBlank()) {
            continue;
         }

         parsedSlots.put(label, slotIndex);
         parsedSelected.put(label, selected);
      }

      if (this.lifestealutils$filterOptionLabels.isEmpty()) {
         for (Map.Entry<String, Integer> entry : parsedSlots.entrySet()) {
            this.lifestealutils$filterOptionLabels.add(entry.getKey());
            this.lifestealutils$filterOptionSlotIndices.add(entry.getValue());
            this.lifestealutils$filterOptionSelectedStates.add(Boolean.TRUE.equals(parsedSelected.get(entry.getKey())));
         }
      } else {
         for (Map.Entry<String, Integer> entry : parsedSlots.entrySet()) {
            String label = entry.getKey();
            int existingIndex = this.lifestealutils$filterOptionLabels.indexOf(label);
            boolean selected = Boolean.TRUE.equals(parsedSelected.get(label));
            if (existingIndex >= 0) {
               this.lifestealutils$filterOptionSlotIndices.set(existingIndex, entry.getValue());
               this.lifestealutils$filterOptionSelectedStates.set(existingIndex, selected);
            } else {
               this.lifestealutils$filterOptionLabels.add(label);
               this.lifestealutils$filterOptionSlotIndices.add(entry.getValue());
               this.lifestealutils$filterOptionSelectedStates.add(selected);
            }
         }
      }

      this.lifestealutils$hasAnyFilterSelected = false;
      for (boolean selected : this.lifestealutils$filterOptionSelectedStates) {
         if (selected) {
            this.lifestealutils$hasAnyFilterSelected = true;
            break;
         }
      }

      lifestealutils$cachedFilterOptionLabels.clear();
      lifestealutils$cachedFilterOptionLabels.addAll(this.lifestealutils$filterOptionLabels);
      lifestealutils$cachedFilterOptionSelectedStates.clear();
      lifestealutils$cachedFilterOptionSelectedStates.addAll(this.lifestealutils$filterOptionSelectedStates);
   }

   @Unique
   private boolean lifestealutils$clickFilterEdit() {
      return lifestealutils$clickSlot(this.lifestealutils$filterItemSlotIndex);
   }

   @Unique
   private boolean lifestealutils$clickFilterOption(int optionIndex) {
      if (optionIndex < 0 || optionIndex >= this.lifestealutils$filterOptionSlotIndices.size()) {
         return true;
      }
      return lifestealutils$clickSlot(this.lifestealutils$filterOptionSlotIndices.get(optionIndex));
   }

   @Unique
   private boolean lifestealutils$saveFilterChanges() {
      if (!this.lifestealutils$hasAnyFilterSelected) {
         return lifestealutils$cancelFilterChanges();
      }
      if (this.lifestealutils$filterConfirmSlotIndex >= 0) {
         return lifestealutils$clickSlot(this.lifestealutils$filterConfirmSlotIndex);
      }
      return lifestealutils$cancelFilterChanges();
   }

   @Unique
   private boolean lifestealutils$cancelFilterChanges() {
      return lifestealutils$clickSlot(this.lifestealutils$filterGoBackSlotIndex);
   }

   @Unique
   private void lifestealutils$refreshSidebarActionState() {
      this.lifestealutils$claimItemsSlotIndex = lifestealutils$findNamedControlSlot(CLAIM_ITEMS_NAME);
      this.lifestealutils$yourListingsSlotIndex = lifestealutils$findNamedControlSlot(YOUR_LISTINGS_NAME);
   }

   @Unique
   private boolean lifestealutils$clickClaimItemsButton() {
      return lifestealutils$clickSlot(this.lifestealutils$claimItemsSlotIndex);
   }

   @Unique
   private boolean lifestealutils$clickYourListingsButton() {
      return lifestealutils$clickSlot(this.lifestealutils$yourListingsSlotIndex);
   }

   @Unique
   private boolean lifestealutils$clickSlot(int slotIndex) {
      if (slotIndex < 0 || slotIndex >= this.menu.slots.size()) {
         return true;
      }
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.gameMode == null) {
         return true;
      }
      client.gameMode.handleInventoryMouseClick(this.menu.containerId, slotIndex, 0, ClickType.PICKUP, client.player);
      return true;
   }

   @Unique
   private boolean lifestealutils$isSearchActiveInUi() {
      return this.lifestealutils$visibleSearchClearable
              && this.lifestealutils$visibleSearchQuery != null
              && !this.lifestealutils$visibleSearchQuery.isBlank();
   }

   @Unique
   private int lifestealutils$findNamedControlSlot(String targetName) {
      int topContainerSlotCount = Math.max(0, this.menu.slots.size() - 36);
      int[] controlRows = lifestealutils$getCandidateControlRows(topContainerSlotCount);
      for (int row : controlRows) {
         int rowStart = row * 9;
         for (int col = 0; col < 9; col++) {
            int slotIndex = rowStart + col;
            if (slotIndex >= topContainerSlotCount) {
               break;
            }
            ItemStack stack = this.menu.slots.get(slotIndex).getItem();
            if (stack.isEmpty()) {
               continue;
            }
            if (targetName.equals(stack.getHoverName().getString())) {
               return slotIndex;
            }
         }
      }
      return -1;
   }

   @Unique
   private int lifestealutils$findNamedTopSlot(String targetName) {
      int topContainerSlotCount = Math.max(0, this.menu.slots.size() - 36);
      for (int slotIndex = 0; slotIndex < topContainerSlotCount; slotIndex++) {
         ItemStack stack = this.menu.slots.get(slotIndex).getItem();
         if (stack.isEmpty()) {
            continue;
         }
         if (targetName.equals(stack.getHoverName().getString())) {
            return slotIndex;
         }
      }
      return -1;
   }

   @Unique
   private int[] lifestealutils$getCandidateControlRows(int topContainerSlotCount) {
      int rows = topContainerSlotCount / 9;
      if (rows <= 0) {
         return new int[0];
      }
      if (rows == 1) {
         return new int[]{0};
      }
      return new int[]{0, rows - 1};
   }

   @Unique
   private boolean lifestealutils$hasSelectedPrefix(String value) {
      return value.startsWith("→") || value.startsWith("➡") || value.startsWith("➜") || value.startsWith(">");
   }

   @Unique
   private boolean lifestealutils$hasOptionPrefix(String value) {
      return value.startsWith("∙") || value.startsWith("•") || value.startsWith("●") || value.startsWith("·") || value.startsWith("-");
   }

   @Unique
   private String lifestealutils$stripPrefix(String value) {
      if (value.isEmpty()) {
         return value;
      }
      return value.substring(1).trim();
   }

   @Unique
   private boolean lifestealutils$isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   @Unique
   private int[] lifestealutils$computeLayout(int screenWidth, int screenHeight) {
      int outerMarginX = Math.max(MIN_OUTER_MARGIN_X, Math.min(MAX_OUTER_MARGIN_X, screenWidth / 24));
      int outerMarginY = Math.max(MIN_OUTER_MARGIN_Y, Math.min(MAX_OUTER_MARGIN_Y, screenHeight / 24));
      int contentX = outerMarginX;
      int contentY = outerMarginY;
      int contentWidth = Math.max(220, screenWidth - outerMarginX * 2);
      int contentHeight = Math.max(120, screenHeight - outerMarginY * 2);

      int sidebarWidth = Math.max(160, (int) Math.round((contentWidth - PANEL_GAP) / 5.5D));
      int mainWidth = Math.max(1, contentWidth - sidebarWidth - PANEL_GAP);
      int sidebarX = contentX;
      int mainX = sidebarX + sidebarWidth + PANEL_GAP;

      return new int[]{contentX, contentY, contentHeight, sidebarX, sidebarWidth, mainX, mainWidth};
   }
}
