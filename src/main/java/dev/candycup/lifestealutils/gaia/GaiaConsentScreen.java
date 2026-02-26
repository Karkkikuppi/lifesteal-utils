package dev.candycup.lifestealutils.gaia;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
//? if >1.21.8 {
import net.minecraft.client.input.KeyEvent;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the Gaia consent notice and captures the user's preference.
 */
public class GaiaConsentScreen extends Screen {
   private static final Component TITLE = Component.translatable("lsu.gaia.consent.title");
   private static final Component TAB_TITLE = Component.translatable("lsu.gaia.consent.title");
   private static final Component ENABLE_BUTTON = Component.translatable("lsu.gaia.consent.enable");
   private static final Component DISABLE_BUTTON = Component.translatable("lsu.gaia.consent.disable");
   private static final Component LOADING_TEXT = Component.translatable("lsu.gaia.consent.loading");
   private static final int LIST_WIDTH = 280;
   private static final int TEXT_PADDING = 6;
   private static final int BUTTON_WIDTH = 150;
   private static final int BUTTON_SPACING = 8;
   private static final int ROW_HEIGHT = 12;

   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final TabManager tabManager;
   private TabNavigationBar tabNavigationBar;
   private final Screen lastScreen;

   private ConsentTextList consentList;
   private boolean decisionMade = false;
   private int lastContentVersion = -1;

   /**
    * Creates a new Gaia consent screen.
    *
    * @param lastScreen the screen to return to when closing
    */
   public GaiaConsentScreen(Screen lastScreen) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.tabManager = new TabManager(
              guiEventListener -> addRenderableWidget(guiEventListener),
              this::removeWidget
      );
   }

   @Override
   protected void init() {
      consentList = new ConsentTextList(this.minecraft, this.font);
      this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
              .addTabs(new ConsentTab(TAB_TITLE, consentList))
              .build();
      addRenderableWidget(this.tabNavigationBar);

      LinearLayout buttonRow = LinearLayout.horizontal().spacing(BUTTON_SPACING);
      buttonRow.addChild(Button.builder(ENABLE_BUTTON, button -> handleConsent(true))
              .width(BUTTON_WIDTH)
              .build());
      buttonRow.addChild(Button.builder(DISABLE_BUTTON, button -> handleConsent(false))
              .width(BUTTON_WIDTH)
              .build());
      this.layout.addToFooter((LayoutElement) buttonRow);

      this.layout.visitWidgets(abstractWidget -> {
         abstractWidget.setTabOrderGroup(1);
         addRenderableWidget(abstractWidget);
      });

      this.tabNavigationBar.selectTab(0, false);
      refreshConsentContent();
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
   public void tick() {
      refreshConsentContent();
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight(), 0.0F, 0.0F, this.width, 2, 32, 2);
   }

   @Override
   protected void renderMenuBackground(GuiGraphics guiGraphics) {
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
      renderMenuBackground(guiGraphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
   }

   @Override
   public void onClose() {
      if (!decisionMade) {
         GaiaConsentController.recordConsentDecision(false);
      }
      this.minecraft.setScreen(this.lastScreen);
   }

   private void handleConsent(boolean enabled) {
      decisionMade = true;
      GaiaConsentController.recordConsentDecision(enabled);
      this.minecraft.setScreen(this.lastScreen);
   }

   private void refreshConsentContent() {
      if (consentList == null) {
         return;
      }
      int version = GaiaConsentController.getConsentContentVersion();
      if (version == lastContentVersion) {
         return;
      }
      lastContentVersion = version;
      consentList.refreshContent();
   }

   /**
    * Tab implementation for the consent text list.
    */
   private class ConsentTab extends GridLayoutTab {
      private final AbstractSelectionList<?> list;

      public ConsentTab(Component title, AbstractSelectionList<?> list) {
         super(title);
         this.layout.addChild((LayoutElement) list, 1, 1);
         this.list = list;
      }

      @Override
      public void doLayout(ScreenRectangle screenRectangle) {
         this.list.updateSizeAndPosition(GaiaConsentScreen.this.width, GaiaConsentScreen.this.layout.getContentHeight(), GaiaConsentScreen.this.layout.getHeaderHeight());
         super.doLayout(screenRectangle);
      }
   }

   /**
    * Displays the consent notice content as a scrollable list of lines.
    */
   private class ConsentTextList extends ObjectSelectionList<ConsentTextList.LineEntry> {
      private final Font font;

      public ConsentTextList(Minecraft minecraft, Font font) {
         super(minecraft, GaiaConsentScreen.this.width, GaiaConsentScreen.this.layout.getContentHeight(), 33, ROW_HEIGHT);
         this.font = font;
      }

      public void refreshContent() {
         clearEntries();
         List<Component> contentLines = getConsentContentLines();
         for (Component lineComponent : contentLines) {
            List<FormattedCharSequence> wrappedLines = font.split(lineComponent, getTextWidth());
            if (wrappedLines.isEmpty()) {
               addEntry(new LineEntry(FormattedCharSequence.EMPTY));
               continue;
            }
            for (FormattedCharSequence line : wrappedLines) {
               addEntry(new LineEntry(line));
            }
         }
      }

      @Override
      public int getRowWidth() {
         return LIST_WIDTH;
      }

      @Override
      protected void renderListBackground(GuiGraphics guiGraphics) {
      }

      @Override
      protected void renderListSeparators(GuiGraphics guiGraphics) {
      }

      private int getTextWidth() {
         return LIST_WIDTH - (TEXT_PADDING * 2);
      }

      private List<Component> getConsentContentLines() {
         String miniMessage = GaiaConsentController.getConsentMiniMessage();
         if (miniMessage == null || miniMessage.isBlank()) {
            return List.of(LOADING_TEXT);
         }
         String[] rawLines = miniMessage.split("\n", -1);
         ArrayList<Component> lines = new ArrayList<>(rawLines.length);
         for (String rawLine : rawLines) {
            if (rawLine.isEmpty()) {
               lines.add(Component.empty());
               continue;
            }
            lines.add(MessagingUtils.miniMessage(rawLine));
         }
         return lines;
      }

      /**
       * A single line of consent notice text.
       */
      private class LineEntry extends ObjectSelectionList.Entry<LineEntry> {
         private final FormattedCharSequence line;

         public LineEntry(FormattedCharSequence line) {
            this.line = line;
         }

         @Override
         public Component getNarration() {
            return Component.empty();
         }

         //? if >1.21.8 {
         @Override
         public void renderContent(GuiGraphics guiGraphics, int index, int entryWidth, boolean isSelected, float partialTick) {
            int contentX = getContentX() + TEXT_PADDING;
            int contentY = getContentY();
            renderLine(guiGraphics, contentX, contentY);
         }

         private void renderLine(GuiGraphics guiGraphics, int x, int y) {
            guiGraphics.drawString(GaiaConsentScreen.this.font, line, x, y, 0xFFFFFFFF);
         }
         //?} else {
         /*@Override
         public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int contentX = x + TEXT_PADDING;
            int contentY = y;
            guiGraphics.drawString(GaiaConsentScreen.this.font, line, contentX, contentY, 0xFFFFFF);
         }
         *///?}
      }
   }
}
