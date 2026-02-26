package dev.candycup.lifestealutils.features.timers;

public record BasicTimerDefinition(
        String name,
        String chatTrigger,
        String toggleOption,
        String defaultFormat,
        String passiveState,
        int durationSeconds,
        String nbtId
) {
}
