package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WaterPatternTest {

    private static final int WIDTH = 30;
    private static final int HEIGHT = 20;

    @Test
    void returnsOnlyWaterGlyphsAndSpace() {
        for (int step = 0; step < 10; step++) {
            for (int row = 0; row < HEIGHT; row++) {
                for (int column = 0; column < WIDTH; column++) {
                    int codePoint =
                            WaterPattern.codePointAt(column, row, step * WaterPattern.STEP_SECONDS);
                    assertTrue(codePoint == '~' || codePoint == '≈' || codePoint == ' ');
                }
            }
        }
    }

    @Test
    void sameInputGivesSameOutput() {
        assertEquals(WaterPattern.codePointAt(7, 3, 1.3f), WaterPattern.codePointAt(7, 3, 1.3f));
    }

    @Test
    void usesAllThreeGlyphsInOneStep() {
        boolean crest = false;
        boolean ripple = false;
        boolean calm = false;
        for (int column = 0; column < WIDTH; column++) {
            int codePoint = WaterPattern.codePointAt(column, 0, 0f);
            crest |= codePoint == '≈';
            ripple |= codePoint == '~';
            calm |= codePoint == ' ';
        }
        assertTrue(crest && ripple && calm);
    }

    @Test
    void patternShiftsBetweenSteps() {
        assertNotEquals(
                WaterPattern.codePointAt(0, 0, 0f),
                WaterPattern.codePointAt(0, 0, WaterPattern.STEP_SECONDS));
    }

    @Test
    void bandsRunDiagonally() {
        assertEquals(WaterPattern.codePointAt(2, 3, 0f), WaterPattern.codePointAt(3, 2, 0f));
        assertEquals(WaterPattern.codePointAt(2, 3, 0f), WaterPattern.codePointAt(1, 4, 0f));
    }

    @Test
    void stepChangesOnlyAtStepBoundaries() {
        assertEquals(0, WaterPattern.stepAt(0f));
        assertEquals(0, WaterPattern.stepAt(WaterPattern.STEP_SECONDS * 0.99f));
        assertEquals(1, WaterPattern.stepAt(WaterPattern.STEP_SECONDS));
        assertEquals(2, WaterPattern.stepAt(WaterPattern.STEP_SECONDS * 2f));
    }
}
