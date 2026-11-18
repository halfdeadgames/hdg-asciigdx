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
        final RecordedVisits vertical =
                RecordedVisits.of(VerticalLineShape.forSquareCells(0, 0, 1));

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
        assertThrows(IllegalArgumentException.class,
                () -> VerticalLineShape.forSquareCells(0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> VerticalLineShape.forCellAspect(0, 0, 1, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> VerticalLineShape.forCellAspect(0, 0, 1, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new HorizontalLineShape(0, 0, 1).setLength(0));
        assertThrows(IllegalArgumentException.class,
                () -> new HorizontalLineShape(Integer.MAX_VALUE, 0, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new HorizontalLineShape(Integer.MAX_VALUE - 2, 0, 3));
        assertThrows(IllegalArgumentException.class,
                () -> VerticalLineShape.forSquareCells(0, Integer.MAX_VALUE - 2, 3));
    }

    private static String cellsOf(CellShape shape) {
        final StringBuilder cells = new StringBuilder();
        for (final RecordedVisits.Visit visit : RecordedVisits.of(shape).visits) {
            if (cells.length() > 0) {
                cells.append(' ');
            }
            cells.append(visit.column()).append(',').append(visit.row());
        }
        return cells.toString();
    }

    private static LineShape diagonal(int startColumn, int startRow, int endColumn, int endRow) {
        return LineShape.forSquareCells(
                startColumn, startRow, endColumn, endRow, LineStyle.DIAGONAL_STEPS);
    }

    private static LineShape noDiagonal(int startColumn, int startRow, int endColumn,
            int endRow) {
        return LineShape.forSquareCells(
                startColumn, startRow, endColumn, endRow, LineStyle.NO_DIAGONAL_STEPS);
    }

    @Test
    void diagonalStepsHaveExactCells() {
        assertEquals("0,0 1,0 2,0 3,0 4,0", cellsOf(diagonal(0, 0, 4, 0)));
        assertEquals("0,0 1,0 2,0 3,0 4,0", cellsOf(diagonal(4, 0, 0, 0)));
        assertEquals("2,0 2,1 2,2 2,3", cellsOf(diagonal(2, 3, 2, 0)));
        assertEquals("0,0 1,1 2,2 3,3", cellsOf(diagonal(3, 3, 0, 0)));
        assertEquals("3,0 2,1 1,2 0,3", cellsOf(diagonal(3, 0, 0, 3)));
        assertEquals("0,0 1,0 2,0 3,1 4,1", cellsOf(diagonal(0, 0, 4, 1)));
        assertEquals("2,0 3,0 4,0 0,1 1,1", cellsOf(diagonal(4, 0, 0, 1)));
        assertEquals("0,0 0,1 0,2 1,3 1,4", cellsOf(diagonal(0, 0, 1, 4)));
        assertEquals("4,0 4,1 4,2 3,3 3,4", cellsOf(diagonal(3, 4, 4, 0)));
        assertEquals("0,0 1,0 2,1", cellsOf(diagonal(0, 0, 2, 1)));
        assertEquals("0,0 1,0 2,1", cellsOf(diagonal(2, 1, 0, 0)));
        assertEquals("1,0 2,0 0,1", cellsOf(diagonal(0, 1, 2, 0)));
        assertEquals("0,0 0,1 1,2", cellsOf(diagonal(0, 0, 1, 2)));
    }

    @Test
    void noDiagonalStepsHaveExactCells() {
        assertEquals("0,0 1,0 2,0 3,0", cellsOf(noDiagonal(3, 0, 0, 0)));
        assertEquals("1,0 1,1 1,2", cellsOf(noDiagonal(1, 2, 1, 0)));
        assertEquals("0,0 1,0 1,1 2,1 2,2", cellsOf(noDiagonal(0, 0, 2, 2)));
        assertEquals("0,0 0,1 0,2 1,2 1,3", cellsOf(noDiagonal(0, 0, 1, 3)));
        assertEquals("1,0 1,1 0,2 1,2 0,3", cellsOf(noDiagonal(1, 0, 0, 3)));
        assertEquals("3,0 4,0 2,1 3,1 2,2", cellsOf(noDiagonal(2, 2, 4, 0)));
        assertEquals("0,0 1,0 2,0 2,1 3,1 4,1", cellsOf(noDiagonal(0, 0, 4, 1)));
        assertEquals("0,0 0,1 0,2 1,2 1,3 1,4", cellsOf(noDiagonal(1, 4, 0, 0)));
    }

    @Test
    void everyLineInSmallRangeKeepsItsGuarantees() {
        for (int startColumn = -2; startColumn <= 2; startColumn++) {
            for (int startRow = -2; startRow <= 2; startRow++) {
                for (int endColumn = -2; endColumn <= 2; endColumn++) {
                    for (int endRow = -2; endRow <= 2; endRow++) {
                        checkDiagonal(startColumn, startRow, endColumn, endRow);
                        checkNoDiagonal(startColumn, startRow, endColumn, endRow);
                    }
                }
            }
        }
    }

    @Test
    void mirroringLeftToRightMirrorsCellsForEveryLine() {
        for (final LineStyle style : LineStyle.values()) {
            forEachEndpointPair((startColumn, startRow, endColumn, endRow) -> {
                final RecordedVisits original = RecordedVisits.of(
                        LineShape.forSquareCells(startColumn, startRow, endColumn, endRow, style));
                final RecordedVisits mirrored = RecordedVisits.of(
                        LineShape.forSquareCells(-startColumn, startRow, -endColumn, endRow, style));
                assertEquals(original.count(), mirrored.count());
                for (final RecordedVisits.Visit visit : original.visits) {
                    assertTrue(mirrored.contains(-visit.column(), visit.row()));
                }
            });
        }
    }

    @Test
    void transposingFlipsCellsForLinesRunningDownRightOrAxisAligned() {
        for (final LineStyle style : LineStyle.values()) {
            forEachEndpointPair((startColumn, startRow, endColumn, endRow) -> {
                final int columnSpan = endColumn - startColumn;
                final int rowSpan = endRow - startRow;
                final boolean runsDownRightOrAxisAligned = columnSpan * rowSpan >= 0;
                if (!runsDownRightOrAxisAligned || Math.abs(columnSpan) == Math.abs(rowSpan)) {
                    return;
                }
                final RecordedVisits original = RecordedVisits.of(
                        LineShape.forSquareCells(startColumn, startRow, endColumn, endRow, style));
                final RecordedVisits transposed = RecordedVisits.of(
                        LineShape.forSquareCells(startRow, startColumn, endRow, endColumn, style));
                assertEquals(original.count(), transposed.count());
                for (final RecordedVisits.Visit visit : original.visits) {
                    assertTrue(transposed.contains(visit.row(), visit.column()));
                }
            });
        }
    }

    @FunctionalInterface
    private interface EndpointPairVisitor {
        void accept(int startColumn, int startRow, int endColumn, int endRow);
    }

    private static void forEachEndpointPair(EndpointPairVisitor visitor) {
        for (int startColumn = -2; startColumn <= 2; startColumn++) {
            for (int startRow = -2; startRow <= 2; startRow++) {
                for (int endColumn = -2; endColumn <= 2; endColumn++) {
                    for (int endRow = -2; endRow <= 2; endRow++) {
                        visitor.accept(startColumn, startRow, endColumn, endRow);
                    }
                }
            }
        }
    }

    private static void checkCommon(LineShape shape, LineShape swapped, RecordedVisits visits) {
        assertEquals(cellsOf(shape), cellsOf(swapped));
        assertTrue(visits.isInRowMajorOrder());
        assertTrue(visits.hasNoDuplicates());
        assertTrue(visits.allFractionsInUnitRange());
        assertTrue(visits.contains(shape.getStartColumn(), shape.getStartRow()));
        assertTrue(visits.contains(shape.getEndColumn(), shape.getEndRow()));
        for (int row = shape.getTop(); row <= shape.getBottom(); row++) {
            final int count = visits.countInRow(row);
            assertTrue(count > 0);
            assertEquals(count, visits.maxColumnInRow(row) - visits.minColumnInRow(row) + 1);
        }
    }

    private static void checkDiagonal(int startColumn, int startRow, int endColumn, int endRow) {
        final LineShape shape = diagonal(startColumn, startRow, endColumn, endRow);
        final RecordedVisits visits = RecordedVisits.of(shape);
        checkCommon(shape, diagonal(endColumn, endRow, startColumn, startRow), visits);
        final int expectedCount =
                Math.max(Math.abs(endColumn - startColumn), Math.abs(endRow - startRow)) + 1;
        assertEquals(expectedCount, visits.count());
    }

    private static void checkNoDiagonal(int startColumn, int startRow, int endColumn,
            int endRow) {
        final LineShape shape = noDiagonal(startColumn, startRow, endColumn, endRow);
        final RecordedVisits visits = RecordedVisits.of(shape);
        checkCommon(shape, noDiagonal(endColumn, endRow, startColumn, startRow), visits);
        final int expectedCount =
                Math.abs(endColumn - startColumn) + Math.abs(endRow - startRow) + 1;
        assertEquals(expectedCount, visits.count());
        for (final RecordedVisits.Visit first : visits.visits) {
            for (final RecordedVisits.Visit second : visits.visits) {
                final boolean touchesAtCorner = Math.abs(first.column() - second.column()) == 1
                        && Math.abs(first.row() - second.row()) == 1;
                if (touchesAtCorner) {
                    assertTrue(visits.contains(first.column(), second.row())
                            || visits.contains(second.column(), first.row()));
                }
            }
        }
    }

    @Test
    void boundsCoverBothEndpoints() {
        final LineShape shape = diagonal(5, -2, -1, 3);

        assertEquals(-1, shape.getLeft());
        assertEquals(-2, shape.getTop());
        assertEquals(5, shape.getRight());
        assertEquals(3, shape.getBottom());
        assertEquals(LineStyle.DIAGONAL_STEPS, shape.getStyle());
    }

    @Test
    void distancesAreMeasuredFromTheMidpoint() {
        final RecordedVisits straight = RecordedVisits.of(diagonal(0, 0, 4, 0));
        final float[] expectedDistances = {2f, 1f, 0f, 1f, 2f};
        final float[] expectedFractions = {1f, 0.5f, 0f, 0.5f, 1f};
        for (int i = 0; i < 5; i++) {
            assertEquals(expectedDistances[i], straight.get(i).distance(), 1e-6f);
            assertEquals(expectedFractions[i], straight.get(i).distanceFraction(), 1e-6f);
        }

        final RecordedVisits slanted = RecordedVisits.of(noDiagonal(0, 0, 2, 2));
        assertEquals((float) Math.sqrt(2), slanted.get(0).distance(), 1e-6f);
        assertEquals(1f, slanted.get(0).distanceFraction(), 1e-6f);
        assertEquals(0f, slanted.at(1, 1).distance(), 1e-6f);
    }

    @Test
    void aspectScalesRowDistances() {
        final LineShape shape = LineShape.forCellAspect(
                0, 0, 0, 2, LineStyle.DIAGONAL_STEPS, 2f);
        final RecordedVisits visits = RecordedVisits.of(shape);

        assertEquals(2f, visits.get(0).distance(), 1e-6f);
        assertEquals(0f, visits.get(1).distance(), 1e-6f);
        assertEquals(2f, visits.get(2).distance(), 1e-6f);
        assertEquals(1f, visits.get(0).distanceFraction(), 1e-6f);
    }

    @Test
    void singleCellLineHasZeroDistance() {
        for (final LineStyle style : LineStyle.values()) {
            final RecordedVisits visits =
                    RecordedVisits.of(LineShape.forSquareCells(3, -4, 3, -4, style));

            assertEquals(1, visits.count());
            assertEquals(3, visits.get(0).column());
            assertEquals(-4, visits.get(0).row());
            assertEquals(0f, visits.get(0).distance(), 0f);
            assertEquals(0f, visits.get(0).distanceFraction(), 0f);
        }
    }

    @Test
    void setEndpointsUpdatesCellsAndBounds() {
        final LineShape shape = diagonal(0, 0, 1, 0);
        shape.setEndpoints(-3, 4, 0, 4);

        assertEquals("-3,4 -2,4 -1,4 0,4", cellsOf(shape));
        assertEquals(-3, shape.getLeft());
        assertEquals(4, shape.getTop());
        assertEquals(0, shape.getRight());
        assertEquals(4, shape.getBottom());
        assertEquals(-3, shape.getStartColumn());
        assertEquals(4, shape.getStartRow());
        assertEquals(0, shape.getEndColumn());
        assertEquals(4, shape.getEndRow());
    }

    @Test
    void maxValueCoordinatesAreRejectedAndLeaveShapeUnchanged() {
        for (final LineStyle style : LineStyle.values()) {
            assertThrows(IllegalArgumentException.class,
                    () -> LineShape.forSquareCells(Integer.MAX_VALUE, 0, 0, 0, style));
            assertThrows(IllegalArgumentException.class,
                    () -> LineShape.forSquareCells(0, Integer.MAX_VALUE, 0, 0, style));
            assertThrows(IllegalArgumentException.class,
                    () -> LineShape.forSquareCells(0, 0, Integer.MAX_VALUE, 0, style));
            assertThrows(IllegalArgumentException.class,
                    () -> LineShape.forSquareCells(0, 0, 0, Integer.MAX_VALUE, style));
        }
        final LineShape shape = diagonal(0, 0, 2, 0);
        assertThrows(IllegalArgumentException.class,
                () -> shape.setEndpoints(1, 1, Integer.MAX_VALUE, 1));

        assertEquals("0,0 1,0 2,0", cellsOf(shape));
        assertEquals(2, shape.getRight());
        assertEquals(0, shape.getBottom());
    }

    @Test
    void extremeCoordinatesGiveCorrectBounds() {
        final LineShape shape = diagonal(Integer.MIN_VALUE, 0, Integer.MAX_VALUE - 1, 0);

        assertEquals(Integer.MIN_VALUE, shape.getLeft());
        assertEquals(Integer.MAX_VALUE - 1, shape.getRight());
    }

    @Test
    void invalidArgumentsThrowForAnyAngleLines() {
        assertThrows(NullPointerException.class,
                () -> LineShape.forSquareCells(0, 0, 1, 1, null));
        assertThrows(NullPointerException.class,
                () -> LineShape.forCellAspect(0, 0, 1, 1, null, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> LineShape.forCellAspect(0, 0, 1, 1, LineStyle.DIAGONAL_STEPS, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> LineShape.forCellAspect(0, 0, 1, 1, LineStyle.DIAGONAL_STEPS, Float.NaN));
    }
}
