package dev.candycup.lifestealutils.features.shop;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;

public final class ShopUiFeature {
    @SerialEntry(comment = "Enable the custom Lifesteal shop interface wrapper")
    @ConfigurableBoolean(location = "qol.customuis.customshopinterfaceenabled", icon = "emerald")
    private static boolean customShopInterfaceEnabled = false;

    private ShopUiFeature() {
    }

    public static boolean isCustomShopInterfaceEnabled() {
        return customShopInterfaceEnabled;
    }
}
