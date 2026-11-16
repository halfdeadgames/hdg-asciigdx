package com.github.halfdeadgames.asciigdx.color;

/**
 * Helpers for colors packed as RGBA8888 ints, the color format used by
 * {@link com.github.halfdeadgames.asciigdx.grid.TerminalGrid}. A color is one {@code int} laid out
 * as {@code 0xRRGGBBAA}: red in the highest byte, then green, then blue, with alpha in the lowest
 * byte. Alpha 0 is fully transparent and 255 is fully opaque. All methods return plain ints, so
 * the results can be passed straight to the grid setters.
 *
 * <p>All methods are pure, allocation-free, and safe to call from any thread.
 *
 * <p>Example:
 *
 * <pre>{@code
 * int orange = CellColor.rgb(0xFF8000);                    // 0xFF8000FF
 * int faded = CellColor.withAlphaScaled(orange, 0.5f);     // 0xFF800080
 * int tinted = CellColor.mixOklab(orange, CellColor.rgb(0x0040FF), 0.25f);
 * }</pre>
 */
public final class CellColor {

    private static final int RED_SHIFT = 24;
    private static final int GREEN_SHIFT = 16;
    private static final int BLUE_SHIFT = 8;
    private static final int ALPHA_BITS = 8;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int MAX_CHANNEL = 255;
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
        return ((rgb & RGB_MASK) << ALPHA_BITS) | ALPHA_MASK;
    }

    /**
     * Packs four channels into an RGBA8888 int.
     *
     * @param red red channel, 0 to 255
     * @param green green channel, 0 to 255
     * @param blue blue channel, 0 to 255
     * @param alpha alpha channel, 0 (transparent) to 255 (opaque)
     * @return the packed color as 0xRRGGBBAA
     * @throws IllegalArgumentException if a channel is outside 0 to 255
     */
    public static int rgba(int red, int green, int blue, int alpha) {
        requireChannel("red", red);
        requireChannel("green", green);
        requireChannel("blue", blue);
        requireChannel("alpha", alpha);
        return (red << RED_SHIFT) | (green << GREEN_SHIFT) | (blue << BLUE_SHIFT) | alpha;
    }

    /**
     * Replaces the alpha of a color and keeps its red, green, and blue.
     *
     * @param rgba color as RGBA8888
     * @param alpha new alpha, 0 (transparent) to 255 (opaque)
     * @return the color with the new alpha
     * @throws IllegalArgumentException if alpha is outside 0 to 255
     */
    public static int withAlpha(int rgba, int alpha) {
        requireChannel("alpha", alpha);
        return (rgba & RGB_ONLY_MASK) | alpha;
    }

    /**
     * Multiplies the alpha of a color by a factor and keeps its red, green, and blue. The result
     * is rounded to the nearest integer and clamped to 0 to 255, so a factor above 1 never
     * exceeds full opacity and a negative factor gives alpha 0. A NaN factor gives alpha 0, the
     * same as a factor of 0.
     *
     * @param rgba color as RGBA8888
     * @param factor multiplier for the alpha byte; 1 keeps the alpha, 0 clears it; NaN counts as 0
     * @return the color with the scaled alpha
     */
    public static int withAlphaScaled(int rgba, float factor) {
        int scaledAlpha = Math.round((rgba & ALPHA_MASK) * factor);
        int clampedAlpha = Math.max(0, Math.min(MAX_CHANNEL, scaledAlpha));
        return (rgba & ~ALPHA_MASK) | clampedAlpha;
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
     * linearly and rounded, so the result takes part of its alpha from both colors. Use
     * {@link #mixRgbKeepingAlpha} to keep the alpha of {@code fromRgba} instead.
     *
     * <p>Performance: each call does double-precision math including cube roots and powers, so it
     * costs noticeably more than {@link #mixRgbKeepingAlpha}. Measure before using it on every
     * cell of a large grid each frame.
     *
     * @param fromRgba color at amount 0, as RGBA8888
     * @param toRgba color at amount 1, as RGBA8888
     * @param amount mix factor, clamped to 0 to 1; NaN counts as 0
     * @return the blended color; exactly {@code fromRgba} at 0 and {@code toRgba} at 1
     * @see #mixRgbKeepingAlpha
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
        return withAlpha(OklabMix.mixRgbChannels(fromRgba, toRgba, amount), alpha);
    }

    /**
     * Interpolates the red, green, and blue bytes of the stored RGBA8888 values per channel,
     * rounding each to the nearest value. This is not gamma-correct. The alpha of
     * {@code fromRgba} is kept unchanged, and the alpha of {@code toRgba} is ignored. Use
     * {@link #mixOklab} to blend alpha as well.
     *
     * @param fromRgba color at amount 0, as RGBA8888
     * @param toRgba color at amount 1, as RGBA8888; its alpha is ignored
     * @param amount mix factor, clamped to 0 to 1; NaN counts as 0
     * @return the blended color; exactly {@code fromRgba} at 0, and the RGB of {@code toRgba} with
     *     the alpha of {@code fromRgba} at 1
     * @see #mixOklab
     */
    public static int mixRgbKeepingAlpha(int fromRgba, int toRgba, float amount) {
        if (!(amount > 0f)) {
            return fromRgba;
        }
        if (amount >= 1f) {
            return (toRgba & RGB_ONLY_MASK) | (fromRgba & ALPHA_MASK);
        }
        int red = mixChannel(fromRgba, toRgba, RED_SHIFT, amount);
        int green = mixChannel(fromRgba, toRgba, GREEN_SHIFT, amount);
        int blue = mixChannel(fromRgba, toRgba, BLUE_SHIFT, amount);
        return (red << RED_SHIFT) | (green << GREEN_SHIFT) | (blue << BLUE_SHIFT)
                | (fromRgba & ALPHA_MASK);
    }

    private static int mixChannel(int fromRgba, int toRgba, int shift, float amount) {
        int from = (fromRgba >>> shift) & CHANNEL_MASK;
        int to = (toRgba >>> shift) & CHANNEL_MASK;
        return Math.round(from + (to - from) * amount);
    }

    private static void requireChannel(String name, int value) {
        if (value < 0 || value > MAX_CHANNEL) {
            throw new IllegalArgumentException(
                    "Channel " + name + " must be 0 to 255, was " + value);
        }
    }
}
