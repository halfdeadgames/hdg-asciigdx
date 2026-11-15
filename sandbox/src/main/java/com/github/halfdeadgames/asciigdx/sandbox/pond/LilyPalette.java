package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** The hand-picked greens a lily can take, chosen to read as water plants on the deep blue water. */
public final class LilyPalette {

    public static final int DEEP_GREEN = 0x2E9447FF;
    public static final int BASE_GREEN = 0x4CC456FF;
    public static final int LIGHT_GREEN = 0x7BE07AFF;
    public static final int YELLOW_GREEN = 0x9CCB3EFF;
    public static final int TEAL_GREEN = 0x3DB58CFF;

    private static final float PALER_AMOUNT = 0.45f;
    private static final int[] COLORS = {DEEP_GREEN, BASE_GREEN, LIGHT_GREEN, YELLOW_GREEN, TEAL_GREEN};

    private LilyPalette() {
    }

    /** Returns the color mixed toward white by a fixed amount, keeping its alpha. */
    public static int paler(int rgba) {
        int red = towardWhite((rgba >>> 24) & 0xFF);
        int green = towardWhite((rgba >>> 16) & 0xFF);
        int blue = towardWhite((rgba >>> 8) & 0xFF);
        return (red << 24) | (green << 16) | (blue << 8) | (rgba & 0xFF);
    }

    private static int towardWhite(int channel) {
        return Math.round(channel + (255 - channel) * PALER_AMOUNT);
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
