package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.ui.framework.screens.DrawableScreen;
import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceType;
import dev.candycup.lifestealutils.features.alliances.service.AllianceManagers;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * displays a list of alliances and invitations using the drawable ui system.
 */
public class AlliancesListScreen extends DrawableScreen {
   private static final Component TITLE = Component.translatable("lsu.alliances.list.title");
   private static final Component LOADING_TEXT = Component.translatable("lsu.alliances.loading");
   private static final Component ERROR_TEXT = Component.translatable("lsu.alliances.error");
   private static final Component EMPTY_TEXT = Component.translatable("lsu.alliances.empty");
   private static final Component INVITES_TITLE = Component.translatable("lsu.alliances.invites");
   private static final Component ALLIANCES_TITLE = Component.translatable("lsu.alliances.your_alliances");
   private static final Component REFRESH_LABEL = Component.translatable("lsu.alliances.refresh");
   private static final Component CREATE_LABEL = Component.translatable("lsu.alliances.create");
   private static final Component CREATE_SUBMIT_LABEL = Component.translatable("lsu.alliances.create.submit");
   private static final Component CANCEL_LABEL = Component.translatable("gui.cancel");
   private static final Component CREATING_TEXT = Component.translatable("lsu.alliances.creating");
   private static final Component REQUIRED_TEXT = Component.translatable("lsu.alliances.create.error.required");
   private static final Component FAILED_TEXT = Component.translatable("lsu.alliances.create.error.failed");
   private static final Component NAME_HINT = Component.translatable("lsu.alliances.create.hint.name");
   private static final Component PREFIX_HINT = Component.translatable("lsu.alliances.create.prefix");
   private static final Component COLOR_HINT = Component.translatable("lsu.alliances.create.hint.color");
   private static final Component INVITE_BADGE = Component.translatable("lsu.alliances.invite_badge");
   private static final Component LOCAL_ONLY_BADGE = Component.translatable("lsu.alliances.local_only");

   private static final int MAX_NAME_LENGTH = 30;
   private static final int MAX_PREFIX_LENGTH = 30;
   private static final int MAX_COLOR_LENGTH = 7;
   private static final int MAX_DESCRIPTION_LENGTH = 200;
   private static final int MAX_MOTD_LENGTH = 300;
   private static final int DEFAULT_MULTILINE_WIDTH = AllianceEditStyle.PANEL_WIDTH - AllianceEditStyle.PANEL_PADDING * 2;

   private final Screen lastScreen;
   private final List<Alliance> alliances = new ArrayList<>();

   private boolean loading = true;
   private boolean initialFetchDone;
   private Component statusText = LOADING_TEXT;
   private int statusColor = AlliancesListStyle.TEXT_WARNING;

   private AlliancesHeaderBlock headerBlock;
   private AlliancesFooterBlock footerBlock;
   private AlliancesListView listView;
   private AllianceDialogLayer dialogLayer;

   private EditBox nameField;
   private EditBox prefixField;
   private EditBox colorField;
   private MultiLineEditBox descriptionField;
   private MultiLineEditBox motdField;
   private AllianceCreateDialog createDialog;
   private AllianceTypeDialog allianceTypeDialog;
   private AllianceDetailButton createDialogSubmitButton;
   private AllianceDetailButton createDialogCancelButton;
   private boolean typeDialogOpen;
   private boolean createDialogOpen;
   private boolean creating;
   private AllianceType createAllianceType = AllianceType.MODERN;
   private final AllianceStatusState createStatus = new AllianceStatusState(AlliancesListStyle.TEXT_MUTED);

   /**
    * creates a new alliances list screen.
    *
    * @param lastScreen the screen to return to when closing
    */
   public AlliancesListScreen(Screen lastScreen) {
      super(TITLE);
      this.lastScreen = lastScreen;
   }

