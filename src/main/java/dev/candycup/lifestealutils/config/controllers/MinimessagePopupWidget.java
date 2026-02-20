package dev.candycup.lifestealutils.config.controllers;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.api.utils.MutableDimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerPopupWidget;
import dev.isxander.yacl3.gui.utils.WidgetUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class MinimessagePopupWidget extends ControllerPopupWidget<MinimessageController> {
   private static final int DIALOG_WIDTH = 360;
   private static final int DIALOG_HEIGHT = 260;
   private static final int PADDING = 8;

   private final MinimessageController controller;
   private final MinimessageControllerElement entryWidget;
   private final MultiLineEditBox editor;

   private MutableDimension<Integer> dialogBounds;
   private MutableDimension<Integer> editorBounds;
   private MutableDimension<Integer> previewBounds;

   public MinimessagePopupWidget(
           MinimessageController control,
           YACLScreen screen,
           Dimension<Integer> dim,
           MinimessageControllerElement entryWidget
   ) {
      super(control, screen, dim, entryWidget);
      this.controller = control;
      this.entryWidget = entryWidget;
      this.editor = MultiLineEditBox.builder().build(this.textRenderer, DIALOG_WIDTH - PADDING * 2, 110, Component.empty());
      this.editor.setCharacterLimit(4096);
      this.editor.setValue(control.getString());
      this.editor.setValueListener(this::onEditorChanged);
      setDimension(dim);
   }

   @Override
   public void setDimension(Dimension<Integer> dim) {
      super.setDimension(dim);

      int x = dim.centerX() - DIALOG_WIDTH / 2;
      int y = dim.centerY() - DIALOG_HEIGHT / 2;

      if (y < screen.tabArea.top() + 4) {
         y = screen.tabArea.top() + 4;
      }
      int maxY = screen.tabArea.bottom() - DIALOG_HEIGHT - 4;
      if (y > maxY) {
         y = maxY;
      }

      dialogBounds = Dimension.ofInt(x, y, DIALOG_WIDTH, DIALOG_HEIGHT);
      int editorTop = y + 28;
      int editorHeight = 110;
      editorBounds = Dimension.ofInt(x + PADDING, editorTop, DIALOG_WIDTH - PADDING * 2, editorHeight);
      previewBounds = Dimension.ofInt(x + PADDING, editorTop + editorHeight + 18, DIALOG_WIDTH - PADDING * 2, DIALOG_HEIGHT - (editorTop + editorHeight + 18 - y) - PADDING);

      editor.setX(editorBounds.x());
      editor.setY(editorBounds.y());
      editor.setWidth(editorBounds.width());
      editor.setHeight(editorBounds.height());
   }

   private void onEditorChanged(String value) {
      controller.setFromString(value);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      int x = dialogBounds.x();
      int y = dialogBounds.y();
      int x2 = dialogBounds.xLimit();
      int y2 = dialogBounds.yLimit();

      graphics.fill(x - 1, y - 1, x2 + 1, y2 + 1, 0xFF000000);
      graphics.fill(x, y, x2, y2, 0xFF2B2B2B);

      graphics.drawString(textRenderer, Component.translatable("lsu.config.minimessage.editor"), x + PADDING, y + 8, 0xFFFFFFFF, true);
      graphics.drawString(textRenderer, Component.translatable("lsu.config.minimessage.preview"), previewBounds.x(), previewBounds.y() - 11, 0xFFBFBFBF, false);

      editor.render(graphics, mouseX, mouseY, delta);

      graphics.fill(previewBounds.x() - 1, previewBounds.y() - 1, previewBounds.xLimit() + 1, previewBounds.yLimit() + 1, 0xFF000000);
      graphics.fill(previewBounds.x(), previewBounds.y(), previewBounds.xLimit(), previewBounds.yLimit(), 0xFF1C1C1C);

      MessagingUtils.MiniMessagePreviewResult result = MessagingUtils.previewMiniMessage(editor.getValue());
      int textX = previewBounds.x() + 4;
      int textY = previewBounds.y() + 4;
      int availableWidth = Math.max(20, previewBounds.width() - 8);
      int maxLines = Math.max(1, (previewBounds.height() - 8) / textRenderer.lineHeight);

      if (!result.valid()) {
         graphics.drawString(
                 textRenderer,
                 Component.translatable("lsu.config.minimessage.invalid").withStyle(ChatFormatting.RED),
                 textX,
                 textY,
                 0xFFFF5555,
                 false
         );
         textY += textRenderer.lineHeight + 2;
         maxLines -= 1;
      }

      List<FormattedCharSequence> lines = textRenderer.split(result.component(), availableWidth);
      for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
         graphics.drawString(textRenderer, lines.get(i), textX, textY + i * textRenderer.lineHeight, 0xFFFFFFFF, false);
      }
   }

   @Override
   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      if (dialogBounds.isPointInside((int) mouseX, (int) mouseY)) {
         if (editorBounds.isPointInside((int) mouseX, (int) mouseY)) {
            editor.setFocused(true);
            WidgetUtils.mouseClicked(editor, mouseX, mouseY, button);
         } else {
            editor.setFocused(false);
         }
         return true;
      }

      if (entryWidget.isMouseOver(mouseX, mouseY)) {
         return true;
      }

      screen.clearPopupControllerWidget();
      return false;
   }

   @Override
   public boolean onMouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
      return WidgetUtils.mouseDragged(editor, mouseX, mouseY, button, dx, dy);
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      editor.mouseMoved(mouseX, mouseY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      return editor.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         screen.clearPopupControllerWidget();
         return true;
      }
      return WidgetUtils.keyPressed(editor, keyCode, scanCode, modifiers);
   }

   @Override
   public boolean onCharTyped(char chr, String cpStr, int modifiers) {
      return WidgetUtils.charTyped(editor, chr, modifiers);
   }

   @Override
   public void close() {
      entryWidget.onPopupClosed();
   }

   @Override
   public Component popupTitle() {
      return Component.translatable("lsu.config.minimessage.title");
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return dialogBounds.isPointInside((int) mouseX, (int) mouseY);
   }
}
