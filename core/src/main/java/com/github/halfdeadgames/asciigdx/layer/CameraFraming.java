package com.github.halfdeadgames.asciigdx.layer;

import java.util.Objects;

/**
 * Computes camera positions that keep a target centered in the view without showing space
 * outside the map. Each call handles one axis, so a game with tall 1:2 cells passes a different
 * cell size per axis. The result is in virtual pixels, ready for {@link LayerStack#setCamera}
 * or {@link Panel#setCamera}; the y axis grows downward.
 *
 * <p>Fractional target positions give smooth slides. Calls are pure and allocate nothing, so
 * they are safe to make every frame.
 *
 * <p>Example, centering on a player cell. The {@code + 0.5f} aims at the middle of the cell
 * rather than its top-left corner:
 *
 * <pre>{@code
 * stack.setCamera(
 *         CameraFraming.centerOn((column + 0.5f) * cellWidth, viewWidth,
 *                 mapColumns * cellWidth, FrameAlignment.CENTER),
 *         CameraFraming.centerOn((row + 0.5f) * cellHeight, viewHeight,
 *                 mapRows * cellHeight, FrameAlignment.START));
 * }</pre>
 */
public final class CameraFraming {

    private CameraFraming() {
    }

    /**
     * Returns the camera position along one axis that centers the target in the view.
     *
     * <p>When the map is at least as large as the view, the camera is the target minus half the
     * view, clamped to 0 through {@code mapSize - viewSize}, and the alignment is ignored. When
     * the map is smaller, the target is ignored and the result is 0 for
     * {@link FrameAlignment#START}, {@code (mapSize - viewSize) / 2} for
     * {@link FrameAlignment#CENTER}, or {@code mapSize - viewSize} for
     * {@link FrameAlignment#END}; the last two are negative.
     *
     * @param targetCenter the point to center on, in virtual pixels from the map's origin (left
     *     or top edge); finite
     * @param viewSize the view's size along this axis in virtual pixels; finite, 0 or more
     * @param mapSize the map's size along this axis in virtual pixels; finite, 0 or more
     * @param smallMapAlignment where a map smaller than the view sits; not null
     * @return the camera position in virtual pixels
     * @throws NullPointerException if smallMapAlignment is null
     * @throws IllegalArgumentException if a value is NaN or infinite, or viewSize or mapSize is
     *     negative
     */
    public static float centerOn(
            float targetCenter,
            float viewSize,
            float mapSize,
            FrameAlignment smallMapAlignment) {
        requireValid(targetCenter, viewSize, mapSize, smallMapAlignment);
        if (mapSize < viewSize) {
            return alignSmallMap(viewSize, mapSize, smallMapAlignment);
        }
        float centered = targetCenter - viewSize / 2f;
        return Math.max(0f, Math.min(centered, mapSize - viewSize));
    }

    private static float alignSmallMap(
            float viewSize, float mapSize, FrameAlignment alignment) {
        float difference = mapSize - viewSize;
        return switch (alignment) {
            case START -> 0f;
            case CENTER -> difference / 2f;
            case END -> difference;
        };
    }

    private static void requireValid(
            float targetCenter,
            float viewSize,
            float mapSize,
            FrameAlignment alignment) {
        if (!Float.isFinite(targetCenter) || !Float.isFinite(viewSize)
                || !Float.isFinite(mapSize)) {
            throw new IllegalArgumentException(
                    "targetCenter, viewSize, and mapSize must be finite: "
                            + targetCenter + ", " + viewSize + ", " + mapSize);
        }
        if (viewSize < 0f || mapSize < 0f) {
            throw new IllegalArgumentException(
                    "viewSize and mapSize must not be negative: " + viewSize + ", " + mapSize);
        }
        Objects.requireNonNull(alignment, "smallMapAlignment");
    }
}
