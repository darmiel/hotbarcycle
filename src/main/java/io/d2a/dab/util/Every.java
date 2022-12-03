package io.d2a.dab.util;

public class Every {

    private final int count;

    private final Runnable runnable;

    private int current;

    public Every(final int count, final Runnable runnable) {
        this.count = count;
        this.runnable = runnable;
    }

    public void tick() {
        if (++this.current >= this.count) {
            this.run();
            this.current = 0;
        }
    }

    private void run() {
        this.runnable.run();
    }

}