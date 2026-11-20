package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValueRangeTest {

    @Test
    void snapsToTheNearestStepFromTheMinimum() {
        ValueRange range = ValueRange.of(1f, 10f, 2f);

        assertEquals(1f, range.snap(1.9f));
        assertEquals(3f, range.snap(2.1f));
        assertEquals(5f, range.snap(4.6f));
    }

    @Test
    void clampsToTheRange() {
        ValueRange range = ValueRange.of(0f, 10f, 1f);

        assertEquals(0f, range.snap(-5f));
        assertEquals(10f, range.snap(99f));
        assertEquals(10f, range.snap(Float.POSITIVE_INFINITY));
        assertEquals(0f, range.snap(Float.NEGATIVE_INFINITY));
    }

    @Test
    void maximumIsReachableWhenItIsNotAMultipleOfTheStep() {
        ValueRange range = ValueRange.of(0f, 10f, 3f);

        assertEquals(9f, range.snap(8f));
        assertEquals(9f, range.snap(9.9f));
        assertEquals(10f, range.snap(10f));
        assertEquals(10f, range.stepUp(9f));
    }

    @Test
    void repeatedStepsDoNotAccumulateRoundingErrors() {
        ValueRange range = ValueRange.of(0f, 1f, 0.1f);
        float value = range.getMin();
        for (int i = 0; i < 10; i++) {
            value = range.stepUp(value);
        }

        assertEquals(1f, value);
        assertEquals(0.3f, range.snap(0.31f), 1e-6f);
        assertEquals(0.7f, range.snap(0.69f), 1e-6f);
    }

    @Test
    void stepUpAndDownStopAtTheEnds() {
        ValueRange range = ValueRange.of(0f, 4f, 1f);

        assertEquals(4f, range.stepUp(4f));
        assertEquals(0f, range.stepDown(0f));
        assertEquals(3f, range.stepDown(4f));
    }

    @Test
    void stepDownFromAMaximumOffTheGridGoesToTheHighestGridValue() {
        ValueRange range = ValueRange.of(0f, 10f, 3f);

        assertEquals(9f, range.stepDown(10f));
        assertEquals(6f, range.stepDown(9f));
        assertEquals(3f, range.stepDown(6f));
        assertEquals(0f, range.stepDown(3f));
        assertEquals(0f, range.stepDown(0f));
    }

    @Test
    void stepDownFromAMaximumOnTheGridGoesOneStep() {
        assertEquals(0.9f, ValueRange.of(0f, 1f, 0.1f).stepDown(1f), 1e-6f);
        assertEquals(8f, ValueRange.of(2f, 10f, 2f).stepDown(10f));
    }

    @Test
    void stepDownFromAMaximumWithinOneStepOfTheMinimumGoesToTheMinimum() {
        assertEquals(0f, ValueRange.of(0f, 1f, 5f).stepDown(1f));
    }

    @Test
    void nanIsRejected() {
        ValueRange range = ValueRange.of(0f, 1f, 0.5f);

        assertThrows(IllegalArgumentException.class, () -> range.snap(Float.NaN));
    }

    @Test
    void invalidRangesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(1f, 1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(2f, 1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(0f, 1f, 0f));
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(0f, 1f, -1f));
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(Float.NaN, 1f, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> ValueRange.of(0f, Float.POSITIVE_INFINITY, 1f));
        assertThrows(IllegalArgumentException.class, () -> ValueRange.of(0f, 1f, Float.NaN));
    }

    @Test
    void fractionAndValueAreInverse() {
        ValueRange range = ValueRange.of(-10f, 10f, 1f);

        assertEquals(0.25f, range.fractionOf(-5f), 1e-6f);
        assertEquals(-5f, range.valueAtFraction(0.25f));
        assertEquals(10f, range.valueAtFraction(7f));
        assertEquals(-10f, range.valueAtFraction(-1f));
    }

    @Test
    void gettersEqualsHashCodeAndToStringDescribeTheRange() {
        ValueRange range = ValueRange.of(1f, 5f, 2f);

        assertEquals(1f, range.getMin());
        assertEquals(5f, range.getMax());
        assertEquals(2f, range.getStepSize());
        assertEquals(ValueRange.of(1f, 5f, 2f), range);
        assertEquals(ValueRange.of(1f, 5f, 2f).hashCode(), range.hashCode());
        assertNotEquals(ValueRange.of(1f, 5f, 1f), range);
        assertNotEquals(ValueRange.of(0f, 5f, 2f), range);
        assertNotEquals(ValueRange.of(1f, 6f, 2f), range);
        assertTrue(range.toString().contains("min=1.0"));
    }
}
