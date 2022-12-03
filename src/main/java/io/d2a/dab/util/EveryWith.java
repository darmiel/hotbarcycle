package io.d2a.dab.util;

import org.jetbrains.annotations.Nullable;

public class EveryWith<T> {

    private final int count;

    private final RunnableWith<T> runnable;

    private int current;

    public EveryWith(final int count, final RunnableWith<T> runnable) {
        this.count = count;
        this.runnable = runnable;
    }

    public void tick(final T t) {
        if (++this.current >= this.count) {
            this.run(t);
            this.current = 0;
        }
    }

    private void run(@Nullable final T t) {
        // disallow null
        if (t == null) {
            return;
        }
        this.runnable.run(t);
    }

}