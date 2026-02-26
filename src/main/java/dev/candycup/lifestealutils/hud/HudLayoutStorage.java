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
   private static final Type MAP_TYPE = new TypeToken<Map<String, HudPositionDto>>() {
   }.getType();

   private HudLayoutStorage() {
   }

   /**
    * Plain DTO for serialization
    */
   @SuppressWarnings("unused") // fields populated by Gson via reflection
   private static final class HudPositionDto {
      float x;
      float y;
      String anchor;
   }

   public static Map<Identifier, HudPosition> load() {
      Path path = getPath();
      if (!Files.exists(path)) {
         return new HashMap<>();
      }
      try (Reader reader = Files.newBufferedReader(path)) {
         Map<String, HudPositionDto> raw = GSON.fromJson(reader, MAP_TYPE);
         Map<Identifier, HudPosition> hydrated = new HashMap<>();
         if (raw == null) {
            return hydrated;
         }
         for (Map.Entry<String, HudPositionDto> entry : raw.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) {
               LOGGER.warn("Skipping HUD element with invalid id {}", entry.getKey());
               continue;
            }
            HudPositionDto dto = entry.getValue();
            if (dto == null) {
               LOGGER.warn("Skipping HUD element {} because it had no saved position", id);
               continue;
            }
            HudAnchor anchor = parseAnchor(dto.anchor);
            hydrated.put(id, HudPosition.clamp(dto.x, dto.y, anchor));
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
         Map<String, HudPositionDto> raw = new LinkedHashMap<>();
         for (Map.Entry<Identifier, HudPosition> entry : positions.entrySet()) {
            HudPosition pos = entry.getValue();
            HudPositionDto dto = new HudPositionDto();
            dto.x = pos.x();
            dto.y = pos.y();
            dto.anchor = pos.anchor().name();
            raw.put(entry.getKey().toString(), dto);
         }
         try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(raw, writer);
         }
      } catch (IOException e) {
         LOGGER.error("Failed to save HUD layout", e);
      }
   }

   private static HudAnchor parseAnchor(String name) {
      if (name == null) {
         return HudAnchor.CENTER;
      }
      try {
         return HudAnchor.valueOf(name.toUpperCase(java.util.Locale.ROOT));
      } catch (IllegalArgumentException e) {
         LOGGER.warn("Unknown HUD anchor '{}', defaulting to CENTER", name);
         return HudAnchor.CENTER;
      }
   }

   private static Path getPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("lifestealutils-hud.json");
   }
}
