package dev.candycup.lifestealutils.features.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;

/**
 * Applies configured resource-pack JSON overrides directly to a downloaded/cached pack zip before apply.
 */
public final class ResourcePackZipEditor {
   private static final String DEFAULT_FONT_PATH = "assets/minecraft/font/default.json";
   private static final String PAPER_MODEL_PATH = "assets/minecraft/models/item/paper.json";
   private static final String PAPER_ITEM_PATH = "assets/minecraft/items/paper.json";
   private static final Set<String> SHIELD_PATHS = Set.of("assets/minecraft/items/shield.json");
   private static final Set<String> SWORD_PATHS = Set.of("assets/minecraft/items/diamond_sword.json", "assets/minecraft/items/netherite_sword.json");
   private static final Set<String> AXE_PATHS = Set.of("assets/minecraft/items/diamond_axe.json", "assets/minecraft/items/netherite_axe.json");
   private static final Set<String> PICKAXE_PATHS = Set.of("assets/minecraft/items/diamond_pickaxe.json", "assets/minecraft/items/netherite_pickaxe.json");
   private static final Set<String> SHOVEL_PATHS = Set.of("assets/minecraft/items/diamond_pickaxe.json", "assets/minecraft/items/netherite_pickaxe.json");
   private static final Set<String> MACE_PATHS = Set.of("assets/minecraft/items/mace.json");
   private static final Set<String> BOW_PATHS = Set.of("assets/minecraft/items/bow.json");
   private static final Set<String> CROSSBOW_PATHS = Set.of("assets/minecraft/items/crossbow.json");

   private ResourcePackZipEditor() {
   }

   /**
    * Applies configured pack overrides to the given zip path in-place.
    *
    * @param zipPath path to the server pack zip file
    * @param logger  logger used for diagnostics
    */
   public static void applyConfiguredOverrides(Path zipPath, Logger logger) {
      OverrideOptions options = OverrideOptions.fromConfig();
      if (!options.hasAnyEnabledRule()) {
         return;
      }

      try {
         applyOverrides(zipPath, logger, options);
      } catch (IOException exception) {
         logger.warn("[lsu-pack-override] failed to apply overrides to {}", zipPath, exception);
      }
   }

