package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import org.junit.jupiter.api.Test;

class SplashColorTest {

    private static final float EPSILON = 1e-5f;
    private final UnderwaterDive dive = new UnderwaterDive(
        new Beat(7.6f, 0.4f, Interpolation.sineOut), new Beat(8.0f, 0.6f, Interpolation.sine));
    private final SplashColor color = new SplashColor(7.6f, new Beat(7.6f, 0.25f, Interpolation.pow2Out), dive);

    @Test
    void flashIsZeroBeforeTheSplash() {
        assertEquals(0f, color.flashAmountAt(0f), EPSILON);
        assertEquals(0f, color.flashAmountAt(7.59f), EPSILON);
    }

    @Test
    void flashJumpsToOneAtTheSplash() {
        assertEquals(1f, color.flashAmountAt(7.6f), EPSILON);
    }

    @Test
    void flashEasesDownToZero() {
        float mid = color.flashAmountAt(7.725f);
        assertTrue(mid > 0f && mid < 0.5f, "mid " + mid);
        assertTrue(color.flashAmountAt(7.7f) > color.flashAmountAt(7.8f));
    }

    @Test
    void flashIsZeroAfterTheBeatAndAtLoopWrap() {
        assertEquals(0f, color.flashAmountAt(7.85f), EPSILON);
        assertEquals(0f, color.flashAmountAt(11.4f), EPSILON);
    }

    @Test
    void saturationIsFullBeforeAndZeroUntilTheSurfacing() {
        assertEquals(1f, color.saturationAt(0f), EPSILON);
        assertEquals(1f, color.saturationAt(7.59f), EPSILON);
        assertEquals(0f, color.saturationAt(7.6f), EPSILON);
        assertEquals(0f, color.saturationAt(7.99f), EPSILON);
        assertEquals(0f, color.saturationAt(8.0f), EPSILON);
    }

    @Test
    void saturationReturnsDuringTheSurfacingAndStaysFull() {
        float mid = color.saturationAt(8.3f);
        assertTrue(mid > 0f && mid < 1f, "mid " + mid);
        assertEquals(1f, color.saturationAt(8.6f), EPSILON);
        assertEquals(1f, color.saturationAt(11.4f), EPSILON);
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new SplashColor(7.6f, null, dive));
        assertThrows(IllegalArgumentException.class, () -> new SplashColor(7.6f, new Beat(7.6f, 0.25f), null));
    }
}
