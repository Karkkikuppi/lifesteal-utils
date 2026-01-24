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

    /**
     * Creates an enum constant with the specified numeric priority.
     *
     * @param value the numeric priority used for ordering listeners; higher values execute before lower ones
     */
    EventPriority(int value) {
        this.value = value;
    }

    /**
     * Numeric priority used to order event listeners; higher numbers execute first.
     *
     * @return the numeric priority value
     */
    public int getValue() {
        return value;
    }
}