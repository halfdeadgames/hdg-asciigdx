package com.github.halfdeadgames.asciigdx.workbench.derive;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;

/** Judges the overlay part being picked and words the warning for it. It holds no drawing or input code. */
public final class OverlayCheck {

    public enum Problem { NONE, SAME_AS_TARGET, EMPTY, MIXED, OUTSIDE }

    /**
     * @param problem what is wrong with the pending part, or NONE
     * @param message the warning, or empty when there is none
     * @param hasRange whether {@code minShift} and {@code maxShift} are meaningful (the source has ink)
     * @param minShift the most negative shift that keeps all ink inside
     * @param maxShift the most positive shift that keeps all ink inside
     */
    public record Result(Problem problem, String message, boolean hasRange, int minShift, int maxShift) {

        public boolean hasWarning() {
            return problem != Problem.NONE;
        }

        /** Whether the part cannot be added. */
        public boolean blocksAdd() {
            return problem != Problem.NONE;
        }

        /** Whether finishing is refused too: the pending part would make the file fail to load. */
        public boolean blocksFinish() {
            return problem == Problem.MIXED || problem == Problem.OUTSIDE;
        }

        /** The allowed shifts as text, for example {@code -2..+3}. */
        public String rangeText() {
            return signed(minShift) + ".." + signed(maxShift);
        }
    }

    public static final String SAME_AS_TARGET_MESSAGE = "PICK A DIFFERENT GLYPH";
    static final String EMPTY_MESSAGE = "SOURCE SLOT IS EMPTY: DRAW IT FIRST";

    private OverlayCheck() {
    }

    /**
     * @param sourceIsTarget whether the source cursor is on the glyph being derived
     * @param source the source glyph, or null when its slot is empty
     * @param firstPart the first part already added, or null when there is none
     * @param shift the pending shift
     */
    public static Result check(boolean sourceIsTarget, PixelGlyph source, PixelGlyph firstPart, int shift) {
        if (sourceIsTarget) {
            return new Result(Problem.SAME_AS_TARGET, SAME_AS_TARGET_MESSAGE, false, 0, 0);
        }
        if (source == null) {
            return new Result(Problem.EMPTY, EMPTY_MESSAGE, false, 0, 0);
        }
        PixelGrid pixels = source.pixels();
        int left = leftmostInk(pixels);
        boolean hasRange = left >= 0;
        int min = hasRange ? -left : 0;
        int max = hasRange ? pixels.getWidth() - 1 - rightmostInk(pixels) : 0;
        if (firstPart != null && firstPart.full() != source.full()) {
            String message = source.full()
                ? "THIS IS A FULL GLYPH BUT EARLIER PARTS ARE BODY GLYPHS: FULL AND BODY GLYPHS CANNOT BE MIXED"
                : "THIS IS A BODY GLYPH BUT EARLIER PARTS ARE FULL GLYPHS: FULL AND BODY GLYPHS CANNOT BE MIXED";
            return new Result(Problem.MIXED, message, hasRange, min, max);
        }
        if (hasRange && (shift < min || shift > max)) {
            String area = source.full() ? "THE CELL" : "THE BODY";
            String message = "SHIFT " + signed(shift) + " PUTS INK OUTSIDE " + area + ": ALLOWED "
                + signed(min) + ".." + signed(max);
            return new Result(Problem.OUTSIDE, message, true, min, max);
        }
        return new Result(Problem.NONE, "", hasRange, min, max);
    }

    public enum OffGrid { NONE, LEFT, RIGHT }

    static final float BLINK_ON_SECONDS = 0.25f;
    static final float BLINK_OFF_SECONDS = 0.25f;

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
        float cycle = BLINK_ON_SECONDS + BLINK_OFF_SECONDS;
        float phase = seconds % cycle;
        if (phase < 0) {
            phase += cycle;
        }
        return phase < BLINK_ON_SECONDS;
    }

    static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    /** First column holding ink, or -1 when the grid has none. */
    private static int leftmostInk(PixelGrid grid) {
        for (int x = 0; x < grid.getWidth(); x++) {
            if (columnHasInk(grid, x)) {
                return x;
            }
        }
        return -1;
    }

    private static int rightmostInk(PixelGrid grid) {
        for (int x = grid.getWidth() - 1; x >= 0; x--) {
            if (columnHasInk(grid, x)) {
                return x;
            }
        }
        return -1;
    }

    private static boolean columnHasInk(PixelGrid grid, int x) {
        for (int y = 0; y < grid.getHeight(); y++) {
            if (grid.isLit(x, y)) {
                return true;
            }
        }
        return false;
    }
}
