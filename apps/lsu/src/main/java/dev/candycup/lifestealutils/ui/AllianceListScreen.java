package dev.candycup.lifestealutils.ui;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.features.alliances.AllianceModels;
import dev.candycup.lifestealutils.features.alliances.AllianceService;
import dev.candycup.lifestealutils.features.alliances.AllianceSyncManager;
import dev.candycup.lifestealutils.gaia.GaiaApiClient;
import dev.candycup.lifestealutils.gaia.GaiaConsentRequiredException;
import dev.candycup.lifestealutils.gaia.modules.alliances.AlliancesModule;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

public class AllianceListScreen extends Screen {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/alliance-list-screen");
   private static final Component TITLE = Component.translatable("lsu.alliances.list.title");
   private static final Component TAB_TITLE = Component.translatable("lsu.alliances.your_alliances");
   private static final int LIST_WIDTH = 320;

   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final TabManager tabManager;
   private TabNavigationBar tabNavigationBar;
   private final Screen lastScreen;
   private AllianceListWidget listWidget;
   private EditBox subscribeIdEdit;
   private Button subscribeButton;

   public AllianceListScreen(Screen lastScreen) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.tabManager = new TabManager(
              guiEventListener -> addRenderableWidget(guiEventListener),
              this::removeWidget
      );
   }

   @Override
   protected void init() {
      AllianceService.reloadFromDisk();
      listWidget = new AllianceListWidget(this.minecraft);
      this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
              .addTabs(new AlliancesTab(TAB_TITLE, listWidget))
              .build();
      addRenderableWidget(this.tabNavigationBar);

      Button createButton = Button.builder(Component.literal("Create"), button -> openCreateCommand())
              .width(100)
              .build();
      Button doneButton = Button.builder(CommonComponents.GUI_DONE, button -> onClose())
              .width(100)
              .build();
      LinearLayout footerRow = LinearLayout.horizontal().spacing(8);
      footerRow.addChild(createButton);
      footerRow.addChild(doneButton);
      this.layout.addToFooter((LayoutElement) footerRow);
      this.layout.visitWidgets(abstractWidget -> {
         abstractWidget.setTabOrderGroup(1);
         addRenderableWidget(abstractWidget);
      });

      this.subscribeIdEdit = new EditBox(this.font, 0, 0, 120, 20, Component.literal("Alliance ID"));
      this.subscribeIdEdit.setMaxLength(64);
      this.subscribeIdEdit.setHint(Component.translatable("lsu.alliances.list.subscribe_hint"));
      addRenderableWidget(this.subscribeIdEdit);
      this.subscribeButton = Button.builder(Component.translatable("lsu.alliances.list.subscribe"), button -> subscribeById())
              .width(90)
              .build();
      addRenderableWidget(this.subscribeButton);

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
      this.tabNavigationBar.getTabs().forEach(tab -> tab.visitChildren(widget -> {
      }));
      this.tabManager.setTabArea(tabArea);
      this.layout.setHeaderHeight(tabBottom);
      this.layout.arrangeElements();

      if (subscribeIdEdit != null && subscribeButton != null) {
         int y = this.height - this.layout.getFooterHeight() - 38;
         int spacing = 6;
         int totalWidth = subscribeIdEdit.getWidth() + spacing + subscribeButton.getWidth();
         int startX = (this.width - totalWidth) / 2;
         subscribeIdEdit.setX(startX);
         subscribeIdEdit.setY(y);
         subscribeButton.setX(startX + subscribeIdEdit.getWidth() + spacing);
         subscribeButton.setY(y);
      }
      updateActionButtons();
   }

   private void updateActionButtons() {
      if (subscribeButton != null && subscribeIdEdit != null) {
         if (!Config.isGaiaAdvancedFeaturesEnabled()) {
            subscribeButton.active = false;
            subscribeButton.setTooltip(Tooltip.create(Component.translatable("lsu.alliances.list.subscribe_requires_gaia")));
         } else {
            String value = subscribeIdEdit.getValue() == null ? "" : subscribeIdEdit.getValue().trim();
            subscribeButton.active = !value.isBlank();
            subscribeButton.setTooltip(null);
         }
      }
   }

   private void openCreateCommand() {
      //? if >1.21.8 {
      this.minecraft.setScreen(new ChatScreen("/lsu alliances create ", false));
      //?} else {
      /*this.minecraft.setScreen(new ChatScreen("/lsu alliances create "));
       *///?}
   }

   private void subscribeById() {
      if (subscribeIdEdit == null) {
         return;
      }
      String id = subscribeIdEdit.getValue() == null ? "" : subscribeIdEdit.getValue().trim();
      if (id.isBlank()) {
         MessagingUtils.showMiniMessage("<red>Enter an invite code first.</red>");
         return;
      }
      AlliancesModule.SubscriptionResult subscriptionResult;
      try {
         subscriptionResult = GaiaApiClient.getInstance().alliances().subscribeWithDetails(id);
      } catch (GaiaConsentRequiredException ignored) {
         MessagingUtils.showMiniMessage("<red>Gaia is disabled. Run /lsu consent-gaia to enable.</red>");
         return;
      } catch (Exception e) {
         LOGGER.error("Failed to subscribe to alliance '{}'", id, e);
         MessagingUtils.showMiniMessage("<red>Failed to contact alliance service.</red>");
         return;
      }
      if (!subscriptionResult.success()) {
         String message = subscriptionResult.errorMessage();
         if (message == null || message.isBlank()) {
            message = "Subscribe failed. Please try again.";
         }
         MessagingUtils.showMiniMessage("<red>" + message + "</red>");
         return;
      }
      AllianceSyncManager.syncSubscriptionsNow();
      AllianceService.reloadFromDisk();
      listWidget.refreshEntries();
      subscribeIdEdit.setValue("");
      updateActionButtons();
      MessagingUtils.showMiniMessage("<green>Subscribed to alliance.</green>");
   }

   private void moveAlliance(String clientId, int direction) {
      if (clientId == null || clientId.isBlank() || direction == 0) {
         return;
      }
      List<AllianceModels.AllianceRecord> ordered = AllianceService.listAll();
      int index = -1;
      for (int i = 0; i < ordered.size(); i++) {
         AllianceModels.AllianceRecord current = ordered.get(i);
         if (clientId.equals(current.clientId)) {
            index = i;
            break;
         }
      }
      if (index < 0) {
         return;
      }
      int newIndex = index + direction;
      if (newIndex < 0 || newIndex >= ordered.size()) {
         return;
      }
      AllianceModels.AllianceRecord current = ordered.get(index);
      ordered.set(index, ordered.get(newIndex));
      ordered.set(newIndex, current);
      AllianceService.reorder(ordered.stream().map(alliance -> alliance.clientId).collect(Collectors.toList()));
      AllianceService.reloadFromDisk();
      if (listWidget != null) {
         listWidget.refreshEntries();
      }
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      updateActionButtons();
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
      this.minecraft.setScreen(this.lastScreen);
   }

   private class AlliancesTab extends GridLayoutTab {
      private final AbstractSelectionList<?> list;

      public AlliancesTab(Component title, AbstractSelectionList<?> list) {
         super(title);
         this.layout.addChild((LayoutElement) list, 1, 1);
         this.list = list;
      }

      @Override
      public void doLayout(ScreenRectangle screenRectangle) {
         this.list.updateSizeAndPosition(AllianceListScreen.this.width, AllianceListScreen.this.layout.getContentHeight(), AllianceListScreen.this.layout.getHeaderHeight());
         super.doLayout(screenRectangle);
      }
   }

   private class AllianceListWidget extends ObjectSelectionList<AllianceListWidget.AllianceEntry> {
      private static final int ENTRY_HEIGHT = 36;

      public AllianceListWidget(Minecraft minecraft) {
         super(minecraft, AllianceListScreen.this.width, AllianceListScreen.this.layout.getContentHeight(), 33, ENTRY_HEIGHT);
         refreshEntries();
      }

      public void refreshEntries() {
         clearEntries();
         List<AllianceModels.AllianceRecord> alliances = AllianceService.listAll();
         for (AllianceModels.AllianceRecord alliance : alliances) {
            addEntry(new AllianceEntry(alliance));
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

      private class AllianceEntry extends ObjectSelectionList.Entry<AllianceEntry> {
         private static final int MOVE_BUTTON_WIDTH = 16;
         private static final int MOVE_BUTTON_HEIGHT = 11;
         private final AllianceModels.AllianceRecord alliance;

         public AllianceEntry(AllianceModels.AllianceRecord alliance) {
            this.alliance = alliance;
         }

         @Override
         public Component getNarration() {
            return Component.literal(alliance.data.name);
         }

         //? if >1.21.8 {
         @Override
         public void renderContent(GuiGraphics guiGraphics, int index, int entryWidth, boolean isSelected, float partialTick) {
            int x = getContentX() + 6;
            int y = getContentY() + 4;
            Component name = Component.literal(alliance.data.name == null || alliance.data.name.isBlank() ? "Unnamed Alliance" : alliance.data.name);
            Component members = Component.translatable("lsu.alliances.members", AllianceService.totalMembers(alliance));
            guiGraphics.drawString(AllianceListScreen.this.font, name, x, y, 0xFFFFFFFF);
            guiGraphics.drawString(AllianceListScreen.this.font, members, x, y + 12, 0xFFAAAAAA);
            int upX = getContentRight() - 40;
            int downX = getContentRight() - 20;
            int buttonY = y + 4;
            guiGraphics.fill(upX, buttonY, upX + MOVE_BUTTON_WIDTH, buttonY + MOVE_BUTTON_HEIGHT, 0x66333333);
            guiGraphics.fill(downX, buttonY, downX + MOVE_BUTTON_WIDTH, buttonY + MOVE_BUTTON_HEIGHT, 0x66333333);
            guiGraphics.drawCenteredString(AllianceListScreen.this.font, "▲", upX + MOVE_BUTTON_WIDTH / 2, buttonY + 2, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(AllianceListScreen.this.font, "▼", downX + MOVE_BUTTON_WIDTH / 2, buttonY + 2, 0xFFFFFFFF);
            if (!alliance.canEdit) {
               Component readonly = Component.literal("Subscribed");
               int width = AllianceListScreen.this.font.width(readonly);
               guiGraphics.drawString(AllianceListScreen.this.font, readonly, getContentRight() - width - 44, y, 0xFF88CCFF);
            }
         }
         //?} else {
         /*@Override
         public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            Component name = Component.literal(alliance.data.name == null || alliance.data.name.isBlank() ? "Unnamed Alliance" : alliance.data.name);
            Component members = Component.translatable("lsu.alliances.members", AllianceService.totalMembers(alliance));
            guiGraphics.drawString(AllianceListScreen.this.font, name, x + 8, y + 4, 0xFFFFFF);
            guiGraphics.drawString(AllianceListScreen.this.font, members, x + 8, y + 16, 0xAAAAAA);
         }
         *///?}

         //? if >1.21.8 {
         @Override
         public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent, boolean bl) {
            if (mouseButtonEvent.button() != 0) {
               return false;
            }

            int upX = getContentRight() - 40;
            int downX = getContentRight() - 20;
            int buttonY = getContentY() + 8;
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            boolean inUp = mouseX >= upX && mouseX < upX + MOVE_BUTTON_WIDTH && mouseY >= buttonY && mouseY < buttonY + MOVE_BUTTON_HEIGHT;
            boolean inDown = mouseX >= downX && mouseX < downX + MOVE_BUTTON_WIDTH && mouseY >= buttonY && mouseY < buttonY + MOVE_BUTTON_HEIGHT;

            AllianceListWidget.this.setSelected(this);
            if (inUp) {
               moveAlliance(alliance.clientId, -1);
               return true;
            }
            if (inDown) {
               moveAlliance(alliance.clientId, 1);
               return true;
            }
            AllianceListScreen.this.minecraft.setScreen(new AllianceDetailScreen(AllianceListScreen.this, alliance.clientId));
            return true;
         }
         //?} else {
         /*@Override
         public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
               return false;
            }
            AllianceListWidget.this.setSelected(this);
            AllianceListScreen.this.minecraft.setScreen(new AllianceDetailScreen(AllianceListScreen.this, alliance.clientId));
            return true;
         }
         *///?}

      }
   }
}
