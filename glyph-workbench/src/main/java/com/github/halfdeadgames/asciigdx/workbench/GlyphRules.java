package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Checks one glyph's pixels against the font's design rules; no drawing, no libGDX. */
final class GlyphRules {

    static final String KEY_GAP = "gap";
    static final String KEY_ACCENT_INK = "accentInk";
    static final String KEY_DESCENDER_INK = "descenderInk";

    private GlyphRules() {
    }

    /**
     * Lists the rules a glyph breaks.
     *
     * @param codePoint the glyph's code point, used in the gap message
     * @param glyph the glyph, or null for an empty slot
     * @param bodyWidth columns before the gap columns
     * @param accentRowCount rows at the top reserved for accents
     * @param descenderFirstRow first row of the descender band
     * @return the warnings, empty when the glyph is fine
     */
    static List<RuleWarning> check(int codePoint, PixelGlyph glyph, int bodyWidth,
                                   int accentRowCount, int descenderFirstRow) {
        List<RuleWarning> warnings = new ArrayList<>();
        if (glyph == null) {
            return warnings;
        }
        PixelGrid pixels = glyph.pixels();
        if (glyph.full() && hasGapInk(pixels, bodyWidth)) {
            warnings.add(new RuleWarning(KEY_GAP,
                String.format(Locale.ROOT, "U+%04X %s has ink in the gap column", codePoint, GlyphName.fullName(codePoint))));
        }
        if (hasInkInRows(pixels, 0, accentRowCount)) {
            warnings.add(new RuleWarning(KEY_ACCENT_INK, "INK IN ACCENT ROW"));
        }
        if (hasInkInRows(pixels, descenderFirstRow, pixels.getHeight())) {
            warnings.add(new RuleWarning(KEY_DESCENDER_INK, "INK IN DESCENDER ROW"));
        }
        return warnings;
    }

    /** Whether any pixel from column {@code bodyWidth} on is lit. */
    static boolean hasGapInk(PixelGrid grid, int bodyWidth) {
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = bodyWidth; x < grid.getWidth(); x++) {
                if (grid.isLit(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasInkInRows(PixelGrid grid, int firstRow, int endRow) {
        for (int y = Math.max(0, firstRow); y < Math.min(endRow, grid.getHeight()); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                if (grid.isLit(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }
}
