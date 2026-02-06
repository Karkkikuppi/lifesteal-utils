package dev.candycup.lifestealutils.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

public final class LifestealServerDetector {
   private static final String HOST_MARKER = "lifesteal.net";

   private LifestealServerDetector() {
   }

   public static boolean isOnLifestealServer() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null) {
         return false;
      }
      ServerData serverData = minecraft.getCurrentServer();
      if (serverData == null) {
         return false;
      }
      String address = serverData.ip;
      if (address == null || address.isBlank()) {
         return false;
      }
      return address.toLowerCase(Locale.ROOT).contains(HOST_MARKER);
   }
}
