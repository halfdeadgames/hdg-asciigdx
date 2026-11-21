package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class ScrollPaneTest {
    private static final int TRACK = 0x111111FF;
    private static final int THUMB = 0x222222FF;
    private static final int OVER = 0x333333FF;
    private static final int PRESSED = 0x444444FF;
    private static final int CONTENT_COLOR = 0x555555FF;
    private static final int EMPTY = TerminalGrid.EMPTY_CODE_POINT;
    private static final ScrollPaneStyle STYLE = ScrollPaneStyle.of(TRACK, THUMB)
            .withThumbPointerOver(OVER).withThumbPressed(PRESSED);

    private final UiSurface surface = UiSurface.ofSize(30, 20);
    private final UiRoot ui = UiRoot.of(surface);
    private final TerminalGrid grid = surface.getSquareGrid();

    private static class Block extends Widget {
        private final int preferredWidth;
        private final int preferredHeight;

        Block(int x, int y, int width, int height) {
            this.preferredWidth = width;
            this.preferredHeight = height;
            setPosition(x, y);
            setSize(width, height);
        }

        @Override
        public int getPreferredWidth() {
            return preferredWidth;
        }

        @Override
        public int getPreferredHeight() {
            return preferredHeight;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
            target.fillBackground(left, top, getWidth(), getHeight(), CONTENT_COLOR);
        }
    }

    private static final class FocusBlock extends Block {
        FocusBlock(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        public boolean isFocusable() {
            return true;
        }
    }

    private ScrollPane paneWith(Widget content, int x, int y, int width, int height) {
        ScrollPane pane = new ScrollPane(content, STYLE);
        pane.setPosition(x, y);
        pane.setSize(width, height);
        ui.getRoot().addChild(pane);
        ui.draw();
        return pane;
    }

    private ScrollPane pane(int contentWidth, int contentHeight, int width, int height) {
        return paneWith(new Block(0, 0, contentWidth, contentHeight), 0, 0, width, height);
    }

    private WidgetGroup groupOf(Widget... children) {
        WidgetGroup group = new WidgetGroup();
        for (Widget child : children) {
            group.addChild(child);
        }
        return group;
    }

    @Test
    void constructorPacksToTheContentAndPositionsNothing() {
        Block content = new Block(0, 0, 20, 12);

        ScrollPane pane = new ScrollPane(content, STYLE);

        assertEquals(20, pane.getWidth());
        assertEquals(12, pane.getHeight());
        assertSame(content, pane.getContent());
        assertSame(pane, content.getParent());
        assertEquals(0, pane.getMaxScrollColumn());
        assertEquals(0, pane.getMaxScrollRow());
    }

    @Test
    void constructorRejectsBadArguments() {
        Block content = new Block(0, 0, 4, 4);
        assertThrows(NullPointerException.class, () -> new ScrollPane(null, STYLE));
        assertThrows(NullPointerException.class, () -> new ScrollPane(content, null));
        WidgetGroup other = new WidgetGroup();
        other.addChild(content);
        assertThrows(IllegalArgumentException.class, () -> new ScrollPane(content, STYLE));
    }

    @Test
    void noBarsAppearWhenTheContentFits() {
        ScrollPane pane = pane(8, 4, 10, 5);

        assertEquals(0, pane.getMaxScrollColumn());
        assertEquals(0, pane.getMaxScrollRow());
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(9, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(0, 4));
    }

    @Test
    void verticalBarAppearsAloneWhenOnlyTheHeightOverflows() {
        ScrollPane pane = pane(8, 12, 10, 5);

        assertEquals(0, pane.getMaxScrollColumn());
        assertEquals(7, pane.getMaxScrollRow());
        // View is 9 x 5. Thumb: 17 eighths of a 40 eighth track.
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(9, 0));
        assertEquals(THUMB, grid.getForeground(9, 0));
        assertEquals(TRACK, grid.getBackground(9, 0));
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(9, 1));
        assertEquals(0x2587, grid.getCodePoint(9, 2));
        assertEquals(TRACK, grid.getForeground(9, 2));
        assertEquals(THUMB, grid.getBackground(9, 2));
        assertEquals(EMPTY, grid.getCodePoint(9, 3));
        assertEquals(TRACK, grid.getBackground(9, 4));
        assertEquals(CONTENT_COLOR, grid.getBackground(0, 4));
    }

    @Test
    void bothBarsAppearWithATrackColoredCorner() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertEquals(11, pane.getMaxScrollColumn());
        assertEquals(8, pane.getMaxScrollRow());
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(9, 0));
        assertEquals(0x2585, grid.getCodePoint(9, 1));
        assertEquals(EMPTY, grid.getCodePoint(9, 2));
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(0, 4));
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(3, 4));
        assertEquals(EMPTY, grid.getCodePoint(4, 4));
        assertEquals(EMPTY, grid.getCodePoint(9, 4));
        assertEquals(TRACK, grid.getBackground(9, 4));
        assertEquals(CONTENT_COLOR, grid.getBackground(8, 3));
    }

    @Test
    void oneBarCanMakeTheOtherNecessary() {
        ScrollPane pane = pane(10, 6, 10, 5);

        assertEquals(1, pane.getMaxScrollColumn());
        assertEquals(2, pane.getMaxScrollRow());
        assertEquals(TRACK, grid.getBackground(9, 4));
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(0, 4));
    }

    @Test
    void contentIsClippedToTheViewport() {
        pane(20, 12, 10, 5);

        assertEquals(CONTENT_COLOR, grid.getBackground(0, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(10, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(0, 5));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, grid.getBackground(15, 8));
    }

    @Test
    void scrollPositionIsClampedAndMovesTheContent() {
        Block content = new Block(0, 0, 20, 12);
        ScrollPane pane = paneWith(content, 0, 0, 10, 5);

        pane.setScrollPosition(100, 100);
        assertEquals(11, pane.getScrollColumn());
        assertEquals(8, pane.getScrollRow());
        assertEquals(-11, content.getX());
        assertEquals(-8, content.getY());

        pane.setScrollPosition(-5, 3);
        assertEquals(0, pane.getScrollColumn());
        assertEquals(3, pane.getScrollRow());
        assertEquals(0, content.getX());
        assertEquals(-3, content.getY());
    }

    @Test
    void scrolledContentDrawsWithTheOffset() {
        Block content = new Block(0, 0, 20, 12);
        ScrollPane pane = paneWith(content, 0, 0, 10, 5);

        pane.setScrollPosition(11, 8);
        ui.draw();

        // Thumb now at the end of both bars.
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(9, 3));
        assertEquals(EighthBlocks.FULL_GLYPH, grid.getCodePoint(8, 4));
        assertEquals(EMPTY, grid.getCodePoint(9, 0));
        assertEquals(CONTENT_COLOR, grid.getBackground(0, 0));
    }

    @Test
    void shrinkingTheContentRelayoutsAndClampsTheScroll() {
        ScrollPane pane = pane(20, 12, 10, 5);
        pane.setScrollPosition(11, 8);

        pane.setSize(20, 12);
        ui.draw();

        assertEquals(0, pane.getMaxScrollColumn());
        assertEquals(0, pane.getMaxScrollRow());
        assertEquals(0, pane.getScrollColumn());
        assertEquals(0, pane.getScrollRow());
    }

    @Test
    void hitUsesTheScrollOffsetAndTheViewportClip() {
        Block child = new Block(15, 10, 3, 2);
        WidgetGroup content = groupOf(child);
        ScrollPane pane = paneWith(content, 2, 1, 10, 5);
        pane.setScrollPosition(9, 8);

        assertSame(child, pane.hit(6, 2));
        assertSame(content, pane.hit(0, 0));
        assertNull(pane.hit(15, 10));
        assertNull(pane.hit(10, 0));
        assertNull(pane.hit(-1, 0));
        assertNull(pane.hit(0, 5));
    }

    @Test
    void barCellsAndTheCornerHitThePane() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertSame(pane, pane.hit(9, 2));
        assertSame(pane, pane.hit(3, 4));
        assertSame(pane, pane.hit(9, 4));
    }

    @Test
    void emptyViewportAreaHitsThePane() {
        ScrollPane pane = paneWith(new Block(0, 0, 4, 4), 0, 0, 10, 5);

        assertSame(pane, pane.hit(8, 3));
        assertSame(pane.getContent(), pane.hit(2, 2));
    }

    @Test
    void wheelScrollsThreeCellsPerStepAndBubblesAtTheEnds() {
        ScrollPane pane = pane(20, 12, 10, 5);
        ui.pointerMoved(3, 2);

        assertTrue(ui.scrolled(0f, 1f));
        assertEquals(3, pane.getScrollRow());
        assertTrue(ui.scrolled(1f, 0f));
        assertEquals(3, pane.getScrollColumn());
        assertTrue(ui.scrolled(0f, 10f));
        assertEquals(8, pane.getScrollRow());
        assertFalse(ui.scrolled(0f, 1f));
        assertTrue(ui.scrolled(0f, -1f));
        assertEquals(5, pane.getScrollRow());
    }

    @Test
    void wheelOnAnInnerPaneAtItsEndScrollsTheOuterPane() {
        ScrollPane inner = new ScrollPane(new Block(0, 0, 8, 5), STYLE);
        inner.setSize(8, 4);
        ScrollPane outer = paneWith(groupOf(new Block(0, 0, 12, 30), inner), 0, 0, 12, 10);
        ui.pointerMoved(2, 1);

        assertTrue(ui.scrolled(0f, 1f));
        assertEquals(2, inner.getScrollRow());
        assertEquals(0, outer.getScrollRow());
        assertTrue(ui.scrolled(0f, 1f));
        assertEquals(3, outer.getScrollRow());
    }

    @Test
    void pageKeysMoveOneViewAndHomeEndJump() {
        ScrollPane pane = pane(20, 12, 10, 5);
        ui.setKeyboardFocus(pane);

        assertTrue(ui.keyDown(Input.Keys.PAGE_DOWN));
        assertEquals(4, pane.getScrollRow());
        assertTrue(ui.keyDown(Input.Keys.PAGE_DOWN));
        assertTrue(ui.keyDown(Input.Keys.PAGE_DOWN));
        assertEquals(8, pane.getScrollRow());
        assertTrue(ui.keyDown(Input.Keys.PAGE_UP));
        assertEquals(4, pane.getScrollRow());
        assertTrue(ui.keyDown(Input.Keys.HOME));
        assertEquals(0, pane.getScrollRow());
        assertTrue(ui.keyDown(Input.Keys.END));
        assertEquals(8, pane.getScrollRow());
    }

    @Test
    void pageKeysAreNotConsumedWhenNothingScrolls() {
        ScrollPane pane = pane(8, 4, 10, 5);
        ui.setKeyboardFocus(pane);

        assertFalse(ui.keyDown(Input.Keys.PAGE_DOWN));
        assertFalse(ui.keyDown(Input.Keys.END));
    }

    @Test
    void pageKeysBubbleFromAFocusedDescendant() {
        FocusBlock button = new FocusBlock(0, 0, 4, 1);
        ScrollPane pane = paneWith(groupOf(new Block(0, 0, 20, 12), button), 0, 0, 10, 5);
        ui.setKeyboardFocus(button);

        assertTrue(ui.keyDown(Input.Keys.PAGE_DOWN));

        assertEquals(4, pane.getScrollRow());
    }

    @Test
    void arrowKeysScrollOnlyWhileThePaneItselfHasFocus() {
        ScrollPane pane = pane(20, 12, 10, 5);
        ui.setKeyboardFocus(pane);

        assertTrue(ui.keyDown(Input.Keys.DOWN));
        assertTrue(ui.keyDown(Input.Keys.RIGHT));
        assertEquals(1, pane.getScrollRow());
        assertEquals(1, pane.getScrollColumn());
        assertTrue(ui.keyDown(Input.Keys.UP));
        assertTrue(ui.keyDown(Input.Keys.LEFT));
        assertEquals(0, pane.getScrollRow());
        assertEquals(0, pane.getScrollColumn());
    }

    @Test
    void arrowKeysAcrossANonScrollingAxisAreNotConsumed() {
        ScrollPane pane = pane(8, 12, 10, 5);
        ui.setKeyboardFocus(pane);

        assertFalse(ui.keyDown(Input.Keys.LEFT));
        assertFalse(ui.keyDown(Input.Keys.RIGHT));
        assertTrue(ui.keyDown(Input.Keys.DOWN));
    }

    @Test
    void arrowKeysDoNotScrollWhenADescendantHasFocus() {
        FocusBlock button = new FocusBlock(0, 0, 4, 1);
        ScrollPane pane = paneWith(groupOf(new Block(0, 0, 20, 12), button), 0, 0, 10, 5);
        ui.setKeyboardFocus(button);

        ui.keyDown(Input.Keys.DOWN);

        assertEquals(0, pane.getScrollRow());
        assertSame(button, ui.getKeyboardFocus());
    }

    @Test
    void trackPressScrollsOnePageTowardThePress() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertTrue(ui.pointerDown(9, 3, 0, 0));
        ui.pointerUp(9, 3, 0, 0);
        assertEquals(4, pane.getScrollRow());

        assertTrue(ui.pointerDown(9, 3, 0, 0));
        ui.pointerUp(9, 3, 0, 0);
        assertEquals(8, pane.getScrollRow());

        assertTrue(ui.pointerDown(9, 0, 0, 0));
        ui.pointerUp(9, 0, 0, 0);
        assertEquals(4, pane.getScrollRow());
    }

    @Test
    void horizontalTrackPressScrollsColumns() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertTrue(ui.pointerDown(8, 4, 0, 0));
        ui.pointerUp(8, 4, 0, 0);

        assertEquals(9, pane.getScrollColumn());
        assertEquals(0, pane.getScrollRow());
    }

    @Test
    void thumbDragMovesTheViewInProportion() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertTrue(ui.pointerDown(9, 0, 0, 0));
        assertTrue(ui.pointerDragged(9, 2, 0));
        assertEquals(6, pane.getScrollRow());
        assertTrue(ui.pointerDragged(9, 19, 0));
        assertEquals(8, pane.getScrollRow());
        assertTrue(ui.pointerDragged(9, -5, 0));
        assertEquals(0, pane.getScrollRow());
        assertTrue(ui.pointerUp(9, -5, 0, 0));
        assertFalse(ui.pointerDragged(9, 2, 0));
        assertEquals(0, pane.getScrollColumn());
    }

    @Test
    void horizontalThumbDragMovesColumns() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertTrue(ui.pointerDown(0, 4, 0, 0));
        assertTrue(ui.pointerDragged(8, 4, 0));

        assertEquals(11, pane.getScrollColumn());
        assertEquals(0, pane.getScrollRow());
    }

    @Test
    void thumbColorFollowsHoverAndPress() {
        pane(20, 12, 10, 5);

        ui.pointerMoved(9, 0);
        ui.draw();
        assertEquals(OVER, grid.getForeground(9, 0));

        ui.pointerDown(9, 0, 0, 0);
        ui.draw();
        assertEquals(PRESSED, grid.getForeground(9, 0));

        ui.pointerUp(9, 0, 0, 0);
        ui.pointerMoved(3, 2);
        ui.draw();
        assertEquals(THUMB, grid.getForeground(9, 0));
    }

    @Test
    void hoveringTheTrackAwayFromTheThumbKeepsTheNormalColor() {
        pane(20, 12, 10, 5);

        ui.pointerMoved(9, 3);
        ui.draw();

        assertEquals(THUMB, grid.getForeground(9, 0));
    }

    @Test
    void pressOnContentThatNobodyConsumesDragsTheContent() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertTrue(ui.pointerDown(3, 2, 0, 0));
        assertTrue(ui.pointerDragged(1, 1, 0));

        assertEquals(2, pane.getScrollColumn());
        assertEquals(1, pane.getScrollRow());
        assertTrue(ui.pointerDragged(8, 8, 0));
        assertEquals(0, pane.getScrollColumn());
        assertEquals(0, pane.getScrollRow());
        assertTrue(ui.pointerUp(8, 8, 0, 0));
        assertFalse(ui.pointerDragged(1, 1, 0));
    }

    @Test
    void pressOnAButtonDoesNotScroll() {
        FocusBlock button = new FocusBlock(0, 0, 8, 2);
        button.addListener(event -> event.getType() == UiEventType.POINTER_DOWN
                || event.getType() == UiEventType.POINTER_DRAGGED
                || event.getType() == UiEventType.POINTER_UP);
        ScrollPane pane = paneWith(groupOf(new Block(0, 0, 20, 12), button), 0, 0, 10, 5);

        assertTrue(ui.pointerDown(1, 1, 0, 0));
        ui.pointerDragged(5, 4, 0);
        ui.pointerUp(5, 4, 0, 0);

        assertEquals(0, pane.getScrollColumn());
        assertEquals(0, pane.getScrollRow());
    }

    @Test
    void secondaryButtonDoesNotStartAScroll() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertFalse(ui.pointerDown(3, 2, 0, 1));
        ui.pointerDragged(1, 1, 0);

        assertEquals(0, pane.getScrollColumn());
    }

    @Test
    void focusMovesScrollTheFocusedWidgetIntoView() {
        FocusBlock first = new FocusBlock(0, 0, 8, 2);
        FocusBlock second = new FocusBlock(0, 4, 8, 2);
        FocusBlock third = new FocusBlock(0, 8, 8, 2);
        FocusBlock last = new FocusBlock(0, 10, 8, 2);
        ScrollPane pane = paneWith(groupOf(first, second, third, last), 0, 0, 10, 5);

        ui.setKeyboardFocus(last);
        assertEquals(7, pane.getScrollRow());
        ui.setKeyboardFocus(first);
        assertEquals(0, pane.getScrollRow());
        ui.setKeyboardFocus(second);
        assertEquals(1, pane.getScrollRow());
    }

    @Test
    void tabNavigationKeepsTheFocusedWidgetInView() {
        FocusBlock first = new FocusBlock(0, 0, 8, 2);
        FocusBlock second = new FocusBlock(0, 4, 8, 2);
        FocusBlock third = new FocusBlock(0, 10, 8, 2);
        ScrollPane pane = paneWith(groupOf(first, second, third), 0, 0, 10, 5);
        ui.setKeyboardFocus(first);

        ui.keyDown(Input.Keys.TAB);
        assertEquals(1, pane.getScrollRow());
        ui.keyDown(Input.Keys.TAB);
        assertEquals(7, pane.getScrollRow());
        assertSame(third, ui.getKeyboardFocus());
    }

    @Test
    void focusFollowsHorizontally() {
        FocusBlock target = new FocusBlock(14, 0, 4, 1);
        ScrollPane pane = paneWith(groupOf(new Block(0, 0, 20, 2), target), 0, 0, 10, 5);

        ui.setKeyboardFocus(target);

        assertEquals(8, pane.getScrollColumn());
    }

    @Test
    void aPressDoesNotScrollTheViewToTheFocusedWidget() {
        FocusBlock second = new FocusBlock(0, 4, 8, 2);
        ScrollPane pane = paneWith(
                groupOf(new Block(0, 0, 8, 12), new FocusBlock(0, 0, 8, 2), second), 0, 0, 10, 5);

        ui.pointerDown(1, 4, 0, 0);

        assertSame(second, ui.getKeyboardFocus());
        assertEquals(0, pane.getScrollRow());
    }

    @Test
    void paneWithoutFocusableContentTakesFocusInTheTabOrder() {
        ScrollPane pane = pane(20, 12, 10, 5);

        assertTrue(ui.moveFocus(FocusDirection.NEXT));

        assertSame(pane, ui.getKeyboardFocus());
    }

    @Test
    void paneWithFocusableContentLeavesFocusToTheContent() {
        FocusBlock button = new FocusBlock(0, 0, 4, 1);
        ScrollPane pane = paneWith(groupOf(new Block(0, 0, 20, 12), button), 0, 0, 10, 5);

        assertTrue(ui.moveFocus(FocusDirection.NEXT));

        assertSame(button, ui.getKeyboardFocus());
        assertTrue(pane.isFocusable());
    }

    @Test
    void hiddenFocusableContentDoesNotCount() {
        FocusBlock button = new FocusBlock(0, 0, 4, 1);
        button.setVisible(false);
        ScrollPane pane = paneWith(groupOf(new Block(0, 0, 20, 12), button), 0, 0, 10, 5);

        assertTrue(ui.moveFocus(FocusDirection.NEXT));

        assertSame(pane, ui.getKeyboardFocus());
    }

    @Test
    void styleCanBeReplaced() {
        ScrollPane pane = pane(20, 12, 10, 5);
        ScrollPaneStyle other = ScrollPaneStyle.of(0x010101FF, 0x020202FF);

        pane.setStyle(other);
        ui.draw();

        assertSame(other, pane.getStyle());
        assertEquals(0x020202FF, grid.getForeground(9, 0));
    }

    @Test
    void verticalScrollStopsOnEveryRow() {
        ScrollPane pane = pane(20, 12, 10, 5);

        pane.setScrollRow(3);
        assertEquals(3, pane.getScrollRow());
        pane.setScrollRow(5);
        assertEquals(5, pane.getScrollRow());
        pane.setScrollPosition(1, 100);
        assertEquals(pane.getMaxScrollRow(), pane.getScrollRow());
        assertEquals(1, pane.getScrollColumn());
    }

    @Test
    void columnAndRowSettersClampAndKeepTheOtherAxis() {
        ScrollPane pane = pane(20, 12, 10, 5);

        pane.setScrollColumn(500);
        pane.setScrollRow(2);
        assertEquals(11, pane.getScrollColumn());
        assertEquals(2, pane.getScrollRow());
        pane.setScrollColumn(-4);
        pane.setScrollRow(-4);
        assertEquals(0, pane.getScrollColumn());
        assertEquals(0, pane.getScrollRow());
    }

    @Test
    void wheelStepCanBeChangedPerPane() {
        ScrollPane pane = pane(20, 12, 10, 5);
        ui.pointerMoved(3, 2);

        assertEquals(ScrollPane.DEFAULT_WHEEL_STEP_CELLS, pane.getWheelStepCells());
        pane.setWheelStepCells(1);
        assertTrue(ui.scrolled(0f, 2f));
        assertEquals(2, pane.getScrollRow());
        assertThrows(IllegalArgumentException.class, () -> pane.setWheelStepCells(0));
        assertEquals(1, pane.getWheelStepCells());
    }
}
