package dev.candycup.lifestealutils.features.alliances;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.PlayerNameRenderEvent;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.features.alliances.service.AllianceManagers;
import dev.candycup.lifestealutils.features.alliances.service.PlayerUuidResolver;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AllianceNameRenderHandler {
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

   private static volatile boolean prefixRefreshInFlight = false;
   private static volatile List<PrefixCandidate> cachedPrefixCandidates = List.of();

   public AllianceNameRenderHandler() {
      LifestealUtilsEvents.PLAYER_NAME_RENDER.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onPlayerNameRender(event);
      });
   }

   public boolean isEnabled() {
      return Config.isEnableAlliances();
   }

   public void onPlayerNameRender(PlayerNameRenderEvent event) {
      applyAllianceFormatting(event);
   }

   private static boolean applyAllianceFormatting(PlayerNameRenderEvent event) {
      PrefixCandidate selectedCandidate = resolveSelectedPrefixCandidate();
      if (selectedCandidate == null) {
         return false;
      }

      if (!isEventPlayerInCandidate(event, selectedCandidate)) {
         return false;
      }

      String colorTag = normalizeColorTag(selectedCandidate.color());
      Component result = event.getModifiedDisplayName();
      if (colorTag != null) {
         result = colorizeNameTag(result, colorTag);
      }

      if (event.getRenderContext() == PlayerNameRenderEvent.RenderContext.NAMETAG
              && Config.isAllianceNamePrefixEnabled()
              && selectedCandidate.prefix() != null
              && !selectedCandidate.prefix().isBlank()) {
         Component prefixComponent = Component.literal(selectedCandidate.prefix());
         Integer rgb = parseHexRgb(colorTag);
         if (rgb != null) {
            prefixComponent = prefixComponent.copy().withStyle(style -> style.withColor(TextColor.fromRgb(rgb)));
         }

         Component separator = Component.literal(" | ").withStyle(ChatFormatting.GRAY);
         MutableComponent prefixed = Component.literal("");
         prefixed.append(prefixComponent);
         prefixed.append(separator);
         prefixed.append(result);
         result = prefixed;
      }

      event.setModifiedDisplayName(ensureMutable(result));
      return true;
   }

   private static boolean isEventPlayerInCandidate(PlayerNameRenderEvent event, PrefixCandidate candidate) {
      String eventPlayerUuid = null;
      UUID resolved = PlayerUuidResolver.resolveOnlineUuidCached(event.getPlayerName());
      if (resolved != null) {
         eventPlayerUuid = normalizeUuid(resolved.toString());
      }

      if (eventPlayerUuid == null || eventPlayerUuid.isBlank()) {
         return false;
      }

      return candidate.memberUuids().contains(eventPlayerUuid);
   }

   public static void refreshPrefixCandidatesNow() {
      if (prefixRefreshInFlight) {
         return;
      }

      prefixRefreshInFlight = true;

      try {
         AllianceManagers.fetchPlayerAlliances()
                 .thenAccept(AllianceNameRenderHandler::updatePrefixCandidates)
                 .exceptionally(error -> {
                    prefixRefreshInFlight = false;
                    return null;
                 });
      } catch (RuntimeException ignored) {
         prefixRefreshInFlight = false;
      }
   }

   private static void updatePrefixCandidates(List<Alliance> alliances) {
      try {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player == null) {
            cachedPrefixCandidates = List.of();
            return;
         }

         String selfUuid = normalizeUuid(minecraft.player.getStringUUID());
         if (selfUuid.isBlank()) {
            cachedPrefixCandidates = List.of();
            return;
         }

         List<PrefixCandidate> candidates = new ArrayList<>();
         for (Alliance alliance : alliances) {
            if (alliance == null || alliance.id() == null || alliance.id().isBlank()) {
               continue;
            }
            String prefix = alliance.prefix();
            AllianceMember selfMember = null;
            if (alliance.isModern()) {
               selfMember = alliance.members().stream()
                       .filter(Objects::nonNull)
                       .filter(member -> normalizeUuid(member.uuid()).equalsIgnoreCase(selfUuid))
                       .findFirst()
                       .orElse(null);
            }

            List<String> memberUuids = alliance.members().stream()
                    .filter(Objects::nonNull)
                    .map(AllianceMember::uuid)
                    .map(AllianceNameRenderHandler::normalizeUuid)
                    .filter(id -> !id.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            String ownerDisplayName = alliance.members().stream()
                    .filter(Objects::nonNull)
                    .filter(member -> normalizeUuid(member.uuid()).equalsIgnoreCase(normalizeUuid(alliance.ownedBy())))
                    .map(AllianceMember::cachedName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .orElse(alliance.name());

            String displayName = ownerDisplayName.equalsIgnoreCase(alliance.name())
                    ? alliance.name()
                    : ownerDisplayName + " - " + alliance.name();

            long joinedAtMillis = selfMember != null
                    ? selfMember.addedAt().toEpochMilli()
                    : alliance.createdAt().toEpochMilli();

            candidates.add(new PrefixCandidate(alliance.id(), displayName, prefix, alliance.color(), joinedAtMillis, memberUuids));
         }

         candidates.sort(Comparator.comparingLong(PrefixCandidate::joinedAtMillis).reversed());
         cachedPrefixCandidates = List.copyOf(candidates);
         syncPrefixPriorityConfig(candidates);
      } finally {
         prefixRefreshInFlight = false;
      }
   }

   private static void syncPrefixPriorityConfig(List<PrefixCandidate> newestFirstCandidates) {
      List<String> current = new ArrayList<>(Config.getAlliancePrefixPriority());
      boolean changed = current.removeIf(entry -> resolvePriorityEntry(entry, newestFirstCandidates) == null);

      for (int i = 0; i < current.size(); i++) {
         PrefixCandidate resolved = resolvePriorityEntry(current.get(i), newestFirstCandidates);
         if (resolved == null) {
            continue;
         }
         if (!resolved.displayName().equals(current.get(i))) {
            current.set(i, resolved.displayName());
            changed = true;
         }
      }

      if (current.isEmpty() && !newestFirstCandidates.isEmpty()) {
         List<String> defaults = newestFirstCandidates.stream()
                 .map(PrefixCandidate::displayName)
                 .collect(Collectors.toCollection(ArrayList::new));
         Config.setAlliancePrefixPriority(defaults);
         return;
      }

      for (PrefixCandidate candidate : newestFirstCandidates) {
         boolean present = current.stream().anyMatch(entry -> {
            PrefixCandidate resolved = resolvePriorityEntry(entry, newestFirstCandidates);
            return resolved != null && resolved.allianceId().equals(candidate.allianceId());
         });
         if (!present) {
            current.add(0, candidate.displayName());
            changed = true;
         }
      }

      if (changed) {
         Config.setAlliancePrefixPriority(current);
      }
   }

   private static PrefixCandidate resolveSelectedPrefixCandidate() {
      List<PrefixCandidate> candidates = cachedPrefixCandidates;
      if (candidates.isEmpty()) {
         return null;
      }

      Map<String, PrefixCandidate> candidatesByAllianceId = new HashMap<>();
      for (PrefixCandidate candidate : candidates) {
         candidatesByAllianceId.put(candidate.allianceId(), candidate);
      }

      for (String preferredAllianceId : Config.getAlliancePrefixPriority()) {
         PrefixCandidate candidate = candidatesByAllianceId.get(preferredAllianceId);
         if (candidate == null) {
            candidate = resolvePriorityEntry(preferredAllianceId, candidates);
         }
         if (hasFormatting(candidate)) {
            return candidate;
         }
      }

      for (PrefixCandidate candidate : candidates) {
         if (hasFormatting(candidate)) {
            return candidate;
         }
      }

      return null;
   }

   private static PrefixCandidate resolvePriorityEntry(String priorityEntry, List<PrefixCandidate> candidates) {
      if (priorityEntry == null || priorityEntry.isBlank()) {
         return null;
      }

      for (PrefixCandidate candidate : candidates) {
         if (priorityEntry.equals(candidate.allianceId())) {
            return candidate;
         }
      }

      for (PrefixCandidate candidate : candidates) {
         if (priorityEntry.equalsIgnoreCase(candidate.displayName())) {
            return candidate;
         }
      }

      return null;
   }

   private static Component colorizeNameTag(Component original, String colorTag) {
      String serialized = MINI_MESSAGE.serialize(MinecraftClientAudiences.of().asAdventure(original));
      String updated = applyColorToLastWord(serialized, colorTag);
      if (updated.equals(serialized)) return original;
      return ensureMutable(MessagingUtils.miniMessage(updated));
   }

   private static Component ensureMutable(Component component) {
      if (component instanceof MutableComponent) return component;
      MutableComponent wrapper = Component.literal("");
      wrapper.append(component);
      return wrapper;
   }

   private static String applyColorToLastWord(String miniMessage, String colorTagRaw) {
      if (miniMessage == null || miniMessage.isBlank()) return miniMessage;

      String colorTag = normalizeColorTag(colorTagRaw);
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
      if (!trimmed.startsWith("#")) {
         trimmed = "#" + trimmed;
      }
      return trimmed.isEmpty() ? null : trimmed;
   }

   private static Integer parseHexRgb(String colorTag) {
      if (colorTag == null || colorTag.isBlank() || !colorTag.startsWith("#") || colorTag.length() != 7) {
         return null;
      }

      try {
         return Integer.parseInt(colorTag.substring(1), 16);
      } catch (NumberFormatException ignored) {
         return null;
      }
   }

   private static String normalizeUuid(String uuid) {
      if (uuid == null || uuid.isBlank()) return "";
      return uuid.replace("-", "").toLowerCase(Locale.ROOT);
   }

   private static int lastNonWhitespaceIndex(String value) {
      for (int i = value.length() - 1; i >= 0; i--) {
         if (!Character.isWhitespace(value.charAt(i))) return i;
      }
      return -1;
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

   private record PrefixCandidate(String allianceId, String displayName, String prefix, String color,
                                  long joinedAtMillis, List<String> memberUuids) {
   }

   private static boolean hasFormatting(PrefixCandidate candidate) {
      if (candidate == null) {
         return false;
      }
      boolean hasPrefix = candidate.prefix() != null && !candidate.prefix().isBlank();
      boolean hasColor = candidate.color() != null && !candidate.color().isBlank();
      return hasPrefix || hasColor;
   }
}
