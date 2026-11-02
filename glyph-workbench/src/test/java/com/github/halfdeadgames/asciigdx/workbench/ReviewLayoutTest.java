package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewLayoutTest {

    @Test
    void rightPanelsStackWithoutGapsAndEndAboveTheStatusBar() {
        ReviewLayout.Result result = ReviewLayout.compute(79, 41, 18);
        assertEquals(19, result.candidates().x());
        assertEquals(ReviewLayout.TOP_ROWS, result.candidates().y());
        assertEquals(result.note().y(), result.candidates().bottom());
        assertEquals(result.messages().y(), result.note().bottom());
        assertEquals(41 - 1, result.messages().bottom());
        assertEquals(79 - 19 - 1, result.candidates().width());
    }

    @Test
    void smallWindowsKeepMinimumSizes() {
        ReviewLayout.Result result = ReviewLayout.compute(30, 20, 18);
        assertTrue(result.candidates().width() >= 20);
        assertTrue(result.candidates().height() >= 6);
    }

    @Test
    void notePanelTextWidthsLeaveRoomForTheScrollbar() {
        assertEquals(35, NotePanel.textChars(40));
        assertEquals(1, NotePanel.textChars(3));
        assertEquals(7, NotePanel.editorRows(9));
    }

    @Test
    void aFailedWriteCanBeUndone() {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 16; y++) {
            rows.add("#" + ".".repeat(15));
        }
        UpscaleTarget target = UpscaleTarget.empty(16, 16);
        UpscaleTarget.Snapshot before = target.snapshot();
        target.put('A', false, PixelGrid.ofRows(rows));
        assertTrue(target.hasGlyph('A'));
        target.restore(before);
        assertFalse(target.hasGlyph('A'));
        assertEquals("cell 16x16\n", target.toText());
    }
}
