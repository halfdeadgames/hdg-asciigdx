package com.github.halfdeadgames.asciigdx.glyph;

/**
 * How a glyph sheet texture is sampled when glyphs are drawn. Pass it to the loading methods of
 * {@link TextureGlyphSource}.
 */
public enum GlyphFilter {

    /**
     * Nearest-pixel sampling. Glyph pixels stay crisp and hard-edged. The sheet is uploaded
     * as it is. This is the default of every loading method that takes no filter.
     */
    NEAREST,

    /**
     * Linear sampling. Edges look smooth when glyphs are scaled to fractional sizes or
     * rotated. The sheet is repacked with a 1 pixel transparent gutter around every glyph so
     * neighbors never bleed in, which makes the uploaded texture slightly larger. The glyph
     * regions and cell size reported by the source do not change. At whole-number scales the
     * result matches {@link #NEAREST} only when glyphs are pixel-aligned.
     */
    LINEAR
}
