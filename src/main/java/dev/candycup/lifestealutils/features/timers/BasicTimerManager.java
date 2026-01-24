package dev.candycup.lifestealutils.features.timers;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class BasicTimerManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/timers");
   private static final Map<String, BasicTimerDefinition> DEFINITIONS = new LinkedHashMap<>();
   private static final Map<String, TimerState> STATES = new LinkedHashMap<>();
   private static final Map<String, HudElementDefinition> HUD_DEFINITIONS = new LinkedHashMap<>();

   private BasicTimerManager() {
   }

   public static void configure(List<BasicTimerDefinition> definitions) {
      DEFINITIONS.clear();
      STATES.clear();
      HUD_DEFINITIONS.clear();

      float baseY = 0.15F;
      float stepY = 0.035F;
      int index = 0;

      for (BasicTimerDefinition definition : definitions) {
         String slug = slugify(definition.name());
         String id = ensureUniqueId(slug);
         DEFINITIONS.put(id, definition);
         STATES.put(id, new TimerState(0));
         Config.ensureBasicTimerKnown(id);

         HudElementDefinition hudDefinition = new HudElementDefinition(
                 Identifier.fromNamespaceAndPath("lifestealutils", id + "_timer"),
                 definition.name(),
                 () -> textFor(id, definition),
                 HudPosition.clamp(0.5F, baseY + (stepY * index))
         );
         HUD_DEFINITIONS.put(id, hudDefinition);
         index++;
      }

      LOGGER.info("[lsu-timers] configured {} basic timers", DEFINITIONS.size());
   }

   public static List<HudElementDefinition> hudDefinitions() {
      return new ArrayList<>(HUD_DEFINITIONS.values());
   }

   public static List<TimerEntry> timerEntries() {
      return DEFINITIONS.entrySet().stream()
              .map(e -> new TimerEntry(e.getKey(), e.getValue()))
              .collect(Collectors.toList());
   }

   public static void handleChatMessage(String message) {
      if (message == null || message.isBlank()) {
         return;
      }
      for (Map.Entry<String, BasicTimerDefinition> entry : DEFINITIONS.entrySet()) {
         BasicTimerDefinition definition = entry.getValue();
         if (definition.chatTrigger() != null && message.contains(definition.chatTrigger())) {
            if (!Config.isBasicTimerEnabled(entry.getKey())) {
               continue;
            }
            start(entry.getKey(), definition.durationSeconds());
         }
      }
   }

   public static void tick() {
      for (TimerState state : STATES.values()) {
         if (state.remainingTicks > 0) {
            state.remainingTicks--;
         }
      }
   }

   private static void start(String id, int durationSeconds) {
      TimerState state = STATES.get(id);
      if (state == null) {
         return;
      }
      state.remainingTicks = Math.max(durationSeconds * 20, 0);
   }

   private static String textFor(String id, BasicTimerDefinition definition) {
      if (!Config.isBasicTimerEnabled(id)) {
         return "";
      }
      TimerState state = STATES.get(id);
      int remainingTicks = state != null ? state.remainingTicks : 0;
      String value;
      if (remainingTicks > 0) {
         int remainingSeconds = (remainingTicks + 19) / 20;
         value = formatDuration(remainingSeconds);
      } else {
         value = definition.passiveState();
      }

      String format = Config.getBasicTimerFormat(id, definition.defaultFormat());
      if (format == null || format.isBlank()) {
         format = "{{timer}}";
      }
      if (format.contains("{{timer}}")) {
         return format.replace("{{timer}}", value);
      }
      return format + " " + value;
   }

   public record TimerEntry(String id, BasicTimerDefinition definition) {
   }

   private static String formatDuration(int seconds) {
      int remaining = Math.max(seconds, 0);
      int hours = remaining / 3600;
      remaining -= hours * 3600;
      int minutes = remaining / 60;
      int secs = remaining % 60;

      StringBuilder builder = new StringBuilder();
      if (hours > 0) {
         builder.append(hours).append("h ");
      }
      if (hours > 0 || minutes > 0) {
         builder.append(minutes).append("m ");
      }
      builder.append(secs).append("s");
      return builder.toString().trim();
   }

   private static String slugify(String name) {
      String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
      slug = slug.replaceAll("_+", "_");
      slug = slug.replaceAll("^_+|_+$", "");
      return slug.isBlank() ? "timer" : slug;
   }

   private static String ensureUniqueId(String base) {
      String candidate = base;
      int counter = 1;
      while (DEFINITIONS.containsKey(candidate)) {
         candidate = base + "_" + counter;
         counter++;
      }
      return candidate;
   }

   private static final class TimerState {
      int remainingTicks;

      TimerState(int remainingTicks) {
         this.remainingTicks = remainingTicks;
      }
   }
}