   @Override
   protected Drawable buildUi() {
      this.headerBlock = new AlliancesHeaderBlock(
              TITLE,
              REFRESH_LABEL,
              this::refreshAlliances,
              () -> !loading
      );
      this.footerBlock = new AlliancesFooterBlock(
              CREATE_LABEL,
              this::onCreateAlliance,
              () -> !loading
      );
      this.listView = new AlliancesListView(
              () -> alliances,
              () -> loading,
              () -> statusText,
              () -> statusColor,
              this::onSelectAlliance,
              this::onAcceptInvite,
              this::onRejectInvite,
              EMPTY_TEXT,
              INVITES_TITLE,
              ALLIANCES_TITLE,
              INVITE_BADGE,
              LOCAL_ONLY_BADGE
      );

      AlliancesColumnLayout columnLayout = new AlliancesColumnLayout(
              headerBlock,
              listView,
              footerBlock,
              AlliancesListStyle.SECTION_GAP
      );

      this.createDialogSubmitButton = AllianceDetailButton.primary(
              () -> CREATE_SUBMIT_LABEL,
              this::onCreateDialogSubmit,
              () -> !creating && isCreateFormValid()
      );
      this.createDialogCancelButton = AllianceDetailButton.secondary(
              () -> CANCEL_LABEL,
              this::closeCreateDialog,
              () -> !creating
      );
      this.createDialog = new AllianceCreateDialog(
              createStatus::message,
              createStatus::color,
              nameField,
              prefixField,
              colorField,
              descriptionField,
              motdField,
              createDialogSubmitButton,
              createDialogCancelButton,
              () -> createAllianceType == AllianceType.MODERN
      );

      this.allianceTypeDialog = new AllianceTypeDialog(
              () -> onCreateTypeSelected(AllianceType.LOCAL),
              () -> onCreateTypeSelected(AllianceType.MODERN),
              this::canCreateModernAlliance
      );

      this.dialogLayer = new AllianceDialogLayer(
              new InsetDrawable(columnLayout, AlliancesListStyle.OUTER_PADDING),
              this::getActiveDialog
      );
      return dialogLayer;
   }

   @Override
   protected void init() {
      AllianceCreateFormFactory.AllianceCreateFormFields fields = AllianceCreateFormFactory.create(
              this.minecraft.font,
              MAX_NAME_LENGTH,
              NAME_HINT,
              MAX_PREFIX_LENGTH,
              PREFIX_HINT,
              MAX_COLOR_LENGTH,
              COLOR_HINT,
              DEFAULT_MULTILINE_WIDTH,
              MAX_DESCRIPTION_LENGTH,
              MAX_MOTD_LENGTH,
              this::updateCreateState
      );
      this.nameField = fields.nameField();
      this.prefixField = fields.prefixField();
      this.colorField = fields.colorField();
      this.descriptionField = fields.descriptionField();
      this.motdField = fields.motdField();

      addRenderableWidget(this.nameField);
      addRenderableWidget(this.prefixField);
      addRenderableWidget(this.colorField);
      addRenderableWidget(this.descriptionField);
      addRenderableWidget(this.motdField);

      setCreateFieldsVisible(false);
      updateCreateState();

      super.init();
      if (!initialFetchDone) {
         initialFetchDone = true;
         refreshAlliances();
      }
   }

