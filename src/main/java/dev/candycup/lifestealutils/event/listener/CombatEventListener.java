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
 * Invoked when the local player initiates an attack against an entity.
 *
 * @param event details of the client-side attack event
 */
    default void onClientAttack(ClientAttackEvent event) {}

    /**
 * Invoked when the server confirms that an entity has taken damage.
 *
 * @param event the damage confirmation event containing details about the confirmed damage
 */
    default void onDamageConfirmed(DamageConfirmedEvent event) {}

    /**
 * Invoked when the local player receives damage.
 *
 * @param event the event describing the damage applied to the local player
 */
    default void onPlayerDamaged(PlayerDamagedEvent event) {}
}