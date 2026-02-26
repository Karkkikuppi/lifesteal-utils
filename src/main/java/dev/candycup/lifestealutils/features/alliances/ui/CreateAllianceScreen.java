package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.ui.framework.screens.DrawableScreen;
import dev.candycup.lifestealutils.gaia.AlliancesAPIClient;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * screen for creating a new alliance using the drawable ui system.
 */
public class CreateAllianceScreen extends DrawableScreen {
   private static final Component TITLE = Component.translatable("lsu.alliances.create.title");
   private static final Component SUBTITLE = Component.translatable("lsu.alliances.create.subtitle");
   private static final Component NAME_LABEL = Component.translatable("lsu.alliances.create.name");
   private static final Component PREFIX_LABEL = Component.translatable("lsu.alliances.create.prefix");
   private static final Component PREFIX_HINT = Component.translatable("lsu.alliances.create.hint.prefix");
   private static final Component COLOR_LABEL = Component.translatable("lsu.alliances.create.color");
   private static final Component COLOR_HINT = Component.translatable("lsu.alliances.create.hint.color");
   private static final Component DESCRIPTION_LABEL = Component.translatable("lsu.alliances.create.description");
   private static final Component DESCRIPTION_HINT = Component.translatable("lsu.alliances.create.hint.description");
   private static final Component MOTD_LABEL = Component.translatable("lsu.alliances.create.motd");
   private static final Component MOTD_HINT = Component.translatable("lsu.alliances.create.hint.motd");
   private static final Component CREATE_LABEL = Component.translatable("lsu.alliances.create.submit");
   private static final Component CANCEL_LABEL = Component.translatable("gui.cancel");
   private static final Component CREATING_TEXT = Component.translatable("lsu.alliances.creating");
   private static final Component REQUIRED_TEXT = Component.translatable("lsu.alliances.create.error.required");
   private static final Component FAILED_TEXT = Component.translatable("lsu.alliances.create.error.failed");
   private static final Component NAME_HINT = Component.translatable("lsu.alliances.create.hint.name");

   private static final int MAX_NAME_LENGTH = 30;
   private static final int MAX_PREFIX_LENGTH = 30;
   private static final int MAX_COLOR_LENGTH = 7;
   private static final int MAX_DESCRIPTION_LENGTH = 200;
   private static final int MAX_MOTD_LENGTH = 300;
   private static final int DEFAULT_MULTILINE_WIDTH = AllianceEditStyle.PANEL_WIDTH - AllianceEditStyle.PANEL_PADDING * 2;

   private final Screen lastScreen;
   private EditBox nameField;
   private EditBox prefixField;
   private EditBox colorField;
   private MultiLineEditBox descriptionField;
   private MultiLineEditBox motdField;
   private AllianceCreatePanel createPanel;
   private AllianceDetailButton createButton;
   private AllianceDetailButton cancelButton;
   private boolean creating;
   private final AllianceStatusState status = new AllianceStatusState(AlliancesListStyle.TEXT_MUTED);

   /**
    * creates a new create alliance screen.
    *
    * @param lastScreen the screen to return to
    */
   public CreateAllianceScreen(Screen lastScreen) {
      super(TITLE);
      this.lastScreen = lastScreen;
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

      updateCreateState();

      super.init();
      setInitialFocus(nameField);
   }

   @Override
   protected Drawable buildUi() {
      this.createButton = AllianceDetailButton.primary(
              () -> CREATE_LABEL,
              this::onCreateClicked,
              () -> !creating && isFormValid()
      );
      this.cancelButton = AllianceDetailButton.secondary(
              () -> CANCEL_LABEL,
              this::onClose,
              () -> !creating
      );

      this.createPanel = new AllianceCreatePanel(
              TITLE,
              SUBTITLE,
              NAME_LABEL,
              PREFIX_LABEL,
              PREFIX_HINT,
              COLOR_LABEL,
              COLOR_HINT,
              DESCRIPTION_LABEL,
              DESCRIPTION_HINT,
              MOTD_LABEL,
              MOTD_HINT,
              status::message,
              status::color,
              nameField,
              prefixField,
              colorField,
              descriptionField,
              motdField,
              createButton,
              cancelButton
      );

      AllianceEditLayout layout = new AllianceEditLayout(createPanel);
      return new ModalOverlay(layout);
   }

