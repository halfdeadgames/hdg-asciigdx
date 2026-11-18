package com.github.halfdeadgames.asciigdx.sandbox;

/**
 * Measures how long the GPU itself spends on the commands issued between {@link #begin()} and
 * {@link #end()}, without the time spent waiting on anything else. Call all methods on the GL
 * thread.
 */
public interface GpuTimer {
    /** A timer for platforms without GPU timer queries; it measures nothing. */
    GpuTimer UNAVAILABLE = new GpuTimer() {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void begin() {
        }

        @Override
        public void end() {
        }

        @Override
        public long readElapsedNanos() {
            return 0L;
        }
    };

    boolean isAvailable();

    void begin();

    void end();

    /** Blocks until the GPU has finished the measured commands, then returns their GPU time. */
    long readElapsedNanos();
}
