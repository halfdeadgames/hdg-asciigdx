package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.tween.FloatTrack;
import com.github.halfdeadgames.asciigdx.tween.TweenSequence;

/**
 * Tracks for the white flash that fades out after the splash, and the saturation that drops to gray
 * and returns on surfacing.
 */
public final class SplashColor {

    private SplashColor() {
    }

    /** 0 before the splash, jumping to 1 at it, then easing back to 0 over the flash. */
    public static FloatTrack flashTrack(float splashSeconds, float flashSeconds,
            Interpolation easing) {
        return TweenSequence.startingAt(splashSeconds, 0f)
                .to(1f, 0f)
                .to(0f, flashSeconds, easing)
                .build();
    }

    /**
     * 1 before the splash, 0 from the splash through the sink, then eased back up to 1 over the
     * dive's surfacing.
     */
    public static FloatTrack saturationTrack(UnderwaterDive dive) {
        return new SaturationTrack(dive);
    }

    private static final class SaturationTrack implements FloatTrack {

        private final UnderwaterDive dive;

        SaturationTrack(UnderwaterDive dive) {
            this.dive = dive;
        }

        @Override
        public float getValueAt(float timeSeconds) {
            if (timeSeconds < dive.sinkStartSeconds()) {
                return 1f;
            }
            return dive.surfaceProgressAt(timeSeconds);
        }

        @Override
        public float getStartSeconds() {
            return dive.sinkStartSeconds();
        }

        @Override
        public float getEndSeconds() {
            return dive.surfaceEndSeconds();
        }
    }
}
