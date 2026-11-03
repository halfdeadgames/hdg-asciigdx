package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class GlyphRulesTest {

    private static final int BODY_WIDTH = 7;
    private static final OptionalInt ACCENT_ROWS = OptionalInt.of(1);
    private static final OptionalInt DESCENDER_FIRST_ROW = OptionalInt.of(7);

    private static List<RuleWarning> check(boolean full, List<String> rows) {
        PixelGlyph glyph = new PixelGlyph(0x41, full, PixelGrid.ofRows(rows));
        return GlyphRules.check(0x41, glyph, BODY_WIDTH, ACCENT_ROWS, DESCENDER_FIRST_ROW);
    }

    private static List<String> keys(List<RuleWarning> warnings) {
        return warnings.stream().map(RuleWarning::key).collect(java.util.stream.Collectors.toList());
    }

    @Test
    void inkInsideTheMetricRowsHasNoWarning() {
        List<String> rows = List.of(
            ".......", ".#.....", ".#.....", ".#.....", ".#.....", ".#.....", ".#.....", ".......");
        assertTrue(check(false, rows).isEmpty());
    }

    @Test
    void inkInTheAccentRowWarns() {
        List<String> rows = List.of(
            ".#.....", ".......", ".......", ".......", ".......", ".......", ".......", ".......");
        assertEquals(List.of(GlyphRules.KEY_ACCENT_INK), keys(check(false, rows)));
        assertEquals("INK IN ACCENT ROW", check(false, rows).get(0).message());
    }

    @Test
    void lowercaseWithSinglePixelInTheAccentRowHasNoWarning() {
        List<String> rows = List.of(
            ".#.....", ".#.....", ".###...", ".#.#...", ".#.#...", ".#.#...", ".......", ".......");
        PixelGlyph glyph = new PixelGlyph('h', false, PixelGrid.ofRows(rows));
        assertTrue(GlyphRules.check('h', glyph, BODY_WIDTH, ACCENT_ROWS, DESCENDER_FIRST_ROW).isEmpty());
    }

    @Test
    void lowercaseWithTwoPixelsInTheAccentRowWarns() {
        List<String> rows = List.of(
            ".##....", ".#.....", ".#.....", ".#.....", ".#.....", ".#.....", ".......", ".......");
        PixelGlyph glyph = new PixelGlyph('l', false, PixelGrid.ofRows(rows));
        assertEquals(List.of(GlyphRules.KEY_ACCENT_INK),
            keys(GlyphRules.check('l', glyph, BODY_WIDTH, ACCENT_ROWS, DESCENDER_FIRST_ROW)));
    }

    @Test
    void lowercaseIWithSingleDotPixelHasNoWarning() {
        List<String> rows = List.of(
            ".#.....", ".......", ".#.....", ".#.....", ".#.....", ".#.....", ".......", ".......");
        PixelGlyph glyph = new PixelGlyph('i', false, PixelGrid.ofRows(rows));
        assertTrue(GlyphRules.check('i', glyph, BODY_WIDTH, ACCENT_ROWS, DESCENDER_FIRST_ROW).isEmpty());
    }

    @Test
    void capitalWithInkInTheAccentRowStillWarns() {
        List<String> rows = List.of(
            ".#.#...", ".#.#...", ".###...", ".#.#...", ".#.#...", ".#.#...", ".......", ".......");
        PixelGlyph glyph = new PixelGlyph('H', false, PixelGrid.ofRows(rows));
        assertEquals(List.of(GlyphRules.KEY_ACCENT_INK),
            keys(GlyphRules.check('H', glyph, BODY_WIDTH, ACCENT_ROWS, DESCENDER_FIRST_ROW)));
    }

    @Test
    void inkInTheDescenderRowWarns() {
        List<String> rows = List.of(
            ".......", ".......", ".......", ".......", ".......", ".......", ".......", "..#....");
        assertEquals(List.of(GlyphRules.KEY_DESCENDER_INK), keys(check(false, rows)));
        assertEquals("INK IN DESCENDER ROW", check(false, rows).get(0).message());
    }

    @Test
    void bothOutsideRowsWarnSeparately() {
        List<String> rows = List.of(
            ".#.....", ".......", ".......", ".......", ".......", ".......", ".......", "..#....");
        assertEquals(List.of(GlyphRules.KEY_ACCENT_INK, GlyphRules.KEY_DESCENDER_INK), keys(check(false, rows)));
    }

    @Test
    void widerMetricsMoveTheRows() {
        PixelGrid grid = PixelGrid.ofRows(List.of(
            "....", "#...", "....", "....", "....", "....", "....", "...."));
        PixelGlyph glyph = new PixelGlyph(0x41, false, grid);
        assertTrue(GlyphRules.check(0x41, glyph, 4, OptionalInt.of(1), OptionalInt.of(7)).isEmpty());
        assertEquals(List.of(GlyphRules.KEY_ACCENT_INK),
            keys(GlyphRules.check(0x41, glyph, 4, OptionalInt.of(2), OptionalInt.of(7))));
    }

    @Test
    void absentAccentSettingSkipsOnlyTheAccentCheck() {
        List<String> rows = List.of(
            ".#.....", ".......", ".......", ".......", ".......", ".......", ".......", "..#....");
        PixelGlyph glyph = new PixelGlyph(0x41, false, PixelGrid.ofRows(rows));
        assertEquals(List.of(GlyphRules.KEY_DESCENDER_INK),
            keys(GlyphRules.check(0x41, glyph, BODY_WIDTH, OptionalInt.empty(), DESCENDER_FIRST_ROW)));
    }

    @Test
    void absentDescenderSettingSkipsOnlyTheDescenderCheck() {
        List<String> rows = List.of(
            ".#.....", ".......", ".......", ".......", ".......", ".......", ".......", "..#....");
        PixelGlyph glyph = new PixelGlyph(0x41, false, PixelGrid.ofRows(rows));
        assertEquals(List.of(GlyphRules.KEY_ACCENT_INK),
            keys(GlyphRules.check(0x41, glyph, BODY_WIDTH, ACCENT_ROWS, OptionalInt.empty())));
    }

    @Test
    void gapCheckStillRunsWithBothSettingsAbsent() {
        List<String> rows = List.of(
            ".#......", ".......#", "........", "........", "........", "........", "........", "..#.....");
        PixelGlyph glyph = new PixelGlyph(0x41, true, PixelGrid.ofRows(rows));
        assertEquals(List.of(GlyphRules.KEY_GAP),
            keys(GlyphRules.check(0x41, glyph, BODY_WIDTH, OptionalInt.empty(), OptionalInt.empty())));
    }

    @Test
    void inkInTheGapColumnOfAFullGlyphWarns() {
        List<String> bodyOnlyRows = List.of(
            "........", "........", "........", "........", "........", ".#......", "........", "........");
        assertTrue(check(true, bodyOnlyRows).isEmpty());
        List<String> gapRows = List.of(
            "........", "........", "........", "........", "........", ".......#", "........", "........");
        List<RuleWarning> warnings = check(true, gapRows);
        assertEquals(List.of(GlyphRules.KEY_GAP), keys(warnings));
        assertEquals("U+0041 " + GlyphName.fullName('A') + " has ink in the gap column", warnings.get(0).message());
    }

    @Test
    void emptyGlyphAndEmptySlotHaveNoWarnings() {
        assertTrue(check(true, List.of(
            "........", "........", "........", "........", "........", "........", "........", "........")).isEmpty());
        assertTrue(GlyphRules.check(0x41, null, BODY_WIDTH, ACCENT_ROWS, DESCENDER_FIRST_ROW).isEmpty());
    }
}