   private static void applyOverrides(Path zipPath, Logger logger, OverrideOptions options) throws IOException {
      Path tempZipPath = Files.createTempFile(zipPath.getParent(), "lsu-rp-edit-", ".zip");
      boolean changed = false;

      try (ZipFile sourceZip = new ZipFile(zipPath.toFile());
           ZipOutputStream targetZip = new ZipOutputStream(Files.newOutputStream(tempZipPath))) {
         Enumeration<? extends ZipEntry> entries = sourceZip.entries();
         while (entries.hasMoreElements()) {
            ZipEntry sourceEntry = entries.nextElement();

            EntryMutation mutation = mutateEntry(sourceZip, sourceEntry, logger, options);
            if (mutation.deleteEntry()) {
               changed = true;
               continue;
            }

            if (mutation.replacedContents()) {
               changed = true;
            }

            ZipEntry targetEntry = new ZipEntry(sourceEntry.getName());
            targetEntry.setComment(sourceEntry.getComment());
            targetEntry.setTime(sourceEntry.getTime());
            targetZip.putNextEntry(targetEntry);
            if (!sourceEntry.isDirectory()) {
               targetZip.write(mutation.contents());
            }
            targetZip.closeEntry();
         }
      }

      if (changed) {
         Files.move(tempZipPath, zipPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         logger.info("[lsu-pack-override] applied configured removals to {}", zipPath);
      } else {
         Files.deleteIfExists(tempZipPath);
      }
   }

   /**
    * zip layer: reads bytes and delegates behavior decisions to feature rules.
    */
   private static EntryMutation mutateEntry(ZipFile sourceZip, ZipEntry sourceEntry, Logger logger, OverrideOptions options) throws IOException {
      String entryName = sourceEntry.getName();
      if (shouldRemoveEntry(entryName, options)) {
         return EntryMutation.delete();
      }

      if (sourceEntry.isDirectory()) {
         return EntryMutation.keep(new byte[0]);
      }

      byte[] originalContents = readAllBytes(sourceZip.getInputStream(sourceEntry));
      byte[] transformedContents = transformEntryContents(entryName, originalContents, logger, options);
      boolean replaced = transformedContents != originalContents;
      return replaced ? EntryMutation.replace(transformedContents) : EntryMutation.keep(originalContents);
   }

   /**
    * feature layer: decides if an entry should be removed entirely.
    */
   private static boolean shouldRemoveEntry(String entryName, OverrideOptions options) {
      return matchesRule(options.removeAllShieldOverrides(), SHIELD_PATHS, entryName)
              || matchesRule(options.removeSwordSkins(), SWORD_PATHS, entryName)
              || matchesRule(options.removeAxeSkins(), AXE_PATHS, entryName)
              || matchesRule(options.removePickaxeSkins(), PICKAXE_PATHS, entryName)
              || matchesRule(options.removeShovelSkins(), SHOVEL_PATHS, entryName)
              || matchesRule(options.removeMaceSkins(), MACE_PATHS, entryName)
              || matchesRule(options.removeBowSkins(), BOW_PATHS, entryName)
              || matchesRule(options.removeCrossbowSkins(), CROSSBOW_PATHS, entryName);
   }

   private static boolean matchesRule(boolean enabled, Set<String> paths, String entryName) {
      return enabled && paths.contains(entryName);
   }

   /**
    * feature layer: transforms targeted JSON entry content while leaving unrelated entries untouched.
    */
   private static byte[] transformEntryContents(String entryName, byte[] entryBytes, Logger logger, OverrideOptions options) {
      if (!options.removeEmojis()) {
         return entryBytes;
      }

      if (DEFAULT_FONT_PATH.equals(entryName)) {
         return filterDefaultFontProviders(entryBytes, logger);
      }

      if (PAPER_MODEL_PATH.equals(entryName)) {
         return filterPaperModelOverrides(entryBytes, logger);
      }

      if (PAPER_ITEM_PATH.equals(entryName)) {
         return filterPaperItemEntries(entryBytes, logger);
      }

      return entryBytes;
   }

   private record EntryMutation(boolean deleteEntry, boolean replacedContents, byte[] contents) {
      private static EntryMutation delete() {
         return new EntryMutation(true, false, new byte[0]);
      }

      private static EntryMutation keep(byte[] contents) {
         return new EntryMutation(false, false, contents);
      }

      private static EntryMutation replace(byte[] contents) {
         return new EntryMutation(false, true, contents);
      }
   }

   private record OverrideOptions(
           boolean removeEmojis,
           boolean removeAllShieldOverrides,
           boolean removeSwordSkins,
           boolean removeAxeSkins,
           boolean removePickaxeSkins,
           boolean removeShovelSkins,
           boolean removeMaceSkins,
           boolean removeBowSkins,
           boolean removeCrossbowSkins
   ) {
      private static OverrideOptions fromConfig() {
         return new OverrideOptions(
                 ResourcePackOverrides.isRemoveEmojis(),
                 ResourcePackOverrides.isRemoveAllShieldOverrides(),
                 ResourcePackOverrides.isRemoveSwordSkins(),
                 ResourcePackOverrides.isRemoveAxeSkins(),
                 ResourcePackOverrides.isRemovePickaxeSkins(),
                 ResourcePackOverrides.isRemoveShovelSkins(),
                 ResourcePackOverrides.isRemoveMaceSkins(),
                 ResourcePackOverrides.isRemoveBowSkins(),
                 ResourcePackOverrides.isRemoveCrossbowSkins()
         );
      }

      private boolean hasAnyEnabledRule() {
         return removeEmojis
                 || removeAllShieldOverrides
                 || removeSwordSkins
                 || removeAxeSkins
                 || removePickaxeSkins
                 || removeShovelSkins
                 || removeMaceSkins
                 || removeBowSkins
                 || removeCrossbowSkins;
      }
   }

   private static byte[] filterDefaultFontProviders(byte[] original, Logger logger) {
      JsonObject root = parseJsonObject(original, DEFAULT_FONT_PATH, logger);
      if (root == null) {
         return original;
      }

      JsonArray providers = root.getAsJsonArray("providers");
      if (providers == null) {
         return original;
      }

      JsonArray filtered = new JsonArray();
      boolean changed = false;
      for (JsonElement providerElement : providers) {
         if (!providerElement.isJsonObject()) {
            filtered.add(providerElement);
            continue;
         }

         JsonObject providerObject = providerElement.getAsJsonObject();
         String file = getString(providerObject, "file");
         if (file != null && file.contains("minecraft:emojis")) {
            changed = true;
            continue;
         }

         filtered.add(providerElement);
      }

      if (!changed) {
         return original;
      }

      root.add("providers", filtered);
      return writeJson(root);
   }

   private static byte[] filterPaperModelOverrides(byte[] original, Logger logger) {
      JsonObject root = parseJsonObject(original, PAPER_MODEL_PATH, logger);
      if (root == null) {
         return original;
      }

      JsonArray overrides = root.getAsJsonArray("overrides");
      if (overrides == null) {
         return original;
      }

      JsonArray filtered = new JsonArray();
      boolean changed = false;
      for (JsonElement overrideElement : overrides) {
         if (!overrideElement.isJsonObject()) {
            filtered.add(overrideElement);
            continue;
         }

         JsonObject overrideObject = overrideElement.getAsJsonObject();
         String model = getString(overrideObject, "model");
         if (model != null && model.startsWith("emojis/")) {
            changed = true;
            continue;
         }

         filtered.add(overrideElement);
      }

      if (!changed) {
         return original;
      }

      root.add("overrides", filtered);
      return writeJson(root);
   }

   private static byte[] filterPaperItemEntries(byte[] original, Logger logger) {
      JsonObject root = parseJsonObject(original, PAPER_ITEM_PATH, logger);
      if (root == null) {
         return original;
      }

      JsonObject modelRoot = root.getAsJsonObject("model");
      if (modelRoot == null) {
         return original;
      }

      JsonArray entries = modelRoot.getAsJsonArray("entries");
      if (entries == null) {
         return original;
      }

      JsonArray filtered = new JsonArray();
      boolean changed = false;
      for (JsonElement entryElement : entries) {
         if (!entryElement.isJsonObject()) {
            filtered.add(entryElement);
            continue;
         }

         JsonObject entryObject = entryElement.getAsJsonObject();
         JsonObject modelObject = entryObject.getAsJsonObject("model");
         if (modelObject == null) {
            filtered.add(entryElement);
            continue;
         }

         String model = getString(modelObject, "model");
         if (model != null && model.startsWith("emojis/")) {
            changed = true;
            continue;
         }

         filtered.add(entryElement);
      }

      if (!changed) {
         return original;
      }

      modelRoot.add("entries", filtered);
      return writeJson(root);
   }

   private static JsonObject parseJsonObject(byte[] inputBytes, String entryName, Logger logger) {
      try {
         JsonElement parsed = JsonParser.parseString(new String(inputBytes, StandardCharsets.UTF_8));
         if (!parsed.isJsonObject()) {
            return null;
         }
         return parsed.getAsJsonObject();
      } catch (RuntimeException exception) {
         logger.warn("[lsu-pack-override] failed to parse {}", entryName, exception);
         return null;
      }
   }

   private static byte[] writeJson(JsonObject jsonObject) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
         writer.write(jsonObject.toString());
      } catch (IOException exception) {
         throw new IllegalStateException("failed to encode json", exception);
      }
      return outputStream.toByteArray();
   }

   private static String getString(JsonObject object, String key) {
      JsonElement element = object.get(key);
      if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
         return null;
      }

      return element.getAsString();
   }

   private static byte[] readAllBytes(InputStream inputStream) throws IOException {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int read;
      while ((read = inputStream.read(chunk)) != -1) {
         buffer.write(chunk, 0, read);
      }
      return buffer.toByteArray();
   }
}