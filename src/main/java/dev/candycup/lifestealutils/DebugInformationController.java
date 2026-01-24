package dev.candycup.lifestealutils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DebugInformationController {
   private DebugInformationController() {
   }

   public static boolean copyBasicInfoToClipboard(Minecraft client) {
      LocalPlayer player = client.player;
      if (player == null) {
         return false;
      }
      String info = buildBasicInfo(player);
      client.keyboardHandler.setClipboard(info);
      return true;
   }

   public static String buildBasicInfo(LocalPlayer player) {
      String username = player.getName().getString();
      String uuid = player.getUUID().toString();

      String mcVersion = getModVersion("minecraft");
      String clientBrand = ClientBrandRetriever.getClientModName();
      String fabricLoaderVersion = getModVersion("fabricloader");
      String fabricApiVersion = getModVersion("fabric-api");
      String javaVersion = System.getProperty("java.version", "unknown");

      String lifestealUtilsVersion = getModVersion("lifestealutils");

      List<String> modLines = FabricLoader.getInstance().getAllMods().stream()
              .map(ModContainer::getMetadata)
              .sorted(Comparator.comparing(metadata -> metadata.getName().toLowerCase(Locale.ROOT)))
              .map(metadata -> "   -> " + metadata.getName() + " (" + metadata.getVersion().getFriendlyString() + ")")
              .collect(Collectors.toList());

      StringBuilder builder = new StringBuilder();
      builder.append(username).append(" (").append(uuid).append(")\n");
      builder.append("Minecraft ").append(mcVersion)
              .append(", ").append(clientBrand)
              .append(", Fabric Loader ").append(fabricLoaderVersion);
      if (!"unknown".equals(fabricApiVersion)) {
         builder.append(", Fabric API ").append(fabricApiVersion);
      }
      builder.append(", Java ").append(javaVersion).append("\n");
      builder.append("Lifesteal Utils ").append(lifestealUtilsVersion).append("\n");
      builder.append("Running mods:\n");
      for (String line : modLines) {
         builder.append(line).append("\n");
      }
      return builder.toString();
   }

   public static String getModVersion(String modId) {
      Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(modId);
      return container.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
   }
}
