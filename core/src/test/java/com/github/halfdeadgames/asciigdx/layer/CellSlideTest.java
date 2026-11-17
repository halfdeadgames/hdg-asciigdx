package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import org.junit.jupiter.api.Test;

class CellSlideTest {

    private static final float DELTA = 0.0001f;

    private static CellSlide linearSlide(float columns, float rows, float start, float duration) {
        CellSlide slide = new CellSlide();
        slide.start(4, 5, columns, rows, start, duration, Interpolation.linear);
        return slide;
    }

    @Test
    void keepsTargetCell() {
        CellSlide slide = linearSlide(-1f, 0f, 0f, 1f);
        assertEquals(4, slide.getTargetColumn());
        assertEquals(5, slide.getTargetRow());
    }

    @Test
    void beforeStartReturnsFullOffset() {
        CellSlide slide = linearSlide(-1f, 2f, 10f, 1f);
        assertEquals(-1f, slide.getOffsetColumnsAt(9f), DELTA);
        assertEquals(2f, slide.getOffsetRowsAt(9f), DELTA);
    }

    @Test
    void atStartReturnsFullOffset() {
        CellSlide slide = linearSlide(-1f, 2f, 10f, 1f);
        assertEquals(-1f, slide.getOffsetColumnsAt(10f), DELTA);
        assertEquals(2f, slide.getOffsetRowsAt(10f), DELTA);
    }

    @Test
    void middleWithLinearEasingReturnsHalf() {
        CellSlide slide = linearSlide(-1f, 2f, 10f, 2f);
        assertEquals(-0.5f, slide.getOffsetColumnsAt(11f), DELTA);
        assertEquals(1f, slide.getOffsetRowsAt(11f), DELTA);
    }

    @Test
    void middleWithNonLinearEasingFollowsFormula() {
        CellSlide slide = new CellSlide();
        slide.start(0, 0, -3f, 0f, 0f, 4f, Interpolation.pow2Out);
        float expected = -3f * (1f - Interpolation.pow2Out.apply(0.25f));
        assertEquals(expected, slide.getOffsetColumnsAt(1f), DELTA);
    }

    @Test
    void atEndReturnsExactlyZero() {
        CellSlide slide = linearSlide(-1f, 2f, 10f, 1f);
        assertEquals(0f, slide.getOffsetColumnsAt(11f));
        assertEquals(0f, slide.getOffsetRowsAt(11f));
    }

    @Test
    void afterEndReturnsExactlyZero() {
        CellSlide slide = linearSlide(-1f, 2f, 10f, 1f);
        assertEquals(0f, slide.getOffsetColumnsAt(50f));
        assertEquals(0f, slide.getOffsetRowsAt(50f));
    }

    @Test
    void zeroDurationIsFullBeforeStartAndZeroFromStart() {
        CellSlide slide = linearSlide(-1f, 1f, 5f, 0f);
        assertEquals(-1f, slide.getOffsetColumnsAt(4.9f), DELTA);
        assertEquals(0f, slide.getOffsetColumnsAt(5f));
        assertEquals(0f, slide.getOffsetRowsAt(6f));
    }

    @Test
    void diagonalOffsetsMoveOnBothAxes() {
        CellSlide slide = linearSlide(-1f, 1f, 0f, 4f);
        assertEquals(-0.75f, slide.getOffsetColumnsAt(1f), DELTA);
        assertEquals(0.75f, slide.getOffsetRowsAt(1f), DELTA);
    }

    @Test
    void multiCellOffsetsScaleWithDistance() {
        CellSlide slide = linearSlide(6f, -4f, 0f, 2f);
        assertEquals(3f, slide.getOffsetColumnsAt(1f), DELTA);
        assertEquals(-2f, slide.getOffsetRowsAt(1f), DELTA);
    }

    @Test
    void finishedBoundaries() {
        CellSlide slide = linearSlide(-1f, 0f, 10f, 2f);
        assertFalse(slide.isFinishedAt(9f));
        assertFalse(slide.isFinishedAt(10f));
        assertFalse(slide.isFinishedAt(11.9f));
        assertTrue(slide.isFinishedAt(12f));
        assertTrue(slide.isFinishedAt(13f));
    }

    @Test
    void zeroDurationIsFinishedAtStart() {
        CellSlide slide = linearSlide(-1f, 0f, 10f, 0f);
        assertFalse(slide.isFinishedAt(9.9f));
        assertTrue(slide.isFinishedAt(10f));
    }

    @Test
    void startingAgainReplacesAllValues() {
        CellSlide slide = linearSlide(-1f, 0f, 0f, 1f);
        slide.start(7, 8, 0f, 3f, 100f, 2f, Interpolation.linear);
        assertEquals(7, slide.getTargetColumn());
        assertEquals(8, slide.getTargetRow());
        assertEquals(0f, slide.getOffsetColumnsAt(50f), DELTA);
        assertEquals(3f, slide.getOffsetRowsAt(50f), DELTA);
        assertEquals(1.5f, slide.getOffsetRowsAt(101f), DELTA);
        assertFalse(slide.isFinishedAt(101f));
        assertTrue(slide.isFinishedAt(102f));
    }

    @Test
    void rejectsNullEasing() {
        assertThrows(IllegalArgumentException.class,
                () -> new CellSlide().start(0, 0, 1f, 1f, 0f, 1f, null));
    }

    @Test
    void rejectsNegativeOrNonFiniteDuration() {
        CellSlide slide = new CellSlide();
        assertThrows(IllegalArgumentException.class,
                () -> slide.start(0, 0, 1f, 1f, 0f, -0.1f, Interpolation.linear));
        assertThrows(IllegalArgumentException.class,
                () -> slide.start(0, 0, 1f, 1f, 0f, Float.NaN, Interpolation.linear));
        assertThrows(IllegalArgumentException.class,
                () -> slide.start(0, 0, 1f, 1f, 0f, Float.POSITIVE_INFINITY,
                        Interpolation.linear));
    }

    @Test
    void rejectsNonFiniteStartSeconds() {
        CellSlide slide = new CellSlide();
        assertThrows(IllegalArgumentException.class,
                () -> slide.start(0, 0, 1f, 1f, Float.NaN, 1f, Interpolation.linear));
        assertThrows(IllegalArgumentException.class,
                () -> slide.start(0, 0, 1f, 1f, Float.POSITIVE_INFINITY, 1f,
                        Interpolation.linear));
    }

    @Test
    void rejectsNonFiniteStartOffsets() {
        CellSlide slide = new CellSlide();
        assertThrows(IllegalArgumentException.class,
                () -> slide.start(0, 0, Float.NaN, 1f, 0f, 1f, Interpolation.linear));
        assertThrows(IllegalArgumentException.class,
                () -> slide.start(0, 0, 1f, Float.NEGATIVE_INFINITY, 0f, 1f,
                        Interpolation.linear));
    }
}
