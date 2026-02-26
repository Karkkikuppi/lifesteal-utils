package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * renders the scrollable alliances list.
 */
final class AlliancesListView implements Drawable {
   private final Supplier<List<Alliance>> alliancesSupplier;
   private final BooleanSupplier loadingSupplier;
   private final Supplier<Component> statusTextSupplier;
   private final IntSupplier statusColorSupplier;
   private final Consumer<Alliance> selectHandler;
   private final Consumer<Alliance> acceptInviteHandler;
   private final Consumer<Alliance> rejectInviteHandler;
   private final Component emptyText;
   private final Component invitesTitle;
   private final Component alliancesTitle;
   private final Component inviteBadge;
   private final Component localOnlyBadge;
   private final Component acceptButtonLabel = Component.translatable("lsu.alliances.accept");
   private final Component rejectButtonLabel = Component.translatable("lsu.alliances.reject");

   private final List<EntryLayout> entries = new ArrayList<>();

   private UiBounds bounds = UiBounds.empty();
   private int contentHeight;
   private int maxScroll;
   private int scrollOffset;
   private int hoveredCardIndex = -1;

   AlliancesListView(
           Supplier<List<Alliance>> alliancesSupplier,
           BooleanSupplier loadingSupplier,
           Supplier<Component> statusTextSupplier,
           IntSupplier statusColorSupplier,
           Consumer<Alliance> selectHandler,
           Consumer<Alliance> acceptInviteHandler,
           Consumer<Alliance> rejectInviteHandler,
           Component emptyText,
           Component invitesTitle,
           Component alliancesTitle,
           Component inviteBadge,
           Component localOnlyBadge
   ) {
      this.alliancesSupplier = alliancesSupplier;
      this.loadingSupplier = loadingSupplier;
      this.statusTextSupplier = statusTextSupplier;
      this.statusColorSupplier = statusColorSupplier;
      this.selectHandler = selectHandler;
      this.acceptInviteHandler = acceptInviteHandler;
      this.rejectInviteHandler = rejectInviteHandler;
      this.emptyText = emptyText;
      this.invitesTitle = invitesTitle;
      this.alliancesTitle = alliancesTitle;
      this.inviteBadge = inviteBadge;
      this.localOnlyBadge = localOnlyBadge;
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
      buildEntries(layoutContext);
      this.maxScroll = Math.max(contentHeight - Math.max(bounds.height() - AlliancesListStyle.CONTENT_PADDING * 2, 0), 0);
      this.scrollOffset = clamp(scrollOffset, 0, maxScroll);
   }

   private void buildEntries(UiLayoutContext layoutContext) {
      entries.clear();
      contentHeight = 0;
      Font font = layoutContext.font();
      int contentWidth = Math.max(bounds.width() - AlliancesListStyle.CONTENT_PADDING * 2, 0);
      boolean loading = loadingSupplier.getAsBoolean();
      Component statusText = statusTextSupplier.get();
      int statusColor = statusColorSupplier.getAsInt();

      if (loading) {
         entries.add(EntryLayout.status(statusText, statusColor));
         contentHeight = font.lineHeight;
         return;
      }

      List<Alliance> alliances = alliancesSupplier.get();
      if (!statusText.getString().isEmpty() && alliances.isEmpty()) {
         entries.add(EntryLayout.status(statusText, statusColor));
         contentHeight = font.lineHeight;
         return;
      }

      if (alliances.isEmpty()) {
         entries.add(EntryLayout.status(emptyText, AlliancesListStyle.TEXT_MUTED));
         contentHeight = font.lineHeight;
         return;
      }

      List<Alliance> invited = new ArrayList<>();
      List<Alliance> joined = new ArrayList<>();

      String playerUuid = layoutContext.minecraft().player != null ? layoutContext.minecraft().player.getStringUUID() : "";
      for (Alliance alliance : alliances) {
         AllianceMember member = alliance.findMemberByUuid(playerUuid);
         if (member != null && member.isInvited()) {
            invited.add(alliance);
         } else {
            joined.add(alliance);
         }
      }

      int cursor = 0;
      if (!joined.isEmpty()) {
         cursor = addSectionEntries(joined, alliancesTitle, font, contentWidth, cursor, false);
      }

      if (!invited.isEmpty()) {
         if (!joined.isEmpty()) {
            cursor += AlliancesListStyle.ENTRY_GAP;
         }
         cursor = addSectionEntries(invited, invitesTitle, font, contentWidth, cursor, true);
      }

      contentHeight = Math.max(cursor - AlliancesListStyle.ENTRY_GAP, 0);
   }