   @Override
   protected boolean enableVanillaWidgets() {
      return true;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void refreshAlliances() {
      setLoadingState(true, LOADING_TEXT, AlliancesListStyle.TEXT_WARNING);
      CompletableFuture<List<Alliance>> fetch = AllianceManagers.fetchPlayerAlliances();
      fetch.thenAccept(fetchedAlliances -> this.minecraft.execute(() -> {
         this.alliances.clear();
         this.alliances.addAll(fetchedAlliances);
         setLoadingState(false, Component.empty(), AlliancesListStyle.TEXT_MUTED);
      })).exceptionally(error -> {
         this.minecraft.execute(() -> {
            this.alliances.clear();
            setLoadingState(false, ERROR_TEXT, AlliancesListStyle.TEXT_ERROR);
         });
         return null;
      });
   }

   private void setLoadingState(boolean loading, Component status, int statusColor) {
      this.loading = loading;
      this.statusText = status;
      this.statusColor = statusColor;
   }

   private void onCreateAlliance() {
      openTypeDialog();
   }

   private Drawable getActiveDialog() {
      if (typeDialogOpen) {
         return allianceTypeDialog;
      }
      if (!createDialogOpen) {
         return null;
      }
      return createDialog;
   }

   private void openTypeDialog() {
      this.typeDialogOpen = true;
      this.createDialogOpen = false;
      setCreateFieldsVisible(false);
      setFocused(null);
   }

   private void onCreateTypeSelected(AllianceType type) {
      if (type == AllianceType.MODERN && !canCreateModernAlliance()) {
         return;
      }
      this.typeDialogOpen = false;
      openCreateDialog(type);
   }

   private boolean canCreateModernAlliance() {
      return Config.isGaiaAdvancedFeaturesEnabled();
   }

   private void openCreateDialog(AllianceType type) {
      if (creating) {
         return;
      }
      this.createAllianceType = type == null ? AllianceType.MODERN : type;
      this.createDialogOpen = true;
      if (this.createDialog != null) {
         this.createDialog.resetScroll();
      }
      this.creating = false;
      this.nameField.setValue("");
      this.prefixField.setValue("");
      this.colorField.setValue("");
      this.descriptionField.setValue("");
      this.motdField.setValue("");
      this.createStatus.clear();
      setCreateFieldsVisible(true);
      setInitialFocus(nameField);
   }

   private void closeCreateDialog() {
      this.createDialogOpen = false;
      setCreateFieldsVisible(false);
      setFocused(null);
   }

   private void setCreateFieldsVisible(boolean visible) {
      this.nameField.visible = visible;
      this.prefixField.visible = visible;
      this.colorField.visible = visible;
      this.descriptionField.visible = visible && createAllianceType == AllianceType.MODERN;
      this.motdField.visible = visible && createAllianceType == AllianceType.MODERN;

      this.nameField.setEditable(visible && !creating);
      this.prefixField.setEditable(visible && !creating);
      this.colorField.setEditable(visible && !creating);
   }

   private void updateCreateState() {
      if (isCreateFormValid()) {
         createStatus.clear();
      }
   }

   private boolean isCreateFormValid() {
      if (createAllianceType == AllianceType.LOCAL) {
         return !nameField.getValue().trim().isEmpty();
      }
      return !nameField.getValue().trim().isEmpty() && !descriptionField.getValue().trim().isEmpty() && !motdField.getValue().trim().isEmpty();
   }

   private void onCreateDialogSubmit() {
      if (creating || !createDialogOpen) {
         return;
      }

      if (!isCreateFormValid()) {
         createStatus.set(REQUIRED_TEXT, AlliancesListStyle.TEXT_ERROR);
         return;
      }

      String name = nameField.getValue().trim();
      String prefix = prefixField.getValue().trim();
      String color = colorField.getValue().trim();
      String description = descriptionField.getValue().trim();
      String motd = motdField.getValue().trim();

      String resolvedDescription = createAllianceType == AllianceType.MODERN ? description : "";
      String resolvedMotd = createAllianceType == AllianceType.MODERN ? motd : "";

      setCreatingState(true);
      AllianceManagers.createAlliance(createAllianceType, name, prefix.isEmpty() ? null : prefix, color.isEmpty() ? null : color, resolvedDescription, resolvedMotd)
              .thenAccept(alliance -> this.minecraft.execute(() -> handleCreateResult(alliance)));
   }

   private void handleCreateResult(Alliance alliance) {
      setCreatingState(false);
      if (alliance != null) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.create.success"));
         this.minecraft.setScreen(new AllianceDetailScreen(this, alliance));
         return;
      }

      MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.create.failure"));
      createStatus.set(FAILED_TEXT, AlliancesListStyle.TEXT_ERROR);
   }

   private void setCreatingState(boolean creating) {
      this.creating = creating;
      this.nameField.setEditable(createDialogOpen && !creating);
      this.prefixField.setEditable(createDialogOpen && !creating);
      this.colorField.setEditable(createDialogOpen && !creating);

      if (creating) {
         createStatus.set(CREATING_TEXT, AlliancesListStyle.TEXT_WARNING);
      } else {
         updateCreateState();
      }
   }

   private void onSelectAlliance(Alliance alliance) {
      this.minecraft.setScreen(new AllianceDetailScreen(this, alliance));
   }

   private void onAcceptInvite(Alliance alliance) {
      AllianceManagers.acceptInvitation(alliance).thenAccept(success -> this.minecraft.execute(() -> {
         if (success) {
            MessagingUtils.showMiniMessage("<green>Invitation accepted.</green>");
            refreshAlliances();
         } else {
            MessagingUtils.showMiniMessage("<red>Failed to accept invitation.</red>");
         }
      }));
   }

