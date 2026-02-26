package dev.candycup.lifestealutils.config.controllers;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.StringControllerElement;

public class MinimessageControllerElement extends StringControllerElement {
   private final MinimessageController controller;
   private MinimessagePopupWidget popupWidget;
   private boolean popupVisible;

   public MinimessageControllerElement(MinimessageController control, YACLScreen screen, Dimension<Integer> dim) {
      super(control, screen, dim, false);
      this.controller = control;
   }

   @Override
   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      if (!isAvailable() || !getDimension().isPointInside((int) mouseX, (int) mouseY)) {
         return false;
      }

      if (!popupVisible) {
         popupVisible = true;
         popupWidget = new MinimessagePopupWidget(controller, screen, getDimension(), this);
         screen.addPopupControllerWidget(popupWidget);
      }

      return true;
   }

   @Override
   public void unfocus() {
      super.unfocus();
      if (popupVisible) {
         screen.clearPopupControllerWidget();
      }
   }

   public void onPopupClosed() {
      popupVisible = false;
      popupWidget = null;
   }
}
