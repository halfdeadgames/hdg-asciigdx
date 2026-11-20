package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EighthBlocksThumbTest {
    private static final BarOrientation VERTICAL = BarOrientation.VERTICAL;
    private static final BarOrientation HORIZONTAL = BarOrientation.HORIZONTAL;

    @Test
    void verticalThumbTopEndIsALowerBlockInThumbColor() {
        // Thumb covers eighths 3 to 20: the top cell holds its last 5 eighths, at the bottom.
        assertEquals(0x2585, EighthBlocks.thumbGlyphForCell(VERTICAL, 3, 20, 0));
        assertFalse(EighthBlocks.isTrackForeground(VERTICAL, 3, 20, 0));
    }

    @Test
    void verticalThumbBottomEndIsALowerBlockOfTheTrackPart() {
        // The bottom cell holds the first 4 eighths of the thumb at its top, so the 4 track
        // eighths below it are the lower block, drawn in track color on thumb background.
        assertEquals(0x2584, EighthBlocks.thumbGlyphForCell(VERTICAL, 3, 20, 2));
        assertTrue(EighthBlocks.isTrackForeground(VERTICAL, 3, 20, 2));
    }

    @Test
    void horizontalThumbRightEndIsALeftBlockInThumbColor() {
        assertEquals(0x258C, EighthBlocks.thumbGlyphForCell(HORIZONTAL, 3, 20, 2));
        assertFalse(EighthBlocks.isTrackForeground(HORIZONTAL, 3, 20, 2));
    }

    @Test
    void horizontalThumbLeftEndIsALeftBlockOfTheTrackPart() {
        // The left cell holds the thumb in its right 5 eighths, so the 3 track eighths before it
        // are the left block, drawn in track color on thumb background.
        assertEquals(0x258D, EighthBlocks.thumbGlyphForCell(HORIZONTAL, 3, 20, 0));
        assertTrue(EighthBlocks.isTrackForeground(HORIZONTAL, 3, 20, 0));
    }

    @Test
    void cellsInsideAndOutsideTheThumbAreFullOrEmpty() {
        for (BarOrientation orientation : BarOrientation.values()) {
            assertEquals(EighthBlocks.FULL_GLYPH,
                    EighthBlocks.thumbGlyphForCell(orientation, 3, 20, 1));
            assertEquals(EighthBlocks.EMPTY_GLYPH,
                    EighthBlocks.thumbGlyphForCell(orientation, 3, 20, 3));
            assertFalse(EighthBlocks.isTrackForeground(orientation, 3, 20, 1));
            assertFalse(EighthBlocks.isTrackForeground(orientation, 3, 20, 3));
        }
    }

    @Test
    void alignedThumbsHaveNoPartialCells() {
        assertEquals(EighthBlocks.FULL_GLYPH,
                EighthBlocks.thumbGlyphForCell(VERTICAL, 8, 24, 1));
        assertEquals(EighthBlocks.FULL_GLYPH,
                EighthBlocks.thumbGlyphForCell(VERTICAL, 8, 24, 2));
        assertEquals(EighthBlocks.EMPTY_GLYPH,
                EighthBlocks.thumbGlyphForCell(VERTICAL, 8, 24, 0));
        assertEquals(EighthBlocks.EMPTY_GLYPH,
                EighthBlocks.thumbGlyphForCell(VERTICAL, 8, 24, 3));
    }

    @Test
    void aOneCellThumbSplitsAcrossTwoCells() {
        // Thumb of 8 eighths starting at 5: 3 eighths in cell 0, 5 in cell 1.
        assertEquals(0x2583, EighthBlocks.thumbGlyphForCell(VERTICAL, 5, 13, 0));
        assertFalse(EighthBlocks.isTrackForeground(VERTICAL, 5, 13, 0));
        assertEquals(0x2583, EighthBlocks.thumbGlyphForCell(VERTICAL, 5, 13, 1));
        assertTrue(EighthBlocks.isTrackForeground(VERTICAL, 5, 13, 1));
    }
}
