package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.tween.Tween;

/**
 * A camera pan from (0, 0) to a target over a time span, in layer pixels relative to the layer
 * pivot.
 */
public final class CameraPan {

    private final Tween pan;
    private final float targetX;
    private final float targetY;

    public CameraPan(Tween pan, float targetX, float targetY) {
        if (pan == null) {
            throw new IllegalArgumentException("pan must not be null");
        }
        this.pan = pan;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    /**
     * Pixels from the pivot to the center of a cell, given its (possibly fractional) index along
     * one axis.
     */
    public static float cellCenterFromPivot(float cellIndex, float cellSize, float pivot) {
        return (cellIndex + 0.5f) * cellSize - pivot;
    }

    public float cameraXAt(float timeSeconds) {
        return targetX * pan.progressAt(timeSeconds);
    }

    public float cameraYAt(float timeSeconds) {
        return targetY * pan.progressAt(timeSeconds);
    }

    /**
     * Returns the screen-space position, along one axis, that stays on the pan target's spot of a
     * zoomed layer. {@code from} is where the spot sits at zoom 1 before the pan, and {@code to}
     * is where the pan centers it. The spot's distance from {@code to} is (target - camera) * zoom,
     * which equals (from - to) * (1 - progress) * zoom.
     */
    public float followAt(float timeSeconds, float from, float to, float zoom) {
        return to + (from - to) * (1f - pan.progressAt(timeSeconds)) * zoom;
    }
}
