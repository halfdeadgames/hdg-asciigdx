package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuideResizeTest {

    private static final int CELL = 8;
    private static final String TEXT = """
        row CAP 1 all
        column C 7 all
        bar BAR 6 1 all
        colbar STEM 7 2 all
        """;

    private static final int ROW_INDEX = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int BAR_INDEX = 2;
    private static final int COLUMN_BAR_INDEX = 3;

    private static GuideListModel modelSelecting(int index) {
        GuideListModel model = new GuideListModel(PaintGuides.parse(TEXT), CELL, CELL);
        model.select(index);
        return model;
    }

    @Test
    void barGrowsAndShrinksKeepingItsFirstRow() {
        PaintGuides guides = PaintGuides.parse(TEXT);
        PaintGuides.Listed grown = guides.withGuideResized(BAR_INDEX, 1, CELL, CELL).allGuides().get(BAR_INDEX);
        assertEquals(6, grown.position());
        assertEquals(2, grown.rowCount());
        PaintGuides.Listed shrunk = guides.withGuideResized(BAR_INDEX, 1, CELL, CELL)
            .withGuideResized(BAR_INDEX, -1, CELL, CELL).allGuides().get(BAR_INDEX);
        assertEquals(1, shrunk.rowCount());
    }

    @Test
    void columnBarGrowsAndShrinksKeepingItsFirstColumn() {
        PaintGuides guides = PaintGuides.parse(TEXT);
        PaintGuides.Listed grown = guides.withGuideResized(COLUMN_BAR_INDEX, -1, CELL, CELL)
            .allGuides().get(COLUMN_BAR_INDEX);
        assertEquals(7, grown.position());
        assertEquals(1, grown.rowCount());
        PaintGuides wide = PaintGuides.parse("colbar W 2 2 all\n").withGuideResized(0, 1, CELL, CELL);
        assertEquals(2, wide.allGuides().get(0).position());
        assertEquals(3, wide.allGuides().get(0).rowCount());
    }

    @Test
    void sizeStopsAtOne() {
        PaintGuides guides = PaintGuides.parse(TEXT);
        assertSame(guides, guides.withGuideResized(BAR_INDEX, -1, CELL, CELL));
        PaintGuides single = guides.withGuideResized(COLUMN_BAR_INDEX, -1, CELL, CELL);
        assertSame(single, single.withGuideResized(COLUMN_BAR_INDEX, -1, CELL, CELL));
    }

    @Test
    void barStopsAtTheCellEdge() {
        PaintGuides guides = PaintGuides.parse(TEXT).withGuideResized(BAR_INDEX, 1, CELL, CELL);
        assertEquals(2, guides.allGuides().get(BAR_INDEX).rowCount());
        assertSame(guides, guides.withGuideResized(BAR_INDEX, 1, CELL, CELL));
    }

    @Test
    void columnBarStopsAtTheCellEdge() {
        PaintGuides guides = PaintGuides.parse("colbar W 6 2 all\n");
        assertSame(guides, guides.withGuideResized(0, 1, CELL, CELL));
        PaintGuides inner = PaintGuides.parse("colbar W 5 2 all\n").withGuideResized(0, 1, CELL, CELL);
        assertEquals(3, inner.allGuides().get(0).rowCount());
        assertSame(inner, inner.withGuideResized(0, 1, CELL, CELL));
    }

    @Test
    void solidGuidesAreNotResizable() {
        PaintGuides guides = PaintGuides.parse(TEXT);
        assertSame(guides, guides.withGuideResized(ROW_INDEX, 1, CELL, CELL));
        assertSame(guides, guides.withGuideResized(COLUMN_INDEX, 1, CELL, CELL));
        assertFalse(modelSelecting(ROW_INDEX).canResizeSelected());
        assertFalse(modelSelecting(COLUMN_INDEX).canResizeSelected());
        assertTrue(modelSelecting(BAR_INDEX).canResizeSelected());
        assertTrue(modelSelecting(COLUMN_BAR_INDEX).canResizeSelected());
    }

    @Test
    void listTextShowsTheNewSize() {
        GuideListModel model = modelSelecting(BAR_INDEX);
        model.resizeSelected(1);
        assertEquals("BAR BAR 6+2", model.row(BAR_INDEX).text());
        model.select(COLUMN_BAR_INDEX);
        model.resizeSelected(-1);
        assertEquals("COLBAR STEM 7+1", model.row(COLUMN_BAR_INDEX).text());
    }

    @Test
    void sizeSurvivesTheGuidesTextRoundTrip() {
        GuideListModel model = modelSelecting(BAR_INDEX);
        model.resizeSelected(1);
        PaintGuides reread = PaintGuides.parse(model.guides().toText());
        assertEquals(2, reread.allGuides().get(BAR_INDEX).rowCount());
        assertEquals(model.guides().toText(), reread.toText());
    }

    @Test
    void onlyARealResizeCountsAsAnEdit() {
        GuideListModel model = modelSelecting(BAR_INDEX);
        model.resizeSelected(-1);
        assertFalse(model.isChanged());
        model.resizeSelected(1);
        assertTrue(model.isChanged());
        model.resizeSelected(-1);
        assertFalse(model.isChanged());
    }
}
