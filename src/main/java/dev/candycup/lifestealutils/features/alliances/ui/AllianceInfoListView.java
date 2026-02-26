package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * renders alliance info entries in a scrollable panel.
 */
final class AllianceInfoListView implements Drawable {
   private static final Component EDIT_LABEL = Component.translatable("lsu.alliances.detail.edit");
   private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
           .withZone(ZoneId.systemDefault());

   private final Supplier<Alliance> allianceSupplier;
   private final BooleanSupplier canEditSupplier;
   private final BooleanSupplier showModernFieldsSupplier;
   private final Consumer<AllianceEditField> editHandler;

   private final List<EntryLayout> entries = new ArrayList<>();

   private UiBounds bounds = UiBounds.empty();
   private int contentHeight;
   private int maxScroll;
   private int scrollOffset;
   private int editButtonWidth = AllianceDetailStyle.BUTTON_EDIT_WIDTH;

   AllianceInfoListView(
           Supplier<Alliance> allianceSupplier,
           BooleanSupplier canEditSupplier,
           BooleanSupplier showModernFieldsSupplier,
           Consumer<AllianceEditField> editHandler
   ) {
      this.allianceSupplier = allianceSupplier;
      this.canEditSupplier = canEditSupplier;
      this.showModernFieldsSupplier = showModernFieldsSupplier;
      this.editHandler = editHandler;
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
      buildEntries(layoutContext);
      this.maxScroll = Math.max(contentHeight - Math.max(bounds.height() - AllianceDetailStyle.CONTENT_PADDING * 2, 0), 0);
      this.scrollOffset = clamp(scrollOffset, 0, maxScroll);
   }

   private void buildEntries(UiLayoutContext layoutContext) {
      entries.clear();
      contentHeight = 0;

      Alliance alliance = allianceSupplier.get();
      if (alliance == null) {
         return;
      }

      Font font = layoutContext.font();
      int contentWidth = Math.max(bounds.width() - AllianceDetailStyle.CONTENT_PADDING * 2, 0);
      boolean canEdit = canEditSupplier.getAsBoolean();
      boolean showModernFields = showModernFieldsSupplier.getAsBoolean();
      this.editButtonWidth = Math.max(
              font.width(EDIT_LABEL) + AllianceDetailStyle.BUTTON_PADDING_X * 2,
              AllianceDetailStyle.BUTTON_EDIT_MIN_WIDTH
      );

      List<EntryDefinition> definitions = new ArrayList<>();
      definitions.add(new EntryDefinition(Component.translatable("lsu.alliances.detail.name"), Component.literal(alliance.name()), AllianceEditField.NAME));
      definitions.add(new EntryDefinition(Component.translatable("lsu.alliances.detail.prefix"), Component.literal(alliance.prefix() == null ? "" : alliance.prefix()), AllianceEditField.PREFIX));
      definitions.add(new EntryDefinition(Component.translatable("lsu.alliances.detail.color"), Component.literal(alliance.color() == null ? "" : alliance.color()), AllianceEditField.COLOR));
      if (showModernFields) {
         definitions.add(new EntryDefinition(Component.translatable("lsu.alliances.detail.description"), Component.literal(alliance.description()), AllianceEditField.DESCRIPTION));
         definitions.add(new EntryDefinition(Component.translatable("lsu.alliances.detail.motd"), Component.literal(alliance.motd()), AllianceEditField.MOTD));
      }
      definitions.add(new EntryDefinition(Component.translatable("lsu.alliances.detail.member_count"), Component.literal(String.valueOf(alliance.getJoinedMembers().size())), null));
      definitions.add(new EntryDefinition(Component.translatable("lsu.alliances.detail.created"), Component.literal(DATE_FORMATTER.format(alliance.createdAt())), null));

      int cursor = 0;
      for (EntryDefinition definition : definitions) {
         boolean editable = definition.editField != null && canEdit;
         EntryLayout layout = EntryLayout.from(definition, editable, font, contentWidth, editButtonWidth);
         entries.add(layout);
         cursor += layout.height + AllianceDetailStyle.ENTRY_GAP;
      }

      contentHeight = Math.max(cursor - AllianceDetailStyle.ENTRY_GAP, 0);
   }

   @Override
   public void render(UiContext context) {
      GuiGraphics graphics = context.graphics();
      AlliancesListStyle.renderBackgroundSprite(graphics, bounds);

      graphics.enableScissor(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height());

      Font font = context.minecraft().font;
      int cursorY = bounds.y() + AllianceDetailStyle.CONTENT_PADDING - scrollOffset;
      int contentWidth = Math.max(bounds.width() - AllianceDetailStyle.CONTENT_PADDING * 2, 0);
      int listX = bounds.x() + AllianceDetailStyle.CONTENT_PADDING;

      for (EntryLayout entry : entries) {
         int entryTop = cursorY;
         if (entryTop + entry.height < bounds.y() || entryTop > bounds.y() + bounds.height()) {
            cursorY += entry.height + AllianceDetailStyle.ENTRY_GAP;
            continue;
         }

         renderEntry(context, graphics, font, listX, entryTop, contentWidth, entry);
         cursorY += entry.height + AllianceDetailStyle.ENTRY_GAP;
      }

      graphics.disableScissor();
   }

