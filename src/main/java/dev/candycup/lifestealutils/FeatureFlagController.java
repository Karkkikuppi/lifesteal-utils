package dev.candycup.lifestealutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import dev.candycup.lifestealutils.features.timers.BasicTimerDefinition;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FeatureFlagController {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/feature-flags");
   private static final Gson GSON = new GsonBuilder().create();
   private static final String FEATURE_FLAG_URL = "https://gist.githubusercontent.com/Karkkikuppi/4146e00d65849ac142bbad711982c69e/raw/lsu.json";
   private static final String CURRENT_VERSION = detectModVersion();

   private static FeatureFlagPayload payload = new FeatureFlagPayload();
   private static boolean loaded = false;

   private FeatureFlagController() {
   }

   public static synchronized void ensureLoaded() {
      if (loaded) {
         return;
      }
      load();
   }

   public static synchronized void load() {
      loaded = true;
      String raw = fetchFeatureFlagJson();
      payload = parsePayload(raw);
      LOGGER.info("[lsu-flags] feature flags loaded ({} feature keys, {} timers)", payload.features.size(), payload.basicTimers.size());
   }

   private static String fetchFeatureFlagJson() {
      NetworkUtilsController.HttpResult result = NetworkUtilsController.get(FEATURE_FLAG_URL);
      if (result.success() && result.body() != null) {
         return result.body();
      }
      if (result.statusCode() > 0) {
         LOGGER.warn("[lsu-flags] feature flag fetch returned non-OK status {}", result.statusCode());
      } else {
         LOGGER.error("[lsu-flags] failed to fetch feature flags: {}", result.error());
      }
      return "{}";
   }

   private static FeatureFlagPayload parsePayload(String json) {
      try {
         FeatureFlagPayload parsed = GSON.fromJson(json, FeatureFlagPayload.class);
         if (parsed == null) {
            return new FeatureFlagPayload();
         }
         if (parsed.features == null) {
            parsed.features = Collections.emptyMap();
         }
         if (parsed.basicTimers == null) {
            parsed.basicTimers = Collections.emptyList();
         }
         if (parsed.triggers == null) {
            parsed.triggers = Collections.emptyMap();
         }
         if (parsed.splashes == null) {
            parsed.splashes = Collections.emptyList();
         }
         return parsed;
      } catch (Exception e) {
         LOGGER.error("[lsu-flags] failed to parse feature flag payload; using empty payload", e);
         return new FeatureFlagPayload();
      }
   }

   public static boolean isFeatureForced(String featureKey) {
      return getForcedState(featureKey) != null;
   }

   public static Boolean getForcedState(String featureKey) {
      FeatureFlagRule rule = selectRule(featureKey);
      return rule != null ? rule.forceState : null;
   }

   public static String getReasoning(String featureKey) {
      FeatureFlagRule rule = selectRule(featureKey);
      return rule != null ? rule.reasoning : null;
   }

   public static boolean isFeatureAvailable(String featureKey) {
      return !isFeatureForced(featureKey);
   }

   public static List<BasicTimerDefinition> getBasicTimers() {
      List<BasicTimerDefinition> timers = new ArrayList<>();
      for (FeatureFlagTimer timer : payload.basicTimers) {
         BasicTimerDefinition definition = timer.toDefinition();
         if (definition != null) {
            timers.add(definition);
         }
      }
      return timers;
   }

   public static String getTrigger(String triggerKey) {
      return payload.triggers.get(triggerKey);
   }

   /**
    * retrieves POI definitions from the remote payload.
    * returns an empty list if none are configured.
    */
   public static List<PoiDefinition> getPois() {
      ensureLoaded();
      List<PoiDefinition> list = new ArrayList<>();
      for (FeatureFlagPoi p : payload.pois) {
         if (p == null || p.id == null || p.name == null) continue;
         boolean disabled = p.disabled != null && p.disabled;
         if (disabled) {
            continue;
         }
         double x = p.x != null ? p.x : 0.0;
         double y = p.y != null ? p.y : 0.0;
         double z = p.z != null ? p.z : 0.0;
         list.add(new PoiDefinition(p.id, p.name, x, y, z, p.dimension, false));
      }
      return list;
   }

   /**
    * retrieves POI definitions from the remote payload, including disabled ones.
    *
    * @return list of poi definitions (may include disabled entries)
    */
   public static List<PoiDefinition> getPoisIncludingDisabled() {
      ensureLoaded();
      List<PoiDefinition> list = new ArrayList<>();
      for (FeatureFlagPoi p : payload.pois) {
         if (p == null || p.id == null || p.name == null) continue;
         boolean disabled = p.disabled != null && p.disabled;
         double x = p.x != null ? p.x : 0.0;
         double y = p.y != null ? p.y : 0.0;
         double z = p.z != null ? p.z : 0.0;
         list.add(new PoiDefinition(p.id, p.name, x, y, z, p.dimension, disabled));
      }
      return list;
   }

   /**
    * retrieves the list of splash texts from the remote payload.
    *
    * @return the list of splash texts, or an empty list if none are configured
    */
   public static List<String> getSplashes() {
      return new ArrayList<>(payload.splashes);
   }

   private static FeatureFlagRule selectRule(String featureKey) {
      List<FeatureFlagRule> rules = payload.features.get(featureKey);
      if (rules == null || rules.isEmpty()) {
         return null;
      }
      for (FeatureFlagRule rule : rules) {
         if (rule.matches(CURRENT_VERSION)) {
            return rule;
         }
      }
      return null;
   }

   private static String detectModVersion() {
      return FabricLoader.getInstance()
              .getModContainer("lifestealutils")
              .map(container -> container.getMetadata().getVersion().getFriendlyString())
              .orElse("0.0.0");
   }

   private static boolean versionSatisfiesRule(String currentVersion, String enableRuleFor) {
      if (enableRuleFor == null || enableRuleFor.isBlank()) {
         return true;
      }

      String trimmed = enableRuleFor.trim();
      Comparison comparison = Comparison.EQ;
      String version = trimmed;

      if (trimmed.startsWith(">=")) {
         comparison = Comparison.GTE;
         version = trimmed.substring(2).trim();
      } else if (trimmed.startsWith("<=")) {
         comparison = Comparison.LTE;
         version = trimmed.substring(2).trim();
      } else if (trimmed.startsWith(">")) {
         comparison = Comparison.GT;
         version = trimmed.substring(1).trim();
      } else if (trimmed.startsWith("<")) {
         comparison = Comparison.LT;
         version = trimmed.substring(1).trim();
      }

      int cmp = compareVersions(normalizeVersion(currentVersion), normalizeVersion(version));
      return switch (comparison) {
         case GT -> cmp > 0;
         case GTE -> cmp >= 0;
         case LT -> cmp < 0;
         case LTE -> cmp <= 0;
         case EQ -> cmp == 0;
      };
   }

   private static String normalizeVersion(String version) {
      String normalized = version.toLowerCase(Locale.ROOT).split("\\+")[0];
      normalized = normalized.replaceAll("[^0-9.]", "");
      return normalized.isBlank() ? "0.0.0" : normalized;
   }

   private static int compareVersions(String left, String right) {
      String[] leftParts = left.split("\\.");
      String[] rightParts = right.split("\\.");
      int max = Math.max(leftParts.length, rightParts.length);
      for (int i = 0; i < max; i++) {
         int l = i < leftParts.length ? parseIntSafe(leftParts[i]) : 0;
         int r = i < rightParts.length ? parseIntSafe(rightParts[i]) : 0;
         if (l != r) {
            return Integer.compare(l, r);
         }
      }
      return 0;
   }

   private static int parseIntSafe(String part) {
      try {
         return Integer.parseInt(part);
      } catch (NumberFormatException e) {
         return 0;
      }
   }

   private enum Comparison {
      GT, GTE, LT, LTE, EQ
   }

   private static final class FeatureFlagPayload {
      Map<String, List<FeatureFlagRule>> features = Collections.emptyMap();
      List<FeatureFlagTimer> basicTimers = Collections.emptyList();
      Map<String, String> triggers = Collections.emptyMap();
      List<String> splashes = Collections.emptyList();
      List<FeatureFlagPoi> pois = Collections.emptyList();
   }

   private static final class FeatureFlagRule {
      @SerializedName("forceState")
      Boolean forceState;
      String reasoning;
      String enableRuleFor;

      boolean matches(String currentVersion) {
         return versionSatisfiesRule(currentVersion, enableRuleFor);
      }
   }

   private static final class FeatureFlagTimer {
      String chatTrigger;
      String name;
      String toggleOption;
      String defaultFormat;
      String passiveState;
      @SerializedName("timerSeconds")
      Integer timerSeconds;
      @SerializedName("timerFormat")
      Integer timerFormatSeconds;

      BasicTimerDefinition toDefinition() {
         int duration = resolveDuration();
         if (chatTrigger == null || name == null || duration <= 0) {
            return null;
         }
         String fallbackFormat = defaultFormat != null ? defaultFormat : "{{timer}}";
         String fallbackPassive = passiveState != null ? passiveState : "Ready!";
         String toggleLabel = toggleOption != null ? toggleOption : name;
         return new BasicTimerDefinition(
                 name,
                 chatTrigger,
                 toggleLabel,
                 fallbackFormat,
                 fallbackPassive,
                 duration
         );
      }

      private int resolveDuration() {
         if (timerSeconds != null && timerSeconds > 0) {
            return timerSeconds;
         }
         if (timerFormatSeconds != null && timerFormatSeconds > 0) {
            return timerFormatSeconds;
         }
         return -1;
      }
   }

   private static final class FeatureFlagPoi {
      String id;
      String name;
      Double x;
      Double y;
      Double z;
      String dimension;
      Boolean disabled;
   }

   public record PoiDefinition(String id, String name, double x, double y, double z, String dimension,
                               boolean disabled) {
   }
}
