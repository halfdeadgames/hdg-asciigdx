package com.github.halfdeadgames.asciigdx.glyph;

/** How a glyph sheet texture is sampled when glyphs are drawn. */
public enum GlyphFilter {

    /** Crisp pixels. This is the default. */
    NEAREST,

    /**
     * Smooth edges when glyphs are scaled to fractional sizes or rotated. The sheet is repacked
     * with a 1 pixel transparent gutter around every glyph so neighbors never bleed in, which makes
     * the texture slightly larger. At whole-number scales this looks the same as {@link #NEAREST}
     * only when glyphs are pixel-aligned.
     */
    LINEAR
}
