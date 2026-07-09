package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.ui.lsu.SlotSnapshot;
import dev.candycup.lifestealutils.features.shop.ShopOverlayRenderer;
import dev.candycup.lifestealutils.features.shop.ShopParseResult;
import dev.candycup.lifestealutils.features.shop.ShopParser;
import dev.candycup.lifestealutils.features.shop.ShopUiFeature;
import dev.candycup.ui.lsu.SlotGraceCache;
import dev.candycup.lifestealutils.features.shop.ShopView;
import dev.candycup.ui.ActionRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
//? if >1.21.8 {
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
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class ShopContainerOverlayMixin<T extends AbstractContainerMenu> {
   @Unique
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/shop-ui");

   @Unique
   private static ShopView.Listings lifestealutils$cachedListingView;

   @Shadow
   @Final
   protected T menu;

   @Shadow
   protected Slot hoveredSlot;

   @Shadow
   protected abstract void renderTooltip(GuiGraphics guiGraphics, int i, int j);

   @Unique
   private final ShopOverlayRenderer lifestealutils$shopRenderer = new ShopOverlayRenderer();

   @Unique
   private final SlotGraceCache lifestealutils$slotGraceCache = new SlotGraceCache();

   @Unique
   private ShopView lifestealutils$currentShopView;

   @Unique
   private ShopView.Listings lifestealutils$lastListingView;

   @Unique
   private String lifestealutils$expectedCategoryTitle;

   @Unique
   private String lifestealutils$lastFallbackKey;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$renderShopOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      ShopView view = lifestealutils$getShopView();
      if (view == null) {
         return;
      }

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      ((ScreenAccessor) self).invokeRenderBlurredBackground(guiGraphics);
      lifestealutils$shopRenderer.render(view, lifestealutils$playerSlots(), guiGraphics, self.width, self.height, mouseX, mouseY);
      Slot tooltipSlot = lifestealutils$shopRenderer.hoveredTooltipSlot();
      if (tooltipSlot != null) {
         this.hoveredSlot = tooltipSlot;
         this.renderTooltip(guiGraphics, mouseX, mouseY);
      }
      ci.cancel();
   }

   //? if >1.21.8 {
   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleShopClick(MouseButtonEvent mouseButtonEvent, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      if (mouseButtonEvent.button() != 0) {
         return;
      }
      if (lifestealutils$handleShopClick(mouseButtonEvent.x(), mouseButtonEvent.y())) {
         cir.setReturnValue(true);
      }
   }
   //?} else {
   /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleShopClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button != 0) {
         return;
      }
      if (lifestealutils$handleShopClick(mouseX, mouseY)) {
         cir.setReturnValue(true);
      }
   }*/
   //?}

   @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleShopScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
      ShopView view = lifestealutils$getShopView();
      if (view == null) {
         return;
      }
      if (lifestealutils$shopRenderer.scroll(verticalAmount)) {
         cir.setReturnValue(true);
      }
   }

   @Unique
   private boolean lifestealutils$handleShopClick(double mouseX, double mouseY) {
      ShopView view = lifestealutils$getShopView();
      if (view == null) {
         return false;
      }

      ActionRegion<ShopOverlayRenderer.ActionKind> action = lifestealutils$shopRenderer.actionAt(mouseX, mouseY);
      if (action == null || action.slotIndex() < 0) {
         return lifestealutils$shopRenderer.handleUiAction(action);
      }
      if (lifestealutils$shopRenderer.handleUiAction(action)) {
         return true;
      }

      if (view instanceof ShopView.Categories && action.item() instanceof SlotSnapshot item) {
         lifestealutils$expectedCategoryTitle = "Shop | " + item.plainName();
      }
      if (view instanceof ShopView.Listings listings && action.item() != null) {
         lifestealutils$cachedListingView = listings;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.gameMode == null) {
         return true;
      }

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      lifestealutils$rememberClickedSlot(self.getTitle().getString(), action.slotIndex());
      client.gameMode.handleInventoryMouseClick(this.menu.containerId, action.slotIndex(), 0, ClickType.PICKUP, client.player);
      return true;
   }

   @Unique
   private ShopView lifestealutils$getShopView() {
      if (!ShopUiFeature.isCustomShopInterfaceEnabled() || !LifestealAPI.isOnLifestealNetwork()) {
         lifestealutils$currentShopView = null;
         return null;
      }

      AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
      String title = self.getTitle().getString();
      ShopView.Listings listingContext = lifestealutils$lastListingView != null ? lifestealutils$lastListingView : lifestealutils$cachedListingView;
      ShopParseResult result = ShopParser.parse(title, lifestealutils$shopItemSnapshots(title), listingContext, lifestealutils$expectedCategoryTitle);
      if (result == null) {
         lifestealutils$currentShopView = null;
         lifestealutils$expectedCategoryTitle = null;
         return null;
      }

      if (result instanceof ShopParseResult.Fallback fallback) {
         lifestealutils$currentShopView = null;
         lifestealutils$logFallback(title, fallback);
         return null;
      }

      ShopView view = ((ShopParseResult.Valid) result).view();
      lifestealutils$currentShopView = view;
      lifestealutils$lastFallbackKey = null;
      if (view instanceof ShopView.Listings listings) {
         lifestealutils$lastListingView = listings;
         lifestealutils$cachedListingView = listings;
         lifestealutils$expectedCategoryTitle = null;
      } else if (view instanceof ShopView.Categories) {
         lifestealutils$lastListingView = null;
         lifestealutils$cachedListingView = null;
         lifestealutils$expectedCategoryTitle = null;
      }
      return view;
   }

   @Unique
   private void lifestealutils$logFallback(String title, ShopParseResult.Fallback fallback) {
      String key = this.menu.containerId + ":" + title + ":" + fallback.code().code() + ":" + fallback.detail();
      if (key.equals(lifestealutils$lastFallbackKey)) {
         return;
      }
      lifestealutils$lastFallbackKey = key;
      LOGGER.warn("Shop overlay fallback {} for title '{}': {}", fallback.code().code(), title, fallback.detail());
   }

   @Unique
   private void lifestealutils$rememberClickedSlot(String title, int slotIndex) {
      if (slotIndex < 0 || slotIndex >= this.menu.slots.size()) {
         return;
      }
      SlotSnapshot snapshot = lifestealutils$snapshot(slotIndex, this.menu.slots.get(slotIndex).getItem());
      lifestealutils$slotGraceCache.capture(this.menu.containerId, title, snapshot);
   }

   @Unique
   private List<SlotSnapshot> lifestealutils$shopItemSnapshots(String title) {
      List<SlotSnapshot> snapshots = new ArrayList<>();
      Inventory playerInventory = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory();
      for (int i = 0; i < this.menu.slots.size(); i++) {
         Slot slot = this.menu.slots.get(i);
         if (playerInventory != null && slot.container == playerInventory) {
            continue;
         }
         SlotSnapshot liveSnapshot = lifestealutils$snapshot(i, slot.getItem());
         snapshots.add(lifestealutils$slotGraceCache.resolve(this.menu.containerId, title, liveSnapshot));
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
}
