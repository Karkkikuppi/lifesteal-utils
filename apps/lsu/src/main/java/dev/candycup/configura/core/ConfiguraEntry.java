package dev.candycup.configura.core;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record ConfiguraEntry<T>(
        String key,
        Type type,
        Class<T> typeClass,
        Supplier<T> getter,
        Consumer<T> setter,
        Supplier<T> defaultSupplier,
        boolean required,
        boolean nullable,
        Optional<String> comment
) {
}
