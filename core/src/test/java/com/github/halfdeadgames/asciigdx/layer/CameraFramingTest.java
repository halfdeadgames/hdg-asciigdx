package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CameraFramingTest {

    private static final float CELL = 16f;
    private static final float DELTA = 0.0001f;

    private static float frame(float cell, int windowCells, int mapCells) {
        return CameraFraming.centerOn(
                (cell + 0.5f) * CELL,
                windowCells * CELL,
                mapCells * CELL,
                FrameAlignment.CENTER);
    }

    @Test
    void targetNearStartClampsToZero() {
        assertEquals(0f, frame(0, 40, 80), DELTA);
        assertEquals(0f, frame(19, 40, 80), DELTA);
    }

    @Test
    void targetNearEndClampsToLastScrollPosition() {
        assertEquals(640f, frame(79, 40, 80), DELTA);
    }

    @Test
    void targetInMiddleCentersTheCell() {
        assertEquals(328f, frame(40, 40, 80), DELTA);
        assertEquals(184f, frame(22, 22, 44), DELTA);
    }

    @Test
    void fractionalCellsSlideSmoothly() {
        assertEquals(336f, frame(40.5f, 40, 80), DELTA);
        assertEquals(332f, frame(40.25f, 40, 80), DELTA);
        assertEquals(324f, frame(39.75f, 40, 80), DELTA);
    }

    @Test
    void fractionalCellsClampAtBothEnds() {
        assertEquals(0f, frame(0.25f, 40, 80), DELTA);
        assertEquals(0f, frame(19.5f, 40, 80), DELTA);
        assertEquals(640f, frame(78.75f, 40, 80), DELTA);
        assertEquals(640f, frame(79.5f, 40, 80), DELTA);
    }

    @Test
    void smallMapAtStartIsZero() {
        assertEquals(0f, CameraFraming.centerOn(50f, 100f, 40f, FrameAlignment.START), DELTA);
    }

    @Test
    void smallMapCenteredIsNegativeHalfDifference() {
        assertEquals(-30f, CameraFraming.centerOn(50f, 100f, 40f, FrameAlignment.CENTER), DELTA);
    }

    @Test
    void smallMapAtEndIsNegativeDifference() {
        assertEquals(-60f, CameraFraming.centerOn(50f, 100f, 40f, FrameAlignment.END), DELTA);
    }

    @Test
    void smallMapIgnoresTargetCenter() {
        for (FrameAlignment alignment : FrameAlignment.values()) {
            assertEquals(
                    CameraFraming.centerOn(0f, 100f, 40f, alignment),
                    CameraFraming.centerOn(9999f, 100f, 40f, alignment),
                    DELTA);
        }
    }

    @Test
    void mapEqualToViewIsZeroForEveryAlignment() {
        for (FrameAlignment alignment : FrameAlignment.values()) {
            assertEquals(0f, CameraFraming.centerOn(30f, 100f, 100f, alignment), DELTA);
        }
    }

    @Test
    void largeMapIgnoresAlignment() {
        float start = CameraFraming.centerOn(500f, 100f, 1000f, FrameAlignment.START);
        float end = CameraFraming.centerOn(500f, 100f, 1000f, FrameAlignment.END);
        assertEquals(450f, start, DELTA);
        assertEquals(450f, end, DELTA);
    }

    @Test
    void nanValueThrows() {
        FrameAlignment alignment = FrameAlignment.CENTER;
        assertThrows(IllegalArgumentException.class,
                () -> CameraFraming.centerOn(Float.NaN, 10f, 20f, alignment));
        assertThrows(IllegalArgumentException.class,
                () -> CameraFraming.centerOn(1f, Float.NaN, 20f, alignment));
        assertThrows(IllegalArgumentException.class,
                () -> CameraFraming.centerOn(1f, 10f, Float.NaN, alignment));
    }

    @Test
    void infiniteValueThrows() {
        FrameAlignment alignment = FrameAlignment.CENTER;
        assertThrows(IllegalArgumentException.class,
                () -> CameraFraming.centerOn(Float.POSITIVE_INFINITY, 10f, 20f, alignment));
        assertThrows(IllegalArgumentException.class,
                () -> CameraFraming.centerOn(1f, Float.POSITIVE_INFINITY, 20f, alignment));
        assertThrows(IllegalArgumentException.class,
                () -> CameraFraming.centerOn(1f, 10f, Float.NEGATIVE_INFINITY, alignment));
    }

    @Test
    void negativeSizeThrows() {
        FrameAlignment alignment = FrameAlignment.CENTER;
        assertThrows(IllegalArgumentException.class,
                () -> CameraFraming.centerOn(1f, -1f, 20f, alignment));
        assertThrows(IllegalArgumentException.class,
                () -> CameraFraming.centerOn(1f, 10f, -1f, alignment));
    }

    @Test
    void nullAlignmentThrows() {
        assertThrows(NullPointerException.class,
                () -> CameraFraming.centerOn(1f, 10f, 20f, null));
    }
}
