package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.ClientAttackEvent;
import dev.candycup.lifestealutils.event.events.DamageConfirmedEvent;
import dev.candycup.lifestealutils.event.events.PlayerDamagedEvent;

/**
 * listener interface for combat-related events.
 * override methods to handle specific events.
 */
public interface CombatEventListener extends LifestealEventListener {

   /**
    * called when the local player initiates an attack on an entity.
    *
    * @param event the attack event
    */
   default void onClientAttack(ClientAttackEvent event) {
   }

   /**
    * called when the server confirms damage to an entity.
    *
    * @param event the damage confirmation event
    */
   default void onDamageConfirmed(DamageConfirmedEvent event) {
   }

   /**
    * called when the local player receives damage.
    *
    * @param event the player damaged event
    */
   default void onPlayerDamaged(PlayerDamagedEvent event) {
   }
}
