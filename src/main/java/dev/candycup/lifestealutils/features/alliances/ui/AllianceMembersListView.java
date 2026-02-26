package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class AllianceMembersListView implements Drawable {
   private final Supplier<Alliance> allianceSupplier;
   private final BooleanSupplier localAllianceSupplier;
   private final BooleanSupplier canEditSupplier;
   private final Predicate<AllianceMember> isCurrentPlayer;
   private final Consumer<AllianceMember> onCancelInvite;
   private final Consumer<AllianceMember> onRemoveMember;
   private final Component emptyText;
   private final Component pendingTitle;

   private final List<EntryLayout> entries = new ArrayList<>();

   private UiBounds bounds = UiBounds.empty();
   private int contentHeight;
   private int maxScroll;
   private int scrollOffset;

   AllianceMembersListView(
           Supplier<Alliance> allianceSupplier,
           BooleanSupplier localAllianceSupplier,
           BooleanSupplier canEditSupplier,
           Predicate<AllianceMember> isCurrentPlayer,
           Consumer<AllianceMember> onCancelInvite,
           Consumer<AllianceMember> onRemoveMember,
           Component emptyText,
           Component pendingTitle
   ) {
      this.allianceSupplier = allianceSupplier;
      this.localAllianceSupplier = localAllianceSupplier;
      this.canEditSupplier = canEditSupplier;
      this.isCurrentPlayer = isCurrentPlayer;
      this.onCancelInvite = onCancelInvite;
      this.onRemoveMember = onRemoveMember;
      this.emptyText = emptyText;
      this.pendingTitle = pendingTitle;
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
      boolean localAlliance = localAllianceSupplier.getAsBoolean();
      List<AllianceMember> joinedMembers = alliance.getJoinedMembers();
      List<AllianceMember> invitedMembers = localAlliance ? List.of() : alliance.getInvitedMembers();

      int cursor = 0;
      for (AllianceMember member : joinedMembers) {
         EntryLayout entry = EntryLayout.member(member, false, localAlliance, font);
         entries.add(entry);
         cursor += entry.height + AllianceDetailStyle.ENTRY_GAP;
      }

      if (!invitedMembers.isEmpty()) {
         if (!joinedMembers.isEmpty()) {
            cursor += AllianceDetailStyle.ENTRY_GAP;
         }
         EntryLayout header = EntryLayout.section(pendingTitle, font);
         entries.add(header);
         cursor += header.height + AllianceDetailStyle.ENTRY_GAP;

         for (AllianceMember member : invitedMembers) {
            EntryLayout entry = EntryLayout.member(member, true, false, font);
            entries.add(entry);
            cursor += entry.height + AllianceDetailStyle.ENTRY_GAP;
         }
      }

      if (joinedMembers.isEmpty() && invitedMembers.isEmpty()) {
         EntryLayout empty = EntryLayout.status(emptyText, font);
         entries.add(empty);
         cursor += empty.height;
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
         if (entry.type == EntryType.STATUS) {
            int textWidth = font.width(entry.title);
            int textX = bounds.x() + (bounds.width() - textWidth) / 2;
            int textY = bounds.y() + (bounds.height() - font.lineHeight) / 2;
            graphics.drawString(font, entry.title, textX, textY, AllianceDetailStyle.TEXT_MUTED, true);
            break;
         }

         if (entryTop + entry.height < bounds.y() || entryTop > bounds.y() + bounds.height()) {
            cursorY += entry.height + AllianceDetailStyle.ENTRY_GAP;
            continue;
         }

         if (entry.type == EntryType.SECTION) {
            renderSection(graphics, font, listX, entryTop, contentWidth, entry);
         } else if (entry.type == EntryType.MEMBER) {
            renderMember(context, font, listX, entryTop, contentWidth, entry);
         }

         cursorY += entry.height + AllianceDetailStyle.ENTRY_GAP;
      }

      graphics.disableScissor();
   }

   private void renderSection(GuiGraphics graphics, Font font, int x, int y, int width, EntryLayout entry) {
      UiBounds sectionBounds = new UiBounds(x, y, width, entry.height);
      AlliancesListStyle.renderBoxBackgroundSprite(graphics, sectionBounds);

      int textX = x + AllianceDetailStyle.ENTRY_PADDING;
      int textY = y + (entry.height - font.lineHeight) / 2;
      graphics.drawString(font, entry.title, textX, textY, AllianceDetailStyle.TEXT_PRIMARY, true);
   }

   private void renderMember(UiContext context, Font font, int x, int y, int width, EntryLayout entry) {
      GuiGraphics graphics = context.graphics();
      int right = x + width;
      int bottom = y + entry.height;

      UiBounds memberBounds = new UiBounds(x, y, width, entry.height);
      AlliancesListStyle.renderBoxBackgroundSprite(graphics, memberBounds);

      int accentTop = y + AllianceDetailStyle.ENTRY_PADDING;
      int accentBottom = bottom - AllianceDetailStyle.ENTRY_PADDING;
      graphics.fill(x + AllianceDetailStyle.ENTRY_PADDING, accentTop, x + AllianceDetailStyle.ENTRY_PADDING + AllianceDetailStyle.ACCENT_BAR_WIDTH, accentBottom, AllianceDetailStyle.MEMBER_ACCENT_COLOR);

      int textX = x + AllianceDetailStyle.ENTRY_PADDING + AllianceDetailStyle.ACCENT_BAR_WIDTH + AllianceDetailStyle.CARD_SECTION_GAP;
      int nameY = y + AllianceDetailStyle.ENTRY_PADDING;
      graphics.drawString(font, entry.name, textX, nameY, AllianceDetailStyle.TEXT_PRIMARY, true);

      if (!entry.localMode) {
         Component statusText = Component.translatable(entry.pending ? "lsu.alliances.detail.member_status.pending" : "lsu.alliances.detail.member_status.full");
         int statusColor = entry.pending ? AllianceDetailStyle.TEXT_PENDING : AllianceDetailStyle.TEXT_POSITIVE;
         graphics.drawString(font, statusText, textX, nameY + font.lineHeight + 2, statusColor, false);

         int badgeX = right - AllianceDetailStyle.ENTRY_PADDING;
         if (entry.admin) {
            badgeX = renderBadge(graphics, font, Component.translatable("lsu.alliances.detail.admin_badge"), badgeX, nameY, false);
         }
         if (entry.pending) {
            renderBadge(graphics, font, Component.translatable("lsu.alliances.detail.pending_badge"), badgeX, nameY + font.lineHeight + 2, true);
         }
      }

      boolean canEdit = canEditSupplier.getAsBoolean();
      boolean canAct = canEdit && entry.member != null && (entry.localMode || !isCurrentPlayer.test(entry.member));
      if (canAct && entry.actionLabel != null && entry.actionButton != null) {
         UiBounds actionBounds = memberActionBounds(x, y, width, entry.height);
         entry.actionButton.renderAt(context, actionBounds, entry.actionLabel);
      }
   }

   private int renderBadge(GuiGraphics graphics, Font font, Component text, int rightX, int y, boolean invite) {
      int textWidth = font.width(text);
      int badgeWidth = textWidth + AllianceDetailStyle.BADGE_PADDING_X * 2;
      int badgeHeight = font.lineHeight + AllianceDetailStyle.BADGE_PADDING_Y * 2;
      int badgeX = rightX - badgeWidth;
      int badgeY = y - AllianceDetailStyle.BADGE_PADDING_Y;
      int bgColor = invite ? AllianceDetailStyle.INVITE_BADGE_BG_COLOR : AllianceDetailStyle.BADGE_BG_COLOR;
      int borderColor = invite ? AllianceDetailStyle.INVITE_BADGE_BORDER_COLOR : AllianceDetailStyle.BADGE_BORDER_COLOR;
      int textColor = invite ? AllianceDetailStyle.TEXT_INVITE : AllianceDetailStyle.TEXT_MUTED;

      graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, bgColor);
      graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 1, borderColor);
      graphics.fill(badgeX, badgeY + badgeHeight - 1, badgeX + badgeWidth, badgeY + badgeHeight, borderColor);
      graphics.fill(badgeX, badgeY, badgeX + 1, badgeY + badgeHeight, borderColor);
      graphics.fill(badgeX + badgeWidth - 1, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, borderColor);

      int textX = badgeX + AllianceDetailStyle.BADGE_PADDING_X;
      int textY = badgeY + AllianceDetailStyle.BADGE_PADDING_Y;
      graphics.drawString(font, text, textX, textY, textColor, false);
      return badgeX - AllianceDetailStyle.CARD_SECTION_GAP;
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

      if (!canEditSupplier.getAsBoolean()) {
         return;
      }

      int cursorY = bounds.y() + AllianceDetailStyle.CONTENT_PADDING - scrollOffset;
      int contentWidth = Math.max(bounds.width() - AllianceDetailStyle.CONTENT_PADDING * 2, 0);
      int listX = bounds.x() + AllianceDetailStyle.CONTENT_PADDING;

      for (EntryLayout entry : entries) {
         if (entry.type != EntryType.MEMBER || entry.member == null) {
            cursorY += entry.height + AllianceDetailStyle.ENTRY_GAP;
            continue;
         }

         if (!entry.localMode && isCurrentPlayer.test(entry.member)) {
            cursorY += entry.height + AllianceDetailStyle.ENTRY_GAP;
            continue;
         }

         UiBounds actionBounds = memberActionBounds(listX, cursorY, contentWidth, entry.height);
         if (input.isHovering(actionBounds)) {
            if (entry.pending) {
               onCancelInvite.accept(entry.member);
            } else {
               onRemoveMember.accept(entry.member);
            }
            return;
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

   private enum EntryType {
      MEMBER,
      SECTION,
      STATUS
   }

   private record EntryLayout(EntryType type, Component title, AllianceMember member, Component name, boolean pending,
                              boolean admin, boolean localMode, int height, Component actionLabel,
                              InlineActionButton actionButton) {

      private static EntryLayout section(Component title, Font font) {
         int height = font.lineHeight + AllianceDetailStyle.ENTRY_PADDING;
         return new EntryLayout(EntryType.SECTION, title, null, null, false, false, false, height, null, null);
      }

      private static EntryLayout status(Component title, Font font) {
         int height = font.lineHeight;
         return new EntryLayout(EntryType.STATUS, title, null, null, false, false, false, height, null, null);
      }

      private static EntryLayout member(AllianceMember member, boolean pending, boolean localMode, Font font) {
         int height = localMode
                 ? AllianceDetailStyle.ENTRY_PADDING * 2 + font.lineHeight
                 : AllianceDetailStyle.ENTRY_PADDING * 2 + font.lineHeight * 2 + 2;
         boolean admin = !localMode && member.hasAdminPermissions();
         Component name = Component.literal(member.cachedName());
         Component actionLabel = pending
                 ? Component.translatable("lsu.alliances.detail.cancel_invite")
                 : Component.translatable("lsu.alliances.detail.remove_member");
         InlineActionButton actionButton = new InlineActionButton();

         return new EntryLayout(EntryType.MEMBER, null, member, name, pending, admin, localMode, height, actionLabel, actionButton);
      }
   }

   private static final class InlineActionButton {
      private void renderAt(UiContext context, UiBounds bounds, Component label) {
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

   private static UiBounds memberActionBounds(int x, int y, int width, int height) {
      int buttonWidth = AllianceDetailStyle.BUTTON_SMALL_WIDTH;
      int buttonHeight = AllianceDetailStyle.BUTTON_HEIGHT;
      int buttonX = x + width - AllianceDetailStyle.ENTRY_PADDING - buttonWidth;
      int buttonY = y + (height - buttonHeight) / 2;
      return new UiBounds(buttonX, buttonY, buttonWidth, buttonHeight);
   }
}
