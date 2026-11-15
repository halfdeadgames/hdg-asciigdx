package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpringOffsetTest {

    private static final float FRAME = 1f / 60f;
    private static final float TOLERANCE = 1e-5f;

    @Test
    void newSpringIsAtRestAtOrigin() {
        var spring = new SpringOffset();
        assertTrue(spring.isAtRest());
        assertEquals(0f, spring.getOffsetX());
        assertEquals(0f, spring.getOffsetY());
    }

    @Test
    void pushMovesOffsetInPushDirection() {
        var spring = new SpringOffset();
        spring.push(2f, -1f);
        spring.advance(FRAME);
        assertTrue(spring.getOffsetX() > 0f);
        assertTrue(spring.getOffsetY() < 0f);
        assertFalse(spring.isAtRest());
    }

    @Test
    void pushOvershootsPastCenterAndSettlesWithinTwoSeconds() {
        var spring = new SpringOffset();
        spring.push(2f, 0f);
        var crossedCenter = false;
        for (var frame = 0; frame < 120; frame++) {
            spring.advance(FRAME);
            if (spring.getOffsetX() < 0f) {
                crossedCenter = true;
            }
        }
        assertTrue(crossedCenter);
        assertTrue(spring.isAtRest());
    }

    @Test
    void offsetNeverExceedsMaxDistanceAndStillSettles() {
        var spring = new SpringOffset();
        spring.push(500f, 500f);
        for (var frame = 0; frame < 120; frame++) {
            spring.advance(FRAME);
            var distance = Math.hypot(spring.getOffsetX(), spring.getOffsetY());
            assertTrue(distance <= SpringOffset.DEFAULT_MAX_DISTANCE + TOLERANCE);
        }
        for (var frame = 0; frame < 240; frame++) {
            spring.advance(FRAME);
        }
        assertTrue(spring.isAtRest());
    }

    @Test
    void twoPushesMoveFartherThanOne() {
        var single = new SpringOffset();
        single.push(1f, 0f);
        single.advance(FRAME);
        var doubled = new SpringOffset();
        doubled.push(1f, 0f);
        doubled.push(1f, 0f);
        doubled.advance(FRAME);
        assertTrue(doubled.getOffsetX() > single.getOffsetX());
    }

    @Test
    void zeroOrNegativeDeltaChangesNothing() {
        var spring = new SpringOffset();
        spring.push(1f, 1f);
        spring.advance(0f);
        spring.advance(-1f);
        assertEquals(0f, spring.getOffsetX());
        assertEquals(0f, spring.getOffsetY());
        assertEquals(1f, spring.getVelocityX());
        assertEquals(1f, spring.getVelocityY());
    }

    @Test
    void hugeDeltaBehavesLikeTheCap() {
        var capped = new SpringOffset();
        capped.push(1f, 0.5f);
        capped.advance(0.1f);
        var huge = new SpringOffset();
        huge.push(1f, 0.5f);
        huge.advance(5f);
        assertEquals(capped.getOffsetX(), huge.getOffsetX(), TOLERANCE);
        assertEquals(capped.getOffsetY(), huge.getOffsetY(), TOLERANCE);
        assertEquals(capped.getVelocityX(), huge.getVelocityX(), TOLERANCE);
    }

    @Test
    void resetReturnsToRest() {
        var spring = new SpringOffset();
        spring.push(3f, 3f);
        spring.advance(FRAME);
        spring.reset();
        assertTrue(spring.isAtRest());
        assertEquals(0f, spring.getVelocityX());
    }

    private static void advanceSeconds(SpringOffset spring, int seconds) {
        for (var frame = 0; frame < seconds * 60; frame++) {
            spring.advance(FRAME);
        }
    }

    @Test
    void movedTargetPullsOffsetToTargetAndRests() {
        var spring = new SpringOffset();
        spring.setTarget(0.4f, 0f);
        advanceSeconds(spring, 2);
        assertEquals(0.4f, spring.getOffsetX());
        assertEquals(0f, spring.getOffsetY());
        assertTrue(spring.isAtRest());
    }

    @Test
    void targetBeyondMaxDistanceIsClamped() {
        var spring = new SpringOffset();
        spring.setTarget(3f, 4f);
        assertEquals(SpringOffset.DEFAULT_MAX_DISTANCE,
                Math.hypot(spring.getTargetX(), spring.getTargetY()), TOLERANCE);
        assertEquals(0.6f * SpringOffset.DEFAULT_MAX_DISTANCE, spring.getTargetX(), TOLERANCE);
        assertEquals(0.8f * SpringOffset.DEFAULT_MAX_DISTANCE, spring.getTargetY(), TOLERANCE);
    }

    @Test
    void targetBackToZeroReturnsOffsetToZero() {
        var spring = new SpringOffset();
        spring.setTarget(0.4f, 0.2f);
        advanceSeconds(spring, 2);
        spring.setTarget(0f, 0f);
        advanceSeconds(spring, 2);
        assertEquals(0f, spring.getOffsetX());
        assertEquals(0f, spring.getOffsetY());
        assertTrue(spring.isAtRest());
    }

    @Test
    void changingTargetAloneMakesSpringNotAtRest() {
        var spring = new SpringOffset();
        spring.setTarget(0.4f, 0f);
        assertFalse(spring.isAtRest());
    }

    @Test
    void resetClearsTarget() {
        var spring = new SpringOffset();
        spring.setTarget(0.4f, 0f);
        spring.reset();
        assertEquals(0f, spring.getTargetX());
        assertTrue(spring.isAtRest());
    }

    @Test
    void constructorRejectsNonPositiveValues() {
        assertThrows(IllegalArgumentException.class, () -> new SpringOffset(0f, 0.3f, 0.75f));
        assertThrows(IllegalArgumentException.class, () -> new SpringOffset(1.2f, 0f, 0.75f));
        assertThrows(IllegalArgumentException.class, () -> new SpringOffset(1.2f, 0.3f, 0f));
        assertThrows(IllegalArgumentException.class, () -> new SpringOffset(-1f, 0.3f, 0.75f));
    }
}
