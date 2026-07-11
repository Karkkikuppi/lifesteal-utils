package dev.candycup.lifestealutils.features.titlescreen;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;

public final class CustomPanorama {
    @SerialEntry(comment = "Custom panorama background on the title screen")
    @ConfigurableBoolean(location = "qol.titlescreen.custompanoramaenabled")
    private static boolean customPanoramaEnabled = true;

    private CustomPanorama() {
    }

    public static boolean isCustomPanoramaEnabled() {
        return customPanoramaEnabled;
    }

    public static void setCustomPanoramaEnabled(boolean enabled) {
        customPanoramaEnabled = enabled;
    }
}
