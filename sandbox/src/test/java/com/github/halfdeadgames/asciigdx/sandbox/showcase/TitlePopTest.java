package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TitlePopTest {

    private static final float EPSILON = 1e-4f;
    private static final int FROM = 0xFF0000FF;
    private static final int TO = 0x00FF00FF;

    private final TitlePop pop = new TitlePop(2f, 0.1f, 0.4f, 3.5f, 5, FROM, TO);

    @Test
    void isHiddenBeforeStart() {
        assertEquals(0f, pop.scaleTrack().valueAt(0, 1.9f), EPSILON);
        assertEquals(0f, pop.alphaTrack().valueAt(0, 1.9f), EPSILON);
    }

    @Test
    void staggersLetters() {
        assertEquals(2f, pop.scaleTrack().startOf(0), EPSILON);
        assertEquals(2.1f, pop.scaleTrack().startOf(1), EPSILON);
        assertEquals(2.4f, pop.alphaTrack().startOf(4), EPSILON);
        assertEquals(0f, pop.alphaTrack().valueAt(1, 2.05f), EPSILON);
        assertTrue(pop.alphaTrack().valueAt(0, 2.05f) > 0f);
    }

    @Test
    void overshootsMidPop() {
        float peak = 0f;
        for (float time = 2f; time <= 2.4f; time += 0.01f) {
            peak = Math.max(peak, pop.scaleTrack().valueAt(0, time));
        }
        assertTrue(peak > 1.2f && peak < 1.4f, "peak was " + peak);
    }

    @Test
    void settlesAtExactlyOneAfterEnd() {
        assertEquals(1f, pop.scaleTrack().valueAt(0, 2.4f), 0f);
        assertEquals(1f, pop.alphaTrack().valueAt(0, 2.4f), 0f);
        assertEquals(1f, pop.scaleTrack().valueAt(4, 10f), 0f);
        assertEquals(1f, pop.alphaTrack().valueAt(4, 10f), 0f);
    }

    @Test
    void alphaRisesLinearly() {
        assertEquals(0.5f, pop.alphaTrack().valueAt(0, 2.2f), EPSILON);
    }

    @Test
    void endsColorsAtFromAndTo() {
        assertEquals(FROM, pop.colorAt(0));
        assertEquals(TO, pop.colorAt(4));
    }

    @Test
    void singleLetterUsesFromColor() {
        var single = new TitlePop(0f, 0.1f, 0.4f, 3.5f, 1, FROM, TO);
        assertEquals(FROM, single.colorAt(0));
    }

    @Test
    void lastLetterSettlesAfterTheStagger() {
        assertEquals(2.8f, pop.scaleTrack().endOf(4), EPSILON);
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new TitlePop(0f, 0.1f, 0.4f, 3.5f, 0, FROM, TO));
        assertThrows(IllegalArgumentException.class,
                () -> new TitlePop(0f, 0.1f, 0f, 3.5f, 3, FROM, TO));
        assertThrows(IllegalArgumentException.class,
                () -> new TitlePop(0f, -0.1f, 0.4f, 3.5f, 3, FROM, TO));
    }
}
