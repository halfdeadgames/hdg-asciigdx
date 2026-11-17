package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import org.junit.jupiter.api.Test;

class TweenTest {

    private static final float TOLERANCE = 1e-6f;

    private final Tween ascending = Tween.linear(2f, 4f, 10f, 30f);

    @Test
    void valueBeforeStartIsFrom() {
        assertEquals(10f, ascending.getValueAt(1f), TOLERANCE);
    }

    @Test
    void valueAtStartIsFrom() {
        assertEquals(10f, ascending.getValueAt(2f), TOLERANCE);
    }

    @Test
    void valueAtMidpointIsHalfwayWithLinearEasing() {
        assertEquals(20f, ascending.getValueAt(4f), TOLERANCE);
    }

    @Test
    void valueAtEndIsTo() {
        assertEquals(30f, ascending.getValueAt(6f), TOLERANCE);
    }

    @Test
    void valueAfterEndIsTo() {
        assertEquals(30f, ascending.getValueAt(100f), TOLERANCE);
    }

    @Test
    void valueDescendsWhenFromIsGreaterThanTo() {
        Tween descending = Tween.linear(0f, 2f, 8f, 0f);
        assertEquals(6f, descending.getValueAt(0.5f), TOLERANCE);
    }

    @Test
    void zeroDurationStaysAtFromBeforeStart() {
        Tween instant = Tween.linear(3f, 0f, 1f, 5f);
        assertEquals(1f, instant.getValueAt(2.9f), TOLERANCE);
    }

    @Test
    void zeroDurationJumpsToToAtStart() {
        Tween instant = Tween.linear(3f, 0f, 1f, 5f);
        assertEquals(5f, instant.getValueAt(3f), TOLERANCE);
    }

    @Test
    void customEasingShapesTheMidpoint() {
        Tween eased = Tween.eased(0f, 2f, 0f, 100f, Interpolation.pow2In);
        assertEquals(25f, eased.getValueAt(1f), TOLERANCE);
    }

    @Test
    void overshootEasingExceedsToInsideTheSpan() {
        Tween overshoot = Tween.eased(0f, 1f, 0f, 10f, Interpolation.swingOut);
        assertTrue(overshoot.getValueAt(0.7f) > 10f);
    }

    @Test
    void overshootEasingEndsExactlyAtTo() {
        Tween overshoot = Tween.eased(0f, 1f, 0f, 10f, Interpolation.swingOut);
        assertEquals(10f, overshoot.getValueAt(1f), 0f);
    }

    @Test
    void queryingOutOfOrderGivesTheSameAnswers() {
        float first = ascending.getValueAt(5f);
        ascending.getValueAt(0f);
        ascending.getValueAt(9f);
        assertEquals(first, ascending.getValueAt(5f), 0f);
    }

    @Test
    void progressIsZeroBeforeStart() {
        assertEquals(0f, ascending.getProgressAt(-5f), 0f);
    }

    @Test
    void progressIsOneAtAndAfterEnd() {
        assertEquals(1f, ascending.getProgressAt(6f), 0f);
        assertEquals(1f, ascending.getProgressAt(60f), 0f);
    }

    @Test
    void progressInsideSpanIsEased() {
        Tween eased = Tween.eased(0f, 2f, 0f, 100f, Interpolation.pow2In);
        assertEquals(0.25f, eased.getProgressAt(1f), TOLERANCE);
    }

    @Test
    void hasStartedAtIsFalseJustBeforeStartAndTrueAtStart() {
        assertFalse(ascending.hasStartedAt(1.999f));
        assertTrue(ascending.hasStartedAt(2f));
    }

    @Test
    void isActiveAtCoversStartInclusiveToEndExclusive() {
        assertFalse(ascending.isActiveAt(1.999f));
        assertTrue(ascending.isActiveAt(2f));
        assertTrue(ascending.isActiveAt(5.999f));
        assertFalse(ascending.isActiveAt(6f));
    }

    @Test
    void isFinishedAtIsTrueFromEndOn() {
        assertFalse(ascending.isFinishedAt(5.999f));
        assertTrue(ascending.isFinishedAt(6f));
    }

    @Test
    void zeroDurationIsNeverActive() {
        Tween instant = Tween.linear(3f, 0f, 1f, 5f);
        assertFalse(instant.isActiveAt(3f));
    }

    @Test
    void gettersReportConstructorValues() {
        assertEquals(2f, ascending.getStartSeconds(), 0f);
        assertEquals(6f, ascending.getEndSeconds(), 0f);
        assertEquals(4f, ascending.getDurationSeconds(), 0f);
        assertEquals(10f, ascending.getFromValue(), 0f);
        assertEquals(30f, ascending.getToValue(), 0f);
    }

    @Test
    void nanStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Tween.linear(Float.NaN, 1f, 0f, 1f));
    }

    @Test
    void infiniteStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Tween.linear(Float.POSITIVE_INFINITY, 1f, 0f, 1f));
    }

    @Test
    void negativeDurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Tween.linear(0f, -1f, 0f, 1f));
    }

    @Test
    void nanDurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Tween.linear(0f, Float.NaN, 0f, 1f));
    }

    @Test
    void infiniteDurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Tween.linear(0f, Float.POSITIVE_INFINITY, 0f, 1f));
    }

    @Test
    void nanFromIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Tween.linear(0f, 1f, Float.NaN, 1f));
    }

    @Test
    void infiniteToIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Tween.linear(0f, 1f, 0f, Float.NEGATIVE_INFINITY));
    }

    @Test
    void nullEasingIsRejected() {
        assertThrows(NullPointerException.class, () -> Tween.eased(0f, 1f, 0f, 1f, null));
    }
}
