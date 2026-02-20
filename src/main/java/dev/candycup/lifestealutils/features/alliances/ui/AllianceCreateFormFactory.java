package dev.candycup.lifestealutils.features.alliances.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

/**
 * creates shared create-alliance form widgets.
 */
final class AllianceCreateFormFactory {
   private AllianceCreateFormFactory() {
   }

   static AllianceCreateFormFields create(
           Font font,
           int nameMaxLength,
           Component nameHint,
           int prefixMaxLength,
           Component prefixHint,
           int colorMaxLength,
           Component colorHint,
           int multilineWidth,
           int descriptionMaxLength,
           int motdMaxLength,
           Runnable onStateChanged
   ) {
      EditBox nameField = new EditBox(font, 0, 0, 0, AllianceEditStyle.FIELD_HEIGHT, Component.empty());
      nameField.setMaxLength(nameMaxLength);
      nameField.setHint(nameHint);

      EditBox prefixField = new EditBox(font, 0, 0, 0, AllianceEditStyle.FIELD_HEIGHT, Component.empty());
      prefixField.setMaxLength(prefixMaxLength);
      prefixField.setHint(prefixHint);

      EditBox colorField = new EditBox(font, 0, 0, 0, AllianceEditStyle.FIELD_HEIGHT, Component.empty());
      colorField.setMaxLength(colorMaxLength);
      colorField.setHint(colorHint);

      MultiLineEditBox descriptionField = MultiLineEditBox.builder().build(font, multilineWidth, AllianceEditStyle.FIELD_HEIGHT_LONG, Component.empty());
      descriptionField.setCharacterLimit(descriptionMaxLength);

      MultiLineEditBox motdField = MultiLineEditBox.builder().build(font, multilineWidth, AllianceEditStyle.FIELD_HEIGHT_LONG, Component.empty());
      motdField.setCharacterLimit(motdMaxLength);

      nameField.setResponder(value -> onStateChanged.run());
      prefixField.setResponder(value -> onStateChanged.run());
      colorField.setResponder(value -> onStateChanged.run());
      descriptionField.setValueListener(value -> onStateChanged.run());
      motdField.setValueListener(value -> onStateChanged.run());

      return new AllianceCreateFormFields(nameField, prefixField, colorField, descriptionField, motdField);
   }

   record AllianceCreateFormFields(EditBox nameField, EditBox prefixField, EditBox colorField,
                                   MultiLineEditBox descriptionField, MultiLineEditBox motdField) {
   }
}
