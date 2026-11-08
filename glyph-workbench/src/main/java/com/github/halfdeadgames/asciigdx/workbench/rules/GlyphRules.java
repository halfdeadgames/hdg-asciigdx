package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** Checks one glyph's pixels against the font's design rules; no drawing, no libGDX. */
public final class GlyphRules {

    public static final String KEY_ACCENT_INK = "accentInk";
    public static final String KEY_DESCENDER_INK = "descenderInk";

    private static final int FIRST_ABOVE_MARK = 0x0300;
    private static final int LAST_ABOVE_MARK = 0x0315;

    private GlyphRules() {
    }

    /**
     * Lists the rules a glyph breaks.
     *
     * @param codePoint the glyph's code point
     * @param glyph the glyph, or null for an empty slot
     * @param accentRowCount rows at the top reserved for accents; empty skips the accent check
     * @param descenderFirstRow first row of the descender band; empty skips the descender check
     * @return the warnings, empty when the glyph is fine or is a tiling glyph
     */
    public static List<RuleWarning> check(int codePoint, PixelGlyph glyph,
                                   OptionalInt accentRowCount, OptionalInt descenderFirstRow) {
        List<RuleWarning> warnings = new ArrayList<>();
        if (glyph == null || TilingGlyphs.isTiling(codePoint)) {
            return warnings;
        }
        PixelGrid pixels = glyph.pixels();
        if (accentRowCount.isPresent() && breaksAccentRowRule(codePoint, pixels, accentRowCount.getAsInt())) {
            warnings.add(new RuleWarning(KEY_ACCENT_INK, "INK IN ACCENT ROW"));
        }
        if (descenderFirstRow.isPresent()
            && countInkInRows(pixels, descenderFirstRow.getAsInt(), pixels.getHeight()) > 0) {
            warnings.add(new RuleWarning(KEY_DESCENDER_INK, "INK IN DESCENDER ROW"));
        }
        return warnings;
    }

    /**
     * A lowercase glyph may put a single pixel in the accent rows (ascender tip or dot); anything else is a violation.
     * Accent marks and letters with an accent above are exempt.
     */
    private static boolean breaksAccentRowRule(int codePoint, PixelGrid grid, int accentRowCount) {
        if (isAccentMark(codePoint) || hasAccentAbove(codePoint)) {
            return false;
        }
        int inkedPixels = countInkInRows(grid, 0, accentRowCount);
        if (Character.isLowerCase(codePoint)) {
            return inkedPixels > 1;
        }
        return inkedPixels > 0;
    }

    private static boolean isAccentMark(int codePoint) {
        return Character.getType(codePoint) == Character.MODIFIER_SYMBOL
            || Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.SPACING_MODIFIER_LETTERS;
    }

    /** Whether the decomposed letter carries a combining mark that sits above it (U+0300..U+0315). */
    private static boolean hasAccentAbove(int codePoint) {
        String decomposed = Normalizer.normalize(new String(Character.toChars(codePoint)), Normalizer.Form.NFD);
        return decomposed.codePoints().anyMatch(part -> part >= FIRST_ABOVE_MARK && part <= LAST_ABOVE_MARK);
    }

    private static int countInkInRows(PixelGrid grid, int firstRow, int endRow) {
        int inkedPixels = 0;
        for (int y = Math.max(0, firstRow); y < Math.min(endRow, grid.getHeight()); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                if (grid.isLit(x, y)) {
                    inkedPixels++;
                }
            }
        }
        return inkedPixels;
    }
}
