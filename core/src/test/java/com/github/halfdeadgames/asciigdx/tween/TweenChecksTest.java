package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TweenChecksTest {

    @Test
    void finiteValuePasses() {
        assertDoesNotThrow(() -> TweenChecks.requireFinite("value", 1.5f));
    }

    @Test
    void nanThrowsWithTheName() {
        assertRejected(Float.NaN);
    }

    @Test
    void positiveInfinityThrowsWithTheName() {
        assertRejected(Float.POSITIVE_INFINITY);
    }

    @Test
    void negativeInfinityThrowsWithTheName() {
        assertRejected(Float.NEGATIVE_INFINITY);
    }

    private static void assertRejected(float value) {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> TweenChecks.requireFinite("someName", value));
        assertTrue(thrown.getMessage().contains("someName"));
    }
}
