package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleTextTest {

    private static BitSet bits(int... numbers) {
        var set = new BitSet();
        for (int number : numbers) {
            set.set(number);
        }
        return set;
    }

    private static GlyphRule rule(RuleCheck check, CheckSettings settings, List<String> groupNames,
            List<RuleException> exceptions) {
        return new GlyphRule("Some rule", check, settings, GlyphSet.all(), groupNames, exceptions);
    }

    private static GlyphRule rule(RuleCheck check, CheckSettings settings) {
        return rule(check, settings, List.of("all"), List.of());
    }

    private static String checkLine(GlyphRule rule) {
        return RuleText.describe(rule).get(1);
    }

    @Test
    void firstLineIsTheUpperCasedName() {
        assertEquals("SOME RULE", RuleText.describe(rule(RuleCheck.CENTERED_X, CheckSettings.NONE)).get(0));
    }

    @Test
    void singleRowIsSingular() {
        var settings = new CheckSettings(bits(3), new BitSet(), 0);
        assertEquals("- No ink in row 3.", checkLine(rule(RuleCheck.NO_INK_ROWS, settings)));
    }

    @Test
    void rowRangeIsCollapsed() {
        var settings = new CheckSettings(bits(12, 13, 14, 15), new BitSet(), 0);
        assertEquals("- No ink in rows 12-15.", checkLine(rule(RuleCheck.NO_INK_ROWS, settings)));
    }

    @Test
    void rowsMixSinglesAndRanges() {
        var settings = new CheckSettings(bits(0, 12, 13, 14, 15), new BitSet(), 0);
        assertEquals("- No ink in rows 0, 12-15.", checkLine(rule(RuleCheck.NO_INK_ROWS, settings)));
    }

    @Test
    void twoSeparateRowsArePlural() {
        var settings = new CheckSettings(bits(2, 9), new BitSet(), 0);
        assertEquals("- No ink in rows 2, 9.", checkLine(rule(RuleCheck.NO_INK_ROWS, settings)));
    }

    @Test
    void columnsUseColumnWording() {
        var single = new CheckSettings(new BitSet(), bits(0), 0);
        var several = new CheckSettings(new BitSet(), bits(0, 1, 15), 0);
        assertEquals("- No ink in column 0.", checkLine(rule(RuleCheck.NO_INK_COLUMNS, single)));
        assertEquals("- No ink in columns 0-1, 15.", checkLine(rule(RuleCheck.NO_INK_COLUMNS, several)));
    }

    @Test
    void ringStatesItsSize() {
        var settings = new CheckSettings(new BitSet(), new BitSet(), 2);
        assertEquals("- Blank ring of 2 px on every side.", checkLine(rule(RuleCheck.RING, settings)));
    }

    @Test
    void centeringChecksAreExplained() {
        assertEquals("- Centered across: equal blank columns left and right.",
                checkLine(rule(RuleCheck.CENTERED_X, CheckSettings.NONE)));
        assertEquals("- Centered both ways: equal blank columns and rows on opposite sides.",
                checkLine(rule(RuleCheck.CENTERED_BOTH, CheckSettings.NONE)));
    }

    @Test
    void allDisplaysAsAllGlyphs() {
        var lines = RuleText.describe(rule(RuleCheck.CENTERED_X, CheckSettings.NONE));
        assertEquals("- Checks: all glyphs.", lines.get(2));
    }

    @Test
    void groupNamesAreJoinedInOrder() {
        var rule = rule(RuleCheck.CENTERED_X, CheckSettings.NONE, List.of("capitals", "all", "digits"), List.of());
        assertEquals("- Checks: capitals, all glyphs, digits.", RuleText.describe(rule).get(2));
    }

    @Test
    void ruleWithoutExceptionsHasThreeLines() {
        assertEquals(3, RuleText.describe(rule(RuleCheck.CENTERED_X, CheckSettings.NONE)).size());
    }

    @Test
    void exceptionNamesItsSourceAndReason() {
        var exceptions = List.of(
                new RuleException(GlyphSet.all(), "tiling", "joins its neighbors"),
                new RuleException(GlyphSet.all(), "g, j", "tails drop!"),
                new RuleException(GlyphSet.all(), "U+00B7", "is it narrow?"));
        var lines = RuleText.describe(rule(RuleCheck.CENTERED_X, CheckSettings.NONE, List.of("all"), exceptions));
        assertEquals("- Skips tiling: joins its neighbors.", lines.get(3));
        assertEquals("- Skips g, j: tails drop!", lines.get(4));
        assertEquals("- Skips U+00B7: is it narrow?", lines.get(5));
    }
}
