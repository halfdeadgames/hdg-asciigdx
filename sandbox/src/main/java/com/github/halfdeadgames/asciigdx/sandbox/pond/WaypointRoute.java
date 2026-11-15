package com.github.halfdeadgames.asciigdx.sandbox.pond;

/**
 * Builds a closed walking route of 'L', 'R', 'U', 'D', and 'W' (wait) steps through a list of waypoints.
 * Each leg spreads its horizontal and vertical steps evenly, so a diagonal leg reads as a steady heading
 * instead of a staircase of long runs. The route must end at its start: finish the chain with
 * {@link #back()} before calling {@link #route()}.
 */
public final class WaypointRoute {

    private final int startColumn;
    private final int startRow;
    private final StringBuilder steps = new StringBuilder();
    private int column;
    private int row;

    private WaypointRoute(int startColumn, int startRow) {
        this.startColumn = startColumn;
        this.startRow = startRow;
        this.column = startColumn;
        this.row = startRow;
    }

    /** Starts a route at the given cell. */
    public static WaypointRoute from(int startColumn, int startRow) {
        return new WaypointRoute(startColumn, startRow);
    }

    /** Walks to the given cell; a leg to the current cell adds nothing. */
    public WaypointRoute to(int targetColumn, int targetRow) {
        appendLeg(targetColumn - column, targetRow - row);
        column = targetColumn;
        row = targetRow;
        return this;
    }

    /** Waits the given number of steps at the current cell. */
    public WaypointRoute pause(int waitSteps) {
        if (waitSteps < 0) {
            throw new IllegalArgumentException("waitSteps must not be negative: " + waitSteps);
        }
        steps.append("W".repeat(waitSteps));
        return this;
    }

    /** Walks back to the start cell, closing the route. */
    public WaypointRoute back() {
        return to(startColumn, startRow);
    }

    /** Returns the step string; fails if the route does not end at its start. */
    public String route() {
        if (column != startColumn || row != startRow) {
            throw new IllegalStateException("route must end at its start; call back() first");
        }
        return steps.toString();
    }

    /**
     * Each step along the longer axis is followed by a step along the shorter axis whenever the
     * shorter axis' share of the distance covered so far has grown by a whole cell.
     */
    private void appendLeg(int columnChange, int rowChange) {
        boolean horizontalIsMajor = Math.abs(columnChange) >= Math.abs(rowChange);
        int majorCount = horizontalIsMajor ? Math.abs(columnChange) : Math.abs(rowChange);
        int minorCount = horizontalIsMajor ? Math.abs(rowChange) : Math.abs(columnChange);
        char majorStep = horizontalIsMajor ? horizontalStep(columnChange) : verticalStep(rowChange);
        char minorStep = horizontalIsMajor ? verticalStep(rowChange) : horizontalStep(columnChange);
        int minorDone = 0;
        for (int majorDone = 1; majorDone <= majorCount; majorDone++) {
            steps.append(majorStep);
            int minorTarget = (2 * majorDone * minorCount + majorCount) / (2 * majorCount);
            if (minorTarget > minorDone) {
                steps.append(minorStep);
                minorDone = minorTarget;
            }
        }
    }

    private static char horizontalStep(int columnChange) {
        return columnChange < 0 ? 'L' : 'R';
    }

    private static char verticalStep(int rowChange) {
        return rowChange < 0 ? 'U' : 'D';
    }
}
