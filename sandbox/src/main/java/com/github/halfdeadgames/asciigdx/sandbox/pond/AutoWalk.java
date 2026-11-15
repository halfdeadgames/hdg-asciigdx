package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** A closed walking route replayed forever at a fixed step interval. */
public final class AutoWalk {

    /** Seconds between two steps. */
    public static final float STEP_SECONDS = 0.3f;

    private static final double STEP_MILLIS = 300.0;
    /** Absorbs float error so a time at an exact multiple of the interval counts its step. */
    private static final double BOUNDARY_TOLERANCE_MILLIS = 0.5;

    private final String route;

    /**
     * Creates the exploring loop for an 80x44 map started at (40, 22). It wanders on diagonal headings
     * and swings near the top-left corner so both camera axes stop, without running along a wall.
     */
    public AutoWalk() {
        this(exploringLoopRoute());
    }

    /** Creates a walk over a route of 'L', 'R', 'U', 'D', and 'W' (wait) steps that must return to its start. */
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

    private static String exploringLoopRoute() {
        return WaypointRoute.from(40, 22)
                .to(30, 17)
                .to(16, 9)
                .to(8, 5)
                .pause(2)
                .to(12, 14)
                .to(22, 26)
                .to(38, 31)
                .to(54, 30)
                .to(56, 24)
                .to(48, 16)
                .back()
                .route();
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
            case 'U', 'D', 'W' -> 0;
            default -> throw new IllegalArgumentException("unknown step: " + step);
        };
    }

    private static int rowDelta(char step) {
        return switch (step) {
            case 'U' -> -1;
            case 'D' -> 1;
            case 'L', 'R', 'W' -> 0;
            default -> throw new IllegalArgumentException("unknown step: " + step);
        };
    }
}
