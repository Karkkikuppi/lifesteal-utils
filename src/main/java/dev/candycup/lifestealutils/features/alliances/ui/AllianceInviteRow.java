package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * renders the invite input row using a vanilla edit box.
 */
final class AllianceInviteRow implements Drawable {
   private final EditBox inviteField;
   private final AllianceDetailButton inviteButton;
   private final BooleanSupplier canInviteSupplier;
   private final BooleanSupplier invitingSupplier;

   private UiBounds bounds = UiBounds.empty();

   AllianceInviteRow(
           EditBox inviteField,
           Supplier<Component> inviteLabelSupplier,
           Runnable onInvite,
           BooleanSupplier canInviteSupplier,
           BooleanSupplier invitingSupplier
   ) {
      this.inviteField = inviteField;
      this.canInviteSupplier = canInviteSupplier;
      this.invitingSupplier = invitingSupplier;
      this.inviteButton = AllianceDetailButton.primaryWithWidth(
              inviteLabelSupplier,
              onInvite,
              () -> canInviteSupplier.getAsBoolean() && !invitingSupplier.getAsBoolean(),
              AllianceDetailStyle.BUTTON_INVITE_WIDTH
      );
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
      int availableWidth = bounds.width();
      int fieldWidth = Math.max(availableWidth - AllianceDetailStyle.BUTTON_INVITE_WIDTH - AllianceDetailStyle.TAB_GAP, 0);
      int fieldHeight = AllianceDetailStyle.BUTTON_HEIGHT;

      int fieldX = bounds.x();
      int fieldY = bounds.y() + (bounds.height() - fieldHeight) / 2;
      inviteField.setX(fieldX);
      inviteField.setY(fieldY);
      inviteField.setWidth(fieldWidth);
      inviteField.setHeight(fieldHeight);

      int buttonX = bounds.x() + fieldWidth + AllianceDetailStyle.TAB_GAP;
      int buttonY = bounds.y() + (bounds.height() - AllianceDetailStyle.BUTTON_HEIGHT) / 2;
      inviteButton.layout(layoutContext.withBounds(new UiBounds(buttonX, buttonY, AllianceDetailStyle.BUTTON_INVITE_WIDTH, AllianceDetailStyle.BUTTON_HEIGHT)));

      boolean canInvite = canInviteSupplier.getAsBoolean();
      boolean inviting = invitingSupplier.getAsBoolean();
      inviteField.setEditable(canInvite && !inviting);
      inviteField.setVisible(canInvite);
   }

   @Override
   public void render(UiContext context) {
      if (inviteField.isVisible()) {
         inviteButton.render(context);
      }
   }

   @Override
   public void handleInput(UiInputState input) {
      if (inviteField.isVisible()) {
         inviteButton.handleInput(input);
      }
   }

   @Override
   public UiBounds bounds() {
      return bounds;
   }

   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(layoutContext.availableBounds().width(), AllianceDetailStyle.BUTTON_HEIGHT + AllianceDetailStyle.BUTTON_PADDING_Y * 2);
   }
}
