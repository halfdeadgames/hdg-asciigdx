package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import org.junit.jupiter.api.Test;

class BeatTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void progressIsZeroBeforeStart() {
        assertEquals(0f, new Beat(2f, 4f).progress(1f), EPSILON);
    }

    @Test
    void progressIsLinearDuringBeat() {
        assertEquals(0.25f, new Beat(2f, 4f).progress(3f), EPSILON);
    }

    @Test
    void progressIsOneAtAndAfterEnd() {
        Beat beat = new Beat(2f, 4f);
        assertEquals(1f, beat.progress(6f), EPSILON);
        assertEquals(1f, beat.progress(100f), EPSILON);
    }

    @Test
    void progressAppliesEasing() {
        Beat beat = new Beat(0f, 2f, Interpolation.pow2In);
        assertEquals(0.25f, beat.progress(1f), EPSILON);
    }

    @Test
    void zeroDurationBeatJumpsAtStart() {
        Beat beat = new Beat(1f, 0f);
        assertEquals(0f, beat.progress(0.99f), EPSILON);
        assertEquals(1f, beat.progress(1f), EPSILON);
        assertEquals(1f, beat.progress(2f), EPSILON);
    }

    @Test
    void reportsStartAndEnd() {
        Beat beat = new Beat(1.5f, 2f);
        assertEquals(1.5f, beat.start(), EPSILON);
        assertEquals(3.5f, beat.end(), EPSILON);
    }

    @Test
    void isActiveIncludesStartAndExcludesEnd() {
        Beat beat = new Beat(1f, 2f);
        assertFalse(beat.isActive(0.99f));
        assertTrue(beat.isActive(1f));
        assertTrue(beat.isActive(2.99f));
        assertFalse(beat.isActive(3f));
    }

    @Test
    void zeroDurationBeatIsNeverActive() {
        assertFalse(new Beat(1f, 0f).isActive(1f));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Beat(0f, -1f));
        assertThrows(IllegalArgumentException.class, () -> new Beat(0f, 1f, null));
    }
}
