package dev.candycup.lifestealutils.features.alliances.service;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.features.alliances.AllianceNameRenderHandler;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AllianceSelectionController {
   private AllianceSelectionController() {
   }

   public static CompletableFuture<Suggestions> suggestAllianceNames(String remaining, SuggestionsBuilder builder) {
      return AllianceManagers.fetchPlayerAlliances().thenApply(playerAlliances -> {
         Set<String> seen = new HashSet<>();
         for (Alliance alliance : playerAlliances) {
            if (alliance == null) {
               continue;
            }

            String displayName = alliance.getDisplayName();
            if (displayName != null && !displayName.isBlank()) {
               String key = displayName.toLowerCase(Locale.ROOT);
               if ((remaining.isBlank() || key.contains(remaining)) && seen.add(key)) {
                  builder.suggest(displayName);
               }
            }

            String name = alliance.name();
            if (name != null && !name.isBlank()) {
               String key = name.toLowerCase(Locale.ROOT);
               if ((remaining.isBlank() || key.contains(remaining)) && seen.add(key)) {
                  builder.suggest(name);
               }
            }
         }
         return builder.build();
      }).exceptionally(error -> builder.build());
   }

   public static int selectAllianceByName(String rawAllianceName) {
      String allianceName = rawAllianceName == null ? "" : rawAllianceName.trim();
      if (allianceName.isEmpty()) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.required"));
         return 0;
      }

      AllianceManagers.fetchPlayerAlliances().thenAccept(playerAlliances -> {
         Minecraft.getInstance().execute(() -> {
            Alliance selectedAlliance = findAllianceByName(playerAlliances, allianceName);
            if (selectedAlliance == null) {
               MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.not_found", MiniMessage.miniMessage().escapeTags(allianceName)));
               return;
            }

            Config.setSelectedAllianceId(selectedAlliance.id());
            MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.success", MiniMessage.miniMessage().escapeTags(selectedAlliance.getDisplayName())));
         });
      });

      return 1;
   }

   public static CompletableFuture<Suggestions> suggestOnlinePlayerNames(String remaining, SuggestionsBuilder builder) {
      Set<String> seen = new HashSet<>();
      for (String playerName : getOnlinePlayerNames()) {
         String lowerName = playerName.toLowerCase(Locale.ROOT);
         if ((remaining.isBlank() || SharedSuggestionProvider.matchesSubStr(remaining, lowerName)) && seen.add(lowerName)) {
            builder.suggest(playerName);
         }
      }
      return builder.buildFuture();
   }

   public static CompletableFuture<Suggestions> suggestCurrentAllianceMemberNames(String remaining, SuggestionsBuilder builder) {
      String selectedAllianceId = Config.getSelectedAllianceId();
      if (selectedAllianceId.isBlank()) {
         return builder.buildFuture();
      }

      return AllianceManagers.fetchPlayerAlliances().thenApply(playerAlliances -> {
         Alliance selectedAlliance = findAllianceById(playerAlliances, selectedAllianceId);
         if (selectedAlliance == null) {
            return builder.build();
         }

         Set<String> seen = new HashSet<>();
         for (AllianceMember member : selectedAlliance.getJoinedMembers()) {
            String suggestion = getMemberSuggestion(member);
            if (suggestion == null || suggestion.isBlank()) {
               continue;
            }
            String lowerSuggestion = suggestion.toLowerCase(Locale.ROOT);
            if ((remaining.isBlank() || SharedSuggestionProvider.matchesSubStr(remaining, lowerSuggestion)) && seen.add(lowerSuggestion)) {
               builder.suggest(suggestion);
            }
         }
         return builder.build();
      }).exceptionally(error -> builder.build());
   }

   public static int addCurrentAllianceMemberByName(String rawPlayerName) {
      String playerName = rawPlayerName == null ? "" : rawPlayerName.trim();
      if (playerName.isEmpty()) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.friend.add.required"));
         return 0;
      }

      String selectedAllianceId = Config.getSelectedAllianceId();
      if (selectedAllianceId.isBlank()) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.none"));
         return 0;
      }

      AllianceManagers.fetchPlayerAlliances().thenAccept(playerAlliances -> {
         Minecraft.getInstance().execute(() -> {
            Alliance selectedAlliance = findAllianceById(playerAlliances, selectedAllianceId);
            if (selectedAlliance == null) {
               Config.setSelectedAllianceId("");
               MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.stale"));
               return;
            }

            String resolvedName = resolveOnlinePlayerName(playerName);
            PlayerUuidResolver.resolveUuidAsync(resolvedName, uuid -> {
               Minecraft.getInstance().execute(() -> {
                  if (uuid == null) {
                     MessagingUtils.showMiniMessage(I18n.get("lsu.friend.add.not_found", escapeMiniMessage(resolvedName)));
                     return;
                  }

                  String uuidString = uuid.toString();
                  String escapedName = escapeMiniMessage(resolvedName);
                  String escapedAllianceName = escapeMiniMessage(selectedAlliance.getDisplayName());
                  if (findMemberByUuid(selectedAlliance, uuidString) != null) {
                     MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.already_member", escapedName, escapedAllianceName));
                     return;
                  }

                  AllianceManagers.addMember(selectedAlliance, uuidString, resolvedName).thenAccept(success -> {
                     Minecraft.getInstance().execute(() -> {
                        if (success) {
                           cacheResolvedName(uuidString, resolvedName);
                           AllianceNameRenderHandler.refreshPrefixCandidatesNow();
                           MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.add_success", escapedName, escapedAllianceName));
                        } else {
                           MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.add_failed", escapedName, escapedAllianceName));
                        }
                     });
                  });
               });
            });
         });
      });

      return 1;
   }

   public static int removeCurrentAllianceMemberByName(String rawPlayerName) {
      String playerName = rawPlayerName == null ? "" : rawPlayerName.trim();
      if (playerName.isEmpty()) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.friend.remove.required"));
         return 0;
      }

      String selectedAllianceId = Config.getSelectedAllianceId();
      if (selectedAllianceId.isBlank()) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.none"));
         return 0;
      }

      AllianceManagers.fetchPlayerAlliances().thenAccept(playerAlliances -> {
         Minecraft.getInstance().execute(() -> {
            Alliance selectedAlliance = findAllianceById(playerAlliances, selectedAllianceId);
            if (selectedAlliance == null) {
               Config.setSelectedAllianceId("");
               MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.stale"));
               return;
            }

            AllianceMember existingMember = findJoinedMemberByQuery(selectedAlliance, playerName);
            String escapedAllianceName = escapeMiniMessage(selectedAlliance.getDisplayName());
            if (existingMember == null) {
               MessagingUtils.showMiniMessage(I18n.get("lsu.friend.remove.not_found", escapeMiniMessage(playerName), escapedAllianceName));
               return;
            }

            String displayName = existingMember.cachedName() != null && !existingMember.cachedName().isBlank()
                    ? existingMember.cachedName()
                    : playerName;
            String escapedName = escapeMiniMessage(displayName);
            AllianceManagers.removeMember(selectedAlliance, existingMember.id()).thenAccept(success -> {
               Minecraft.getInstance().execute(() -> {
                  if (success) {
                     AllianceNameRenderHandler.refreshPrefixCandidatesNow();
                     MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.remove_success", escapedName, escapedAllianceName));
                  } else {
                     MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.remove_failed", escapedName, escapedAllianceName));
                  }
               });
            });
         });
      });

      return 1;
   }

   public static int listCurrentAllianceMembers() {
      String selectedAllianceId = Config.getSelectedAllianceId();
      if (selectedAllianceId.isBlank()) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.none"));
         return 0;
      }

      AllianceManagers.fetchPlayerAlliances().thenAccept(playerAlliances -> {
         Minecraft.getInstance().execute(() -> {
            Alliance selectedAlliance = findAllianceById(playerAlliances, selectedAllianceId);
            if (selectedAlliance == null) {
               Config.setSelectedAllianceId("");
               MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.stale"));
               return;
            }

            List<String> entries = selectedAlliance.getJoinedMembers().stream()
                    .map(AllianceSelectionController::getMemberSuggestion)
                    .filter(name -> name != null && !name.isBlank())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(AllianceSelectionController::escapeMiniMessage)
                    .map(name -> "<white>" + name + "</white>")
                    .toList();

            String escapedAllianceName = escapeMiniMessage(selectedAlliance.getDisplayName());
            if (entries.isEmpty()) {
               MessagingUtils.showMiniMessage(I18n.get("lsu.friend.list.empty", escapedAllianceName));
               return;
            }

            MessagingUtils.showMiniMessage(I18n.get("lsu.friend.list.entries", escapedAllianceName, String.join("<gray>, </gray>", entries)));
         });
      });

      return 1;
   }

   public static void toggleSelectedAllianceMember(String targetUuid, String targetName) {
      String selectedAllianceId = Config.getSelectedAllianceId();
      if (selectedAllianceId.isBlank()) {
         MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.none"));
         return;
      }

      AllianceManagers.fetchPlayerAlliances().thenAccept(playerAlliances -> {
         Minecraft.getInstance().execute(() -> {
            Alliance selectedAlliance = findAllianceById(playerAlliances, selectedAllianceId);
            if (selectedAlliance == null) {
               Config.setSelectedAllianceId("");
               MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.stale"));
               return;
            }

            String escapedName = MiniMessage.miniMessage().escapeTags(targetName);
            String escapedAllianceName = MiniMessage.miniMessage().escapeTags(selectedAlliance.getDisplayName());
            AllianceMember existingMember = findMemberByUuid(selectedAlliance, targetUuid);
            if (existingMember != null) {
               AllianceManagers.removeMember(selectedAlliance, existingMember.id()).thenAccept(success -> {
                  Minecraft.getInstance().execute(() -> {
                     if (success) {
                        AllianceNameRenderHandler.refreshPrefixCandidatesNow();
                        MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.remove_success", escapedName, escapedAllianceName));
                     } else {
                        MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.remove_failed", escapedName, escapedAllianceName));
                     }
                  });
               });
               return;
            }

            AllianceManagers.addMember(selectedAlliance, targetUuid, targetName).thenAccept(success -> {
               Minecraft.getInstance().execute(() -> {
                  if (success) {
                     cacheResolvedName(targetUuid, targetName);
                     AllianceNameRenderHandler.refreshPrefixCandidatesNow();
                     MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.add_success", escapedName, escapedAllianceName));
                  } else {
                     MessagingUtils.showMiniMessage(I18n.get("lsu.alliances.select.add_failed", escapedName, escapedAllianceName));
                  }
               });
            });
         });
      });
   }

   private static void cacheResolvedName(String targetUuid, String targetName) {
      if (targetUuid == null || targetUuid.isBlank() || targetName == null || targetName.isBlank()) {
         return;
      }

      try {
         PlayerUuidResolver.updateCache(UUID.fromString(targetUuid), targetName);
      } catch (IllegalArgumentException ignored) {
      }
   }

   private static AllianceMember findJoinedMemberByQuery(Alliance alliance, String query) {
      if (alliance == null || query == null || query.isBlank()) {
         return null;
      }

      String normalizedQuery = normalizeUuid(query);
      for (AllianceMember member : alliance.getJoinedMembers()) {
         if (member == null) {
            continue;
         }
         if (member.uuid() != null && normalizeUuid(member.uuid()).equalsIgnoreCase(normalizedQuery)) {
            return member;
         }
         if (member.cachedName() != null && member.cachedName().equalsIgnoreCase(query)) {
            return member;
         }
      }

      return null;
   }

   private static AllianceMember findMemberByUuid(Alliance alliance, String targetUuid) {
      if (alliance == null || targetUuid == null || targetUuid.isBlank()) {
         return null;
      }

      String normalizedTargetUuid = normalizeUuid(targetUuid);
      for (AllianceMember member : alliance.members()) {
         if (member == null || member.uuid() == null) {
            continue;
         }
         if (normalizeUuid(member.uuid()).equalsIgnoreCase(normalizedTargetUuid)) {
            return member;
         }
      }
      return null;
   }

   private static List<String> getOnlinePlayerNames() {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      if (connection == null) {
         return List.of();
      }

      List<String> playerNames = new ArrayList<>();
      for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
         if (playerInfo == null || playerInfo.getProfile() == null) {
            continue;
         }

         String playerName = getProfileName(playerInfo);
         if (playerName != null && !playerName.isBlank()) {
            playerNames.add(playerName);
         }
      }
      return playerNames;
   }

   private static String getMemberSuggestion(AllianceMember member) {
      if (member == null) {
         return null;
      }
      if (member.cachedName() != null && !member.cachedName().isBlank()) {
         return member.cachedName();
      }
      return member.uuid();
   }

   private static String resolveOnlinePlayerName(String query) {
      for (String playerName : getOnlinePlayerNames()) {
         if (playerName.equalsIgnoreCase(query)) {
            return playerName;
         }
      }
      return query;
   }

   private static String getProfileName(PlayerInfo playerInfo) {
      Object profile = playerInfo.getProfile();
      if (profile == null) {
         return null;
      }

      try {
         return (String) profile.getClass().getMethod("getName").invoke(profile);
      } catch (Exception ignored) {
      }
      try {
         return (String) profile.getClass().getMethod("name").invoke(profile);
      } catch (Exception ignored) {
      }
      return null;
   }

   private static String escapeMiniMessage(String value) {
      return MiniMessage.miniMessage().escapeTags(value == null ? "" : value);
   }

   private static Alliance findAllianceById(List<Alliance> alliances, String allianceId) {
      for (Alliance alliance : alliances) {
         if (alliance != null && alliance.id().equals(allianceId)) {
            return alliance;
         }
      }
      return null;
   }

   private static Alliance findAllianceByName(List<Alliance> alliances, String query) {
      String lowered = query.toLowerCase(Locale.ROOT);
      for (Alliance alliance : alliances) {
         if (alliance == null) {
            continue;
         }

         if (alliance.getDisplayName().equalsIgnoreCase(query) || alliance.name().equalsIgnoreCase(query)) {
            return alliance;
         }
      }

      for (Alliance alliance : alliances) {
         if (alliance == null) {
            continue;
         }

         String displayName = alliance.getDisplayName().toLowerCase(Locale.ROOT);
         String name = alliance.name().toLowerCase(Locale.ROOT);
         if (displayName.contains(lowered) || name.contains(lowered)) {
            return alliance;
         }
      }

      return null;
   }

   private static String normalizeUuid(String uuid) {
      if (uuid == null) {
         return "";
      }
      return uuid.replace("-", "");
   }
}
