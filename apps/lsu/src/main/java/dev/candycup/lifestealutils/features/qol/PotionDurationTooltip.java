package dev.candycup.lifestealutils.features.qol;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;

public final class PotionDurationTooltip {
    @SerialEntry(comment = "Show the actual potion duration in the tooltip, including your prestige perk boost")
    @ConfigurableBoolean(location = "qol.potions.showactualpotionduration")
    private static boolean showActualPotionDuration = true;

    private PotionDurationTooltip() {
    }

    public static boolean isShowActualPotionDuration() {
        return showActualPotionDuration;
    }

    public static void setShowActualPotionDuration(boolean enabled) {
        showActualPotionDuration = enabled;
    }
}
