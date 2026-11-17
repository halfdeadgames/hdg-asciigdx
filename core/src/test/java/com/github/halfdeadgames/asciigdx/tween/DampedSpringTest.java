package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DampedSpringTest {

    private static final float[] DAMPING_RATIOS = {0.3f, 1.0f, 1.00005f, 2.5f};
    private static final float[] FREQUENCIES = {1.2f, 0.4f};
    private static final double SIMULATION_STEP = 1e-4;
    private static final double SAMPLE_INTERVAL = 0.05;
    private static final int SAMPLE_COUNT = 100;
    private static final double SIMULATION_TOLERANCE = 1e-4;

    private static final class Simulation {
        private final double omega;
        private final double zeta;
        private double position;
        private double velocity;
        private double rest;

        Simulation(float frequencyHz, float dampingRatio) {
            omega = 2.0 * Math.PI * frequencyHz;
            zeta = dampingRatio;
        }

        void push(double velocityChange) {
            velocity += velocityChange;
        }

        void moveRest(double distance) {
            rest += distance;
        }

        void advance(double seconds) {
            var stepCount = (int) Math.round(seconds / SIMULATION_STEP);
            for (int i = 0; i < stepCount; i++) {
                stepRungeKutta();
            }
        }

        double getPosition() {
            return position;
        }

        private double acceleration(double currentPosition, double currentVelocity) {
            return -omega * omega * (currentPosition - rest)
                    - 2.0 * zeta * omega * currentVelocity;
        }

        private void stepRungeKutta() {
            var h = SIMULATION_STEP;
            var k1x = velocity;
            var k1v = acceleration(position, velocity);
            var k2x = velocity + 0.5 * h * k1v;
            var k2v = acceleration(position + 0.5 * h * k1x, velocity + 0.5 * h * k1v);
            var k3x = velocity + 0.5 * h * k2v;
            var k3v = acceleration(position + 0.5 * h * k2x, velocity + 0.5 * h * k2v);
            var k4x = velocity + h * k3v;
            var k4v = acceleration(position + h * k3x, velocity + h * k3v);
            position += h / 6.0 * (k1x + 2.0 * k2x + 2.0 * k3x + k4x);
            velocity += h / 6.0 * (k1v + 2.0 * k2v + 2.0 * k3v + k4v);
        }
    }

    @Test
    void impulseOffset_matchesNumericSimulation() {
        for (var frequency : FREQUENCIES) {
            for (var damping : DAMPING_RATIOS) {
                var spring = DampedSpring.ofFrequency(frequency, damping);
                var simulation = new Simulation(frequency, damping);
                simulation.push(1.0);
                for (int sample = 1; sample <= SAMPLE_COUNT; sample++) {
                    simulation.advance(SAMPLE_INTERVAL);
                    var seconds = (float) (sample * SAMPLE_INTERVAL);
                    assertEquals(simulation.getPosition(), spring.getImpulseOffsetAt(seconds),
                            SIMULATION_TOLERANCE, "f=" + frequency + " z=" + damping
                                    + " t=" + seconds);
                }
            }
        }
    }

    @Test
    void stepProgress_matchesNumericSimulation() {
        for (var frequency : FREQUENCIES) {
            for (var damping : DAMPING_RATIOS) {
                var spring = DampedSpring.ofFrequency(frequency, damping);
                var simulation = new Simulation(frequency, damping);
                simulation.moveRest(1.0);
                for (int sample = 1; sample <= SAMPLE_COUNT; sample++) {
                    simulation.advance(SAMPLE_INTERVAL);
                    var seconds = (float) (sample * SAMPLE_INTERVAL);
                    assertEquals(simulation.getPosition(), spring.getStepProgressAt(seconds),
                            SIMULATION_TOLERANCE, "f=" + frequency + " z=" + damping
                                    + " t=" + seconds);
                }
            }
        }
    }

    @Test
    void twoPushesAndARestMove_sumMatchesCombinedSimulation() {
        for (var damping : DAMPING_RATIOS) {
            var spring = DampedSpring.ofFrequency(1.2f, damping);
            var simulation = new Simulation(1.2f, damping);
            simulation.push(2.0);
            simulation.advance(0.7);
            simulation.push(-1.5);
            simulation.moveRest(0.5);
            simulation.advance(2.3);
            var nowSeconds = 3.0f;
            var summed = 2.0f * spring.getImpulseOffsetAt(nowSeconds)
                    - 1.5f * spring.getImpulseOffsetAt(nowSeconds - 0.7f)
                    + 0.5f * spring.getStepProgressAt(nowSeconds - 0.7f);
            assertEquals(simulation.getPosition(), summed, SIMULATION_TOLERANCE, "z=" + damping);
        }
    }

    @Test
    void responses_areZeroAtAndBeforeTimeZero() {
        for (var damping : DAMPING_RATIOS) {
            var spring = DampedSpring.ofFrequency(1.2f, damping);
            assertEquals(0f, spring.getImpulseOffsetAt(0f));
            assertEquals(0f, spring.getStepProgressAt(0f));
            assertEquals(0f, spring.getImpulseOffsetAt(-1f));
            assertEquals(0f, spring.getStepProgressAt(-1f));
        }
    }

    @Test
    void impulseOffset_hasSlopeOfOneAtTimeZero() {
        for (var damping : DAMPING_RATIOS) {
            var spring = DampedSpring.ofFrequency(1.2f, damping);
            var tinySeconds = 1e-3f;
            assertEquals(1f, spring.getImpulseOffsetAt(tinySeconds) / tinySeconds, 2e-2f);
        }
    }

    @Test
    void stepProgress_tendsToOneForLargeTimes() {
        for (var damping : DAMPING_RATIOS) {
            var spring = DampedSpring.ofFrequency(1.2f, damping);
            assertEquals(1f, spring.getStepProgressAt(30f), 1e-4f);
        }
    }

    @Test
    void stepProgress_overshootsOnlyWhenUnderdamped() {
        assertTrue(maxStepProgress(DampedSpring.ofFrequency(1.2f, 0.3f)) > 1.1f);
        assertTrue(maxStepProgress(DampedSpring.ofFrequency(1.2f, 1.0f)) <= 1.0f);
        assertTrue(maxStepProgress(DampedSpring.ofFrequency(1.2f, 1.00005f)) <= 1.0f);
        assertTrue(maxStepProgress(DampedSpring.ofFrequency(1.2f, 2.5f)) <= 1.0f);
    }

    private static final float SETTLE_SAMPLE_STEP_SECONDS = 0.002f;
    private static final int SETTLE_SAMPLE_COUNT = 10000;

    private static float pushLeftover(DampedSpring spring, float pushSpeed, float seconds) {
        return Math.abs(pushSpeed * spring.getImpulseOffsetAt(seconds));
    }

    private static float moveLeftover(DampedSpring spring, float moveDistance, float seconds) {
        return Math.abs(moveDistance * (1f - spring.getStepProgressAt(seconds)));
    }

    @Test
    void getImpulseSettleSeconds_boundsThePushForAllLaterTimes() {
        for (var frequency : FREQUENCIES) {
            for (var damping : DAMPING_RATIOS) {
                var spring = DampedSpring.ofFrequency(frequency, damping);
                for (var pushSpeed : new float[] {0.5f, -4f, 8f}) {
                    var tolerance = 0.01f;
                    var settleSeconds = spring.getImpulseSettleSeconds(pushSpeed, tolerance);
                    assertTrue(settleSeconds >= 0f);
                    for (int i = 0; i <= SETTLE_SAMPLE_COUNT; i++) {
                        var seconds = settleSeconds + i * SETTLE_SAMPLE_STEP_SECONDS;
                        assertTrue(pushLeftover(spring, pushSpeed, seconds) <= tolerance + 1e-6f,
                                "f=" + frequency + " z=" + damping + " v=" + pushSpeed
                                        + " t=" + seconds);
                    }
                }
            }
        }
    }

    @Test
    void getStepSettleSeconds_boundsTheMoveForAllLaterTimes() {
        for (var frequency : FREQUENCIES) {
            for (var damping : DAMPING_RATIOS) {
                var spring = DampedSpring.ofFrequency(frequency, damping);
                for (var moveDistance : new float[] {0.5f, -2f, 8f}) {
                    var tolerance = 0.01f;
                    var settleSeconds = spring.getStepSettleSeconds(moveDistance, tolerance);
                    assertTrue(settleSeconds >= 0f);
                    for (int i = 0; i <= SETTLE_SAMPLE_COUNT; i++) {
                        var seconds = settleSeconds + i * SETTLE_SAMPLE_STEP_SECONDS;
                        assertTrue(moveLeftover(spring, moveDistance, seconds)
                                <= tolerance + 1e-6f, "f=" + frequency + " z=" + damping
                                        + " d=" + moveDistance + " t=" + seconds);
                    }
                }
            }
        }
    }

    @Test
    void settleSeconds_areNotMuchLaterThanNeeded() {
        var tolerance = 0.01f;
        var size = 4f;
        for (var frequency : FREQUENCIES) {
            for (var damping : DAMPING_RATIOS) {
                var spring = DampedSpring.ofFrequency(frequency, damping);
                var pushSettle = spring.getImpulseSettleSeconds(size, tolerance);
                var moveSettle = spring.getStepSettleSeconds(size, tolerance);
                var pushSeenLate = false;
                var moveSeenLate = false;
                for (float seconds = pushSettle / 2f; seconds < pushSettle; seconds += 0.001f) {
                    pushSeenLate |= pushLeftover(spring, size, seconds) > tolerance;
                }
                for (float seconds = moveSettle / 2f; seconds < moveSettle; seconds += 0.001f) {
                    moveSeenLate |= moveLeftover(spring, size, seconds) > tolerance;
                }
                assertTrue(pushSeenLate, "push f=" + frequency + " z=" + damping);
                assertTrue(moveSeenLate, "move f=" + frequency + " z=" + damping);
            }
        }
    }

    @Test
    void settleSeconds_eventsWithinToleranceSettleImmediately() {
        var spring = DampedSpring.ofFrequency(1.2f, 0.3f);
        assertEquals(0f, spring.getImpulseSettleSeconds(0f, 0.01f));
        assertEquals(0f, spring.getStepSettleSeconds(0f, 0.01f));
        assertEquals(0f, spring.getStepSettleSeconds(-0.01f, 0.01f));
        assertEquals(0f, spring.getImpulseSettleSeconds(0.001f, 0.01f));
    }

    @Test
    void settleSeconds_extremeSpringsInRangeStayFinite() {
        for (var frequency : new float[] {0.001f, 1000f}) {
            for (var damping : new float[] {0.001f, 1f, 1000f}) {
                var spring = DampedSpring.ofFrequency(frequency, damping);
                var pushSettle = spring.getImpulseSettleSeconds(Float.MAX_VALUE, Float.MIN_VALUE);
                var moveSettle = spring.getStepSettleSeconds(Float.MAX_VALUE, Float.MIN_VALUE);
                assertTrue(Float.isFinite(pushSettle), "push f=" + frequency + " z=" + damping);
                assertTrue(Float.isFinite(moveSettle), "move f=" + frequency + " z=" + damping);
            }
        }
    }

    @Test
    void ofFrequency_acceptsTheRangeBoundaries() {
        DampedSpring.ofFrequency(0.001f, 0.001f);
        DampedSpring.ofFrequency(1000f, 1000f);
    }

    @Test
    void ofFrequency_rejectsInvalidFrequencyAndDamping() {
        var invalidValues = new float[] {0f, -1f, 0.0009f, 1001f, Float.NaN,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (var invalid : invalidValues) {
            assertThrows(IllegalArgumentException.class,
                    () -> DampedSpring.ofFrequency(invalid, 0.5f));
            assertThrows(IllegalArgumentException.class,
                    () -> DampedSpring.ofFrequency(1f, invalid));
        }
    }

    @Test
    void settleSeconds_rejectInvalidArguments() {
        var spring = DampedSpring.ofFrequency(1.2f, 0.5f);
        for (var invalid : new float[] {0f, -0.5f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> spring.getImpulseSettleSeconds(1f, invalid));
            assertThrows(IllegalArgumentException.class,
                    () -> spring.getStepSettleSeconds(1f, invalid));
        }
        for (var invalid : new float[] {Float.NaN, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> spring.getImpulseSettleSeconds(invalid, 0.01f));
            assertThrows(IllegalArgumentException.class,
                    () -> spring.getStepSettleSeconds(invalid, 0.01f));
        }
    }

    @Test
    void queries_rejectNonFiniteElapsedSeconds() {
        var spring = DampedSpring.ofFrequency(1.2f, 0.5f);
        for (var invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> spring.getImpulseOffsetAt(invalid));
            assertThrows(IllegalArgumentException.class, () -> spring.getStepProgressAt(invalid));
        }
    }

    @Test
    void equalsAndHashCode_followFrequencyAndDampingRatio() {
        var spring = DampedSpring.ofFrequency(1.2f, 0.5f);
        assertEquals(spring, DampedSpring.ofFrequency(1.2f, 0.5f));
        assertEquals(spring.hashCode(), DampedSpring.ofFrequency(1.2f, 0.5f).hashCode());
        assertNotEquals(spring, DampedSpring.ofFrequency(1.3f, 0.5f));
        assertNotEquals(spring, DampedSpring.ofFrequency(1.2f, 0.6f));
        assertNotEquals(spring, null);
        assertEquals(1.2f, spring.getFrequencyHz());
        assertEquals(0.5f, spring.getDampingRatio());
    }

    private static float maxStepProgress(DampedSpring spring) {
        var maximum = 0f;
        for (float seconds = 0f; seconds < 10f; seconds += 0.001f) {
            maximum = Math.max(maximum, spring.getStepProgressAt(seconds));
        }
        return maximum;
    }
}
