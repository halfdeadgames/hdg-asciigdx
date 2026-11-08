package com.github.halfdeadgames.asciigdx.workbench.derive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverlayCheckTest {

    // Ink in columns 1 and 3 of a 6 wide cell.
    private static final PixelGlyph INKED = new PixelGlyph(0x41, PixelGrid.ofRows(List.of(
        ".#.#..", "......")));
    private static final PixelGlyph BLANK = new PixelGlyph(0x43, PixelGrid.blank(6, 2));

    @Test
    void validShiftHasNoWarningAndReportsTheRange() {
        OverlayCheck.Result result = OverlayCheck.check(false, INKED, 2);
        assertEquals(OverlayCheck.Problem.NONE, result.problem());
        assertFalse(result.hasWarning());
        assertTrue(result.hasRange());
        assertEquals(-1, result.minShift());
        assertEquals(2, result.maxShift());
        assertEquals("-1..+2", result.rangeText());
    }

    @Test
    void shiftOutsideTheCellNamesTheAllowedRangeAndBlocksFinish() {
        OverlayCheck.Result tooFar = OverlayCheck.check(false, INKED, 3);
        assertEquals(OverlayCheck.Problem.OUTSIDE, tooFar.problem());
        assertEquals("SHIFT +3 PUTS INK OUTSIDE THE CELL: ALLOWED -1..+2", tooFar.message());
        assertTrue(tooFar.blocksAdd());
        assertTrue(tooFar.blocksFinish());
        OverlayCheck.Result tooLeft = OverlayCheck.check(false, INKED, -2);
        assertEquals("SHIFT -2 PUTS INK OUTSIDE THE CELL: ALLOWED -1..+2", tooLeft.message());
        assertEquals(OverlayCheck.Problem.NONE, OverlayCheck.check(false, INKED, -1).problem());
    }

    @Test
    void sourceWithoutInkHasNoRangeAndAnyShiftPasses() {
        OverlayCheck.Result result = OverlayCheck.check(false, BLANK, 4);
        assertEquals(OverlayCheck.Problem.NONE, result.problem());
        assertFalse(result.hasRange());
    }

    @Test
    void sourceOnTheTargetBlocksAddingButNotFinishing() {
        OverlayCheck.Result result = OverlayCheck.check(true, INKED, 0);
        assertEquals(OverlayCheck.Problem.SAME_AS_TARGET, result.problem());
        assertEquals("PICK A DIFFERENT GLYPH", result.message());
        assertTrue(result.blocksAdd());
        assertFalse(result.blocksFinish());
    }

    @Test
    void emptySourceSlotBlocksAddingWithAMessage() {
        OverlayCheck.Result result = OverlayCheck.check(false, null, 0);
        assertEquals(OverlayCheck.Problem.EMPTY, result.problem());
        assertEquals("SOURCE SLOT IS EMPTY: DRAW IT FIRST", result.message());
        assertTrue(result.blocksAdd());
        assertFalse(result.blocksFinish());
    }

    @Test
    void offGridNamesTheSideWhenAllInkIsBeyondTheGrid() {
        assertEquals(OverlayCheck.OffGrid.RIGHT, OverlayCheck.offGrid(1, 3, 7, 8));
        assertEquals(OverlayCheck.OffGrid.LEFT, OverlayCheck.offGrid(1, 3, -4, 8));
    }

    @Test
    void offGridIsNoneWhileAnyInkIsStillInsideTheGrid() {
        assertEquals(OverlayCheck.OffGrid.NONE, OverlayCheck.offGrid(1, 3, 0, 8));
        assertEquals(OverlayCheck.OffGrid.NONE, OverlayCheck.offGrid(1, 3, 5, 8));
        assertEquals(OverlayCheck.OffGrid.NONE, OverlayCheck.offGrid(1, 3, -3, 8));
    }

    @Test
    void blinkIsOnForTheFirstQuarterSecondOfEachHalfSecond() {
        assertTrue(OverlayCheck.blinkOn(0f));
        assertTrue(OverlayCheck.blinkOn(0.2f));
        assertFalse(OverlayCheck.blinkOn(0.3f));
        assertTrue(OverlayCheck.blinkOn(0.55f));
    }

    @Test
    void warningsUseOnlyUppercaseAsciiAndPunctuationTheFontDraws() {
        for (OverlayCheck.Result result : List.of(
            OverlayCheck.check(false, INKED, 9),
            OverlayCheck.check(true, INKED, 0), OverlayCheck.check(false, null, 0))) {
            assertTrue(result.message().matches("[A-Z0-9 :.+\\-]+"), result.message());
        }
    }
}
