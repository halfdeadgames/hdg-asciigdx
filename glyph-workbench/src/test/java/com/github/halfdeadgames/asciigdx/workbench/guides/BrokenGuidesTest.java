package com.github.halfdeadgames.asciigdx.workbench.guides;

import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.RULES;
import static com.github.halfdeadgames.asciigdx.workbench.guides.FourGroupRules.UPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrokenGuidesTest {

    private static final String GUIDES = """
        row    TOP    1 all
        row    CAP    2 symbols
        column MID    4 uppercase
        bar    BAR    1 2 uppercase,extra,odd
        colbar CB     1 2 nothing
        """;

    private static final RulesFile WITH_SYMBOLS = RulesFile.parse("""
        {"groups": {"uppercase": ["A-Z"], "symbols": ["!-/"]}}
        """, 8, 8);

    private static PaintGuides guides() {
        return PaintGuides.parse(GUIDES);
    }

    @Test
    void onlyBrokenGuidesAreReportedInListOrder() {
        List<BrokenGuides.Broken> broken = BrokenGuides.find(guides(), RULES);

        assertEquals(List.of("CAP", "BAR", "CB"), broken.stream().map(BrokenGuides.Broken::label).toList());
    }

    @Test
    void severalUndefinedNamesAreJoinedInTheMessage() {
        List<BrokenGuides.Broken> broken = BrokenGuides.find(guides(), RULES);

        assertEquals("Guide CAP: unknown group symbols", broken.get(0).message());
        assertEquals("Guide BAR: unknown group extra, odd", broken.get(1).message());
    }

    @Test
    void aFontWithoutGroupsReportsNone() {
        assertTrue(BrokenGuides.find(guides(), RulesFile.empty()).isEmpty());
        assertTrue(BrokenGuides.find(guides(), RulesFile.parse("{}", 8, 8)).isEmpty());
    }

    @Test
    void allAndDefinedNamesReportNone() {
        PaintGuides fine = PaintGuides.parse("row A 1 all\nrow B 2 uppercase,digits\n");

        assertTrue(BrokenGuides.find(fine, RULES).isEmpty());
    }

    @Test
    void shownGuidesFlagEachKindOfBrokenGuide() {
        PaintGuides.Shown shown = guides().forGlyph(UPPER, RULES, 8, 8);

        assertFalse(shown.isRowBroken(0));
        assertTrue(shown.isRowBroken(1));
        assertFalse(shown.isWidthBroken(0));
        assertTrue(shown.isBarBroken(0));
        assertTrue(shown.isColumnBarBroken(0));
    }

    @Test
    void shownFlagsFollowTheSortedWidthGuides() {
        PaintGuides sorted = PaintGuides.parse("column LATE 6 nothing\ncolumn EARLY 2 all\n");
        PaintGuides.Shown shown = sorted.forGlyph(UPPER, RULES, 8, 8);

        assertEquals("EARLY", shown.widths().get(0).label());
        assertFalse(shown.isWidthBroken(0));
        assertTrue(shown.isWidthBroken(1));
    }

    @Test
    void flagsClearOnceTheRulesDefineTheGroup() {
        PaintGuides.Shown shown = guides().forGlyph(UPPER, WITH_SYMBOLS, 8, 8);

        assertFalse(shown.isRowBroken(1));
        assertTrue(shown.isBarBroken(0));
        assertEquals(List.of("BAR", "CB"),
            BrokenGuides.find(guides(), WITH_SYMBOLS).stream().map(BrokenGuides.Broken::label).toList());
    }

    @Test
    void flagsPastTheEndCountAsNotBroken() {
        PaintGuides.Shown shown = new PaintGuides.Shown(List.of(), List.of(), List.of());

        assertFalse(shown.isRowBroken(0));
        assertFalse(shown.isColumnBarBroken(3));
    }
}
