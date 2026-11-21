package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class ProgressBarTest {
    private static final int FILL = 0x00FF00FF;
    private static final int EMPTY = 0x202020FF;
    private static final int UNUSED = 0x123456FF;
    private static final ProgressBarStyle STYLE = style();
    private static final ValueRange PERCENT = ValueRange.of(0f, 100f, 1f);

    private final UiSurface surface = UiSurface.ofSize(20, 12);
    private final UiRoot ui = UiRoot.of(surface);

    private static ProgressBarStyle style() {
        return ProgressBarStyle.of(LabelStyle.of(FILL, UNUSED), LabelStyle.of(UNUSED, EMPTY));
    }

    private ProgressBar show(ProgressBar bar, int x, int y) {
        bar.setPosition(x, y);
        ui.getRoot().addChild(bar);
        ui.draw();
        return bar;
    }

    @Test
    void startsAtTheMinimumAndIsNotFocusable() {
        ProgressBar bar = ProgressBar.horizontal(ValueRange.of(10f, 20f, 1f), 5, STYLE);

        assertEquals(10f, bar.getValue());
        assertFalse(bar.isFocusable());
        assertEquals(5, bar.getWidth());
        assertEquals(1, bar.getHeight());
        assertEquals(5, bar.getPreferredLengthCells());
        assertEquals(ValueRange.of(10f, 20f, 1f), bar.getRange());
    }

    @Test
    void horizontalBarFillsFromTheLeftWithAPartialEighth() {
        ProgressBar bar = ProgressBar.horizontal(PERCENT, 10, STYLE);
        bar.setValue(55f);
        show(bar, 2, 3);

        for (int column = 2; column < 7; column++) {
            assertEquals(0x2588, surface.getSquareGrid().getCodePoint(column, 3));
            assertEquals(FILL, surface.getSquareGrid().getForeground(column, 3));
        }
        assertEquals(0x258C, surface.getSquareGrid().getCodePoint(7, 3));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getSquareGrid().getCodePoint(8, 3));
        assertEquals(EMPTY, surface.getSquareGrid().getBackground(11, 3));
        assertEquals(EMPTY, surface.getSquareGrid().getBackground(7, 3));
    }

    @Test
    void verticalBarFillsFromTheBottomUp() {
        ProgressBar bar = ProgressBar.vertical(PERCENT, 4, STYLE);
        bar.setValue(50f);
        show(bar, 1, 2);

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getSquareGrid().getCodePoint(1, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getSquareGrid().getCodePoint(1, 3));
        assertEquals(0x2588, surface.getSquareGrid().getCodePoint(1, 4));
        assertEquals(0x2588, surface.getSquareGrid().getCodePoint(1, 5));
        assertEquals(1, bar.getWidth());
        assertEquals(4, bar.getHeight());
    }

    @Test
    void verticalBarShowsAPartialLowerEighthAtTheBoundary() {
        ProgressBar bar = ProgressBar.vertical(PERCENT, 2, STYLE);
        bar.setValue(25f);
        show(bar, 0, 0);

        assertEquals(0x2584, surface.getSquareGrid().getCodePoint(0, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getSquareGrid().getCodePoint(0, 0));
    }

    @Test
    void barDrawsOnlyInTheSquareGrid() {
        ProgressBar horizontal = ProgressBar.horizontal(PERCENT, 4, STYLE);
        horizontal.setValue(100f);
        show(horizontal, 0, 2);
        ProgressBar vertical = ProgressBar.vertical(PERCENT, 3, STYLE);
        vertical.setValue(100f);
        show(vertical, 10, 4);

        assertEquals(1, horizontal.getHeight());
        assertEquals(3, vertical.getHeight());
        assertEquals(0x2588, surface.getSquareGrid().getCodePoint(3, 2));
        assertEquals(0x2588, surface.getSquareGrid().getCodePoint(10, 6));
        for (int textColumn = 0; textColumn < surface.getTextColumns(); textColumn++) {
            for (int row = 0; row < 12; row++) {
                assertEquals(TerminalGrid.EMPTY_CODE_POINT,
                        surface.getTextGrid().getCodePoint(textColumn, row));
            }
        }
    }

    @Test
    void setValueSnapsClampsAndRejectsNan() {
        ProgressBar bar = ProgressBar.horizontal(ValueRange.of(0f, 10f, 2f), 5, STYLE);

        bar.setValue(5f);
        assertEquals(6f, bar.getValue());
        bar.setValue(99f);
        assertEquals(10f, bar.getValue());
        bar.setValue(Float.NEGATIVE_INFINITY);
        assertEquals(0f, bar.getValue());
        assertThrows(IllegalArgumentException.class, () -> bar.setValue(Float.NaN));
    }

    @Test
    void redrawIsRequestedOnlyWhenTheValueChanges() {
        ProgressBar bar = show(ProgressBar.horizontal(PERCENT, 5, STYLE), 0, 0);
        bar.clearRedrawNeeded();

        bar.setValue(0f);
        assertFalse(bar.isRedrawNeeded());
        bar.setValue(1f);
        assertTrue(bar.isRedrawNeeded());
    }

    @Test
    void styleUsesTheFillForegroundAndTheEmptyBackground() {
        ProgressBarStyle style = style();

        assertEquals(FILL, style.getFill().getForegroundRgba());
        assertEquals(EMPTY, style.getEmpty().getBackgroundRgba());
        assertEquals(style(), style);
    }

    @Test
    void styleRejectsNulls() {
        LabelStyle look = LabelStyle.of(FILL, EMPTY);

        assertThrows(NullPointerException.class, () -> ProgressBarStyle.of(null, look));
        assertThrows(NullPointerException.class, () -> ProgressBarStyle.of(look, null));
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProgressBar.horizontal(PERCENT, 0, STYLE));
        assertThrows(NullPointerException.class, () -> ProgressBar.horizontal(null, 3, STYLE));
        assertThrows(NullPointerException.class, () -> ProgressBar.horizontal(PERCENT, 3, null));
    }
}
