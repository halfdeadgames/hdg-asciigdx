package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WindowTest {

    private static final int BORDER_BACKGROUND = 0x101010FF;
    private static final int FOCUSED_BACKGROUND = 0x202020FF;
    private static final int TITLE_FOREGROUND = 0x303030FF;
    private static final int TITLE_BACKGROUND = 0x404040FF;
    private static final int WINDOW_BACKGROUND = 0x505050FF;
    private static final LabelStyle BORDER =
            LabelStyle.of(UiFont.SQUARE, 0xAAAAAAFF, BORDER_BACKGROUND);
    private static final LabelStyle FOCUSED_BORDER =
            LabelStyle.of(UiFont.SQUARE, 0xBBBBBBFF, FOCUSED_BACKGROUND);
    private static final LabelStyle TITLE =
            LabelStyle.of(UiFont.SQUARE, TITLE_FOREGROUND, TITLE_BACKGROUND);
    private static final ButtonStyle BUTTON = ButtonStyle.of(
            LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x600000FF),
            LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x700000FF),
            LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x800000FF),
            LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x900000FF));
    private static final WindowStyle PLAIN =
            WindowStyle.of(BoxStyle.SINGLE, BORDER, TITLE, WINDOW_BACKGROUND)
                    .withFocusedBorder(FOCUSED_BORDER);
    private static final WindowStyle CLOSABLE = PLAIN.withCloseButton(BUTTON);

    private final UiSurface surface = UiSurface.ofSize(40, 12);
    private final UiRoot ui = UiRoot.of(surface);
    private final TerminalGrid grid = surface.getSquareGrid();
    private final List<Window> closed = new ArrayList<>();

    private Window open(Window window, int x, int y) {
        window.setPosition(x, y);
        ui.getWindowLayer().addChild(window);
        ui.draw();
        return window;
    }

    private String rowText(int row, int fromColumn, int toColumn) {
        StringBuilder text = new StringBuilder();
        for (int column = fromColumn; column < toColumn; column++) {
            text.appendCodePoint(grid.getCodePoint(column, row));
        }
        return text.toString();
    }

    // Layout and size

    @Test
    void packFitsTheContentAndTheBorder() {
        Window window = new Window("", PLAIN);
        window.setContent(new TextButton("OK", BUTTON));

        window.pack();

        assertEquals(6, window.getWidth());
        assertEquals(3, window.getHeight());
    }

    @Test
    void packFitsTheWholeTitleWhenTheContentIsNarrower() {
        Window window = new Window("Hi", PLAIN);

        assertEquals(7, window.getWidth());
        assertEquals(2, window.getHeight());
    }

    @Test
    void packReservesRoomForTheCloseButtonNextToTheTitle() {
        assertEquals(10, new Window("T", CLOSABLE).getWidth());
        assertEquals(6, new Window("", CLOSABLE).getWidth());
    }

    @Test
    void contentSitsInsideTheBorderAndKeepsItsSize() {
        Window window = new Window("W", PLAIN);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        window.setSize(12, 6);

        open(window, 0, 0);

        assertEquals(1, content.getX());
        assertEquals(1, content.getY());
        assertEquals(4, content.getWidth());
        assertEquals(1, content.getHeight());
    }

    @Test
    void rejectedContentLeavesTheWindowUnchanged() {
        Window window = new Window("W", CLOSABLE);
        TextButton kept = new TextButton("A", BUTTON);
        window.setContent(kept);
        WidgetGroup owner = new WidgetGroup();
        TextButton owned = new TextButton("B", BUTTON);
        owner.addChild(owned);
        WidgetGroup parent = new WidgetGroup();
        parent.addChild(window);

        assertThrows(IllegalArgumentException.class, () -> window.setContent(owned));
        assertThrows(IllegalArgumentException.class, () -> window.setContent(parent));
        assertThrows(IllegalArgumentException.class, () -> window.setContent(window));

        assertSame(kept, window.getContent());
        assertSame(window, kept.getParent());
        assertEquals(2, window.getChildCount());
        assertSame(kept, window.getChild(0));
        assertSame(owner, owned.getParent());
    }

    private static final class FieldReadingWindow extends Window {
        private final int[] extra = {1, 2, 3};

        FieldReadingWindow() {
            super("W", CLOSABLE);
        }

        @Override
        public int getPreferredWidth() {
            return super.getPreferredWidth() + extra.length;
        }
    }

    @Test
    void constructorDoesNotCallOverridableSizeMethods() {
        FieldReadingWindow window = new FieldReadingWindow();

        window.pack();

        assertEquals(new Window("W", CLOSABLE).getWidth() + 3, window.getWidth());
    }

    @Test
    void contentApiFollowsTheNoSentinelRule() {
        Window window = new Window("W", PLAIN);
        TextButton first = new TextButton("A", BUTTON);
        TextButton second = new TextButton("B", BUTTON);

        assertFalse(window.hasContent());
        assertThrows(IllegalStateException.class, window::getContent);

        window.setContent(first);
        assertTrue(window.hasContent());
        assertSame(first, window.getContent());

        window.setContent(second);
        assertSame(second, window.getContent());
        assertNull(first.getParent());

        window.clearContent();
        assertFalse(window.hasContent());
        assertNull(second.getParent());
        assertThrows(NullPointerException.class, () -> window.setContent(null));
    }

    @Test
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new Window(null, PLAIN));
        assertThrows(NullPointerException.class, () -> new Window("W", null));
    }

    // Drawing

    @Test
    void titleSitsInsideTheTopBorderRow() {
        open(new Window("Hi", PLAIN), 2, 1);

        assertEquals("┌─ Hi ┐", rowText(1, 2, 9));
        assertEquals("└─────┘", rowText(2, 2, 9));
        assertEquals(TITLE_FOREGROUND, grid.getForeground(5, 1));
        assertEquals(TITLE_BACKGROUND, grid.getBackground(4, 1));
        assertEquals(TITLE_BACKGROUND, grid.getBackground(7, 1));
        assertEquals(BORDER_BACKGROUND, grid.getBackground(3, 1));
        assertEquals(BORDER_BACKGROUND, grid.getBackground(8, 1));
    }

    @Test
    void aTitleThatIsTooLongIsCutWithAnEllipsis() {
        Window window = new Window("Hello World", PLAIN);
        window.setSize(10, 3);

        open(window, 0, 0);

        assertEquals("┌─ Hell… ┐", rowText(0, 0, 10));
    }

    @Test
    void noTitleIsDrawnWhenNoCellIsLeft() {
        Window window = new Window("Hello", PLAIN);
        window.setSize(5, 3);

        open(window, 0, 0);

        assertEquals("┌───┐", rowText(0, 0, 5));
    }

    @Test
    void closeButtonSitsAtTheRightEndWithOneBorderGlyphBeforeTheCorner() {
        open(new Window("T", CLOSABLE), 0, 0);

        assertEquals('┌', grid.getCodePoint(0, 0));
        assertEquals('T', grid.getCodePoint(3, 0));
        assertEquals(' ', grid.getCodePoint(4, 0));
        assertEquals('x', grid.getCodePoint(6, 0));
        assertEquals('─', grid.getCodePoint(8, 0));
        assertEquals('┐', grid.getCodePoint(9, 0));
        assertEquals(0x600000FF, grid.getBackground(5, 0));
        assertEquals(0x600000FF, grid.getBackground(7, 0));
    }

    @Test
    void titleIsCutBeforeTheCloseButton() {
        Window window = new Window("Long title", CLOSABLE);
        window.setSize(12, 3);

        open(window, 0, 0);

        // The button starts at column 7, so the title gets 3 cells (columns 3 to 5), then a space.
        assertEquals('L', grid.getCodePoint(3, 0));
        assertEquals('o', grid.getCodePoint(4, 0));
        assertEquals('…', grid.getCodePoint(5, 0));
        assertEquals(' ', grid.getCodePoint(6, 0));
        assertEquals('x', grid.getCodePoint(8, 0));
    }

    @Test
    void insideIsFilledWithTheBackgroundAndHasNoGlyphs() {
        Window window = new Window("W", PLAIN);
        window.setSize(8, 5);
        open(window, 10, 2);

        assertEquals(WINDOW_BACKGROUND, grid.getBackground(12, 4));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(12, 4));
        assertEquals('│', grid.getCodePoint(10, 4));
        assertEquals('┘', grid.getCodePoint(17, 6));
    }

    @Test
    void setTitleRedrawsAndNullIsRejected() {
        Window window = open(new Window("Aa", PLAIN), 0, 0);

        window.setTitle("Bb");
        ui.draw();

        assertEquals("┌─ Bb ┐", rowText(0, 0, 7));
        assertEquals("Bb", window.getTitle());
        assertThrows(NullPointerException.class, () -> window.setTitle(null));
    }

    @Test
    void borderUsesTheFocusedLookWhileFocusIsInsideTheWindow() {
        Window window = new Window("W", PLAIN);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        window.pack();
        open(window, 0, 0);
        assertEquals(BORDER_BACKGROUND, grid.getBackground(0, 0));

        ui.setKeyboardFocus(content);
        ui.draw();
        assertEquals(FOCUSED_BACKGROUND, grid.getBackground(0, 0));
        assertEquals(FOCUSED_BACKGROUND, grid.getBackground(5, 2));
        assertEquals(0xBBBBBBFF, grid.getForeground(0, 0));

        ui.setKeyboardFocus(null);
        ui.draw();
        assertEquals(BORDER_BACKGROUND, grid.getBackground(0, 0));
    }

    // Closing

    @Test
    void closeRemovesTheWindowThenNotifiesTheListeners() {
        Window window = open(new Window("W", PLAIN), 0, 0);
        List<Integer> childCountsSeen = new ArrayList<>();
        window.addCloseListener(done -> {
            closed.add(done);
            childCountsSeen.add(ui.getWindowLayer().getChildCount());
        });

        window.close();

        assertEquals(List.of(window), closed);
        assertEquals(List.of(0), childCountsSeen);
        assertNull(window.getParent());
    }

    @Test
    void closeWithoutAParentStillNotifies() {
        Window window = new Window("W", PLAIN);
        window.addCloseListener(closed::add);

        window.close();

        assertEquals(List.of(window), closed);
    }

    @Test
    void removedCloseListenersAreNotCalled() {
        Window window = new Window("W", PLAIN);
        WindowCloseListener listener = closed::add;
        window.addCloseListener(listener);
        window.removeCloseListener(listener);

        window.close();

        assertTrue(closed.isEmpty());
    }

    @Test
    void clickingTheCloseButtonClosesTheWindow() {
        Window window = open(new Window("T", CLOSABLE), 0, 0);
        window.addCloseListener(closed::add);

        assertTrue(ui.pointerDown(6, 0, 0, 0));
        assertEquals(1, ui.getWindowLayer().getChildCount());
        assertTrue(ui.pointerUp(6, 0, 0, 0));

        assertEquals(List.of(window), closed);
        assertEquals(0, ui.getWindowLayer().getChildCount());
    }

    @Test
    void closeButtonShowsItsHoverAndPressedLooks() {
        open(new Window("T", CLOSABLE), 0, 0);

        ui.pointerMoved(6, 0);
        ui.draw();
        assertEquals(0x700000FF, grid.getBackground(6, 0));

        ui.pointerDown(6, 0, 0, 0);
        ui.draw();
        assertEquals(0x800000FF, grid.getBackground(6, 0));
    }

    @Test
    void releasingOutsideTheCloseButtonDoesNotClose() {
        Window window = open(new Window("T", CLOSABLE), 0, 0);
        window.addCloseListener(closed::add);

        ui.pointerDown(6, 0, 0, 0);
        ui.pointerUp(20, 8, 0, 0);

        assertTrue(closed.isEmpty());
        assertEquals(1, ui.getWindowLayer().getChildCount());
    }

    @Test
    void escapeClosesAWindowThatIsSetToCloseOnEscape() {
        Window window = new Window("W", PLAIN);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        window.setCloseOnEscape(true);
        window.addCloseListener(closed::add);
        open(window, 0, 0);
        ui.setKeyboardFocus(content);

        assertTrue(ui.keyDown(Input.Keys.ESCAPE));

        assertEquals(List.of(window), closed);
        assertEquals(0, ui.getWindowLayer().getChildCount());
        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void escapeDoesNothingByDefault() {
        Window window = new Window("W", PLAIN);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        open(window, 0, 0);
        ui.setKeyboardFocus(content);

        assertFalse(window.isCloseOnEscape());
        assertFalse(ui.keyDown(Input.Keys.ESCAPE));

        assertEquals(1, ui.getWindowLayer().getChildCount());
    }

    // Dragging

    private Window sizedWindow(int x, int y) {
        Window window = new Window("W", PLAIN);
        window.setSize(10, 5);
        return open(window, x, y);
    }

    @Test
    void draggingTheTopBorderMovesTheWindowByWholeCells() {
        Window window = sizedWindow(5, 4);

        assertTrue(ui.pointerDown(7, 4, 0, 0));
        assertTrue(ui.pointerDragged(12, 6, 0));
        assertEquals(10, window.getX());
        assertEquals(6, window.getY());

        assertTrue(ui.pointerDragged(13, 7, 0));
        assertEquals(11, window.getX());
        assertEquals(7, window.getY());
        assertTrue(ui.pointerUp(13, 7, 0, 0));

        assertFalse(ui.pointerDragged(20, 9, 0));
        assertEquals(11, window.getX());
    }

    @Test
    void draggedWindowIsDrawnAtTheNewPlace() {
        sizedWindow(5, 4);

        ui.pointerDown(7, 4, 0, 0);
        ui.pointerDragged(12, 6, 0);
        ui.draw();

        assertEquals('┌', grid.getCodePoint(10, 6));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(5, 4));
    }

    @Test
    void dragIsClampedSoTheTopRowStaysInsideTheSurface() {
        Window window = sizedWindow(5, 4);

        ui.pointerDown(7, 4, 0, 0);
        ui.pointerDragged(500, 500, 0);
        assertEquals(30, window.getX());
        assertEquals(11, window.getY());

        ui.pointerDragged(-500, -500, 0);
        assertEquals(0, window.getX());
        assertEquals(0, window.getY());
    }

    @Test
    void pressingBelowTheTopRowDoesNotDrag() {
        Window window = sizedWindow(5, 4);

        assertTrue(ui.pointerDown(7, 6, 0, 0));
        ui.pointerDragged(20, 9, 0);

        assertEquals(5, window.getX());
        assertEquals(4, window.getY());
    }

    @Test
    void anImmovableWindowIgnoresDrags() {
        Window window = sizedWindow(5, 4);
        window.setMovable(false);

        assertFalse(window.isMovable());
        ui.pointerDown(7, 4, 0, 0);
        ui.pointerDragged(20, 9, 0);

        assertEquals(5, window.getX());
        assertEquals(4, window.getY());
    }

    @Test
    void aNewWindowIsMovable() {
        assertTrue(new Window("W", PLAIN).isMovable());
    }

    @Test
    void pressingTheCloseButtonDoesNotStartADrag() {
        Window window = open(new Window("T", CLOSABLE), 3, 3);

        ui.pointerDown(9, 3, 0, 0);
        ui.pointerDragged(20, 9, 0);

        assertEquals(3, window.getX());
        assertEquals(3, window.getY());
    }

    @Test
    void aWindowInTheMainTreeDragsAndClampsToo() {
        Window window = new Window("W", PLAIN);
        window.setSize(10, 5);
        window.setPosition(3, 3);
        ui.getRoot().addChild(window);
        ui.draw();

        ui.pointerDown(4, 3, 0, 0);
        ui.pointerDragged(500, -500, 0);

        assertEquals(30, window.getX());
        assertEquals(0, window.getY());
    }

    @Test
    void aDragIsClampedAgainstTheSurfaceWhenTheLayerIsOffset() {
        ui.getWindowLayer().setPosition(2, 1);
        Window window = sizedWindow(5, 4);

        ui.pointerDown(7 + 2, 4 + 1, 0, 0);
        ui.pointerDragged(500, 500, 0);

        assertEquals(30 - 2, window.getX());
        assertEquals(11 - 1, window.getY());
    }

    // Drag bounds

    private Window pressTitleAndDragToBottomRight(Window window) {
        ui.pointerDown(window.getX() + 2, window.getY(), 0, 0);
        ui.pointerDragged(500, 500, 0);
        return window;
    }

    private void dragWindowToTopLeft() {
        ui.pointerDragged(-500, -500, 0);
    }

    @Test
    void dragIsClampedInsideTheDragBoundsOnAllFourSides() {
        ui.setWindowDragBounds(new CellArea(10, 3, 20, 6));
        Window window = sizedWindow(12, 4);

        pressTitleAndDragToBottomRight(window);
        assertEquals(20, window.getX());
        assertEquals(8, window.getY());

        dragWindowToTopLeft();
        assertEquals(10, window.getX());
        assertEquals(3, window.getY());
    }

    @Test
    void aWindowWiderThanTheDragBoundsPinsItsLeftEdge() {
        ui.setWindowDragBounds(new CellArea(10, 3, 6, 6));
        Window window = sizedWindow(12, 4);

        pressTitleAndDragToBottomRight(window);
        assertEquals(10, window.getX());
        assertEquals(8, window.getY());

        dragWindowToTopLeft();
        assertEquals(10, window.getX());
    }

    @Test
    void aDragBoundsPastTheSurfaceIsIntersectedWithIt() {
        ui.setWindowDragBounds(new CellArea(25, 2, 50, 8));
        Window window = sizedWindow(12, 4);

        pressTitleAndDragToBottomRight(window);
        assertEquals(30, window.getX());
        assertEquals(9, window.getY());

        dragWindowToTopLeft();
        assertEquals(25, window.getX());
        assertEquals(2, window.getY());
    }

    @Test
    void aDragBoundsFullyOutsideTheSurfaceFallsBackToTheSurface() {
        ui.setWindowDragBounds(new CellArea(50, 0, 5, 5));
        Window window = sizedWindow(12, 4);

        pressTitleAndDragToBottomRight(window);
        assertEquals(30, window.getX());
        assertEquals(11, window.getY());

        dragWindowToTopLeft();
        assertEquals(0, window.getX());
        assertEquals(0, window.getY());
    }

    @Test
    void aHugeDragBoundsDoesNotOverflowAndClampsToTheSurfaceEdges() {
        ui.setWindowDragBounds(new CellArea(0, 1, Integer.MAX_VALUE, Integer.MAX_VALUE));
        Window window = sizedWindow(12, 4);

        pressTitleAndDragToBottomRight(window);
        assertEquals(30, window.getX());
        assertEquals(11, window.getY());

        dragWindowToTopLeft();
        assertEquals(0, window.getX());
        assertEquals(1, window.getY());
    }

    @Test
    void clearingTheDragBoundsRestoresTheSurfaceClamp() {
        ui.setWindowDragBounds(new CellArea(10, 3, 20, 6));
        ui.clearWindowDragBounds();
        Window window = sizedWindow(12, 4);

        pressTitleAndDragToBottomRight(window);

        assertEquals(30, window.getX());
        assertEquals(11, window.getY());
    }

    @Test
    void dragBoundsAreReadableOnlyWhileSet() {
        assertFalse(ui.hasWindowDragBounds());
        assertThrows(IllegalStateException.class, ui::getWindowDragBounds);

        CellArea area = new CellArea(1, 2, 3, 4);
        ui.setWindowDragBounds(area);
        assertTrue(ui.hasWindowDragBounds());
        assertEquals(area, ui.getWindowDragBounds());

        ui.clearWindowDragBounds();
        assertFalse(ui.hasWindowDragBounds());
        assertThrows(IllegalStateException.class, ui::getWindowDragBounds);
    }

    @Test
    void settingANullDragBoundsThrows() {
        assertThrows(NullPointerException.class, () -> ui.setWindowDragBounds(null));
    }

    @Test
    void setPositionIgnoresTheDragBounds() {
        ui.setWindowDragBounds(new CellArea(10, 3, 20, 6));
        Window window = sizedWindow(12, 4);

        window.setPosition(0, 11);

        assertEquals(0, window.getX());
        assertEquals(11, window.getY());
    }

    // Stacking and focus

    @Test
    void pressingAnywhereOnAWindowBringsItToTheFront() {
        Window back = open(new Window("A", PLAIN), 0, 0);
        back.setSize(10, 4);
        Window front = open(new Window("B", PLAIN), 5, 0);
        front.setSize(10, 4);
        ui.draw();

        ui.pointerDown(1, 3, 0, 0);

        assertSame(back, ui.getWindowLayer().getChild(1));
        assertSame(front, ui.getWindowLayer().getChild(0));
        ui.pointerUp(1, 3, 0, 0);
        ui.pointerMoved(6, 2);
        assertSame(back, ui.getHoveredWidget());
    }

    @Test
    void pressingAConsumingChildBringsTheWindowToTheFrontToo() {
        Window back = new Window("A", PLAIN);
        back.setContent(new TextButton("OK", BUTTON));
        open(back, 0, 0);
        open(new Window("B", PLAIN), 20, 0);

        ui.pointerDown(2, 1, 0, 0);

        assertSame(back, ui.getWindowLayer().getChild(1));
    }

    @Test
    void bringingAWindowToTheFrontKeepsFocusInsideIt() {
        Window back = new Window("A", PLAIN);
        TextButton content = new TextButton("OK", BUTTON);
        back.setContent(content);
        open(back, 0, 0);
        open(new Window("B", PLAIN), 20, 0);
        ui.setKeyboardFocus(content);

        back.toFront();

        assertSame(content, ui.getKeyboardFocus());
        assertSame(back, ui.getWindowLayer().getChild(1));
    }

    private Window windowWithContent(TextButton content) {
        Window window = new Window("W", PLAIN);
        if (content != null) {
            window.setContent(content);
        }
        window.setSize(12, 5);
        return open(window, 0, 0);
    }

    private TextButton rootButton() {
        TextButton button = new TextButton("Go", BUTTON);
        button.setPosition(30, 10);
        ui.getRoot().addChild(button);
        return button;
    }

    @Test
    void pressingTheWindowAreaKeepsFocusThatIsAlreadyInsideIt() {
        TextButton content = new TextButton("OK", BUTTON);
        Window window = windowWithContent(content);
        ui.setKeyboardFocus(content);

        assertTrue(ui.pointerDown(8, 3, 0, 0));
        ui.pointerUp(8, 3, 0, 0);
        assertSame(content, ui.getKeyboardFocus());

        assertTrue(ui.pointerDown(5, 0, 0, 0));
        assertSame(content, ui.getKeyboardFocus());
        ui.pointerDragged(9, 2, 0);
        assertEquals(4, window.getX());
        assertEquals(2, window.getY());
    }

    @Test
    void pressingTheWindowAreaFocusesItsFirstWidgetWhenFocusIsElsewhere() {
        TextButton content = new TextButton("OK", BUTTON);
        windowWithContent(content);
        TextButton outside = rootButton();
        ui.setKeyboardFocus(outside);

        assertTrue(ui.pointerDown(8, 3, 0, 0));

        assertSame(content, ui.getKeyboardFocus());
        assertFalse(ui.isFocusVisible());
    }

    @Test
    void pressingTheWindowAreaFocusesItsFirstWidgetWhenNothingHasFocus() {
        TextButton content = new TextButton("OK", BUTTON);
        windowWithContent(content);

        ui.pointerDown(8, 3, 0, 0);

        assertSame(content, ui.getKeyboardFocus());
    }

    @Test
    void pressingAWindowWithNothingFocusableClearsFocus() {
        windowWithContent(null);
        TextButton outside = rootButton();
        ui.setKeyboardFocus(outside);

        ui.pointerDown(8, 3, 0, 0);

        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void toFrontWithoutAParentDoesNothing() {
        new Window("W", PLAIN).toFront();
    }

    @Test
    void aPressOnTheBorderIsConsumed() {
        sizedWindow(5, 4);

        assertTrue(ui.pointerDown(6, 7, 0, 0));
        assertTrue(ui.pointerDown(5, 4, 1, 0));
    }

    @Test
    void closeButtonTakesPartInKeyboardNavigationAfterTheContent() {
        Window window = new Window("T", CLOSABLE);
        TextButton content = new TextButton("OK", BUTTON);
        window.setContent(content);
        open(window, 0, 0);
        ui.setKeyboardFocus(content);

        ui.keyDown(Input.Keys.TAB);
        assertTrue(ui.getKeyboardFocus() instanceof TextButton);
        assertFalse(ui.getKeyboardFocus() == content);
        ui.keyDown(Input.Keys.TAB);
        assertSame(content, ui.getKeyboardFocus());
    }
}
