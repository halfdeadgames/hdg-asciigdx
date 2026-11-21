package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiInputTest {
    private static final int TEXT_COLUMN_PIXELS = 5;
    private static final int ROW_PIXELS = 10;

    private final UiRoot root = UiRoot.of(UiSurface.ofSize(20, 10));
    // The picker reports TEXT columns: a text column is half a UI cell wide.
    private final UiInput input = UiInput.forRoot(root,
            (screenX, screenY, cellOut) -> {
                cellOut.x = screenX / TEXT_COLUMN_PIXELS;
                cellOut.y = screenY / ROW_PIXELS;
            });
    private final List<String> log = new ArrayList<>();

    private static final class Box extends Widget {
        Box() {
            setSize(5, 5);
        }

        @Override
        public boolean isFocusable() {
            return true;
        }

        @Override
        public int getPreferredWidth() {
            return 5;
        }

        @Override
        public int getPreferredHeight() {
            return 5;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
        }
    }

    private Box addLoggingBox() {
        Box box = new Box();
        box.addListener(event -> {
            if (event.getType() == UiEventType.POINTER_ENTERED
                    || event.getType() == UiEventType.POINTER_EXITED) {
                return false;
            }
            log.add(event.getType() + " " + event.getSurfaceColumn() + ","
                    + event.getSurfaceRow() + " p" + event.getPointer() + " b"
                    + event.getButton());
            return true;
        });
        root.getRoot().addChild(box);
        return box;
    }

    @Test
    void pointerCallsAreMappedThroughThePicker() {
        addLoggingBox();

        assertTrue(input.touchDown(25, 35, 0, 0));
        assertTrue(input.touchDragged(26, 36, 0));
        assertTrue(input.touchUp(27, 37, 0, 0));
        assertTrue(input.mouseMoved(15, 15));

        assertEquals(List.of("POINTER_DOWN 2,3 p0 b0", "POINTER_DRAGGED 2,3 p0 b-1",
                "POINTER_UP 2,3 p0 b0", "POINTER_MOVED 1,1 p0 b-1"), log);
    }

    private Box addTextColumnLoggingBox(List<String> columns) {
        Box box = new Box();
        box.addListener(event -> {
            if (event.getType() == UiEventType.POINTER_DOWN
                    || event.getType() == UiEventType.POINTER_UP
                    || event.getType() == UiEventType.POINTER_DRAGGED
                    || event.getType() == UiEventType.POINTER_MOVED) {
                columns.add(event.getType() + " square=" + event.getSurfaceColumn()
                        + " text=" + event.getSurfaceTextColumn()
                        + " local=" + event.getColumn() + "/" + event.getTextColumn());
            }
            return true;
        });
        root.getRoot().addChild(box);
        return box;
    }

    @Test
    void anEvenTextColumnGivesTheSquareColumnAndTheExactTextColumn() {
        List<String> columns = new ArrayList<>();
        addTextColumnLoggingBox(columns);

        // Text column 4 is the left half of square column 2.
        input.touchDown(20, 35, 0, 0);

        assertEquals(List.of("POINTER_DOWN square=2 text=4 local=2/4"), columns);
    }

    @Test
    void anOddTextColumnGivesTheSameSquareColumnAndItsOwnTextColumn() {
        List<String> columns = new ArrayList<>();
        addTextColumnLoggingBox(columns);

        // Text column 5 is the right half of square column 2.
        input.touchDown(25, 35, 0, 0);

        assertEquals(List.of("POINTER_DOWN square=2 text=5 local=2/5"), columns);
    }

    @Test
    void everyPointerKindKeepsTheExactTextColumn() {
        List<String> columns = new ArrayList<>();
        addTextColumnLoggingBox(columns);

        input.touchDown(10, 5, 0, 0);
        input.touchDragged(15, 5, 0);
        input.touchUp(15, 5, 0, 0);
        input.mouseMoved(5, 5);

        assertEquals(List.of(
                "POINTER_DOWN square=1 text=2 local=1/2",
                "POINTER_DRAGGED square=1 text=3 local=1/3",
                "POINTER_UP square=1 text=3 local=1/3",
                "POINTER_MOVED square=0 text=1 local=0/1"), columns);
    }

    @Test
    void aWidgetNotAtTheLeftEdgeSeesTheTextColumnRelativeToItself() {
        List<String> columns = new ArrayList<>();
        Box box = addTextColumnLoggingBox(columns);
        box.setPosition(3, 0);

        // Text column 7 is the right half of square column 3, the box's first column.
        input.touchDown(35, 5, 0, 0);

        assertEquals(List.of("POINTER_DOWN square=3 text=7 local=0/1"), columns);
    }

    @Test
    void rootPointerMethodsCalledDirectlyGiveTheLeftHalfTextColumn() {
        List<String> columns = new ArrayList<>();
        addTextColumnLoggingBox(columns);

        root.pointerDown(3, 2, 0, 0);
        root.pointerDragged(4, 2, 0);
        root.pointerUp(4, 2, 0, 0);
        root.pointerMoved(1, 1);

        assertEquals(List.of(
                "POINTER_DOWN square=3 text=6 local=3/6",
                "POINTER_DRAGGED square=4 text=8 local=4/8",
                "POINTER_UP square=4 text=8 local=4/8",
                "POINTER_MOVED square=1 text=2 local=1/2"), columns);
    }

    @Test
    void cancelledForwardsThePointer() {
        addLoggingBox();
        input.touchDown(15, 15, 2, 0);

        assertTrue(input.touchCancelled(0, 0, 2, 0));

        assertEquals("POINTER_CANCELLED 0,0 p2 b-1", log.get(log.size() - 1));
    }

    @Test
    void keysAreForwardedToTheFocusedWidget() {
        addLoggingBox();
        input.touchDown(15, 15, 0, 0);
        log.clear();

        assertTrue(input.keyDown(66));
        assertTrue(input.keyUp(66));
        assertTrue(input.keyTyped('a'));

        assertEquals(3, log.size());
        assertTrue(log.get(0).startsWith("KEY_DOWN"));
    }

    @Test
    void scrolledForwardsToTheHoveredWidgetAndUnhitInputIsNotConsumed() {
        addLoggingBox();

        assertFalse(input.scrolled(0f, 1f));
        input.mouseMoved(15, 15);
        log.clear();
        assertTrue(input.scrolled(0f, 1f));
        assertEquals(List.of("SCROLLED 0,0 p-1 b-1"), log);
        assertTrue(input.scrolled(0f, 1f));
        assertFalse(input.touchDown(190, 90, 0, 0));
        assertFalse(input.keyDown(66));
    }

    @Test
    void touchOnlyTapEndsHoverAfterUpAndSendsExited() {
        Box box = new Box();
        List<UiEventType> types = new ArrayList<>();
        box.addListener(event -> {
            types.add(event.getType());
            return false;
        });
        root.getRoot().addChild(box);

        input.touchDown(15, 15, 0, 0);
        assertSame(box, root.getHoveredWidget());
        input.touchUp(15, 15, 0, 0);

        assertNull(root.getHoveredWidget());
        assertEquals(UiEventType.POINTER_EXITED, types.get(types.size() - 1));
    }

    @Test
    void touchOnlyCancelEndsHoverToo() {
        addLoggingBox();
        input.touchDown(15, 15, 0, 0);

        input.touchCancelled(15, 15, 0, 0);

        assertNull(root.getHoveredWidget());
    }

    @Test
    void afterAMouseMoveUpKeepsHover() {
        Box box = addLoggingBox();
        input.mouseMoved(15, 15);
        input.touchDown(15, 15, 0, 0);

        input.touchUp(15, 15, 0, 0);

        assertSame(box, root.getHoveredWidget());
    }

    @Test
    void secondFingerUpDoesNotEndHover() {
        Box box = addLoggingBox();
        input.touchDown(15, 15, 0, 0);
        input.touchDown(15, 15, 1, 0);

        input.touchUp(15, 15, 1, 0);

        assertSame(box, root.getHoveredWidget());
    }

    @Test
    void pointersOutsideTheSupportedRangeAreIgnored() {
        addLoggingBox();

        assertFalse(input.touchDown(15, 15, UiRoot.MAX_POINTERS, 0));
        assertFalse(input.touchDown(15, 15, -1, 0));
        assertFalse(input.touchDragged(15, 15, UiRoot.MAX_POINTERS));
        assertFalse(input.touchUp(15, 15, UiRoot.MAX_POINTERS, 0));
        assertFalse(input.touchCancelled(15, 15, UiRoot.MAX_POINTERS, 0));

        assertTrue(log.isEmpty());
    }

    @Test
    void theProcessorsKeyboardListenerSkipsAMissingGdxInputAndLaterListenersStillRun() {
        List<Boolean> wanted = new ArrayList<>();
        root.addTextInputWantedListener(wanted::add);
        TextField field = new TextField(TextFieldStyle.of(
                LabelStyle.of(0xFFFFFFFF, 0x000000FF)), 10);
        root.getRoot().addChild(field);

        // Gdx.input is null in plain tests; the processor must skip it and later listeners run.
        root.setKeyboardFocus(field);

        assertEquals(List.of(true), wanted);
        assertTrue(root.isTextInputWanted());
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> UiInput.forRoot(null, (x, y, out) -> { }));
        assertThrows(NullPointerException.class, () -> UiInput.forRoot(root, null));
    }
}
