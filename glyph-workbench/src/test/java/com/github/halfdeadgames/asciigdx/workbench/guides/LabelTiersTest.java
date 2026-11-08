package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LabelTiersTest {

    @Test
    void widthGuidesAloneAlternateTiersByPosition() {
        LabelTiers.Tiered tiered = LabelTiers.assign(List.of(new PaintGuides.WidthGuide("A", 1, 0),
            new PaintGuides.WidthGuide("B", 3, 0), new PaintGuides.WidthGuide("C", 5, 0)), List.of());
        assertEquals(0, tiered.widths().get(0).tier());
        assertEquals(1, tiered.widths().get(1).tier());
        assertEquals(0, tiered.widths().get(2).tier());
        assertTrue(tiered.columnBars().isEmpty());
    }

    @Test
    void aColumnGuideAndAColumnBarSideBySideGetDifferentTiers() {
        LabelTiers.Tiered tiered = LabelTiers.assign(List.of(new PaintGuides.WidthGuide("A", 4, 0)),
            List.of(new PaintGuides.ColumnBarGuide("B", 4, 2)));
        assertEquals(0, tiered.widths().get(0).tier());
        assertEquals(1, tiered.columnBars().get(0).tier());
    }

    @Test
    void aColumnBarIsOrderedByTheMiddleOfItsBand() {
        LabelTiers.Tiered tiered = LabelTiers.assign(
            List.of(new PaintGuides.WidthGuide("A", 6, 0), new PaintGuides.WidthGuide("C", 9, 0)),
            List.of(new PaintGuides.ColumnBarGuide("B", 3, 4)));
        // The band starts at column 3 but its middle is column 5, so it comes before both guides.
        assertEquals(0, tiered.columnBars().get(0).tier());
        assertEquals(1, tiered.widths().get(0).tier());
        assertEquals(0, tiered.widths().get(1).tier());
    }

    @Test
    void aColumnBarStartingBeforeAGuideCanStillComeAfterIt() {
        LabelTiers.Tiered tiered = LabelTiers.assign(List.of(new PaintGuides.WidthGuide("A", 5, 0)),
            List.of(new PaintGuides.ColumnBarGuide("B", 4, 4)));
        // The middle of the band is column 6, after the guide at column 5.
        assertEquals(0, tiered.widths().get(0).tier());
        assertEquals(1, tiered.columnBars().get(0).tier());
    }

    @Test
    void eachListKeepsItsOwnOrder() {
        LabelTiers.Tiered tiered = LabelTiers.assign(List.of(),
            List.of(new PaintGuides.ColumnBarGuide("LATE", 6, 2), new PaintGuides.ColumnBarGuide("EARLY", 0, 2)));
        assertEquals("LATE", tiered.columnBars().get(0).label());
        assertEquals(1, tiered.columnBars().get(0).tier());
        assertEquals("EARLY", tiered.columnBars().get(1).label());
        assertEquals(0, tiered.columnBars().get(1).tier());
    }

    @Test
    void forGlyphSharesTheTiersBetweenColumnGuidesAndColumnBars() {
        PaintGuides guides = PaintGuides.parse("""
            column W 4 all
            colbar BAND 3 2 all
            """);
        PaintGuides.Shown shown = guides.forGlyph(FourGroupRules.UPPER, FourGroupRules.RULES, 8, 8);
        assertEquals(0, shown.widths().get(0).tier());
        assertEquals(1, shown.columnBars().get(0).tier());
    }
}
