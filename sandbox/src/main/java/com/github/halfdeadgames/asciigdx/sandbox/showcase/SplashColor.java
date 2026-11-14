package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** The white flash that fades out after the splash, and the saturation that drops to gray and returns on surfacing. */
public final class SplashColor {

    private final float splashSeconds;
    private final Beat flashBeat;
    private final UnderwaterDive dive;

    /** @param flashBeat should start at the splash; its progress runs the flash from full to none */
    public SplashColor(float splashSeconds, Beat flashBeat, UnderwaterDive dive) {
        if (flashBeat == null || dive == null) {
            throw new IllegalArgumentException("flashBeat and dive must not be null");
        }
        this.splashSeconds = splashSeconds;
        this.flashBeat = flashBeat;
        this.dive = dive;
    }

    /** 1 at the beat start, easing to 0 at its end; 0 outside the beat. */
    public float flashAmountAt(float timeSeconds) {
        if (!flashBeat.isActive(timeSeconds)) {
            return 0f;
        }
        return 1f - flashBeat.progress(timeSeconds);
    }

    /** 1 before the splash, 0 from the splash until the surfacing starts, then back up to 1. */
    public float saturationAt(float timeSeconds) {
        if (timeSeconds < splashSeconds) {
            return 1f;
        }
        return dive.surfaceProgressAt(timeSeconds);
    }
}
