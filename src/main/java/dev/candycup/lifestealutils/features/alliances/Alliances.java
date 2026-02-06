package dev.candycup.lifestealutils.features.alliances;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.PlayerNameRenderEvent;
import dev.candycup.lifestealutils.event.listener.RenderEventListener;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * manages alliance system and colorizes alliance member names.
 * <p>
 * note: this class maintains a static API for commands and management,
 * but implements RenderEventListener for event-based name coloring.
 */
public final class Alliances implements RenderEventListener {
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   @Override
   public boolean isEnabled() {
      return Config.getEnableAlliances();
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   @Override
   public void onPlayerNameRender(PlayerNameRenderEvent event) {
      if (!isAlliedName(event.getPlayerName())) return;

      Component modified = colorizeNameTag(event.getModifiedDisplayName());
      event.setModifiedDisplayName(modified);
   }

   public static void showAllianceList() {
      List<String> entries = getAllianceDisplayNames();
      StringBuilder builder = new StringBuilder();
      builder.append("\n");
      builder.append("<red>   Lifesteal Utils (members in your Alliance)</red>\n");
      if (entries.isEmpty()) {
         builder.append("<bold>   <red>x</red>  </bold><white>None</white>\n");
      } else {
         for (String entry : entries) {
            builder.append("<bold>   <red>x</red>  </bold><white>")
                    .append(MINI_MESSAGE.escapeTags(entry))
                    .append("</white>\n");
         }
      }
      builder.append("\n");
      builder.append("<gray>   /lsu alliances add <username>\n");
      builder.append("<gray>   /lsu alliances remove <username>\n");
      builder.append("<gray>   /lsu alliances clear\n");
      appendWarningIfDisabled(builder);
      builder.append("\n");

      MessagingUtils.showMiniMessage(builder.toString());
   }

   public static boolean addAlliance(Player player) {
      if (player == null) return false;
      UUID uuid = player.getUUID();
      if (uuid == null) return false;
      boolean added = addAllianceUuid(uuid);
      if (added) {
         Component nameComponent = player.getName();
         if (nameComponent != null) {
            String name = nameComponent.getString();
            if (name != null && !name.isBlank()) {
               UuidResolver.updateCache(uuid, name);
            }
         }
         playAllianceSound();
      }
      return added;
   }

   public static Boolean toggleAlliance(Player player) {
      if (player == null) return null;
      UUID uuid = player.getUUID();
      if (uuid == null) return null;
      List<String> list = new ArrayList<>(Config.getAllianceUuids());
      String id = uuid.toString();
      boolean added;
      if (list.contains(id)) {
         list.remove(id);
         added = false;
      } else {
         list.add(id);
         added = true;
         // cache the username from the player
         Component nameComponent = player.getName();
         if (nameComponent != null) {
            String name = nameComponent.getString();
            if (name != null && !name.isBlank()) {
               UuidResolver.updateCache(uuid, name);
            }
         }
      }
      Config.setAllianceUuids(list);
      playAllianceSound();
      return added;
   }

   public static boolean addAlliance(String usernameOrUuid) {
      UUID uuid = UuidResolver.resolveUuid(usernameOrUuid);
      if (uuid == null) return false;
      boolean added = addAllianceUuid(uuid);
      if (added) playAllianceSound();
      return added;
   }

   /**
    * adds an alliance asynchronously, resolving the UUID off-thread if needed.
    *
    * @param usernameOrUuid the username or UUID string
    * @param callback       called on the main thread with true if added, false if not found or already exists
    */
   public static void addAllianceAsync(String usernameOrUuid, Consumer<Boolean> callback) {
      UuidResolver.resolveUuidAsync(usernameOrUuid, uuid -> {
         if (uuid == null) {
            callback.accept(false);
            return;
         }
         boolean added = addAllianceUuid(uuid);
         if (added) playAllianceSound();
         callback.accept(added);
      });
   }

   public static boolean removeAlliance(String usernameOrUuid) {
      UUID uuid = UuidResolver.resolveUuid(usernameOrUuid);
      if (uuid == null) return false;
      List<String> list = new ArrayList<>(Config.getAllianceUuids());
      boolean removed = list.remove(uuid.toString());
      if (removed) {
         Config.setAllianceUuids(list);
         playAllianceSound();
      }
      return removed;
   }

   /**
    * removes an alliance asynchronously, resolving the UUID off-thread if needed.
    *
    * @param usernameOrUuid the username or UUID string
    * @param callback       called on the main thread with true if removed, false if not found
    */
   public static void removeAllianceAsync(String usernameOrUuid, Consumer<Boolean> callback) {
      UuidResolver.resolveUuidAsync(usernameOrUuid, uuid -> {
         if (uuid == null) {
            callback.accept(false);
            return;
         }
         List<String> list = new ArrayList<>(Config.getAllianceUuids());
         boolean removed = list.remove(uuid.toString());
         if (removed) {
            Config.setAllianceUuids(list);
            playAllianceSound();
         }
         callback.accept(removed);
      });
   }

   public static void clearAlliances() {
      Config.setAllianceUuids(new ArrayList<>());
   }

   public static boolean isAlliedName(String username) {
      if (username == null || username.isBlank()) return false;
      UUID uuid = UuidResolver.resolveOnlineUuidCached(username);
      if (uuid == null) return false;
      return Config.getAllianceUuids().contains(uuid.toString());
   }

   private static Component colorizeNameTag(Component original) {
      String serialized = MiniMessage.miniMessage().serialize(MinecraftClientAudiences.of().asAdventure(original));
      String updated = applyColorToLastWord(serialized);
      if (updated.equals(serialized)) return original;
      return ensureMutable(MessagingUtils.miniMessage(updated));
   }

   private static Component ensureMutable(Component component) {
      if (component instanceof MutableComponent) return component;
      MutableComponent wrapper = Component.literal("");
      wrapper.append(component);
      return wrapper;
   }

   public static String getLastVisibleWord(String miniMessage) {
      if (miniMessage == null || miniMessage.isBlank()) return null;
      VisibleMapping mapping = VisibleMapping.fromMiniMessage(miniMessage);
      String visible = mapping.visible;
      int end = lastNonWhitespaceIndex(visible);
      if (end < 0) return null;
      int start = end;
      while (start > 0 && !Character.isWhitespace(visible.charAt(start - 1))) {
         start--;
      }
      return visible.substring(start, end + 1);
   }

   private static String applyColorToLastWord(String miniMessage) {
      if (miniMessage == null || miniMessage.isBlank()) return miniMessage;

      String colorTag = normalizeColorTag(Config.getAllianceNameColorTag());
      if (colorTag == null) return miniMessage;

      VisibleMapping mapping = VisibleMapping.fromMiniMessage(miniMessage);
      String visible = mapping.visible;
      int end = lastNonWhitespaceIndex(visible);
      if (end < 0) return miniMessage;

      int start = end;
      while (start > 0 && !Character.isWhitespace(visible.charAt(start - 1))) {
         start--;
      }

      if (start >= mapping.visibleToRaw.size() || end >= mapping.visibleToRaw.size()) return miniMessage;

      int rawStart = mapping.visibleToRaw.get(start);
      int rawEnd = mapping.visibleToRaw.get(end);

      String openTag = "<" + colorTag + ">";
      String closeTag = "</" + colorTag + ">";

      StringBuilder sb = new StringBuilder(miniMessage);
      sb.insert(rawEnd + 1, closeTag);
      sb.insert(rawStart, openTag);
      return sb.toString();
   }

   private static String normalizeColorTag(String raw) {
      if (raw == null) return null;
      String trimmed = raw.trim();
      if (trimmed.isEmpty()) return null;
      if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
         trimmed = trimmed.substring(1, trimmed.length() - 1);
      }
      if (trimmed.startsWith("/")) {
         trimmed = trimmed.substring(1);
      }
      return trimmed.isEmpty() ? null : trimmed;
   }

