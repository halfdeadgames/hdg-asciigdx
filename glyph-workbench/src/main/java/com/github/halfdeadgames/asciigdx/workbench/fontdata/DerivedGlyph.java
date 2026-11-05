package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.Objects;

/**
 * A glyph kept as a recipe over other glyphs.
 *
 * @param codePoint the Unicode code point
 * @param op the recipe
 */
public record DerivedGlyph(int codePoint, GlyphOp op) implements GlyphEntry {

    public DerivedGlyph {
        Objects.requireNonNull(op, "op must not be null");
    }
}
