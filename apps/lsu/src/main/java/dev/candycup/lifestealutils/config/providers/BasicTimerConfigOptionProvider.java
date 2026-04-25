package dev.candycup.lifestealutils.config.providers;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.LifestealUtils;
import dev.candycup.configura.core.Configura;
import dev.candycup.configura.core.ConfiguraDynamicEntryDefinition;
import dev.candycup.lifestealutils.config.ConfigOptionCollector;
import dev.candycup.lifestealutils.config.ConfigOptionDescriptor;
import dev.candycup.lifestealutils.config.ConfigOptionProvider;
import dev.candycup.lifestealutils.features.timers.BasicTimerManager;

import java.util.Optional;

public final class BasicTimerConfigOptionProvider implements ConfigOptionProvider {
   @Override
   public void registerOptions(ConfigOptionCollector collector) {
      BasicTimerManager timerManager = LifestealUtils.getBasicTimerManager();
      if (timerManager == null) {
         return;
      }

      for (BasicTimerManager.TimerEntry entry : timerManager.getTimerEntries()) {
         String id = entry.id();
         String timerName = entry.definition().name();
         String toggleName = entry.definition().toggleOption() != null && !entry.definition().toggleOption().isBlank()
                 ? entry.definition().toggleOption()
                 : timerName;
         Config.ensureBasicTimerKnown(id);
         Config.ensureBasicTimerFormat(id, entry.definition().defaultFormat());

         Configura.registerDynamicEntry(
                 ConfiguraDynamicEntryDefinition.create(
                         "timer_" + id + "_enabled",
                         Optional.empty(),
                         false,
                         false,
                         Boolean.class,
                         Boolean.class,
                         () -> Config.isBasicTimerEnabled(id),
                         value -> Config.setBasicTimerEnabled(id, value),
                         () -> false
                 )
         );

         Configura.registerDynamicEntry(
                 ConfiguraDynamicEntryDefinition.create(
                         "timer_" + id + "_format",
                         Optional.empty(),
                         false,
                         false,
                         String.class,
                         String.class,
                         () -> Config.getBasicTimerFormat(id, entry.definition().defaultFormat()),
                         value -> Config.setBasicTimerFormat(id, value),
                         () -> entry.definition().defaultFormat()
                 )
         );

         collector.add(ConfigOptionDescriptor.bool(
                 "timers",
                 "customenchanttimers",
                 id + "_enabled",
                 () -> false,
                 () -> Config.isBasicTimerEnabled(id),
                 value -> Config.setBasicTimerEnabled(id, value)
         ).hardTranslation(
                 toggleName,
                 "Enable or disable the %s timer overlay.".formatted(timerName)
         ).withAccordion(id, timerName));

         collector.add(ConfigOptionDescriptor.minimessage(
                 "timers",
                 "customenchanttimers",
                 id + "_format",
                 () -> entry.definition().defaultFormat(),
                 () -> Config.getBasicTimerFormat(id, entry.definition().defaultFormat()),
                 value -> Config.setBasicTimerFormat(id, value)
         ).hardTranslation(
                 timerName + " Format",
                 "Customize how the %s timer is rendered. Use {{timer}} for the timer value.".formatted(timerName)
         ).withAccordion(id, timerName));
      }
   }
}
