package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** The white flash that fades out after the splash, and the saturation that drops to gray at the splash. */
public final class SplashColor {

    private final float splashSeconds;
    private final Beat flashBeat;

    /** @param flashBeat should start at the splash; its progress runs the flash from full to none */
    public SplashColor(float splashSeconds, Beat flashBeat) {
        if (flashBeat == null) {
            throw new IllegalArgumentException("flashBeat must not be null");
        }
        this.splashSeconds = splashSeconds;
        this.flashBeat = flashBeat;
    }

    /** 1 at the beat start, easing to 0 at its end; 0 outside the beat. */
    public float flashAmountAt(float timeSeconds) {
        if (!flashBeat.isActive(timeSeconds)) {
            return 0f;
        }
        return 1f - flashBeat.progress(timeSeconds);
    }

    /** 1 before the splash, 0 from the splash on. */
    public float saturationAt(float timeSeconds) {
        return timeSeconds < splashSeconds ? 1f : 0f;
    }
}
