package dev.candycup.lifestealutils.event;

/**
 * defines execution order for event listeners.
 * listeners with higher priority execute first.
 */
public enum EventPriority {
   LOW(0),
   NORMAL(100),
   HIGH(200);

   private final int value;

   EventPriority(int value) {
      this.value = value;
   }

   public int getValue() {
      return value;
   }
}
