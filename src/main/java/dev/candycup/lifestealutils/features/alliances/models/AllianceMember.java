package dev.candycup.lifestealutils.features.alliances.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * represents a member of an alliance.
 */
public record AllianceMember(String id, String uuid, String cachedName, AllianceMembershipState membershipState,
                             Instant addedAt, String addedBy, List<String> permissions, String allianceId) {

   public UUID getUuidObject() {
      try {
         return UUID.fromString(uuid);
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   /**
    * checks whether this member has admin permissions (denoted by the '*' permission).
    *
    * @return true if the member has full admin access
    */
   public boolean hasAdminPermissions() {
      return permissions.contains("*");
   }

   /**
    * checks whether this member is actively in the alliance.
    *
    * @return true if the member has joined
    */
   public boolean isJoined() {
      return membershipState == AllianceMembershipState.JOINED;
   }

   /**
    * checks whether this member has a pending invitation.
    *
    * @return true if the member is invited but has not joined
    */
   public boolean isInvited() {
      return membershipState == AllianceMembershipState.INVITED;
   }
}
