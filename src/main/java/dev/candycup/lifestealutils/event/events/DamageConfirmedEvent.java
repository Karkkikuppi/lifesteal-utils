package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;

/**
 * fired when the server confirms damage to an entity.
 * this event occurs after a ClientAttackEvent when the server validates the hit.
 */
public class DamageConfirmedEvent extends LSUEvent {
    private final int entityId;
    private final ClientboundDamageEventPacket packet;

    /**
     * Creates a new DamageConfirmedEvent representing the server's confirmation of damage to an entity.
     *
     * @param entityId the ID of the entity that was confirmed as damaged
     * @param packet the clientbound packet containing the server's damage event details
     */
    public DamageConfirmedEvent(int entityId, ClientboundDamageEventPacket packet) {
        this.entityId = entityId;
        this.packet = packet;
    }

    /**
     * Gets the ID of the entity for which the server confirmed damage.
     *
     * @return the entity's ID
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * Retrieves the clientbound damage event packet associated with this event.
     *
     * @return the ClientboundDamageEventPacket provided by the server for the confirmed damage
     */
    public ClientboundDamageEventPacket getPacket() {
        return packet;
    }

    /**
     * Indicates whether this event type can be cancelled.
     *
     * @return `true` if the event can be cancelled, `false` otherwise.
     */
    @Override
    public boolean isCancellable() {
        return false;
    }
}