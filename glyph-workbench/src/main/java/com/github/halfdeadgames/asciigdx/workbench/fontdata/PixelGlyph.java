package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.Objects;

/**
 * A glyph with drawn pixels. A normal glyph holds only its body; the letter gap is added on
 * export. A full glyph holds the whole cell.
 *
 * @param codePoint the Unicode code point
 * @param full whether {@code pixels} cover the whole cell instead of the body
 * @param pixels body or cell pixels
 */
public record PixelGlyph(int codePoint, boolean full, PixelGrid pixels) implements GlyphEntry {

    public PixelGlyph {
        Objects.requireNonNull(pixels, "pixels must not be null");
    }

    /**
     * Returns the whole cell: the pixels, plus empty gap columns on the right unless the glyph is full.
     *
     * @param cellWidth width of the cell
     * @return a grid {@code cellWidth} wide
     */
    public PixelGrid toCell(int cellWidth) {
        return full ? pixels : pixels.paddedRight(cellWidth);
    }
}