   private void renderEntry(UiContext context, GuiGraphics graphics, Font font, int x, int y, int width, EntryLayout entry) {
      int right = x + width;
      int bottom = y + entry.height;

      UiBounds entryBounds = new UiBounds(x, y, width, entry.height);
      AlliancesListStyle.renderBoxBackgroundSprite(graphics, entryBounds);

      int accentTop = y + AllianceDetailStyle.ENTRY_PADDING;
      int accentBottom = bottom - AllianceDetailStyle.ENTRY_PADDING;
      graphics.fill(x + AllianceDetailStyle.ENTRY_PADDING, accentTop, x + AllianceDetailStyle.ENTRY_PADDING + AllianceDetailStyle.ACCENT_BAR_WIDTH, accentBottom, AllianceDetailStyle.INFO_ACCENT_COLOR);

      int textX = x + AllianceDetailStyle.ENTRY_PADDING + AllianceDetailStyle.ACCENT_BAR_WIDTH + AllianceDetailStyle.CARD_SECTION_GAP;
      int labelY = y + AllianceDetailStyle.ENTRY_PADDING;
      graphics.drawString(font, entry.label, textX, labelY, AllianceDetailStyle.TEXT_MUTED, true);

      int valueY = labelY + font.lineHeight + AllianceDetailStyle.CARD_SECTION_GAP;
      for (FormattedCharSequence line : entry.valueLines) {
         graphics.drawString(font, line, textX, valueY, AllianceDetailStyle.TEXT_PRIMARY, false);
         valueY += font.lineHeight;
      }

      if (entry.editButton != null) {
         UiBounds buttonBounds = editButtonBounds(x, y, width, entry.height);
         entry.editButton.renderAt(context, buttonBounds);
      }
   }

   @Override
   public void handleInput(UiInputState input) {
      if (!input.isHovering(bounds)) {
         return;
      }

      double scrollDelta = input.scrollY();
      if (scrollDelta != 0) {
         int delta = (int) Math.round(scrollDelta * -AlliancesListStyle.SCROLL_STEP);
         scrollOffset = clamp(scrollOffset + delta, 0, maxScroll);
      }

      if (!input.leftClicked()) {
         return;
      }

      int cursorY = bounds.y() + AllianceDetailStyle.CONTENT_PADDING - scrollOffset;
      int contentWidth = Math.max(bounds.width() - AllianceDetailStyle.CONTENT_PADDING * 2, 0);
      int listX = bounds.x() + AllianceDetailStyle.CONTENT_PADDING;

      for (EntryLayout entry : entries) {
         UiBounds entryBounds = new UiBounds(listX, cursorY, contentWidth, entry.height);
         if (entry.editField != null && entry.editButton != null) {
            UiBounds buttonBounds = editButtonBounds(listX, cursorY, contentWidth, entry.height);
            if (input.isHovering(buttonBounds)) {
               editHandler.accept(entry.editField);
               return;
            }
         }
         cursorY += entry.height + AllianceDetailStyle.ENTRY_GAP;
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

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private record EntryDefinition(Component label, Component value, AllianceEditField editField) {
   }

   private record EntryLayout(Component label, List<FormattedCharSequence> valueLines, AllianceEditField editField,
                              int height, InlineButton editButton) {

      private static EntryLayout from(EntryDefinition definition, boolean editable, Font font, int contentWidth, int editButtonWidth) {
         int textWidth = Math.max(contentWidth - AllianceDetailStyle.ENTRY_PADDING * 2 - AllianceDetailStyle.ACCENT_BAR_WIDTH - AllianceDetailStyle.CARD_SECTION_GAP, 0);
         if (editable) {
            textWidth = Math.max(textWidth - editButtonWidth - AllianceDetailStyle.CARD_SECTION_GAP, 0);
         }

         List<FormattedCharSequence> valueLines = font.split(definition.value, textWidth);
         if (valueLines.isEmpty()) {
            valueLines = List.of(FormattedCharSequence.EMPTY);
         }

         int height = AllianceDetailStyle.ENTRY_PADDING * 2 + font.lineHeight + AllianceDetailStyle.CARD_SECTION_GAP + valueLines.size() * font.lineHeight;

         InlineButton button = null;
         if (editable) {
            button = new InlineButton(EDIT_LABEL);
         }

         return new EntryLayout(definition.label, valueLines, definition.editField, height, button);
      }
   }

   private record InlineButton(Component label) {

      private void renderAt(UiContext context, UiBounds bounds) {
         boolean hovered = context.input().isHovering(bounds);
         context.graphics().blitSprite(
                 RenderPipelines.GUI_TEXTURED,
                 AllianceDetailStyle.BUTTON_SECONDARY_SPRITES.get(true, hovered),
                 bounds.x(),
                 bounds.y(),
                 bounds.width(),
                 bounds.height()
         );
         int textWidth = context.minecraft().font.width(label);
         int textHeight = context.minecraft().font.lineHeight;
         int textX = bounds.x() + (bounds.width() - textWidth) / 2;
         int textY = bounds.y() + (bounds.height() - textHeight) / 2;
         context.graphics().drawString(context.minecraft().font, label, textX, textY, AllianceDetailStyle.BUTTON_TEXT, true);
      }
   }

   private UiBounds editButtonBounds(int x, int y, int width, int height) {
      int buttonWidth = editButtonWidth;
      int buttonHeight = AllianceDetailStyle.BUTTON_HEIGHT;
      int buttonX = x + width - AllianceDetailStyle.ENTRY_PADDING - buttonWidth;
      int buttonY = y + (height - buttonHeight) / 2;
      return new UiBounds(buttonX, buttonY, buttonWidth, buttonHeight);
   }
}
