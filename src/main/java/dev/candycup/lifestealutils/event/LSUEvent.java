package dev.candycup.lifestealutils.event;

/**
 * base class for all Lifesteal Utils events.
 * events can be cancelled to prevent further processing.
 */
public abstract class LSUEvent {
   private boolean cancelled = false;

   /**
    * @return true if this event has been cancelled
    */
   public boolean isCancelled() {
      return cancelled;
   }

   /**
    * set whether this event should be cancelled.
    * cancelling an event prevents it from being processed further.
    *
    * @param cancelled true to cancel the event
    */
   public void setCancelled(boolean cancelled) {
      if (!isCancellable()) {
         throw new UnsupportedOperationException("Cannot cancel a non-cancellable event");
      }
      this.cancelled = cancelled;
   }

   /**
    * @return true if this event can be cancelled
    */
   public boolean isCancellable() {
      return false;
   }
}
