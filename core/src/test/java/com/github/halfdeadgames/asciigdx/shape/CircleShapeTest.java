package com.github.halfdeadgames.asciigdx.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CircleShapeTest {

    private static final List<Integer> EXPECTED_ROW_WIDTHS = List.of(1, 5, 5, 7, 5, 5, 1);

    @Test
    void squareCellsGiveSymmetricRowWidths() {
        final CircleShape shape = new CircleShape(10.5f, 10.5f, 3f, ShapeFill.FILLED, 1f);
        final RecordedVisits visits = RecordedVisits.of(shape);

        assertEquals(29, visits.count());
        assertTrue(visits.hasNoDuplicates());
        assertTrue(visits.isInRowMajorOrder());
        for (int i = 0; i < EXPECTED_ROW_WIDTHS.size(); i++) {
            final int row = 7 + i;
            assertEquals(EXPECTED_ROW_WIDTHS.get(i).intValue(), visits.countInRow(row),
                    "row " + row);
            assertEquals(20, visits.minColumnInRow(row) + visits.maxColumnInRow(row), "row " + row);
        }
        assertEquals(7, shape.getLeft());
        assertEquals(7, shape.getTop());
        assertEquals(13, shape.getRight());
        assertEquals(13, shape.getBottom());
        assertEquals(1f, visits.largestFraction(), 0f);
        assertTrue(visits.allFractionsInUnitRange());
        assertEquals(0f, visits.at(10, 10).distance(), 0f);
    }

    @Test
    void outlineIsTheFilledCellsWithAnOutsideSideNeighbor() {
        final RecordedVisits filled =
                RecordedVisits.of(new CircleShape(10.5f, 10.5f, 3f, ShapeFill.FILLED, 1f));
        final RecordedVisits outline =
                RecordedVisits.of(new CircleShape(10.5f, 10.5f, 3f, ShapeFill.OUTLINE, 1f));

        assertTrue(outline.hasNoDuplicates());
        assertTrue(outline.isInRowMajorOrder());
        for (final RecordedVisits.Visit cell : filled.visits) {
            final boolean enclosed = filled.contains(cell.column() - 1, cell.row())
                    && filled.contains(cell.column() + 1, cell.row())
                    && filled.contains(cell.column(), cell.row() - 1)
                    && filled.contains(cell.column(), cell.row() + 1);
            assertEquals(!enclosed, outline.contains(cell.column(), cell.row()));
        }
        assertFalse(outline.contains(10, 10));
        assertEquals(1f, outline.largestFraction(), 0f);
    }

    @Test
    void outlineIsAClosedRing() {
        final RecordedVisits outline =
                RecordedVisits.of(new CircleShape(10.5f, 10.5f, 5f, ShapeFill.OUTLINE, 1f));

        for (final RecordedVisits.Visit cell : outline.visits) {
            int neighbors = 0;
            for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
                for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                    final boolean isSelf = rowOffset == 0 && columnOffset == 0;
                    if (!isSelf && outline.contains(cell.column() + columnOffset,
                            cell.row() + rowOffset)) {
                        neighbors++;
                    }
                }
            }
            assertTrue(neighbors >= 2, "cell " + cell.column() + "," + cell.row());
        }
        assertFalse(outline.contains(10, 10));
    }

    @Test
    void tallCellsSpanHalfTheRowsAndLookRound() {
        final CircleShape square = new CircleShape(10.5f, 10.5f, 3f, ShapeFill.FILLED, 1f);
        final CircleShape tall = new CircleShape(10.5f, 10.5f, 3f, ShapeFill.FILLED, 2f);
        final RecordedVisits visits = RecordedVisits.of(tall);

        assertEquals(17, visits.count());
        assertEquals(3, tall.getBottom() - tall.getTop() + 1);
        assertEquals(5, visits.countInRow(9));
        assertEquals(7, visits.countInRow(10));
        assertEquals(5, visits.countInRow(11));
        final int widthInColumns = tall.getRight() - tall.getLeft() + 1;
        final int heightInColumnWidths = (tall.getBottom() - tall.getTop() + 1) * 2;
        assertTrue(Math.abs(widthInColumns - heightInColumnWidths) <= 1);
        assertTrue(tall.getBottom() - tall.getTop() < square.getBottom() - square.getTop());
    }

    @Test
    void tinyRadiusAroundACellCenterKeepsThatCell() {
        final RecordedVisits visits =
                RecordedVisits.of(new CircleShape(5.5f, 5.5f, 0.1f, ShapeFill.FILLED, 1f));

        assertEquals(1, visits.count());
        assertTrue(visits.contains(5, 5));
        assertEquals(0f, visits.get(0).distanceFraction(), 0f);
    }

    @Test
    void radiusMissingEveryCellCenterFallsBackToTheCenterCell() {
        final CircleShape shape = new CircleShape(5.9f, 5.9f, 0.1f, ShapeFill.OUTLINE, 1f);
        final RecordedVisits visits = RecordedVisits.of(shape);

        assertEquals(1, visits.count());
        assertTrue(visits.contains(5, 5));
        assertEquals(0.4f * (float) Math.sqrt(2), visits.get(0).distance(), 1e-5f);
        assertEquals(0f, visits.get(0).distanceFraction(), 0f);
        assertEquals(5, shape.getLeft());
        assertEquals(5, shape.getBottom());
    }

    @Test
    void offCenterSingleMemberCellHasFractionZero() {
        final RecordedVisits visits =
                RecordedVisits.of(new CircleShape(5.6f, 5.6f, 0.2f, ShapeFill.FILLED, 1f));

        assertEquals(1, visits.count());
        assertTrue(visits.contains(5, 5));
        assertEquals(0f, visits.get(0).distanceFraction(), 0f);
    }

    @Test
    void outOfRangeCenterOrRadiusThrows() {
        final float huge = CircleShape.COORDINATE_LIMIT * 2f;
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(huge, 0f, 1f, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, huge, 1f, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, huge, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, Float.MAX_VALUE, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, 1f, ShapeFill.FILLED, 1f).setCenter(huge, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, 1f, ShapeFill.FILLED, 1f).setRadius(huge));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, 1e7f, ShapeFill.FILLED, 0.1f));
    }

    @Test
    void movingAndResizingChangeCellsAndBounds() {
        final CircleShape shape = new CircleShape(10.5f, 10.5f, 3f, ShapeFill.FILLED, 1f);
        shape.setCenter(0.5f, -4.5f);

        assertEquals(-3, shape.getLeft());
        assertEquals(-8, shape.getTop());
        assertEquals(3, shape.getRight());
        assertEquals(-2, shape.getBottom());
        assertTrue(RecordedVisits.of(shape).contains(0, -5));

        shape.setRadius(1f);
        assertEquals(5, RecordedVisits.of(shape).count());
        assertEquals(-1, shape.getLeft());
    }

    @Test
    void invalidArgumentsThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, 0f, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, -1f, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, Float.POSITIVE_INFINITY, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(Float.NaN, 0f, 1f, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, Float.POSITIVE_INFINITY, 1f, ShapeFill.FILLED, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, 1f, ShapeFill.FILLED, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, 1f, ShapeFill.FILLED, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new CircleShape(0f, 0f, 1f, ShapeFill.FILLED, 1f).setRadius(Float.NaN));
    }
}
