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
import net.minecraft.client.resources.language.I18n;

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
