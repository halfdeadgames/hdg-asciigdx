package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LabelTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;
    private static final LabelStyle STYLE = LabelStyle.of(UiFont.SQUARE, RED, BLUE);
    private static final LabelStyle TALL_STYLE = LabelStyle.of(UiFont.TALL, RED, BLUE);

    private final UiSurface surface = UiSurface.ofSize(20, 6);
    private final TerminalGrid grid = surface.getSquareGrid();

    private String rowText(TerminalGrid source, int row, int fromColumn, int count) {
        StringBuilder builder = new StringBuilder();
        for (int column = fromColumn; column < fromColumn + count; column++) {
            builder.appendCodePoint(source.getCodePoint(column, row));
        }
        return builder.toString();
    }

    private String rowText(int row, int fromColumn, int count) {
        return rowText(grid, row, fromColumn, count);
    }

    @Test
    void drawsTextAtPositionWithStyleColors() {
        Label label = new Label("Hi", STYLE);
        label.setPosition(3, 2);
        label.draw(surface);
        assertEquals("Hi", rowText(2, 3, 2));
        assertEquals(RED, grid.getForeground(3, 2));
        assertEquals(BLUE, grid.getBackground(4, 2));
    }

    @Test
    void cutsTextOffAtWidth() {
        Label label = new Label("Hello", STYLE);
        label.setSize(3, 1);
        label.setPosition(1, 0);
        label.draw(surface);
        assertEquals("Hel", rowText(0, 1, 3));
        assertNotEquals('l', grid.getCodePoint(4, 0));
        assertNotEquals(BLUE, grid.getBackground(4, 0));
    }

    @Test
    void backgroundFillsWholeRectangle() {
        Label label = new Label("ab", STYLE);
        label.setSize(5, 3);
        label.setPosition(2, 1);
        label.draw(surface);
        for (int row = 1; row < 4; row++) {
            for (int column = 2; column < 7; column++) {
                assertEquals(BLUE, grid.getBackground(column, row));
            }
        }
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(2, 2));
        assertNotEquals(BLUE, grid.getBackground(7, 1));
    }

    @Test
    void setTextKeepsSizeAndPackFitsText() {
        Label label = new Label("abc", STYLE);
        label.setText("longer text");
        assertEquals(3, label.getWidth());
        label.pack();
        assertEquals(11, label.getWidth());
        assertEquals(1, label.getHeight());
    }

    @Test
    void preferredWidthCountsCodePoints() {
        Label label = new Label("a😀b", STYLE);
        assertEquals(3, label.getPreferredWidth());
        assertEquals(3, label.getWidth());
        label.draw(surface);
        assertEquals(0x1F600, grid.getCodePoint(1, 0));
        assertEquals('b', grid.getCodePoint(2, 0));
    }

    @Test
    void zeroSizeDrawsNothing() {
        Label label = new Label("abc", STYLE);
        label.setSize(0, 1);
        label.draw(surface);
        assertNotEquals('a', grid.getCodePoint(0, 0));
        assertNotEquals(BLUE, grid.getBackground(0, 0));
    }

    @Test
    void partlyOffSurfaceDrawsVisiblePart() {
        Label left = new Label("abcd", STYLE);
        left.setPosition(-2, 0);
        left.draw(surface);
        assertEquals("cd", rowText(0, 0, 2));

        Label right = new Label("wxyz", STYLE);
        right.setPosition(18, 5);
        right.draw(surface);
        assertEquals("wx", rowText(5, 18, 2));

        Label above = new Label("up", STYLE);
        above.setPosition(0, -1);
        above.draw(surface);
    }

    @Test
    void nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> new Label(null, STYLE));
        assertThrows(NullPointerException.class, () -> new Label("a", null));
        Label label = new Label("a", STYLE);
        assertThrows(NullPointerException.class, () -> label.setText(null));
        assertThrows(NullPointerException.class, () -> label.setStyle(null));
        assertThrows(NullPointerException.class, () -> label.draw(null));
    }

    @Test
    void setStyleChangesColors() {
        Label label = new Label("a", STYLE);
        label.setStyle(LabelStyle.of(UiFont.SQUARE, WHITE, BLACK));
        label.draw(surface);
        assertEquals(WHITE, grid.getForeground(0, 0));
        assertEquals(BLACK, grid.getBackground(0, 0));
    }

    @Test
    void fullyTransparentBackgroundKeepsCellBackgrounds() {
        surface.fillBackground(0, 0, 20, 6, BLACK);
        Label label = new Label("ab", LabelStyle.of(UiFont.SQUARE, RED,
                TerminalGrid.TRANSPARENT_RGBA));
        label.setSize(3, 2);
        label.draw(surface);
        assertEquals("ab", rowText(0, 0, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(2, 0));
        assertEquals(RED, grid.getForeground(0, 0));
        assertEquals(BLACK, grid.getBackground(0, 0));
        assertEquals(BLACK, grid.getBackground(2, 1));
    }

    @Test
    void partlyTransparentBackgroundReplacesCellBackgrounds() {
        surface.fillBackground(0, 0, 20, 6, BLACK);
        int halfTransparentBlue = 0x0000FF80;
        Label label = new Label("ab", LabelStyle.of(UiFont.SQUARE, RED, halfTransparentBlue));
        label.draw(surface);
        assertEquals(halfTransparentBlue, grid.getBackground(0, 0));
    }

    @Test
    void tallLabelPrefersTwoRows() {
        assertEquals(2, new Label("abc", TALL_STYLE).getPreferredHeight());
        assertEquals(2, new Label("abc", TALL_STYLE).getHeight());
        assertEquals(1, new Label("abc", STYLE).getPreferredHeight());
    }

    @Test
    void tallLabelTextLandsInTallGrid() {
        Label label = new Label("Hi", TALL_STYLE);
        label.setPosition(1, 2);
        label.draw(surface);
        TerminalGrid tallGrid = surface.getTallGrid();
        assertEquals("Hi", rowText(tallGrid, 1, 1, 2));
        assertEquals(RED, tallGrid.getForeground(1, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(1, 2));
        assertEquals(BLUE, grid.getBackground(1, 3));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, tallGrid.getBackground(1, 1));
    }

    @Test
    void tallLabelOnOddRowSnapsUp() {
        Label label = new Label("Hi", TALL_STYLE);
        label.setPosition(0, 3);
        label.draw(surface);
        assertEquals("Hi", rowText(surface.getTallGrid(), 1, 0, 2));
    }

    @Test
    void squareLabelTextLandsInSquareGrid() {
        Label label = new Label("Hi", STYLE);
        label.draw(surface);
        assertEquals("Hi", rowText(0, 0, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getTallGrid().getCodePoint(0, 0));
    }
}
