package dev.candycup.lifestealutils.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.candycup.lifestealutils.persistence.PersistentDiskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class PersistentKnowledgeController {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/knowledge");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        StringWriter buffer = new StringWriter();
        try (Writer writer = buffer) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            LOGGER.warn("failed to serialize knowledge data", e);
            return;
        }
        if (!PersistentDiskManager.writeAtomic(getKnowledgePath(), buffer.toString())) {
            LOGGER.warn("failed to save knowledge.json");
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
        return PersistentDiskManager.resolveUserPath(KNOWLEDGE_FILE);
    }

    public static class KnowledgeData {
        public Map<String, Float> prestigeEnhancements = new HashMap<>();
    }
}
