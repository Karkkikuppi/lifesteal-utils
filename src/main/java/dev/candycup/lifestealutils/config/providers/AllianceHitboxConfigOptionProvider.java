package dev.candycup.lifestealutils.config.providers;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.config.ConfigOptionCollector;
import dev.candycup.lifestealutils.config.ConfigOptionDescriptor;
import dev.candycup.lifestealutils.config.ConfigOptionProvider;
import dev.candycup.lifestealutils.features.alliances.AllianceHitboxColorResolver;
import dev.candycup.lifestealutils.features.alliances.AllianceNameRenderHandler;

import java.util.Locale;

public final class AllianceHitboxConfigOptionProvider implements ConfigOptionProvider {
   @Override
   public void registerOptions(ConfigOptionCollector collector) {
      for (AllianceNameRenderHandler.HitboxAllianceCandidate candidate : AllianceNameRenderHandler.getHitboxCandidates()) {
         String allianceId = candidate.allianceId();
         String defaultColor = AllianceHitboxColorResolver.normalizeConfigColor(candidate.color());
         String sortKey = buildSortKey(candidate.displayName(), allianceId);

         collector.add(ConfigOptionDescriptor.string(
                 "alliances",
                 "hitboxes",
                 sortKey,
                 () -> defaultColor,
                 () -> Config.getAllianceHitboxColorOverride(allianceId, defaultColor),
                 value -> Config.setAllianceHitboxColorOverride(allianceId, value)
         ).hardTranslation(
                 candidate.displayName() + " Color",
                 "Color used for vanilla F3+B hitboxes on players in this alliance."
         ));
      }
   }

   private static String buildSortKey(String displayName, String allianceId) {
      String normalizedName = displayName == null ? "" : displayName
              .toLowerCase(Locale.ROOT)
              .replaceAll("[^a-z0-9]+", "_")
              .replaceAll("^_+|_+$", "");
      if (normalizedName.isBlank()) {
         normalizedName = "alliance";
      }
      return normalizedName + "_" + allianceId.replace("-", "");
   }
}
