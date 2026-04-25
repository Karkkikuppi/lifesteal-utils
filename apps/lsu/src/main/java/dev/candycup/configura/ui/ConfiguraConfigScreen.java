package dev.candycup.configura.ui;

import dev.candycup.configura.core.ToggleGroup;
import dev.candycup.lifestealutils.interapi.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.fabricmc.loader.api.FabricLoader;
//? if >1.21.8 {
import net.minecraft.client.input.MouseButtonEvent;
//? }
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ConfiguraConfigScreen extends Screen {
   private static final int SIDEBAR_WIDTH = 182;
   private static final int HEADER_HEIGHT = 20;
   private static final int SIDEBAR_ROW_HEIGHT = 18;
   private static final int SIDEBAR_STROKE_WIDTH = 2;
   private static final int SIDEBAR_INNER_LEFT_PADDING = 4;
   private static final int SIDEBAR_INNER_RIGHT_PADDING = 2;
   private static final int SIDEBAR_SCROLLBAR_WIDTH = 6;
   private static final int SIDEBAR_SCROLLBAR_GAP = 2;
   private static final int SIDEBAR_SCROLL_PIXELS_PER_STEP = 20;
   private static final int SIDEBAR_TOP = 24;
   private static final int SIDEBAR_BOTTOM_PADDING = 30;
   private static final int SIDEBAR_SECTION_GAP = 8;
   private static final int ENTRY_ICON_BASE_SIZE = 16;
   private static final int ENTRY_ICON_RENDER_SIZE = 24;
   private static final int ENTRY_ICON_SLOT_WIDTH = 32;
   private static final int ENTRY_ICON_TEXT_GAP = 4;
   private static final int ENTRY_TITLE_DESC_GAP = 5;
   private static final int ENTRY_CONTROL_GAP = 8;
   private static final int ENTRY_BOTTOM_GAP = 14;
   private static final int CONTENT_WIDTH_SCALE_NUMERATOR = 2;
   private static final int CONTENT_WIDTH_SCALE_DENOMINATOR = 3;
   private static final int CONTENT_WIDTH_REDUCTION_NUMERATOR = 4;
   private static final int CONTENT_WIDTH_REDUCTION_DENOMINATOR = 5;
   private static final int PANEL_GAP = 8;
   private static final int CONTENT_PANEL_TOP = 24;
   private static final int CONTENT_PANEL_BOTTOM_PADDING = 30;
   private static final int CONTENT_HORIZONTAL_PADDING = 10;
   private static final int CONTENT_ROW_LEFT_PADDING = 18;
   private static final int CONTENT_DESCRIPTION_WIDTH_PERCENT = 60;
   private static final int CONTENT_HEADER_TOP_PADDING = 14;
   private static final int CONTENT_FEATURE_HEADER_SIDE_PADDING = 18;
   private static final int CONTENT_FEATURE_TITLE_DESC_GAP = 7;
   private static final int CONTENT_FEATURE_HEADER_BOTTOM_GAP = 22;
   private static final int CONTENT_HEADER_BOTTOM_GAP = 7;
   private static final int CONTENT_ROWS_BOTTOM_PADDING = 8;
   private static final int CONTENT_SCROLLBAR_WIDTH = 6;
   private static final int CONTENT_SCROLLBAR_GAP = 6;
   private static final int SCROLL_PIXELS_PER_STEP = 20;
   private static final int PREVIEW_TOP_GAP = 6;
   private static final int PREVIEW_BOTTOM_GAP = 6;
   private static final int PREVIEW_HORIZONTAL_PADDING = 4;
   private static final int PREVIEW_MIN_HEIGHT = 22;
   private static final int LIST_PREVIEW_MAX_LINES = 4;
   private static final int ACCORDION_HEADER_HEIGHT = 20;
   private static final int ACCORDION_HEADER_BOTTOM_GAP = 6;
   private static final int ACCORDION_HEADER_STROKE_WIDTH = 2;
   private static final int ACCORDION_CONTENT_INDENT = 12;

   private final Screen parent;
   private final ConfiguraConfigModel.ResolvedConfig resolved;

   private final List<RowControl> controls = new ArrayList<>();
   private final List<RowLayout> rowLayouts = new ArrayList<>();
   private final List<ToggleEntryLabel> toggleEntryLabels = new ArrayList<>();
   private final List<SidebarHitbox> sidebarHitboxes = new ArrayList<>();
   private final List<AccordionHeaderLayout> accordionHeaderLayouts = new ArrayList<>();
   private final List<AccordionHeaderHitbox> accordionHeaderHitboxes = new ArrayList<>();
   private final Set<String> openAccordionIds = new LinkedHashSet<>();
   private final Map<String, Object> initialValues = new LinkedHashMap<>();
   private final Set<String> dirtyKeys = new LinkedHashSet<>();
   private final String modVersion;

   private Button saveButton;
   private Button resetDefaultsButton;
   private ConfiguraConfigModel.UiCategory selectedCategory;
   private ConfiguraConfigModel.UiFeature selectedFeature;
   private int contentRowsHeight;
   private int contentScrollOffset;
   private int maxContentScroll;
   private int sidebarRowsHeight;
   private int sidebarScrollOffset;
   private int maxSidebarScroll;
   private String inlineEditorKey;
   private ConfiguraConfigModel.OptionType inlineEditorType;
   private String inlineEditorDraft = "";
   private MultiLineEditBox inlineEditor;
   private Button inlineSaveButton;
   private Button inlineCancelButton;
   private int inlineEditorX;
   private int inlineEditorBaseY;
   private int inlineEditorWidth;
   private int inlineEditorHeight;
   private int inlineSaveBaseY;
   private int inlineCancelBaseY;
   private boolean resetDefaultsConfirmArmed;

   public ConfiguraConfigScreen(Screen parent, ConfiguraConfigModel.ResolvedConfig resolved) {
      super(resolved.title());
      this.parent = parent;
      this.resolved = resolved;
      this.modVersion = resolveModVersion();
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.controls.clear();
      this.rowLayouts.clear();
      this.toggleEntryLabels.clear();
      this.sidebarHitboxes.clear();
      this.accordionHeaderLayouts.clear();
      this.accordionHeaderHitboxes.clear();
      clearInlineWidgetReferences();

      if (selectedCategory == null && !resolved.categories().isEmpty()) {
         selectedCategory = resolved.categories().getFirst();
      }
      if (selectedCategory != null && (selectedFeature == null || selectedCategory.features().stream().noneMatch(feature -> feature == selectedFeature))) {
         selectedFeature = selectedCategory.features().isEmpty() ? null : selectedCategory.features().getFirst();
      }
      if (inlineEditorKey != null && !selectedFeatureContainsKey(inlineEditorKey)) {
         closeInlineEditorState();
      }

      ScreenLayout layout = computeLayout();
      rebuildSidebarHitboxes(layout);
      rebuildControls(layout);
      clampSidebarScroll(layout);
      clampContentScroll(layout);
      updateControlsForScroll(layout);

      this.saveButton = Button.builder(Component.translatable("lsu.config.configura.save"), button -> saveNow())
              .pos(this.width - 210, this.height - 24)
              .size(100, 20)
              .build();
      this.saveButton.active = !dirtyKeys.isEmpty();
      this.addRenderableWidget(this.saveButton);

      this.resetDefaultsButton = Button.builder(Component.literal("Reset to default"), button -> onResetDefaultsClicked())
              .pos(this.width - 315, this.height - 24)
              .size(100, 20)
              .build();
      this.addRenderableWidget(this.resetDefaultsButton);

      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
                 saveNow();
                 onClose();
              })
              .pos(this.width - 105, this.height - 24)
              .size(100, 20)
              .build());
   }

   private void rebuildSidebarHitboxes(ScreenLayout layout) {
      this.sidebarRowsHeight = 0;
      this.maxSidebarScroll = 0;

      int sectionX = layout.sidebarRowsX;
      int sectionWidth = layout.sidebarRowsWidth;
      int y = SIDEBAR_TOP;
      for (ConfiguraConfigModel.UiCategory category : resolved.categories()) {
         sidebarHitboxes.add(new SidebarHitbox(sectionX, y, sectionWidth, HEADER_HEIGHT, category, null));
         y += HEADER_HEIGHT;
         for (ConfiguraConfigModel.UiFeature feature : category.features()) {
            sidebarHitboxes.add(new SidebarHitbox(sectionX, y, sectionWidth, SIDEBAR_ROW_HEIGHT, category, feature));
            y += SIDEBAR_ROW_HEIGHT;
         }
         y += SIDEBAR_SECTION_GAP;
      }
      this.sidebarRowsHeight = Math.max(0, y - SIDEBAR_TOP);
   }

   private void rebuildControls(ScreenLayout layout) {
      this.contentRowsHeight = 0;
      this.contentScrollOffset = 0;
      this.maxContentScroll = 0;
      if (selectedFeature == null) {
         return;
      }

      int iconX = layout.contentX + CONTENT_ROW_LEFT_PADDING;
      int contentTextRight = layout.scrollbarX - CONTENT_SCROLLBAR_GAP;
      int textX = iconX + ENTRY_ICON_SLOT_WIDTH + ENTRY_ICON_TEXT_GAP;
      int textWidth = Math.max(80, contentTextRight - textX);
      int descriptionWidth = Math.max(80, (layout.contentWidth() * CONTENT_DESCRIPTION_WIDTH_PERCENT) / 100);
      int rowsTop = contentRowsTop(layout);
      int y = rowsTop;

      for (ConfiguraConfigModel.UiFeatureItem item : selectedFeature.items()) {
         switch (item) {
            case ConfiguraConfigModel.UiFeatureConfigurable c -> {
               y = buildRowForConfigurable(c.configurable(), layout, iconX, textX, textWidth, descriptionWidth, y);
            }
            case ConfiguraConfigModel.UiFeatureAccordion a -> {
               y = buildAccordionHeader(a.accordion(), layout, iconX, textWidth, y);
               if (openAccordionIds.contains(scopedAccordionId(a.accordion().id()))) {
                  int indentedIconX = iconX + ACCORDION_CONTENT_INDENT;
                  int indentedTextX = textX + ACCORDION_CONTENT_INDENT;
                  int indentedTextWidth = Math.max(40, textWidth - ACCORDION_CONTENT_INDENT);
                  for (ConfiguraConfigModel.UiConfigurable configurable : a.accordion().configurables()) {
                     y = buildRowForConfigurable(configurable, layout, indentedIconX, indentedTextX, indentedTextWidth, descriptionWidth, y);
                  }
               }
            }
         }
      }

      this.contentRowsHeight = Math.max(0, y - rowsTop);
   }

   private int buildAccordionHeader(ConfiguraConfigModel.UiAccordion accordion, ScreenLayout layout, int iconX, int textWidth, int y) {
      int headerWidth = textWidth + ENTRY_ICON_SLOT_WIDTH + ENTRY_ICON_TEXT_GAP;
      String scopedId = scopedAccordionId(accordion.id());
      accordionHeaderLayouts.add(new AccordionHeaderLayout(accordion, scopedId, y, iconX, headerWidth));
      accordionHeaderHitboxes.add(new AccordionHeaderHitbox(accordion, scopedId, y, ACCORDION_HEADER_HEIGHT, iconX, headerWidth));
      return y + ACCORDION_HEADER_HEIGHT + ACCORDION_HEADER_BOTTOM_GAP;
   }

   private int buildRowForConfigurable(
           ConfiguraConfigModel.UiConfigurable configurable,
           ScreenLayout layout,
           int iconX, int textX, int textWidth, int descriptionWidth,
           int y
   ) {
      ensureInitialTracked(configurable);

      List<FormattedCharSequence> descLines = this.font.split(configurable.description(), descriptionWidth);
      int descLineCount = Math.max(1, descLines.size());
      List<FormattedCharSequence> disabledMessageLines = List.of();
      int disabledMessageY = -1;
      if (configurable.remotelyForced() && configurable.disabledMessage() != null && !configurable.disabledMessage().getString().isBlank()) {
         Component disabledLine = Component.literal("⚠ ").append(configurable.disabledMessage().copy().withStyle(ChatFormatting.BOLD));
         disabledMessageLines = this.font.split(disabledLine, descriptionWidth);
         if (!disabledMessageLines.isEmpty()) {
            disabledMessageY = y + this.font.lineHeight + ENTRY_TITLE_DESC_GAP + descLineCount * this.font.lineHeight + ENTRY_TITLE_DESC_GAP;
         }
      }
      int titleY = y;
      int descY = titleY + this.font.lineHeight + ENTRY_TITLE_DESC_GAP;
      int messageBlockHeight = disabledMessageLines.isEmpty() ? 0 : ENTRY_TITLE_DESC_GAP + disabledMessageLines.size() * this.font.lineHeight;
      int contentHeight = this.font.lineHeight + ENTRY_TITLE_DESC_GAP + descLineCount * this.font.lineHeight + messageBlockHeight;
      int iconRenderSize = ENTRY_ICON_RENDER_SIZE;
      int iconY = titleY + Math.max(0, (contentHeight - iconRenderSize) / 2);
      int detailsBottom = descY + descLineCount * this.font.lineHeight + messageBlockHeight;
      boolean inlineOpen = isInlineEditorOpen(configurable);
      Object previewValue = configurable.readValue();
      int previewY = -1;
      int previewHeight = 0;
      int listPreviewLineCount = 0;
      if (configurable.type() == ConfiguraConfigModel.OptionType.MINIMESSAGE) {
         if (inlineOpen) {
            previewValue = inlineEditorDraft;
         }
         previewY = detailsBottom + PREVIEW_TOP_GAP;
         previewHeight = PREVIEW_MIN_HEIGHT;
      } else if (configurable.type() == ConfiguraConfigModel.OptionType.LIST) {
         if (inlineOpen) {
            previewValue = parseListDraft(inlineEditorDraft);
         }
         previewY = detailsBottom + PREVIEW_TOP_GAP;
         listPreviewLineCount = listPreviewLineCount(previewValue);
         previewHeight = Math.max(PREVIEW_MIN_HEIGHT, PREVIEW_TOP_GAP + listPreviewLineCount * this.font.lineHeight + PREVIEW_BOTTOM_GAP);
      }

      int controlY = previewHeight > 0
              ? previewY + previewHeight + PREVIEW_BOTTOM_GAP
              : detailsBottom + ENTRY_CONTROL_GAP;

      RowLayout rowLayout = new RowLayout(configurable, iconX, iconY, iconRenderSize, textX, titleY, descY, disabledMessageY, textWidth, descLines, disabledMessageLines, previewY, previewHeight, listPreviewLineCount);
      rowLayouts.add(rowLayout);

      int controlWidth = Math.min(230, textWidth);
      Button action = null;

      switch (configurable.type()) {
         case BOOLEAN -> {
            boolean current = (Boolean) configurable.readValue();
            action = Button.builder(toggleLabel(current), button -> {
                       boolean next = !((Boolean) configurable.readValue());
                       configurable.writeValue(next);
                       updateDirtyState(configurable);
                       button.setMessage(toggleLabel(next));
                    })
                    .size(90, 20)
                    .pos(textX, controlY)
                    .build();
         }
         case STRING -> {
            EditBox box = new EditBox(this.font, textX, controlY, controlWidth, 20, configurable.displayName());
            box.setValue(Objects.toString(configurable.readValue(), ""));
            box.setResponder(value -> {
               configurable.writeValue(value);
               updateDirtyState(configurable);
            });
            box.setEditable(!configurable.remotelyForced());
            this.addRenderableWidget(box);
            this.controls.add(new RowControl(configurable, null, box, null, textX, controlY, controlWidth, 20));
            return controlY + 20 + ENTRY_BOTTOM_GAP;
         }
         case MINIMESSAGE -> {
            if (!inlineOpen) {
               action = Button.builder(Component.translatable("lsu.config.configura.edit"), button -> {
                          openInlineEditor(configurable);
                          init();
                       })
                       .size(110, 20)
                       .pos(textX, controlY)
                       .build();
            }
         }
         case FLOAT -> {
            float currentFloat = ((Number) configurable.readValue()).floatValue();
            ConfiguraFloatSlider slider = new ConfiguraFloatSlider(
                    textX, controlY, controlWidth, 20,
                    currentFloat, configurable.min(), configurable.max(),
                    value -> {
                       configurable.writeValue(value);
                       updateDirtyState(configurable);
                    });
            slider.active = !configurable.remotelyForced();
            this.addRenderableWidget(slider);
            this.controls.add(new RowControl(configurable, null, null, slider, textX, controlY, controlWidth, 20));
            return controlY + 20 + ENTRY_BOTTOM_GAP;
         }
         case ENUM -> {
            List<? extends Enum<?>> values = configurable.enumValues();
            action = Button.builder(configurable.enumLabel((Enum<?>) configurable.readValue()), button -> {
                       Enum<?> current = (Enum<?>) configurable.readValue();
                       int index = values.indexOf(current);
                       int nextIndex = (index + 1) % values.size();
                       Enum<?> next = values.get(nextIndex);
                       configurable.writeValue(next);
                       updateDirtyState(configurable);
                       button.setMessage(configurable.enumLabel(next));
                    })
                    .size(Math.min(160, textWidth), 20)
                    .pos(textX, controlY)
                    .build();
         }
         case LIST -> {
            if (!inlineOpen) {
               action = Button.builder(Component.translatable("lsu.config.configura.edit_list"), button -> {
                          openInlineEditor(configurable);
                          init();
                       })
                       .size(120, 20)
                       .pos(textX, controlY)
                       .build();
            }
         }
         case TOGGLE_GROUP -> {
            ToggleGroup group = (ToggleGroup) configurable.readValue();
            int subY = controlY;
            int btnWidth = 90;
            for (ConfiguraConfigModel.UiToggleEntry entry : configurable.toggleEntries()) {
               boolean currentVal = group.get(entry.key());
               int labelWidth = this.font.width(entry.displayName());
               boolean splitRow = labelWidth + 8 + btnWidth > textWidth;
               final String entryKey = entry.key();
               int btnX;
               int btnY;
               if (splitRow) {
                  toggleEntryLabels.add(new ToggleEntryLabel(entry.displayName(), textX, subY));
                  btnX = textX;
                  btnY = subY + this.font.lineHeight + 4;
               } else {
                  toggleEntryLabels.add(new ToggleEntryLabel(entry.displayName(), textX, subY + (20 - this.font.lineHeight) / 2));
                  btnX = textX + textWidth - btnWidth;
                  btnY = subY;
               }
               Button entryBtn = Button.builder(toggleLabel(currentVal), button -> {
                          ToggleGroup tg = (ToggleGroup) configurable.readValue();
                          boolean next = !tg.get(entryKey);
                          tg.set(entryKey, next);
                          configurable.writeValue(tg);
                          updateDirtyState(configurable);
                          button.setMessage(toggleLabel(next));
                       })
                       .size(btnWidth, 20)
                       .pos(btnX, btnY)
                       .build();
               entryBtn.active = !configurable.remotelyForced();
               this.addRenderableWidget(entryBtn);
               this.controls.add(new RowControl(configurable, entryBtn, null, null, btnX, btnY, btnWidth, 20));
               subY += splitRow ? this.font.lineHeight + 4 + 20 + 2 : 22;
            }
            return subY + ENTRY_BOTTOM_GAP;
         }
         default -> throw new IllegalStateException("Unknown type " + configurable.type());
      }

      int rowBottom = detailsBottom;
      if (action != null) {
         action.active = !configurable.remotelyForced();
         this.addRenderableWidget(action);
         this.controls.add(new RowControl(configurable, action, null, null, textX, controlY, action.getWidth(), action.getHeight()));
         rowBottom = controlY + 20;
      } else if (previewHeight > 0) {
         rowBottom = previewY + previewHeight;
      }
      if (isInlineEditorOpen(configurable) && !configurable.remotelyForced()) {
         int editorTop = rowBottom + PREVIEW_BOTTOM_GAP;
         int editorWidth = Math.max(120, textWidth);
         int editorHeight = configurable.type() == ConfiguraConfigModel.OptionType.MINIMESSAGE ? 64 : 72;

         MultiLineEditBox expandedEditor = MultiLineEditBox.builder().build(this.font, editorWidth, editorHeight, Component.empty());
         expandedEditor.setX(textX);
         expandedEditor.setY(editorTop);
         expandedEditor.setValue(inlineEditorDraft);
         expandedEditor.setCharacterLimit(configurable.type() == ConfiguraConfigModel.OptionType.MINIMESSAGE ? 4096 : 8192);
         this.addRenderableWidget(expandedEditor);

         int editorButtonsY = editorTop + editorHeight + 6;
         Button inlineSave = Button.builder(Component.translatable("lsu.config.configura.save"), button -> commitInlineEditor())
                 .size(72, 20)
                 .pos(textX, editorButtonsY)
                 .build();
         Button inlineCancel = Button.builder(CommonComponents.GUI_CANCEL, button -> {
                    closeInlineEditorState();
                    init();
                 })
                 .size(72, 20)
                 .pos(textX + 78, editorButtonsY)
                 .build();
         this.addRenderableWidget(inlineSave);
         this.addRenderableWidget(inlineCancel);

         this.inlineEditor = expandedEditor;
         this.inlineSaveButton = inlineSave;
         this.inlineCancelButton = inlineCancel;
         this.inlineEditorX = textX;
         this.inlineEditorBaseY = editorTop;
         this.inlineEditorWidth = editorWidth;
         this.inlineEditorHeight = editorHeight;
         this.inlineSaveBaseY = editorButtonsY;
         this.inlineCancelBaseY = editorButtonsY;
         rowBottom = editorButtonsY + 20;
      }
      int nextY = rowBottom + ENTRY_BOTTOM_GAP;
      if (rowLayout.previewHeight > 0 && action != null) {
         nextY = Math.max(nextY, rowLayout.previewY + rowLayout.previewHeight + PREVIEW_BOTTOM_GAP + 20 + ENTRY_BOTTOM_GAP);
      }
      return nextY;
   }

   private String scopedAccordionId(String accordionId) {
      if (selectedCategory == null || selectedFeature == null) {
         return accordionId;
      }
      return selectedCategory.id() + "." + selectedFeature.id() + "." + accordionId;
   }

   private static Component toggleLabel(boolean enabled) {
      return Component.translatable(enabled ? "lsu.config.configura.toggle.on" : "lsu.config.configura.toggle.off");
   }

   private Component resetDefaultsLabel() {
      if (resetDefaultsConfirmArmed) {
         return Component.literal("Reset entire config?").withStyle(ChatFormatting.GOLD);
      }
      return Component.literal("Reset to default");
   }

   private void onResetDefaultsClicked() {
      if (!resetDefaultsConfirmArmed) {
         resetDefaultsConfirmArmed = true;
         if (resetDefaultsButton != null) {
            resetDefaultsButton.setMessage(resetDefaultsLabel());
         }
         return;
      }

      this.resolved.onResetToDefaults().run();
      this.resolved.onResetFeedback().run();
      this.dirtyKeys.clear();
      this.initialValues.clear();
      this.resetDefaultsConfirmArmed = false;
      closeInlineEditorState();
      init();
   }

   private void ensureInitialTracked(ConfiguraConfigModel.UiConfigurable configurable) {
      initialValues.computeIfAbsent(configurable.key(), ignored -> snapshot(configurable.readValue()));
   }

   private static Object snapshot(Object value) {
      if (value instanceof List<?> list) {
         return new ArrayList<>(list);
      }
      return value;
   }

   private void updateDirtyState(ConfiguraConfigModel.UiConfigurable configurable) {
      Object initial = initialValues.get(configurable.key());
      Object current = snapshot(configurable.readValue());
      if (Objects.equals(initial, current)) {
         dirtyKeys.remove(configurable.key());
      } else {
         dirtyKeys.add(configurable.key());
      }
      if (saveButton != null) {
         saveButton.active = !dirtyKeys.isEmpty();
      }
   }

   private boolean selectedFeatureContainsKey(String key) {
      if (selectedFeature == null || key == null) {
         return false;
      }
      return selectedFeature.allConfigurables().stream().anyMatch(configurable -> Objects.equals(configurable.key(), key));
   }

   private boolean isInlineEditorOpen(ConfiguraConfigModel.UiConfigurable configurable) {
      return configurable != null
              && inlineEditorKey != null
              && Objects.equals(inlineEditorKey, configurable.key())
              && (inlineEditorType == ConfiguraConfigModel.OptionType.MINIMESSAGE || inlineEditorType == ConfiguraConfigModel.OptionType.LIST);
   }

   private void openInlineEditor(ConfiguraConfigModel.UiConfigurable configurable) {
      if (configurable == null) {
         return;
      }
      if (configurable.type() != ConfiguraConfigModel.OptionType.MINIMESSAGE && configurable.type() != ConfiguraConfigModel.OptionType.LIST) {
         return;
      }
      closeInlineEditorState();
      inlineEditorKey = configurable.key();
      inlineEditorType = configurable.type();
      if (inlineEditorType == ConfiguraConfigModel.OptionType.MINIMESSAGE) {
         inlineEditorDraft = Objects.toString(configurable.readValue(), "");
      } else {
         inlineEditorDraft = serializeListDraft(configurable.readValue());
      }
   }

   private static String serializeListDraft(Object value) {
      if (!(value instanceof List<?> list) || list.isEmpty()) {
         return "";
      }
      List<String> lines = new ArrayList<>(list.size());
      for (Object entry : list) {
         lines.add(Objects.toString(entry, ""));
      }
      return String.join("\n", lines);
   }

   private static List<String> parseListDraft(String value) {
      List<String> parsed = new ArrayList<>();
      if (value == null || value.isBlank()) {
         return parsed;
      }
      for (String line : value.split("\\R")) {
         String trimmed = line.trim();
         if (!trimmed.isEmpty()) {
            parsed.add(trimmed);
         }
      }
      return parsed;
   }

   private static int listPreviewLineCount(Object value) {
      if (!(value instanceof List<?> list) || list.isEmpty()) {
         return 1;
      }
      if (list.size() <= LIST_PREVIEW_MAX_LINES - 1) {
         return list.size();
      }
      return LIST_PREVIEW_MAX_LINES;
   }

   private List<Component> listPreviewComponents(Object value, int previewTextWidth) {
      if (!(value instanceof List<?> rawList) || rawList.isEmpty()) {
         return List.of(listPreviewLine("(empty)", previewTextWidth));
      }

      int visibleItems = Math.min(rawList.size(), LIST_PREVIEW_MAX_LINES - 1);
      List<Component> lines = new ArrayList<>();
      for (int i = 0; i < visibleItems; i++) {
         String raw = Objects.toString(rawList.get(i), "").trim();
         if (raw.isEmpty()) {
            raw = "(blank)";
         }
         lines.add(listPreviewLine(raw, previewTextWidth));
      }

      if (rawList.size() > visibleItems) {
         int remaining = rawList.size() - visibleItems;
         lines.add(listPreviewLine("... +" + remaining + " more", previewTextWidth));
      }
      return lines;
   }

   private Component listPreviewLine(String line, int previewTextWidth) {
      int available = Math.max(20, previewTextWidth - this.font.width("* "));
      String clipped = this.font.plainSubstrByWidth(line, available);
      return Component.literal("* ").withStyle(ChatFormatting.DARK_GRAY)
              .append(Component.literal(clipped).withStyle(ChatFormatting.GRAY));
   }

   private void commitInlineEditor() {
      if (inlineEditorKey == null || inlineEditorType == null) {
         return;
      }
      ConfiguraConfigModel.UiConfigurable configurable = findConfigurableByKey(inlineEditorKey);
      if (configurable == null) {
         closeInlineEditorState();
         init();
         return;
      }

      String value = inlineEditor == null ? inlineEditorDraft : inlineEditor.getValue();
      if (inlineEditorType == ConfiguraConfigModel.OptionType.MINIMESSAGE) {
         configurable.writeValue(value);
      } else {
         configurable.writeValue(parseListDraft(value));
      }
      updateDirtyState(configurable);
      closeInlineEditorState();
      init();
   }

   private void closeInlineEditorState() {
      inlineEditorKey = null;
      inlineEditorType = null;
      inlineEditorDraft = "";
      clearInlineWidgetReferences();
   }

   private void clearInlineWidgetReferences() {
      inlineEditor = null;
      inlineSaveButton = null;
      inlineCancelButton = null;
      inlineEditorX = 0;
      inlineEditorBaseY = 0;
      inlineEditorWidth = 0;
      inlineEditorHeight = 0;
      inlineSaveBaseY = 0;
      inlineCancelBaseY = 0;
   }

   private void saveNow() {
      if (dirtyKeys.isEmpty()) {
         return;
      }
      this.resolved.onSave().run();
      for (String key : new ArrayList<>(dirtyKeys)) {
         ConfiguraConfigModel.UiConfigurable configurable = findConfigurableByKey(key);
         if (configurable != null) {
            initialValues.put(key, snapshot(configurable.readValue()));
         }
      }
      this.dirtyKeys.clear();
      if (this.saveButton != null) {
         this.saveButton.active = false;
      }
      this.resetDefaultsConfirmArmed = false;
      if (this.resetDefaultsButton != null) {
         this.resetDefaultsButton.setMessage(resetDefaultsLabel());
      }
      this.resolved.onSavedFeedback().run();
   }

   private ConfiguraConfigModel.UiConfigurable findConfigurableByKey(String key) {
      for (ConfiguraConfigModel.UiCategory category : resolved.categories()) {
         for (ConfiguraConfigModel.UiFeature feature : category.features()) {
            for (ConfiguraConfigModel.UiConfigurable configurable : feature.allConfigurables()) {
               if (Objects.equals(configurable.key(), key)) {
                  return configurable;
               }
            }
         }
      }
      return null;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      ScreenLayout layout = computeLayout();
      guiGraphics.fill(layout.contentX, layout.contentTop, layout.contentRight, layout.contentBottom, 0xAA151515);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      ScreenLayout layout = computeLayout();
      clampSidebarScroll(layout);
      clampContentScroll(layout);
      if (inlineEditor != null) {
         inlineEditorDraft = inlineEditor.getValue();
      }
      updateControlsForScroll(layout);

      super.render(guiGraphics, mouseX, mouseY, partialTick);

      int headingY = 8;
      int headingGap = 6;
      int titleWidth = this.font.width(this.title);
      Component metaText = Component.literal("v" + modVersion + " by Candycup");
      int metaWidth = this.font.width(metaText);
      int totalWidth = titleWidth + headingGap + metaWidth;
      int startX = (this.width - totalWidth) / 2;
      guiGraphics.drawString(this.font, this.title, startX, headingY, 0xFFFFFFFF, true);
      guiGraphics.drawString(this.font, metaText, startX + titleWidth + headingGap, headingY, 0xFF9A9A9A, true);
      renderSidebar(guiGraphics);

      renderFeatureHeader(guiGraphics, layout);

      boolean clipApplied = pushContentClip(guiGraphics, layout);
      for (RowLayout row : rowLayouts) {
         ConfiguraConfigModel.UiConfigurable configurable = row.configurable;
         Component title = configurable.displayName();
         if (dirtyKeys.contains(configurable.key())) {
            title = title.copy().withStyle(ChatFormatting.BOLD);
         }
         int titleColor = configurable.remotelyForced() ? 0xFFFFCC66 : 0xFFD8D8D8;
         int titleY = row.titleY - contentScrollOffset;
         int descriptionY = row.descriptionY - contentScrollOffset;
         int iconY = row.iconY - contentScrollOffset;
         guiGraphics.drawString(this.font, title, row.textX, titleY, titleColor, false);

         int lineY = descriptionY;
         if (row.descriptionLines.isEmpty()) {
            guiGraphics.drawString(this.font, configurable.description(), row.textX, lineY, 0xFF999999, false);
         } else {
            for (FormattedCharSequence line : row.descriptionLines) {
               guiGraphics.drawString(this.font, line, row.textX, lineY, 0xFF999999, false);
               lineY += this.font.lineHeight;
            }
         }

         if (!row.disabledMessageLines.isEmpty()) {
            int disabledY = row.disabledMessageY - contentScrollOffset;
            for (FormattedCharSequence line : row.disabledMessageLines) {
               guiGraphics.drawString(this.font, line, row.textX, disabledY, titleColor, false);
               disabledY += this.font.lineHeight;
            }
         }

         drawScaledItem(guiGraphics, configurable.icon(), row.iconX, iconY, row.iconRenderSize);

         if (row.previewHeight > 0) {
            int previewTop = row.previewY - contentScrollOffset;
            int previewBottom = previewTop + row.previewHeight;
            int previewLeft = row.textX;
            int previewRight = row.textX + row.textWidth;
            guiGraphics.fill(previewLeft, previewTop, previewRight, previewBottom, 0xAA111111);

            int previewTextY = previewTop + PREVIEW_TOP_GAP;
            if (configurable.type() == ConfiguraConfigModel.OptionType.MINIMESSAGE) {
               String previewValue = isInlineEditorOpen(configurable) ? inlineEditorDraft : Objects.toString(configurable.readValue(), "");
               ConfiguraTextPreview preview = ConfiguraUiText.previewMiniMessage(previewValue);
               int color = preview.valid() ? 0xFFFFFFFF : 0xFFFF5555;
               guiGraphics.drawString(this.font, preview.component(), previewLeft + PREVIEW_HORIZONTAL_PADDING, previewTextY, color, false);
            } else if (configurable.type() == ConfiguraConfigModel.OptionType.LIST) {
               int previewTextWidth = Math.max(40, row.textWidth - PREVIEW_HORIZONTAL_PADDING * 2);
               Object previewValue = isInlineEditorOpen(configurable) ? parseListDraft(inlineEditorDraft) : configurable.readValue();
               List<Component> lines = listPreviewComponents(previewValue, previewTextWidth);
               for (int i = 0; i < Math.min(row.listPreviewLineCount, lines.size()); i++) {
                  guiGraphics.drawString(this.font, lines.get(i), previewLeft + PREVIEW_HORIZONTAL_PADDING, previewTextY, 0xFFFFFFFF, false);
                  previewTextY += this.font.lineHeight;
               }
            }
         }
      }
      for (ToggleEntryLabel label : toggleEntryLabels) {
         int renderY = label.baseY() - contentScrollOffset;
         guiGraphics.drawString(this.font, label.text(), label.labelX(), renderY, 0xFFB8B8B8, false);
      }
      for (AccordionHeaderLayout header : accordionHeaderLayouts) {
         int renderY = header.baseY() - contentScrollOffset;
         boolean open = openAccordionIds.contains(header.scopedId());
         String prefix = open ? "▲ " : "▶ ";
         Component label = Component.literal(prefix).append(header.accordion().displayName());
         guiGraphics.fill(header.x(), renderY, header.x() + header.width(), renderY + ACCORDION_HEADER_HEIGHT, 0xAA2A2A2A);
         guiGraphics.fill(header.x(), renderY, header.x() + ACCORDION_HEADER_STROKE_WIDTH, renderY + ACCORDION_HEADER_HEIGHT, 0xFF8A8A8A);
         int textY = renderY + (ACCORDION_HEADER_HEIGHT - this.font.lineHeight) / 2;
         guiGraphics.drawString(this.font, label, header.x() + ACCORDION_HEADER_STROKE_WIDTH + 4, textY, 0xFFE0E0E0, false);
      }
      popContentClip(guiGraphics, clipApplied);
      renderContentScrollbar(guiGraphics, layout);
   }

   private void renderSidebar(GuiGraphics guiGraphics) {
      ScreenLayout layout = computeLayout();
      boolean clipApplied = pushSidebarClip(guiGraphics, layout);
      for (SidebarHitbox hitbox : sidebarHitboxes) {
         int renderY = hitbox.y - sidebarScrollOffset;
         if (renderY + hitbox.height <= layout.sidebarRowsTop || renderY >= layout.sidebarRowsBottom) {
            continue;
         }
         if (hitbox.feature == null) {
            guiGraphics.fillGradient(hitbox.x, renderY, hitbox.x + hitbox.width, renderY + hitbox.height, 0xCC3F3F46, 0xCC232329);
            guiGraphics.fill(hitbox.x, renderY, hitbox.x + SIDEBAR_STROKE_WIDTH, renderY + hitbox.height, 0xFF7DD3FC);
            int textY = renderY + (hitbox.height - this.font.lineHeight) / 2;
            guiGraphics.drawString(this.font, hitbox.category.displayName(), hitbox.x + SIDEBAR_STROKE_WIDTH + 6, textY, 0xFFFFFFFF);
         } else {
            boolean selected = selectedFeature == hitbox.feature;
            int background = selected ? 0xCC1F3A4A : 0xAA1B1B1F;
            int textColor = selected ? 0xFFFFFFFF : 0xFFD0D0D0;
            guiGraphics.fill(hitbox.x, renderY, hitbox.x + hitbox.width, renderY + hitbox.height, background);
            if (selected) {
               guiGraphics.fill(hitbox.x, renderY, hitbox.x + SIDEBAR_STROKE_WIDTH, renderY + hitbox.height, 0xFF38BDF8);
            }
            int textY = renderY + (hitbox.height - this.font.lineHeight) / 2;
            guiGraphics.drawString(this.font, hitbox.feature.displayName(), hitbox.x + SIDEBAR_STROKE_WIDTH + 6, textY, textColor);
         }
      }
      popSidebarClip(guiGraphics, clipApplied);
      renderSidebarScrollbar(guiGraphics, layout);
   }

   private ScreenLayout computeLayout() {
      int oldContentWidth = Math.max(220, this.width - SIDEBAR_WIDTH - 30);
      int baseContentWidth = Math.max(220, (oldContentWidth * CONTENT_WIDTH_SCALE_NUMERATOR) / CONTENT_WIDTH_SCALE_DENOMINATOR);
      int contentWidth = Math.max(220, (baseContentWidth * CONTENT_WIDTH_REDUCTION_NUMERATOR) / CONTENT_WIDTH_REDUCTION_DENOMINATOR);
      int totalWidth = SIDEBAR_WIDTH + PANEL_GAP + contentWidth;
      int left = Math.max(4, (this.width - totalWidth) / 2);
      int sidebarX = left;
      int contentX = sidebarX + SIDEBAR_WIDTH + PANEL_GAP;
      int contentRight = Math.min(this.width - 4, contentX + contentWidth);
      int contentTop = CONTENT_PANEL_TOP;
      int contentBottom = this.height - CONTENT_PANEL_BOTTOM_PADDING;
      int contentRowsTop = contentTop + CONTENT_HEADER_TOP_PADDING;
      int contentRowsBottom = contentBottom - CONTENT_ROWS_BOTTOM_PADDING;
      int scrollbarX = contentRight - CONTENT_HORIZONTAL_PADDING - CONTENT_SCROLLBAR_WIDTH;
      int sidebarRowsTop = SIDEBAR_TOP;
      int sidebarRowsBottom = this.height - SIDEBAR_BOTTOM_PADDING;
      int sidebarScrollbarX = sidebarX + SIDEBAR_WIDTH - SIDEBAR_INNER_RIGHT_PADDING - SIDEBAR_SCROLLBAR_WIDTH;
      int sidebarRowsX = sidebarX + SIDEBAR_INNER_LEFT_PADDING;
      int sidebarRowsRight = sidebarScrollbarX - SIDEBAR_SCROLLBAR_GAP;
      int sidebarRowsWidth = Math.max(40, sidebarRowsRight - sidebarRowsX);
      return new ScreenLayout(
              sidebarX,
              sidebarRowsX,
              sidebarRowsWidth,
              sidebarRowsTop,
              sidebarRowsBottom,
              sidebarScrollbarX,
              contentX,
              contentRight,
              contentWidth,
              contentTop,
              contentBottom,
              contentRowsTop,
              contentRowsBottom,
              scrollbarX
      );
   }

   private int contentRowsTop(ScreenLayout layout) {
      return layout.contentRowsTop + getFeatureHeaderHeight(layout.contentWidth());
   }

   private void clampSidebarScroll(ScreenLayout layout) {
      int viewportHeight = Math.max(1, layout.sidebarRowsBottom - layout.sidebarRowsTop);
      this.maxSidebarScroll = Math.max(0, sidebarRowsHeight - viewportHeight);
      this.sidebarScrollOffset = Math.max(0, Math.min(maxSidebarScroll, sidebarScrollOffset));
   }

   private void clampContentScroll(ScreenLayout layout) {
      int viewportHeight = Math.max(1, layout.contentRowsBottom - contentRowsTop(layout));
      this.maxContentScroll = Math.max(0, contentRowsHeight - viewportHeight);
      this.contentScrollOffset = Math.max(0, Math.min(maxContentScroll, contentScrollOffset));
   }

   private void updateControlsForScroll(ScreenLayout layout) {
      int rowsTop = contentRowsTop(layout);
      for (RowControl control : controls) {
         int renderY = control.baseY - contentScrollOffset;
         boolean visible = renderY + control.height > rowsTop && renderY < layout.contentRowsBottom;
         if (control.actionButton != null) {
            control.actionButton.setY(renderY);
            control.actionButton.visible = visible;
            control.actionButton.active = visible && !control.configurable.remotelyForced();
         }
         if (control.input != null) {
            control.input.setY(renderY);
            control.input.setVisible(visible);
            control.input.setEditable(visible && !control.configurable.remotelyForced());
            if (!visible && control.input.isFocused()) {
               control.input.setFocused(false);
            }
         }
         if (control.slider != null) {
            control.slider.setY(renderY);
            control.slider.visible = visible;
            control.slider.active = visible && !control.configurable.remotelyForced();
         }
      }

      if (inlineEditor != null) {
         int editorY = inlineEditorBaseY - contentScrollOffset;
         boolean editorVisible = editorY + inlineEditorHeight > rowsTop && editorY < layout.contentRowsBottom;
         inlineEditor.setY(editorY);
         inlineEditor.visible = editorVisible;
         inlineEditor.active = editorVisible;
         if (!editorVisible && inlineEditor.isFocused()) {
            inlineEditor.setFocused(false);
         }
      }
      if (inlineSaveButton != null) {
         int saveY = inlineSaveBaseY - contentScrollOffset;
         boolean saveVisible = saveY + inlineSaveButton.getHeight() > rowsTop && saveY < layout.contentRowsBottom;
         inlineSaveButton.setY(saveY);
         inlineSaveButton.visible = saveVisible;
         inlineSaveButton.active = saveVisible;
      }
      if (inlineCancelButton != null) {
         int cancelY = inlineCancelBaseY - contentScrollOffset;
         boolean cancelVisible = cancelY + inlineCancelButton.getHeight() > rowsTop && cancelY < layout.contentRowsBottom;
         inlineCancelButton.setY(cancelY);
         inlineCancelButton.visible = cancelVisible;
         inlineCancelButton.active = cancelVisible;
      }
   }

   private boolean pushContentClip(GuiGraphics guiGraphics, ScreenLayout layout) {
      int rowsTop = contentRowsTop(layout);
      if (layout.contentRowsBottom <= rowsTop || layout.contentRight <= layout.contentX) {
         return false;
      }
      guiGraphics.enableScissor(layout.contentX + CONTENT_HORIZONTAL_PADDING, rowsTop, layout.scrollbarX - CONTENT_SCROLLBAR_GAP, layout.contentRowsBottom);
      return true;
   }

   private boolean pushSidebarClip(GuiGraphics guiGraphics, ScreenLayout layout) {
      if (layout.sidebarRowsBottom <= layout.sidebarRowsTop || layout.sidebarRowsWidth <= 0) {
         return false;
      }
      guiGraphics.enableScissor(layout.sidebarRowsX, layout.sidebarRowsTop, layout.sidebarRowsX + layout.sidebarRowsWidth, layout.sidebarRowsBottom);
      return true;
   }

   private static void popContentClip(GuiGraphics guiGraphics, boolean clipApplied) {
      if (!clipApplied) {
         return;
      }
      guiGraphics.disableScissor();
   }

   private static void popSidebarClip(GuiGraphics guiGraphics, boolean clipApplied) {
      if (!clipApplied) {
         return;
      }
      guiGraphics.disableScissor();
   }

   private void renderContentScrollbar(GuiGraphics guiGraphics, ScreenLayout layout) {
      if (maxContentScroll <= 0) {
         return;
      }
      int rowsTop = contentRowsTop(layout);
      int trackX = layout.scrollbarX;
      int trackTop = rowsTop;
      int trackBottom = layout.contentRowsBottom;
      int trackHeight = Math.max(0, trackBottom - trackTop);
      if (trackHeight <= 0) {
         return;
      }

      guiGraphics.fill(trackX, trackTop, trackX + CONTENT_SCROLLBAR_WIDTH, trackBottom, 0x66404040);

      float visibleRatio = Math.min(1.0F, (float) trackHeight / (float) contentRowsHeight);
      int thumbHeight = Math.max(16, Math.round(trackHeight * visibleRatio));
      int thumbTravel = Math.max(0, trackHeight - thumbHeight);
      float progress = (float) contentScrollOffset / (float) maxContentScroll;
      int thumbY = trackTop + Math.round(progress * thumbTravel);
      guiGraphics.fill(trackX, thumbY, trackX + CONTENT_SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xAA9CA3AF);
   }

   private void renderSidebarScrollbar(GuiGraphics guiGraphics, ScreenLayout layout) {
      if (maxSidebarScroll <= 0) {
         return;
      }
      int trackX = layout.sidebarScrollbarX;
      int trackTop = layout.sidebarRowsTop;
      int trackBottom = layout.sidebarRowsBottom;
      int trackHeight = Math.max(0, trackBottom - trackTop);
      if (trackHeight <= 0) {
         return;
      }

      guiGraphics.fill(trackX, trackTop, trackX + SIDEBAR_SCROLLBAR_WIDTH, trackBottom, 0x66404040);

      float visibleRatio = Math.min(1.0F, (float) trackHeight / (float) sidebarRowsHeight);
      int thumbHeight = Math.max(16, Math.round(trackHeight * visibleRatio));
      int thumbTravel = Math.max(0, trackHeight - thumbHeight);
      float progress = (float) sidebarScrollOffset / (float) maxSidebarScroll;
      int thumbY = trackTop + Math.round(progress * thumbTravel);
      guiGraphics.fill(trackX, thumbY, trackX + SIDEBAR_SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xAA9CA3AF);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY == 0) {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }

      ScreenLayout layout = computeLayout();
      int rowsTop = contentRowsTop(layout);
      boolean inRows = mouseX >= layout.contentX && mouseX < layout.contentRight && mouseY >= rowsTop && mouseY < layout.contentRowsBottom;
      if (inRows && maxContentScroll > 0) {
         int delta = (int) Math.round(scrollY * SCROLL_PIXELS_PER_STEP);
         contentScrollOffset = Math.max(0, Math.min(maxContentScroll, contentScrollOffset - delta));
         updateControlsForScroll(layout);
         return true;
      }

      boolean inSidebarRows = mouseX >= layout.sidebarRowsX && mouseX < layout.sidebarRowsX + layout.sidebarRowsWidth
              && mouseY >= layout.sidebarRowsTop && mouseY < layout.sidebarRowsBottom;
      if (inSidebarRows && maxSidebarScroll > 0) {
         int delta = (int) Math.round(scrollY * SIDEBAR_SCROLL_PIXELS_PER_STEP);
         sidebarScrollOffset = Math.max(0, Math.min(maxSidebarScroll, sidebarScrollOffset - delta));
         return true;
      }

      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   private boolean handleAccordionClick(double mouseX, double mouseY) {
      ScreenLayout layout = computeLayout();
      int rowsTop = contentRowsTop(layout);
      if (!(mouseX >= layout.contentX + CONTENT_HORIZONTAL_PADDING
              && mouseX < layout.scrollbarX - CONTENT_SCROLLBAR_GAP
              && mouseY >= rowsTop && mouseY < layout.contentRowsBottom)) {
         return false;
      }
      for (AccordionHeaderHitbox hitbox : accordionHeaderHitboxes) {
         int renderY = hitbox.baseY() - contentScrollOffset;
         if (mouseX >= hitbox.x() && mouseX < hitbox.x() + hitbox.width()
                 && mouseY >= renderY && mouseY < renderY + hitbox.height()) {
            String id = hitbox.scopedId();
            if (openAccordionIds.contains(id)) {
               openAccordionIds.remove(id);
            } else {
               openAccordionIds.add(id);
            }
            SoundUtils.playUiClick();
            init();
            return true;
         }
      }
      return false;
   }

   private void renderFeatureHeader(GuiGraphics guiGraphics, ScreenLayout layout) {
      if (selectedFeature == null || selectedCategory == null) {
         return;
      }

      int titleX = layout.contentX + CONTENT_FEATURE_HEADER_SIDE_PADDING;
      int titleY = layout.contentTop + CONTENT_HEADER_TOP_PADDING;
      guiGraphics.drawString(this.font, selectedFeature.displayName().copy().withStyle(ChatFormatting.BOLD), titleX, titleY, 0xFFFFFFFF, false);

      List<FormattedCharSequence> lines = getFeatureDescriptionLines(layout.contentWidth());
      if (lines.isEmpty()) {
         return;
      }

      int lineY = titleY + this.font.lineHeight + CONTENT_FEATURE_TITLE_DESC_GAP;
      for (FormattedCharSequence line : lines) {
         guiGraphics.drawString(this.font, line, titleX, lineY, 0xFF999999, false);
         lineY += this.font.lineHeight;
      }
   }

   private int getFeatureHeaderHeight(int contentWidth) {
      if (selectedFeature == null || selectedCategory == null) {
         return 0;
      }
      List<FormattedCharSequence> lines = getFeatureDescriptionLines(contentWidth);
      int descriptionHeight = lines.size() * this.font.lineHeight;
      int extraGap = lines.isEmpty() ? 0 : CONTENT_FEATURE_TITLE_DESC_GAP;
      return this.font.lineHeight + extraGap + descriptionHeight + CONTENT_FEATURE_HEADER_BOTTOM_GAP;
   }

   private List<FormattedCharSequence> getFeatureDescriptionLines(int contentWidth) {
      if (selectedFeature == null || selectedCategory == null) {
         return List.of();
      }
      String key = "lsu.config.%s.%s.header_desc".formatted(
              selectedCategory.id().toLowerCase(Locale.ROOT),
              selectedFeature.id().toLowerCase(Locale.ROOT)
      );
      Language language = Language.getInstance();
      if (!language.has(key)) {
         return List.of();
      }
      int width = Math.max(80, contentWidth - CONTENT_FEATURE_HEADER_SIDE_PADDING * 2 - CONTENT_SCROLLBAR_WIDTH - CONTENT_SCROLLBAR_GAP);
      return this.font.split(Component.translatable(key), width);
   }

   private static String resolveModVersion() {
      return FabricLoader.getInstance()
              .getModContainer("lifestealutils")
              .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
              .orElse("unknown");
   }

   private static void drawScaledItem(GuiGraphics guiGraphics, net.minecraft.world.item.ItemStack stack, int x, int y, int size) {
      float scale = (float) size / ENTRY_ICON_BASE_SIZE;
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate(x, y);
      guiGraphics.pose().scale(scale, scale);
      guiGraphics.renderItem(stack, 0, 0);
      guiGraphics.pose().popMatrix();
   }

   //? if >1.21.8 {
   @Override
   public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
      double mouseX = mouseButtonEvent.x();
      double mouseY = mouseButtonEvent.y();
      if (handleAccordionClick(mouseX, mouseY)) {
         return true;
      }
      ScreenLayout layout = computeLayout();
      if (!(mouseX >= layout.sidebarRowsX && mouseX < layout.sidebarRowsX + layout.sidebarRowsWidth
              && mouseY >= layout.sidebarRowsTop && mouseY < layout.sidebarRowsBottom)) {
         return super.mouseClicked(mouseButtonEvent, doubleClick);
      }
      for (SidebarHitbox hitbox : sidebarHitboxes) {
         int renderY = hitbox.y - sidebarScrollOffset;
         if (mouseX >= hitbox.x && mouseX < hitbox.x + hitbox.width && mouseY >= renderY && mouseY < renderY + hitbox.height) {
            if (hitbox.feature == null) {
               closeInlineEditorState();
               selectedCategory = hitbox.category;
               selectedFeature = hitbox.category.features().isEmpty() ? null : hitbox.category.features().getFirst();
            } else {
               closeInlineEditorState();
               selectedCategory = hitbox.category;
               selectedFeature = hitbox.feature;
            }
            SoundUtils.playUiClick();
            init();
            return true;
         }
      }
      return super.mouseClicked(mouseButtonEvent, doubleClick);
   }
   //? } else {
   /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (handleAccordionClick(mouseX, mouseY)) {
         return true;
      }
      ScreenLayout layout = computeLayout();
      if (!(mouseX >= layout.sidebarRowsX && mouseX < layout.sidebarRowsX + layout.sidebarRowsWidth
              && mouseY >= layout.sidebarRowsTop && mouseY < layout.sidebarRowsBottom)) {
         return super.mouseClicked(mouseX, mouseY, button);
      }
       for (SidebarHitbox hitbox : sidebarHitboxes) {
          int renderY = hitbox.y - sidebarScrollOffset;
          if (mouseX >= hitbox.x && mouseX < hitbox.x + hitbox.width && mouseY >= renderY && mouseY < renderY + hitbox.height) {
             if (hitbox.feature == null) {
                selectedCategory = hitbox.category;
                selectedFeature = hitbox.category.features().isEmpty() ? null : hitbox.category.features().getFirst();
             } else {
                selectedCategory = hitbox.category;
               selectedFeature = hitbox.feature;
            }
            init();
            return true;
         }
      }
       return super.mouseClicked(mouseX, mouseY, button);
    }
   *///? }

   private record ToggleEntryLabel(Component text, int labelX, int baseY) {
   }

   private record RowLayout(
           ConfiguraConfigModel.UiConfigurable configurable,
           int iconX,
           int iconY,
           int iconRenderSize,
           int textX,
           int titleY,
           int descriptionY,
           int disabledMessageY,
           int textWidth,
           List<FormattedCharSequence> descriptionLines,
           List<FormattedCharSequence> disabledMessageLines,
           int previewY,
           int previewHeight,
           int listPreviewLineCount
   ) {
   }

   private record AccordionHeaderLayout(
           ConfiguraConfigModel.UiAccordion accordion,
           String scopedId,
           int baseY,
           int x,
           int width
   ) {
   }

   private record AccordionHeaderHitbox(
           ConfiguraConfigModel.UiAccordion accordion,
           String scopedId,
           int baseY,
           int height,
           int x,
           int width
   ) {
   }

   private record SidebarHitbox(
           int x,
           int y,
           int width,
           int height,
           ConfiguraConfigModel.UiCategory category,
           ConfiguraConfigModel.UiFeature feature
   ) {
   }

   private record RowControl(
           ConfiguraConfigModel.UiConfigurable configurable,
           Button actionButton,
           EditBox input,
           AbstractWidget slider,
           int baseX,
           int baseY,
           int width,
           int height
   ) {
   }

   private record ScreenLayout(
           int sidebarX,
           int sidebarRowsX,
           int sidebarRowsWidth,
           int sidebarRowsTop,
           int sidebarRowsBottom,
           int sidebarScrollbarX,
           int contentX,
           int contentRight,
           int contentWidth,
           int contentTop,
           int contentBottom,
           int contentRowsTop,
           int contentRowsBottom,
           int scrollbarX
   ) {
   }

}
