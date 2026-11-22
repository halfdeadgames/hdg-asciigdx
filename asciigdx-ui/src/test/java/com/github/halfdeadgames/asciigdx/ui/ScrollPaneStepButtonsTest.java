package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class ScrollPaneStepButtonsTest {
    private static final int TRACK = 0x111111FF;
    private static final int THUMB = 0x222222FF;
    private static final int ARROW = 0x666666FF;
    private static final int ARROW_OVER = 0x777777FF;
    private static final int ARROW_PRESSED = 0x888888FF;
    private static final int BAR_COLUMN = 9;
    private static final int FIRST_ROW = 0;
    private static final int LAST_ROW = 4;
    private static final ScrollPaneStyle STYLE = ScrollPaneStyle.of(TRACK, THUMB)
            .withStepArrow(ARROW).withStepArrowPointerOver(ARROW_OVER)
            .withStepArrowPressed(ARROW_PRESSED).withStepButtonsShown(true);

    private final UiSurface surface = UiSurface.ofSize(30, 20);
    private final UiRoot ui = UiRoot.of(surface);
    private final TerminalGrid grid = surface.getSquareGrid();

    private static final class Block extends Widget {
        private final int width;
        private final int height;

        Block(int width, int height) {
            this.width = width;
            this.height = height;
            setSize(width, height);
        }

        @Override
        public int getPreferredWidth() {
            return width;
        }

        @Override
        public int getPreferredHeight() {
            return height;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
        }
    }

    // View is 9 x height, bar in column 9; the content is 12 rows, so the view scrolls.
    private ScrollPane paneOfHeight(int height) {
        ScrollPane pane = new ScrollPane(new Block(8, 12), STYLE);
        pane.setSize(10, height);
        ui.getRoot().addChild(pane);
        ui.draw();
        return pane;
    }

    @Test
    void arrowsFillTheEndCellsAndTheTrackUsesTheCellsBetween() {
        ScrollPane pane = paneOfHeight(5);

        assertEquals(0x25B2, grid.getCodePoint(BAR_COLUMN, FIRST_ROW));
        assertEquals(0x25BC, grid.getCodePoint(BAR_COLUMN, LAST_ROW));
        assertEquals(ARROW, grid.getForeground(BAR_COLUMN, FIRST_ROW));
        assertEquals(TRACK, grid.getBackground(BAR_COLUMN, FIRST_ROW));
        assertEquals(TRACK, grid.getBackground(BAR_COLUMN, LAST_ROW));
        // Track of 3 cells = 24 eighths; the thumb is 24 * 5 / 12 = 10 eighths.
        assertEquals(10, pane.thumbLengthEighths(BarOrientation.VERTICAL));
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(BAR_COLUMN, 1));
    }

    @Test
    void barsShorterThanThreeCellsShowNoArrows() {
        ScrollPane pane = paneOfHeight(2);

        assertNotEquals(0x25B2, grid.getCodePoint(BAR_COLUMN, FIRST_ROW));
        assertNotEquals(0x25BC, grid.getCodePoint(BAR_COLUMN, 1));
        // The whole 2-cell bar is track: 16 eighths, and the thumb has its minimum of one cell.
        assertEquals(EighthBlocks.EIGHTHS_PER_CELL,
                pane.thumbLengthEighths(BarOrientation.VERTICAL));
        // A press on the second cell is a page click, not an arrow step.
        ui.pointerDown(BAR_COLUMN, 1, 0, 0);
        ui.pointerUp(BAR_COLUMN, 1, 0, 0);
        assertEquals(2, pane.getScrollRow());
    }

    @Test
    void pressingAnArrowMovesOneCell() {
        ScrollPane pane = paneOfHeight(5);

        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);
        ui.pointerUp(BAR_COLUMN, LAST_ROW, 0, 0);
        assertEquals(1, pane.getScrollRow());

        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);
        ui.pointerUp(BAR_COLUMN, LAST_ROW, 0, 0);
        ui.pointerDown(BAR_COLUMN, FIRST_ROW, 0, 0);
        ui.pointerUp(BAR_COLUMN, FIRST_ROW, 0, 0);
        assertEquals(1, pane.getScrollRow());
    }

    @Test
    void holdingAnArrowRepeatsAfterTheDelayThenAtTheInterval() {
        ScrollPane pane = paneOfHeight(5);

        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);
        assertEquals(1, pane.getScrollRow());
        ui.update(0.39f);
        assertEquals(1, pane.getScrollRow());
        ui.update(0.02f);
        assertEquals(2, pane.getScrollRow());
        ui.update(0.05f);
        assertEquals(3, pane.getScrollRow());
        ui.update(0.05f);
        assertEquals(4, pane.getScrollRow());
    }

    @Test
    void releasingTheArrowStopsTheRepeat() {
        ScrollPane pane = paneOfHeight(5);
        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);
        ui.update(0.41f);
        assertEquals(2, pane.getScrollRow());

        ui.pointerUp(BAR_COLUMN, LAST_ROW, 0, 0);
        ui.update(1f);

        assertEquals(2, pane.getScrollRow());
    }

    @Test
    void leavingTheArrowPausesTheRepeatAndReturningResumesIt() {
        ScrollPane pane = paneOfHeight(5);
        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);

        ui.pointerDragged(BAR_COLUMN, 2, 0);
        ui.update(1f);
        assertEquals(1, pane.getScrollRow());

        ui.pointerDragged(BAR_COLUMN, LAST_ROW, 0);
        ui.update(0.41f);
        assertEquals(2, pane.getScrollRow());
    }

    @Test
    void arrowColorFollowsHoverAndPress() {
        paneOfHeight(5);

        ui.pointerMoved(BAR_COLUMN, LAST_ROW);
        ui.draw();
        assertEquals(ARROW_OVER, grid.getForeground(BAR_COLUMN, LAST_ROW));
        assertEquals(ARROW, grid.getForeground(BAR_COLUMN, FIRST_ROW));

        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);
        ui.draw();
        assertEquals(ARROW_PRESSED, grid.getForeground(BAR_COLUMN, LAST_ROW));

        ui.pointerDragged(BAR_COLUMN, 2, 0);
        ui.draw();
        assertEquals(ARROW, grid.getForeground(BAR_COLUMN, LAST_ROW));
    }

    @Test
    void repeatContinuesWhenKeyboardFocusMovesAway() {
        ScrollPane pane = paneOfHeight(5);
        Widget other = new Widget() {
            @Override
            public boolean isFocusable() {
                return true;
            }

            @Override
            public int getPreferredWidth() {
                return 2;
            }

            @Override
            public int getPreferredHeight() {
                return 1;
            }

            @Override
            protected void drawContent(UiSurface target, int left, int top) {
            }
        };
        other.setPosition(20, 10);
        other.setSize(2, 1);
        ui.getRoot().addChild(other);
        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);

        ui.setKeyboardFocus(other);
        ui.update(0.41f);

        assertEquals(2, pane.getScrollRow());
    }

    @Test
    void aHugeFrameMakesAtMostTheCapOfSteps() {
        ScrollPane pane = new ScrollPane(new Block(8, 100), STYLE);
        pane.setSize(10, 5);
        ui.getRoot().addChild(pane);
        ui.draw();
        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);

        ui.update(1e6f);

        assertEquals(1 + 8, pane.getScrollRow());
        ui.update(0.01f);
        assertEquals(1 + 8, pane.getScrollRow());
    }

    @Test
    void theHoldEndsWhenTheArrowsDisappear() {
        ScrollPane pane = paneOfHeight(5);
        ui.pointerDown(BAR_COLUMN, LAST_ROW, 0, 0);

        pane.setStyle(ScrollPaneStyle.of(TRACK, THUMB));
        ui.update(0.41f);
        ui.update(1f);

        assertEquals(1, pane.getScrollRow());
    }

    @Test
    void pageClickOnTheShortenedTrackScrollsOnePage() {
        ScrollPane pane = paneOfHeight(5);

        // Row 3 is the last track cell, below the thumb that covers rows 1 and 2.
        ui.pointerDown(BAR_COLUMN, 3, 0, 0);
        ui.pointerUp(BAR_COLUMN, 3, 0, 0);

        assertEquals(5, pane.getScrollRow());
    }
}
