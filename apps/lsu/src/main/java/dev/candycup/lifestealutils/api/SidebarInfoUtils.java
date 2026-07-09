package dev.candycup.lifestealutils.api;

public class SidebarInfoUtils {
   protected static int coinBalance = 0;
   protected static int gemBalance = 0;
   protected static int kills = 0;

    public static void updateFromSidebarLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }

        String normalized = line.replaceAll("[^A-Za-z0-9:,]", "").trim();
        if (normalized.startsWith("Coins:")) {
            coinBalance = parseInt(normalized.substring("Coins:".length()));
        } else if (normalized.startsWith("Gems:")) {
            gemBalance = parseInt(normalized.substring("Gems:".length()));
        } else if (normalized.startsWith("Kills:")) {
            kills = parseInt(normalized.substring("Kills:".length()));
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
