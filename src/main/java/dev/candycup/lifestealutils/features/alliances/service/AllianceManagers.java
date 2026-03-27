package dev.candycup.lifestealutils.features.alliances.service;

import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.features.alliances.models.AllianceType;
import dev.candycup.lifestealutils.gaia.AlliancesAPIClient;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class AllianceManagers {
   private static final AllianceManager MODERN = new ModernAllianceManager();
   private static final AllianceManager LOCAL = new LocalAllianceManager();

   private AllianceManagers() {
   }

   public static CompletableFuture<List<Alliance>> fetchPlayerAlliances() {
      CompletableFuture<List<Alliance>> localFuture = LOCAL.fetchPlayerAlliances().exceptionally(error -> List.of());
      CompletableFuture<List<Alliance>> modernFuture;
      CompletableFuture<List<Alliance>> modernInvitesFuture;

      try {
         modernFuture = MODERN.fetchPlayerAlliances().exceptionally(error -> List.of());
         modernInvitesFuture = AlliancesAPIClient.fetchPlayerInvites().exceptionally(error -> List.of());
      } catch (RuntimeException error) {
         modernFuture = CompletableFuture.completedFuture(List.of());
         modernInvitesFuture = CompletableFuture.completedFuture(List.of());
      }

      return modernFuture
              .thenCombine(modernInvitesFuture, (modern, invites) -> {
                 List<Alliance> result = new ArrayList<>(modern);
                 for (Alliance invite : invites) {
                    if (invite == null) {
                       continue;
                    }
                    boolean exists = result.stream().anyMatch(existing -> existing != null && existing.id().equals(invite.id()));
                    if (!exists) {
                       result.add(invite);
                    }
                 }
                 return result;
              })
              .thenCombine(localFuture, (modernAndInvites, local) -> {
                 List<Alliance> result = new ArrayList<>(modernAndInvites);
                 result.addAll(local);
                 return result;
              });
   }

   public static CompletableFuture<Alliance> createAlliance(AllianceType type, String name, String prefix, String color, String description, String motd) {
      return forType(type).createAlliance(name, prefix, color, description, motd);
   }

   public static CompletableFuture<Alliance> fetchAlliance(Alliance alliance) {
      if (alliance == null) {
         return CompletableFuture.completedFuture(null);
      }
      return forAlliance(alliance).fetchAlliance(alliance.id());
   }

   public static CompletableFuture<Alliance> updateAlliance(Alliance alliance, String name, String prefix, String color, String description, String motd) {
      if (alliance == null) {
         return CompletableFuture.completedFuture(null);
      }
      return forAlliance(alliance).updateAlliance(alliance.id(), name, prefix, color, description, motd);
   }

   public static CompletableFuture<Boolean> addMember(Alliance alliance, String uuid, String cachedName) {
      if (alliance == null) {
         return CompletableFuture.completedFuture(false);
      }
      return forAlliance(alliance).addMember(alliance.id(), uuid, cachedName);
   }

   public static CompletableFuture<Boolean> removeMember(Alliance alliance, String memberId) {
      if (alliance == null) {
         return CompletableFuture.completedFuture(false);
      }
      return forAlliance(alliance).removeMember(memberId);
   }

   public static CompletableFuture<Boolean> acceptInvitation(Alliance alliance) {
      if (alliance == null) {
         return CompletableFuture.completedFuture(false);
      }
      return forAlliance(alliance).acceptInvitation(alliance.id());
   }

   public static CompletableFuture<Boolean> rejectInvitation(Alliance alliance) {
      if (alliance == null) {
         return CompletableFuture.completedFuture(false);
      }
      return forAlliance(alliance).rejectInvitation(alliance.id());
   }

   public static CompletableFuture<Boolean> deleteAlliance(Alliance alliance) {
      if (alliance == null) {
         return CompletableFuture.completedFuture(false);
      }
      return forAlliance(alliance).deleteAlliance(alliance.id());
   }

   public static boolean hasActiveAlliance(List<Alliance> alliances) {
      return findActiveAlliance(alliances) != null;
   }

   public static Alliance findActiveAlliance(List<Alliance> alliances) {
      if (alliances == null || alliances.isEmpty()) {
         return null;
      }

      String playerUuid = getCurrentPlayerUuid();
      for (Alliance alliance : alliances) {
         if (isActiveAlliance(alliance, playerUuid)) {
            return alliance;
         }
      }
      return null;
   }

   private static AllianceManager forAlliance(Alliance alliance) {
      return forType(alliance.type());
   }

   private static AllianceManager forType(AllianceType type) {
      if (type == AllianceType.LOCAL) {
         return LOCAL;
      }
      return MODERN;
   }

   private static boolean isActiveAlliance(Alliance alliance, String playerUuid) {
      if (alliance == null) {
         return false;
      }
      if (alliance.isLocal()) {
         return true;
      }

      AllianceMember selfMember = findSelfMember(alliance, playerUuid);
      return selfMember == null || selfMember.isJoined();
   }

   private static boolean isInvitationForPlayer(Alliance alliance, String playerUuid) {
      AllianceMember selfMember = findSelfMember(alliance, playerUuid);
      return selfMember != null && selfMember.isInvited();
   }

   private static AllianceMember findSelfMember(Alliance alliance, String playerUuid) {
      if (alliance == null || playerUuid.isBlank()) {
         return null;
      }

      for (AllianceMember member : alliance.members()) {
         if (member == null || member.uuid() == null) {
            continue;
         }
         if (normalizeUuid(member.uuid()).equals(playerUuid)) {
            return member;
         }
      }

      return null;
   }

   private static String getCurrentPlayerUuid() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null || minecraft.getUser() == null || minecraft.getUser().getProfileId() == null) {
         return "";
      }
      return normalizeUuid(minecraft.getUser().getProfileId().toString());
   }

   private static String normalizeUuid(String uuid) {
      if (uuid == null || uuid.isBlank()) {
         return "";
      }
      return uuid.replace("-", "").toLowerCase(Locale.ROOT);
   }
}
