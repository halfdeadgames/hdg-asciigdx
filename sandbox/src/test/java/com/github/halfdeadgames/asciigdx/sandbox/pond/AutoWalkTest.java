package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoWalkTest {

    private static final int START_COLUMN = 40;
    private static final int START_ROW = 22;
    /** Steps 0 to 6: left, left, up, wait, right, right, down. */
    private static final String ROUTE = "LLUWRRD";

    private final AutoWalk walk = new AutoWalk(ROUTE);
    private final CellPoint point = new CellPoint();

    /**
     * The logical cell during step k is the position after k + 1 steps, so sample at the start of
     * step n - 1.
     */
    private void assertAtStep(int stepsTaken, int column, int row) {
        walk.positionAt((stepsTaken - 1) * AutoWalk.STEP_SECONDS, START_COLUMN, START_ROW, point);
        assertEquals(column, point.getColumn(), "column after " + stepsTaken + " steps");
        assertEquals(row, point.getRow(), "row after " + stepsTaken + " steps");
    }

    private void assertDirectionAtStep(int step, int columnChange, int rowChange) {
        walk.stepDirectionAt(startOfStep(step) + 0.1f, point);
        assertEquals(columnChange, point.getColumn(), "column change of step " + step);
        assertEquals(rowChange, point.getRow(), "row change of step " + step);
    }

    private static float startOfStep(int step) {
        return step * AutoWalk.STEP_SECONDS;
    }

    @Test
    void logicalCellChangesAtStartOfStepAndHoldsDuringIt() {
        walk.positionAt(0f, START_COLUMN, START_ROW, point);
        assertEquals(39, point.getColumn());
        walk.positionAt(startOfStep(0) + 0.15f, START_COLUMN, START_ROW, point);
        assertEquals(39, point.getColumn());
        walk.positionAt(startOfStep(1) - 0.01f, START_COLUMN, START_ROW, point);
        assertEquals(39, point.getColumn());
        walk.positionAt(startOfStep(1), START_COLUMN, START_ROW, point);
        assertEquals(38, point.getColumn());
        assertEquals(22, point.getRow());
    }

    @Test
    void directionFollowsTheRoute() {
        assertDirectionAtStep(0, -1, 0);
        assertDirectionAtStep(2, 0, -1);
        assertDirectionAtStep(4, 1, 0);
        assertDirectionAtStep(6, 0, 1);
        assertDirectionAtStep(7, -1, 0);
    }

    @Test
    void waitStepKeepsThePositionAndReportsNoDirection() {
        assertAtStep(3, 38, 21);
        assertAtStep(4, 38, 21);
        assertDirectionAtStep(3, 0, 0);
    }

    @Test
    void progressRunsFromZeroToJustBelowOneAcrossAStep() {
        assertEquals(0f, walk.stepProgressAt(0f), 1e-4f);
        assertEquals(0.5f, walk.stepProgressAt(0.15f), 1e-3f);
        assertEquals(0.5f, walk.stepProgressAt(startOfStep(7) + 0.15f), 1e-3f);
        assertEquals(0.97f, walk.stepProgressAt(startOfStep(1) - 0.009f), 1e-2f);
        assertEquals(0f, walk.stepProgressAt(startOfStep(5)), 1e-3f);
    }

    @Test
    void progressStaysInsideZeroToOneAtBoundaries() {
        for (int step = 0; step < 300; step++) {
            float progress = walk.stepProgressAt(startOfStep(step));
            assertTrue(progress >= 0f && progress <= 1f, "progress " + progress);
            progress = walk.stepProgressAt(startOfStep(step) - 0.0004f * (step > 0 ? 1 : 0));
            assertTrue(progress >= 0f && progress <= 1f, "progress " + progress);
        }
    }

    @Test
    void routeIsClosedOverTheLastStepOfALoop() {
        walk.positionAt(startOfStep(6), START_COLUMN, START_ROW, point);
        assertEquals(START_COLUMN, point.getColumn());
        assertEquals(START_ROW, point.getRow());
    }

    @Test
    void routeLengthCountsEveryStepIncludingWaits() {
        assertEquals(ROUTE.length(), walk.routeLength());
    }

    @Test
    void stepCountChangesAtMultiplesOfInterval() {
        assertEquals(0, walk.stepCountAt(0f));
        assertEquals(0, walk.stepCountAt(0.29f));
        assertEquals(1, walk.stepCountAt(AutoWalk.STEP_SECONDS));
        assertEquals(2, walk.stepCountAt(2 * AutoWalk.STEP_SECONDS));
        assertEquals(3, walk.stepCountAt(3 * AutoWalk.STEP_SECONDS));
        assertEquals(2, walk.stepCountAt(3 * AutoWalk.STEP_SECONDS - 0.01f));
        assertEquals(1000, walk.stepCountAt(1000 * AutoWalk.STEP_SECONDS));
    }

    @Test
    void positionsAtChosenSteps() {
        assertAtStep(1, 39, 22);
        assertAtStep(2, 38, 22);
        assertAtStep(3, 38, 21);
        assertAtStep(5, 39, 21);
    }

    @Test
    void routeIsClosedAndLoopsForever() {
        assertAtStep(7, START_COLUMN, START_ROW);
        assertAtStep(7 * 5, START_COLUMN, START_ROW);
        assertAtStep(7 * 3 + 3, 38, 21);
    }

    @Test
    void rejectsBadRoutes() {
        assertThrows(IllegalArgumentException.class, () -> new AutoWalk(""));
        assertThrows(IllegalArgumentException.class, () -> new AutoWalk("LL"));
        assertThrows(IllegalArgumentException.class, () -> new AutoWalk("LXRX"));
    }

    @Test
    void rejectsNegativeTime() {
        assertThrows(IllegalArgumentException.class, () -> walk.stepCountAt(-1f));
    }
}
