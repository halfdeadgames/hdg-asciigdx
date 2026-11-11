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
    // Ink in rows 1 and 2 of a 4-high cell, columns 1 and 3 of a 6-wide cell.
    private static final PixelGlyph TALL = new PixelGlyph(0x44, PixelGrid.ofRows(List.of(
        "......", ".#....", "...#..", "......")));

    @Test
    void validShiftHasNoWarningAndReportsTheRange() {
        OverlayCheck.Result result = OverlayCheck.check(false, INKED, 2, 0);
        assertEquals(OverlayCheck.Problem.NONE, result.problem());
        assertFalse(result.hasWarning());
        assertTrue(result.hasRange());
        assertEquals(-1, result.minShift());
        assertEquals(2, result.maxShift());
        assertEquals("-1..+2", result.rangeText());
    }

    @Test
    void shiftOutsideTheCellNamesTheAllowedRangeAndBlocksFinish() {
        OverlayCheck.Result tooFar = OverlayCheck.check(false, INKED, 3, 0);
        assertEquals(OverlayCheck.Problem.OUTSIDE, tooFar.problem());
        assertEquals("SHIFT +3 PUTS INK OUTSIDE THE CELL: ALLOWED -1..+2", tooFar.message());
        assertTrue(tooFar.hasWarning());
        assertTrue(tooFar.blocksFinish());
        OverlayCheck.Result tooLeft = OverlayCheck.check(false, INKED, -2, 0);
        assertEquals("SHIFT -2 PUTS INK OUTSIDE THE CELL: ALLOWED -1..+2", tooLeft.message());
        assertEquals(OverlayCheck.Problem.NONE, OverlayCheck.check(false, INKED, -1, 0).problem());
    }

    @Test
    void downRangeComesFromTheInkRows() {
        OverlayCheck.Result result = OverlayCheck.check(false, TALL, 0, 1);
        assertEquals(OverlayCheck.Problem.NONE, result.problem());
        assertEquals(-1, result.minDown());
        assertEquals(1, result.maxDown());
        assertEquals("-1..+1", result.downRangeText());
    }

    @Test
    void downOutsideTheCellNamesTheAllowedRangeAndBlocksFinish() {
        OverlayCheck.Result tooLow = OverlayCheck.check(false, TALL, 0, 3);
        assertEquals(OverlayCheck.Problem.OUTSIDE, tooLow.problem());
        assertEquals("DOWN +3 PUTS INK OUTSIDE THE CELL: ALLOWED -1..+1", tooLow.message());
        assertTrue(tooLow.blocksFinish());
        OverlayCheck.Result tooHigh = OverlayCheck.check(false, TALL, 0, -2);
        assertEquals("DOWN -2 PUTS INK OUTSIDE THE CELL: ALLOWED -1..+1", tooHigh.message());
        assertEquals(-1, tooHigh.minDown());
        assertEquals(1, tooHigh.maxDown());
    }

    @Test
    void shiftProblemWinsWhenBothShiftAndDownAreOutside() {
        OverlayCheck.Result result = OverlayCheck.check(false, TALL, 9, 9);
        assertTrue(result.message().startsWith("SHIFT +9"), result.message());
    }

    @Test
    void sourceWithoutInkHasNoRangeAndAnyShiftPasses() {
        OverlayCheck.Result result = OverlayCheck.check(false, BLANK, 4, 5);
        assertEquals(OverlayCheck.Problem.NONE, result.problem());
        assertFalse(result.hasRange());
        assertEquals(0, result.minDown());
        assertEquals(0, result.maxDown());
    }

    @Test
    void sourceOnTheTargetBlocksAddingButNotFinishing() {
        OverlayCheck.Result result = OverlayCheck.check(true, INKED, 0, 0);
        assertEquals(OverlayCheck.Problem.SAME_AS_TARGET, result.problem());
        assertEquals("PICK A DIFFERENT GLYPH", result.message());
        assertTrue(result.hasWarning());
        assertFalse(result.blocksFinish());
    }

    @Test
    void emptySourceSlotBlocksAddingWithAMessage() {
        OverlayCheck.Result result = OverlayCheck.check(false, null, 0, 0);
        assertEquals(OverlayCheck.Problem.EMPTY, result.problem());
        assertEquals("SOURCE SLOT IS EMPTY: DRAW IT FIRST", result.message());
        assertTrue(result.hasWarning());
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
    void offGridRowsNamesTheEdgeWhenAllInkIsBeyondTheGrid() {
        assertEquals(OverlayCheck.OffGrid.DOWN, OverlayCheck.offGridRows(1, 3, 7, 8));
        assertEquals(OverlayCheck.OffGrid.DOWN, OverlayCheck.offGridRows(1, 3, 7, 8));
        assertEquals(OverlayCheck.OffGrid.UP, OverlayCheck.offGridRows(1, 3, -4, 8));
    }

    @Test
    void offGridRowsIsNoneWhileAnyInkIsStillInsideTheGrid() {
        assertEquals(OverlayCheck.OffGrid.NONE, OverlayCheck.offGridRows(1, 3, 0, 8));
        assertEquals(OverlayCheck.OffGrid.NONE, OverlayCheck.offGridRows(1, 3, 6, 8));
        assertEquals(OverlayCheck.OffGrid.NONE, OverlayCheck.offGridRows(1, 3, -3, 8));
        assertEquals(OverlayCheck.OffGrid.DOWN, OverlayCheck.offGridRows(1, 3, 7, 8));
        assertEquals(OverlayCheck.OffGrid.UP, OverlayCheck.offGridRows(1, 3, -4, 8));
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
            OverlayCheck.check(false, INKED, 9, 0), OverlayCheck.check(false, TALL, 0, -7),
            OverlayCheck.check(true, INKED, 0, 0), OverlayCheck.check(false, null, 0, 0))) {
            assertTrue(result.message().matches("[A-Z0-9 :.+\\-]+"), result.message());
        }
    }
}
