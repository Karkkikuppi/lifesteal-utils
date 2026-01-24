package dev.candycup.lifestealutils.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudLayoutStorage {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Type MAP_TYPE = new TypeToken<Map<String, HudPosition>>() {
   }.getType();

   private HudLayoutStorage() {
   }

   public static Map<Identifier, HudPosition> load() {
      Path path = getPath();
      if (!Files.exists(path)) {
         return new HashMap<>();
      }
      try (Reader reader = Files.newBufferedReader(path)) {
         Map<String, HudPosition> raw = GSON.fromJson(reader, MAP_TYPE);
         Map<Identifier, HudPosition> hydrated = new HashMap<>();
         if (raw == null) {
            return hydrated;
         }
         for (Map.Entry<String, HudPosition> entry : raw.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) {
               LOGGER.warn("Skipping HUD element with invalid id {}", entry.getKey());
               continue;
            }
            HudPosition pos = entry.getValue();
            if (pos == null) {
               LOGGER.warn("Skipping HUD element {} because it had no saved position", id);
               continue;
            }
            hydrated.put(id, HudPosition.clamp(pos.x(), pos.y()));
         }
         return hydrated;
      } catch (IOException e) {
         LOGGER.error("Failed to load HUD layout", e);
         return new HashMap<>();
      }
   }

   public static void save(Map<Identifier, HudPosition> positions) {
      Path path = getPath();
      try {
         Files.createDirectories(path.getParent());
         Map<String, HudPosition> raw = new LinkedHashMap<>();
         for (Map.Entry<Identifier, HudPosition> entry : positions.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
         }
         try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(raw, writer);
         }
      } catch (IOException e) {
         LOGGER.error("Failed to save HUD layout", e);
      }
   }

   private static Path getPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("lifestealutils-hud.json");
   }
}
