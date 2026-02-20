package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.ui.framework.screens.DrawableScreen;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.features.alliances.service.AllianceManagers;
import dev.candycup.lifestealutils.features.alliances.service.PlayerUuidResolver;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * displays detailed information about an alliance using the drawable ui system.
 */
public class AllianceDetailScreen extends DrawableScreen {
   private static final Component TITLE = Component.translatable("lsu.alliances.detail.title");
   private static final Component ACCEPT_INVITE_BUTTON = Component.translatable("lsu.alliances.detail.accept_invite");
   private static final Component REJECT_INVITE_BUTTON = Component.translatable("lsu.alliances.detail.reject_invite");
   private static final Component LEAVE_BUTTON = Component.translatable("lsu.alliances.detail.leave");
   private static final Component DELETE_BUTTON = Component.translatable("lsu.alliances.detail.delete");
   private static final Component REFRESHING_TEXT = Component.translatable("lsu.alliances.detail.refreshing");
   private static final Component REFRESH_FAILED_TEXT = Component.translatable("lsu.alliances.detail.refresh_failed");
   private static final Component NO_MEMBERS_TEXT = Component.translatable("lsu.alliances.detail.no_members");
   private static final Component INVITE_FAILED_TEXT = Component.translatable("lsu.alliances.detail.invite_failed");
   private static final Component INVITE_REQUIRED_TEXT = Component.translatable("lsu.alliances.detail.invite_required");
   private static final Component ADD_REQUIRED_TEXT = Component.translatable("lsu.alliances.detail.add_required");
   private static final Component ADD_FAILED_TEXT = Component.translatable("lsu.alliances.detail.add_failed");
   private static final Component CANCEL_FAILED_TEXT = Component.translatable("lsu.alliances.detail.cancel_failed");
   private static final Component REMOVE_FAILED_TEXT = Component.translatable("lsu.alliances.detail.remove_failed");
   private static final Component INVITE_LABEL = Component.translatable("lsu.alliances.detail.invite");
   private static final Component ADD_LABEL = Component.translatable("lsu.alliances.detail.add");
   private static final Component INVITE_HINT = Component.translatable("lsu.alliances.detail.invite_hint");
   private static final Component REFRESH_LABEL = Component.translatable("lsu.alliances.refresh");
   private static final Component BACK_LABEL = Component.translatable("gui.back");
   private static final Component INFO_TAB_LABEL = Component.translatable("lsu.alliances.detail.info_tab");
   private static final Component MEMBERS_TAB_LABEL = Component.translatable("lsu.alliances.detail.members_tab");
   private static final Component PENDING_INVITES_TITLE = Component.translatable("lsu.alliances.detail.pending_invites");

   private static final int INVITE_MAX_LENGTH = 36;
   private static final int EDIT_MAX_NAME_LENGTH = 30;
   private static final int EDIT_MAX_PREFIX_LENGTH = 30;
   private static final int EDIT_MAX_COLOR_LENGTH = 7;
   private static final int EDIT_MAX_DESCRIPTION_LENGTH = 200;
   private static final int EDIT_MAX_MOTD_LENGTH = 300;
   private static final int DEFAULT_MULTILINE_WIDTH = AllianceEditStyle.PANEL_WIDTH - AllianceEditStyle.PANEL_PADDING * 2;
   private final Screen lastScreen;
   private Alliance alliance;
   private AllianceMember currentPlayerMember;
   private boolean isInvited;
   private boolean refreshing;
   private boolean inviting;
   private String normalizedPlayerUuid = "";

   private Tab activeTab = Tab.INFO;

   private AllianceDetailHeaderBlock headerBlock;
   private AllianceStatusText statusText;
   private AllianceDetailTabBar tabBar;
   private AllianceTabContent tabContent;
   private AllianceInfoListView infoList;
   private AllianceMembersListView membersList;
   private AllianceMembersPanel membersPanel;
   private AllianceDetailActionsRow actionsRow;
   private AllianceDialogLayer dialogLayer;

   private EditBox inviteField;
   private AllianceInviteRow inviteRow;
   private AllianceConfirmDialog confirmDialog;
   private AllianceEditDialog editDialog;

