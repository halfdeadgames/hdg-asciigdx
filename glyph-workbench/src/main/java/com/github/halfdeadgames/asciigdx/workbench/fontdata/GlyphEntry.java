package com.github.halfdeadgames.asciigdx.workbench.fontdata;

/** One glyph of a font data file: drawn pixels or a recipe built from other glyphs. */
public sealed interface GlyphEntry permits PixelGlyph, DerivedGlyph {

    /** Returns the Unicode code point this glyph is for. */
    int codePoint();
}
