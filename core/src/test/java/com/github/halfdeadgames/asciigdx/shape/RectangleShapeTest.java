package com.github.halfdeadgames.asciigdx.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RectangleShapeTest {

    @Test
    void filledVisitsEveryCellInRowMajorOrder() {
        final RectangleShape shape = RectangleShape.forSquareCells(2, 5, 4, 3, ShapeFill.FILLED);
        final RecordedVisits visits = RecordedVisits.of(shape);

        assertEquals(12, visits.count());
        assertTrue(visits.isInRowMajorOrder());
        assertTrue(visits.hasNoDuplicates());
        assertEquals(2, visits.get(0).column());
        assertEquals(5, visits.get(0).row());
        assertEquals(5, visits.get(11).column());
        assertEquals(7, visits.get(11).row());
    }

    @Test
    void boundsAreInclusive() {
        final RectangleShape shape = RectangleShape.forSquareCells(2, 5, 4, 3, ShapeFill.FILLED);

        assertEquals(2, shape.getLeft());
        assertEquals(5, shape.getTop());
        assertEquals(5, shape.getRight());
        assertEquals(7, shape.getBottom());
    }

    @Test
    void centerCellHasZeroDistanceAndCornersHaveFractionOne() {
        final RecordedVisits visits = RecordedVisits.of(RectangleShape.forSquareCells(0, 0, 3, 3, ShapeFill.FILLED));

        assertEquals(0f, visits.at(1, 1).distance(), 0f);
        assertEquals(0f, visits.at(1, 1).distanceFraction(), 0f);
        assertEquals((float) Math.sqrt(2), visits.at(0, 0).distance(), 1e-6f);
        assertEquals(1f, visits.at(0, 0).distanceFraction(), 0f);
        assertEquals(1f, visits.at(2, 2).distanceFraction(), 0f);
        assertEquals(1f, visits.largestFraction(), 0f);
        assertTrue(visits.allFractionsInUnitRange());
    }

    @Test
    void rowDistanceIsScaledByAspect() {
        final RecordedVisits visits = RecordedVisits.of(
                RectangleShape.forCellAspect(0, 0, 3, 3, ShapeFill.FILLED, 2f));

        assertEquals(2f, visits.at(1, 0).distance(), 1e-6f);
        assertEquals(1f, visits.largestFraction(), 0f);
    }

    @Test
    void outlineHasBorderCellsOnly() {
        final RectangleShape shape = RectangleShape.forSquareCells(0, 0, 5, 4, ShapeFill.OUTLINE);
        final RecordedVisits visits = RecordedVisits.of(shape);

        assertEquals(2 * 5 + 2 * 4 - 4, visits.count());
        assertTrue(visits.hasNoDuplicates());
        assertTrue(visits.isInRowMajorOrder());
        assertFalse(visits.contains(2, 1));
        assertFalse(visits.contains(3, 2));
        assertTrue(visits.contains(0, 2));
        assertTrue(visits.contains(4, 2));
        assertEquals(1f, visits.largestFraction(), 0f);
    }

    @Test
    void thinOutlinesContainAllCells() {
        assertEquals(5, outlineCount(1, 5));
        assertEquals(5, outlineCount(5, 1));
        assertEquals(10, outlineCount(2, 5));
        assertEquals(10, outlineCount(5, 2));
        assertEquals(1, outlineCount(1, 1));
        assertEquals(8, outlineCount(3, 3));
    }

    @Test
    void singleCellFractionIsZero() {
        final RecordedVisits visits = RecordedVisits.of(
                RectangleShape.forSquareCells(4, 4, 1, 1, ShapeFill.FILLED));

        assertEquals(1, visits.count());
        assertEquals(0f, visits.get(0).distanceFraction(), 0f);
    }

    @Test
    void movingChangesVisitedCellsAndBounds() {
        final RectangleShape shape = RectangleShape.forSquareCells(0, 0, 2, 2, ShapeFill.FILLED);
        shape.setPosition(10, 20);
        final RecordedVisits visits = RecordedVisits.of(shape);

        assertEquals(10, visits.get(0).column());
        assertEquals(20, visits.get(0).row());
        assertEquals(10, shape.getLeft());
        assertEquals(21, shape.getBottom());

        shape.setSize(3, 1);
        assertEquals(12, shape.getRight());
        assertEquals(20, shape.getBottom());
        assertEquals(3, RecordedVisits.of(shape).count());
    }

    @Test
    void negativeCoordinatesAreVisitedAsIs() {
        final RecordedVisits visits = RecordedVisits.of(
                RectangleShape.forSquareCells(-2, -1, 2, 2, ShapeFill.FILLED));

        assertEquals(-2, visits.get(0).column());
        assertEquals(-1, visits.get(0).row());
        assertTrue(visits.contains(-1, 0));
    }

    @Test
    void invalidArgumentsThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> RectangleShape.forSquareCells(0, 0, 0, 1, ShapeFill.FILLED));
        assertThrows(IllegalArgumentException.class,
                () -> RectangleShape.forSquareCells(0, 0, 1, 0, ShapeFill.FILLED));
        assertThrows(IllegalArgumentException.class,
                () -> RectangleShape.forCellAspect(0, 0, 1, 1, ShapeFill.FILLED, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> RectangleShape.forCellAspect(0, 0, 1, 1, ShapeFill.FILLED, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> RectangleShape.forSquareCells(Integer.MAX_VALUE, 0, 2, 1, ShapeFill.FILLED));
        assertThrows(IllegalArgumentException.class,
                () -> RectangleShape.forSquareCells(Integer.MAX_VALUE - 2, 0, 3, 1, ShapeFill.FILLED));
        assertThrows(IllegalArgumentException.class,
                () -> RectangleShape.forSquareCells(0, Integer.MAX_VALUE - 2, 1, 3, ShapeFill.FILLED));
        assertThrows(IllegalArgumentException.class,
                () -> RectangleShape.forSquareCells(0, 0, 1, 1, ShapeFill.FILLED).setSize(0, 1));
    }

    private static int outlineCount(int width, int height) {
        final RecordedVisits visits = RecordedVisits.of(
                RectangleShape.forSquareCells(0, 0, width, height, ShapeFill.OUTLINE));
        assertTrue(visits.hasNoDuplicates());
        return visits.count();
    }
}
