package com.github.halfdeadgames.asciigdx.glyph;

/**
 * Code points of quadrant block glyphs, for sub-cell drawing: each cell is split into four
 * quadrants that are either lit or empty, which doubles the resolution in both directions.
 *
 * <p>A quadrant combination is a 4-bit mask: {@link #TOP_LEFT}, {@link #TOP_RIGHT},
 * {@link #BOTTOM_LEFT}, and {@link #BOTTOM_RIGHT}. With {@link Shade#SOLID} the lit quadrants are
 * filled, and the code points are the standard Unicode quadrant blocks (U+2580 range). The other
 * shades fill lit quadrants with a pattern instead; Unicode has no characters for those, so they
 * live in the Private Use Area at {@code U+E000 + shade index * 16 + mask}, where the shade index
 * counts from 0 for {@link Shade#BAYER_75}. Mask 0 is always a space.
 *
 * <p>The bundled default sheet draws all of these. A custom sheet that wants them lists the same
 * code points in its character list.
 */
public final class QuadrantGlyphs {

    public static final int TOP_LEFT = 1;
    public static final int TOP_RIGHT = 2;
    public static final int BOTTOM_LEFT = 4;
    public static final int BOTTOM_RIGHT = 8;
    public static final int ALL = 15;

    /** First code point of the patterned shades. */
    public static final int PRIVATE_USE_START = 0xE000;

    private static final int[] SOLID_BY_MASK = {
        ' ', 0x2598, 0x259D, 0x2580, 0x2596, 0x258C, 0x259E, 0x259B,
        0x2597, 0x259A, 0x2590, 0x259C, 0x2584, 0x2599, 0x259F, 0x2588,
    };

    /** How the lit quadrants of a glyph are filled. Lit pixels per quadrant are given in percent. */
    public enum Shade {
        SOLID,
        BAYER_75,
        BAYER_50,
        BAYER_25,
        HATCH_75,
        HATCH_50,
        HATCH_25,
        FLIPPED_HATCH_75,
        FLIPPED_HATCH_50,
        FLIPPED_HATCH_25
    }

    private QuadrantGlyphs() {
    }

    /**
     * Returns the code point of a quadrant glyph.
     *
     * @param shade how lit quadrants are filled
     * @param mask which quadrants are lit, a combination of the quadrant constants (0 to 15)
     * @return the code point; a space for mask 0
     * @throws IllegalArgumentException if {@code mask} is outside 0 to 15
     */
    public static int codePoint(Shade shade, int mask) {
        if (mask < 0 || mask > ALL) {
            throw new IllegalArgumentException("Mask must be 0 to 15, was " + mask);
        }
        if (mask == 0) {
            return ' ';
        }
        if (shade == Shade.SOLID) {
            return SOLID_BY_MASK[mask];
        }
        return PRIVATE_USE_START + (shade.ordinal() - 1) * 16 + mask;
    }
}
