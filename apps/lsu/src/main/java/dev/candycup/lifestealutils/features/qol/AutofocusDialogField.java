package dev.candycup.lifestealutils.features.qol;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import lombok.Getter;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

public final class AutofocusDialogField {
    @Getter
    @SerialEntry(comment = "Automatically focus the first text input when a server dialog opens")
    @ConfigurableBoolean(location = "qol.dialogs.autoselectdialogfield")
    private static boolean enabled = true;

    private AutofocusDialogField() {
    }

    public static AbstractWidget firstField(Screen screen) {
        return firstFieldIn(screen);
    }

    private static AbstractWidget firstFieldIn(ContainerEventHandler container) {
        for (GuiEventListener child : container.children()) {
            switch (child) {
                case EditBox editBox -> {
                    return editBox;
                }
                case MultiLineEditBox multiLineEditBox -> {
                    return multiLineEditBox;
                }
                case ContainerEventHandler nested -> {
                    AbstractWidget field = firstFieldIn(nested);
                    if (field != null) {
                        return field;
                    }
                }
                default -> {
                }
            }
        }
        return null;
    }
}
