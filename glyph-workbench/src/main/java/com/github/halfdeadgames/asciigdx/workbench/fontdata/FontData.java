package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A parsed font data file: the cell size and its glyphs by code point.
 *
 * @param cellWidth width of a cell, at least 8
 * @param cellHeight height of a cell
 * @param entries glyphs by code point
 */
public record FontData(int cellWidth, int cellHeight, Map<Integer, GlyphEntry> entries) {

    private static final int GAP_DIVISOR = 8;

    /** Creates font data and copies the entries. */
    public FontData {
        if (cellWidth < GAP_DIVISOR || cellHeight < 1) {
            throw new IllegalArgumentException(
                "Cell must be at least 8 wide and 1 high, was " + cellWidth + "x" + cellHeight);
        }
        for (Map.Entry<Integer, GlyphEntry> entry : entries.entrySet()) {
            if (entry.getKey() != entry.getValue().codePoint()) {
                throw new IllegalArgumentException("Entry key U+" + Integer.toHexString(entry.getKey())
                    + " does not match its glyph");
            }
        }
        entries = Collections.unmodifiableMap(new TreeMap<>(entries));
    }

    /** Returns the letter gap in columns: one eighth of the cell width. */
    public int gapWidth() {
        return cellWidth / GAP_DIVISOR;
    }

    /** Returns the width of the drawn part of a normal glyph. */
    public int bodyWidth() {
        return cellWidth - gapWidth();
    }
}
