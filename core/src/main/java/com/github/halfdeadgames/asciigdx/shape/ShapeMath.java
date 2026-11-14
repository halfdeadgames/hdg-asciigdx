package com.github.halfdeadgames.asciigdx.shape;

/** Validation and distance helpers shared by the shape implementations. */
final class ShapeMath {

    private ShapeMath() {
    }

    static float requireValidAspect(float rowHeightPerColumnWidth) {
        if (!Float.isFinite(rowHeightPerColumnWidth) || rowHeightPerColumnWidth <= 0f) {
            throw new IllegalArgumentException(
                    "rowHeightPerColumnWidth must be finite and > 0 but was " + rowHeightPerColumnWidth);
        }
        return rowHeightPerColumnWidth;
    }

    static float requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite but was " + value);
        }
        return value;
    }

    static int requireSpan(int start, int length, String lengthName) {
        if (length < 1) {
            throw new IllegalArgumentException(lengthName + " must be >= 1 but was " + length);
        }
        // Loops run "i <= last", so last must stay below MAX_VALUE or i++ would wrap around.
        if ((long) start + length - 1 >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "span starting at " + start + " with " + lengthName + " " + length + " overflows");
        }
        return length;
    }

    static float distance(double columnDelta, double rowDeltaInColumnWidths) {
        return (float) Math.sqrt(columnDelta * columnDelta + rowDeltaInColumnWidths * rowDeltaInColumnWidths);
    }

    static float distanceFraction(float distance, float largestDistance) {
        if (largestDistance <= 0f) {
            return 0f;
        }
        return Math.min(1f, distance / largestDistance);
    }
}
