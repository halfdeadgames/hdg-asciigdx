package com.github.halfdeadgames.asciigdx.color;

/**
 * Helpers for the RGBA8888 ints ({@code 0xRRGGBBAA}) used by {@code TerminalGrid}. All methods
 * return plain ints, so the results can be passed straight to the grid setters.
 */
public final class CellColor {

    private static final int RGB_MASK = 0xFFFFFF;
    private static final int ALPHA_MASK = 0xFF;
    private static final int RGB_ONLY_MASK = 0xFFFFFF00;

    private CellColor() {
    }

    /**
     * Converts a 0xRRGGBB color to RGBA8888 with full alpha. A 6-digit hex literal like
     * {@code 0xFF4040} is read as {@code 0x00FF4040}, so its channels shift down one byte and its
     * blue becomes alpha. Wrap it with this method.
     *
     * @param rgb color as 0xRRGGBB; bits above the low 24 are ignored
     * @return the color as 0xRRGGBBFF
     */
    public static int rgb(int rgb) {
        return ((rgb & RGB_MASK) << 8) | ALPHA_MASK;
    }

    /**
     * Packs four channels into an RGBA8888 int.
     *
     * @param red red channel, 0 to 255
     * @param green green channel, 0 to 255
     * @param blue blue channel, 0 to 255
     * @param alpha alpha channel, 0 to 255
     * @return the packed color
     * @throws IllegalArgumentException if a channel is outside 0 to 255
     */
    public static int rgba(int red, int green, int blue, int alpha) {
        requireChannel("red", red);
        requireChannel("green", green);
        requireChannel("blue", blue);
        requireChannel("alpha", alpha);
        return (red << 24) | (green << 16) | (blue << 8) | alpha;
    }

    /**
     * Replaces the alpha of a color and keeps its red, green, and blue.
     *
     * @param rgba color as RGBA8888
     * @param alpha new alpha, 0 to 255
     * @return the color with the new alpha
     * @throws IllegalArgumentException if alpha is outside 0 to 255
     */
    public static int withAlpha(int rgba, int alpha) {
        requireChannel("alpha", alpha);
        return (rgba & RGB_ONLY_MASK) | alpha;
    }

    /**
     * Inverts a color into its negative: red, green, and blue become 255 minus their value, and
     * alpha is kept.
     *
     * @param rgba color as RGBA8888
     * @return the negative color with the same alpha
     */
    public static int invert(int rgba) {
        return ((~rgba) & RGB_ONLY_MASK) | (rgba & ALPHA_MASK);
    }

    /**
     * Blends two colors in Oklab, which gives perceptually even mixes. Alpha is interpolated
     * linearly and rounded.
     *
     * @param fromRgba color at amount 0, as RGBA8888
     * @param toRgba color at amount 1, as RGBA8888
     * @param amount mix factor, clamped to 0 to 1
     * @return the blended color; exactly {@code fromRgba} at 0 and {@code toRgba} at 1
     */
    public static int mixOklab(int fromRgba, int toRgba, float amount) {
        if (!(amount > 0f)) {
            return fromRgba;
        }
        if (amount >= 1f) {
            return toRgba;
        }
        int fromAlpha = fromRgba & ALPHA_MASK;
        int toAlpha = toRgba & ALPHA_MASK;
        int alpha = Math.round(fromAlpha + (toAlpha - fromAlpha) * amount);
        return withAlpha(OklabMix.mixRgb(fromRgba, toRgba, amount), alpha);
    }

    private static void requireChannel(String name, int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(
                "Channel " + name + " must be 0 to 255, was " + value);
        }
    }
}
