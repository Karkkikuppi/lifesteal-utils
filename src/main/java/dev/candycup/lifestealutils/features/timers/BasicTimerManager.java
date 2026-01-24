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

   /**
    * Create a BasicTimerManager and configure it with the provided timer definitions.
    *
    * @param definitions the list of BasicTimerDefinition objects used to initialize the manager's timers and HUD entries
    */
   public BasicTimerManager(List<BasicTimerDefinition> definitions) {
      configure(definitions);
   }

   /**
    * Initializes internal timer definitions, states, and HUD elements from the provided list.
    *
    * Clears any existing configuration, then for each supplied BasicTimerDefinition creates a unique
    * timer id, initializes its remaining-ticks state to zero, registers the timer with configuration,
    * and creates a corresponding HudElementDefinition positioned sequentially for display.
    *
    * @param definitions list of timer definitions to configure; each entry becomes a managed timer with a HUD element
    */
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

   /**
    * Provide a snapshot list of HUD element definitions for all configured timers.
    *
    * @return a new List containing all HUD element definitions in insertion order; modifying the returned list does not affect the manager's internal state
    */
   public List<HudElementDefinition> getHudDefinitions() {
      return new ArrayList<>(hudDefinitions.values());
   }

   /**
    * Provides timer entries for all configured basic timers in insertion order.
    *
    * @return a list of TimerEntry objects pairing each timer id with its definition, in configuration order
    */
   public List<TimerEntry> getTimerEntries() {
      return definitions.entrySet().stream()
              .map(e -> new TimerEntry(e.getKey(), e.getValue()))
              .collect(Collectors.toList());
   }

   /**
    * Determine whether any configured basic timer is enabled in the user configuration.
    *
    * @return `true` if any configured basic timer is enabled, `false` otherwise.
    */
   @Override
   public boolean isEnabled() {
      // enabled if any timer is enabled
      return definitions.keySet().stream()
          .anyMatch(Config::isBasicTimerEnabled);
   }

   /**
    * Processes an incoming chat message and starts any enabled basic timers whose chat trigger appears in the message.
    *
    * Extracts the message text from the provided event; if the message is null or blank the method returns immediately.
    * For each configured timer definition, if the definition has a non-null chat trigger and the message contains that trigger,
    * and the timer is enabled via configuration, the corresponding timer is started using the definition's configured duration.
    *
    * @param event the chat message event containing the message to inspect
    */
   @Override
   public void onChatMessageReceived(ChatMessageReceivedEvent event) {
      String message = event.getMessage().getString();
      if (message == null || message.isBlank()) {
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

   /**
    * Advances all active timers by one tick, decrementing their remaining tick counts.
    *
    * <p>For each tracked timer with a remaining tick count greater than zero, reduces that count by one.</p>
    */
   @Override
   public void onClientTick(ClientTickEvent event) {
      for (TimerState state : states.values()) {
         if (state.remainingTicks > 0) {
            state.remainingTicks--;
         }
      }
   }

   /**
    * Starts or restarts the timer with the given identifier to the specified duration.
    *
    * @param id              the unique timer identifier
    * @param durationSeconds the duration in seconds to set for the timer; negative values are treated as zero
    */
   private void start(String id, int durationSeconds) {
      TimerState state = states.get(id);
      if (state == null) {
         return;
      }
      state.remainingTicks = Math.max(durationSeconds * 20, 0);
   }

   /**
    * Format the display text for the timer identified by `id` using its definition and current state.
    *
    * @param id         the unique identifier of the timer
    * @param definition the timer's definition providing passive state and default format
    * @return           an empty string if the timer is disabled; otherwise a string where the chosen format's
    *                   `{{timer}}` placeholder is replaced with the remaining duration (or the definition's
    *                   passive state when inactive). If the configured format has no `{{timer}}` placeholder,
    *                   the method returns the format followed by a space and the value.
    */
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

   /**
    * Formats a duration given in seconds into a compact human-readable string.
    *
    * @param seconds total seconds (negative values are treated as zero)
    * @return a string like "1h 2m 3s", omitting hours or minutes when zero (e.g., "2m 5s" or "5s")
    */
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

   /**
    * Converts a human-readable name into a lowercase identifier-safe slug.
    *
    * @param name the input string to convert into a slug
    * @return a lowercase slug containing only letters, digits, and underscores with no leading or trailing underscores; returns `"timer"` if the resulting slug is empty
    */
   private String slugify(String name) {
      String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
      slug = slug.replaceAll("_+", "_");
      slug = slug.replaceAll("^_+|_+$", "");
      return slug.isBlank() ? "timer" : slug;
   }

   /**
    * Generate a unique identifier by appending a numeric suffix when necessary.
    *
    * @param base the proposed base identifier
    * @return a unique identifier not present in {@code definitions}; returns {@code base} if unused,
    *         otherwise {@code base_<n>} where {@code <n>} is the smallest positive integer that makes it unique
    */
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