package com.github.halfdeadgames.asciigdx.workbench.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;
import org.junit.jupiter.api.Test;

class PreviewFitTest {

    private static final int GAP = 12;
    private static final int LABEL = 48;
    private static final int TILE_GAP = 24;
    private static final int TALL_ENOUGH = 10_000;

    @Test
    void smallCellsKeepEveryScale() {
        assertEquals(4, PreviewFit.scaleCount(504, TALL_ENOUGH, new GlyphSize(8, 8), GAP, LABEL));
        assertEquals(8, PreviewFit.largestScale(4));
    }

    @Test
    void wideCellsDropTheLargestScale() {
        assertEquals(3, PreviewFit.scaleCount(504, TALL_ENOUGH, new GlyphSize(32, 32), GAP, LABEL));
    }

    @Test
    void tinyPanelStillKeepsOneX() {
        assertEquals(1, PreviewFit.scaleCount(10, 10, new GlyphSize(32, 32), GAP, LABEL));
        assertEquals(1, PreviewFit.tileBlockCount(10, 1, 32, TILE_GAP, LABEL));
    }

    @Test
    void heightCapDropsLargeScales() {
        assertEquals(4, PreviewFit.scaleCount(5000, 128, new GlyphSize(8, 16), GAP, LABEL));
        assertEquals(3, PreviewFit.scaleCount(5000, 100, new GlyphSize(8, 16), GAP, LABEL));
        assertEquals(2, PreviewFit.scaleCount(5000, 60, new GlyphSize(8, 16), GAP, LABEL));
    }

    @Test
    void tileKeepsAllBlocksForSmallCells() {
        assertEquals(3, PreviewFit.tileBlockCount(504, 8, 8, TILE_GAP, LABEL));
    }

    @Test
    void tileDropsRightmostBlocksThatDoNotFit() {
        assertEquals(1, PreviewFit.tileBlockCount(504, 8, 32, TILE_GAP, LABEL));
        assertEquals(2, PreviewFit.tileBlockCount(504, 4, 32, TILE_GAP, LABEL));
    }

    @Test
    void tileOfOneXIsASingleBlock() {
        assertEquals(1, PreviewFit.tileBlockCount(504, 1, 8, TILE_GAP, LABEL));
    }
}