   private static boolean addAllianceUuid(UUID uuid) {
      if (uuid == null) return false;
      List<String> list = new ArrayList<>(Config.getAllianceUuids());
      String id = uuid.toString();
      if (list.contains(id)) return false;
      list.add(id);
      Config.setAllianceUuids(list);
      return true;
   }

   private static void playAllianceSound() {
      Minecraft client = Minecraft.getInstance();
      if (client == null || client.getSoundManager() == null) return;
      client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f));
   }

   public static String withDisabledWarning(String message) {
      if (Config.getEnableAlliances()) return message;
      return message + "\n<red><italic>Warning! You've disabled the alliances feature. Alliance members won't be highlighted!</italic></red>";
   }

   private static void appendWarningIfDisabled(StringBuilder builder) {
      if (!Config.getEnableAlliances()) {
         builder.append("<red><italic>Warning! You've disabled the alliances feature. Alliance members won't be highlighted!</italic></red>\n");
      }
   }

   private static int lastNonWhitespaceIndex(String value) {
      for (int i = value.length() - 1; i >= 0; i--) {
         if (!Character.isWhitespace(value.charAt(i))) return i;
      }
      return -1;
   }

   private static List<String> getAllianceDisplayNames() {
      List<String> entries = new ArrayList<>();
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      for (String id : Config.getAllianceUuids()) {
         String name = null;
         if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
               if (info == null || info.getProfile() == null) continue;
               UUID uuid = getProfileId(info);
               if (uuid != null && id.equals(uuid.toString())) {
                  name = getProfileName(info);
                  if (name != null) {
                     UuidResolver.updateCache(uuid, name);
                  }
                  break;
               }
            }
         }
         if (name == null) {
            name = UuidResolver.getCachedUsername(id);
         }
         entries.add(name != null ? name : id);
      }
      entries.sort(Comparator.comparing(String::toLowerCase));
      return entries;
   }

   private record VisibleMapping(String visible, List<Integer> visibleToRaw) {

      private static VisibleMapping fromMiniMessage(String miniMessage) {
         StringBuilder visible = new StringBuilder();
         List<Integer> mapping = new ArrayList<>();
         boolean inTag = false;
         for (int i = 0; i < miniMessage.length(); i++) {
            char c = miniMessage.charAt(i);
            if (inTag) {
               if (c == '>') inTag = false;
               continue;
            }
            if (c == '<') {
               inTag = true;
               continue;
            }
            mapping.add(i);
            visible.append(c);
         }
         return new VisibleMapping(visible.toString(), mapping);
      }
   }

   private static String getProfileName(PlayerInfo info) {
      Object profile = info.getProfile();
      if (profile == null) return null;
      try {
         Method method = profile.getClass().getMethod("getName");
         Object value = method.invoke(profile);
         return value != null ? value.toString() : null;
      } catch (Exception ignored) {
      }
      try {
         Method method = profile.getClass().getMethod("name");
         Object value = method.invoke(profile);
         return value != null ? value.toString() : null;
      } catch (Exception ignored) {
      }
      return null;
   }

   private static UUID getProfileId(PlayerInfo info) {
      Object profile = info.getProfile();
      if (profile == null) return null;
      try {
         Method method = profile.getClass().getMethod("getId");
         Object value = method.invoke(profile);
         return value instanceof UUID ? (UUID) value : null;
      } catch (Exception ignored) {
      }
      try {
         Method method = profile.getClass().getMethod("id");
         Object value = method.invoke(profile);
         return value instanceof UUID ? (UUID) value : null;
      } catch (Exception ignored) {
      }
      return null;
   }
}
