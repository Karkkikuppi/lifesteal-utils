package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.world.entity.Entity;

/**
 * fired when the local player initiates an attack on an entity.
 * posted before server confirmation.
 */
public class ClientAttackEvent extends LSUEvent {
    private final Entity target;
    private final long timestamp;

    /**
     * Constructs a ClientAttackEvent for an attack directed at the given entity occurring at the specified timestamp.
     *
     * @param target the entity being attacked, may be null if not set
     * @param timestamp the time at which the event occurred
     */
    public ClientAttackEvent(Entity target, long timestamp) {
        this.target = target;
        this.timestamp = timestamp;
    }

    /**
     * The entity that the local player attempted to attack.
     *
     * @return the target Entity, or {@code null} if no target is set
     */
    public Entity getTarget() {
        return target;
    }

    /**
     * Retrieves the entity ID of the current attack target.
     *
     * @return the target entity's ID, or -1 if no target is set.
     */
    public int getTargetId() {
        return target != null ? target.getId() : -1;
    }

    /**
     * Gets the event timestamp.
     *
     * @return the stored timestamp representing when the event occurred
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Indicates that this event supports cancellation.
     *
     * @return `true` if the event can be cancelled, `false` otherwise.
     */
    @Override
    public boolean isCancellable() {
        return true;
    }
}