package com.github.halfdeadgames.asciigdx.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LineShapeTest {

    @Test
    void horizontalLineVisitsLeftToRightWithSymmetricDistances() {
        final HorizontalLineShape shape = new HorizontalLineShape(3, 4, 5);
        final RecordedVisits visits = RecordedVisits.of(shape);

        assertEquals(5, visits.count());
        assertTrue(visits.isInRowMajorOrder());
        final float[] expectedDistances = {2f, 1f, 0f, 1f, 2f};
        final float[] expectedFractions = {1f, 0.5f, 0f, 0.5f, 1f};
        for (int i = 0; i < 5; i++) {
            assertEquals(3 + i, visits.get(i).column());
            assertEquals(4, visits.get(i).row());
            assertEquals(expectedDistances[i], visits.get(i).distance(), 1e-6f);
            assertEquals(expectedFractions[i], visits.get(i).distanceFraction(), 1e-6f);
        }
        assertEquals(3, shape.getLeft());
        assertEquals(4, shape.getTop());
        assertEquals(7, shape.getRight());
        assertEquals(4, shape.getBottom());
    }

    @Test
    void evenLengthCenterFallsBetweenTwoCells() {
        final RecordedVisits visits = RecordedVisits.of(new HorizontalLineShape(0, 0, 4));

        assertEquals(1.5f, visits.get(0).distance(), 1e-6f);
        assertEquals(0.5f, visits.get(1).distance(), 1e-6f);
        assertEquals(0.5f, visits.get(2).distance(), 1e-6f);
        assertEquals(1f, visits.largestFraction(), 0f);
    }

    @Test
    void verticalLineScalesDistanceByAspect() {
        final VerticalLineShape shape = VerticalLineShape.forCellAspect(1, 1, 3, 2f);
        final RecordedVisits visits = RecordedVisits.of(shape);

        assertEquals(3, visits.count());
        assertTrue(visits.isInRowMajorOrder());
        assertEquals(2f, visits.get(0).distance(), 1e-6f);
        assertEquals(0f, visits.get(1).distance(), 1e-6f);
        assertEquals(2f, visits.get(2).distance(), 1e-6f);
        assertEquals(1f, visits.get(0).distanceFraction(), 0f);
        assertEquals(1f, visits.get(2).distanceFraction(), 0f);
        assertEquals(1, shape.getLeft());
        assertEquals(1, shape.getRight());
        assertEquals(1, shape.getTop());
        assertEquals(3, shape.getBottom());
    }

    @Test
    void lengthOneHasFractionZero() {
        final RecordedVisits horizontal = RecordedVisits.of(new HorizontalLineShape(0, 0, 1));
        final RecordedVisits vertical = RecordedVisits.of(VerticalLineShape.forSquareCells(0, 0, 1));

        assertEquals(1, horizontal.count());
        assertEquals(0f, horizontal.get(0).distanceFraction(), 0f);
        assertEquals(1, vertical.count());
        assertEquals(0f, vertical.get(0).distanceFraction(), 0f);
    }

    @Test
    void movingAndResizingChangeCellsAndBounds() {
        final HorizontalLineShape horizontal = new HorizontalLineShape(0, 0, 2);
        horizontal.setPosition(-5, 7);
        horizontal.setLength(3);
        assertEquals(-5, horizontal.getLeft());
        assertEquals(-3, horizontal.getRight());
        assertEquals(7, horizontal.getTop());
        assertEquals(-5, RecordedVisits.of(horizontal).get(0).column());

        final VerticalLineShape vertical = VerticalLineShape.forSquareCells(0, 0, 2);
        vertical.setPosition(4, -6);
        vertical.setLength(4);
        assertEquals(4, vertical.getLeft());
        assertEquals(-6, vertical.getTop());
        assertEquals(-3, vertical.getBottom());
        assertEquals(-6, RecordedVisits.of(vertical).get(0).row());
    }

    @Test
    void invalidArgumentsThrow() {
        assertThrows(IllegalArgumentException.class, () -> new HorizontalLineShape(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> VerticalLineShape.forSquareCells(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> VerticalLineShape.forCellAspect(0, 0, 1, 0f));
        assertThrows(IllegalArgumentException.class, () -> VerticalLineShape.forCellAspect(0, 0, 1, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new HorizontalLineShape(0, 0, 1).setLength(0));
        assertThrows(IllegalArgumentException.class,
                () -> new HorizontalLineShape(Integer.MAX_VALUE, 0, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new HorizontalLineShape(Integer.MAX_VALUE - 2, 0, 3));
        assertThrows(IllegalArgumentException.class,
                () -> VerticalLineShape.forSquareCells(0, Integer.MAX_VALUE - 2, 3));
    }
}
