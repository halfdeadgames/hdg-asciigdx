package com.github.halfdeadgames.asciigdx.text;

/**
 * Describes how one line of text that is wider than its area moves over time. Instances are
 * immutable and hold no playback state: the offset for any moment is computed from the seconds
 * since the scroll started, so the same instance can drive any number of lines.
 *
 * <p>Two motions exist, each with its own factory:
 * <ul>
 *   <li>{@link #backAndForth(float, float)} pauses at the start, moves to the end of the text,
 *       pauses, and moves back, repeating forever.</li>
 *   <li>{@link #loop(float, float, int)} pauses at the start, then moves the text left until it
 *       wraps around after a gap of blank cells, repeating forever.</li>
 * </ul>
 *
 * <p>This class is an immutable value compared with {@code equals}; it has no public getters.
 *
 * <p>All time is in seconds supplied by the game, never frames. Offsets are whole cells, so text
 * moves one cell per step and never jumps.
 *
 * <p>Example: {@code TextScroll scroll = TextScroll.backAndForth(6f, 1f);}
 */
public final class TextScroll {

    private final boolean looping;
    private final float cellsPerSecond;
    private final float pauseSeconds;
    private final int gapCells;

    private TextScroll(boolean looping, float cellsPerSecond, float pauseSeconds, int gapCells) {
        this.looping = looping;
        this.cellsPerSecond = cellsPerSecond;
        this.pauseSeconds = pauseSeconds;
        this.gapCells = gapCells;
    }

    /**
     * Creates a scroll that moves to the end of the text and back, pausing at both ends.
     *
     * <p>One cycle is: pause at offset 0, move forward, pause at the last offset, move back. The
     * cycle repeats forever.
     *
     * @param cellsPerSecond speed in cells per second; must be finite and greater than 0
     * @param endPauseSeconds pause at each end, in seconds; must be finite and at least 0
     * @return the scroll; never {@code null}
     * @throws IllegalArgumentException if a value is outside its valid range
     */
    public static TextScroll backAndForth(float cellsPerSecond, float endPauseSeconds) {
        requireValid(cellsPerSecond, endPauseSeconds, 0);
        return new TextScroll(false, cellsPerSecond, endPauseSeconds, 0);
    }

    /**
     * Creates a scroll that moves the text left forever. The text is followed by blank cells and
     * then repeats, so it wraps around.
     *
     * <p>Each lap starts with a pause at offset 0, then moves one full strip (text plus gap).
     *
     * @param cellsPerSecond speed in cells per second; must be finite and greater than 0
     * @param lapPauseSeconds pause at the start of each lap, in seconds; must be finite and at
     *         least 0
     * @param gapCells blank cells between the end of the text and its repeated start; at least 0
     *         (0 lets the text run directly into its own start)
     * @return the scroll; never {@code null}
     * @throws IllegalArgumentException if a value is outside its valid range
     */
    public static TextScroll loop(float cellsPerSecond, float lapPauseSeconds, int gapCells) {
        requireValid(cellsPerSecond, lapPauseSeconds, gapCells);
        return new TextScroll(true, cellsPerSecond, lapPauseSeconds, gapCells);
    }

    /**
     * Returns how many cells the text has moved left at a given moment.
     *
     * <p>Returns 0 when the text fits ({@code textCells <= widthCells}). Returns 0 when
     * {@code secondsSinceStart} is NaN, infinite, or negative. For back-and-forth the result is in
     * {@code [0, textCells - widthCells]}. For loop it is in {@code [0, textCells + gapCells)}.
     * This is a pure function: it reads and keeps no state.
     *
     * @param textCells text length in cells (code points); at least 0
     * @param widthCells width of the area in cells; at least 0
     * @param secondsSinceStart seconds since this text was first shown in this area; the caller
     *         owns that clock and resets it to 0 when the text or width changes. Very large
     *         values lose float precision, so passing total game uptime makes the steps uneven
     *         after long runs. Any value is accepted
     * @return the offset in cells
     * @throws IllegalArgumentException if {@code textCells} or {@code widthCells} is negative
     */
    public int getOffsetCells(int textCells, int widthCells, float secondsSinceStart) {
        if (textCells < 0) {
            throw new IllegalArgumentException("textCells must be at least 0: " + textCells);
        }
        if (widthCells < 0) {
            throw new IllegalArgumentException("widthCells must be at least 0: " + widthCells);
        }
        if (textCells <= widthCells || Float.isNaN(secondsSinceStart)
                || Float.isInfinite(secondsSinceStart) || secondsSinceStart < 0f) {
            return 0;
        }
        if (looping) {
            return loopOffset((long) textCells + gapCells, secondsSinceStart);
        }
        return backAndForthOffset(textCells - widthCells, secondsSinceStart);
    }

    boolean isLooping() {
        return looping;
    }

    int getGapCells() {
        return gapCells;
    }

    private int backAndForthOffset(int overflowCells, double seconds) {
        double speed = cellsPerSecond;
        double travelSeconds = overflowCells / speed;
        double cycleSeconds = 2.0 * pauseSeconds + 2.0 * travelSeconds;
        double elapsed = seconds % cycleSeconds;
        if (elapsed < pauseSeconds) {
            return 0;
        }
        elapsed -= pauseSeconds;
        if (elapsed < travelSeconds) {
            return (int) Math.min(overflowCells, Math.floor(elapsed * speed));
        }
        elapsed -= travelSeconds;
        if (elapsed < pauseSeconds) {
            return overflowCells;
        }
        elapsed -= pauseSeconds;
        return (int) Math.max(0, overflowCells - Math.floor(elapsed * speed));
    }

    private int loopOffset(long stripCells, double seconds) {
        double speed = cellsPerSecond;
        double lapSeconds = pauseSeconds + stripCells / speed;
        double elapsed = seconds % lapSeconds;
        if (elapsed < pauseSeconds) {
            return 0;
        }
        double travelled = Math.floor((elapsed - pauseSeconds) * speed);
        return (int) Math.min(Math.min(stripCells - 1, Integer.MAX_VALUE), travelled);
    }

    private static void requireValid(float cellsPerSecond, float pauseSeconds, int gapCells) {
        if (!(cellsPerSecond > 0f) || Float.isInfinite(cellsPerSecond)) {
            throw new IllegalArgumentException(
                    "cellsPerSecond must be finite and greater than 0: " + cellsPerSecond);
        }
        if (!(pauseSeconds >= 0f) || Float.isInfinite(pauseSeconds)) {
            throw new IllegalArgumentException(
                    "pause seconds must be finite and at least 0: " + pauseSeconds);
        }
        if (gapCells < 0) {
            throw new IllegalArgumentException("gapCells must be at least 0: " + gapCells);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TextScroll)) {
            return false;
        }
        TextScroll that = (TextScroll) other;
        return looping == that.looping
                && Float.compare(cellsPerSecond, that.cellsPerSecond) == 0
                && Float.compare(pauseSeconds, that.pauseSeconds) == 0
                && gapCells == that.gapCells;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(looping);
        result = 31 * result + Float.hashCode(cellsPerSecond);
        result = 31 * result + Float.hashCode(pauseSeconds);
        result = 31 * result + gapCells;
        return result;
    }

    @Override
    public String toString() {
        if (looping) {
            return "TextScroll.loop(" + cellsPerSecond + ", " + pauseSeconds + ", " + gapCells
                    + ")";
        }
        return "TextScroll.backAndForth(" + cellsPerSecond + ", " + pauseSeconds + ")";
    }
}
