package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** A closed walking route replayed forever at a fixed step interval. */
public final class AutoWalk {

    /** Seconds between two steps. */
    public static final float STEP_SECONDS = 0.3f;

    private static final double STEP_MILLIS = 300.0;
    /** Absorbs float error so a time at an exact multiple of the interval counts its step. */
    private static final double BOUNDARY_TOLERANCE_MILLIS = 0.5;

    private final String route;

    /** Creates the route for an 80x44 map started at (40, 22): left edge, top-left corner, and back. */
    public AutoWalk() {
        this(edgeTourRoute());
    }

    /** Creates a walk over a route of 'L', 'R', 'U', and 'D' steps that must return to its start. */
    public AutoWalk(String route) {
        if (route.isEmpty()) {
            throw new IllegalArgumentException("route must not be empty");
        }
        if (!isClosed(route)) {
            throw new IllegalArgumentException("route must return to its start: " + route);
        }
        this.route = route;
    }

    /** Returns the number of steps in one loop. */
    public int routeLength() {
        return route.length();
    }

    /** Returns how many steps have been taken after the given time, counting across loops. */
    public int stepCountAt(float seconds) {
        if (seconds < 0f) {
            throw new IllegalArgumentException("seconds must not be negative: " + seconds);
        }
        return (int) Math.floor((seconds * 1000.0 + BOUNDARY_TOLERANCE_MILLIS) / STEP_MILLIS);
    }

    /**
     * Writes the logical cell at the given time into {@code out}. Step k runs from k * interval to
     * (k + 1) * interval, and the logical cell during step k is the position after k + 1 steps, so
     * the cell changes at the start of a step and the display slides the glyph in afterward.
     */
    public void positionAt(float seconds, int startColumn, int startRow, CellPoint out) {
        int stepsIntoLoop = (stepCountAt(seconds) + 1) % route.length();
        int column = startColumn;
        int row = startRow;
        for (int i = 0; i < stepsIntoLoop; i++) {
            column += columnDelta(route.charAt(i));
            row += rowDelta(route.charAt(i));
        }
        out.set(column, row);
    }

    /** Writes the column and row change of the step in progress at the given time into {@code out}. */
    public void stepDirectionAt(float seconds, CellPoint out) {
        char step = route.charAt(stepCountAt(seconds) % route.length());
        out.set(columnDelta(step), rowDelta(step));
    }

    /** Returns how far the step in progress has run, from 0 at its start to 1 at its end. */
    public float stepProgressAt(float seconds) {
        double stepStartMillis = stepCountAt(seconds) * STEP_MILLIS;
        double progress = (seconds * 1000.0 - stepStartMillis) / STEP_MILLIS;
        return (float) Math.max(0.0, Math.min(1.0, progress));
    }

    private static String edgeTourRoute() {
        return repeated('L', 40) + repeated('U', 22) + repeated('R', 40) + repeated('D', 22);
    }

    private static String repeated(char step, int count) {
        return String.valueOf(step).repeat(count);
    }

    private static boolean isClosed(String route) {
        int column = 0;
        int row = 0;
        for (int i = 0; i < route.length(); i++) {
            column += columnDelta(route.charAt(i));
            row += rowDelta(route.charAt(i));
        }
        return column == 0 && row == 0;
    }

    private static int columnDelta(char step) {
        return switch (step) {
            case 'L' -> -1;
            case 'R' -> 1;
            case 'U', 'D' -> 0;
            default -> throw new IllegalArgumentException("unknown step: " + step);
        };
    }

    private static int rowDelta(char step) {
        return switch (step) {
            case 'U' -> -1;
            case 'D' -> 1;
            case 'L', 'R' -> 0;
            default -> throw new IllegalArgumentException("unknown step: " + step);
        };
    }
}
