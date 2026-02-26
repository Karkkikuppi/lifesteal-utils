package dev.candycup.lifestealutils.config;

import dev.candycup.lifestealutils.config.providers.BasicTimerConfigOptionProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ConfigDescriptorRegistry {
   private static final Set<ConfigOptionProvider> PROVIDERS = new LinkedHashSet<>();
   private static boolean defaultsRegistered;

   private ConfigDescriptorRegistry() {
   }

   public static synchronized void registerDefaultProviders() {
      if (defaultsRegistered) {
         return;
      }

      registerProvider(new BasicTimerConfigOptionProvider());
      defaultsRegistered = true;
   }

   public static synchronized void registerProvider(ConfigOptionProvider provider) {
      PROVIDERS.add(provider);
   }

   public static synchronized List<ConfigOptionProvider> getProviders() {
      return new ArrayList<>(PROVIDERS);
   }
}
