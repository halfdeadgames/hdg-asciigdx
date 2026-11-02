package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintLayoutTest {

    @Test
    void panelsFillTheScreenWithoutOverlap() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 8);
        assertEquals(19, result.grid().x());
        assertEquals(41 - 1, result.messages().bottom());
        assertEquals(result.messages().y(), result.grid().bottom());
        assertEquals(result.messages().y(), result.note().bottom());
        assertEquals(result.preview().bottom(), result.note().y());
        assertEquals(79, result.preview().x() + result.preview().width());
        assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
        assertEquals(result.grid().x(), result.messages().x());
    }

    @Test
    void previewHeightGrowsWithTheGlyphHeight() {
        assertEquals(6, PaintLayout.previewHeight(8));
        assertTrue(PaintLayout.previewHeight(32) > PaintLayout.previewHeight(16));
        // 1x and 2x blocks plus two label rows must fit inside the borders
        for (int height : new int[] {8, 16, 24, 32, 40}) {
            int content = 24 + height + 12 + 24 + 2 * height;
            assertTrue((PaintLayout.previewHeight(height) - 2) * 24 >= content);
        }
    }

    @Test
    void narrowWindowsKeepAMinimumGrid() {
        PaintLayout.Result result = PaintLayout.compute(50, 30, 18, 8);
        assertTrue(result.grid().width() >= 20);
        assertTrue(result.note().height() >= 6);
    }
}
