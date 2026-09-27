package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.features.ContainerOverlayBackgroundState;
import dev.candycup.lifestealutils.features.ah.AhOverlayRenderer;
import dev.candycup.lifestealutils.features.ah.AhOverlaySearchInput;
import dev.candycup.lifestealutils.features.ah.AhOverlaySearchState;
import dev.candycup.lifestealutils.features.ah.AhParseResult;
import dev.candycup.lifestealutils.features.ah.AhParser;
import dev.candycup.lifestealutils.features.ah.AhSearchAutomation;
import dev.candycup.lifestealutils.features.ah.AhStabilityCode;
import dev.candycup.lifestealutils.features.ah.AhView;
import dev.candycup.lifestealutils.features.baltop.BaltopScrapeCoordinator;
import dev.candycup.ui.ActionRegion;
import dev.candycup.ui.lsu.SlotSnapshot;
import dev.candycup.ui.lsu.SlotGraceCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
//? if >1.21.8 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class AuctionContainerOverlayMixin<T extends AbstractContainerMenu> implements AhOverlaySearchState, AhOverlaySearchInput, ContainerOverlayBackgroundState {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/ah-overlay");

    @Unique
    private static boolean lifestealutils$allowFilterOverlayFromAuctionItems;

    @Unique
    private static AhView.AhState lifestealutils$sharedLastState;

    @Unique
    private static AhView.Items lifestealutils$sharedLastItemsView;

    @Unique
    private static boolean lifestealutils$pendingFilterPopoverAnchor;

    @Unique
    private static double lifestealutils$pendingFilterPopoverX;

    @Unique
    private static double lifestealutils$pendingFilterPopoverY;

    @Shadow
    @Final
    protected T menu;

    @Shadow
    protected Slot hoveredSlot;

    @Unique
    private final AhOverlayRenderer lifestealutils$ahRenderer = new AhOverlayRenderer();

    @Unique
    private final SlotGraceCache lifestealutils$slotGraceCache = new SlotGraceCache();

    @Unique
    private final Set<String> lifestealutils$loggedMetaWarnings = new HashSet<>();

    @Unique
    private AhView.AhState lifestealutils$lastState;

    @Unique
    private AhView.Items lifestealutils$lastItemsView;

    @Unique
    private String lifestealutils$lastFallbackKey;

    @Unique
    private boolean lifestealutils$searchFocused;

    @Unique
    private String lifestealutils$searchText = "";

    @Unique
    private boolean lifestealutils$suppressNextSearchDialog;

    @Unique
    private long lifestealutils$lastPageKeyClickMs;

    @Unique
    private int lifestealutils$lastNextClickStateId = Integer.MIN_VALUE;

    @Unique
    private int lifestealutils$lastPrevClickStateId = Integer.MIN_VALUE;

    //? if <26.1 {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void lifestealutils$renderAuctionBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!lifestealutils$shouldReplaceAuctionBackground()) {
            return;
        }

        ((ScreenAccessor) this).invokeRenderBlurredBackground(guiGraphics);
        ci.cancel();
    }
    //?}

    @Unique
    public boolean lifestealutils$shouldReplaceAuctionBackground() {
        return lifestealutils$getAhView() != null;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void lifestealutils$renderAuctionOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AhView view = lifestealutils$getAhView();
        if (view == null) {
            return;
        }

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (view instanceof AhView.FilterEdit && lifestealutils$pendingFilterPopoverAnchor) {
            lifestealutils$ahRenderer.openFilterPopoverAt(lifestealutils$pendingFilterPopoverX, lifestealutils$pendingFilterPopoverY);
            lifestealutils$pendingFilterPopoverAnchor = false;
        }
        AhView.Items previousItems = lifestealutils$lastItemsView == null ? lifestealutils$sharedLastItemsView : lifestealutils$lastItemsView;
        lifestealutils$ahRenderer.render(view, previousItems, lifestealutils$playerSlots(), guiGraphics, self.width, self.height, mouseX, mouseY, lifestealutils$searchText, lifestealutils$searchFocused);
        // The concrete container screen performs its tooltip pass after this method returns.
        // Always clear the field when nothing is hovered so an old tooltip cannot follow the cursor.
        this.hoveredSlot = lifestealutils$ahRenderer.hoveredTooltipSlot();
        ci.cancel();
    }

    //? if >1.21.8 {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void lifestealutils$handleAuctionClick(MouseButtonEvent mouseButtonEvent, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (lifestealutils$handleAuctionClick(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void lifestealutils$handleAuctionKey(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (lifestealutils$handleAuctionKey(keyEvent.key())) {
            cir.setReturnValue(true);
        }
    }

    //?} else {
   /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleAuctionClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (lifestealutils$handleAuctionClick(mouseX, mouseY, button)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleAuctionKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
      if (lifestealutils$handleAuctionKey(keyCode)) {
         cir.setReturnValue(true);
      }
   }

   *///?}

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void lifestealutils$handleAuctionScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        AhView view = lifestealutils$getAhView();
        if (view == null) {
            return;
        }
        if (lifestealutils$ahRenderer.scroll(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean lifestealutils$handleAuctionClick(double mouseX, double mouseY, int button) {
        AhView view = lifestealutils$getAhView();
        if (view == null) {
            return false;
        }
        if (button != 0) {
            return true;
        }

        ActionRegion<AhOverlayRenderer.ActionKind> action = lifestealutils$ahRenderer.actionAt(mouseX, mouseY);
        if (action == null) {
            lifestealutils$searchFocused = false;
            return true;
        }
        if (action.kind() == AhOverlayRenderer.ActionKind.FOCUS_TEXT) {
            lifestealutils$searchFocused = true;
            return true;
        }
        lifestealutils$searchFocused = false;
        if (action.kind() == AhOverlayRenderer.ActionKind.SEARCH_SUBMIT) {
            return lifestealutils$submitSearch(action.slotIndex());
        }
        if (action.kind() == AhOverlayRenderer.ActionKind.SEARCH_RESET) {
            return lifestealutils$resetSearch(action.slotIndex());
        }
        if (action.kind() == AhOverlayRenderer.ActionKind.OPEN_FILTERS) {
            lifestealutils$ahRenderer.openFilterPopoverAt(mouseX, mouseY);
            lifestealutils$pendingFilterPopoverAnchor = true;
            lifestealutils$pendingFilterPopoverX = mouseX;
            lifestealutils$pendingFilterPopoverY = mouseY;
            return lifestealutils$clickSlot(action.slotIndex());
        }
        if (action.kind() == AhOverlayRenderer.ActionKind.NAVIGATE && action.item() instanceof String command) {
            return lifestealutils$navigateTo(command);
        }
        if (action.kind() == AhOverlayRenderer.ActionKind.SLOT_CLICK) {
            return lifestealutils$clickSlot(action.slotIndex());
        }
        return true;
    }

    @Unique
    private boolean lifestealutils$navigateTo(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return true;
        }

        client.player.closeContainer();
        if ("baltop".equals(command)) {
            BaltopScrapeCoordinator.handleBaltopCommand(client);
            return true;
        }
        client.player.connection.sendCommand(command);
        return true;
    }

    @Unique
    private boolean lifestealutils$handleAuctionKey(int keyCode) {
        AhView view = lifestealutils$getAhView();
        if (view == null) {
            return false;
        }
        if (lifestealutils$searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                return lifestealutils$submitSearch(view.state().searchState().searchSlot());
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                lifestealutils$searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !lifestealutils$searchText.isEmpty()) {
                lifestealutils$searchText = lifestealutils$searchText.substring(0, lifestealutils$searchText.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                lifestealutils$searchText = "";
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            return lifestealutils$clickPageButton(view, true);
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            return lifestealutils$clickPageButton(view, false);
        }
        return false;
    }

    @Unique
    public boolean lifestealutils$handleAhOverlayCharacter(int codepoint) {
        if (!lifestealutils$searchFocused || lifestealutils$getAhView() == null) {
            return false;
        }
        if (Character.isISOControl(codepoint)) {
            return true;
        }
        lifestealutils$searchText += new String(Character.toChars(codepoint));
        if (lifestealutils$searchText.length() > 64) {
            lifestealutils$searchText = lifestealutils$searchText.substring(0, 64);
        }
        return true;
    }

    @Unique
    private AhView lifestealutils$getAhView() {
        if (!AhSearchAutomation.isCustomAhInterfaceEnabled() || !LifestealAPI.isOnLifestealNetwork()) {
            lifestealutils$allowFilterOverlayFromAuctionItems = false;
            return null;
        }
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        String title = self.getTitle().getString();
        if (AhParser.AUCTION_ITEMS_TITLE.equals(title)) {
            lifestealutils$allowFilterOverlayFromAuctionItems = true;
        }
        AhView.AhState previousState = lifestealutils$lastState == null ? lifestealutils$sharedLastState : lifestealutils$lastState;
        AhParseResult result = AhParser.parse(title, lifestealutils$itemSnapshots(title), previousState, lifestealutils$allowFilterOverlayFromAuctionItems);
        if (result == null) {
            return null;
        }
        if (result instanceof AhParseResult.Fallback fallback) {
            lifestealutils$logFallback(title, fallback);
            return null;
        }

        AhView view = ((AhParseResult.Valid) result).view();
        if (view instanceof AhView.Items items) {
            lifestealutils$lastItemsView = items;
            lifestealutils$sharedLastItemsView = items;
        }
        lifestealutils$lastState = view.state();
        lifestealutils$sharedLastState = view.state();
        lifestealutils$lastFallbackKey = null;
        lifestealutils$logWarnings(view);
        if (!lifestealutils$searchFocused) {
            lifestealutils$searchText = view.state().searchState().activeQuery() == null ? "" : view.state().searchState().activeQuery();
        }
        AhSearchAutomation.setActiveQuery(view.state().searchState().activeQuery());
        return view;
    }

    @Unique
    private boolean lifestealutils$submitSearch(int searchSlot) {
        if (lifestealutils$searchText == null || lifestealutils$searchText.isBlank()) {
            return true;
        }
        if (searchSlot < 0) {
            return true;
        }
        AhSearchAutomation.queueSearch(lifestealutils$searchText);
        lifestealutils$suppressNextSearchDialog = true;
        return lifestealutils$clickSlot(searchSlot);
    }

    @Unique
    private boolean lifestealutils$resetSearch(int searchSlot) {
        if (searchSlot >= 0) {
            lifestealutils$clickSlot(searchSlot);
        }
        AhSearchAutomation.setActiveQuery(null);
        AhSearchAutomation.queueSearch(null);
        lifestealutils$searchText = "";
        lifestealutils$searchFocused = false;
        return true;
    }

    @Unique
    private boolean lifestealutils$clickPageButton(AhView view, boolean forward) {
        if (!(view instanceof AhView.Items)) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lifestealutils$lastPageKeyClickMs < 100L) {
            return true;
        }
        int stateId = this.menu.getStateId();
        if (forward && lifestealutils$lastNextClickStateId == stateId) {
            return true;
        }
        if (!forward && lifestealutils$lastPrevClickStateId == stateId) {
            return true;
        }
        int slot = forward ? view.state().controls().nextPageSlot() : view.state().controls().previousPageSlot();
        if (slot < 0) {
            return true;
        }
        lifestealutils$lastPageKeyClickMs = now;
        if (forward) {
            lifestealutils$lastNextClickStateId = stateId;
        } else {
            lifestealutils$lastPrevClickStateId = stateId;
        }
        return lifestealutils$clickSlot(slot);
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
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        lifestealutils$slotGraceCache.capture(this.menu.containerId, self.getTitle().getString(), lifestealutils$snapshot(slotIndex, this.menu.slots.get(slotIndex).getItem()));
        client.gameMode.handleInventoryMouseClick(this.menu.containerId, slotIndex, 0, ClickType.PICKUP, client.player);
        return true;
    }

    @Unique
    public boolean lifestealutils$consumeSuppressNextSearchDialogFlag() {
        boolean value = lifestealutils$suppressNextSearchDialog;
        lifestealutils$suppressNextSearchDialog = false;
        return value;
    }

    @Unique
    private List<SlotSnapshot> lifestealutils$itemSnapshots(String title) {
        List<SlotSnapshot> snapshots = new ArrayList<>();
        Inventory playerInventory = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (playerInventory != null && slot.container == playerInventory) {
                continue;
            }
            SlotSnapshot live = lifestealutils$snapshot(i, slot.getItem());
            snapshots.add(lifestealutils$slotGraceCache.resolve(this.menu.containerId, title, live));
        }
        return snapshots;
    }

    @Unique
    private List<Slot> lifestealutils$playerSlots() {
        List<Slot> slots = new ArrayList<>();
        Inventory playerInventory = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory();
        if (playerInventory == null) {
            return slots;
        }
        for (Slot slot : this.menu.slots) {
            Container container = slot.container;
            if (container == playerInventory) {
                slots.add(slot);
            }
        }
        return slots;
    }

    @Unique
    private SlotSnapshot lifestealutils$snapshot(int menuSlotIndex, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new SlotSnapshot(menuSlotIndex, ItemStack.EMPTY, net.minecraft.network.chat.Component.empty(), "", List.of(), true, false, false, false);
        }
        ItemStack stableStack = stack.copy();
        ItemLore lore = stableStack.get(DataComponents.LORE);
        return new SlotSnapshot(
                menuSlotIndex,
                stableStack,
                stableStack.getHoverName(),
                stableStack.getHoverName().getString(),
                lore == null ? List.of() : lore.lines(),
                false,
                stableStack.getItem() == Items.GRAY_STAINED_GLASS_PANE,
                stableStack.getItem() == Items.BARRIER,
                stableStack.getItem() == Items.REDSTONE_TORCH
        );
    }

    @Unique
    private void lifestealutils$logFallback(String title, AhParseResult.Fallback fallback) {
        String key = this.menu.containerId + ":" + title + ":" + fallback.code().code() + ":" + fallback.detail();
        if (key.equals(lifestealutils$lastFallbackKey)) {
            return;
        }
        lifestealutils$lastFallbackKey = key;
        LOGGER.warn("Auction overlay fallback {} for title '{}': {}", fallback.code().code(), title, fallback.detail());
    }

    @Unique
    private void lifestealutils$logWarnings(AhView view) {
        for (String warning : view.warnings()) {
            if (lifestealutils$loggedMetaWarnings.add(warning)) {
                LOGGER.warn("Auction overlay warning {}: {}", AhStabilityCode.AH_LISTING_META_MISSING.code(), warning);
            }
        }
    }
}
