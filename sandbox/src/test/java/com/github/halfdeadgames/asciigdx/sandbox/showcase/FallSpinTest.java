package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import org.junit.jupiter.api.Test;

class FallSpinTest {

    private static final float EPSILON = 1e-4f;

    private final FallSpin spin = new FallSpin(new Beat(6f, 1.3f, Interpolation.sine), 720f, 0.5f);

    @Test
    void isUprightAndFullSizeBeforeStart() {
        assertEquals(0f, spin.rotationDegreesAt(5f), EPSILON);
        assertEquals(1f, spin.scaleAt(5f), EPSILON);
    }

    @Test
    void endsAfterTwoTurnsAtHalfSize() {
        assertEquals(720f, spin.rotationDegreesAt(7.3f), EPSILON);
        assertEquals(0.5f, spin.scaleAt(7.3f), EPSILON);
        assertEquals(720f, spin.rotationDegreesAt(9f), EPSILON);
    }

    @Test
    void isHalfwayAtTheMiddle() {
        assertEquals(360f, spin.rotationDegreesAt(6.65f), EPSILON);
        assertEquals(0.75f, spin.scaleAt(6.65f), EPSILON);
    }

    @Test
    void swapsToCrispWhenTheBeatEnds() {
        assertFalse(spin.isCrispAt(5f));
        assertFalse(spin.isCrispAt(7.29f));
        assertTrue(spin.isCrispAt(7.31f));
        assertTrue(spin.isCrispAt(11f));
    }

    @Test
    void rejectsNullBeat() {
        assertThrows(IllegalArgumentException.class, () -> new FallSpin(null, 0f, 1f));
    }
}