   private EditBox editShortField;
   private MultiLineEditBox editLongField;
   private AllianceEditField activeEditField;
   private boolean editSaving;
   private boolean editUseShortField;
   private final AllianceStatusState editStatus = new AllianceStatusState(AlliancesListStyle.TEXT_MUTED);

   private final AllianceStatusState status = new AllianceStatusState(AllianceDetailStyle.TEXT_MUTED);

   private enum Tab {
      INFO,
      MEMBERS
   }

   /**
    * creates a new alliance detail screen.
    *
    * @param lastScreen the screen to return to when closing
    * @param alliance   the alliance to display
    */
   public AllianceDetailScreen(Screen lastScreen, Alliance alliance) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.alliance = alliance;
      updateMembershipState();
   }

   @Override
   protected void init() {
      this.inviteField = new EditBox(this.minecraft.font, 0, 0, 0, AllianceDetailStyle.BUTTON_HEIGHT, Component.empty());
      this.inviteField.setMaxLength(INVITE_MAX_LENGTH);
      this.inviteField.setHint(INVITE_HINT);
      addRenderableWidget(this.inviteField);

      this.editShortField = new EditBox(this.minecraft.font, 0, 0, 0, AllianceEditStyle.FIELD_HEIGHT, Component.empty());
      this.editShortField.setMaxLength(EDIT_MAX_NAME_LENGTH);
      this.editLongField = MultiLineEditBox.builder().build(this.minecraft.font, DEFAULT_MULTILINE_WIDTH, AllianceEditStyle.FIELD_HEIGHT_LONG, Component.empty());
      this.editShortField.visible = false;
      this.editLongField.visible = false;
      addRenderableWidget(this.editShortField);
      addRenderableWidget(this.editLongField);

      this.editShortField.setResponder(value -> updateEditSaveState());
      this.editLongField.setValueListener(value -> updateEditSaveState());
      super.init();
      refreshContent();
   }

   @Override
   protected Drawable buildUi() {
      this.headerBlock = new AllianceDetailHeaderBlock(
              TITLE,
              this::allianceHeaderName,
              REFRESH_LABEL,
              this::refreshAlliance,
              BACK_LABEL,
              this::onClose,
              () -> !refreshing
      );
      this.statusText = new AllianceStatusText(status::message, status::color);

      AllianceDetailTabButton infoTab = new AllianceDetailTabButton(() -> INFO_TAB_LABEL, () -> setActiveTab(Tab.INFO), () -> activeTab == Tab.INFO);
      AllianceDetailTabButton membersTab = new AllianceDetailTabButton(() -> MEMBERS_TAB_LABEL, () -> setActiveTab(Tab.MEMBERS), () -> activeTab == Tab.MEMBERS);
      this.tabBar = new AllianceDetailTabBar(infoTab, membersTab);

      this.infoList = new AllianceInfoListView(this::getAlliance, this::canEditAlliance, () -> !isLocalAlliance(), this::openEditField);

      this.inviteRow = new AllianceInviteRow(
              inviteField,
              this::inviteButtonLabel,
              this::onInviteMember,
              this::canEditAlliance,
              () -> inviting
      );
      this.membersList = new AllianceMembersListView(
              this::getAlliance,
              this::isLocalAlliance,
              this::canEditAlliance,
              this::isCurrentPlayerMember,
              this::onCancelInvite,
              this::onRemoveMember,
              NO_MEMBERS_TEXT,
              PENDING_INVITES_TITLE
      );
      this.membersPanel = new AllianceMembersPanel(inviteRow, membersList, this::shouldShowInviteRow);

      this.tabContent = new AllianceTabContent(infoList, membersPanel, () -> activeTab == Tab.INFO);

      this.actionsRow = new AllianceDetailActionsRow();
      rebuildActions();

      AllianceDetailLayout layout = new AllianceDetailLayout(headerBlock, statusText, tabBar, tabContent, actionsRow);
      this.dialogLayer = new AllianceDialogLayer(layout, this::getActiveDialog);

      return new InsetDrawable(dialogLayer, AllianceDetailStyle.OUTER_PADDING);
   }

   @Override
   protected boolean enableVanillaWidgets() {
      return true;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private Alliance getAlliance() {
      return alliance;
   }

   private Component allianceHeaderName() {
      if (alliance == null) {
         return Component.empty();
      }

      Component label = Component.literal(alliance.getDisplayName());
      Integer rgb = parseAllianceRgb(alliance.color());
      if (rgb == null) {
         return label;
      }

      return label.copy().withStyle(style -> style.withColor(TextColor.fromRgb(rgb)));
   }

   private Integer parseAllianceRgb(String color) {
      if (color == null || color.isBlank()) {
         return null;
      }

      String normalized = color.startsWith("#") ? color.substring(1) : color;
      if (normalized.length() != 6) {
         return null;
      }

      try {
         return Integer.parseInt(normalized, 16);
      } catch (NumberFormatException ignored) {
         return null;
      }
   }

   private void refreshContent() {
      updateMembershipState();
      rebuildActions();
      updateInviteState();
   }

   private void setActiveTab(Tab tab) {
      this.activeTab = tab;
      updateInviteState();
   }

   private void updateMembershipState() {
      String playerUuid = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getStringUUID() : "";
      this.normalizedPlayerUuid = normalizeUuid(playerUuid);
      if (alliance == null) {
         this.currentPlayerMember = null;
         this.isInvited = false;
         return;
      }
      this.currentPlayerMember = alliance.members().stream()
              .filter(member -> normalizeUuid(member.uuid()).equalsIgnoreCase(this.normalizedPlayerUuid))
              .findFirst()
              .orElse(null);
      this.isInvited = currentPlayerMember != null && currentPlayerMember.isInvited();
   }

   private String normalizeUuid(String uuid) {
      return uuid == null ? "" : uuid.replace("-", "");
   }

   private boolean isCurrentPlayerMember(AllianceMember member) {
      if (member == null) {
         return false;
      }
      String memberUuid = normalizeUuid(member.uuid());
      return !memberUuid.isEmpty() && memberUuid.equalsIgnoreCase(normalizedPlayerUuid);
   }

   private boolean canEditAlliance() {
      if (isLocalAlliance()) {
         return true;
      }
      if (currentPlayerMember != null && currentPlayerMember.hasAdminPermissions()) {
         return true;
      }
      if (alliance == null) {
         return false;
      }
      String ownerUuid = normalizeUuid(alliance.ownedBy());
      return !ownerUuid.isEmpty() && ownerUuid.equalsIgnoreCase(normalizedPlayerUuid);
   }

   private boolean isLocalAlliance() {
      return alliance != null && alliance.isLocal();
   }

   private Component inviteButtonLabel() {
      return isLocalAlliance() ? ADD_LABEL : INVITE_LABEL;
   }

   private boolean shouldShowInviteRow() {
      return activeTab == Tab.MEMBERS && canEditAlliance();
   }

   private void openEditField(AllianceEditField field) {
      if (!canEditAlliance()) {
         return;
      }

      this.activeEditField = field;
      this.editSaving = false;
      this.editStatus.clear();
      configureEditField();
      this.editDialog = buildEditDialog();
      updateInviteState();
      setInitialFocus(editUseShortField ? editShortField : editLongField);
   }

   private AllianceEditDialog buildEditDialog() {
      AllianceDetailButton saveButton = AllianceDetailButton.primary(
              () -> CommonComponents.GUI_DONE,
              this::onEditSaveClicked,
              () -> !editSaving && isEditFormValid()
      );
      AllianceDetailButton cancelButton = AllianceDetailButton.secondary(
              () -> CommonComponents.GUI_BACK,
              this::closeEditDialog,
              () -> !editSaving
      );

      return new AllianceEditDialog(
              this::getEditTitleText,
              this::getEditSubtitleText,
              editStatus::message,
              editStatus::color,
              editShortField,
              editLongField,
              () -> editUseShortField,
              saveButton,
              cancelButton
      );
   }

   private void closeEditDialog() {
      this.editDialog = null;
      this.activeEditField = null;
      this.editShortField.visible = false;
      this.editLongField.visible = false;
      setFocused(null);
      updateInviteState();
   }

   private void rebuildActions() {
      List<AllianceDetailButton> buttons = new ArrayList<>();

      if (isLocalAlliance()) {
         buttons.add(AllianceDetailButton.secondary(
                 () -> DELETE_BUTTON,
                 this::onDeleteAlliance,
                 () -> true
         ));
         actionsRow.setButtons(buttons);
         return;
      }

      if (isInvited) {
         buttons.add(AllianceDetailButton.primary(
                 () -> ACCEPT_INVITE_BUTTON,
                 this::onAcceptInvite,
                 () -> true
         ));
         buttons.add(AllianceDetailButton.secondary(
                 () -> REJECT_INVITE_BUTTON,
                 this::onRejectInvite,
                 () -> true
         ));
      } else if (currentPlayerMember != null && currentPlayerMember.isJoined()) {
         buttons.add(AllianceDetailButton.secondary(
                 () -> LEAVE_BUTTON,
                 this::onLeaveAlliance,
                 () -> true
         ));
         if (currentPlayerMember.hasAdminPermissions()) {
            buttons.add(AllianceDetailButton.secondary(
                    () -> DELETE_BUTTON,
                    this::onDeleteAlliance,
                    () -> true
            ));
         }
      }

      actionsRow.setButtons(buttons);
   }

   private void updateInviteState() {
      if (editDialog != null) {
         inviteField.setEditable(false);
         inviteField.setVisible(false);
         return;
      }
      boolean canInvite = canEditAlliance();
      boolean enabled = canInvite && !inviting;
      inviteField.setEditable(enabled);
      inviteField.setVisible(activeTab == Tab.MEMBERS && canInvite);
   }

   private void onInviteMember() {
      if (inviting || !canEditAlliance()) {
         return;
      }

      String input = inviteField.getValue().trim();
      if (input.isEmpty()) {
         status.set(isLocalAlliance() ? ADD_REQUIRED_TEXT : INVITE_REQUIRED_TEXT, AllianceDetailStyle.TEXT_WARNING);
         return;
      }

      setInviting(true);
      PlayerUuidResolver.resolveUuidAsync(input, uuid -> {
         if (uuid == null) {
            this.minecraft.execute(() -> {
               setInviting(false);
               status.set(INVITE_FAILED_TEXT, AllianceDetailStyle.TEXT_ERROR);
            });
            return;
         }

         AllianceManagers.addMember(alliance, uuid.toString(), input)
                 .thenAccept(success -> this.minecraft.execute(() -> {
                    setInviting(false);
                    if (success) {
                       inviteField.setValue("");
                       if (isLocalAlliance()) {
                          MessagingUtils.showMiniMessage("<green>Member added successfully.</green>");
                       } else {
                          MessagingUtils.showMiniMessage("<green>Invitation sent successfully.</green>");
                       }
                       refreshAlliance();
                    } else {
                       status.set(isLocalAlliance() ? ADD_FAILED_TEXT : INVITE_FAILED_TEXT, AllianceDetailStyle.TEXT_ERROR);
                    }
                 }));
      });
   }

   private void setInviting(boolean inviting) {
      this.inviting = inviting;
      updateInviteState();
   }

   private void onAcceptInvite() {
      AllianceManagers.acceptInvitation(alliance).thenAccept(success -> {
         this.minecraft.execute(() -> {
            if (success) {
               MessagingUtils.showMiniMessage("<green>You have joined the alliance!</green>");
               refreshAlliance();
            } else {
               MessagingUtils.showMiniMessage("<red>Failed to accept invitation.</red>");
            }
         });
      });
   }

   private void onRejectInvite() {
      AllianceManagers.rejectInvitation(alliance).thenAccept(success -> {
         this.minecraft.execute(() -> {
            if (success) {
               MessagingUtils.showMiniMessage("<yellow>Invitation declined.</yellow>");
               this.minecraft.setScreen(this.lastScreen);
            } else {
               MessagingUtils.showMiniMessage("<red>Failed to decline invitation.</red>");
            }
         });
      });
   }

   private void onLeaveAlliance() {
      if (currentPlayerMember == null) {
         return;
      }

      Component confirmMessage = Component.translatable("lsu.alliances.detail.leave_confirm", alliance.name());
      showConfirmDialog(
              Component.translatable("lsu.alliances.detail.leave_title"),
              confirmMessage,
              () -> AllianceManagers.removeMember(alliance, currentPlayerMember.id()).thenAccept(success -> {
                 this.minecraft.execute(() -> {
                    if (success) {
                       MessagingUtils.showMiniMessage("<green>You have left the alliance.</green>");
                       this.minecraft.setScreen(this.lastScreen);
                    } else {
                       MessagingUtils.showMiniMessage("<red>Failed to leave alliance.</red>");
                    }
                 });
              })
      );
   }

   private void onDeleteAlliance() {
      Component confirmMessage = Component.translatable("lsu.alliances.detail.delete_confirm", alliance.name());
      showConfirmDialog(
              Component.translatable("lsu.alliances.detail.delete_title"),
              confirmMessage,
              () -> AllianceManagers.deleteAlliance(alliance).thenAccept(success -> {
                 this.minecraft.execute(() -> {
                    if (success) {
                       MessagingUtils.showMiniMessage("<green>Alliance deleted successfully.</green>");
                       this.minecraft.setScreen(this.lastScreen);
                    } else {
                       MessagingUtils.showMiniMessage("<red>Failed to delete alliance.</red>");
                    }
                 });
              })
      );
   }

   private void refreshAlliance() {
      setRefreshing(true);
      AllianceManagers.fetchAlliance(alliance).thenAccept(updated -> {
         this.minecraft.execute(() -> {
            setRefreshing(false);
            if (updated != null) {
               this.alliance = updated;
               refreshContent();
            } else {
               status.set(REFRESH_FAILED_TEXT, AllianceDetailStyle.TEXT_ERROR);
            }
         });
      }).exceptionally(error -> {
         this.minecraft.execute(() -> {
            setRefreshing(false);
            status.set(REFRESH_FAILED_TEXT, AllianceDetailStyle.TEXT_ERROR);
         });
         return null;
      });
   }

   private void setRefreshing(boolean refreshing) {
      this.refreshing = refreshing;
      if (refreshing) {
         status.set(REFRESHING_TEXT, AllianceDetailStyle.TEXT_WARNING);
      } else {
         status.clear();
      }
   }

   private void onCancelInvite(AllianceMember member) {
      Component confirmMessage = Component.translatable("lsu.alliances.detail.cancel_confirm", member.cachedName());
      showConfirmDialog(
              Component.translatable("lsu.alliances.detail.cancel_title"),
              confirmMessage,
              () -> removeMember(member, CANCEL_FAILED_TEXT)
      );
   }

   private void onRemoveMember(AllianceMember member) {
      Component confirmMessage = Component.translatable("lsu.alliances.detail.remove_confirm", member.cachedName());
      showConfirmDialog(
              Component.translatable("lsu.alliances.detail.remove_title"),
              confirmMessage,
              () -> removeMember(member, REMOVE_FAILED_TEXT)
      );
   }

   private void removeMember(AllianceMember member, Component failureText) {
      if (!canEditAlliance()) {
         return;
      }

      AllianceManagers.removeMember(alliance, member.id()).thenAccept(success -> {
         this.minecraft.execute(() -> {
            if (success) {
               refreshAlliance();
            } else {
               status.set(failureText, AllianceDetailStyle.TEXT_ERROR);
            }
         });
      });
   }

   private void showConfirmDialog(Component title, Component message, Runnable onConfirm) {
      closeEditDialog();
      this.confirmDialog = new AllianceConfirmDialog(
              title,
              message,
              Component.translatable("gui.yes"),
              () -> {
                 confirmDialog = null;
                 onConfirm.run();
              },
              Component.translatable("gui.no"),
              () -> confirmDialog = null
      );
   }

   private Drawable getActiveDialog() {
      if (editDialog != null) {
         return editDialog;
      }
      return confirmDialog;
   }

   private void configureEditField() {
      if (activeEditField == null || alliance == null) {
         editUseShortField = true;
         editShortField.setValue("");
         editLongField.setValue("");
         return;
      }

      switch (activeEditField) {
         case NAME -> {
            editUseShortField = true;
            editShortField.setMaxLength(EDIT_MAX_NAME_LENGTH);
            editShortField.setValue(alliance.name());
         }
         case PREFIX -> {
            editUseShortField = true;
            editShortField.setMaxLength(EDIT_MAX_PREFIX_LENGTH);
            editShortField.setValue(alliance.prefix() == null ? "" : alliance.prefix());
         }
         case COLOR -> {
            editUseShortField = true;
            editShortField.setMaxLength(EDIT_MAX_COLOR_LENGTH);
            editShortField.setValue(alliance.color() == null ? "" : alliance.color());
         }
         case DESCRIPTION -> {
            editUseShortField = false;
            editLongField.setCharacterLimit(EDIT_MAX_DESCRIPTION_LENGTH);
            editLongField.setValue(alliance.description());
         }
         case MOTD -> {
            editUseShortField = false;
            editLongField.setCharacterLimit(EDIT_MAX_MOTD_LENGTH);
            editLongField.setValue(alliance.motd());
         }
      }
   }

   private Component getEditTitleText() {
      if (activeEditField == null) {
         return Component.empty();
      }
      return switch (activeEditField) {
         case NAME -> Component.translatable("lsu.alliances.detail.edit.name.title");
         case PREFIX -> Component.translatable("lsu.alliances.detail.edit.prefix.title");
         case COLOR -> Component.translatable("lsu.alliances.detail.edit.color.title");
         case DESCRIPTION -> Component.translatable("lsu.alliances.detail.edit.description.title");
         case MOTD -> Component.translatable("lsu.alliances.detail.edit.motd.title");
      };
   }

   private Component getEditSubtitleText() {
      if (activeEditField == null) {
         return Component.empty();
      }
      return switch (activeEditField) {
         case NAME -> Component.translatable("lsu.alliances.detail.edit.name.subtitle");
         case PREFIX -> Component.translatable("lsu.alliances.detail.edit.prefix.subtitle");
         case COLOR -> Component.translatable("lsu.alliances.detail.edit.color.subtitle");
         case DESCRIPTION -> Component.translatable("lsu.alliances.detail.edit.description.subtitle");
         case MOTD -> Component.translatable("lsu.alliances.detail.edit.motd.subtitle");
      };
   }

   private void updateEditSaveState() {
      if (isEditFormValid()) {
         editStatus.clear();
      }
   }

   private boolean isEditFormValid() {
      String value = getEditValue();
      if (activeEditField == AllianceEditField.PREFIX) {
         return value != null;
      }
      if (activeEditField == AllianceEditField.COLOR) {
         return value != null;
      }
      return value != null && !value.trim().isEmpty();
   }

   private String getEditValue() {
      return editUseShortField ? editShortField.getValue() : editLongField.getValue();
   }

   private void onEditSaveClicked() {
      if (editSaving || alliance == null || activeEditField == null) {
         return;
      }

      if (!isEditFormValid()) {
         editStatus.set(Component.translatable("lsu.alliances.detail.edit.required"), AlliancesListStyle.TEXT_ERROR);
         return;
      }

      String rawValue = getEditValue();
      String value = rawValue == null ? "" : rawValue.trim();
      setEditSaving(true);

      String name = null;
      String prefix = null;
      String color = null;
      String description = null;
      String motd = null;

      switch (activeEditField) {
         case NAME -> name = value;
         case PREFIX -> prefix = value;
         case COLOR -> color = value;
         case DESCRIPTION -> description = value;
         case MOTD -> motd = value;
      }

      AllianceManagers.updateAlliance(alliance, name, prefix, color, description, motd).thenAccept(updated -> {
         this.minecraft.execute(() -> handleEditUpdateResult(updated));
      });
   }

   private void handleEditUpdateResult(Alliance updated) {
      setEditSaving(false);
      if (updated != null) {
         MessagingUtils.showMiniMessage("<green>Alliance updated successfully.</green>");
         this.alliance = updated;
         refreshContent();
         closeEditDialog();
      } else {
         MessagingUtils.showMiniMessage("<red>Failed to update alliance.</red>");
         editStatus.set(Component.translatable("lsu.alliances.detail.edit.failed"), AlliancesListStyle.TEXT_ERROR);
      }
   }

   private void setEditSaving(boolean saving) {
      this.editSaving = saving;
      if (saving) {
         editStatus.set(Component.translatable("lsu.alliances.detail.edit.saving"), AlliancesListStyle.TEXT_WARNING);
      }
   }

   private static final class AllianceStatusText implements Drawable {
      private final Supplier<Component> textSupplier;
      private final IntSupplier colorSupplier;

      private UiBounds bounds = UiBounds.empty();

      private AllianceStatusText(Supplier<Component> textSupplier, IntSupplier colorSupplier) {
         this.textSupplier = textSupplier;
         this.colorSupplier = colorSupplier;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         this.bounds = layoutContext.availableBounds();
      }

      @Override
      public void render(UiContext context) {
         Component text = textSupplier.get();
         if (text == null || text.getString().isEmpty()) {
            return;
         }

         Font font = context.minecraft().font;
         int textX = bounds.x();
         int textY = bounds.y() + (bounds.height() - font.lineHeight) / 2;
         context.graphics().drawString(font, text, textX, textY, colorSupplier.getAsInt(), true);
      }

      @Override
      public void handleInput(UiInputState input) {
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         return new UiSize(layoutContext.availableBounds().width(), AllianceDetailStyle.STATUS_HEIGHT);
      }
   }

   private static final class AllianceTabContent implements Drawable {
      private final Drawable infoPanel;
      private final Drawable membersPanel;
      private final BooleanSupplier infoActiveSupplier;

      private UiBounds bounds = UiBounds.empty();

      private AllianceTabContent(Drawable infoPanel, Drawable membersPanel, BooleanSupplier infoActiveSupplier) {
         this.infoPanel = infoPanel;
         this.membersPanel = membersPanel;
         this.infoActiveSupplier = infoActiveSupplier;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         this.bounds = layoutContext.availableBounds();
         infoPanel.layout(layoutContext.withBounds(bounds));
         membersPanel.layout(layoutContext.withBounds(bounds));
      }

      @Override
      public void render(UiContext context) {
         if (infoActiveSupplier.getAsBoolean()) {
            infoPanel.render(context);
         } else {
            membersPanel.render(context);
         }
      }

      @Override
      public void handleInput(UiInputState input) {
         if (infoActiveSupplier.getAsBoolean()) {
            infoPanel.handleInput(input);
         } else {
            membersPanel.handleInput(input);
         }
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         return new UiSize(layoutContext.availableBounds().width(), layoutContext.availableBounds().height());
      }
   }

   private static final class AllianceDetailActionsRow implements Drawable {
      private final List<AllianceDetailButton> buttons = new ArrayList<>();

      private UiBounds bounds = UiBounds.empty();

      private void setButtons(List<AllianceDetailButton> newButtons) {
         buttons.clear();
         buttons.addAll(newButtons);
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         this.bounds = layoutContext.availableBounds();

         int totalWidth = 0;
         for (AllianceDetailButton button : buttons) {
            totalWidth += button.preferredSize(layoutContext).width();
         }
         int totalGap = Math.max(buttons.size() - 1, 0) * AllianceDetailStyle.TAB_GAP;
         totalWidth += totalGap;

         int cursorX = bounds.x() + (bounds.width() - totalWidth) / 2;
         int buttonY = bounds.y() + (bounds.height() - AllianceDetailStyle.BUTTON_HEIGHT) / 2;
         for (AllianceDetailButton button : buttons) {
            UiSize size = button.preferredSize(layoutContext);
            button.layout(layoutContext.withBounds(new UiBounds(cursorX, buttonY, size.width(), size.height())));
            cursorX += size.width() + AllianceDetailStyle.TAB_GAP;
         }
      }

      @Override
      public void render(UiContext context) {
         for (AllianceDetailButton button : buttons) {
            button.render(context);
         }
      }

      @Override
      public void handleInput(UiInputState input) {
         for (AllianceDetailButton button : buttons) {
            button.handleInput(input);
         }
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         return new UiSize(layoutContext.availableBounds().width(), AllianceDetailStyle.ACTION_ROW_HEIGHT);
      }
   }

   private static final class AllianceDetailLayout implements Drawable {
      private final Drawable header;
      private final Drawable status;
      private final Drawable tabs;
      private final Drawable content;
      private final Drawable actions;

      private UiBounds bounds = UiBounds.empty();

      private AllianceDetailLayout(Drawable header, Drawable status, Drawable tabs, Drawable content, Drawable actions) {
         this.header = header;
         this.status = status;
         this.tabs = tabs;
         this.content = content;
         this.actions = actions;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         this.bounds = layoutContext.availableBounds();
         int width = bounds.width();

         int headerHeight = header.preferredSize(layoutContext).height();
         int statusHeight = status.preferredSize(layoutContext).height();
         int tabsHeight = tabs.preferredSize(layoutContext).height();
         int actionsHeight = actions.preferredSize(layoutContext).height();

         int contentHeight = Math.max(
                 bounds.height() - headerHeight - statusHeight - tabsHeight - actionsHeight - AllianceDetailStyle.SECTION_GAP * 4,
                 0
         );

         int cursorY = bounds.y();
         UiBounds headerBounds = new UiBounds(bounds.x(), cursorY, width, headerHeight);
         cursorY += headerHeight + AllianceDetailStyle.SECTION_GAP;

         UiBounds statusBounds = new UiBounds(bounds.x(), cursorY, width, statusHeight);
         cursorY += statusHeight + AllianceDetailStyle.SECTION_GAP;

         UiBounds tabsBounds = new UiBounds(bounds.x(), cursorY, width, tabsHeight);
         cursorY += tabsHeight + AllianceDetailStyle.SECTION_GAP;

         UiBounds contentBounds = new UiBounds(bounds.x(), cursorY, width, contentHeight);
         cursorY += contentHeight + AllianceDetailStyle.SECTION_GAP;

         UiBounds actionsBounds = new UiBounds(bounds.x(), cursorY, width, actionsHeight);

         header.layout(layoutContext.withBounds(headerBounds));
         status.layout(layoutContext.withBounds(statusBounds));
         tabs.layout(layoutContext.withBounds(tabsBounds));
         content.layout(layoutContext.withBounds(contentBounds));
         actions.layout(layoutContext.withBounds(actionsBounds));
      }

      @Override
      public void render(UiContext context) {
         header.render(context);
         status.render(context);
         tabs.render(context);
         content.render(context);
         actions.render(context);
      }

      @Override
      public void handleInput(UiInputState input) {
         header.handleInput(input);
         status.handleInput(input);
         tabs.handleInput(input);
         content.handleInput(input);
         actions.handleInput(input);
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         return new UiSize(layoutContext.availableBounds().width(), layoutContext.availableBounds().height());
      }
   }

   private static final class InsetDrawable implements Drawable {
      private final Drawable child;
      private final int inset;

      private UiBounds bounds = UiBounds.empty();

      private InsetDrawable(Drawable child, int inset) {
         this.child = child;
         this.inset = Math.max(inset, 0);
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         UiBounds available = layoutContext.availableBounds();
         bounds = available;
         int innerWidth = Math.max(available.width() - inset * 2, 0);
         int innerHeight = Math.max(available.height() - inset * 2, 0);
         UiBounds innerBounds = new UiBounds(available.x() + inset, available.y() + inset, innerWidth, innerHeight);
         child.layout(layoutContext.withBounds(innerBounds));
      }

      @Override
      public void render(UiContext context) {
         child.render(context);
      }

      @Override
      public void handleInput(UiInputState input) {
         child.handleInput(input);
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         return new UiSize(layoutContext.availableBounds().width(), layoutContext.availableBounds().height());
      }
   }
}
