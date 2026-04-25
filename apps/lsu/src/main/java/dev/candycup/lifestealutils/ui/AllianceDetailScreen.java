package dev.candycup.lifestealutils.ui;

import dev.candycup.lifestealutils.features.alliances.AllianceIdGenerator;
import dev.candycup.lifestealutils.features.alliances.AllianceModels;
import dev.candycup.lifestealutils.features.alliances.AllianceProfileCacheManager;
import dev.candycup.lifestealutils.features.alliances.AllianceService;
import dev.candycup.lifestealutils.features.alliances.AllianceSyncManager;
import dev.candycup.lifestealutils.gaia.GaiaApiClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
//? if >1.21.8 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
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
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
//? if >1.21.8 {
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
//?} else {
/*import net.minecraft.client.resources.PlayerSkin;
 *///?}

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class AllianceDetailScreen extends Screen {
   private static final int INFO_EDITOR_RESERVED_SPACE = 52;
   private static final Component TITLE = Component.translatable("lsu.alliances.detail.title");
   private static final Component INFO_TAB = Component.translatable("lsu.alliances.detail.info_tab");
   private static final Component MEMBERS_TAB = Component.translatable("lsu.alliances.detail.members_tab");

   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final TabManager tabManager;
   private TabNavigationBar tabNavigationBar;
   private SimpleTab infoTab;
   private SimpleTab membersTab;
   private final Screen lastScreen;
   private final String allianceClientId;

   private InfoListWidget infoListWidget;
   private MembersListWidget membersListWidget;
   private MultiLineEditBox manualEditor;
   private Button manualEditorButton;
   private Button topAddPlayerButton;
   private Button topAddListButton;
   private Button topRemoveListButton;
   private Button topRemovePlayerButton;
   private Button topMoveListUpButton;
   private Button topMoveListDownButton;
   private EditBox renameListEdit;
   private Button saveListNameButton;
   private EditBox infoEdit;
   private Button saveInfoButton;
   private Button permissionMembersButton;
   private Button permissionEveryoneButton;
   private Button leaveOrDeleteButton;
   private boolean addPlayerMode = false;
   private String selectedInfoKey = "";
   private String selectedInfoTitle = "";
   private int selectedInfoMaxLength = 256;
   private String selectedListId = "";
   private String selectedPlayerUuid = "";
   private boolean membersTabSelected = false;
   private boolean manualMode = false;

   private static final java.util.Map<String, Supplier<PlayerSkin>> SKIN_SUPPLIER_BY_UUID = new java.util.concurrent.ConcurrentHashMap<>();

   public AllianceDetailScreen(Screen lastScreen, String allianceClientId) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.allianceClientId = allianceClientId;
      this.tabManager = new TabManager(this::addRenderableWidget, this::removeWidget, this::onTabSelected, tab -> {
      });
   }

   @Override
   protected void init() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null) {
         onClose();
         return;
      }

      infoListWidget = new InfoListWidget(this.minecraft);
      membersListWidget = new MembersListWidget(this.minecraft);
      if (alliance.canEdit) {
         selectedInfoKey = "name";
         selectedInfoTitle = Component.translatable("lsu.alliances.detail.name").getString();
         selectedInfoMaxLength = 64;
      } else {
         selectedInfoKey = "";
         selectedInfoTitle = "";
      }
      this.infoTab = new SimpleTab(INFO_TAB, infoListWidget);
      this.membersTab = new SimpleTab(MEMBERS_TAB, membersListWidget);
      this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
              .addTabs(this.infoTab, this.membersTab)
              .build();
      addRenderableWidget(this.tabNavigationBar);

      this.manualEditorButton = Button.builder(Component.literal("Manual Editor"), button -> toggleManualMode())
              .width(120)
              .build();
      this.topAddPlayerButton = Button.builder(Component.literal("+ Add Player"), button -> openAddPlayerCommand())
              .width(100)
              .build();
      this.topAddListButton = Button.builder(Component.literal("+ Add List"), button -> addList())
              .width(90)
              .build();
      this.topRemoveListButton = Button.builder(Component.literal("- Remove List"), button -> removeSelectedList())
              .width(100)
              .build();
      this.topRemovePlayerButton = Button.builder(Component.literal("- Remove Player"), button -> removeSelectedPlayer())
              .width(120)
              .build();
      this.topMoveListUpButton = Button.builder(Component.literal("▲ Move List Up"), button -> moveSelectedList(-1))
              .width(110)
              .build();
      this.topMoveListDownButton = Button.builder(Component.literal("▼ Move List Down"), button -> moveSelectedList(1))
              .width(110)
              .build();
      addRenderableWidget(this.topAddPlayerButton);
      addRenderableWidget(this.topAddListButton);
      addRenderableWidget(this.topRemoveListButton);
      addRenderableWidget(this.topRemovePlayerButton);
      addRenderableWidget(this.topMoveListUpButton);
      addRenderableWidget(this.topMoveListDownButton);
      Button doneButton = Button.builder(CommonComponents.GUI_DONE, button -> onClose())
              .width(100)
              .build();
      this.leaveOrDeleteButton = Button.builder(Component.translatable("lsu.alliances.detail.leave"), button -> leaveOrDeleteAlliance())
              .width(130)
              .build();

      LinearLayout footerRow = LinearLayout.horizontal().spacing(8);
      footerRow.addChild(manualEditorButton);
      footerRow.addChild(this.leaveOrDeleteButton);
      footerRow.addChild(doneButton);
      this.layout.addToFooter((LayoutElement) footerRow);
      this.layout.visitWidgets(widget -> {
         widget.setTabOrderGroup(1);
         addRenderableWidget(widget);
      });

      this.tabNavigationBar.selectTab(0, false);
      updateFooterButtons();

      this.manualEditor = MultiLineEditBox.builder().build(this.font, this.width - 80, this.height - 140, Component.empty());
      this.manualEditor.setCharacterLimit(20000);

      this.renameListEdit = new EditBox(this.font, 0, 0, 220, 20, Component.literal("List name"));
      this.renameListEdit.setMaxLength(64);
      this.renameListEdit.visible = false;
      this.renameListEdit.active = false;
      addRenderableWidget(this.renameListEdit);

      this.saveListNameButton = Button.builder(Component.literal("Save"), button -> applyRename())
              .width(70)
              .build();
      this.saveListNameButton.visible = false;
      this.saveListNameButton.active = false;
      addRenderableWidget(this.saveListNameButton);

      this.infoEdit = new EditBox(this.font, 0, 0, 260, 20, Component.literal("Value"));
      this.infoEdit.setMaxLength(1024);
      this.infoEdit.visible = false;
      this.infoEdit.active = false;
      addRenderableWidget(this.infoEdit);

      this.saveInfoButton = Button.builder(Component.literal("Apply"), button -> applyInfoEdit())
              .width(70)
              .build();
      this.saveInfoButton.visible = false;
      this.saveInfoButton.active = false;
      addRenderableWidget(this.saveInfoButton);

      this.permissionMembersButton = Button.builder(Component.literal("Members only"), button -> setSubscriptionPermission("MEMBERS"))
              .width(130)
              .build();
      this.permissionMembersButton.visible = false;
      this.permissionMembersButton.active = false;
      addRenderableWidget(this.permissionMembersButton);

      this.permissionEveryoneButton = Button.builder(Component.literal("Everyone"), button -> setSubscriptionPermission("ANYONE"))
              .width(110)
              .build();
      this.permissionEveryoneButton.visible = false;
      this.permissionEveryoneButton.active = false;
      addRenderableWidget(this.permissionEveryoneButton);

      refreshManualEditorFromMembers();
      refreshInfoEditorFromSelection();
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
      int contentBottom = this.height - this.layout.getFooterHeight() - INFO_EDITOR_RESERVED_SPACE;
      int contentHeight = Math.max(40, contentBottom - tabBottom);
      ScreenRectangle tabArea = new ScreenRectangle(0, tabBottom, this.width, contentHeight);
      this.tabManager.setTabArea(tabArea);
      this.layout.setHeaderHeight(tabBottom);
      this.layout.arrangeElements();

      if (manualEditor != null) {
         manualEditor.setX(40);
         manualEditor.setY(tabBottom + 12);
         manualEditor.setWidth(this.width - 80);
         manualEditor.setHeight(this.height - this.layout.getFooterHeight() - tabBottom - 20);
      }

      if (topAddPlayerButton != null && topAddListButton != null && topRemoveListButton != null) {
         int spacing = 6;
         int totalWidth = topAddPlayerButton.getWidth() + spacing + topAddListButton.getWidth() + spacing + topRemoveListButton.getWidth();
         int actionRowY = this.height - this.layout.getFooterHeight() - 82;
         int startX = (this.width - totalWidth) / 2;
         int y = actionRowY + 26;
         topAddPlayerButton.setX(startX);
         topAddPlayerButton.setY(y);
         topAddListButton.setX(startX + topAddPlayerButton.getWidth() + spacing);
         topAddListButton.setY(y);
         topRemoveListButton.setX(startX + topAddPlayerButton.getWidth() + spacing + topAddListButton.getWidth() + spacing);
         topRemoveListButton.setY(y);
      }

      if (topRemovePlayerButton != null && topMoveListUpButton != null && topMoveListDownButton != null) {
         int spacing = 6;
         int totalWidth = topRemovePlayerButton.getWidth() + spacing + topMoveListUpButton.getWidth() + spacing + topMoveListDownButton.getWidth();
         int actionRowY = this.height - this.layout.getFooterHeight() - 82;
         int startX = (this.width - totalWidth) / 2;
         int y = actionRowY + 50;
         topRemovePlayerButton.setX(startX);
         topRemovePlayerButton.setY(y);
         topMoveListUpButton.setX(startX + topRemovePlayerButton.getWidth() + spacing);
         topMoveListUpButton.setY(y);
         topMoveListDownButton.setX(startX + topRemovePlayerButton.getWidth() + spacing + topMoveListUpButton.getWidth() + spacing);
         topMoveListDownButton.setY(y);
      }

      if (renameListEdit != null && saveListNameButton != null) {
         int rowY = this.height - this.layout.getFooterHeight() - 82;
         int totalWidth = renameListEdit.getWidth() + 8 + saveListNameButton.getWidth();
         int startX = (this.width - totalWidth) / 2;
         renameListEdit.setX(startX);
         renameListEdit.setY(rowY);
         saveListNameButton.setX(startX + renameListEdit.getWidth() + 8);
         saveListNameButton.setY(rowY);
      }

      if (infoEdit != null && saveInfoButton != null) {
         int rowY = this.height - this.layout.getFooterHeight() - 44;
         int totalWidth = infoEdit.getWidth() + 8 + saveInfoButton.getWidth();
         int startX = (this.width - totalWidth) / 2;
         infoEdit.setX(startX);
         infoEdit.setY(rowY);
         saveInfoButton.setX(startX + infoEdit.getWidth() + 8);
         saveInfoButton.setY(rowY);
      }

      if (permissionMembersButton != null && permissionEveryoneButton != null) {
         int rowY = this.height - this.layout.getFooterHeight() - 44;
         int spacing = 8;
         int totalWidth = permissionMembersButton.getWidth() + spacing + permissionEveryoneButton.getWidth();
         int startX = (this.width - totalWidth) / 2;
         permissionMembersButton.setX(startX);
         permissionMembersButton.setY(rowY);
         permissionEveryoneButton.setX(startX + permissionMembersButton.getWidth() + spacing);
         permissionEveryoneButton.setY(rowY);
      }

      updateFooterButtons();
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight(), 0.0F, 0.0F, this.width, 2, 32, 2);
      if (manualMode) {
         guiGraphics.fill(0, 0, this.width, this.height - this.layout.getFooterHeight(), 0xFF202020);
      }
      if (!manualMode) {
         AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
         if (alliance != null && alliance.data != null) {
            String nameText = alliance.data.name == null || alliance.data.name.isBlank() ? "Unnamed Alliance" : alliance.data.name;
            int nameX = 12;
            int nameY = 8;
            guiGraphics.drawString(this.font, Component.literal(nameText), nameX, nameY, 0xFFFFFFFF);
            if (!alliance.canEdit) {
               int suffixX = nameX + this.font.width(nameText) + 4;
               guiGraphics.drawString(this.font, Component.literal("(Subscribed)"), suffixX, nameY, 0xFF88CCFF);
            }
         }
      }
      if (manualMode && manualEditor != null) {
         manualEditor.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      if (renameListEdit != null && renameListEdit.visible) {
         String selectedListName = selectedListNameWithFallback();
         if (!selectedListName.isBlank()) {
            Component renameTitle = addPlayerMode
                    ? Component.literal("Adding player to list \"" + selectedListName + "\"")
                    : Component.literal("Renaming list \"" + selectedListName + "\"");
            int titleWidth = this.font.width(renameTitle);
            int titleX = (this.width - titleWidth) / 2;
            int titleY = renameListEdit.getY() - 11;
            guiGraphics.drawString(this.font, renameTitle, titleX, titleY, 0xFFDDDDDD);
         }
         renameListEdit.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      if (infoEdit != null && infoEdit.visible) {
         if (selectedInfoTitle != null && !selectedInfoTitle.isBlank()) {
            Component infoTitle = Component.literal("Editing " + selectedInfoTitle);
            int titleWidth = this.font.width(infoTitle);
            int titleX = (this.width - titleWidth) / 2;
            int titleY = infoEdit.getY() - 11;
            guiGraphics.drawString(this.font, infoTitle, titleX, titleY, 0xFFDDDDDD);
         }
         infoEdit.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      if (permissionMembersButton != null && permissionEveryoneButton != null
              && permissionMembersButton.visible && selectedInfoTitle != null && !selectedInfoTitle.isBlank()) {
         Component infoTitle = Component.literal("Editing " + selectedInfoTitle);
         int titleWidth = this.font.width(infoTitle);
         int titleX = (this.width - titleWidth) / 2;
         int titleY = permissionMembersButton.getY() - 11;
         guiGraphics.drawString(this.font, infoTitle, titleX, titleY, 0xFFDDDDDD);
      }
   }

   //? if >1.21.8 {
   @Override
   public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
      if (permissionMembersButton != null && permissionMembersButton.visible && permissionMembersButton.active
              && permissionMembersButton.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
         return permissionMembersButton.mouseClicked(mouseButtonEvent, doubleClick);
      }
      if (permissionEveryoneButton != null && permissionEveryoneButton.visible && permissionEveryoneButton.active
              && permissionEveryoneButton.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
         return permissionEveryoneButton.mouseClicked(mouseButtonEvent, doubleClick);
      }
      if (saveInfoButton != null && saveInfoButton.visible && saveInfoButton.active
              && saveInfoButton.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
         return saveInfoButton.mouseClicked(mouseButtonEvent, doubleClick);
      }
      if (saveListNameButton != null && saveListNameButton.visible && saveListNameButton.active
              && saveListNameButton.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
         return saveListNameButton.mouseClicked(mouseButtonEvent, doubleClick);
      }

      if (manualMode && manualEditor != null) {
         if (manualEditor.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.setFocused(manualEditor);
            return manualEditor.mouseClicked(mouseButtonEvent, doubleClick);
         }
         manualEditor.setFocused(false);
      }

      if (renameListEdit != null && renameListEdit.visible) {
         if (renameListEdit.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.setFocused(renameListEdit);
            return renameListEdit.mouseClicked(mouseButtonEvent, doubleClick);
         }
         renameListEdit.setFocused(false);
      }

      if (infoEdit != null && infoEdit.visible) {
         if (infoEdit.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            this.setFocused(infoEdit);
            return infoEdit.mouseClicked(mouseButtonEvent, doubleClick);
         }
         infoEdit.setFocused(false);
      }
      return super.mouseClicked(mouseButtonEvent, doubleClick);
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      if (manualMode && manualEditor != null && manualEditor.isFocused() && manualEditor.keyPressed(keyEvent)) {
         return true;
      }
      if (renameListEdit != null && renameListEdit.visible && renameListEdit.isFocused() && renameListEdit.keyPressed(keyEvent)) {
         return true;
      }
      if (infoEdit != null && infoEdit.visible && infoEdit.isFocused() && infoEdit.keyPressed(keyEvent)) {
         return true;
      }
      return super.keyPressed(keyEvent);
   }

   @Override
   public boolean charTyped(CharacterEvent characterEvent) {
      if (manualMode && manualEditor != null && manualEditor.isFocused() && manualEditor.charTyped(characterEvent)) {
         return true;
      }
      if (renameListEdit != null && renameListEdit.visible && renameListEdit.isFocused() && renameListEdit.charTyped(characterEvent)) {
         return true;
      }
      if (infoEdit != null && infoEdit.visible && infoEdit.isFocused() && infoEdit.charTyped(characterEvent)) {
         return true;
      }
      return super.charTyped(characterEvent);
   }
   //?} else {
   /*@Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (manualMode && manualEditor != null) {
         if (manualEditor.isMouseOver(mouseX, mouseY)) {
            return manualEditor.mouseClicked(mouseX, mouseY, button);
         }
         manualEditor.setFocused(false);
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (manualMode && manualEditor != null && manualEditor.isFocused() && manualEditor.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (manualMode && manualEditor != null && manualEditor.isFocused() && manualEditor.charTyped(chr, modifiers)) {
         return true;
      }
      return super.charTyped(chr, modifiers);
   }
   *///?}

   @Override
   protected void renderMenuBackground(GuiGraphics guiGraphics) {
      guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
      renderMenuBackground(guiGraphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
   }

   @Override
   public void onClose() {
      if (manualMode) {
         applyManualEditorToMembers();
         manualMode = false;
      }
      this.minecraft.setScreen(this.lastScreen);
   }

   private void onTabSelected(net.minecraft.client.gui.components.tabs.Tab tab) {
      membersTabSelected = tab == membersTab;
      if (!membersTabSelected && manualMode) {
         applyManualEditorToMembers();
         manualMode = false;
         if (manualEditor != null) {
            manualEditor.setFocused(false);
         }
      }
      if (!membersTabSelected) {
         refreshInfoEditorFromSelection();
      }
      updateFooterButtons();
   }

   private void updateFooterButtons() {
      if (manualEditorButton == null || topAddPlayerButton == null || topAddListButton == null || topRemoveListButton == null || topRemovePlayerButton == null || topMoveListUpButton == null || topMoveListDownButton == null || saveListNameButton == null || saveInfoButton == null || infoEdit == null || leaveOrDeleteButton == null || permissionMembersButton == null || permissionEveryoneButton == null) {
         return;
      }
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      boolean editableMembers = membersTabSelected && alliance != null && alliance.canEdit;
      boolean editableInfo = !membersTabSelected && alliance != null && alliance.canEdit;
      boolean showPageContent = !manualMode;

      manualEditorButton.visible = editableMembers;
      manualEditorButton.active = editableMembers;

      topAddPlayerButton.visible = editableMembers && showPageContent;
      topAddListButton.visible = editableMembers && showPageContent;
      topRemoveListButton.visible = editableMembers && showPageContent;
      topRemovePlayerButton.visible = editableMembers && showPageContent && selectedPlayerUuid != null && !selectedPlayerUuid.isBlank();
      topMoveListUpButton.visible = editableMembers && showPageContent;
      topMoveListDownButton.visible = editableMembers && showPageContent;
      topAddPlayerButton.active = editableMembers && showPageContent;
      topAddListButton.active = editableMembers && showPageContent;
      topRemoveListButton.active = editableMembers && showPageContent;
      topRemovePlayerButton.active = editableMembers && showPageContent && selectedPlayerUuid != null && !selectedPlayerUuid.isBlank();
      topMoveListUpButton.active = editableMembers && showPageContent;
      topMoveListDownButton.active = editableMembers && showPageContent;

      if (editableMembers && alliance != null) {
         int selectedIndex = listIndexById(alliance, selectedListId);
         if (selectedIndex < 0) {
            selectedIndex = 0;
         }
         topRemoveListButton.active = showPageContent && alliance.data.lists.size() > 1;
         topMoveListUpButton.active = showPageContent && selectedIndex > 0;
         topMoveListDownButton.active = showPageContent && selectedIndex >= 0 && selectedIndex < alliance.data.lists.size() - 1;
      }

      if (infoListWidget != null) {
         infoListWidget.visible = showPageContent;
         infoListWidget.active = showPageContent;
      }
      if (membersListWidget != null) {
         membersListWidget.visible = showPageContent;
         membersListWidget.active = showPageContent;
      }

      boolean manualEditing = editableMembers && manualMode;
      boolean showingRenameControls = editableMembers && showPageContent;
      if (renameListEdit != null) {
         renameListEdit.visible = showingRenameControls;
         renameListEdit.active = showingRenameControls && !manualMode;
      }
      saveListNameButton.visible = showingRenameControls;
      saveListNameButton.active = showingRenameControls && !manualMode;
      saveListNameButton.setMessage(addPlayerMode ? Component.literal("Add") : Component.literal("Save"));

      boolean showingInfoControls = editableInfo && showPageContent && selectedInfoKey != null && !selectedInfoKey.isBlank();
      boolean showingPermissionControls = showingInfoControls && "subscription_permission".equals(selectedInfoKey);
      infoEdit.visible = showingInfoControls;
      infoEdit.active = showingInfoControls;
      saveInfoButton.visible = showingInfoControls;
      saveInfoButton.active = showingInfoControls;

      if (showingPermissionControls) {
         infoEdit.visible = false;
         infoEdit.active = false;
         saveInfoButton.visible = false;
         saveInfoButton.active = false;
      }

      permissionMembersButton.visible = showingPermissionControls;
      permissionEveryoneButton.visible = showingPermissionControls;
      permissionMembersButton.active = showingPermissionControls;
      permissionEveryoneButton.active = showingPermissionControls;
      if (showingPermissionControls && alliance != null) {
         String permission = alliance.subscriptionPermission == null ? "MEMBERS" : alliance.subscriptionPermission;
         if (permission.equalsIgnoreCase("ANYONE")) {
            permissionEveryoneButton.active = false;
         } else {
            permissionMembersButton.active = false;
         }
      }

      if (manualEditor != null) {
         manualEditor.active = manualEditing;
      }

      if (manualMode) {
         manualEditorButton.setMessage(Component.literal("Visual Editor"));
      } else {
         manualEditorButton.setMessage(Component.literal("Manual Editor"));
      }

      if (!showingRenameControls && renameListEdit != null) {
         renameListEdit.setFocused(false);
      }

      if (!showingInfoControls) {
         infoEdit.setFocused(false);
      }

      if (showingRenameControls && renameListEdit != null && renameListEdit.getValue().isBlank()) {
         if (addPlayerMode) {
            renameListEdit.setHint(Component.literal("Username or UUID"));
         } else {
            renameListEdit.setHint(Component.literal("List name"));
         }
      }

      boolean showLeaveDelete = alliance != null && !membersTabSelected;
      leaveOrDeleteButton.visible = showLeaveDelete;
      leaveOrDeleteButton.active = showLeaveDelete;
      if (showLeaveDelete) {
         boolean showLeave = alliance.serverId != null && !alliance.serverId.isBlank() && !alliance.canEdit;
         leaveOrDeleteButton.setMessage(Component.translatable(showLeave ? "lsu.alliances.detail.leave" : "lsu.alliances.detail.delete"));
      }
   }

   private void toggleManualMode() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit || !membersTabSelected) {
         return;
      }
      manualMode = !manualMode;
      if (manualMode) {
         refreshManualEditorFromMembers();
         manualEditor.setFocused(true);
         addPlayerMode = false;
      } else {
         applyManualEditorToMembers();
         manualEditor.setFocused(false);
      }
      updateFooterButtons();
   }

   private void leaveOrDeleteAlliance() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null) {
         return;
      }

      boolean shouldLeave = alliance.serverId != null && !alliance.serverId.isBlank() && !alliance.canEdit;
      if (shouldLeave) {
         boolean ok;
         try {
            ok = GaiaApiClient.getInstance().alliances().unsubscribe(alliance.serverId);
         } catch (Exception ignored) {
            return;
         }
         if (!ok) {
            return;
         }
      }

      AllianceService.delete(alliance.clientId);
      AllianceSyncManager.syncSubscriptionsNow();
      AllianceService.reloadFromDisk();
      this.minecraft.setScreen(this.lastScreen);
   }

   private void refreshManualEditorFromMembers() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || manualEditor == null) {
         return;
      }
      StringBuilder out = new StringBuilder();
      for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
         if (!out.isEmpty()) {
            out.append("\n\n");
         }
         out.append("[").append(list.name == null ? "Unnamed List" : list.name).append("]");
         for (AllianceModels.AllianceMember member : list.members) {
            if (member.uuid == null || member.uuid.isBlank()) {
               continue;
            }
            out.append('\n');
            out.append(member.uuid);
         }
      }
      manualEditor.setValue(out.toString());
   }

   private void applyManualEditorToMembers() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit || manualEditor == null || alliance.data.lists.isEmpty()) {
         return;
      }
      String[] lines = manualEditor.getValue().split("\\R");
      ArrayList<AllianceModels.AlliancePlayerList> rebuiltLists = new ArrayList<>();

      AllianceModels.AlliancePlayerList currentList = null;
      for (String rawLine : lines) {
         String line = rawLine == null ? "" : rawLine.trim();
         if (line.isBlank()) {
            continue;
         }
         if (line.startsWith("[") && line.endsWith("]") && line.length() >= 3) {
            String listName = line.substring(1, line.length() - 1).trim();
            currentList = new AllianceModels.AlliancePlayerList();
            currentList.id = AllianceIdGenerator.newListId();
            currentList.name = listName.isBlank() ? "Unnamed List" : listName;
            currentList.prefix = "";
            currentList.prefixColor = alliance.data.color;
            currentList.nameColor = 0xFFFFFF;
            currentList.members = new ArrayList<>();
            rebuiltLists.add(currentList);
            continue;
         }

         if (currentList == null) {
            currentList = new AllianceModels.AlliancePlayerList();
            currentList.id = AllianceIdGenerator.newListId();
            currentList.name = "Members";
            currentList.prefix = "";
            currentList.prefixColor = alliance.data.color;
            currentList.nameColor = 0xFFFFFF;
            currentList.members = new ArrayList<>();
            rebuiltLists.add(currentList);
         }

         String normalized = AllianceProfileCacheManager.normalizeUuid(line);
         if (normalized == null) {
            continue;
         }
         AllianceModels.AllianceMember member = new AllianceModels.AllianceMember();
         member.uuid = normalized;
         member.addedAt = System.currentTimeMillis();
         currentList.members.add(member);
      }

      if (rebuiltLists.isEmpty()) {
         AllianceModels.AlliancePlayerList fallback = new AllianceModels.AlliancePlayerList();
         fallback.id = AllianceIdGenerator.newListId();
         fallback.name = "Members";
         fallback.prefix = "";
         fallback.prefixColor = alliance.data.color;
         fallback.nameColor = 0xFFFFFF;
         fallback.members = new ArrayList<>();
         rebuiltLists.add(fallback);
      }

      alliance.data.lists = rebuiltLists;
      if (selectedListId == null || selectedListId.isBlank() || listIndexById(alliance, selectedListId) < 0) {
         selectedListId = alliance.data.lists.get(0).id;
      }

      AllianceService.save(alliance);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      if (membersListWidget != null) {
         membersListWidget.refreshEntries();
      }
      if (renameListEdit != null) {
         renameListEdit.setValue("");
      }
   }

   private void applyRename() {
      if (addPlayerMode) {
         applyAddPlayer();
         return;
      }

      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || alliance.data == null || alliance.data.lists == null || alliance.data.lists.isEmpty()) {
         return;
      }
      String targetName = renameListEdit.getValue() == null ? "" : renameListEdit.getValue().trim();
      if (targetName.isBlank()) {
         return;
      }
      AllianceModels.AlliancePlayerList target = null;
      for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
         if (list.id != null && list.id.equals(selectedListId)) {
            target = list;
            break;
         }
      }
      if (target == null) {
         target = alliance.data.lists.get(0);
      }
      target.name = targetName;
      selectedListId = target.id;
      renameListEdit.setValue(targetName);
      AllianceService.save(alliance);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      if (membersListWidget != null) {
         membersListWidget.refreshEntries();
      }
      if (infoListWidget != null) {
         infoListWidget.refreshEntries();
      }
   }

   private void selectInfoField(String key, String title, int maxLength) {
      selectedInfoKey = key == null ? "" : key;
      selectedInfoTitle = title == null ? "" : title;
      selectedInfoMaxLength = Math.max(1, maxLength);
      refreshInfoEditorFromSelection();
      updateFooterButtons();
   }

   private void refreshInfoEditorFromSelection() {
      if (infoEdit == null) {
         return;
      }
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || alliance.data == null || selectedInfoKey == null || selectedInfoKey.isBlank()) {
         infoEdit.setValue("");
         return;
      }
      infoEdit.setMaxLength(selectedInfoMaxLength);
      if ("name".equals(selectedInfoKey)) {
         infoEdit.setValue(alliance.data.name == null ? "" : alliance.data.name);
         return;
      }
      if ("description".equals(selectedInfoKey)) {
         infoEdit.setValue(alliance.data.description == null ? "" : alliance.data.description);
         return;
      }
      if ("color".equals(selectedInfoKey)) {
         infoEdit.setValue(formatRgb(alliance.data.color));
         return;
      }
      if (selectedInfoKey.startsWith("prefix:")) {
         String listId = selectedInfoKey.substring("prefix:".length());
         AllianceModels.AlliancePlayerList list = findListById(alliance, listId);
         infoEdit.setValue(list == null || list.prefix == null ? "" : list.prefix);
         return;
      }
      if (selectedInfoKey.startsWith("prefix_color:")) {
         String listId = selectedInfoKey.substring("prefix_color:".length());
         AllianceModels.AlliancePlayerList list = findListById(alliance, listId);
         infoEdit.setValue(list == null ? formatRgb(alliance.data.color) : formatRgb(list.prefixColor));
         return;
      }
      if (selectedInfoKey.startsWith("name_color:")) {
         String listId = selectedInfoKey.substring("name_color:".length());
         AllianceModels.AlliancePlayerList list = findListById(alliance, listId);
         infoEdit.setValue(list == null ? "#FFFFFF" : formatRgb(list.nameColor));
         return;
      }
      if ("subscription_permission".equals(selectedInfoKey)) {
         infoEdit.setValue(permissionEditorValue(alliance.subscriptionPermission));
      }
   }

   private void applyInfoEdit() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit || alliance.data == null || selectedInfoKey == null || selectedInfoKey.isBlank() || infoEdit == null) {
         return;
      }
      String value = infoEdit.getValue() == null ? "" : infoEdit.getValue().trim();
      if ("name".equals(selectedInfoKey)) {
         if (value.isBlank()) {
            return;
         }
         alliance.data.name = value;
      } else if ("description".equals(selectedInfoKey)) {
         alliance.data.description = value;
      } else if ("color".equals(selectedInfoKey)) {
         Integer rgb = parseRgbColor(value);
         if (rgb == null) {
            return;
         }
         alliance.data.color = rgb;
      } else if (selectedInfoKey.startsWith("prefix:")) {
         String listId = selectedInfoKey.substring("prefix:".length());
         AllianceModels.AlliancePlayerList list = findListById(alliance, listId);
         if (list == null) {
            return;
         }
         list.prefix = value;
      } else if (selectedInfoKey.startsWith("prefix_color:")) {
         String listId = selectedInfoKey.substring("prefix_color:".length());
         AllianceModels.AlliancePlayerList list = findListById(alliance, listId);
         if (list == null) {
            return;
         }
         Integer rgb = parseRgbColor(value);
         if (rgb == null) {
            return;
         }
         list.prefixColor = rgb;
      } else if (selectedInfoKey.startsWith("name_color:")) {
         String listId = selectedInfoKey.substring("name_color:".length());
         AllianceModels.AlliancePlayerList list = findListById(alliance, listId);
         if (list == null) {
            return;
         }
         Integer rgb = parseRgbColor(value);
         if (rgb == null) {
            return;
         }
         list.nameColor = rgb;
      } else if ("subscription_permission".equals(selectedInfoKey)) {
         String permission = parseSubscriptionPermission(value);
         if (permission == null) {
            return;
         }
         alliance.subscriptionPermission = permission;
      } else {
         return;
      }

      AllianceService.save(alliance);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      refreshInfoEditorFromSelection();
      if (infoListWidget != null) {
         infoListWidget.refreshEntries();
      }
      if (membersListWidget != null) {
         membersListWidget.refreshEntries();
      }
   }

   private void setSubscriptionPermission(String permission) {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit || alliance.data == null || permission == null || permission.isBlank()) {
         return;
      }
      String normalized = permission.trim().toUpperCase();
      if (!normalized.equals("MEMBERS") && !normalized.equals("ANYONE")) {
         return;
      }
      if (normalized.equalsIgnoreCase(alliance.subscriptionPermission == null ? "" : alliance.subscriptionPermission)) {
         updateFooterButtons();
         return;
      }
      alliance.subscriptionPermission = normalized;
      AllianceService.save(alliance);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      refreshInfoEditorFromSelection();
      if (infoListWidget != null) {
         infoListWidget.refreshEntries();
      }
      updateFooterButtons();
   }

   private AllianceModels.AlliancePlayerList findListById(AllianceModels.AllianceRecord alliance, String listId) {
      if (alliance == null || alliance.data == null || alliance.data.lists == null || listId == null || listId.isBlank()) {
         return null;
      }
      for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
         if (list.id != null && list.id.equals(listId)) {
            return list;
         }
      }
      return null;
   }

   private static Integer parseRgbColor(String raw) {
      if (raw == null || raw.isBlank()) {
         return null;
      }
      String value = raw.trim();
      try {
         int parsed = Integer.parseInt(value);
         if (parsed < 0 || parsed > 0xFFFFFF) {
            return null;
         }
         return parsed;
      } catch (NumberFormatException ignored) {
      }
      if (value.matches("(?i)^[0-9a-f]{6}$")) {
         try {
            return Integer.parseInt(value, 16);
         } catch (NumberFormatException e) {
            return null;
         }
      }
      if (value.matches("(?i)^[0-9a-f]{3}$")) {
         try {
            String expanded = "" + value.charAt(0) + value.charAt(0)
                    + value.charAt(1) + value.charAt(1)
                    + value.charAt(2) + value.charAt(2);
            return Integer.parseInt(expanded, 16);
         } catch (NumberFormatException e) {
            return null;
         }
      }
      if (value.startsWith("#")) {
         String hex = value.substring(1);
         if (hex.matches("(?i)^[0-9a-f]{6}$")) {
            try {
               return Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
               return null;
            }
         }
         if (hex.matches("(?i)^[0-9a-f]{3}$")) {
            try {
               String expanded = "" + hex.charAt(0) + hex.charAt(0)
                       + hex.charAt(1) + hex.charAt(1)
                       + hex.charAt(2) + hex.charAt(2);
               return Integer.parseInt(expanded, 16);
            } catch (NumberFormatException e) {
               return null;
            }
         }
         return null;
      }
      String[] parts = value.split(",");
      if (parts.length != 3) {
         return null;
      }
      try {
         int r = Integer.parseInt(parts[0].trim());
         int g = Integer.parseInt(parts[1].trim());
         int b = Integer.parseInt(parts[2].trim());
         if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            return null;
         }
         return (r << 16) | (g << 8) | b;
      } catch (NumberFormatException e) {
         return null;
      }
   }

   private static String formatRgb(int color) {
      int safe = (color < 0 || color > 0xFFFFFF) ? 0xFFFFFF : color;
      return String.format("#%06X", safe);
   }

   private static int visibleColorOrWhite(int color) {
      if (color < 0 || color > 0xFFFFFF) {
         return 0xFFFFFFFF;
      }
      return 0xFF000000 | color;
   }

   private static Component permissionText(String subscriptionPermission) {
      if (subscriptionPermission != null && subscriptionPermission.equalsIgnoreCase("ANYONE")) {
         return Component.literal("Anyone can subscribe");
      }
      return Component.literal("Only members can subscribe");
   }

   private static String permissionEditorValue(String subscriptionPermission) {
      if (subscriptionPermission != null && subscriptionPermission.equalsIgnoreCase("ANYONE")) {
         return "everyone";
      }
      return "members";
   }

   private static String parseSubscriptionPermission(String rawValue) {
      if (rawValue == null || rawValue.isBlank()) {
         return null;
      }
      String normalized = rawValue.trim().toUpperCase();
      if (normalized.equals("EVERYONE") || normalized.equals("ANYONE")) {
         return "ANYONE";
      }
      if (normalized.equals("MEMBERS") || normalized.equals("MEMBER") || normalized.equals("MEMBERS_ONLY")) {
         return "MEMBERS";
      }
      return null;
   }

   private static Component inviteCodeText(AllianceModels.AllianceRecord alliance) {
      if (alliance == null || alliance.serverId == null || alliance.serverId.isBlank()) {
         return Component.literal("<alliance is unshareable>");
      }
      return Component.literal(alliance.serverId);
   }

   private void openAddPlayerCommand() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit) {
         return;
      }
      addPlayerMode = true;
      if (renameListEdit != null) {
         renameListEdit.setValue("");
         renameListEdit.setHint(Component.literal("Username or UUID"));
         renameListEdit.setFocused(true);
      }
      updateFooterButtons();
   }

   private void applyAddPlayer() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit) {
         return;
      }
      String usernameOrUuid = renameListEdit.getValue() == null ? "" : renameListEdit.getValue().trim();
      if (usernameOrUuid.isBlank()) {
         return;
      }

      AllianceModels.AlliancePlayerList targetList = selectedListWithFallback(alliance);
      if (targetList == null) {
         return;
      }

      String uuid = AllianceProfileCacheManager.resolveUuidFromInput(usernameOrUuid);
      if (uuid == null) {
         return;
      }

      boolean added = AllianceService.addMember(alliance, targetList.id, uuid);
      if (!added) {
         return;
      }

      AllianceProfileCacheManager.cache(usernameOrUuid, uuid);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      if (membersListWidget != null) {
         membersListWidget.refreshEntries();
      }
      if (infoListWidget != null) {
         infoListWidget.refreshEntries();
      }

      renameListEdit.setValue("");
      renameListEdit.setFocused(true);
   }

   private void removeSelectedPlayer() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit || selectedPlayerUuid == null || selectedPlayerUuid.isBlank()) {
         return;
      }
      boolean removed = AllianceService.removeMember(alliance, selectedPlayerUuid);
      if (!removed) {
         return;
      }
      selectedPlayerUuid = "";
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      if (membersListWidget != null) {
         membersListWidget.refreshEntries();
      }
      if (infoListWidget != null) {
         infoListWidget.refreshEntries();
      }
      updateFooterButtons();
   }

   private void removeSelectedList() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit || alliance.data == null || alliance.data.lists == null || alliance.data.lists.size() <= 1) {
         return;
      }
      int index = listIndexById(alliance, selectedListId);
      if (index < 0) {
         index = alliance.data.lists.size() - 1;
      }
      alliance.data.lists.remove(index);
      if (alliance.data.lists.isEmpty()) {
         AllianceModels.AlliancePlayerList fallback = new AllianceModels.AlliancePlayerList();
         fallback.id = AllianceIdGenerator.newListId();
         fallback.name = "Members";
         fallback.prefix = "";
         fallback.prefixColor = alliance.data.color;
         fallback.nameColor = 0xFFFFFF;
         fallback.members = new ArrayList<>();
         alliance.data.lists.add(fallback);
      }
      if (index >= alliance.data.lists.size()) {
         index = alliance.data.lists.size() - 1;
      }
      selectedListId = alliance.data.lists.get(index).id;
      selectedPlayerUuid = "";
      AllianceService.save(alliance);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      if (membersListWidget != null) {
         membersListWidget.refreshEntries();
      }
      if (infoListWidget != null) {
         infoListWidget.refreshEntries();
      }
      if (renameListEdit != null) {
         renameListEdit.setValue(addPlayerMode ? "" : selectedListName());
      }
      updateFooterButtons();
   }

   private void moveSelectedList(int direction) {
      if (direction == 0) {
         return;
      }
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit || alliance.data == null || alliance.data.lists == null || alliance.data.lists.size() < 2) {
         return;
      }
      int index = listIndexById(alliance, selectedListId);
      if (index < 0) {
         index = 0;
      }
      int newIndex = index + direction;
      if (newIndex < 0 || newIndex >= alliance.data.lists.size()) {
         return;
      }
      AllianceModels.AlliancePlayerList current = alliance.data.lists.get(index);
      alliance.data.lists.set(index, alliance.data.lists.get(newIndex));
      alliance.data.lists.set(newIndex, current);
      AllianceService.save(alliance);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      if (membersListWidget != null) {
         membersListWidget.refreshEntries();
      }
      if (infoListWidget != null) {
         infoListWidget.refreshEntries();
      }
      updateFooterButtons();
   }

   private int listIndexById(AllianceModels.AllianceRecord alliance, String listId) {
      if (alliance == null || alliance.data == null || alliance.data.lists == null || listId == null || listId.isBlank()) {
         return -1;
      }
      for (int i = 0; i < alliance.data.lists.size(); i++) {
         AllianceModels.AlliancePlayerList list = alliance.data.lists.get(i);
         if (list.id != null && list.id.equals(listId)) {
            return i;
         }
      }
      return -1;
   }

   private String selectedListName() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || alliance.data == null || alliance.data.lists == null) {
         return "";
      }
      for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
         if (list.id != null && list.id.equals(selectedListId)) {
            return list.name == null ? "" : list.name;
         }
      }
      return "";
   }

   private String selectedListNameWithFallback() {
      String selected = selectedListName();
      if (!selected.isBlank()) {
         return selected;
      }
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      AllianceModels.AlliancePlayerList fallback = selectedListWithFallback(alliance);
      return fallback == null || fallback.name == null ? "" : fallback.name;
   }

   private AllianceModels.AlliancePlayerList selectedListWithFallback(AllianceModels.AllianceRecord alliance) {
      if (alliance == null || alliance.data == null || alliance.data.lists == null || alliance.data.lists.isEmpty()) {
         return null;
      }
      if (selectedListId != null && !selectedListId.isBlank()) {
         for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
            if (selectedListId.equals(list.id)) {
               return list;
            }
         }
      }
      if (alliance.lastUsedListId != null && !alliance.lastUsedListId.isBlank()) {
         for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
            if (alliance.lastUsedListId.equals(list.id)) {
               return list;
            }
         }
      }
      for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
         if (list.members != null && !list.members.isEmpty()) {
            return list;
         }
      }
      return alliance.data.lists.get(0);
   }

   private void addList() {
      AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
      if (alliance == null || !alliance.canEdit) {
         return;
      }
      AllianceModels.AlliancePlayerList list = new AllianceModels.AlliancePlayerList();
      list.id = AllianceIdGenerator.newListId();
      list.name = "List " + (alliance.data.lists.size() + 1);
      list.prefix = "";
      list.prefixColor = alliance.data.color;
      list.nameColor = 0xFFFFFF;
      list.members = new ArrayList<>();
      alliance.data.lists.add(list);
      alliance.lastUsedListId = list.id;
      AllianceService.save(alliance);
      AllianceSyncManager.publishOrUpdateAsync(alliance);
      if (infoListWidget != null) {
         infoListWidget.refreshEntries();
      }
      if (membersListWidget != null) {
         membersListWidget.refreshEntries();
      }
   }

   private class SimpleTab extends GridLayoutTab {
      private final AbstractSelectionList<?> list;

      public SimpleTab(Component title, AbstractSelectionList<?> list) {
         super(title);
         this.layout.addChild((LayoutElement) list, 1, 1);
         this.list = list;
      }

      @Override
      public void doLayout(ScreenRectangle screenRectangle) {
         int tabBottom = AllianceDetailScreen.this.tabNavigationBar.getRectangle().bottom();
         int contentBottom = AllianceDetailScreen.this.height - AllianceDetailScreen.this.layout.getFooterHeight() - INFO_EDITOR_RESERVED_SPACE;
         int listHeight = Math.max(40, contentBottom - tabBottom);
         this.list.updateSizeAndPosition(AllianceDetailScreen.this.width, listHeight, tabBottom);
         super.doLayout(screenRectangle);
      }
   }

   private class InfoListWidget extends ObjectSelectionList<InfoListWidget.RowEntry> {
      public InfoListWidget(Minecraft minecraft) {
         super(minecraft, AllianceDetailScreen.this.width, AllianceDetailScreen.this.layout.getContentHeight(), 33, 18);
         refreshEntries();
      }

      public void refreshEntries() {
         clearEntries();
         AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
         if (alliance == null || alliance.data == null) {
            return;
         }
         addEntry(new RowEntry(Component.translatable("lsu.alliances.detail.name"), Component.literal(alliance.data.name == null ? "" : alliance.data.name), true, "name", 64));
         addEntry(new RowEntry(Component.translatable("lsu.alliances.detail.description"), Component.literal(alliance.data.description == null ? "" : alliance.data.description), true, "description", 1024));
         addEntry(new RowEntry(Component.translatable("lsu.alliances.detail.color"), Component.literal(formatRgb(alliance.data.color)), true, "color", 32, visibleColorOrWhite(alliance.data.color)));
         addEntry(new RowEntry(Component.translatable("lsu.alliances.detail.member_count"), Component.literal(String.valueOf(AllianceService.totalMembers(alliance))), false, "", 1));

         if (alliance.data.lists != null) {
            for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
               String listName = list.name == null || list.name.isBlank() ? "Unnamed List" : list.name;
               String listId = list.id == null ? "" : list.id;
               addEntry(new RowEntry(Component.literal("(" + listName + ") Prefix"), Component.literal(list.prefix == null ? "" : list.prefix), true, "prefix:" + listId, 256));
               addEntry(new RowEntry(Component.literal("(" + listName + ") Prefix Color"), Component.literal(formatRgb(list.prefixColor)), true, "prefix_color:" + listId, 32, visibleColorOrWhite(list.prefixColor)));
               addEntry(new RowEntry(Component.literal("(" + listName + ") Name Color"), Component.literal(formatRgb(list.nameColor)), true, "name_color:" + listId, 32, visibleColorOrWhite(list.nameColor)));
            }
         }

         if (alliance.canEdit) {
            addEntry(new RowEntry(Component.literal("Invite Code"), inviteCodeText(alliance), false, "", 1));
            addEntry(new RowEntry(Component.literal("Permission"), permissionText(alliance.subscriptionPermission), true, "subscription_permission", 16));
         }
      }

      @Override
      public int getRowWidth() {
         return 340;
      }

      @Override
      protected void renderListBackground(GuiGraphics guiGraphics) {
      }

      @Override
      protected void renderListSeparators(GuiGraphics guiGraphics) {
      }

      private class RowEntry extends ObjectSelectionList.Entry<RowEntry> {
         private final Component left;
         private final Component right;
         private final boolean editable;
         private final String key;
         private final int maxLength;
         private final int rightColor;

         RowEntry(Component left, Component right, boolean editable, String key, int maxLength) {
            this(left, right, editable, key, maxLength, -1);
         }

         RowEntry(Component left, Component right, boolean editable, String key, int maxLength, int rightColor) {
            this.left = left;
            this.right = right;
            this.editable = editable;
            this.key = key == null ? "" : key;
            this.maxLength = maxLength;
            this.rightColor = rightColor;
         }

         @Override
         public Component getNarration() {
            return left.copy().append(Component.literal(" ")).append(right);
         }

         //? if >1.21.8 {
         @Override
         public void renderContent(GuiGraphics guiGraphics, int index, int entryWidth, boolean isSelected, float partialTick) {
            int x = getContentX() + 8;
            int y = getContentY() + 4;
            int configuredRightColor = rightColor != -1 ? rightColor : (editable ? 0xFF88CCFF : 0xFFAAAAAA);
            guiGraphics.drawString(AllianceDetailScreen.this.font, left, x, y, 0xFFFFFFFF);
            guiGraphics.drawString(AllianceDetailScreen.this.font, right, x + 140, y, configuredRightColor);
         }
         //?} else {
         /*@Override
         public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            guiGraphics.drawString(AllianceDetailScreen.this.font, left, x + 8, y + 4, 0xFFFFFF);
            guiGraphics.drawString(AllianceDetailScreen.this.font, right, x + 148, y + 4, 0xAAAAAA);
         }
          *///?}

         //? if >1.21.8 {
         @Override
         public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
            if (mouseButtonEvent.button() != 0) {
               return false;
            }
            InfoListWidget.this.setSelected(this);
            if (editable) {
               selectInfoField(key, left.getString(), maxLength);
            }
            return true;
         }
         //?} else {
         /*@Override
         public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
               return false;
            }
            InfoListWidget.this.setSelected(this);
            if (editable) {
               selectInfoField(key, left.getString(), maxLength);
            }
            return true;
         }
         *///?}
      }
   }

   private class MembersListWidget extends ObjectSelectionList<MembersListWidget.MemberEntry> {
      public MembersListWidget(Minecraft minecraft) {
         super(minecraft, AllianceDetailScreen.this.width, AllianceDetailScreen.this.layout.getContentHeight(), 33, 18);
         refreshEntries();
      }

      public void refreshEntries() {
         clearEntries();
         AllianceModels.AllianceRecord alliance = AllianceService.findByClientId(allianceClientId);
         if (alliance == null || alliance.data == null || alliance.data.lists == null) {
            return;
         }
         for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
            addEntry(new MemberEntry(list.id, null, Component.literal("[" + list.name + "]"), true, visibleColorOrWhite(list.prefixColor)));
            if (list.members == null || list.members.isEmpty()) {
               addEntry(new MemberEntry(list.id, null, Component.literal("<no players in list>").withStyle(style -> style.withItalic(true).withColor(0xAAAAAA)), false, 0xFFFFFF));
            } else {
               for (AllianceModels.AllianceMember member : list.members) {
                  addEntry(new MemberEntry(list.id, member.uuid, Component.literal(AllianceProfileCacheManager.displayNameForUuid(member.uuid)), false, 0xFFFFFF));
               }
            }
         }

         ArrayList<String> allUuids = new ArrayList<>();
         for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
            for (AllianceModels.AllianceMember member : list.members) {
               if (member.uuid != null && !member.uuid.isBlank()) {
                  allUuids.add(member.uuid);
               }
            }
         }
         for (String unknown : AllianceProfileCacheManager.collectUnknownUuids(allUuids)) {
            AllianceProfileCacheManager.queueNameLookupForUuid(unknown);
         }
      }

      @Override
      public int getRowWidth() {
         return 340;
      }

      @Override
      protected void renderListBackground(GuiGraphics guiGraphics) {
      }

      @Override
      protected void renderListSeparators(GuiGraphics guiGraphics) {
      }

      private class MemberEntry extends ObjectSelectionList.Entry<MemberEntry> {
         private static final int FACE_SIZE = 12;
         private final String listId;
         private final String memberUuid;
         private final Component text;
         private final boolean heading;
         private final int headingColor;
         private final Supplier<PlayerSkin> skinSupplier;

         MemberEntry(String listId, String memberUuid, Component text, boolean heading, int headingColor) {
            this.listId = listId;
            this.memberUuid = memberUuid;
            this.text = text;
            this.heading = heading;
            this.headingColor = headingColor;
            if (!heading && memberUuid != null) {
               String normalized = AllianceProfileCacheManager.normalizeUuid(memberUuid);
               if (normalized != null) {
                  this.skinSupplier = SKIN_SUPPLIER_BY_UUID.computeIfAbsent(normalized, key -> {
                     //? if >1.21.8 {
                     ResolvableProfile profile = ResolvableProfile.createUnresolved(UUID.fromString(key));
                     AllianceDetailScreen.this.minecraft.playerSkinRenderCache().lookup(profile);
                     return () -> AllianceDetailScreen.this.minecraft.playerSkinRenderCache().getOrDefault(profile).playerSkin();
                     //?} else {
                     /*return () -> DefaultPlayerSkin.get(UUID.fromString(key));
                      *///?}
                  });
               } else {
                  this.skinSupplier = DefaultPlayerSkin::getDefaultSkin;
               }
            } else {
               this.skinSupplier = DefaultPlayerSkin::getDefaultSkin;
            }
         }

         @Override
         public Component getNarration() {
            return text;
         }

         //? if >1.21.8 {
         @Override
         public void renderContent(GuiGraphics guiGraphics, int index, int entryWidth, boolean isSelected, float partialTick) {
            int x = getContentX() + 8;
            int y = getContentY() + 4;
            if (heading) {
               guiGraphics.drawString(AllianceDetailScreen.this.font, text, x, y, headingColor);
            } else {
               PlayerFaceRenderer.draw(guiGraphics, skinSupplier.get(), x, y - 1, FACE_SIZE);
               guiGraphics.drawString(AllianceDetailScreen.this.font, text, x + FACE_SIZE + 4, y, 0xFFFFFFFF);
            }
         }
         //?} else {
         /*@Override
         public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            guiGraphics.drawString(AllianceDetailScreen.this.font, text, x + 8, y + 4, heading ? 0x88CCFF : 0xFFFFFF);
         }
         *///?}

         //? if >1.21.8 {
         @Override
         public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
            if (mouseButtonEvent.button() != 0) {
               return false;
            }
            MembersListWidget.this.setSelected(this);
            if (heading && listId != null && !listId.isBlank()) {
               selectedListId = listId;
               selectedPlayerUuid = "";
               if (renameListEdit != null && renameListEdit.visible) {
                  renameListEdit.setValue(selectedListName());
                  renameListEdit.setFocused(true);
               }
            } else {
               selectedPlayerUuid = memberUuid == null ? "" : memberUuid;
            }
            updateFooterButtons();
            return true;
         }
         //?} else {
         /*@Override
         public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
               return false;
            }
            MembersListWidget.this.setSelected(this);
            if (heading && listId != null && !listId.isBlank()) {
               selectedListId = listId;
               selectedPlayerUuid = "";
               if (renameListEdit != null && renameListEdit.visible) {
                  renameListEdit.setValue(selectedListName());
                  renameListEdit.setFocused(true);
               }
            } else {
               selectedPlayerUuid = memberUuid == null ? "" : memberUuid;
            }
            updateFooterButtons();
            return true;
         }
         *///?}
      }
   }
}
