package dev.candycup.lifestealutils.config;

import dev.candycup.lifestealutils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ConfigContainerRegistry {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils");
   private static final Set<Class<?>> REGISTERED_CONTAINERS = new LinkedHashSet<>();
   private static final String GENERATED_INDEX_CLASS = "dev.candycup.lifestealutils.config.generated.GeneratedConfigContainerIndex";
   private static boolean initialized;

   private ConfigContainerRegistry() {
   }

   public static synchronized void initializeGeneratedIndex() {
      if (initialized) {
         return;
      }

      clear();
      try {
         Class<?> generatedClass = Class.forName(GENERATED_INDEX_CLASS);
         Method registerAll = generatedClass.getMethod("registerAll");
         registerAll.invoke(null);
      } catch (Exception exception) {
         LOGGER.warn("failed to load generated config container index, falling back to Config.class", exception);
         registerContainer(Config.class);
      }

      if (REGISTERED_CONTAINERS.isEmpty()) {
         registerContainer(Config.class);
      }

      initialized = true;
   }

   public static synchronized void clear() {
      REGISTERED_CONTAINERS.clear();
   }

   public static synchronized void registerContainer(Class<?> containerClass) {
      REGISTERED_CONTAINERS.add(containerClass);
   }

   public static synchronized List<Class<?>> getRegisteredContainers() {
      return new ArrayList<>(REGISTERED_CONTAINERS);
   }
}