package dev.candycup.configura.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ToggleGroup {
   private final List<ToggleEntry> schema;
   private final Map<String, Boolean> values;

   public record ToggleEntry(String key, boolean defaultValue, String icon) {
   }

   private ToggleGroup(List<ToggleEntry> schema) {
      this.schema = List.copyOf(schema);
      this.values = new LinkedHashMap<>();
      for (ToggleEntry entry : schema) {
         this.values.put(entry.key(), entry.defaultValue());
      }
   }

   private ToggleGroup(List<ToggleEntry> schema, Map<String, Boolean> values) {
      this.schema = schema;
      this.values = new LinkedHashMap<>(values);
   }

   public boolean get(String key) {
      return values.getOrDefault(key, false);
   }

   public void set(String key, boolean value) {
      values.put(key, value);
   }

   public List<ToggleEntry> schema() {
      return schema;
   }

   public Map<String, Boolean> toRawMap() {
      return new LinkedHashMap<>(values);
   }

   public ToggleGroup copy() {
      return new ToggleGroup(schema, values);
   }

   public ToggleGroup withValues(Map<?, ?> rawMap) {
      ToggleGroup copy = copy();
      for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
         if (entry.getKey() instanceof String key && entry.getValue() instanceof Boolean b
                 && copy.values.containsKey(key)) {
            copy.values.put(key, b);
         }
      }
      return copy;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof ToggleGroup other)) return false;
      return Objects.equals(values, other.values);
   }

   @Override
   public int hashCode() {
      return values.hashCode();
   }

   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {
      private final List<ToggleEntry> entries = new ArrayList<>();

      public Builder entry(String key, boolean defaultValue, String icon) {
         entries.add(new ToggleEntry(key, defaultValue, icon));
         return this;
      }

      public Builder entry(String key, boolean defaultValue) {
         return entry(key, defaultValue, "");
      }

      public ToggleGroup build() {
         return new ToggleGroup(entries);
      }
   }
}
