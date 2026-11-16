package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.tween.Tween;

/**
 * A rocking rotation whose amplitude rises from 0 at the sway start to its peak at mid-sway, then
 * falls back to 0.
 */
public final class WaterSway {

    private final Tween sway;
    private final float peakDegrees;
    private final float cyclesPerSecond;

    public WaterSway(Tween sway, float peakDegrees, float cyclesPerSecond) {
        if (sway == null) {
            throw new IllegalArgumentException("sway must not be null");
        }
        this.sway = sway;
        this.peakDegrees = peakDegrees;
        this.cyclesPerSecond = cyclesPerSecond;
    }

    /** Rotation in degrees, positive is clockwise on screen; exactly 0 outside the sway. */
    public float degreesAt(float timeSeconds) {
        if (!sway.isActiveAt(timeSeconds)) {
            return 0f;
        }
        double elapsed = timeSeconds - sway.start();
        double duration = sway.end() - sway.start();
        double envelope = Math.sin(Math.PI * elapsed / duration);
        double rocking = Math.sin(2.0 * Math.PI * cyclesPerSecond * elapsed);
        return (float) (peakDegrees * envelope * rocking);
    }
}
