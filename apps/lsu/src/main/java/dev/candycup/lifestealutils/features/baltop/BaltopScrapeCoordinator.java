package dev.candycup.lifestealutils.features.baltop;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.RequiresGaia;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * coordinates custom baltop scraping lifecycle and fallback command behavior.
 */
public final class BaltopScrapeCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils");

    private static boolean pendingScrape = false;

    @SerialEntry(comment = "Enable the custom baltop interface that replaces the server's /baltop GUI")
    @RequiresGaia(forceStateWhenDenied = "false")
    @ConfigurableBoolean(location = "qol.customuis.custombaltopinterfaceenabled")
    private static boolean customBaltopInterfaceEnabled = true;

    private BaltopScrapeCoordinator() {
    }

    public static boolean isCustomBaltopInterfaceEnabled() {
        return customBaltopInterfaceEnabled;
    }

    public static void setCustomBaltopInterfaceEnabled(boolean enabled) {
        customBaltopInterfaceEnabled = enabled;
    }

    /**
     * queues the custom baltop interface to open once no other screen is active.
     */
    public static void queueScrape() {
        pendingScrape = true;
    }

    /**
     * handles the /lsu baltop command behavior.
     *
     * @param client the minecraft client
     */
    public static void handleBaltopCommand(Minecraft client) {
        if (customBaltopInterfaceEnabled) {
            queueScrape();
            return;
        }
        if (client.player != null) {
            client.player.connection.sendCommand("baltop");
        }
    }

    /**
     * ticks the coordinator and scraper state machine.
     *
     * @param client the minecraft client
     */
    public static void tick(Minecraft client) {
        tryStartPendingScrape(client);
        BaltopScraper.getInstance().tick();
    }

    private static void tryStartPendingScrape(Minecraft client) {
        if (!pendingScrape || client.screen != null) {
            return;
        }

        pendingScrape = false;
        BaltopScraper.getInstance().startScraping(
                null,
                error -> {
                    LOGGER.warn("Baltop scraping failed: {}", error);
                    MessagingUtils.showTranslated("lsu.baltop.load_failed", MessagingUtils.arg(error, true));
                }
        );
    }
}
