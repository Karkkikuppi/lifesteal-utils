package dev.candycup.lifestealutils;

import dev.candycup.configura.core.Configura;
import dev.candycup.configura.core.GsonJson5ConfiguraCodec;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigUtils {
    @Getter
    private static boolean applyingRemoteOverrides;
    public static Configura<ConfigUtils> HANDLER = Configura.builder(ConfigUtils.class)
            .containers(dev.candycup.lifestealutils.config.ConfigContainerRegistry.getRegisteredContainers())
            .path(FabricLoader.getInstance().getConfigDir().resolve("lifestealutils.json5"))
            .codec(new GsonJson5ConfiguraCodec(true))
            .build();

    public ConfigUtils() {

    }

    public static void load() {
        FeatureFlagController.ensureLoaded();
        ConfigMigrations.beginSession();
        HANDLER = Configura.builder(ConfigUtils.class)
                .containers(dev.candycup.lifestealutils.config.ConfigContainerRegistry.getRegisteredContainers())
                .path(FabricLoader.getInstance().getConfigDir().resolve("lifestealutils.json5"))
                .codec(new GsonJson5ConfiguraCodec(true))
                .migration(1, ConfigMigrations::applyMigration1)
                .migration(2, ConfigMigrations::applyMigration2)
                .migration(3, ConfigMigrations::applyMigration3)
                .build();
        HANDLER.load();
        if (ConfigMigrations.consumeTouched()) {
            HANDLER.save();
        }
        dev.candycup.lifestealutils.config.ConfigResolver.applyRemoteOverridesAtLoad();
    }

    public static void resetAll() {
        HANDLER.resetToDefaults();
        HANDLER.save();
    }

    public static void runWithRemoteOverrideApplication(Runnable runnable) {
        boolean previous = applyingRemoteOverrides;
        applyingRemoteOverrides = true;
        try {
            runnable.run();
        } finally {
            applyingRemoteOverrides = previous;
        }
    }
}
