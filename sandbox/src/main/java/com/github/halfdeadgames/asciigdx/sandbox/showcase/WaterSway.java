package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** A rocking rotation whose amplitude rises from 0 at the beat start to its peak at mid-beat, then falls back to 0. */
public final class WaterSway {

    private final Beat beat;
    private final float peakDegrees;
    private final float cyclesPerSecond;

    public WaterSway(Beat beat, float peakDegrees, float cyclesPerSecond) {
        if (beat == null) {
            throw new IllegalArgumentException("beat must not be null");
        }
        this.beat = beat;
        this.peakDegrees = peakDegrees;
        this.cyclesPerSecond = cyclesPerSecond;
    }

    /** Counterclockwise degrees; exactly 0 outside the beat. */
    public float degreesAt(float timeSeconds) {
        if (!beat.isActive(timeSeconds)) {
            return 0f;
        }
        double elapsed = timeSeconds - beat.start();
        double duration = beat.end() - beat.start();
        double envelope = Math.sin(Math.PI * elapsed / duration);
        double rocking = Math.sin(2.0 * Math.PI * cyclesPerSecond * elapsed);
        return (float) (peakDegrees * envelope * rocking);
    }
}
