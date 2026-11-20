package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class UiSurfaceBoxTest {

    private static final int FOREGROUND = 0x112233FF;
    private static final int BACKGROUND = 0x445566FF;
    private static final LabelStyle LOOK = LabelStyle.of(UiFont.SQUARE, FOREGROUND, BACKGROUND);

    private final UiSurface surface = UiSurface.ofSize(10, 6);
    private final TerminalGrid grid = surface.getSquareGrid();

    @Test
    void drawsEdgesAndCornersOfTheRectangle() {
        surface.drawBox(1, 1, 5, 4, BoxStyle.SINGLE, LOOK);

        assertEquals('┌', grid.getCodePoint(1, 1));
        assertEquals('┐', grid.getCodePoint(5, 1));
        assertEquals('└', grid.getCodePoint(1, 4));
        assertEquals('┘', grid.getCodePoint(5, 4));
        for (int column = 2; column <= 4; column++) {
            assertEquals('─', grid.getCodePoint(column, 1));
            assertEquals('─', grid.getCodePoint(column, 4));
        }
        for (int row = 2; row <= 3; row++) {
            assertEquals('│', grid.getCodePoint(1, row));
            assertEquals('│', grid.getCodePoint(5, row));
        }
    }

    @Test
    void borderCellsGetTheLooksForegroundAndBackground() {
        surface.drawBox(1, 1, 5, 4, BoxStyle.DOUBLE, LOOK);

        assertEquals(FOREGROUND, grid.getForeground(1, 1));
        assertEquals(BACKGROUND, grid.getBackground(1, 1));
        assertEquals(FOREGROUND, grid.getForeground(3, 4));
        assertEquals(BACKGROUND, grid.getBackground(5, 3));
    }

    @Test
    void leavesTheInsideAndTheOutsideUntouched() {
        surface.drawBox(1, 1, 5, 4, BoxStyle.SINGLE, LOOK);

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(3, 2));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(3, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(6, 1));
    }

    @Test
    void erasesATallGlyphThatCoversABorderCell() {
        surface.writeGlyph(UiFont.TALL, 1, 2, 'T', FOREGROUND);

        surface.drawBox(1, 1, 5, 4, BoxStyle.SINGLE, LOOK);

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getTallGrid().getCodePoint(1, 1));
    }

    @Test
    void rejectsALookInTheTallFont() {
        LabelStyle tallLook = LabelStyle.of(UiFont.TALL, FOREGROUND, BACKGROUND);

        assertThrows(IllegalArgumentException.class,
                () -> surface.drawBox(0, 0, 4, 4, BoxStyle.SINGLE, tallLook));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> surface.drawBox(0, 0, 4, 4, null, LOOK));
        assertThrows(NullPointerException.class,
                () -> surface.drawBox(0, 0, 4, 4, BoxStyle.SINGLE, null));
    }

    @Test
    void drawsNothingWhenWidthOrHeightIsBelowTwo() {
        surface.drawBox(1, 1, 1, 4, BoxStyle.SINGLE, LOOK);
        surface.drawBox(3, 1, 4, 1, BoxStyle.SINGLE, LOOK);
        surface.drawBox(1, 1, 0, 0, BoxStyle.SINGLE, LOOK);
        surface.drawBox(1, 1, -3, 5, BoxStyle.SINGLE, LOOK);

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 10; column++) {
                assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(column, row));
            }
        }
    }

    @Test
    void drawsTheSmallestBoxAsFourCorners() {
        surface.drawBox(2, 2, 2, 2, BoxStyle.SINGLE, LOOK);

        assertEquals('┌', grid.getCodePoint(2, 2));
        assertEquals('┐', grid.getCodePoint(3, 2));
        assertEquals('└', grid.getCodePoint(2, 3));
        assertEquals('┘', grid.getCodePoint(3, 3));
    }

    @Test
    void skipsCellsOutsideTheSurface() {
        surface.drawBox(-2, -1, 5, 4, BoxStyle.SINGLE, LOOK);
        surface.drawBox(8, 4, 5, 4, BoxStyle.SINGLE, LOOK);

        assertEquals('┘', grid.getCodePoint(2, 2));
        assertEquals('─', grid.getCodePoint(0, 2));
        assertEquals('│', grid.getCodePoint(2, 0));
        assertEquals('┌', grid.getCodePoint(8, 4));
        assertEquals('─', grid.getCodePoint(9, 4));
    }
}
