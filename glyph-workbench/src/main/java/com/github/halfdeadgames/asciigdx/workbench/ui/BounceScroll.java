package com.github.halfdeadgames.asciigdx.workbench.ui;

/**
 * Back-and-forth scrolling for text wider than its space: hold at the start, scroll to the end,
 * hold, scroll back. Sides with hidden text show a ".." marker in their outer two cells.
 */
public final class BounceScroll {

    static final String HIDDEN_MARKER = "..";
    static final int MIN_MARKED_WIDTH = 6;
    static final float HOLD_SECONDS = 1.5f;
    static final float CHARS_PER_SECOND = 4f;

    private BounceScroll() {
    }

    /**
     * Characters to skip from the left of the text at the given time, between 0 and
     * {@code overflowChars}.
     */
    static int offset(int overflowChars, float seconds) {
        if (overflowChars <= 0) {
            return 0;
        }
        float scrollSeconds = overflowChars / CHARS_PER_SECOND;
        float cycleSeconds = 2 * (HOLD_SECONDS + scrollSeconds);
        float phase = seconds % cycleSeconds;
        if (phase < 0) {
            phase += cycleSeconds;
        }
        if (phase < HOLD_SECONDS) {
            return 0;
        }
        phase -= HOLD_SECONDS;
        if (phase < scrollSeconds) {
            return Math.min(overflowChars, (int) (phase * CHARS_PER_SECOND));
        }
        phase -= scrollSeconds;
        if (phase < HOLD_SECONDS) {
            return overflowChars;
        }
        phase -= HOLD_SECONDS;
        return Math.max(0, overflowChars - (int) (phase * CHARS_PER_SECOND));
    }

    /**
     * The visible part of {@code text}, {@code width} cells or fewer, scrolled for the given time.
     * When the text overflows and the window is at least {@value #MIN_MARKED_WIDTH} cells wide, the
     * two outer cells on each side that has hidden text are replaced by {@value #HIDDEN_MARKER}.
     */
    public static String window(String text, int width, float seconds) {
        int total = TextCells.cellCount(text);
        int visible = Math.min(Math.max(0, width), total);
        int offset = offset(total - visible, seconds);
        int start = TextCells.indexAfterCells(text, 0, offset);
        String slice = text.substring(start, TextCells.indexAfterCells(text, start, visible));
        if (total <= visible || visible < MIN_MARKED_WIDTH) {
            return slice;
        }
        boolean hiddenLeft = offset > 0;
        boolean hiddenRight = offset + visible < total;
        return markHidden(slice, hiddenLeft, hiddenRight);
    }

    private static String markHidden(String slice, boolean hiddenLeft, boolean hiddenRight) {
        String marked = slice;
        if (hiddenLeft) {
            marked = HIDDEN_MARKER + TextCells.dropCells(marked, HIDDEN_MARKER.length());
        }
        if (hiddenRight) {
            marked = withoutLastCells(marked, HIDDEN_MARKER.length()) + HIDDEN_MARKER;
        }
        return marked;
    }

    private static String withoutLastCells(String text, int cells) {
        return TextCells.prefix(text, Math.max(0, TextCells.cellCount(text) - cells));
    }
}
