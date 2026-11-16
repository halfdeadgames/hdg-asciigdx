package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoWalkDefaultRouteTest {

    private static final int START_COLUMN = 40;
    private static final int START_ROW = 22;
    private static final int MAP_COLUMNS = 80;
    private static final int MAP_ROWS = 44;
    private static final int EDGE_ZONE = 10;
    private static final int MAX_STRAIGHT_RUN_IN_EDGE_ZONE = 3;

    private final AutoWalk walk = new AutoWalk();
    private final CellPoint direction = new CellPoint();

    private static boolean isInEdgeZone(int column, int row) {
        return column < EDGE_ZONE
                || row < EDGE_ZONE
                || column >= MAP_COLUMNS - EDGE_ZONE
                || row >= MAP_ROWS - EDGE_ZONE;
    }

    private void readDirectionOfStep(int step) {
        walk.stepDirectionAt(step * AutoWalk.STEP_SECONDS, direction);
    }

    @Test
    void routeStaysAtLeastEightColumnsAndFiveRowsFromTheMapEdges() {
        int column = START_COLUMN;
        int row = START_ROW;
        for (int step = 0; step < walk.routeLength(); step++) {
            readDirectionOfStep(step);
            column += direction.getColumn();
            row += direction.getRow();
            assertTrue(column >= 8 && column <= MAP_COLUMNS - 9,
                    "column " + column + " at step " + step);
            assertTrue(row >= 5 && row <= MAP_ROWS - 6, "row " + row + " at step " + step);
        }
    }

    @Test
    void routeReachesACellWhereBothCameraAxesStop() {
        int column = START_COLUMN;
        int row = START_ROW;
        boolean reached = false;
        for (int step = 0; step < walk.routeLength(); step++) {
            readDirectionOfStep(step);
            column += direction.getColumn();
            row += direction.getRow();
            reached |= column < 20 && row < 11;
        }
        assertTrue(reached, "no cell with column < 20 and row < 11");
    }

    @Test
    void routeNeverWalksMoreThanThreeStraightStepsWithinTenCellsOfAnEdge() {
        int column = START_COLUMN;
        int row = START_ROW;
        int straightRun = 0;
        int previousColumnChange = 0;
        int previousRowChange = 0;
        for (int step = 0; step < walk.routeLength(); step++) {
            readDirectionOfStep(step);
            boolean continuesRun = direction.getColumn() == previousColumnChange
                    && direction.getRow() == previousRowChange;
            straightRun = continuesRun ? straightRun + 1 : 1;
            if (isInEdgeZone(column, row)) {
                assertTrue(straightRun <= MAX_STRAIGHT_RUN_IN_EDGE_ZONE,
                        "straight run of " + straightRun + " near an edge at step " + step);
            }
            previousColumnChange = direction.getColumn();
            previousRowChange = direction.getRow();
            column += direction.getColumn();
            row += direction.getRow();
        }
    }

    @Test
    void routeReturnsToTheStartAfterOneLoop() {
        int column = START_COLUMN;
        int row = START_ROW;
        for (int step = 0; step < walk.routeLength(); step++) {
            readDirectionOfStep(step);
            column += direction.getColumn();
            row += direction.getRow();
        }
        assertEquals(START_COLUMN, column);
        assertEquals(START_ROW, row);
    }
}
