package dev.candycup.lifestealutils.features.ah;

import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.RequiresGaia;
import lombok.Getter;

public final class AhSearchAutomation {
    @Getter
    @SerialEntry(comment = "Enable the custom auction house interface overlay GUI")
    @RequiresGaia(forceStateWhenDenied = "false")
    @ConfigurableBoolean(location = "qol.customuis.auctionui", icon = "gold_ingot")
    private static boolean customAhInterfaceEnabled = false;

    private static String pendingQuery;
    private static long pendingQueryExpiresAtMs;
    private static String activeQuery;

    private AhSearchAutomation() {
    }

    public static synchronized void queueSearch(String query) {
        if (query == null) {
            pendingQuery = null;
            pendingQueryExpiresAtMs = 0L;
            return;
        }
        String trimmed = query.trim();
        pendingQuery = trimmed;
        pendingQueryExpiresAtMs = System.currentTimeMillis() + 15000L;
    }

    public static synchronized void setActiveQuery(String query) {
        if (query == null || query.isBlank()) {
            activeQuery = null;
        } else {
            activeQuery = query;
        }
    }

    public static synchronized String getActiveQuery() {
        return activeQuery;
    }

    public static synchronized boolean isSearchActive() {
        return activeQuery != null && !activeQuery.isBlank();
    }

    public static synchronized String getPendingQueryIfValid() {
        if (pendingQuery == null) {
            return null;
        }
        if (System.currentTimeMillis() > pendingQueryExpiresAtMs) {
            pendingQuery = null;
            pendingQueryExpiresAtMs = 0L;
            return null;
        }
        return pendingQuery;
    }

    public static synchronized String consumePendingQuery() {
        String current = getPendingQueryIfValid();
        pendingQuery = null;
        pendingQueryExpiresAtMs = 0L;
        return current;
    }
}
