package dev.candycup.lifestealutils.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class PersistentKnowledgeController {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/knowledge");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String ROOT_DIR = "lifestealutils";
   private static final String KNOWLEDGE_FILE = "knowledge.json";

   private static KnowledgeData data = new KnowledgeData();

   private PersistentKnowledgeController() {
   }

   public static void initialize() {
      load();
   }

   public static void load() {
      Path path = getKnowledgePath();
      if (!Files.exists(path)) {
         data = new KnowledgeData();
         return;
      }
      try (Reader reader = Files.newBufferedReader(path)) {
         KnowledgeData loaded = GSON.fromJson(reader, KnowledgeData.class);
         data = loaded != null ? loaded : new KnowledgeData();
         if (data.prestigeEnhancements == null) {
            data.prestigeEnhancements = new HashMap<>();
         }
      } catch (Exception e) {
         LOGGER.warn("failed to load knowledge.json", e);
         data = new KnowledgeData();
      }
   }

   public static void save() {
      Path path = getKnowledgePath();
      Path temp = path.resolveSibling("knowledge.tmp");
      try {
         Files.createDirectories(path.getParent());
         try (Writer writer = Files.newBufferedWriter(temp)) {
            GSON.toJson(data, writer);
         }
         Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (Exception e) {
         LOGGER.warn("failed to save knowledge.json", e);
      }
   }

   public static Map<String, Float> getPrestigeEnhancements() {
      return data.prestigeEnhancements;
   }

   public static void setPrestigeEnhancements(Map<String, Float> enhancements) {
      data.prestigeEnhancements = enhancements != null ? new HashMap<>(enhancements) : new HashMap<>();
      save();
   }

   public static float getPrestigeEnhancement(String key) {
      if (key == null) return 0f;
      Float value = data.prestigeEnhancements.get(key);
      return value != null ? value : 0f;
   }

   private static Path getKnowledgePath() {
      return FabricLoader.getInstance().getGameDir()
              .resolve(ROOT_DIR)
              .resolve(KNOWLEDGE_FILE);
   }

   public static class KnowledgeData {
      public Map<String, Float> prestigeEnhancements = new HashMap<>();
   }
}
