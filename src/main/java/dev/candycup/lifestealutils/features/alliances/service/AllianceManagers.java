package dev.candycup.lifestealutils.features.alliances.service;

import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceType;
import dev.candycup.lifestealutils.gaia.AlliancesAPIClient;

import java.util.ArrayList;
import java.util.List;
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

   private static AllianceManager forAlliance(Alliance alliance) {
      return forType(alliance.type());
   }

   private static AllianceManager forType(AllianceType type) {
      if (type == AllianceType.LOCAL) {
         return LOCAL;
      }
      return MODERN;
   }
}