   @Override
   protected boolean enableVanillaWidgets() {
      return true;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void updateCreateState() {
      if (isFormValid()) {
         status.clear();
      }
   }

   private boolean isFormValid() {
      return !nameField.getValue().trim().isEmpty()
              && !descriptionField.getValue().trim().isEmpty()
              && !motdField.getValue().trim().isEmpty();
   }

   private void onCreateClicked() {
      if (creating) {
         return;
      }

      if (!isFormValid()) {
         status.set(REQUIRED_TEXT, AlliancesListStyle.TEXT_ERROR);
         return;
      }

      String name = nameField.getValue().trim();
      String prefix = prefixField.getValue().trim();
      String color = colorField.getValue().trim();
      String description = descriptionField.getValue().trim();
      String motd = motdField.getValue().trim();

      setCreatingState(true);
      AlliancesAPIClient.createAlliance(name, prefix.isEmpty() ? null : prefix, color.isEmpty() ? null : color, description, motd).thenAccept(alliance -> {
         this.minecraft.execute(() -> handleCreateResult(alliance));
      });
   }

   private void handleCreateResult(Alliance alliance) {
      setCreatingState(false);
      if (alliance != null) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.create.success"));
         this.minecraft.setScreen(new AllianceDetailScreen(this.lastScreen, alliance));
      } else {
         MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.create.failure"));
         status.set(FAILED_TEXT, AlliancesListStyle.TEXT_ERROR);
      }
   }

   private void setCreatingState(boolean creating) {
      this.creating = creating;
      if (creating) {
         status.set(CREATING_TEXT, AlliancesListStyle.TEXT_WARNING);
      } else {
         updateCreateState();
      }
   }

   private static final class AllianceCreatePanel implements Drawable {
      private final Component title;
      private final Component subtitle;
      private final Component nameLabel;
      private final Component prefixLabel;
      private final Component prefixHint;
      private final Component colorLabel;
      private final Component colorHint;
      private final Component descriptionLabel;
      private final Component descriptionHint;
      private final Component motdLabel;
      private final Component motdHint;
      private final Supplier<Component> statusSupplier;
      private final IntSupplier statusColorSupplier;
      private final EditBox nameField;
      private final EditBox prefixField;
      private final EditBox colorField;
      private final MultiLineEditBox descriptionField;
      private final MultiLineEditBox motdField;
      private final AllianceDetailButton createButton;
      private final AllianceDetailButton cancelButton;

      private UiBounds bounds = UiBounds.empty();

      private AllianceCreatePanel(
              Component title,
              Component subtitle,
              Component nameLabel,
              Component prefixLabel,
              Component prefixHint,
              Component colorLabel,
              Component colorHint,
              Component descriptionLabel,
              Component descriptionHint,
              Component motdLabel,
              Component motdHint,
              Supplier<Component> statusSupplier,
              IntSupplier statusColorSupplier,
              EditBox nameField,
              EditBox prefixField,
              EditBox colorField,
              MultiLineEditBox descriptionField,
              MultiLineEditBox motdField,
              AllianceDetailButton createButton,
              AllianceDetailButton cancelButton
      ) {
         this.title = title;
         this.subtitle = subtitle;
         this.nameLabel = nameLabel;
         this.prefixLabel = prefixLabel;
         this.prefixHint = prefixHint;
         this.colorLabel = colorLabel;
         this.colorHint = colorHint;
         this.descriptionLabel = descriptionLabel;
         this.descriptionHint = descriptionHint;
         this.motdLabel = motdLabel;
         this.motdHint = motdHint;
         this.statusSupplier = statusSupplier;
         this.statusColorSupplier = statusColorSupplier;
         this.nameField = nameField;
         this.prefixField = prefixField;
         this.colorField = colorField;
         this.descriptionField = descriptionField;
         this.motdField = motdField;
         this.createButton = createButton;
         this.cancelButton = cancelButton;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         this.bounds = layoutContext.availableBounds();
         Font font = layoutContext.font();

         int cursorY = bounds.y() + AllianceEditStyle.PANEL_PADDING;
         int textX = bounds.x() + AllianceEditStyle.PANEL_PADDING;
         int contentWidth = bounds.width() - AllianceEditStyle.PANEL_PADDING * 2;

         cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;
         cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;

         cursorY = layoutField(font, textX, contentWidth, cursorY, nameField, AllianceEditStyle.FIELD_HEIGHT, 1);
         cursorY = layoutField(font, textX, contentWidth, cursorY, prefixField, AllianceEditStyle.FIELD_HEIGHT, 2);
         cursorY = layoutField(font, textX, contentWidth, cursorY, colorField, AllianceEditStyle.FIELD_HEIGHT, 2);
         cursorY = layoutField(font, textX, contentWidth, cursorY, descriptionField, AllianceEditStyle.FIELD_HEIGHT_LONG, 2);
         cursorY = layoutField(font, textX, contentWidth, cursorY, motdField, AllianceEditStyle.FIELD_HEIGHT_LONG, 2);

         cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;

         int buttonsY = cursorY;
         UiSize createSize = createButton.preferredSize(layoutContext);
         UiSize cancelSize = cancelButton.preferredSize(layoutContext);
         int totalWidth = createSize.width() + AllianceEditStyle.ACTION_GAP + cancelSize.width();
         int buttonsX = bounds.x() + (bounds.width() - totalWidth) / 2;
         createButton.layout(layoutContext.withBounds(new UiBounds(buttonsX, buttonsY, createSize.width(), AllianceDetailStyle.BUTTON_HEIGHT)));
         cancelButton.layout(layoutContext.withBounds(new UiBounds(buttonsX + createSize.width() + AllianceEditStyle.ACTION_GAP, buttonsY, cancelSize.width(), AllianceDetailStyle.BUTTON_HEIGHT)));
      }

