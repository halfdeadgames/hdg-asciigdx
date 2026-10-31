package com.github.halfdeadgames.asciigdx.glyph;

/**
 * Pure integer math for turning a color-keyed glyph image into white glyphs with alpha.
 * Pixels are RGBA8888 ints: red in the high byte, alpha in the low byte, as returned by
 * {@code Pixmap.getPixel} and {@code Color.rgba8888}.
 *
 * <p>The default key is the sheet's top-left pixel; the loader chooses it, not this class.
 * Conversion whitens glyph colors so the renderer can tint them.
 */
public final class ColorKeyConversion {

    private static final int WHITE_RGB = 0xFFFFFF00;
    private static final int OPAQUE = 255;

    private ColorKeyConversion() {
    }

    /**
     * Converts a pixel to white with an alpha derived from its distance to the key color.
     * An exact key match is fully transparent. With a black key the alpha equals the
     * brightest channel. Anti-aliased edge pixels keep partial alpha. The alpha of both the
     * pixel and the key is ignored.
     *
     * @param rgba8888 the pixel to convert
     * @param keyRgba8888 the key color that becomes transparent
     * @return white (0xFFFFFF) in the high bytes with the computed alpha in the low byte
     */
    public static int toWhiteWithAlpha(int rgba8888, int keyRgba8888) {
        int redDistance = channelDistance(rgba8888, keyRgba8888, 24);
        int greenDistance = channelDistance(rgba8888, keyRgba8888, 16);
        int blueDistance = channelDistance(rgba8888, keyRgba8888, 8);
        int alpha = Math.max(redDistance, Math.max(greenDistance, blueDistance));
        return WHITE_RGB | alpha;
    }

    /**
     * Tells whether a pixel is not fully opaque.
     *
     * @param rgba8888 the pixel to test
     * @return true if its alpha is below 255
     */
    public static boolean isTransparent(int rgba8888) {
        return (rgba8888 & 0xFF) < OPAQUE;
    }

    private static int channelDistance(int pixel, int key, int shift) {
        return Math.abs(((pixel >>> shift) & 0xFF) - ((key >>> shift) & 0xFF));
    }
}
