package com.github.halfdeadgames.asciigdx.glyph;

/** Controls whether a glyph sheet image is converted to white-with-alpha glyphs. */
public enum TransparencyMode {

    /** Converts only if the image has no pixel with alpha below 255. */
    AUTO,

    /** Always converts the image. */
    ALWAYS,

    /** Keeps the image as-is. */
    NEVER;

    /**
     * Decides whether an image must be converted.
     *
     * @param imageHasTransparentPixel whether any pixel of the image has alpha below 255
     * @return true if the image should be converted
     */
    public boolean shouldConvert(boolean imageHasTransparentPixel) {
        return switch (this) {
            case AUTO -> !imageHasTransparentPixel;
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }
}
