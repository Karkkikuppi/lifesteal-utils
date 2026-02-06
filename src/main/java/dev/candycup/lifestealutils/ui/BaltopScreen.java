package dev.candycup.lifestealutils.ui;

import dev.candycup.lifestealutils.features.baltop.BaltopScraper;
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
import java.util.function.Supplier;

/**
 * A screen displaying the balance leaderboard (baltop) in a Statistics-like format.
 * Shows player heads, usernames, balances, and positions.
 * Supports live updating as entries are scraped from the server.
 */
public class BaltopScreen extends Screen {
   private static final Component TITLE = Component.translatable("lifestealutils.baltop.title");
   private static final Component BALTOP_TAB = Component.translatable("lifestealutils.baltop.tab");
   private static final Component LOADING_TEXT = Component.translatable("lifestealutils.baltop.loading");
   private static final int LIST_WIDTH = 280;
   private static final int PADDING = 8;

   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final TabManager tabManager;
   private TabNavigationBar tabNavigationBar;
   private final Screen lastScreen;
   private final BaltopScraper scraper;
   private BaltopList baltopList;
   private boolean loadingComplete = false;

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
      if (baltopList != null && minecraft != null) {
         baltopList.refreshFromScraper();
      }
   }

   /**
    * Called by the scraper when all pages have been scraped.
    */
   public void onLoadingComplete() {
      loadingComplete = true;
      refreshEntries();
   }

   /**
    * Called by the scraper when scraping fails.
    */
   public void onLoadingFailed(String reason) {
      loadingComplete = true;
      // the error message is already shown via MessagingUtils, just mark as done
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
      private static final int ENTRY_HEIGHT = 36;
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
            int totalTextHeight = lineHeight * 2 + 2; // two lines with 2px spacing
            int textStartY = contentY + (contentHeight - totalTextHeight) / 2;

            guiGraphics.drawString(BaltopScreen.this.font, this.usernameComponent, textX, textStartY, textColor);
            guiGraphics.drawString(BaltopScreen.this.font, this.balanceComponent, textX, textStartY + lineHeight + 2, textColor);

            // render position on the right side
            int positionWidth = BaltopScreen.this.font.width(this.positionComponent);
            int positionX = contentRight - positionWidth - PADDING;
            int positionY = contentY + (contentHeight - lineHeight) / 2;
            guiGraphics.drawString(BaltopScreen.this.font, this.positionComponent, positionX, positionY, textColor);
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
