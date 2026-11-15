package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** The hand-picked greens a lily can take, chosen to read as water plants on the deep blue water. */
public final class LilyPalette {

    public static final int DEEP_GREEN = 0x2E9447FF;
    public static final int BASE_GREEN = 0x4CC456FF;
    public static final int LIGHT_GREEN = 0x7BE07AFF;
    public static final int YELLOW_GREEN = 0x9CCB3EFF;
    public static final int TEAL_GREEN = 0x3DB58CFF;

    private static final int[] COLORS = {DEEP_GREEN, BASE_GREEN, LIGHT_GREEN, YELLOW_GREEN, TEAL_GREEN};

    private LilyPalette() {
    }

    /** Returns how many colors the palette holds. */
    public static int size() {
        return COLORS.length;
    }

    /** Returns the RGBA color at the index, from 0 up to but excluding {@link #size()}. */
    public static int colorAt(int index) {
        return COLORS[index];
    }
}
