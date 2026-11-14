package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** A camera pan from (0, 0) to a target over a beat, in layer pixels relative to the layer pivot. */
public final class CameraPan {

    private final Beat beat;
    private final float targetX;
    private final float targetY;

    public CameraPan(Beat beat, float targetX, float targetY) {
        if (beat == null) {
            throw new IllegalArgumentException("beat must not be null");
        }
        this.beat = beat;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    /** Pixels from the pivot to the center of a cell, given its (possibly fractional) index along one axis. */
    public static float cellCenterFromPivot(float cellIndex, float cellSize, float pivot) {
        return (cellIndex + 0.5f) * cellSize - pivot;
    }

    public float cameraXAt(float timeSeconds) {
        return targetX * beat.progress(timeSeconds);
    }

    public float cameraYAt(float timeSeconds) {
        return targetY * beat.progress(timeSeconds);
    }

    /**
     * Returns the screen-space position, along one axis, that stays on the pan target's spot of a zoomed layer.
     * {@code from} is where the spot sits at zoom 1 before the pan, and {@code to} is where the pan centers it.
     * The spot's distance from {@code to} is (target - camera) * zoom, which equals
     * (from - to) * (1 - progress) * zoom.
     */
    public float followAt(float timeSeconds, float from, float to, float zoom) {
        return to + (from - to) * (1f - beat.progress(timeSeconds)) * zoom;
    }
}
