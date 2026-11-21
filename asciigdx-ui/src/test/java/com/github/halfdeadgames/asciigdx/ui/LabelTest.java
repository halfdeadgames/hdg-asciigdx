package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LabelTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;
    private static final LabelStyle STYLE = LabelStyle.of(RED, BLUE);

    private final UiSurface surface = UiSurface.ofSize(20, 6);
    private final TerminalGrid squareGrid = surface.getSquareGrid();
    private final TerminalGrid textGrid = surface.getTextGrid();

    private String textRow(int row, int fromTextColumn, int count) {
        StringBuilder builder = new StringBuilder();
        for (int column = fromTextColumn; column < fromTextColumn + count; column++) {
            builder.appendCodePoint(textGrid.getCodePoint(column, row));
        }
        return builder.toString();
    }

    @Test
    void drawsTextAtPositionInTextColumnsWithStyleColors() {
        Label label = new Label("Hi", STYLE);
        label.setPosition(3, 2);
        label.draw(surface);
        assertEquals("Hi", textRow(2, 6, 2));
        assertEquals(RED, textGrid.getForeground(6, 2));
        assertEquals(BLUE, squareGrid.getBackground(3, 2));
    }

    @Test
    void cutsTextOffAtWidthInTextColumns() {
        Label label = new Label("Hello", STYLE);
        label.setSize(2, 1);
        label.setPosition(1, 0);
        label.draw(surface);
        assertEquals("Hell", textRow(0, 2, 4));
        assertNotEquals('o', textGrid.getCodePoint(6, 0));
        assertNotEquals(BLUE, squareGrid.getBackground(3, 0));
    }

    @Test
    void backgroundFillsWholeRectangle() {
        Label label = new Label("ab", STYLE);
        label.setSize(5, 3);
        label.setPosition(2, 1);
        label.draw(surface);
        for (int row = 1; row < 4; row++) {
            for (int column = 2; column < 7; column++) {
                assertEquals(BLUE, squareGrid.getBackground(column, row));
            }
        }
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(4, 2));
        assertNotEquals(BLUE, squareGrid.getBackground(7, 1));
    }

    @Test
    void setTextKeepsSizeAndPackFitsText() {
        Label label = new Label("abc", STYLE);
        label.setText("longer text");
        assertEquals(2, label.getWidth());
        label.pack();
        assertEquals(6, label.getWidth());
        assertEquals(1, label.getHeight());
    }

    @Test
    void evenLengthTextTakesHalfItsLettersInCells() {
        assertEquals(2, new Label("abcd", STYLE).getPreferredWidth());
        assertEquals(2, new Label("abcd", STYLE).getWidth());
    }

    @Test
    void oddLengthTextRoundsUpToWholeCells() {
        assertEquals(2, new Label("abc", STYLE).getPreferredWidth());
        assertEquals(3, new Label("abcde", STYLE).getWidth());
        assertEquals(0, new Label("", STYLE).getWidth());
    }

    @Test
    void oddLengthTextLeavesTheLastHalfCellBlank() {
        Label label = new Label("abc", STYLE);
        label.setPosition(1, 0);
        label.draw(surface);
        assertEquals("abc", textRow(0, 2, 3));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(5, 0));
        assertEquals(BLUE, squareGrid.getBackground(2, 0));
    }

    @Test
    void preferredWidthCountsCodePoints() {
        Label label = new Label("a😀b", STYLE);
        assertEquals(2, label.getPreferredWidth());
        assertEquals(2, label.getWidth());
        label.draw(surface);
        assertEquals('a', textGrid.getCodePoint(0, 0));
        assertEquals(0x1F600, textGrid.getCodePoint(1, 0));
        assertEquals('b', textGrid.getCodePoint(2, 0));
    }

    @Test
    void zeroSizeDrawsNothing() {
        Label label = new Label("abc", STYLE);
        label.setSize(0, 1);
        label.draw(surface);
        assertNotEquals('a', textGrid.getCodePoint(0, 0));
        assertNotEquals(BLUE, squareGrid.getBackground(0, 0));
    }

    @Test
    void partlyOffSurfaceDrawsVisiblePart() {
        Label left = new Label("abcdef", STYLE);
        left.setPosition(-2, 0);
        left.draw(surface);
        assertEquals("ef", textRow(0, 0, 2));

        Label right = new Label("wxyzuv", STYLE);
        right.setPosition(18, 5);
        right.draw(surface);
        assertEquals("wxyz", textRow(5, 36, 4));

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
        label.setStyle(LabelStyle.of(WHITE, BLACK));
        label.draw(surface);
        assertEquals(WHITE, textGrid.getForeground(0, 0));
        assertEquals(BLACK, squareGrid.getBackground(0, 0));
    }

    @Test
    void fullyTransparentBackgroundKeepsCellBackgrounds() {
        surface.fillBackground(0, 0, 20, 6, BLACK);
        Label label = new Label("ab", LabelStyle.of(RED, TerminalGrid.TRANSPARENT_RGBA));
        label.setSize(3, 2);
        label.draw(surface);
        assertEquals("ab", textRow(0, 0, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(2, 0));
        assertEquals(RED, textGrid.getForeground(0, 0));
        assertEquals(BLACK, squareGrid.getBackground(0, 0));
        assertEquals(BLACK, squareGrid.getBackground(2, 1));
    }

    @Test
    void partlyTransparentBackgroundReplacesCellBackgrounds() {
        surface.fillBackground(0, 0, 20, 6, BLACK);
        int halfTransparentBlue = 0x0000FF80;
        Label label = new Label("ab", LabelStyle.of(RED, halfTransparentBlue));
        label.draw(surface);
        assertEquals(halfTransparentBlue, squareGrid.getBackground(0, 0));
    }

    @Test
    void preferredHeightIsOneRow() {
        assertEquals(1, new Label("abc", STYLE).getPreferredHeight());
        assertEquals(1, new Label("abc", STYLE).getHeight());
    }

    @Test
    void textLandsInTextGridAndNotInSquareGrid() {
        Label label = new Label("Hi", STYLE);
        label.draw(surface);
        assertEquals("Hi", textRow(0, 0, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, squareGrid.getCodePoint(0, 0));
    }

    @Test
    void textAlignmentIsLeftByDefault() {
        assertEquals(HorizontalAlignment.LEFT, new Label("a", STYLE).getTextAlignment());
    }

    @Test
    void leftAlignmentStartsAtTheLeftEdge() {
        Label label = wideLabel("Hi", HorizontalAlignment.LEFT);
        label.draw(surface);
        assertEquals("Hi", textRow(0, 2, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(4, 0));
    }

    @Test
    void centerAlignmentSplitsTheSpareTextColumnsRoundedDown() {
        Label label = wideLabel("Hi", HorizontalAlignment.CENTER);
        label.draw(surface);
        // Width 5 cells is 10 text columns; spare 8, half is 4, after the label's own offset of 2.
        assertEquals("Hi", textRow(0, 6, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(5, 0));
    }

    @Test
    void centerAlignmentCanStartOnAHalfCell() {
        Label label = new Label("abc", STYLE);
        label.setSize(4, 1);
        label.setTextAlignment(HorizontalAlignment.CENTER);
        label.draw(surface);
        // Width 4 cells is 8 text columns; spare 5, half rounded down is 2.
        assertEquals("abc", textRow(0, 2, 3));

        Label oddSpare = new Label("abc", STYLE);
        oddSpare.setSize(3, 1);
        oddSpare.setPosition(0, 1);
        oddSpare.setTextAlignment(HorizontalAlignment.CENTER);
        oddSpare.draw(surface);
        // Width 3 cells is 6 text columns; spare 3, half rounded down is 1: a half-cell start.
        assertEquals("abc", textRow(1, 1, 3));
    }

    @Test
    void rightAlignmentEndsAtTheRightEdge() {
        Label label = wideLabel("Hi", HorizontalAlignment.RIGHT);
        label.draw(surface);
        assertEquals("Hi", textRow(0, 10, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, textGrid.getCodePoint(12, 0));
    }

    @Test
    void textLongerThanTheLabelStartsAtTheLeftEdgeForEveryAlignment() {
        for (HorizontalAlignment alignment : HorizontalAlignment.values()) {
            UiSurface fresh = UiSurface.ofSize(20, 6);
            Label label = new Label("Hello", STYLE);
            label.setSize(2, 1);
            label.setTextAlignment(alignment);
            label.draw(fresh);
            assertEquals('H', fresh.getTextGrid().getCodePoint(0, 0));
            assertEquals('l', fresh.getTextGrid().getCodePoint(3, 0));
        }
    }

    @Test
    void setTextAlignmentRejectsNull() {
        Label label = new Label("a", STYLE);
        assertThrows(NullPointerException.class, () -> label.setTextAlignment(null));
    }

    @Test
    void changingTheAlignmentRequestsARedrawAndAnEqualOneDoesNot() {
        Label label = new Label("a", STYLE);
        label.clearRedrawNeeded();

        label.setTextAlignment(HorizontalAlignment.LEFT);
        assertFalse(label.isRedrawNeeded());
        label.setTextAlignment(HorizontalAlignment.RIGHT);
        assertTrue(label.isRedrawNeeded());
        assertEquals(HorizontalAlignment.RIGHT, label.getTextAlignment());
    }

    private Label wideLabel(String text, HorizontalAlignment alignment) {
        Label label = new Label(text, STYLE);
        label.setSize(5, 1);
        label.setPosition(1, 0);
        label.setTextAlignment(alignment);
        return label;
    }
}
