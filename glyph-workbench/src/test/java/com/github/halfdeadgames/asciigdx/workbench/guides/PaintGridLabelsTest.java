package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PaintGridLabelsTest {

    // Row labels must fit in y 76..264 (100 - 3 * 8 to 100 + 160 + 4); width labels in x -16..208.
    private static final PaintGridLabels.Frame FRAME = new PaintGridLabels.Frame(0, 100, 10, 200, 160, 8, 4);

    private final PaintGridLabels labels = new PaintGridLabels();

    private static PaintGuides.Shown shown(List<PaintGuides.Guide> rows, List<PaintGuides.WidthGuide> widths,
                                           List<PaintGuides.BarGuide> bars) {
        return new PaintGuides.Shown(rows, widths, bars);
    }

    private static PaintGuides.Shown empty() {
        return shown(List.of(), List.of(), List.of());
    }

    @Test
    void noGuidesAndNoHighlightGiveNoSlots() {
        labels.layout(empty(), null, FRAME);
        assertEquals(-1, labels.highlightRowSlot());
        assertEquals(-1, labels.highlightWidthSlot());
    }

    @Test
    void aRowLabelIsCenteredOnItsGuide() {
        labels.layout(shown(List.of(new PaintGuides.Guide("a", 3)), List.of(), List.of()), null, FRAME);
        assertEquals(126, labels.rowLabelY(0));
    }

    @Test
    void collidingRowLabelsSeparateAroundTheirMean() {
        labels.layout(shown(List.of(new PaintGuides.Guide("a", 3), new PaintGuides.Guide("b", 3)),
            List.of(), List.of()), null, FRAME);
        assertEquals(120, labels.rowLabelY(0));
        assertEquals(132, labels.rowLabelY(1));
    }

    @Test
    void barLabelsShareTheRowColumnAfterTheRowLabels() {
        labels.layout(shown(List.of(new PaintGuides.Guide("a", 3)), List.of(),
            List.of(new PaintGuides.BarGuide("bar", 2, 4))), null, FRAME);
        assertEquals(125, labels.rowLabelY(0));
        assertEquals(137, labels.rowLabelY(1));
    }

    @Test
    void aRowLabelBeyondTheLowerBoundIsClamped() {
        labels.layout(shown(List.of(new PaintGuides.Guide("a", 40)), List.of(), List.of()), null, FRAME);
        assertEquals(256, labels.rowLabelY(0));
    }

    @Test
    void aHighlightMatchingAShownRowReusesItsSlot() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.ROW, "a", 3, 0, "");
        labels.layout(shown(List.of(new PaintGuides.Guide("b", 3), new PaintGuides.Guide("a", 3)),
            List.of(), List.of()), mark, FRAME);
        assertEquals(1, labels.highlightRowSlot());
        assertEquals(-1, labels.highlightWidthSlot());
    }

    @Test
    void aHighlightWithOtherLabelGetsAnExtraRowSlot() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.ROW, "x", 7, 0, "");
        labels.layout(shown(List.of(new PaintGuides.Guide("a", 3)), List.of(), List.of()), mark, FRAME);
        assertEquals(1, labels.highlightRowSlot());
        assertEquals(126, labels.rowLabelY(0));
        assertEquals(166, labels.rowLabelY(1));
    }

    @Test
    void aHighlightMatchingAShownBarReusesItsSlotAfterTheRows() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.BAR, "bar", 2, 4, "");
        labels.layout(shown(List.of(new PaintGuides.Guide("a", 8)), List.of(),
            List.of(new PaintGuides.BarGuide("bar", 2, 4))), mark, FRAME);
        assertEquals(1, labels.highlightRowSlot());
    }

    @Test
    void aBarHighlightNotShownIsPlacedAtTheMiddleOfItsBand() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.BAR, "bar", 2, 4, "");
        labels.layout(empty(), mark, FRAME);
        assertEquals(0, labels.highlightRowSlot());
        assertEquals(136, labels.rowLabelY(0));
    }

    @Test
    void aHighlightWithOtherBandSizeIsNotTakenForAShownBar() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.BAR, "bar", 2, 3, "");
        labels.layout(shown(List.of(), List.of(), List.of(new PaintGuides.BarGuide("bar", 2, 4))), mark, FRAME);
        assertEquals(1, labels.highlightRowSlot());
    }

    @Test
    void aWidthLabelIsCenteredOnItsGuide() {
        labels.layout(shown(List.of(), List.of(new PaintGuides.WidthGuide("W", 5, 0)), List.of()), null, FRAME);
        assertEquals(46, labels.widthLabelX(0));
    }

    @Test
    void longerWidthLabelsAreCenteredByTheirLength() {
        labels.layout(shown(List.of(), List.of(new PaintGuides.WidthGuide("WW", 5, 0)), List.of()), null, FRAME);
        assertEquals(42, labels.widthLabelX(0));
    }

    @Test
    void collidingWidthLabelsOfOneTierSeparate() {
        labels.layout(shown(List.of(), List.of(new PaintGuides.WidthGuide("AA", 5, 0),
            new PaintGuides.WidthGuide("BB", 5, 0)), List.of()), null, FRAME);
        assertEquals(32, labels.widthLabelX(0));
        assertEquals(52, labels.widthLabelX(1));
    }

    @Test
    void widthLabelsOfDifferentTiersDoNotPushEachOther() {
        labels.layout(shown(List.of(), List.of(new PaintGuides.WidthGuide("AA", 5, 0),
            new PaintGuides.WidthGuide("BB", 5, 1)), List.of()), null, FRAME);
        assertEquals(42, labels.widthLabelX(0));
        assertEquals(42, labels.widthLabelX(1));
    }

    @Test
    void aColumnHighlightMatchingAShownWidthReusesItsSlot() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.COLUMN, "W", 5, 0, "");
        labels.layout(shown(List.of(), List.of(new PaintGuides.WidthGuide("W", 5, 1)), List.of()), mark, FRAME);
        assertEquals(0, labels.highlightWidthSlot());
        assertEquals(-1, labels.highlightRowSlot());
    }

    @Test
    void aColumnHighlightNotShownGetsAnExtraSlotInTierZero() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.COLUMN, "C", 5, 0, "");
        labels.layout(empty(), mark, FRAME);
        assertEquals(0, labels.highlightWidthSlot());
        assertEquals(46, labels.widthLabelX(0));
    }

    @Test
    void anExtraColumnHighlightIsSpreadWithTheTierZeroWidthLabels() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.COLUMN, "BB", 5, 0, "");
        labels.layout(shown(List.of(), List.of(new PaintGuides.WidthGuide("AA", 5, 0)), List.of()), mark, FRAME);
        assertEquals(1, labels.highlightWidthSlot());
        assertEquals(32, labels.widthLabelX(0));
        assertEquals(52, labels.widthLabelX(1));
    }

    @Test
    void layingOutAgainReplacesTheEarlierSlots() {
        PaintGuides.Listed mark = new PaintGuides.Listed(PaintGuides.ListedKind.COLUMN, "C", 5, 0, "");
        labels.layout(empty(), mark, FRAME);
        labels.layout(empty(), null, FRAME);
        assertEquals(-1, labels.highlightWidthSlot());
    }
}
