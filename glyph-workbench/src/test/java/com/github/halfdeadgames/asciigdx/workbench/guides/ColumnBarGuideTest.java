package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ColumnBarGuideTest {

    private static final String ALL_KINDS = """
        row CAP 1 all
        column C 7 uppercase,digits
        bar MID 6 1 lowercase
        colbar STEM 3 2 uppercase,digits
        colbar WIDE 0 4 all
        """;

    private static String badLineMessage(String text) {
        return assertThrows(IllegalArgumentException.class, () -> PaintGuides.parse(text)).getMessage();
    }

    private static PaintGuides.Listed only(PaintGuides guides) {
        List<PaintGuides.Listed> all = guides.allGuides();
        assertEquals(1, all.size());
        return all.get(0);
    }

    @Test
    void columnBarLineParses() {
        PaintGuides guides = PaintGuides.parse("colbar STEM 3 2 uppercase\n");
        PaintGuides.Listed listed = only(guides);
        assertEquals(PaintGuides.ListedKind.COLUMN_BAR, listed.kind());
        assertEquals("STEM", listed.label());
        assertEquals(3, listed.position());
        assertEquals(2, listed.rowCount());
        assertEquals("uppercase", listed.groups());
        assertEquals(1, guides.columnBarCount());
    }

    @Test
    void centerColumnIsTheMiddleOfTheBand() {
        assertEquals(8f, new PaintGuides.ColumnBarGuide("A", 7, 2).centerColumn());
        assertEquals(3.5f, new PaintGuides.ColumnBarGuide("A", 3, 1).centerColumn());
    }

    @Test
    void allFourKindsRoundTripAndColumnBarsComeLast() {
        PaintGuides guides = PaintGuides.parse(ALL_KINDS);
        assertEquals(ALL_KINDS, guides.toText());
        assertEquals(ALL_KINDS, PaintGuides.parse(guides.toText()).toText());
        List<PaintGuides.ListedKind> kinds = guides.allGuides().stream().map(PaintGuides.Listed::kind).toList();
        assertEquals(List.of(PaintGuides.ListedKind.ROW, PaintGuides.ListedKind.COLUMN, PaintGuides.ListedKind.BAR,
            PaintGuides.ListedKind.COLUMN_BAR, PaintGuides.ListedKind.COLUMN_BAR), kinds);
    }

    @Test
    void badColumnBarLinesNameTheLine() {
        assertTrue(badLineMessage("colbar A 1 all\n").startsWith("Line 1: expected 'colbar"));
        assertTrue(badLineMessage("row A 1 all\ncolbar A x 2 all\n").startsWith("Line 2: bad number 'x'"));
        assertTrue(badLineMessage("colbar A 1 two all\n").startsWith("Line 1: bad number 'two'"));
        assertTrue(badLineMessage("\ncolbar A 1 0 all\n").startsWith("Line 2: '0' must be at least 1"));
        assertTrue(badLineMessage("colbar A -1 2 all\n").contains("must be at least 0"));
        assertTrue(badLineMessage("stripe A 1 all\n").contains("'colbar'"));
    }

    @Test
    void newColumnBarIsCenteredAndTwoWide() {
        PaintGuides.Listed sixteen = only(PaintGuides.NONE.withColumnBarAdded(16));
        assertEquals(7, sixteen.position());
        assertEquals(2, sixteen.rowCount());
        assertEquals("NEW", sixteen.label());
        assertEquals("all", sixteen.groups());
        assertEquals(PaintGuides.ListedKind.COLUMN_BAR, sixteen.kind());
    }

    @Test
    void newColumnBarInAnOddCellSitsOneColumnLeftOfTheMiddle() {
        PaintGuides.Listed nine = only(PaintGuides.NONE.withColumnBarAdded(9));
        assertEquals(3, nine.position());
        assertEquals(2, nine.rowCount());
    }

    @Test
    void newColumnBarInANarrowCellIsOneColumnAtZero() {
        PaintGuides.Listed one = only(PaintGuides.NONE.withColumnBarAdded(1));
        assertEquals(0, one.position());
        assertEquals(1, one.rowCount());
        PaintGuides.Listed two = only(PaintGuides.NONE.withColumnBarAdded(2));
        assertEquals(0, two.position());
        assertEquals(2, two.rowCount());
    }

    @Test
    void movingClampsTheWholeBandInsideTheCell() {
        PaintGuides guides = PaintGuides.parse("colbar A 3 2 all\n");
        assertEquals(4, only(guides.withGuideMoved(0, 1, 8, 8)).position());
        assertEquals(6, only(guides.withGuideMoved(0, 50, 8, 8)).position());
        assertEquals(0, only(guides.withGuideMoved(0, -50, 8, 8)).position());
        assertEquals(2, only(guides.withGuideMoved(0, 50, 8, 8)).rowCount());
    }

    @Test
    void listTextShowsFirstPlusCount() {
        GuideListModel model = new GuideListModel(PaintGuides.parse(ALL_KINDS), 8, 8);
        assertEquals("COLBAR STEM 3+2", model.row(3, true).text());
        assertEquals("COLBAR WIDE 0+4", model.row(4, true).text());
        assertEquals("BAR MID 6+1", model.row(2, true).text());
    }

    @Test
    void addingSelectsTheNewColumnBarAfterTheOthers() {
        GuideListModel model = new GuideListModel(PaintGuides.parse(ALL_KINDS), 16, 8);
        model.addColumnBar();
        assertEquals(5, model.selectedIndex());
        assertEquals(6, model.size());
        assertEquals(PaintGuides.ListedKind.COLUMN_BAR, model.selectedGuide().kind());
        assertEquals(7, model.selectedGuide().position());
        assertEquals("COLBAR NEW 7+2", model.row(5, true).text());
    }

    @Test
    void addingToAnEmptyListSelectsIndexZero() {
        GuideListModel model = new GuideListModel(PaintGuides.NONE, 16, 16);
        model.addColumnBar();
        assertEquals(0, model.selectedIndex());
        assertEquals(1, model.size());
    }

    @Test
    void bracketKeysMoveTheSelectedColumnBar() {
        GuideListModel model = new GuideListModel(PaintGuides.NONE, 16, 16);
        model.addColumnBar();
        model.adjust(-1);
        assertEquals(6, model.selectedGuide().position());
        model.adjust(100);
        assertEquals(14, model.selectedGuide().position());
    }

    @Test
    void removingDropsOnlyTheChosenColumnBar() {
        PaintGuides guides = PaintGuides.parse(ALL_KINDS).withoutGuide(3);
        assertEquals(1, guides.columnBarCount());
        assertEquals("WIDE", guides.allGuides().get(3).label());
        assertEquals(1, guides.rowCount());
        assertEquals(1, guides.barCount());
    }

    @Test
    void relabelingKeepsThePositionAndGroups() {
        PaintGuides.Listed relabeled = PaintGuides.parse(ALL_KINDS).withGuideRelabeled(3, "BODY").allGuides().get(3);
        assertEquals("BODY", relabeled.label());
        assertEquals(3, relabeled.position());
        assertEquals(2, relabeled.rowCount());
        assertEquals("uppercase,digits", relabeled.groups());
    }

    @Test
    void togglingAGroupWorksAndTheLastNameGoesBackToAll() {
        PaintGuides guides = PaintGuides.parse("colbar A 1 2 uppercase\n");
        List<String> defined = List.of("uppercase", "lowercase");
        assertEquals("uppercase,lowercase", only(guides.withGroupToggled(0, "lowercase", defined)).groups());
        assertEquals("all", only(guides.withGroupToggled(0, "uppercase", defined)).groups());
        assertSame(guides, guides.withGroupToggled(1, "uppercase", defined));
    }

    @Test
    void shownKeepsOnlyColumnBarsOfTheGroupThatFitTheCellWidth() {
        PaintGuides guides = PaintGuides.parse(ALL_KINDS);
        List<String> upper = guides.forGlyph(FourGroupRules.UPPER, FourGroupRules.RULES, 8, 8).columnBars().stream()
            .map(PaintGuides.ColumnBarGuide::label).toList();
        assertEquals(List.of("STEM", "WIDE"), upper);
        List<String> lower = guides.forGlyph(FourGroupRules.LOWER, FourGroupRules.RULES, 8, 8).columnBars().stream()
            .map(PaintGuides.ColumnBarGuide::label).toList();
        assertEquals(List.of("WIDE"), lower);
        List<String> narrow = guides.forGlyph(FourGroupRules.UPPER, FourGroupRules.RULES, 4, 8).columnBars().stream()
            .map(PaintGuides.ColumnBarGuide::label).toList();
        assertEquals(List.of("WIDE"), narrow);
        assertTrue(guides.forGlyph(FourGroupRules.UPPER, FourGroupRules.RULES, 3, 8).columnBars().isEmpty());
        assertFalse(guides.isEmpty());
    }
}
