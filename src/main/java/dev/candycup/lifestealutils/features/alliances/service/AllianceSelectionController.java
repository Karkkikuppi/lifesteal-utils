package dev.candycup.lifestealutils.features.alliances.service;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.candycup.lifestealutils.features.alliances.Alliances;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AllianceSelectionController {
   private AllianceSelectionController() {
   }

   public static CompletableFuture<Suggestions> suggestOnlinePlayerNames(String remaining, SuggestionsBuilder builder) {
      Set<String> suggestions = new HashSet<>();
      for (String playerName : getOnlinePlayerNames()) {
         if (matchesSuggestion(remaining, playerName)) {
            suggestions.add(playerName);
         }
      }

      suggestions.stream()
              .sorted(String.CASE_INSENSITIVE_ORDER)
              .forEach(builder::suggest);
      return builder.buildFuture();
   }

   public static CompletableFuture<Suggestions> suggestCurrentAllianceMemberNames(String remaining, SuggestionsBuilder builder) {
      Set<String> suggestions = new HashSet<>();
      for (String playerName : Alliances.getAllianceDisplayNames()) {
         if (matchesSuggestion(remaining, playerName)) {
            suggestions.add(playerName);
         }
      }

      suggestions.stream()
              .sorted(String.CASE_INSENSITIVE_ORDER)
              .forEach(builder::suggest);
      return builder.buildFuture();
   }

   public static int addCurrentAllianceMemberByName(String rawPlayerName) {
      String playerName = rawPlayerName == null ? "" : rawPlayerName.trim();
      if (playerName.isEmpty()) {
         MessagingUtils.showMiniMessage("<red>Please provide a player name.</red>");
         return 0;
      }

      String resolvedName = resolveOnlinePlayerName(playerName);
      Alliances.addAllianceAsync(resolvedName, added -> Minecraft.getInstance().execute(() -> {
         String escapedName = escapeMiniMessage(resolvedName);
         if (added) {
            MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<green>Added <white>" + escapedName + "</white> to your alliance.</green>"));
         } else {
            MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<red>Could not find player <white>" + escapedName + "</white>.</red>"));
         }
      }));
      return 1;
   }

   public static int removeCurrentAllianceMemberByName(String rawPlayerName) {
      String playerName = rawPlayerName == null ? "" : rawPlayerName.trim();
      if (playerName.isEmpty()) {
         MessagingUtils.showMiniMessage("<red>Please provide a player from the current friend list.</red>");
         return 0;
      }

      Alliances.removeAllianceAsync(playerName, removed -> Minecraft.getInstance().execute(() -> {
         String escapedName = escapeMiniMessage(playerName);
         if (removed) {
            MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<yellow>Removed <white>" + escapedName + "</white> from your alliance.</yellow>"));
         } else {
            MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<red>Could not find player <white>" + escapedName + "</white> in your alliance.</red>"));
         }
      }));
      return 1;
   }

   public static int listCurrentAllianceMembers() {
      Alliances.showAllianceList();
      return 1;
   }

   private static List<String> getOnlinePlayerNames() {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      if (connection == null) {
         return List.of();
      }

      List<String> playerNames = new ArrayList<>();
      for (var playerInfo : connection.getOnlinePlayers()) {
         if (playerInfo == null || playerInfo.getProfile() == null) {
            continue;
         }

         //? if >1.21.8 {
         String playerName = playerInfo.getProfile().name();
         //?} else {
         /*String playerName = playerInfo.getProfile().getName();
          *///?}
         if (playerName != null && !playerName.isBlank()) {
            playerNames.add(playerName);
         }
      }
      return playerNames;
   }

   private static String resolveOnlinePlayerName(String query) {
      for (String playerName : getOnlinePlayerNames()) {
         if (playerName.equalsIgnoreCase(query)) {
            return playerName;
         }
      }
      return query;
   }

   private static boolean matchesSuggestion(String remaining, String suggestion) {
      if (suggestion == null || suggestion.isBlank()) {
         return false;
      }
      if (remaining == null || remaining.isBlank()) {
         return true;
      }
      return SharedSuggestionProvider.matchesSubStr(remaining, suggestion.toLowerCase(Locale.ROOT));
   }

   private static String escapeMiniMessage(String value) {
      return MiniMessage.miniMessage().escapeTags(value == null ? "" : value);
   }
}
