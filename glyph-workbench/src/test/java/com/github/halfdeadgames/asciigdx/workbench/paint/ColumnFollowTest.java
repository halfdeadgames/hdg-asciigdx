package com.github.halfdeadgames.asciigdx.workbench.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollState;
import org.junit.jupiter.api.Test;

/** The sheet scrolls glyph columns with a {@link ScrollState} that has 16 "rows" and a window of n of them. */
class ColumnFollowTest {

    private static ScrollState window(int visibleColumns) {
        ScrollState columns = new ScrollState(PaintSheetView.COLUMNS);
        columns.setVisibleRows(visibleColumns);
        return columns;
    }

    @Test
    void cursorRightOfTheViewScrollsJustEnough() {
        ScrollState columns = window(6);
        columns.keepVisible(9);
        assertEquals(4, columns.offset());
        assertTrue(columns.isVisible(9));
    }

    @Test
    void cursorLeftOfTheViewScrollsJustEnough() {
        ScrollState columns = window(6);
        columns.keepVisible(15);
        columns.keepVisible(3);
        assertEquals(3, columns.offset());
    }

    @Test
    void visibleCursorLeavesTheOffsetAlone() {
        ScrollState columns = window(6);
        columns.keepVisible(8);
        int before = columns.offset();
        columns.keepVisible(before + 2);
        assertEquals(before, columns.offset());
    }

    @Test
    void lastColumnNeverScrollsPastTheEnd() {
        ScrollState columns = window(6);
        columns.keepVisible(15);
        assertEquals(10, columns.offset());
    }

    @Test
    void wideningClampsSoNoSpaceAppearsOnTheRight() {
        ScrollState columns = window(6);
        columns.keepVisible(15);
        columns.setVisibleRows(10);
        assertEquals(6, columns.offset());
        columns.setVisibleRows(PaintSheetView.COLUMNS);
        assertEquals(0, columns.offset());
        assertFalse(columns.needed());
    }
}
