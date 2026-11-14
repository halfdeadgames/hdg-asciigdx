package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LightningFlashTest {

    private static final float EPSILON = 1e-5f;
    private static final float STRENGTH = 0.85f;
    private final LightningFlash flash = new LightningFlash(
        STRENGTH,
        new LightningFlash.Flicker(3.40f, 3.48f),
        new LightningFlash.Flicker(3.56f, 3.66f));

    @Test
    void isZeroBeforeFirstFlicker() {
        assertEquals(0f, flash.amountAt(0f), EPSILON);
        assertEquals(0f, flash.amountAt(3.39f), EPSILON);
    }

    @Test
    void isFullStrengthInsideEachFlicker() {
        assertEquals(STRENGTH, flash.amountAt(3.44f), EPSILON);
        assertEquals(STRENGTH, flash.amountAt(3.60f), EPSILON);
    }

    @Test
    void isZeroInTheGap() {
        assertEquals(0f, flash.amountAt(3.50f), EPSILON);
    }

    @Test
    void isZeroAfterLastFlicker() {
        assertEquals(0f, flash.amountAt(3.70f), EPSILON);
        assertEquals(0f, flash.amountAt(9f), EPSILON);
    }

    @Test
    void flickerStartIsInclusive() {
        assertEquals(STRENGTH, flash.amountAt(3.40f), EPSILON);
        assertEquals(STRENGTH, flash.amountAt(3.56f), EPSILON);
    }

    @Test
    void flickerEndIsExclusive() {
        assertEquals(0f, flash.amountAt(3.48f), EPSILON);
        assertEquals(0f, flash.amountAt(3.66f), EPSILON);
    }
}
