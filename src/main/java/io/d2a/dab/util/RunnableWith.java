package io.d2a.dab.util;

import org.jetbrains.annotations.NotNull;

public interface RunnableWith<T> {

    void run(@NotNull T t);

    static RunnableWith<Void> wrap(final Runnable runnable) {
        return t -> runnable.run();
    }

}