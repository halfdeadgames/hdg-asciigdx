package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.math.Interpolation;

/**
 * One glyph sliding into a map cell. Offsets are in cells, measured from the target cell, and
 * shrink to zero over the duration. Mutable so a pool can reuse instances.
 */
final class CellSlide {

    private int targetColumn;
    private int targetRow;
    private float startOffsetColumns;
    private float startOffsetRows;
    private float startSeconds;
    private float durationSeconds;
    private Interpolation easing = Interpolation.linear;

    void start(
            int targetColumn,
            int targetRow,
            float startOffsetColumns,
            float startOffsetRows,
            float startSeconds,
            float durationSeconds,
            Interpolation easing) {
        if (easing == null) {
            throw new IllegalArgumentException("easing must not be null");
        }
        requireFinite(durationSeconds, "durationSeconds");
        if (durationSeconds < 0f) {
            throw new IllegalArgumentException(
                    "durationSeconds must be 0 or more, was " + durationSeconds);
        }
        requireFinite(startSeconds, "startSeconds");
        requireFinite(startOffsetColumns, "startOffsetColumns");
        requireFinite(startOffsetRows, "startOffsetRows");
        this.targetColumn = targetColumn;
        this.targetRow = targetRow;
        this.startOffsetColumns = startOffsetColumns;
        this.startOffsetRows = startOffsetRows;
        this.startSeconds = startSeconds;
        this.durationSeconds = durationSeconds;
        this.easing = easing;
    }

    int getTargetColumn() {
        return targetColumn;
    }

    int getTargetRow() {
        return targetRow;
    }

    float getOffsetColumnsAt(float timeSeconds) {
        return offsetAt(startOffsetColumns, timeSeconds);
    }

    float getOffsetRowsAt(float timeSeconds) {
        return offsetAt(startOffsetRows, timeSeconds);
    }

    private float offsetAt(float startOffset, float timeSeconds) {
        if (isFinishedAt(timeSeconds)) {
            return 0f;
        }
        return startOffset * remainingFractionAt(timeSeconds);
    }

    boolean isFinishedAt(float timeSeconds) {
        return timeSeconds >= startSeconds + durationSeconds;
    }

    private float remainingFractionAt(float timeSeconds) {
        if (timeSeconds < startSeconds) {
            return 1f;
        }
        float progress = (timeSeconds - startSeconds) / durationSeconds;
        return 1f - easing.apply(Math.min(1f, Math.max(0f, progress)));
    }

    private static void requireFinite(float value, String name) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, was " + value);
        }
    }
}
