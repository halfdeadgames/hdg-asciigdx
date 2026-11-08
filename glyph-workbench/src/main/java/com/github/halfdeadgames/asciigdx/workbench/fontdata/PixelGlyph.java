package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.Objects;

/**
 * A glyph with drawn pixels that cover the whole cell.
 *
 * @param codePoint the Unicode code point
 * @param pixels the cell pixels
 */
public record PixelGlyph(int codePoint, PixelGrid pixels) implements GlyphEntry {

    public PixelGlyph {
        Objects.requireNonNull(pixels, "pixels must not be null");
    }
}
