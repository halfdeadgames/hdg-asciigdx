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
    private static final ProgressBarStyle SQUARE = style(UiFont.SQUARE);
    private static final ProgressBarStyle TALL = style(UiFont.TALL);
    private static final ValueRange PERCENT = ValueRange.of(0f, 100f, 1f);

    private final UiSurface surface = UiSurface.ofSize(20, 12);
    private final UiRoot ui = UiRoot.of(surface);

    private static ProgressBarStyle style(UiFont font) {
        return ProgressBarStyle.of(LabelStyle.of(font, FILL, UNUSED),
                LabelStyle.of(font, UNUSED, EMPTY));
    }

    private ProgressBar show(ProgressBar bar, int x, int y) {
        bar.setPosition(x, y);
        ui.getRoot().addChild(bar);
        ui.draw();
        return bar;
    }

    @Test
    void startsAtTheMinimumAndIsNotFocusable() {
        ProgressBar bar = ProgressBar.horizontal(ValueRange.of(10f, 20f, 1f), 5, SQUARE);

        assertEquals(10f, bar.getValue());
        assertFalse(bar.isFocusable());
        assertEquals(5, bar.getWidth());
        assertEquals(1, bar.getHeight());
        assertEquals(5, bar.getPreferredLengthCells());
        assertEquals(ValueRange.of(10f, 20f, 1f), bar.getRange());
    }

    @Test
    void horizontalBarFillsFromTheLeftWithAPartialEighth() {
        ProgressBar bar = ProgressBar.horizontal(PERCENT, 10, SQUARE);
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
        ProgressBar bar = ProgressBar.vertical(PERCENT, 4, SQUARE);
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
        ProgressBar bar = ProgressBar.vertical(PERCENT, 2, SQUARE);
        bar.setValue(25f);
        show(bar, 0, 0);

        assertEquals(0x2584, surface.getSquareGrid().getCodePoint(0, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getSquareGrid().getCodePoint(0, 0));
    }

    @Test
    void tallBarUsesTwoRowsPerCellAndTheTallGrid() {
        ValueRange unit = ValueRange.of(0f, 1f, 0.5f);
        ProgressBar horizontal = ProgressBar.horizontal(unit, 4, TALL);
        horizontal.setValue(1f);
        show(horizontal, 0, 2);
        ProgressBar vertical = ProgressBar.vertical(unit, 3, TALL);
        vertical.setValue(1f);
        show(vertical, 10, 4);

        assertEquals(2, horizontal.getHeight());
        assertEquals(2, horizontal.getRowSnap());
        assertEquals(0x2588, surface.getTallGrid().getCodePoint(3, 1));
        assertEquals(6, vertical.getHeight());
        assertEquals(0x2588, surface.getTallGrid().getCodePoint(10, 2));
        assertEquals(0x2588, surface.getTallGrid().getCodePoint(10, 3));
        assertEquals(0x2588, surface.getTallGrid().getCodePoint(10, 4));
        assertEquals(EMPTY, surface.getSquareGrid().getBackground(10, 9));
    }

    @Test
    void setValueSnapsClampsAndRejectsNan() {
        ProgressBar bar = ProgressBar.horizontal(ValueRange.of(0f, 10f, 2f), 5, SQUARE);

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
        ProgressBar bar = show(ProgressBar.horizontal(PERCENT, 5, SQUARE), 0, 0);
        bar.clearRedrawNeeded();

        bar.setValue(0f);
        assertFalse(bar.isRedrawNeeded());
        bar.setValue(1f);
        assertTrue(bar.isRedrawNeeded());
    }

    @Test
    void squeezedBarClearsItsAreaAndDrawsNoStaleGlyphs() {
        ProgressBar bar = ProgressBar.vertical(PERCENT, 4, TALL);
        bar.setValue(100f);
        show(bar, 0, 0);
        assertEquals(0x2588, surface.getTallGrid().getCodePoint(0, 0));

        bar.setSize(1, 1);
        ui.draw();

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getTallGrid().getCodePoint(0, 0));
        assertEquals(EMPTY, surface.getSquareGrid().getBackground(0, 0));
    }

    @Test
    void styleUsesTheFillForegroundAndTheEmptyBackground() {
        ProgressBarStyle style = style(UiFont.SQUARE);

        assertEquals(FILL, style.getFill().getForegroundRgba());
        assertEquals(EMPTY, style.getEmpty().getBackgroundRgba());
        assertEquals(UiFont.SQUARE, style.getFont());
        assertEquals(style(UiFont.SQUARE), style);
    }

    @Test
    void styleRejectsMixedFontsAndNulls() {
        LabelStyle square = LabelStyle.of(UiFont.SQUARE, FILL, EMPTY);
        LabelStyle tall = LabelStyle.of(UiFont.TALL, FILL, EMPTY);

        assertThrows(IllegalArgumentException.class, () -> ProgressBarStyle.of(square, tall));
        assertThrows(NullPointerException.class, () -> ProgressBarStyle.of(null, square));
        assertThrows(NullPointerException.class, () -> ProgressBarStyle.of(square, null));
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProgressBar.horizontal(PERCENT, 0, SQUARE));
        assertThrows(NullPointerException.class, () -> ProgressBar.horizontal(null, 3, SQUARE));
        assertThrows(NullPointerException.class, () -> ProgressBar.horizontal(PERCENT, 3, null));
    }
}
