package dev.candycup.lifestealutils.features.alliances.models;

/**
 * represents the state of an alliance member's membership.
 */
public enum AllianceMembershipState {
   /**
    * the member has been invited but has not yet accepted.
    */
   INVITED,

   /**
    * the member has accepted and is actively in the alliance.
    */
   JOINED,

   /**
    * the member has declined an invite.
    */
   DECLINED
}
