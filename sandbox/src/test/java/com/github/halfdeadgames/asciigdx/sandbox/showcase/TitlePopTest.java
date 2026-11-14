package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertFalse(pop.isShownAt(0, 1.9f));
        assertEquals(0f, pop.scaleAt(0, 1.9f), EPSILON);
        assertEquals(0f, pop.alphaAt(0, 1.9f), EPSILON);
    }

    @Test
    void staggersLetters() {
        assertTrue(pop.isShownAt(0, 2.05f));
        assertFalse(pop.isShownAt(1, 2.05f));
        assertTrue(pop.isShownAt(1, 2.1f));
    }

    @Test
    void overshootsMidBeat() {
        float peak = 0f;
        for (float time = 2f; time <= 2.4f; time += 0.01f) {
            peak = Math.max(peak, pop.scaleAt(0, time));
        }
        assertTrue(peak > 1.2f && peak < 1.4f, "peak was " + peak);
    }

    @Test
    void settlesAtExactlyOneAfterEnd() {
        assertEquals(1f, pop.scaleAt(0, 2.4f), 0f);
        assertEquals(1f, pop.alphaAt(0, 2.4f), 0f);
        assertEquals(1f, pop.scaleAt(4, 10f), 0f);
        assertEquals(1f, pop.alphaAt(4, 10f), 0f);
    }

    @Test
    void alphaRisesLinearly() {
        assertEquals(0.5f, pop.alphaAt(0, 2.2f), EPSILON);
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
        assertEquals(0.4f, single.endSeconds(), EPSILON);
    }

    @Test
    void endSecondsIsWhenLastLetterSettles() {
        assertEquals(2.8f, pop.endSeconds(), EPSILON);
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TitlePop(0f, 0.1f, 0.4f, 3.5f, 0, FROM, TO));
        assertThrows(IllegalArgumentException.class, () -> new TitlePop(0f, 0.1f, 0f, 3.5f, 3, FROM, TO));
        assertThrows(IllegalArgumentException.class, () -> new TitlePop(0f, -0.1f, 0.4f, 3.5f, 3, FROM, TO));
    }
}
