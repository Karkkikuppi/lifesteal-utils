package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;

import java.util.function.BooleanSupplier;

/**
 * lays out the invite row and members list.
 */
final class AllianceMembersPanel implements Drawable {
   private final AllianceInviteRow inviteRow;
   private final AllianceMembersListView membersList;
   private final BooleanSupplier showInviteSupplier;

   private UiBounds bounds = UiBounds.empty();

   AllianceMembersPanel(AllianceInviteRow inviteRow, AllianceMembersListView membersList, BooleanSupplier showInviteSupplier) {
      this.inviteRow = inviteRow;
      this.membersList = membersList;
      this.showInviteSupplier = showInviteSupplier;
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();

      int cursorY = bounds.y();
      int inviteHeight = showInviteSupplier.getAsBoolean() ? inviteRow.preferredSize(layoutContext).height() : 0;
      if (inviteHeight > 0) {
         UiBounds inviteBounds = new UiBounds(bounds.x(), cursorY, bounds.width(), inviteHeight);
         inviteRow.layout(layoutContext.withBounds(inviteBounds));
         cursorY += inviteHeight + AllianceDetailStyle.PANEL_GAP;
      }

      int listHeight = Math.max(bounds.height() - (cursorY - bounds.y()), 0);
      UiBounds listBounds = new UiBounds(bounds.x(), cursorY, bounds.width(), listHeight);
      membersList.layout(layoutContext.withBounds(listBounds));
   }

   @Override
   public void render(UiContext context) {
      if (showInviteSupplier.getAsBoolean()) {
         inviteRow.render(context);
      }
      membersList.render(context);
   }

   @Override
   public void handleInput(UiInputState input) {
      if (showInviteSupplier.getAsBoolean()) {
         inviteRow.handleInput(input);
      }
      membersList.handleInput(input);
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
