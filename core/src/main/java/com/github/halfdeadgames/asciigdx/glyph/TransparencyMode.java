package com.github.halfdeadgames.asciigdx.glyph;

/**
 * Controls whether a glyph sheet image is converted to white glyphs with alpha, so the glyphs
 * can be tinted. Conversion turns each pixel white and derives its alpha from how far its
 * color is from the key color; pixels equal to the key become fully transparent.
 */
public enum TransparencyMode {

    /**
     * Converts the image only if no pixel has an alpha below 255. An image that already has
     * transparent pixels is kept as it is.
     */
    AUTO,

    /** Always converts the image, whatever alpha it already has. */
    ALWAYS,

    /** Keeps the image as it is. The key color is not used. */
    NEVER;

    /**
     * Decides whether an image must be converted.
     *
     * @param imageHasTransparentPixel whether any pixel of the image has alpha below 255
     * @return true if the image should be converted
     */
    boolean shouldConvert(boolean imageHasTransparentPixel) {
        return switch (this) {
            case AUTO -> !imageHasTransparentPixel;
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }
}
