package dev.candycup.configura.core;

import java.util.Map;

@FunctionalInterface
public interface ConfiguraMigration {
   void migrate(Map<String, Object> config);

   static void invertBoolean(Map<String, Object> config, String oldKey, String newKey) {
      if (config.containsKey(oldKey)) {
         Object val = config.remove(oldKey);
         config.put(newKey, !(val instanceof Boolean b) || !b);
      }
   }

   static void rename(Map<String, Object> config, String oldKey, String newKey) {
      if (config.containsKey(oldKey)) {
         config.put(newKey, config.remove(oldKey));
      }
   }
}
