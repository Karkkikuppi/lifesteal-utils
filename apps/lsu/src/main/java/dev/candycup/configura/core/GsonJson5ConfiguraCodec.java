package dev.candycup.configura.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GsonJson5ConfiguraCodec implements ConfiguraCodec {
   private static final TypeToken<Map<String, Object>> MAP_TYPE = new TypeToken<>() {
   };

   private final Gson gson;

   public GsonJson5ConfiguraCodec(boolean prettyPrinting) {
      GsonBuilder builder = new GsonBuilder();
      if (prettyPrinting) {
         builder.setPrettyPrinting();
      }
      this.gson = builder.create();
   }

   @Override
   public Map<String, Object> decode(String raw) {
      if (raw == null || raw.isBlank()) {
         return new LinkedHashMap<>();
      }

      Map<String, Object> parsed = gson.fromJson(raw, MAP_TYPE);
      if (parsed == null) {
         return new LinkedHashMap<>();
      }
      return new LinkedHashMap<>(parsed);
   }

   @Override
   public String encode(Map<String, Object> values) {
      return gson.toJson(values);
   }
}
