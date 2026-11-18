package com.github.halfdeadgames.asciigdx.sandbox;

/** A window's drawable size in pixels, without the title bar and borders. */
public record WindowSize(int width, int height) {
    /** The size the capture benchmarks are measured at. */
    public static final WindowSize FULL_HD = new WindowSize(1920, 1080);

    public WindowSize {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Window size must be positive, got " + width + "x" + height);
        }
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