   private void onRejectInvite(Alliance alliance) {
      AllianceManagers.rejectInvitation(alliance).thenAccept(success -> this.minecraft.execute(() -> {
         if (success) {
            MessagingUtils.showMiniMessage("<yellow>Invitation declined.</yellow>");
            refreshAlliances();
         } else {
            MessagingUtils.showMiniMessage("<red>Failed to decline invitation.</red>");
         }
      }));
   }

   private static final class AlliancesActionButton implements Drawable {
      private final boolean primary;
      private final Supplier<Component> labelSupplier;
      private final Runnable onClick;
      private final BooleanSupplier enabledSupplier;

      private UiBounds bounds = UiBounds.empty();
      private boolean hovered;
      private boolean pressed;

      private AlliancesActionButton(
              boolean primary,
              Supplier<Component> labelSupplier,
              Runnable onClick,
              BooleanSupplier enabledSupplier
      ) {
         this.primary = primary;
         this.labelSupplier = labelSupplier;
         this.onClick = onClick;
         this.enabledSupplier = enabledSupplier;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         Component label = labelSupplier.get();
         int textWidth = layoutContext.font().width(label);
         int textHeight = layoutContext.font().lineHeight;
         int width = Math.max(textWidth + AlliancesListStyle.BUTTON_PADDING_X * 2, AlliancesListStyle.BUTTON_MIN_WIDTH);
         int height = textHeight + AlliancesListStyle.BUTTON_PADDING_Y * 2;
         UiBounds available = layoutContext.availableBounds();
         this.bounds = new UiBounds(available.x(), available.y(), width, height);
      }

      @Override
      public void render(UiContext context) {
         boolean enabled = enabledSupplier.getAsBoolean();
         int textColor = AllianceButtonUiSupport.resolveTextColor(
                 enabled,
                 hovered,
                 AlliancesListStyle.BUTTON_TEXT,
                 AlliancesListStyle.BUTTON_TEXT_HOVER,
                 AlliancesListStyle.BUTTON_TEXT_DISABLED
         );
         context.graphics().blitSprite(
                 RenderPipelines.GUI_TEXTURED,
                 (primary ? AlliancesListStyle.BUTTON_PRIMARY_SPRITES : AlliancesListStyle.BUTTON_SECONDARY_SPRITES).get(enabled, hovered),
                 bounds.x(),
                 bounds.y(),
                 bounds.width(),
                 bounds.height()
         );

         Component label = labelSupplier.get();
         AllianceButtonUiSupport.drawCenteredLabel(context, bounds, label, textColor);
      }

      @Override
      public void handleInput(UiInputState input) {
         boolean enabled = enabledSupplier.getAsBoolean();
         hovered = AllianceButtonUiSupport.resolveHovered(input, bounds, enabled);
         if (!enabled) {
            pressed = false;
            return;
         }
         pressed = AllianceButtonUiSupport.resolvePressed(input, pressed, hovered, onClick);
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         Component label = labelSupplier.get();
         int textWidth = layoutContext.font().width(label);
         int textHeight = layoutContext.font().lineHeight;
         return new UiSize(Math.max(textWidth + AlliancesListStyle.BUTTON_PADDING_X * 2, AlliancesListStyle.BUTTON_MIN_WIDTH),
                 textHeight + AlliancesListStyle.BUTTON_PADDING_Y * 2);
      }
   }

   private static final class AlliancesHeaderBlock implements Drawable {
      private final Component title;
      private final AlliancesActionButton refreshButton;

      private UiBounds bounds = UiBounds.empty();
      private UiBounds titleBounds = UiBounds.empty();

