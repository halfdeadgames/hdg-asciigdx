package com.github.halfdeadgames.asciigdx.workbench.derive;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.InkBounds;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.ui.Blink;
import java.util.Optional;

/**
 * Judges the overlay part being picked and words the warning for it. It holds no drawing or input
 * code.
 */
public final class OverlayCheck {

    public enum Problem { NONE, SAME_AS_TARGET, EMPTY, OUTSIDE }

    /**
     * @param problem what is wrong with the pending part, or NONE
     * @param message the warning, or empty when there is none
     * @param hasRange whether {@code minShift} and {@code maxShift} are meaningful (the source has
     *     ink)
     * @param minShift the most negative shift that keeps all ink inside
     * @param maxShift the most positive shift that keeps all ink inside
     * @param minDown the most negative down that keeps all ink inside (meaningful when
     *     {@code hasRange})
     * @param maxDown the most positive down that keeps all ink inside (meaningful when
     *     {@code hasRange})
     */
    public record Result(Problem problem, String message, boolean hasRange, int minShift,
                         int maxShift,
                         int minDown, int maxDown) {

        /** Creates a result whose down range is empty (0..0). */
        public Result(Problem problem, String message, boolean hasRange, int minShift,
                int maxShift) {
            this(problem, message, hasRange, minShift, maxShift, 0, 0);
        }

        public boolean hasWarning() {
            return problem != Problem.NONE;
        }

        /** Whether finishing is refused too: the pending part would make the file fail to load. */
        public boolean blocksFinish() {
            return problem == Problem.OUTSIDE;
        }

        /** The allowed shifts as text, for example {@code -2..+3}. */
        public String rangeText() {
            return signed(minShift) + ".." + signed(maxShift);
        }

        /** The allowed down values as text, for example {@code -1..+2}. */
        public String downRangeText() {
            return signed(minDown) + ".." + signed(maxDown);
        }
    }

    public static final String SAME_AS_TARGET_MESSAGE = "PICK A DIFFERENT GLYPH";
    static final String EMPTY_MESSAGE = "SOURCE SLOT IS EMPTY: DRAW IT FIRST";

    private OverlayCheck() {
    }

    /**
     * @param sourceIsTarget whether the source cursor is on the glyph being derived
     * @param source the source glyph, or null when its slot is empty
     * @param shift the pending shift
     * @param down the pending down (negative is up)
     */
    public static Result check(boolean sourceIsTarget, PixelGlyph source, int shift, int down) {
        if (sourceIsTarget) {
            return new Result(Problem.SAME_AS_TARGET, SAME_AS_TARGET_MESSAGE, false, 0, 0);
        }
        if (source == null) {
            return new Result(Problem.EMPTY, EMPTY_MESSAGE, false, 0, 0);
        }
        PixelGrid pixels = source.pixels();
        Optional<InkBounds> bounds = InkBounds.measure(pixels);
        boolean hasRange = bounds.isPresent();
        int min = hasRange ? -bounds.get().left() : 0;
        int max = hasRange ? bounds.get().right() : 0;
        int minDown = hasRange ? -bounds.get().top() : 0;
        int maxDown = hasRange ? bounds.get().bottom() : 0;
        if (hasRange && (shift < min || shift > max)) {
            return outside("SHIFT", shift, min, max, min, max, minDown, maxDown);
        }
        if (hasRange && (down < minDown || down > maxDown)) {
            return outside("DOWN", down, minDown, maxDown, min, max, minDown, maxDown);
        }
        return new Result(Problem.NONE, "", hasRange, min, max, minDown, maxDown);
    }

    private static Result outside(String axis, int value, int allowedMin, int allowedMax,
            int minShift, int maxShift, int minDown, int maxDown) {
        String message = axis + " " + signed(value) + " PUTS INK OUTSIDE THE CELL: ALLOWED "
                + signed(allowedMin) + ".." + signed(allowedMax);
        return new Result(Problem.OUTSIDE, message, true, minShift, maxShift, minDown, maxDown);
    }

    public enum OffGrid { NONE, LEFT, RIGHT, UP, DOWN }

    /**
     * Which edge all moved ink lies beyond vertically, or NONE when some ink is still inside the
     * grid.
     *
     * @param firstInkRow topmost row holding ink
     * @param lastInkRow bottommost row holding ink
     * @param down rows the ink moves down (negative moves up)
     * @param gridHeight height of the visible grid
     */
    public static OffGrid offGridRows(int firstInkRow, int lastInkRow, int down, int gridHeight) {
        if (lastInkRow + down < 0) {
            return OffGrid.UP;
        }
        if (firstInkRow + down >= gridHeight) {
            return OffGrid.DOWN;
        }
        return OffGrid.NONE;
    }

    /**
     * Which side of the grid all shifted ink lies beyond, or NONE when some ink is still inside it.
     *
     * @param firstInkColumn leftmost column holding ink
     * @param lastInkColumn rightmost column holding ink
     * @param shift columns the ink moves right (negative moves left)
     * @param gridWidth width of the visible grid
     */
    public static OffGrid offGrid(int firstInkColumn, int lastInkColumn, int shift, int gridWidth) {
        if (lastInkColumn + shift < 0) {
            return OffGrid.LEFT;
        }
        if (firstInkColumn + shift >= gridWidth) {
            return OffGrid.RIGHT;
        }
        return OffGrid.NONE;
    }

    /** Whether a blinking marker is lit at the given time. */
    public static boolean blinkOn(float seconds) {
        return Blink.isOnAt(seconds);
    }

    /** The value with a leading + when positive, for example +3, 0, -2. */
    public static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }
}