      private int layoutField(Font font, int textX, int contentWidth, int cursorY, EditBox field, int height, int labelLines) {
         int fieldY = cursorY + font.lineHeight * labelLines + AllianceEditStyle.PANEL_GAP;
         field.setX(textX);
         field.setY(fieldY);
         field.setWidth(contentWidth);
         field.setHeight(height);
         field.visible = true;
         return fieldY + height + AllianceEditStyle.PANEL_GAP;
      }

      private int layoutField(Font font, int textX, int contentWidth, int cursorY, MultiLineEditBox field, int height, int labelLines) {
         int fieldY = cursorY + font.lineHeight * labelLines + AllianceEditStyle.PANEL_GAP;
         field.setX(textX);
         field.setY(fieldY);
         field.setWidth(contentWidth);
         field.setHeight(height);
         field.visible = true;
         return fieldY + height + AllianceEditStyle.PANEL_GAP;
      }

      @Override
      public void render(UiContext context) {
         AllianceEditStyle.renderPanel(context.graphics(), bounds);

         Font font = context.minecraft().font;
         int cursorY = bounds.y() + AllianceEditStyle.PANEL_PADDING;
         int textX = bounds.x() + AllianceEditStyle.PANEL_PADDING;

         context.graphics().drawString(font, title, textX, cursorY, AllianceEditStyle.TEXT_PRIMARY, true);
         cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;
         context.graphics().drawString(font, subtitle, textX, cursorY, AllianceEditStyle.TEXT_MUTED, false);
         cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;

         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, nameLabel, AllianceEditStyle.TEXT_PRIMARY);
         cursorY += AllianceEditStyle.FIELD_HEIGHT + AllianceEditStyle.PANEL_GAP;

         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, prefixLabel, AllianceEditStyle.TEXT_PRIMARY);
         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, prefixHint, AllianceEditStyle.TEXT_MUTED);
         cursorY += AllianceEditStyle.FIELD_HEIGHT + AllianceEditStyle.PANEL_GAP;

         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, colorLabel, AllianceEditStyle.TEXT_PRIMARY);
         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, colorHint, AllianceEditStyle.TEXT_MUTED);
         cursorY += AllianceEditStyle.FIELD_HEIGHT + AllianceEditStyle.PANEL_GAP;

         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, descriptionLabel, AllianceEditStyle.TEXT_PRIMARY);
         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, descriptionHint, AllianceEditStyle.TEXT_MUTED);
         cursorY += AllianceEditStyle.FIELD_HEIGHT_LONG + AllianceEditStyle.PANEL_GAP;

         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, motdLabel, AllianceEditStyle.TEXT_PRIMARY);
         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, motdHint, AllianceEditStyle.TEXT_MUTED);
         cursorY += AllianceEditStyle.FIELD_HEIGHT_LONG + AllianceEditStyle.PANEL_GAP;

         Component status = statusSupplier.get();
         if (status != null && !status.getString().isEmpty()) {
            context.graphics().drawString(font, status, textX, cursorY, statusColorSupplier.getAsInt(), true);
         }

         createButton.render(context);
         cancelButton.render(context);
      }

      private int renderLabelAndAdvance(UiContext context, Font font, int textX, int cursorY, Component label, int color) {
         context.graphics().drawString(font, label, textX, cursorY, color, false);
         return cursorY + font.lineHeight + AllianceEditStyle.PANEL_GAP;
      }

      @Override
      public void handleInput(UiInputState input) {
         createButton.handleInput(input);
         cancelButton.handleInput(input);
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

   private static final class AllianceEditLayout implements Drawable {
      private final Drawable panel;

      private UiBounds bounds = UiBounds.empty();

      private AllianceEditLayout(Drawable panel) {
         this.panel = panel;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         UiBounds available = layoutContext.availableBounds();
         int width = Math.min(AllianceEditStyle.PANEL_WIDTH, available.width());
         int x = available.x() + (available.width() - width) / 2;
         int y = available.y() + AllianceEditStyle.PANEL_GAP * 2;
         int height = Math.max(available.height() - AllianceEditStyle.PANEL_GAP * 4, 0);
         UiBounds panelBounds = new UiBounds(x, y, width, height);

         this.bounds = available;
         panel.layout(layoutContext.withBounds(panelBounds));
      }

      @Override
      public void render(UiContext context) {
         panel.render(context);
      }

      @Override
      public void handleInput(UiInputState input) {
         panel.handleInput(input);
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

   private static final class ModalOverlay implements Drawable {
      private final Drawable child;

      private UiBounds bounds = UiBounds.empty();

      private ModalOverlay(Drawable child) {
         this.child = child;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         bounds = layoutContext.availableBounds();
         child.layout(layoutContext.withBounds(bounds));
      }

      @Override
      public void render(UiContext context) {
         context.graphics().fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), AllianceDetailStyle.OVERLAY_COLOR);
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
