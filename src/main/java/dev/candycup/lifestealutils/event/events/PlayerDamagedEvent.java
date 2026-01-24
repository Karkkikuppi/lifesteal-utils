package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;

/**
 * fired when the local player receives damage.
 * this typically resets combat tracking for the player.
 */
public class PlayerDamagedEvent extends LSUEvent {
    private final int entityId;
    private final ClientboundDamageEventPacket packet;

    /**
     * Creates an event representing the local player receiving damage.
     *
     * @param entityId the identifier of the damaged entity
     * @param packet   the ClientboundDamageEventPacket carrying the damage details
     */
    public PlayerDamagedEvent(int entityId, ClientboundDamageEventPacket packet) {
        this.entityId = entityId;
        this.packet = packet;
    }

    /**
     * Gets the entity ID associated with this damage event.
     *
     * @return the numeric identifier of the damaged entity
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * Retrieves the damage event packet associated with this event.
     *
     * @return the ClientboundDamageEventPacket containing the damage information
     */
    public ClientboundDamageEventPacket getPacket() {
        return packet;
    }

    /**
     * Indicates whether this event supports cancellation.
     *
     * @return `true` if the event supports cancellation, `false` otherwise (this event does not support cancellation).
     */
    @Override
    public boolean isCancellable() {
        return false;
    }
}