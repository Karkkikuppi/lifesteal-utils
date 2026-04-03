package dev.candycup.lifestealutils.features.alliances;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.PlayerNameRenderEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ServerChangeEvent;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.features.alliances.service.AllianceManagers;
import dev.candycup.lifestealutils.features.alliances.service.PlayerUuidResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AllianceNameRenderHandler {
   private static final Object PREFIX_REFRESH_LOCK = new Object();
   private static volatile boolean prefixRefreshInFlight = false;
   private static volatile boolean prefixRefreshQueued = false;
   private static volatile List<PrefixCandidate> cachedPrefixCandidates = List.of();

   public AllianceNameRenderHandler() {
      LifestealUtilsEvents.PLAYER_NAME_RENDER.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onPlayerNameRender(event);
      });
      LifestealUtilsEvents.SERVER_CHANGE.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onServerChange(event);
      });
      LifestealUtilsEvents.GATEWAY_CONNECTED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         refreshPrefixCandidatesNow();
      });
   }

   public boolean isEnabled() {
      return Config.isEnableAlliances();
   }

   public void onPlayerNameRender(PlayerNameRenderEvent event) {
      applyAllianceFormatting(event);
   }

   public void onServerChange(ServerChangeEvent event) {
      if (event == null) {
         return;
      }

      if (event.isDisconnected()) {
         clearPrefixCandidates();
         return;
      }

      if (event.isConnected()) {
         refreshPrefixCandidatesNow();
      }
   }

   private static boolean applyAllianceFormatting(PlayerNameRenderEvent event) {
      String eventPlayerUuid = resolveEventPlayerUuid(event);
      if (eventPlayerUuid == null || eventPlayerUuid.isBlank()) {
         return false;
      }

      PrefixCandidate selectedCandidate = resolveHitboxPrefixCandidate(eventPlayerUuid);
      if (selectedCandidate == null) {
         return false;
      }

      String colorTag = resolveEffectiveColorTag(selectedCandidate);
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

   private static String resolveEventPlayerUuid(PlayerNameRenderEvent event) {
      String eventPlayerUuid = null;
      UUID resolved = PlayerUuidResolver.resolveOnlineUuidCached(event.getPlayerName());
      if (resolved != null) {
         eventPlayerUuid = normalizeUuid(resolved.toString());
      }

      return eventPlayerUuid;
   }

   public static void refreshPrefixCandidatesNow() {
      synchronized (PREFIX_REFRESH_LOCK) {
         if (prefixRefreshInFlight) {
            prefixRefreshQueued = true;
            return;
         }

         prefixRefreshInFlight = true;
         prefixRefreshQueued = false;
      }

      try {
         AllianceManagers.fetchPlayerAlliances()
                 .thenAccept(AllianceNameRenderHandler::updatePrefixCandidates)
                 .exceptionally(error -> {
                    finishPrefixRefresh();
                    return null;
                 });
      } catch (RuntimeException ignored) {
         finishPrefixRefresh();
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
         finishPrefixRefresh();
      }
   }

   private static void clearPrefixCandidates() {
      cachedPrefixCandidates = List.of();
      synchronized (PREFIX_REFRESH_LOCK) {
         prefixRefreshInFlight = false;
         prefixRefreshQueued = false;
      }
   }

   private static void finishPrefixRefresh() {
      boolean shouldRefreshAgain;
      synchronized (PREFIX_REFRESH_LOCK) {
         prefixRefreshInFlight = false;
         shouldRefreshAgain = prefixRefreshQueued;
         prefixRefreshQueued = false;
      }

      if (shouldRefreshAgain) {
         refreshPrefixCandidatesNow();
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

   public static List<HitboxAllianceCandidate> getHitboxCandidates() {
      return cachedPrefixCandidates.stream()
              .map(candidate -> new HitboxAllianceCandidate(candidate.allianceId(), candidate.displayName(), candidate.color()))
              .toList();
   }

   public static HitboxAllianceCandidate resolveHitboxCandidate(String playerUuid) {
      PrefixCandidate candidate = resolveHitboxPrefixCandidate(playerUuid);
      if (candidate == null) {
         return null;
      }
      return new HitboxAllianceCandidate(candidate.allianceId(), candidate.displayName(), candidate.color());
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

   private static PrefixCandidate resolveHitboxPrefixCandidate(String playerUuid) {
      String normalizedPlayerUuid = normalizeUuid(playerUuid);
      if (normalizedPlayerUuid.isBlank()) {
         return null;
      }

      List<PrefixCandidate> candidates = cachedPrefixCandidates;
      if (candidates.isEmpty()) {
         return null;
      }

      for (String preferredAllianceId : Config.getAlliancePrefixPriority()) {
         PrefixCandidate candidate = resolvePriorityEntry(preferredAllianceId, candidates);
         if (candidate != null && candidate.memberUuids().contains(normalizedPlayerUuid)) {
            return candidate;
         }
      }

      for (PrefixCandidate candidate : candidates) {
         if (candidate.memberUuids().contains(normalizedPlayerUuid)) {
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
      Integer rgb = parseHexRgb(colorTag);
      if (rgb == null) {
         return original;
      }

      try {
         List<StyledTextSegment> segments = flattenStyledText(original);
         if (segments.isEmpty()) {
            return original;
         }

         WordRange lastWordRange = findLastWordRange(segments);
         if (lastWordRange == null) {
            return original;
         }

         MutableComponent rebuilt = Component.empty();
         TextColor color = TextColor.fromRgb(rgb);
         int offset = 0;
         boolean changed = false;

         for (StyledTextSegment segment : segments) {
            String text = segment.text();
            int segmentStart = offset;
            int segmentEnd = offset + text.length();
            int highlightStart = Math.max(lastWordRange.startInclusive(), segmentStart);
            int highlightEnd = Math.min(lastWordRange.endExclusive(), segmentEnd);

            if (highlightStart >= highlightEnd) {
               appendStyledText(rebuilt, text, segment.style());
            } else {
               int localStart = highlightStart - segmentStart;
               int localEnd = highlightEnd - segmentStart;
               appendStyledText(rebuilt, text.substring(0, localStart), segment.style());
               appendStyledText(rebuilt, text.substring(localStart, localEnd), segment.style().withColor(color));
               appendStyledText(rebuilt, text.substring(localEnd), segment.style());
               changed = true;
            }

            offset = segmentEnd;
         }

         return changed ? rebuilt : original;
      } catch (RuntimeException ignored) {
         return original;
      }
   }

   private static Component ensureMutable(Component component) {
      if (component instanceof MutableComponent) return component;
      MutableComponent wrapper = Component.literal("");
      wrapper.append(component);
      return wrapper;
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

   private static String resolveEffectiveColorTag(PrefixCandidate candidate) {
      if (candidate == null) {
         return null;
      }

      String configuredColor = Config.getAllianceHitboxColorOverride(
              candidate.allianceId(),
              AllianceHitboxColorResolver.normalizeConfigColor(candidate.color())
      );
      return normalizeColorTag(configuredColor);
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

   private static List<StyledTextSegment> flattenStyledText(Component component) {
      List<StyledTextSegment> segments = new ArrayList<>();
      component.visit((style, text) -> {
         if (text != null && !text.isEmpty()) {
            segments.add(new StyledTextSegment(text, style));
         }
         return Optional.empty();
      }, Style.EMPTY);
      return segments;
   }

   private static WordRange findLastWordRange(List<StyledTextSegment> segments) {
      int totalLength = segments.stream().mapToInt(segment -> segment.text().length()).sum();
      if (totalLength == 0) {
         return null;
      }

      StringBuilder visible = new StringBuilder(totalLength);
      for (StyledTextSegment segment : segments) {
         visible.append(segment.text());
      }

      int end = lastNonWhitespaceIndex(visible.toString());
      if (end < 0) {
         return null;
      }

      int start = end;
      while (start > 0 && !Character.isWhitespace(visible.charAt(start - 1))) {
         start--;
      }
      return new WordRange(start, end + 1);
   }

   private static void appendStyledText(MutableComponent target, String text, Style style) {
      if (text == null || text.isEmpty()) {
         return;
      }
      target.append(Component.literal(text).withStyle(style));
   }

   private record StyledTextSegment(String text, Style style) {
   }

   private record WordRange(int startInclusive, int endExclusive) {
   }

   private record PrefixCandidate(String allianceId, String displayName, String prefix, String color,
                                  long joinedAtMillis, List<String> memberUuids) {
   }

   public record HitboxAllianceCandidate(String allianceId, String displayName, String color) {
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
