package com.github.halfdeadgames.asciigdx.workbench.ui;

/** Back-and-forth scrolling for text wider than its space: hold at the start, scroll to the end, hold, scroll back. */
public final class BounceScroll {

    static final float HOLD_SECONDS = 1.5f;
    static final float CHARS_PER_SECOND = 4f;

    private BounceScroll() {
    }

    /** Characters to skip from the left of the text at the given time, between 0 and {@code overflowChars}. */
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

    /** The visible part of {@code text}, {@code width} characters or fewer, scrolled for the given time. */
    public static String window(String text, int width, float seconds) {
        int visible = Math.min(Math.max(0, width), text.length());
        int start = offset(text.length() - visible, seconds);
        return text.substring(start, start + visible);
    }
}
