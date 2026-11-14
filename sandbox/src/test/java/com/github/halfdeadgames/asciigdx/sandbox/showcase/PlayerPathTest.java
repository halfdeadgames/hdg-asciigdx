package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerPathTest {

    private static final float EPSILON = 1e-5f;

    private final PlayerPath path = new PlayerPath(15, 7, 3);

    @Test
    void staysOnStartColumnBeforeFirstStep() {
        assertEquals(15f, path.columnAt(0f), EPSILON);
        assertEquals(15f, path.columnAt(4.79f), EPSILON);
    }

    @Test
    void restsOnWholeCellsBetweenSlides() {
        assertEquals(16f, path.columnAt(5.0f), EPSILON);
        assertEquals(17f, path.columnAt(5.3f), EPSILON);
    }

    @Test
    void endsOnLastColumnAfterFinalSlide() {
        assertEquals(18f, path.columnAt(5.5f), EPSILON);
        assertEquals(18f, path.columnAt(5.6f), EPSILON);
    }

    @Test
    void staysOnPlankRowAtNormalScaleBeforeHop() {
        assertEquals(7f, path.rowAt(0f), EPSILON);
        assertEquals(7f, path.rowAt(5.59f), EPSILON);
        assertEquals(1f, path.scaleAt(0f), EPSILON);
        assertEquals(1f, path.scaleAt(5.59f), EPSILON);
    }

    @Test
    void scalePeaksMidHop() {
        assertEquals(1.4f, path.scaleAt(5.8f), EPSILON);
    }

    @Test
    void landsPastTheLowerRopeAtNormalScale() {
        assertEquals(19f, path.columnAt(6.0f), EPSILON);
        assertEquals(9f, path.rowAt(6.0f), EPSILON);
        assertEquals(1f, path.scaleAt(6.0f), EPSILON);
        assertEquals(19f, path.columnAt(11f), EPSILON);
        assertEquals(9f, path.rowAt(11f), EPSILON);
        assertEquals(1f, path.scaleAt(11f), EPSILON);
    }

    @Test
    void isBetweenTwoRowsMidHop() {
        float row = path.rowAt(5.75f);
        assertTrue(row > 7f && row < 9f, "row " + row);
    }

    @Test
    void isBetweenTwoCellsMidSlide() {
        float column = path.columnAt(4.85f);
        assertTrue(column > 15f && column < 16f, "column " + column);
    }

    @Test
    void neverMovesBackward() {
        float previous = path.columnAt(0f);
        for (float time = 0f; time < 11.5f; time += 0.005f) {
            float column = path.columnAt(time);
            assertTrue(column >= previous - EPSILON, "column " + column + " at " + time);
            previous = column;
        }
    }

    @Test
    void zeroStepsNeverMove() {
        assertEquals(15f, new PlayerPath(15, 7, 0).columnAt(5.5f), EPSILON);
    }

    @Test
    void rejectsNegativeStepCount() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerPath(15, 7, -1));
    }
}
