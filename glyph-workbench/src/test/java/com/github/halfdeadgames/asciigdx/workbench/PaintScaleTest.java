package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintScaleTest {

    private static final int[][] FONTS = {{8, 8}, {16, 16}, {8, 16}, {32, 32}};

    private static final int[] EIGHT_BARE_WIDTHS = {336, 608, 888, 1152};
    private static final int[] EIGHT_HEIGHTS = {168, 288, 432, 576};

    @Test
    void eightByEightMinimumsAreExact() {
        for (int factor = 1; factor <= 4; factor++) {
            assertEquals(EIGHT_BARE_WIDTHS[factor - 1], PaintScale.minimumWidth(factor, PaintSheetView.GAP, 8, 8));
            assertEquals(EIGHT_HEIGHTS[factor - 1], PaintScale.minimumHeight(factor, 8));
        }
    }

    @Test
    void eightByEightWindowsPickTheProportionalFactor() {
        assertEquals(2, PaintScale.bestFactor(1900, 1000, PaintSheetView.GAP, 8, 8));
        assertEquals(2, PaintScale.bestFactor(1280, 720, PaintSheetView.GAP, 8, 8));
        assertEquals(2, PaintScale.bestFactor(900, 480, PaintSheetView.GAP, 8, 8));
        assertEquals(1, PaintScale.bestFactor(600, 360, PaintSheetView.GAP, 8, 8));
        assertEquals(3, PaintScale.bestFactor(2560, 1440, PaintSheetView.GAP, 8, 8));
        assertEquals(4, PaintScale.bestFactor(3840, 2160, PaintSheetView.GAP, 8, 8));
    }

    @Test
    void theShorterWindowSideLimitsTheFactor() {
        assertEquals(2, PaintScale.bestFactor(3840, 1000, PaintSheetView.GAP, 8, 8));
    }

    @Test
    void theProportionalTargetRoundsHalfUp() {
        assertEquals(3, PaintScale.bestFactor(2375, 1250, PaintSheetView.GAP, 8, 8));
        assertEquals(2, PaintScale.bestFactor(2374, 1250, PaintSheetView.GAP, 8, 8));
    }

    @Test
    void hugeWindowCapsAtFour() {
        assertEquals(4, PaintScale.bestFactor(100_000, 100_000, PaintSheetView.GAP, 8, 8));
    }

    @Test
    void tinyWindowGivesOne() {
        assertEquals(1, PaintScale.bestFactor(1, 1, PaintSheetView.GAP, 8, 8));
        assertEquals(1, PaintScale.bestFactor(0, 0, PaintSheetView.GAP, 32, 32));
    }

    @Test
    void steppingClampsToTheAllowedRange() {
        assertEquals(3, PaintScale.stepped(2, 1));
        assertEquals(1, PaintScale.stepped(2, -1));
        assertEquals(4, PaintScale.stepped(4, 1));
        assertEquals(1, PaintScale.stepped(1, -1));
    }

    @Test
    void fittingFactorKeepsARequestThatFitsItsBareMinimum() {
        assertEquals(4, PaintScale.fittingFactor(4, 1152, 576, PaintSheetView.GAP, 8, 8));
        assertEquals(2, PaintScale.fittingFactor(2, 1900, 1000, PaintSheetView.GAP, 8, 8));
    }

    @Test
    void fittingFactorDropsToTheLargestBareFit() {
        assertEquals(3, PaintScale.fittingFactor(4, 1151, 1000, PaintSheetView.GAP, 8, 8));
        assertEquals(3, PaintScale.fittingFactor(4, 1900, 575, PaintSheetView.GAP, 8, 8));
        assertEquals(2, PaintScale.fittingFactor(4, 800, 1000, PaintSheetView.GAP, 8, 8));
        assertEquals(1, PaintScale.fittingFactor(4, 1, 1, PaintSheetView.GAP, 8, 8));
    }

    @Test
    void fittingFactorNeverRaisesTheRequest() {
        assertEquals(2, PaintScale.fittingFactor(2, 100_000, 100_000, PaintSheetView.GAP, 8, 8));
    }

    @Test
    void enlargingTheWindowRestoresTheRequestedFactor() {
        assertEquals(2, PaintScale.fittingFactor(4, 800, 1000, PaintSheetView.GAP, 8, 8));
        assertEquals(4, PaintScale.fittingFactor(4, 1900, 1000, PaintSheetView.GAP, 8, 8));
    }

    @Test
    void parseFactorAcceptsOnlyTheAllowedRange() {
        assertEquals(1, PaintScale.parseFactor("1"));
        assertEquals(4, PaintScale.parseFactor(" 4 "));
        assertEquals(0, PaintScale.parseFactor("0"));
        assertEquals(0, PaintScale.parseFactor("5"));
        assertEquals(0, PaintScale.parseFactor("two"));
        assertEquals(0, PaintScale.parseFactor(""));
        assertEquals(0, PaintScale.parseFactor(null));
    }

    @Test
    void labelShowsTheFactorAndAuto() {
        assertEquals("UI 3X AUTO", PaintScale.label(3, true));
        assertEquals("UI 2X", PaintScale.label(2, false));
    }

    @Test
    void largerFactorsNeedLargerWindows() {
        for (int[] font : FONTS) {
            for (int factor = 2; factor <= 4; factor++) {
                assertTrue(PaintScale.minimumWidth(factor, PaintSheetView.GAP, font[0], font[1])
                    > PaintScale.minimumWidth(factor - 1, PaintSheetView.GAP, font[0], font[1]));
                assertTrue(PaintScale.minimumHeight(factor, font[1]) > PaintScale.minimumHeight(factor - 1, font[1]));
            }
        }
    }
}
