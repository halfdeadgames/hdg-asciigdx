package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import org.junit.jupiter.api.Test;

class TweenTest {

    private static final float TOLERANCE = 1e-6f;

    private final Tween ascending = new Tween(2f, 4f, 10f, 30f);

    @Test
    void valueBeforeStartIsFrom() {
        assertEquals(10f, ascending.valueAt(1f), TOLERANCE);
    }

    @Test
    void valueAtStartIsFrom() {
        assertEquals(10f, ascending.valueAt(2f), TOLERANCE);
    }

    @Test
    void valueAtMidpointIsHalfwayWithLinearEasing() {
        assertEquals(20f, ascending.valueAt(4f), TOLERANCE);
    }

    @Test
    void valueAtEndIsTo() {
        assertEquals(30f, ascending.valueAt(6f), TOLERANCE);
    }

    @Test
    void valueAfterEndIsTo() {
        assertEquals(30f, ascending.valueAt(100f), TOLERANCE);
    }

    @Test
    void valueDescendsWhenFromIsGreaterThanTo() {
        Tween descending = new Tween(0f, 2f, 8f, 0f);
        assertEquals(6f, descending.valueAt(0.5f), TOLERANCE);
    }

    @Test
    void zeroDurationStaysAtFromBeforeStart() {
        Tween instant = new Tween(3f, 0f, 1f, 5f);
        assertEquals(1f, instant.valueAt(2.9f), TOLERANCE);
    }

    @Test
    void zeroDurationJumpsToToAtStart() {
        Tween instant = new Tween(3f, 0f, 1f, 5f);
        assertEquals(5f, instant.valueAt(3f), TOLERANCE);
    }

    @Test
    void customEasingShapesTheMidpoint() {
        Tween eased = new Tween(0f, 2f, 0f, 100f, Interpolation.pow2In);
        assertEquals(25f, eased.valueAt(1f), TOLERANCE);
    }

    @Test
    void overshootEasingExceedsToInsideTheSpan() {
        Tween overshoot = new Tween(0f, 1f, 0f, 10f, Interpolation.swingOut);
        assertTrue(overshoot.valueAt(0.7f) > 10f);
    }

    @Test
    void overshootEasingEndsExactlyAtTo() {
        Tween overshoot = new Tween(0f, 1f, 0f, 10f, Interpolation.swingOut);
        assertEquals(10f, overshoot.valueAt(1f), 0f);
    }

    @Test
    void queryingOutOfOrderGivesTheSameAnswers() {
        float first = ascending.valueAt(5f);
        ascending.valueAt(0f);
        ascending.valueAt(9f);
        assertEquals(first, ascending.valueAt(5f), 0f);
    }

    @Test
    void progressIsZeroBeforeStart() {
        assertEquals(0f, ascending.progressAt(-5f), 0f);
    }

    @Test
    void progressIsOneAtAndAfterEnd() {
        assertEquals(1f, ascending.progressAt(6f), 0f);
        assertEquals(1f, ascending.progressAt(60f), 0f);
    }

    @Test
    void progressInsideSpanIsEased() {
        Tween eased = new Tween(0f, 2f, 0f, 100f, Interpolation.pow2In);
        assertEquals(0.25f, eased.progressAt(1f), TOLERANCE);
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
        Tween instant = new Tween(3f, 0f, 1f, 5f);
        assertFalse(instant.isActiveAt(3f));
    }

    @Test
    void gettersReportConstructorValues() {
        assertEquals(2f, ascending.start(), 0f);
        assertEquals(6f, ascending.end(), 0f);
        assertEquals(4f, ascending.duration(), 0f);
        assertEquals(10f, ascending.from(), 0f);
        assertEquals(30f, ascending.to(), 0f);
    }

    @Test
    void nanStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Tween(Float.NaN, 1f, 0f, 1f));
    }

    @Test
    void infiniteStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Tween(Float.POSITIVE_INFINITY, 1f, 0f, 1f));
    }

    @Test
    void negativeDurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Tween(0f, -1f, 0f, 1f));
    }

    @Test
    void nanDurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Tween(0f, Float.NaN, 0f, 1f));
    }

    @Test
    void infiniteDurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Tween(0f, Float.POSITIVE_INFINITY, 0f, 1f));
    }

    @Test
    void nanFromIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Tween(0f, 1f, Float.NaN, 1f));
    }

    @Test
    void infiniteToIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Tween(0f, 1f, 0f, Float.NEGATIVE_INFINITY));
    }

    @Test
    void nullEasingIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Tween(0f, 1f, 0f, 1f, null));
    }
}
