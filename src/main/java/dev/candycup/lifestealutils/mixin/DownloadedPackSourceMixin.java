package dev.candycup.lifestealutils.mixin;

import com.mojang.logging.LogUtils;
import dev.candycup.lifestealutils.features.resources.ResourcePackZipEditor;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.server.packs.repository.Pack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lists resource-pack zip entries before the pack is read and applied.
 */
@Mixin(DownloadedPackSource.class)
public class DownloadedPackSourceMixin {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String LIFESTEAL_ASSETS_HOST = "assets.lifesteal.net";
   private static final Set<UUID> ELIGIBLE_PACK_IDS = ConcurrentHashMap.newKeySet();

   @Inject(method = "pushPack", at = @At("HEAD"))
   private void onPushPack(UUID packId, URL url, String hash, CallbackInfo ci) {
      if (isEligibleLifestealAssetsUrl(url)) {
         ELIGIBLE_PACK_IDS.add(packId);
      } else {
         ELIGIBLE_PACK_IDS.remove(packId);
      }
   }

   @Inject(method = "popPack", at = @At("HEAD"))
   private void onPopPack(UUID packId, CallbackInfo ci) {
      ELIGIBLE_PACK_IDS.remove(packId);
   }

   @Inject(method = "popAll", at = @At("HEAD"))
   private void onPopAll(CallbackInfo ci) {
      ELIGIBLE_PACK_IDS.clear();
   }

   @Inject(method = "loadRequestedPacks", at = @At("HEAD"))
   private void onLoadRequestedPacks(List<PackReloadConfig.IdAndPath> list, CallbackInfoReturnable<List<Pack>> cir) {
      for (PackReloadConfig.IdAndPath idAndPath : list) {
         if (!ELIGIBLE_PACK_IDS.contains(idAndPath.id())) {
            continue;
         }

         ResourcePackZipEditor.applyConfiguredOverrides(idAndPath.path(), LOGGER);
      }
   }

   private static boolean isEligibleLifestealAssetsUrl(URL url) {
      return "https".equalsIgnoreCase(url.getProtocol()) && LIFESTEAL_ASSETS_HOST.equalsIgnoreCase(url.getHost());
   }
}