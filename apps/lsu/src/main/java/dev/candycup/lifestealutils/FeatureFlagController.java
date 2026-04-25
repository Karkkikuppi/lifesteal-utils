package dev.candycup.lifestealutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import dev.candycup.lifestealutils.features.timers.BasicTimerDefinition;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FeatureFlagController {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/feature-flags");
   private static final Gson GSON = new GsonBuilder().create();
   private static final String FEATURE_FLAG_URL = "https://karkkikuppi.github.io/lifesteal-utils/registry/remote.json";
   private static final String REGISTRY_REMOTE_PATH = "registry/remote.json";

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
      LOGGER.info(
              "[lsu-flags] remote registry loaded ({} config rules, {} incompatibility rules, {} timers)",
              payload.configRules.size(),
              payload.incompatibilityFlags.size(),
              payload.basicTimers.size()
      );
   }

   private static String fetchFeatureFlagJson() {
      if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
         String localPayload = readDevelopmentFeatureFlagJson();
         if (localPayload != null) {
            return localPayload;
         }
      }

      NetworkUtilsController.HttpResult result = NetworkUtilsController.get(FEATURE_FLAG_URL);
      if (result.success() && result.body() != null) {
         return result.body();
      }
      if (result.statusCode() > 0) {
         LOGGER.warn("[lsu-flags] remote registry fetch returned non-OK status {}", result.statusCode());
      } else {
         LOGGER.error("[lsu-flags] failed to fetch remote registry: {}", result.error());
      }
      return "{}";
   }

   private static String readDevelopmentFeatureFlagJson() {
      List<Path> roots = List.of(
              FabricLoader.getInstance().getGameDir(),
              Path.of(System.getProperty("user.dir", "."))
      );

      for (Path root : roots) {
         Path candidate = findRegistryPayload(root);
         if (candidate == null) {
            continue;
         }

         try {
            LOGGER.info("[lsu-flags] development environment detected, loading flags from {}", candidate.toAbsolutePath().normalize());
            return Files.readString(candidate);
         } catch (Exception exception) {
            LOGGER.warn("[lsu-flags] failed to read development flags from {}", candidate.toAbsolutePath().normalize(), exception);
            return "{}";
         }
      }

      LOGGER.warn("[lsu-flags] development environment detected, but '{}' could not be found from run directories", REGISTRY_REMOTE_PATH);
      return "{}";
   }

   private static Path findRegistryPayload(Path start) {
      if (start == null) {
         return null;
      }

      Path current = start.toAbsolutePath().normalize();
      while (current != null) {
         Path candidate = current.resolve("registry").resolve("remote.json");
         if (Files.isRegularFile(candidate)) {
            return candidate;
         }
         current = current.getParent();
      }
      return null;
   }

   private static FeatureFlagPayload parsePayload(String json) {
      try {
         FeatureFlagPayload parsed = GSON.fromJson(json, FeatureFlagPayload.class);
         if (parsed == null) {
            return new FeatureFlagPayload();
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
         if (parsed.configRules == null) {
            parsed.configRules = Collections.emptyList();
         }
         if (parsed.incompatibilityFlags == null) {
            parsed.incompatibilityFlags = Collections.emptyList();
         }
         return parsed;
      } catch (Exception e) {
         LOGGER.error("[lsu-flags] failed to parse remote registry payload; using empty payload", e);
         return new FeatureFlagPayload();
      }
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

   public static List<String> getSplashes() {
      return new ArrayList<>(payload.splashes);
   }

   public static List<RemoteConfigRule> getRemoteConfigRules() {
      ensureLoaded();
      List<RemoteConfigRule> rules = new ArrayList<>();
      for (int i = 0; i < payload.configRules.size(); i++) {
         FeatureFlagConfigRule rule = payload.configRules.get(i);
         RemoteConfigRule normalized = RemoteConfigRule.from(rule, i);
         if (normalized != null) {
            rules.add(normalized);
         }
      }
      return rules;
   }

   public static RuntimeInfo getRuntimeInfo() {
      return new RuntimeInfo(detectModVersion(), detectGameVersion());
   }

   public static void assertNoIncompatibleModsDetected() {
      ensureLoaded();
      RuntimeInfo runtimeInfo = getRuntimeInfo();
      List<IncompatibilityRule> rules = getIncompatibilityRules();
      if (rules.isEmpty()) {
         return;
      }

      for (var container : FabricLoader.getInstance().getAllMods()) {
         String modId = container.getMetadata().getId();
         String modVersion = container.getMetadata().getVersion().getFriendlyString();

         for (IncompatibilityRule rule : rules) {
            if (rule.matches(runtimeInfo, modId, modVersion)) {
               String reason = rule.reason() != null && !rule.reason().isBlank()
                       ? rule.reason()
                       : "This mod combination is remotely blocked due to incompatibility.";
               throw new IllegalStateException(
                       "Lifesteal Utils has been configured to fail due to a detected incompatibility with the mod '%s' (%s). The reason for this: %s"
                               .formatted(modId, modVersion, reason)
               );
            }
         }
      }
   }

   public static List<IncompatibilityRule> getIncompatibilityRules() {
      ensureLoaded();
      List<IncompatibilityRule> rules = new ArrayList<>();
      for (int i = 0; i < payload.incompatibilityFlags.size(); i++) {
         FeatureFlagIncompatibilityRule raw = payload.incompatibilityFlags.get(i);
         IncompatibilityRule normalized = IncompatibilityRule.from(raw, i);
         if (normalized != null) {
            rules.add(normalized);
         }
      }
      return rules;
   }

   private static String detectModVersion() {
      return FabricLoader.getInstance()
              .getModContainer("lifestealutils")
              .map(container -> container.getMetadata().getVersion().getFriendlyString())
              .orElse("0.0.0");
   }

   private static String detectGameVersion() {
      return FabricLoader.getInstance()
              .getModContainer("minecraft")
              .map(container -> container.getMetadata().getVersion().getFriendlyString())
              .orElse("unknown");
   }

   private static final class FeatureFlagPayload {
      List<FeatureFlagTimer> basicTimers = Collections.emptyList();
      Map<String, String> triggers = Collections.emptyMap();
      List<String> splashes = Collections.emptyList();
      @SerializedName("configRules")
      List<FeatureFlagConfigRule> configRules = Collections.emptyList();
      @SerializedName("incompatibilityFlags")
      List<FeatureFlagIncompatibilityRule> incompatibilityFlags = Collections.emptyList();
   }

   private static final class FeatureFlagConfigRule {
      FeatureFlagApplyFor applyFor;
      String applyForKey;
      Object forceState;
      String reason;
      Integer priority;
   }

   private static final class FeatureFlagApplyFor {
      String modVersion;
      String gameVersion;
   }

   private static final class FeatureFlagIncompatibilityRule {
      String modIdRegex;
      String modVersionRegex;
      String lsuVersionRegex;
      String gameVersionRegex;
      String reason;
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
      String nbtId;

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
                 duration,
                 nbtId
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

   public record RuntimeInfo(String modVersion, String gameVersion) {
   }

   public record IncompatibilityRule(
           String modIdRegex,
           String modVersionRegex,
           String lsuVersionRegex,
           String gameVersionRegex,
           String reason,
           int order
   ) {
      static IncompatibilityRule from(FeatureFlagIncompatibilityRule raw, int order) {
         if (raw == null || raw.modIdRegex == null || raw.modIdRegex.isBlank()) {
            return null;
         }

         if (!isValidPattern(raw.modIdRegex)) {
            return null;
         }
         if (!isValidOptionalPattern(raw.modVersionRegex)) {
            return null;
         }
         if (!isValidOptionalPattern(raw.lsuVersionRegex)) {
            return null;
         }
         if (!isValidOptionalPattern(raw.gameVersionRegex)) {
            return null;
         }

         return new IncompatibilityRule(
                 raw.modIdRegex,
                 blankToNull(raw.modVersionRegex),
                 blankToNull(raw.lsuVersionRegex),
                 blankToNull(raw.gameVersionRegex),
                 raw.reason,
                 order
         );
      }

      public boolean matches(RuntimeInfo runtimeInfo, String modId, String modVersion) {
         return Pattern.compile(modIdRegex).matcher(Objects.toString(modId, "")).matches()
                 && matchesOptionalRegex(modVersionRegex, modVersion)
                 && matchesOptionalRegex(lsuVersionRegex, runtimeInfo.modVersion())
                 && matchesOptionalRegex(gameVersionRegex, runtimeInfo.gameVersion());
      }

      private static boolean matchesOptionalRegex(String regex, String value) {
         if (regex == null || regex.isBlank()) {
            return true;
         }
         return Pattern.compile(regex).matcher(Objects.toString(value, "")).matches();
      }

      private static boolean isValidOptionalPattern(String regex) {
         return regex == null || regex.isBlank() || isValidPattern(regex);
      }

      private static boolean isValidPattern(String regex) {
         try {
            Pattern.compile(regex);
            return true;
         } catch (PatternSyntaxException exception) {
            LOGGER.warn("[lsu-flags] ignoring incompatibility rule with invalid regex '{}': {}", regex, exception.getMessage());
            return false;
         }
      }

      private static String blankToNull(String value) {
         if (value == null || value.isBlank()) {
            return null;
         }
         return value;
      }
   }

   public record RemoteConfigRule(
           String applyForKey,
           String modVersionRegex,
           String gameVersionRegex,
           Object forceState,
           String reason,
           int priority,
           int order
   ) {
      static RemoteConfigRule from(FeatureFlagConfigRule raw, int order) {
         if (raw == null || raw.applyFor == null) {
            return null;
         }
         if (raw.applyForKey == null || raw.applyForKey.isBlank()) {
            return null;
         }
         String modPattern = raw.applyFor.modVersion;
         String gamePattern = raw.applyFor.gameVersion;
         if (modPattern == null || modPattern.isBlank() || gamePattern == null || gamePattern.isBlank()) {
            return null;
         }
         if (!isValidPattern(modPattern) || !isValidPattern(gamePattern)) {
            return null;
         }

         return new RemoteConfigRule(
                 raw.applyForKey.trim().toLowerCase(Locale.ROOT),
                 modPattern,
                 gamePattern,
                 raw.forceState,
                 raw.reason,
                 raw.priority != null ? raw.priority : 0,
                 order
         );
      }

      public boolean matches(RuntimeInfo runtimeInfo) {
         return Pattern.compile(modVersionRegex).matcher(runtimeInfo.modVersion()).matches()
                 && Pattern.compile(gameVersionRegex).matcher(runtimeInfo.gameVersion()).matches();
      }

      private static boolean isValidPattern(String regex) {
         try {
            Pattern.compile(regex);
            return true;
         } catch (PatternSyntaxException exception) {
            LOGGER.warn("[lsu-flags] ignoring config rule with invalid regex '{}': {}", regex, exception.getMessage());
            return false;
         }
      }
   }

}
