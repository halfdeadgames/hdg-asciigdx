package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.tween.FloatTrack;
import com.github.halfdeadgames.asciigdx.tween.Tween;
import org.junit.jupiter.api.Test;

class SplashColorTest {

    private static final float EPSILON = 1e-5f;
    private final FloatTrack flash = SplashColor.flashTrack(7.6f, 0.25f, Interpolation.pow2Out);
    private final FloatTrack saturation = SplashColor.saturationTrack(new UnderwaterDive(
            Tween.eased(7.6f, 0.4f, 0f, 1f, Interpolation.sineOut),
            Tween.eased(7.6f + 0.4f, 0.6f, 0f, 1f, Interpolation.sine)));

    @Test
    void flashIsZeroBeforeTheSplash() {
        assertEquals(0f, flash.getValueAt(0f), EPSILON);
        assertEquals(0f, flash.getValueAt(7.59f), EPSILON);
    }

    @Test
    void flashJumpsToOneAtTheSplash() {
        assertEquals(1f, flash.getValueAt(7.6f), EPSILON);
    }

    @Test
    void flashEasesDownToZero() {
        float mid = flash.getValueAt(7.725f);
        assertTrue(mid > 0f && mid < 0.5f, "mid " + mid);
        assertTrue(flash.getValueAt(7.7f) > flash.getValueAt(7.8f));
    }

    @Test
    void flashIsZeroAfterTheFlashAndAtLoopWrap() {
        assertEquals(0f, flash.getValueAt(7.85f), EPSILON);
        assertEquals(0f, flash.getValueAt(11.4f), EPSILON);
    }

    @Test
    void saturationIsFullBeforeAndZeroUntilTheSurfacing() {
        assertEquals(1f, saturation.getValueAt(0f), EPSILON);
        assertEquals(1f, saturation.getValueAt(7.59f), EPSILON);
        assertEquals(0f, saturation.getValueAt(7.6f), EPSILON);
        assertEquals(0f, saturation.getValueAt(7.99f), EPSILON);
        assertEquals(0f, saturation.getValueAt(8.0f), EPSILON);
    }

    @Test
    void saturationReturnsDuringTheSurfacingAndStaysFull() {
        float mid = saturation.getValueAt(8.3f);
        assertTrue(mid > 0f && mid < 1f, "mid " + mid);
        assertEquals(1f, saturation.getValueAt(8.6f), EPSILON);
        assertEquals(1f, saturation.getValueAt(11.4f), EPSILON);
    }
}
