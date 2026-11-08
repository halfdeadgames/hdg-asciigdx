package com.github.halfdeadgames.asciigdx.workbench.guides;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import org.junit.jupiter.api.Test;

class GuideListRowsTest {

    private static final CellRect AREA = new CellRect(10, 5, 20, 20);
    private static final int LEGEND_ROWS = 6;

    @Test
    void firstRowIsOneBelowTheFrameTop() {
        int visible = GuideListRows.visibleRows(AREA, 0);
        assertEquals(0, GuideListRows.indexAt(AREA, visible, 0, 30, 12, 6));
    }

    @Test
    void lastVisibleRowIsFound() {
        int visible = GuideListRows.visibleRows(AREA, 0);
        assertEquals(17, GuideListRows.indexAt(AREA, visible, 0, 30, 12, 6 + 17));
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 30, 12, 6 + 18));
    }

    @Test
    void scrolledListOffsetsTheIndex() {
        int visible = GuideListRows.visibleRows(AREA, 0);
        assertEquals(14, GuideListRows.indexAt(AREA, visible, 12, 30, 12, 8));
    }

    @Test
    void legendRowsReturnNone() {
        int visible = GuideListRows.visibleRows(AREA, LEGEND_ROWS);
        assertEquals(12, visible);
        assertEquals(11, GuideListRows.indexAt(AREA, visible, 0, 30, 12, 6 + 11));
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 30, 12, 6 + 12));
    }

    @Test
    void outsideThePanelReturnsNone() {
        int visible = GuideListRows.visibleRows(AREA, 0);
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 30, 9, 6));
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 30, 10, 6));
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 30, 29, 6));
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 30, 12, 5));
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 30, 12, 25));
    }

    @Test
    void emptyListReturnsNone() {
        int visible = GuideListRows.visibleRows(AREA, 0);
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 0, 12, 6));
    }

    @Test
    void rowsBelowAShortListReturnNone() {
        int visible = GuideListRows.visibleRows(AREA, 0);
        assertEquals(2, GuideListRows.indexAt(AREA, visible, 0, 3, 12, 8));
        assertEquals(GuideListRows.NONE, GuideListRows.indexAt(AREA, visible, 0, 3, 12, 9));
    }
}
