package ru.mkilord.node.common;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SuperTime {
    public static <T> T mark(Supplier<T> runnable, Consumer<Long> log) {
        var start = Instant.now();
        var out = runnable.get();
        var end = Instant.now();
        log.accept(Duration.between(start, end).toMillis());
        return out;
    }
}
