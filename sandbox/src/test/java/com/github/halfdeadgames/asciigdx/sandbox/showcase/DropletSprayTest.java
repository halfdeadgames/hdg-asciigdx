package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DropletSprayTest {

    private static final int COUNT = 8;
    private static final float MAX_DISTANCE = 4f;
    private static final float PEAK_SCALE = 1.8f;
    private static final float EPSILON = 1e-5f;
    private final DropletSpray spray = new DropletSpray(new Beat(7.6f, 0.7f), COUNT, MAX_DISTANCE, PEAK_SCALE);

    @Test
    void isInvisibleOutsideTheBeat() {
        assertFalse(spray.isVisibleAt(7.59f));
        assertFalse(spray.isVisibleAt(8.3f));
        assertFalse(spray.isVisibleAt(11.4f));
        assertEquals(0f, spray.alphaAt(7.59f), EPSILON);
        assertEquals(0f, spray.alphaAt(8.3f), EPSILON);
        assertTrue(spray.isVisibleAt(7.6f));
    }

    @Test
    void distanceStartsAtZeroAndGrowsMonotonically() {
        for (int index = 0; index < COUNT; index++) {
            assertEquals(0f, spray.distanceCellsAt(index, 7.0f), EPSILON);
            assertEquals(0f, spray.distanceCellsAt(index, 7.6f), EPSILON);
            float previous = 0f;
            for (float time = 7.6f; time < 8.3f; time += 0.01f) {
                float distance = spray.distanceCellsAt(index, time);
                assertTrue(distance >= previous, "droplet " + index + " time " + time);
                previous = distance;
            }
        }
    }

    @Test
    void distanceEndsAtMaxTimesMultiplier() {
        for (int index = 0; index < COUNT; index++) {
            float multiplier = spray.distanceMultiplier(index);
            assertTrue(multiplier >= 0.6f - EPSILON && multiplier <= 1f + EPSILON);
            assertEquals(MAX_DISTANCE * multiplier, spray.distanceCellsAt(index, 8.3f), EPSILON);
        }
    }

    @Test
    void directionsAreUnitLengthAndDistinct() {
        for (int first = 0; first < COUNT; first++) {
            float x = spray.directionX(first);
            float y = spray.directionY(first);
            assertEquals(1f, (float) Math.hypot(x, y), EPSILON);
            for (int second = first + 1; second < COUNT; second++) {
                boolean same = Math.abs(x - spray.directionX(second)) < EPSILON
                    && Math.abs(y - spray.directionY(second)) < EPSILON;
                assertFalse(same, first + " vs " + second);
            }
        }
    }

    @Test
    void scaleIsOneAtTheEndsAndPeaksInTheMiddle() {
        assertEquals(1f, spray.scaleAt(7.6f), EPSILON);
        assertEquals(1f, spray.scaleAt(8.3f), EPSILON);
        assertEquals(PEAK_SCALE, spray.scaleAt(7.95f), EPSILON);
        assertTrue(spray.scaleAt(7.8f) > 1f && spray.scaleAt(7.8f) < PEAK_SCALE);
    }

    @Test
    void alphaIsFullInTheFirstHalfThenFadesToZero() {
        assertEquals(1f, spray.alphaAt(7.6f), EPSILON);
        assertEquals(1f, spray.alphaAt(7.9f), EPSILON);
        assertEquals(0.5f, spray.alphaAt(8.125f), 1e-3f);
        assertEquals(0f, spray.alphaAt(8.2999f), 1e-3f);
        assertEquals(0f, spray.alphaAt(8.3f), EPSILON);
    }

    @Test
    void sameInputsGiveSameOutputs() {
        DropletSpray other = new DropletSpray(new Beat(7.6f, 0.7f), COUNT, MAX_DISTANCE, PEAK_SCALE);
        for (int index = 0; index < COUNT; index++) {
            assertEquals(spray.directionX(index), other.directionX(index));
            assertEquals(spray.directionY(index), other.directionY(index));
            assertEquals(spray.distanceCellsAt(index, 7.9f), other.distanceCellsAt(index, 7.9f));
        }
    }

    @Test
    void rejectsInvalidArguments() {
        Beat beat = new Beat(7.6f, 0.7f);
        assertThrows(IllegalArgumentException.class, () -> new DropletSpray(null, 8, 4f, 1.8f));
        assertThrows(IllegalArgumentException.class, () -> new DropletSpray(beat, 0, 4f, 1.8f));
        assertThrows(IllegalArgumentException.class, () -> new DropletSpray(beat, 8, 0f, 1.8f));
        assertThrows(IllegalArgumentException.class, () -> new DropletSpray(beat, 8, Float.NaN, 1.8f));
        assertThrows(IllegalArgumentException.class, () -> new DropletSpray(beat, 8, 4f, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new DropletSpray(beat, 8, 4f, -1f));
    }
}
