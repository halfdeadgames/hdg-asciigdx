package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A parsed font data file: the cell size and its glyphs by code point.
 *
 * @param cellWidth width of a cell, at least 1
 * @param cellHeight height of a cell, at least 1
 * @param entries glyphs by code point
 */
public record FontData(int cellWidth, int cellHeight, Map<Integer, GlyphEntry> entries) {

    /** Creates font data and copies the entries. */
    public FontData {
        if (cellWidth < 1 || cellHeight < 1) {
            throw new IllegalArgumentException(
                "Cell must be at least 1 wide and 1 high, was " + cellWidth + "x" + cellHeight);
        }
        for (Map.Entry<Integer, GlyphEntry> entry : entries.entrySet()) {
            if (entry.getKey() != entry.getValue().codePoint()) {
                throw new IllegalArgumentException("Entry key " + CodePoints.label(entry.getKey())
                    + " does not match its glyph");
            }
        }
        entries = Collections.unmodifiableMap(new TreeMap<>(entries));
    }
}