      private AlliancesHeaderBlock(Component title, Component refreshLabel, Runnable onRefresh, BooleanSupplier enabledSupplier) {
         this.title = title;
         this.refreshButton = new AlliancesActionButton(
                 false,
                 () -> refreshLabel,
                 onRefresh,
                 enabledSupplier
         );
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         this.bounds = layoutContext.availableBounds();
         Font font = layoutContext.font();
         int titleWidth = font.width(title);
         int titleHeight = font.lineHeight;
         int titleX = bounds.x() + AlliancesListStyle.HEADER_PADDING_X;
         int titleY = bounds.y() + (bounds.height() - titleHeight) / 2;
         this.titleBounds = new UiBounds(titleX, titleY, titleWidth, titleHeight);

         UiSize buttonSize = refreshButton.preferredSize(layoutContext);
         int buttonX = bounds.x() + bounds.width() - AlliancesListStyle.HEADER_PADDING_X - buttonSize.width();
         int buttonY = bounds.y() + (bounds.height() - buttonSize.height()) / 2;
         refreshButton.layout(layoutContext.withBounds(new UiBounds(buttonX, buttonY, buttonSize.width(), buttonSize.height())));
      }

      @Override
      public void render(UiContext context) {
         AlliancesListStyle.renderHeaderBackgroundSprite(context.graphics(), bounds);
         context.graphics().drawString(context.minecraft().font, title, titleBounds.x(), titleBounds.y(), AlliancesListStyle.TEXT_PRIMARY, true);
         refreshButton.render(context);
      }

      @Override
      public void handleInput(UiInputState input) {
         refreshButton.handleInput(input);
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         return new UiSize(layoutContext.availableBounds().width(), AlliancesListStyle.HEADER_HEIGHT);
      }
   }

   private static final class AlliancesFooterBlock implements Drawable {
      private final AlliancesActionButton createButton;

      private UiBounds bounds = UiBounds.empty();

      private AlliancesFooterBlock(Component createLabel, Runnable onCreate, BooleanSupplier enabledSupplier) {
         this.createButton = new AlliancesActionButton(
                 true,
                 () -> createLabel,
                 onCreate,
                 enabledSupplier
         );
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         this.bounds = layoutContext.availableBounds();
         UiSize buttonSize = createButton.preferredSize(layoutContext);
         int buttonX = bounds.x() + (bounds.width() - buttonSize.width()) / 2;
         int buttonY = bounds.y() + (bounds.height() - buttonSize.height()) / 2;
         createButton.layout(layoutContext.withBounds(new UiBounds(buttonX, buttonY, buttonSize.width(), buttonSize.height())));
      }

      @Override
      public void render(UiContext context) {
         createButton.render(context);
      }

      @Override
      public void handleInput(UiInputState input) {
         createButton.handleInput(input);
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         return new UiSize(layoutContext.availableBounds().width(), AlliancesListStyle.FOOTER_HEIGHT + AlliancesListStyle.FOOTER_PADDING * 2);
      }
   }

   private static final class AlliancesColumnLayout implements Drawable {
      private final Drawable header;
      private final Drawable content;
      private final Drawable footer;
      private final int gap;

      private UiBounds bounds = UiBounds.empty();

      private AlliancesColumnLayout(Drawable header, Drawable content, Drawable footer, int gap) {
         this.header = header;
         this.content = content;
         this.footer = footer;
         this.gap = gap;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         this.bounds = layoutContext.availableBounds();
         int width = bounds.width();
         int headerHeight = header.preferredSize(layoutContext).height();
         int footerHeight = footer.preferredSize(layoutContext).height();
         int contentHeight = Math.max(bounds.height() - headerHeight - footerHeight - gap * 2, 0);

         UiBounds headerBounds = new UiBounds(bounds.x(), bounds.y(), width, headerHeight);
         UiBounds contentBounds = new UiBounds(bounds.x(), bounds.y() + headerHeight + gap, width, contentHeight);
         UiBounds footerBounds = new UiBounds(bounds.x(), contentBounds.y() + contentHeight + gap, width, footerHeight);

         header.layout(layoutContext.withBounds(headerBounds));
         content.layout(layoutContext.withBounds(contentBounds));
         footer.layout(layoutContext.withBounds(footerBounds));
      }

      @Override
      public void render(UiContext context) {
         header.render(context);
         content.render(context);
         footer.render(context);
      }

      @Override
      public void handleInput(UiInputState input) {
         header.handleInput(input);
         content.handleInput(input);
         footer.handleInput(input);
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
         UiBounds inner = new UiBounds(available.x() + inset, available.y() + inset, innerWidth, innerHeight);
         child.layout(layoutContext.withBounds(inner));
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
