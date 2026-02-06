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

   public ClientAttackEvent(Entity target, long timestamp) {
      this.target = target;
      this.timestamp = timestamp;
   }

   public Entity getTarget() {
      return target;
   }

   public int getTargetId() {
      return target != null ? target.getId() : -1;
   }

   public long getTimestamp() {
      return timestamp;
   }

   @Override
   public boolean isCancellable() {
      return true;
   }
}
