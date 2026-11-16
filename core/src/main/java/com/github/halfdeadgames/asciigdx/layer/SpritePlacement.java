package com.github.halfdeadgames.asciigdx.layer;

/**
 * Math for moving a sprite's pivot without moving its drawn quad. The drawn corner is
 * {@code position + pivot + M * (corner - pivot)}, where {@code M} is the scale followed by the
 * clockwise rotation, applied in the same order and with the same trig as {@link SpriteQuadWriter}.
 * Moving the pivot by {@code shift} pixels keeps every corner in place when the position changes by
 * {@code -(I - M) * shift}. All values are in y-down layer pixels. Each axis is returned on its
 * own so callers need no temporary objects.
 */
final class SpritePlacement {

    private SpritePlacement() {
    }

    /**
     * Returns the position's new x after the pivot moved by the given pixels.
     *
     * @param position current x of the cell box's top-left corner (or of the cell offset)
     * @param pivotShiftX pixels the pivot moves right
     * @param pivotShiftY pixels the pivot moves down
     * @param rotationDegrees clockwise rotation in degrees
     */
    static float xKeepingPlacement(float position, float pivotShiftX, float pivotShiftY,
            float scaleX, float scaleY, float rotationDegrees) {
        float yUpDegrees = -rotationDegrees;
        float sin = SpriteQuadWriter.exactSin(yUpDegrees);
        float cos = SpriteQuadWriter.exactCos(yUpDegrees);
        float transformedShiftX = cos * scaleX * pivotShiftX + sin * scaleY * pivotShiftY;
        return position - (pivotShiftX - transformedShiftX);
    }

    /** Returns the position's new y; see {@link #xKeepingPlacement}. */
    static float yKeepingPlacement(float position, float pivotShiftX, float pivotShiftY,
            float scaleX, float scaleY, float rotationDegrees) {
        float yUpDegrees = -rotationDegrees;
        float sin = SpriteQuadWriter.exactSin(yUpDegrees);
        float cos = SpriteQuadWriter.exactCos(yUpDegrees);
        float transformedShiftY = -sin * scaleX * pivotShiftX + cos * scaleY * pivotShiftY;
        return position - (pivotShiftY - transformedShiftY);
    }
}
