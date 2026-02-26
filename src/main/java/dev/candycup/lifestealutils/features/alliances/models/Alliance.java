package dev.candycup.lifestealutils.features.alliances.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * represents an alliance with its metadata and members.
 */
public record Alliance(String id, String name, String prefix, String color, String description, String motd,
                       String ownedBy, List<AllianceMember> members, Instant createdAt, Instant updatedAt,
                       AllianceType type) {
   public Alliance(
           String id,
           String name,
           String prefix,
           String color,
           String description,
           String motd,
           String ownedBy,
           List<AllianceMember> members,
           Instant createdAt,
           Instant updatedAt
   ) {
      this(id, name, prefix, color, description, motd, ownedBy, members, createdAt, updatedAt, AllianceType.MODERN);
   }

   public Alliance(
           String id,
           String name,
           String prefix,
           String color,
           String description,
           String motd,
           String ownedBy,
           List<AllianceMember> members,
           Instant createdAt,
           Instant updatedAt,
           AllianceType type
   ) {
      this.id = id;
      this.name = name;
      this.prefix = prefix;
      this.color = color;
      this.description = description;
      this.motd = motd;
      this.ownedBy = ownedBy;
      this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
      this.type = type == null ? AllianceType.MODERN : type;
   }

   public String getDisplayName() {
      if (prefix == null || prefix.isBlank()) {
         return name;
      }
      return prefix + " | " + name;
   }

   @Override
   public List<AllianceMember> members() {
      return new ArrayList<>(members);
   }

   public boolean isLocal() {
      return type == AllianceType.LOCAL;
   }

   public boolean isModern() {
      return type == AllianceType.MODERN;
   }

   /**
    * gets only the actively joined members of the alliance.
    *
    * @return a list of joined members
    */
   public List<AllianceMember> getJoinedMembers() {
      return members.stream()
              .filter(AllianceMember::isJoined)
              .collect(Collectors.toList());
   }

   /**
    * gets only the invited members who have not yet joined.
    *
    * @return a list of invited members
    */
   public List<AllianceMember> getInvitedMembers() {
      return members.stream()
              .filter(AllianceMember::isInvited)
              .collect(Collectors.toList());
   }

   /**
    * finds a member by their UUID.
    *
    * @param uuid the uuid to search for
    * @return the member if found, otherwise null
    */
   public AllianceMember findMemberByUuid(String uuid) {
      return members.stream()
              .filter(m -> m.uuid().equals(uuid))
              .findFirst()
              .orElse(null);
   }
}
