package dev.candycup.lifestealutils.features.combat;

import dev.candycup.lifestealutils.features.timers.BasicTimerDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the built-in Bulwark cooldown timer.
 */
public final class BulwarkCooldownTracker {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/bulwark");

   public static final String TIMER_NAME = "Bulwark Cooldown";
   public static final String CHAT_TRIGGER = "You activated Bulwark's ability";
   public static final String DEFAULT_FORMAT = "<green><bold>Bulwark: </bold></green>{{cooldown}}";
   public static final int COOLDOWN_SECONDS = 30;

   public BulwarkCooldownTracker() {
      LOGGER.info("[lsu-bulwark] bulwark cooldown tracker initialized");
   }

   public static BasicTimerDefinition timerDefinition() {
      return new BasicTimerDefinition(
              TIMER_NAME,
              CHAT_TRIGGER,
              TIMER_NAME,
              DEFAULT_FORMAT.replace("{{cooldown}}", "{{timer}}"),
              "Ready!",
              COOLDOWN_SECONDS
      );
   }
}
