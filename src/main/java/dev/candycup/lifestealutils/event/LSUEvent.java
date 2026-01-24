package dev.candycup.lifestealutils.event;

/**
 * base class for all Lifesteal Utils events.
 * events can be cancelled to prevent further processing.
 */
public abstract class LSUEvent {
    private boolean cancelled = false;

    /**
     * Checks whether the event has been cancelled.
     *
     * @return true if the event is cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Mark this event as cancelled or not.
     *
     * @param cancelled true to mark the event cancelled, false to clear cancellation
     * @throws UnsupportedOperationException if this event does not support cancellation
     */
    public void setCancelled(boolean cancelled) {
        if (!isCancellable()) {
            throw new UnsupportedOperationException("Cannot cancel a non-cancellable event");
        }
        this.cancelled = cancelled;
    }

    /**
     * Indicates whether this event supports cancellation.
     *
     * @return `true` if the event supports cancellation, `false` otherwise.
     */
    public boolean isCancellable() {
        return false;
    }
}