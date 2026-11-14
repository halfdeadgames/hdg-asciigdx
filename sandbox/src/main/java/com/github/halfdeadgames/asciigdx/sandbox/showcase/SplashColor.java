package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.tween.FloatTrack;
import com.github.halfdeadgames.asciigdx.tween.TweenSequence;

/** Tracks for the white flash that fades out after the splash, and the saturation that drops to gray and returns on surfacing. */
public final class SplashColor {

    private SplashColor() {
    }

    /** 0 before the splash, jumping to 1 at it, then easing back to 0 over the flash. */
    public static FloatTrack flashTrack(float splashSeconds, float flashSeconds, Interpolation easing) {
        return TweenSequence.startingAt(splashSeconds, 0f)
            .to(1f, 0f)
            .to(0f, flashSeconds, easing)
            .build();
    }

    /** 1 before the splash, 0 from the splash through the sink, then eased back up to 1 over the surfacing. */
    public static FloatTrack saturationTrack(
            float splashSeconds, float sinkSeconds, float surfaceSeconds, Interpolation easing) {
        return TweenSequence.startingAt(splashSeconds, 1f)
            .to(0f, 0f)
            .hold(sinkSeconds)
            .to(1f, surfaceSeconds, easing)
            .build();
    }
}
