package dev.candycup.lifestealutils.ui;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.LifestealServerDetector;
import dev.candycup.lifestealutils.features.baltop.BaltopScraper;
import dev.candycup.lifestealutils.gaia.collectivum.CollectivumBaltopClient;
import dev.candycup.lifestealutils.gaia.curiositas.CuriositasBaltopSnapshotClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
//? if >1.21.8 {
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.client.input.KeyEvent;
//?} else {
/*import net.minecraft.client.resources.PlayerSkin;
 *///?}

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaltopScreen extends Screen {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/baltop-screen");
   private static final Component TITLE = Component.translatable("lifestealutils.baltop.title");
   private static final Component BALTOP_TAB = Component.translatable("lifestealutils.baltop.tab");
   private static final Component LOADING_TEXT = Component.translatable("lifestealutils.baltop.loading");
   private static final int LIST_WIDTH = 280;
   private static final int PADDING = 8;
   private static final String BALANCE_PREFIX = "$";
   private static final String BALANCE_SEPARATOR = ",";
   private static final CuriositasBaltopSnapshotClient.SnapshotRange DEFAULT_SNAPSHOT_RANGE = CuriositasBaltopSnapshotClient.SnapshotRange.RANGE_24_HOURS;
   private static final String DELTA_UP_TRANSLATION_KEY = "lifestealutils.baltop.delta.up";
   private static final String DELTA_DOWN_TRANSLATION_KEY = "lifestealutils.baltop.delta.down";
   private static final String DELTA_UNCHANGED_TRANSLATION_KEY = "lifestealutils.baltop.delta.unchanged";
   private static final int DELTA_UP_COLOR = 0xFF4CAF50;
   private static final int DELTA_DOWN_COLOR = 0xFFF44336;
   private static final int DELTA_UNCHANGED_COLOR = 0xFFB0BEC5;

   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final TabManager tabManager;
   private TabNavigationBar tabNavigationBar;
   private final Screen lastScreen;
   private final BaltopScraper scraper;
   private BaltopList baltopList;
   private boolean loadingComplete = false;
   private boolean submissionSent = false;
   private final Map<String, Long> snapshotPastAmountsByUsername = new ConcurrentHashMap<>();
   private volatile String snapshotRangeTranslationKey = DEFAULT_SNAPSHOT_RANGE.translationKey();
   private boolean snapshotRequestStarted = false;

   /**
    * Creates a new BaltopScreen that displays data from a scraper.
    * The screen will update dynamically as the scraper receives more entries.
    *
    * @param lastScreen the screen to return to when closing
    * @param scraper    the scraper providing baltop entries
    */
   public BaltopScreen(Screen lastScreen, BaltopScraper scraper) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.scraper = scraper;
      this.tabManager = new TabManager(
              guiEventListener -> addRenderableWidget(guiEventListener),
              this::removeWidget
      );
   }

   /**
    * Called by the scraper when new entries are available.
    * Refreshes the list to show the latest data.
    */
   public void refreshEntries() {
      if (baltopList != null) {
         baltopList.refreshFromScraper();
      }
   }

   /**
    * Called by the scraper when all pages have been scraped.
    */
   public void onLoadingComplete() {
      loadingComplete = true;
      refreshEntries();
      submitBaltopIfNeeded();
      fetchSnapshotIfNeeded();
   }

   /**
    * Called by the scraper when scraping fails.
    */
   public void onLoadingFailed(String reason) {
      loadingComplete = true;
      // the error message is already shown via MessagingUtils, just mark as done
   }

   private void submitBaltopIfNeeded() {
      if (submissionSent) {
         return;
      }

      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         return;
      }

      if (!LifestealAPI.isOnLifestealNetwork()) {
         return;
      }

      List<BaltopScraper.BaltopEntry> entries = scraper.getScrapedEntries();
      if (entries.isEmpty()) {
         return;
      }

      List<CollectivumBaltopClient.BaltopEntryPayload> payload = new ArrayList<>();
      for (BaltopScraper.BaltopEntry entry : entries) {
         long amount = parseBalanceAmount(entry.balance());
         if (amount < 0) {
            LOGGER.debug("skipping baltop entry with invalid amount: {}", entry.balance());
            continue;
         }
         payload.add(new CollectivumBaltopClient.BaltopEntryPayload(entry.username(), amount));
      }

      if (payload.isEmpty()) {
         return;
      }

      submissionSent = true;
      CollectivumBaltopClient.submitBaltopEntries(payload)
              .thenAccept(success -> {
                 if (!success) {
                    LOGGER.debug("baltop submission failed");
                 }
              });
   }

   private void fetchSnapshotIfNeeded() {
      if (snapshotRequestStarted) {
         return;
      }

      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         return;
      }

      if (!LifestealAPI.isOnLifestealNetwork()) {
         return;
      }

      snapshotRequestStarted = true;
      CuriositasBaltopSnapshotClient.fetchSnapshot(DEFAULT_SNAPSHOT_RANGE)
              .thenAccept(snapshotResponse -> {
                 Minecraft client = this.minecraft;
                 if (snapshotResponse == null) {
                    return;
                 }

                 client.execute(() -> {
                    snapshotPastAmountsByUsername.clear();
                    for (CuriositasBaltopSnapshotClient.SnapshotEntry entry : snapshotResponse.entries()) {
                       if (entry.pastAmount() != null) {
                          snapshotPastAmountsByUsername.put(normalizeUsername(entry.username()), entry.pastAmount());
                       }
                    }
                    snapshotRangeTranslationKey = snapshotResponse.range().translationKey();
                    refreshEntries();
                 });
              });
   }

   private long parseBalanceAmount(String balance) {
      if (balance == null || balance.isBlank()) {
         return -1;
      }

      String sanitized = balance.replace(BALANCE_PREFIX, "").replace(BALANCE_SEPARATOR, "").trim();
      if (sanitized.isEmpty()) {
         return -1;
      }

      for (int i = 0; i < sanitized.length(); i++) {
         if (!Character.isDigit(sanitized.charAt(i))) {
            return -1;
         }
      }

      try {
         return Long.parseLong(sanitized);
      } catch (NumberFormatException e) {
         return -1;
      }
   }

   private String normalizeUsername(String username) {
      return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
   }

   private String formatCoins(long amount) {
      return String.format(Locale.US, "%,d", amount);
   }

   @Override
   protected void init() {
      // create the tab navigation bar with a single "Baltop" tab
      baltopList = new BaltopList(this.minecraft);

      this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
              .addTabs(new BaltopTab(BALTOP_TAB, baltopList))
              .build();
      addRenderableWidget(this.tabNavigationBar);

      // add done button to footer
      this.layout.addToFooter((LayoutElement) Button.builder(CommonComponents.GUI_DONE, button -> onClose())
              .width(200)
              .build());

      this.layout.visitWidgets(abstractWidget -> {
         abstractWidget.setTabOrderGroup(1);
         addRenderableWidget(abstractWidget);
      });

      this.tabNavigationBar.selectTab(0, false);
      repositionElements();
   }

   @Override
   protected void repositionElements() {
      if (this.tabNavigationBar == null) {
         return;
      }
      this.tabNavigationBar.setWidth(this.width);
      this.tabNavigationBar.arrangeElements();

      int tabBottom = this.tabNavigationBar.getRectangle().bottom();
      ScreenRectangle tabArea = new ScreenRectangle(0, tabBottom, this.width, this.height - this.layout.getFooterHeight() - tabBottom);
      this.tabNavigationBar.getTabs().forEach(tab -> tab.visitChildren(abstractWidget -> {
      }));
      this.tabManager.setTabArea(tabArea);
      this.layout.setHeaderHeight(tabBottom);
      this.layout.arrangeElements();
   }

   //? if >1.21.8 {
   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      if (this.tabNavigationBar != null && this.tabNavigationBar.keyPressed(keyEvent)) {
         return true;
      }
      return super.keyPressed(keyEvent);
   }
   //?} else {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.tabNavigationBar != null && this.tabNavigationBar.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?}

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight(), 0.0F, 0.0F, this.width, 2, 32, 2);

      // render loading indicator if still scraping
      if (!loadingComplete && scraper.isLoading()) {
         int entryCount = scraper.getScrapedEntries().size();
         Component loadingStatus = Component.translatable("lifestealutils.baltop.loading.count", entryCount);
         int textWidth = this.font.width(loadingStatus);
         int textX = (this.width - textWidth) / 2;
         int textY = this.layout.getHeaderHeight() + 4;
         guiGraphics.drawString(this.font, loadingStatus, textX, textY, 0xFFFFAA00); // orange color
      }

      // render version text in bottom right corner
      String version = FabricLoader.getInstance()
              .getModContainer("lifestealutils")
              .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
              .orElse("unknown");
      Component versionText = Component.literal("Lifesteal Utils v" + version);
      int textWidth = this.font.width(versionText);
      int textX = this.width - textWidth - PADDING;
      int textY = this.height - this.layout.getFooterHeight() + PADDING;
      guiGraphics.drawString(this.font, versionText, textX, textY, 0xFF808080); // dark gray color
   }

   @Override
   protected void renderMenuBackground(GuiGraphics guiGraphics) {
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
      renderMenuBackground(guiGraphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   /**
    * Tab implementation for the Baltop list.
    */
   private class BaltopTab extends GridLayoutTab {
      private final AbstractSelectionList<?> list;

      public BaltopTab(Component title, AbstractSelectionList<?> list) {
         super(title);
         this.layout.addChild((LayoutElement) list, 1, 1);
         this.list = list;
      }

      @Override
      public void doLayout(ScreenRectangle screenRectangle) {
         this.list.updateSizeAndPosition(BaltopScreen.this.width, BaltopScreen.this.layout.getContentHeight(), BaltopScreen.this.layout.getHeaderHeight());
         super.doLayout(screenRectangle);
      }
   }

   /**
    * The list displaying baltop entries.
    */
   private class BaltopList extends ObjectSelectionList<BaltopList.BaltopEntry> {
      private static final int ENTRY_HEIGHT = 48;
      private int lastKnownEntryCount = 0;

      public BaltopList(Minecraft minecraft) {
         super(minecraft, BaltopScreen.this.width, BaltopScreen.this.layout.getContentHeight(), 33, ENTRY_HEIGHT);
         refreshFromScraper();
      }

      /**
       * Refreshes the list entries from the scraper's current data.
       * Only adds new entries to avoid losing scroll position.
       */
      public void refreshFromScraper() {
         List<BaltopScraper.BaltopEntry> scraperEntries = BaltopScreen.this.scraper.getScrapedEntries();

         // only add new entries (incremental update)
         for (int i = lastKnownEntryCount; i < scraperEntries.size(); i++) {
            addEntry(new BaltopEntry(scraperEntries.get(i)));
         }
         lastKnownEntryCount = scraperEntries.size();
      }

      @Override
      public int getRowWidth() {
         return LIST_WIDTH;
      }

      @Override
      protected void renderListBackground(GuiGraphics guiGraphics) {
         // no custom background, uses the default
      }

      @Override
      protected void renderListSeparators(GuiGraphics guiGraphics) {
         // no separators
      }

      /**
       * Entry representing a single player in the baltop list.
       */
      private class BaltopEntry extends ObjectSelectionList.Entry<BaltopEntry> {
         private static final int FACE_SIZE = 24;
         private static final int PADDING = 4;

         private final BaltopScraper.BaltopEntry data;
         private final Supplier<PlayerSkin> skinSupplier;
         private final Component usernameComponent;
         private final Component balanceComponent;
         private final Component positionComponent;
         private final long parsedCurrentAmount;

         public BaltopEntry(BaltopScraper.BaltopEntry data) {
            this.data = data;
            // use the item profile so skull textures render immediately
            ResolvableProfile profile = data.profile();
            if (profile == null) {
               this.skinSupplier = DefaultPlayerSkin::getDefaultSkin;
            } else {
               //? if >1.21.8 {
               BaltopScreen.this.minecraft.playerSkinRenderCache().lookup(profile);
               this.skinSupplier = () -> BaltopScreen.this.minecraft.playerSkinRenderCache().getOrDefault(profile).playerSkin();
               //?} else {
               /*profile.resolve();
               this.skinSupplier = () -> {
                  ResolvableProfile resolved = profile.pollResolve();
                  if (resolved != null) {
                     PlayerSkin resolvedSkin = BaltopScreen.this.minecraft.getSkinManager().getInsecureSkin(resolved.gameProfile(), null);
                     if (resolvedSkin != null) {
                        return resolvedSkin;
                     }
                  }
                  return DefaultPlayerSkin.get(profile.gameProfile());
               };
               *///?}
            }

            // bold the username only if it's the current player
            boolean isCurrentPlayer = BaltopScreen.this.minecraft.player != null
                    && BaltopScreen.this.minecraft.player.getName().getString().equals(data.username());
            this.usernameComponent = Component.literal(data.username())
                    .withStyle(style -> style.withColor(0xFFFFFF).withBold(isCurrentPlayer));

            this.balanceComponent = Component.literal(data.balance()).withStyle(style -> style.withColor(0xFFD700)); // gold
            this.parsedCurrentAmount = BaltopScreen.this.parseBalanceAmount(data.balance());
            this.positionComponent = Component.literal("#" + data.position()).withStyle(style -> {
               return switch (data.position()) {
                  case 1 -> style.withColor(0xFFD700).withBold(true); // gold
                  case 2 -> style.withColor(0xC0C0C0).withBold(true); // silver
                  case 3 -> style.withColor(0xCD7F32).withBold(true); // bronze
                  default -> style.withColor(0xAAAAAA);
               };
            });
         }

         //? if >1.21.8 {
         @Override
         public void renderContent(GuiGraphics guiGraphics, int index, int entryWidth, boolean isSelected, float partialTick) {
            int contentX = getContentX();
            int contentY = getContentY();
            int contentHeight = getContentHeight();
            int contentRight = getContentRight();
            renderEntryContent(guiGraphics, contentX, contentY, contentHeight, contentRight);
         }
         //?} else {
            /*@Override
            public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float partialTick) {
                int contentX = x + 2;
                int contentY = y + 2;
                int contentHeight = entryHeight - 4;
                int contentRight = x + entryWidth - 2;
                renderEntryContent(guiGraphics, contentX, contentY, contentHeight, contentRight);
            }
            *///?}

         private void renderEntryContent(GuiGraphics guiGraphics, int contentX, int contentY, int contentHeight, int contentRight) {
            // determine row color based on position (alternating) - must use ARGB with full alpha
            int rowIndex = BaltopList.this.children().indexOf(this);
            int textColor = (rowIndex % 2 == 0) ? 0xFFFFFFFF : 0xFFBBBBBB;

            // render player face on the left side (skin loads asynchronously)
            int faceX = contentX + PADDING;
            int faceY = contentY + (contentHeight - FACE_SIZE) / 2;
            PlayerSkin currentSkin = this.skinSupplier.get();
            PlayerFaceRenderer.draw(guiGraphics, currentSkin, faceX, faceY, FACE_SIZE);

            // render username and balance stacked vertically, next to the face
            int textX = faceX + FACE_SIZE + PADDING * 2;
            int lineHeight = BaltopScreen.this.font.lineHeight;
            Component deltaComponent = buildDeltaComponent();
            boolean hasDeltaLine = deltaComponent != null;
            int totalTextHeight = hasDeltaLine ? lineHeight * 3 + 4 : lineHeight * 2 + 2;
            int textStartY = contentY + (contentHeight - totalTextHeight) / 2;

            guiGraphics.drawString(BaltopScreen.this.font, this.usernameComponent, textX, textStartY, textColor);
            guiGraphics.drawString(BaltopScreen.this.font, this.balanceComponent, textX, textStartY + lineHeight + 2, textColor);
            if (hasDeltaLine) {
               int deltaColor = resolveDeltaColor();
               guiGraphics.drawString(BaltopScreen.this.font, deltaComponent, textX, textStartY + (lineHeight + 2) * 2, deltaColor);
            }

            // render position on the right side
            int positionWidth = BaltopScreen.this.font.width(this.positionComponent);
            int positionX = contentRight - positionWidth - PADDING;
            int positionY = contentY + (contentHeight - lineHeight) / 2;
            guiGraphics.drawString(BaltopScreen.this.font, this.positionComponent, positionX, positionY, textColor);
         }

         private Component buildDeltaComponent() {
            if (parsedCurrentAmount < 0) {
               return null;
            }

            Long pastAmount = snapshotPastAmountsByUsername.get(normalizeUsername(data.username()));
            if (pastAmount == null) {
               return null;
            }

            Component rangeComponent = Component.translatable(snapshotRangeTranslationKey);

            long deltaAmount = parsedCurrentAmount - pastAmount;
            if (deltaAmount > 0) {
               return Component.translatable(DELTA_UP_TRANSLATION_KEY, formatCoins(deltaAmount), rangeComponent);
            }
            if (deltaAmount < 0) {
               return Component.translatable(DELTA_DOWN_TRANSLATION_KEY, formatCoins(Math.abs(deltaAmount)), rangeComponent);
            }
            return Component.translatable(DELTA_UNCHANGED_TRANSLATION_KEY, rangeComponent);
         }

         private int resolveDeltaColor() {
            if (parsedCurrentAmount < 0) {
               return DELTA_UNCHANGED_COLOR;
            }

            Long pastAmount = snapshotPastAmountsByUsername.get(normalizeUsername(data.username()));
            if (pastAmount == null) {
               return DELTA_UNCHANGED_COLOR;
            }

            long deltaAmount = parsedCurrentAmount - pastAmount;
            if (deltaAmount > 0) {
               return DELTA_UP_COLOR;
            }
            if (deltaAmount < 0) {
               return DELTA_DOWN_COLOR;
            }
            return DELTA_UNCHANGED_COLOR;
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", Component.literal(
                    data.position() + ". " + data.username() + " - " + data.balance()
            ));
         }
      }
   }
}
