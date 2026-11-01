package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ColumnPanelTest {

    @Test
    void keepsFullScaleWhenColumnsFit() {
        assertEquals(8, ColumnPanel.fitScale(new int[] {192, 216, 216, 120}, 12, 1163));
    }

    @Test
    void shrinksOnlyTheBigViewWhenCrowded() {
        int[] labels = {96, 96, 96, 96, 96, 96, 96};
        int scale = ColumnPanel.fitScale(labels, 12, 900);
        assertTrue(scale < 8 && scale >= 1);
    }

    @Test
    void heightLimitShrinksOnlyTheBigView() {
        assertEquals(8, ColumnPanel.fitScaleForHeight(ColumnPanel.contentHeight(8) + 2 * ColumnPanel.BORDER));
        assertTrue(ColumnPanel.fitScaleForHeight(ColumnPanel.contentHeight(8) + 2 * ColumnPanel.BORDER - 1) < 8);
        assertEquals(1, ColumnPanel.fitScaleForHeight(0));
    }

    @Test
    void framedGapLeavesRoomForBothBorders() {
        assertEquals(ColumnPanel.BORDER, ColumnPanel.GAP);
        assertEquals(8, ColumnPanel.fitScale(new int[] {192, 216, 216, 120}, ColumnPanel.GAP, 1163 - 2 * ColumnPanel.BORDER));
    }

    @Test
    void fiveFramedColumnsKeepFullScaleAtWindowWidth() {
        // ORIGINAL, "1 A" / NEAREST, "2 B" / SCALE2X, "3 C" / FILL, "4 D" / TRIM header widths
        int[] labels = {8 * 24, 7 * 24, 7 * 24, 4 * 24, 4 * 24};
        int rightPanelStart = 440;
        int available = 1600 - 12 - rightPanelStart - 2 * ColumnPanel.BORDER;
        assertEquals(8, ColumnPanel.fitScale(labels, ColumnPanel.GAP, available));
    }

    @Test
    void contentHeightIsWholeTextCellsAndGrowsWithScale() {
        assertEquals(336, ColumnPanel.contentHeight(8));
        assertEquals(0, ColumnPanel.contentHeight(3) % Painter.TEXT_SIZE);
        assertTrue(ColumnPanel.contentHeight(8) > ColumnPanel.contentHeight(2));
    }

    @Test
    void roundsPixelsUpToWholeTextCells() {
        assertEquals(0, Painter.roundUpToCells(0));
        assertEquals(24, Painter.roundUpToCells(1));
        assertEquals(24, Painter.roundUpToCells(24));
        assertEquals(48, Painter.roundUpToCells(25));
    }

    // ORIGINAL, NEAREST, SCALE2X, FILL, TRIM, MMPX, SFX, LIGHT header widths
    private static final int[] HEADER_WIDTHS = {8 * 24, 7 * 24, 7 * 24, 4 * 24, 4 * 24, 4 * 24, 3 * 24, 5 * 24};
    private static final int WINDOW_WIDTH = 1900;
    private static final int WINDOW_HEIGHT = 1000;
    private static final int MARGIN = 12;
    private static final int DETAIL_LEFT = 504;
    // MARGIN + two header lines + MARGIN + the selected-glyph line, as WorkbenchApp lays them out
    private static final int COLUMNS_TOP = 96;
    private static final int BELOW_COLUMNS = MARGIN + Painter.TEXT_SIZE + SamplePanel.outerHeight(4)
        + MARGIN + 3 * Painter.TEXT_SIZE + MARGIN;

    private static ColumnPanel.Plan planAtWindow(int[] labels) {
        int usable = WINDOW_WIDTH - MARGIN - DETAIL_LEFT - 2 * ColumnPanel.BORDER;
        int maxOuterHeight = WINDOW_HEIGHT - COLUMNS_TOP - BELOW_COLUMNS;
        return ColumnPanel.plan(labels, ColumnPanel.GAP, usable, maxOuterHeight);
    }

    @Test
    void eightColumnsKeepTheBigViewAtLeastFourTimesAtWindowSize() {
        ColumnPanel.Plan plan = planAtWindow(HEADER_WIDTHS);
        assertEquals(1, plan.rows());
        assertTrue(plan.scale() >= ColumnPanel.MIN_SCALE);
        assertTrue(ColumnPanel.UNIT * plan.scale() >= 64);
    }

    @Test
    void twoToEightColumnsFitInsideTheWindowAboveTheContextRow() {
        int maxOuterHeight = WINDOW_HEIGHT - COLUMNS_TOP - BELOW_COLUMNS;
        for (int count = 2; count <= 8; count++) {
            int[] labels = Arrays.copyOf(HEADER_WIDTHS, count);
            ColumnPanel.Plan plan = planAtWindow(labels);
            assertTrue(plan.scale() >= ColumnPanel.MIN_SCALE, count + " columns");
            assertTrue(ColumnPanel.framedHeight(plan) <= maxOuterHeight, count + " columns too tall");
            for (int first = 0; first < count; first += plan.perRow()) {
                int[] row = Arrays.copyOfRange(labels, first, Math.min(count, first + plan.perRow()));
                int right = DETAIL_LEFT + ColumnPanel.framedWidth(row, ColumnPanel.GAP, plan.scale());
                assertTrue(right <= WINDOW_WIDTH - MARGIN, count + " columns too wide");
            }
        }
    }

    @Test
    void wrapsOnlyWhenItGivesALargerBigView() {
        int[] labels = Arrays.copyOf(HEADER_WIDTHS, 8);
        ColumnPanel.Plan roomy = ColumnPanel.plan(labels, ColumnPanel.GAP, 700, 800);
        assertEquals(2, roomy.rows());
        assertEquals(4, roomy.perRow());
        assertEquals(6, roomy.scale());
        ColumnPanel.Plan flat = ColumnPanel.plan(labels, ColumnPanel.GAP, 700, 364);
        assertEquals(1, flat.rows());
        assertEquals(8, flat.perRow());
    }

    @Test
    void singleColumnNeverWraps() {
        ColumnPanel.Plan plan = ColumnPanel.plan(new int[] {500}, ColumnPanel.GAP, 10, 1000);
        assertEquals(1, plan.rows());
        assertEquals(1, plan.perRow());
    }

    @Test
    void neverGoesBelowOne() {
        assertEquals(1, ColumnPanel.fitScale(new int[] {500, 500, 500}, 12, 100));
    }
}