   private int addSectionEntries(List<Alliance> alliances, Component title, Font font, int contentWidth, int cursor, boolean invitedSection) {
      int sectionHeight = font.lineHeight + AlliancesListStyle.SECTION_PADDING_Y * 2;
      entries.add(EntryLayout.section(title, sectionHeight));
      cursor += sectionHeight + AlliancesListStyle.ENTRY_GAP;

      for (Alliance alliance : alliances) {
         EntryLayout cardEntry = EntryLayout.card(alliance, buildDescriptionLines(alliance, font, contentWidth), font, invitedSection);
         entries.add(cardEntry);
         cursor += cardEntry.height + AlliancesListStyle.ENTRY_GAP;
      }

      return cursor;
   }

   private List<FormattedCharSequence> buildDescriptionLines(Alliance alliance, Font font, int contentWidth) {
      String description = alliance.description();
      if (description == null || description.isBlank()) {
         return List.of();
      }
      int maxWidth = Math.max(contentWidth - AlliancesListStyle.CARD_PADDING * 2, 0);
      return font.split(Component.literal(description), maxWidth);
   }

   @Override
   public void render(UiContext context) {
      GuiGraphics graphics = context.graphics();
      AlliancesListStyle.renderBackgroundSprite(graphics, bounds);

      graphics.enableScissor(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height());
      if (entries.isEmpty()) {
         graphics.disableScissor();
         return;
      }

      Font font = context.minecraft().font;
      int cursorY = bounds.y() + AlliancesListStyle.CONTENT_PADDING - scrollOffset;
      int contentWidth = Math.max(bounds.width() - AlliancesListStyle.CONTENT_PADDING * 2, 0);
      int listX = bounds.x() + AlliancesListStyle.CONTENT_PADDING;

      for (int index = 0; index < entries.size(); index++) {
         EntryLayout entry = entries.get(index);
         int entryTop = cursorY;
         if (entry.type == EntryType.STATUS) {
            int textWidth = font.width(entry.title);
            int textX = bounds.x() + (bounds.width() - textWidth) / 2;
            int textY = bounds.y() + (bounds.height() - font.lineHeight) / 2;
            graphics.drawString(font, entry.title, textX, textY, entry.color, true);
            break;
         }

         if (entryTop + entry.height < bounds.y() || entryTop > bounds.y() + bounds.height()) {
            cursorY += entry.height + AlliancesListStyle.ENTRY_GAP;
            continue;
         }

         if (entry.type == EntryType.SECTION) {
            renderSection(graphics, font, listX, entryTop, contentWidth, entry);
         } else if (entry.type == EntryType.CARD) {
            renderCard(graphics, font, listX, entryTop, contentWidth, entry, index == hoveredCardIndex);
         }

         cursorY += entry.height + AlliancesListStyle.ENTRY_GAP;
      }

      graphics.disableScissor();
   }

   private void renderSection(GuiGraphics graphics, Font font, int x, int y, int width, EntryLayout entry) {
      UiBounds sectionBounds = new UiBounds(x, y, width, entry.height);
      AlliancesListStyle.renderBoxBackgroundSprite(graphics, sectionBounds);

      int textX = x + AlliancesListStyle.SECTION_PADDING_X;
      int textY = y + AlliancesListStyle.SECTION_PADDING_Y;
      graphics.drawString(font, entry.title, textX, textY, AlliancesListStyle.TEXT_PRIMARY, true);
   }

   private void renderCard(GuiGraphics graphics, Font font, int x, int y, int width, EntryLayout entry, boolean hovered) {
      int right = x + width;
      int bottom = y + entry.height;
      UiBounds cardBounds = new UiBounds(x, y, width, entry.height);
      AlliancesListStyle.renderBoxBackgroundSprite(graphics, cardBounds);
      if (hovered) {
         graphics.fill(x, y, right, bottom, 0x22000000);
      }

      int cursorX = x + AlliancesListStyle.CARD_PADDING;
      int cursorY = y + AlliancesListStyle.CARD_PADDING;

      Integer allianceRgb = parseAllianceRgb(entry.alliance != null ? entry.alliance.color() : null);
      int allianceTextColor = allianceRgb != null ? (0xFF000000 | allianceRgb) : AlliancesListStyle.TEXT_PRIMARY;

      if (entry.alliance != null && entry.alliance.prefix() != null && !entry.alliance.prefix().isBlank()) {
         String prefixText = entry.alliance.prefix();
         String separator = " | ";
         String nameText = entry.alliance.name();

         graphics.drawString(font, prefixText, cursorX, cursorY, allianceTextColor, true);
         int separatorX = cursorX + font.width(prefixText);
         graphics.drawString(font, separator, separatorX, cursorY, AlliancesListStyle.TEXT_MUTED, true);
         int nameX = separatorX + font.width(separator);
         graphics.drawString(font, nameText, nameX, cursorY, allianceTextColor, true);
      } else {
         graphics.drawString(font, entry.name, cursorX, cursorY, allianceTextColor, true);
      }

      if (entry.invited) {
         renderBadge(graphics, font, inviteBadge, right - AlliancesListStyle.CARD_PADDING, cursorY, true);
      }

      int descriptionTop = cursorY + font.lineHeight + AlliancesListStyle.CARD_SECTION_GAP;
      if (!entry.descriptionLines.isEmpty()) {
         int lineY = descriptionTop;
         for (FormattedCharSequence line : entry.descriptionLines) {
            graphics.drawString(font, line, cursorX, lineY, AlliancesListStyle.TEXT_MUTED, false);
            lineY += font.lineHeight;
         }
         descriptionTop = lineY + AlliancesListStyle.CARD_SECTION_GAP;
      }

      int membersCount = entry.joinedCount;
      Component membersText = Component.translatable("lsu.alliances.members", membersCount);
      if (entry.local) {
         renderBadge(graphics, font, localOnlyBadge, right - AlliancesListStyle.CARD_PADDING, descriptionTop, false);
         descriptionTop += font.lineHeight + AlliancesListStyle.BADGE_PADDING_Y * 2 + AlliancesListStyle.CARD_SECTION_GAP;
      }
      renderBadge(graphics, font, membersText, right - AlliancesListStyle.CARD_PADDING, descriptionTop, false);

      if (entry.invited) {
         int buttonHeight = font.lineHeight + AlliancesListStyle.BADGE_PADDING_Y * 2;
         int actionsY = bottom - AlliancesListStyle.CARD_PADDING - buttonHeight;
         renderInviteActionButtons(graphics, font, x + AlliancesListStyle.CARD_PADDING, actionsY, hovered);
      }
   }

