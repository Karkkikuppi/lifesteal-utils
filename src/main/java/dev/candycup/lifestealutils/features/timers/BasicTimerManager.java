package dev.candycup.lifestealutils.features.timers;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.events.ClientTickEvent;
import dev.candycup.lifestealutils.event.listener.ChatEventListener;
import dev.candycup.lifestealutils.event.listener.TickEventListener;
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

public final class BasicTimerManager implements ChatEventListener, TickEventListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/timers");

   private final Map<String, BasicTimerDefinition> definitions = new LinkedHashMap<>();
   private final Map<String, TimerState> states = new LinkedHashMap<>();
   private final Map<String, HudElementDefinition> hudDefinitions = new LinkedHashMap<>();

   public BasicTimerManager(List<BasicTimerDefinition> definitions) {
      configure(definitions);
   }

   private void configure(List<BasicTimerDefinition> definitions) {
      this.definitions.clear();
      this.states.clear();
      this.hudDefinitions.clear();

      float baseY = 0.15F;
      float stepY = 0.035F;
      int index = 0;

      for (BasicTimerDefinition definition : definitions) {
         String slug = slugify(definition.name());
         String id = ensureUniqueId(slug);
         this.definitions.put(id, definition);
         this.states.put(id, new TimerState(0));
         Config.ensureBasicTimerKnown(id);

         HudElementDefinition hudDefinition = new HudElementDefinition(
                 Identifier.fromNamespaceAndPath("lifestealutils", id + "_timer"),
                 definition.name(),
                 () -> textFor(id, definition),
                 HudPosition.clamp(0.5F, baseY + (stepY * index))
         );
         this.hudDefinitions.put(id, hudDefinition);
         index++;
      }

      LOGGER.info("[lsu-timers] configured {} basic timers", this.definitions.size());
   }

   public List<HudElementDefinition> getHudDefinitions() {
      return new ArrayList<>(hudDefinitions.values());
   }

   public List<TimerEntry> getTimerEntries() {
      return definitions.entrySet().stream()
              .map(e -> new TimerEntry(e.getKey(), e.getValue()))
              .collect(Collectors.toList());
   }

   @Override
   public boolean isEnabled() {
      // enabled if any timer is enabled
      return definitions.keySet().stream()
              .anyMatch(Config::isBasicTimerEnabled);
   }

   @Override
   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      String message = event.getMessage().getString();
      if (message.isBlank()) {
         return;
      }

      for (Map.Entry<String, BasicTimerDefinition> entry : definitions.entrySet()) {
         BasicTimerDefinition definition = entry.getValue();
         if (definition.chatTrigger() != null && message.contains(definition.chatTrigger())) {
            if (!Config.isBasicTimerEnabled(entry.getKey())) {
               continue;
            }
            start(entry.getKey(), definition.durationSeconds());
            LOGGER.debug("[lsu-timers] started timer '{}' from chat trigger", definition.name());
         }
      }
   }

   @Override
   public void onClientTick(ClientTickEvent event) {
      for (TimerState state : states.values()) {
         if (state.remainingTicks > 0) {
            state.remainingTicks--;
         }
      }
   }

   private void start(String id, int durationSeconds) {
      TimerState state = states.get(id);
      if (state == null) {
         return;
      }
      state.remainingTicks = Math.max(durationSeconds * 20, 0);
   }

   private String textFor(String id, BasicTimerDefinition definition) {
      if (!Config.isBasicTimerEnabled(id)) {
         return "";
      }
      TimerState state = states.get(id);
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

   private String slugify(String name) {
      String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
      slug = slug.replaceAll("_+", "_");
      slug = slug.replaceAll("^_+|_+$", "");
      return slug.isBlank() ? "timer" : slug;
   }

   private String ensureUniqueId(String base) {
      String candidate = base;
      int counter = 1;
      while (definitions.containsKey(candidate)) {
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