   private void renderInviteActionButtons(GuiGraphics graphics, Font font, int x, int y, boolean hovered) {
      UiBounds acceptBounds = createActionButtonBounds(font, x, y, acceptButtonLabel);
      UiBounds rejectBounds = createActionButtonBounds(font, acceptBounds.x() + acceptBounds.width() + AlliancesListStyle.CARD_SECTION_GAP, y, rejectButtonLabel);

      renderActionButton(graphics, font, acceptBounds, acceptButtonLabel, hovered);
      renderActionButton(graphics, font, rejectBounds, rejectButtonLabel, hovered);
   }

   private UiBounds createActionButtonBounds(Font font, int x, int y, Component label) {
      int width = font.width(label) + AlliancesListStyle.BADGE_PADDING_X * 2;
      int height = font.lineHeight + AlliancesListStyle.BADGE_PADDING_Y * 2;
      return new UiBounds(x, y, width, height);
   }

   private void renderActionButton(GuiGraphics graphics, Font font, UiBounds bounds, Component label, boolean hovered) {
      int bgColor = hovered ? AlliancesListStyle.BADGE_BG_COLOR : AlliancesListStyle.INVITE_BADGE_BG_COLOR;
      int borderColor = hovered ? AlliancesListStyle.BADGE_BORDER_COLOR : AlliancesListStyle.INVITE_BADGE_BORDER_COLOR;
      graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), bgColor);
      graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + 1, borderColor);
      graphics.fill(bounds.x(), bounds.y() + bounds.height() - 1, bounds.x() + bounds.width(), bounds.y() + bounds.height(), borderColor);
      graphics.fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.y() + bounds.height(), borderColor);
      graphics.fill(bounds.x() + bounds.width() - 1, bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), borderColor);
      graphics.drawString(font, label, bounds.x() + AlliancesListStyle.BADGE_PADDING_X, bounds.y() + AlliancesListStyle.BADGE_PADDING_Y, AlliancesListStyle.TEXT_PRIMARY, false);
   }

   private void renderBadge(GuiGraphics graphics, Font font, Component text, int rightX, int y, boolean invite) {
      int textWidth = font.width(text);
      int badgeWidth = textWidth + AlliancesListStyle.BADGE_PADDING_X * 2;
      int badgeHeight = font.lineHeight + AlliancesListStyle.BADGE_PADDING_Y * 2;
      int badgeX = rightX - badgeWidth;
      int badgeY = y - AlliancesListStyle.BADGE_PADDING_Y;
      int bgColor = invite ? AlliancesListStyle.INVITE_BADGE_BG_COLOR : AlliancesListStyle.BADGE_BG_COLOR;
      int borderColor = invite ? AlliancesListStyle.INVITE_BADGE_BORDER_COLOR : AlliancesListStyle.BADGE_BORDER_COLOR;
      int textColor = invite ? AlliancesListStyle.TEXT_INVITE : AlliancesListStyle.TEXT_MUTED;

      graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, bgColor);
      graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 1, borderColor);
      graphics.fill(badgeX, badgeY + badgeHeight - 1, badgeX + badgeWidth, badgeY + badgeHeight, borderColor);
      graphics.fill(badgeX, badgeY, badgeX + 1, badgeY + badgeHeight, borderColor);
      graphics.fill(badgeX + badgeWidth - 1, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, borderColor);

      int textX = badgeX + AlliancesListStyle.BADGE_PADDING_X;
      int textY = badgeY + AlliancesListStyle.BADGE_PADDING_Y;
      graphics.drawString(font, text, textX, textY, textColor, false);
   }

   @Override
   public void handleInput(UiInputState input) {
      hoveredCardIndex = -1;
      if (!input.isHovering(bounds)) {
         return;
      }

      double scrollDelta = input.scrollY();
      if (scrollDelta != 0) {
         int delta = (int) Math.round(scrollDelta * -AlliancesListStyle.SCROLL_STEP);
         scrollOffset = clamp(scrollOffset + delta, 0, maxScroll);
      }

      if (loadingSupplier.getAsBoolean()) {
         return;
      }

      int cursorY = bounds.y() + AlliancesListStyle.CONTENT_PADDING - scrollOffset;
      int contentWidth = Math.max(bounds.width() - AlliancesListStyle.CONTENT_PADDING * 2, 0);
      int listX = bounds.x() + AlliancesListStyle.CONTENT_PADDING;
      Font font = Minecraft.getInstance().font;

      for (int index = 0; index < entries.size(); index++) {
         EntryLayout entry = entries.get(index);
         if (entry.type != EntryType.CARD) {
            cursorY += entry.height + AlliancesListStyle.ENTRY_GAP;
            continue;
         }

         UiBounds cardBounds = new UiBounds(listX, cursorY, contentWidth, entry.height);
         if (input.isHovering(cardBounds)) {
            hoveredCardIndex = index;
            if (input.leftClicked()) {
               if (entry.invited) {
                  int actionY = cardBounds.y() + cardBounds.height() - (font.lineHeight + AlliancesListStyle.BADGE_PADDING_Y * 2) - AlliancesListStyle.CARD_PADDING;
                  UiBounds acceptBounds = createActionButtonBounds(font, cardBounds.x() + AlliancesListStyle.CARD_PADDING, actionY, acceptButtonLabel);
                  UiBounds rejectBounds = createActionButtonBounds(font, acceptBounds.x() + acceptBounds.width() + AlliancesListStyle.CARD_SECTION_GAP, actionY, rejectButtonLabel);
                  if (input.isHovering(acceptBounds)) {
                     acceptInviteHandler.accept(entry.alliance);
                     break;
                  }
                  if (input.isHovering(rejectBounds)) {
                     rejectInviteHandler.accept(entry.alliance);
                     break;
                  }
               }
               selectHandler.accept(entry.alliance);
            }
            break;
         }

         cursorY += entry.height + AlliancesListStyle.ENTRY_GAP;
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

   private static Integer parseAllianceRgb(String color) {
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

   /**
    * captures the layout data for list entries.
    */
   private record EntryLayout(EntryType type, Component title, int color, int height, Alliance alliance, Component name,
                              List<FormattedCharSequence> descriptionLines, boolean invited, boolean local,
                              int joinedCount) {

      private static EntryLayout status(Component text, int color) {
         return new EntryLayout(EntryType.STATUS, text, color, 0, null, null, List.of(), false, false, 0);
      }

      private static EntryLayout section(Component title, int height) {
         return new EntryLayout(EntryType.SECTION, title, AlliancesListStyle.TEXT_PRIMARY, height, null, null, List.of(), false, false, 0);
      }

      private static EntryLayout card(Alliance alliance, List<FormattedCharSequence> descriptionLines, Font font, boolean invited) {
         int nameLineHeight = font.lineHeight;
         int descriptionHeight = descriptionLines.size() * nameLineHeight;
         int badgeHeight = nameLineHeight + AlliancesListStyle.BADGE_PADDING_Y * 2;
         int height = AlliancesListStyle.CARD_PADDING * 2 + nameLineHeight + badgeHeight + AlliancesListStyle.CARD_SECTION_GAP;
         if (alliance.isLocal()) {
            height += badgeHeight + AlliancesListStyle.CARD_SECTION_GAP;
         }
         if (descriptionHeight > 0) {
            height += descriptionHeight + AlliancesListStyle.CARD_SECTION_GAP;
         }
         if (invited) {
            height += badgeHeight + AlliancesListStyle.CARD_SECTION_GAP;
         }

         int joinedCount = alliance.getJoinedMembers().size();
         return new EntryLayout(
                 EntryType.CARD,
                 null,
                 AlliancesListStyle.TEXT_PRIMARY,
                 height,
                 alliance,
                 Component.literal(alliance.getDisplayName()),
                 descriptionLines,
                 invited,
                 alliance.isLocal(),
                 joinedCount
         );
      }
   }

   private enum EntryType {
      STATUS,
      SECTION,
      CARD
   }
}
