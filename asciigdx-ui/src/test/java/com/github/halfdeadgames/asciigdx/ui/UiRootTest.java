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

class UiRootTest {
    private static final int BLUE = 0x0000FFFF;
    private static final int RED = 0xFF0000FF;

    private final UiSurface surface = UiSurface.ofSize(60, 30);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> log = new ArrayList<>();

    private static final class Box extends Widget {
        private final int color;
        private boolean focusable;

        Box(int x, int y, int width, int height, int color) {
            this.color = color;
            setPosition(x, y);
            setSize(width, height);
        }

        @Override
        public boolean isFocusable() {
            return focusable;
        }

        @Override
        public int getPreferredWidth() {
            return getWidth();
        }

        @Override
        public int getPreferredHeight() {
            return getHeight();
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
            target.fillBackground(left, top, getWidth(), getHeight(), color);
        }
    }

    private Box box(WidgetGroup parent, int x, int y, int width, int height) {
        Box box = new Box(x, y, width, height, BLUE);
        parent.addChild(box);
        return box;
    }

    private void track(Widget widget, String name, boolean result) {
        widget.addListener(event -> {
            log.add(event.getType() + " " + name);
            return result;
        });
    }

    @Test
    void routesPointerToDeepestWidgetWithRelativeCoordinates() {
        WidgetGroup group = new WidgetGroup();
        group.setPosition(10, 0);
        ui.getRoot().addChild(group);
        Box inner = box(group, 1, 1, 3, 3);
        int[] seen = new int[4];
        inner.addListener(event -> {
            seen[0] = event.getColumn();
            seen[1] = event.getRow();
            seen[2] = event.getSurfaceColumn();
            seen[3] = event.getSurfaceRow();
            assertSame(inner, event.getTarget());
            assertSame(inner, event.getCurrentWidget());
            return true;
        });

        assertTrue(ui.pointerDown(12, 2, 0, 0));

        assertEquals(1, seen[0]);
        assertEquals(1, seen[1]);
        assertEquals(12, seen[2]);
        assertEquals(2, seen[3]);
    }

    @Test
    void pointerOutsideSurfaceHitsNothing() {
        Box wide = box(ui.getRoot(), -5, -5, 200, 200);
        track(wide, "wide", true);

        assertFalse(ui.pointerDown(-1, 0, 0, 0));
        assertFalse(ui.pointerMoved(60, 5));
        assertTrue(log.isEmpty());
    }

    @Test
    void bubblingStopsWhenHandled() {
        WidgetGroup parent = new WidgetGroup();
        ui.getRoot().addChild(parent);
        Box child = box(parent, 0, 0, 5, 5);
        track(child, "child", false);
        track(parent, "parent", true);
        track(ui.getRoot(), "root", true);

        assertTrue(ui.pointerDown(1, 1, 0, 0));

        assertEquals(List.of("POINTER_DOWN child", "POINTER_DOWN parent"), log.subList(
                log.size() - 2, log.size()));
        assertFalse(log.contains("POINTER_DOWN root"));
    }

    @Test
    void unhandledEventReportsFalse() {
        Box child = box(ui.getRoot(), 0, 0, 5, 5);
        track(child, "child", false);

        assertFalse(ui.pointerDown(1, 1, 0, 0));
    }

    @Test
    void popupLayerGroupItselfIsNeverHit() {
        ui.getPopupLayer().setSize(60, 30);
        track(ui.getPopupLayer(), "layer", true);

        assertFalse(ui.pointerDown(5, 5, 0, 0));
        assertNull(ui.getHoveredWidget());
        assertTrue(log.isEmpty());
    }

    @Test
    void popupChildWinsOverOverlappingRootChild() {
        Box below = box(ui.getRoot(), 0, 0, 10, 10);
        Box above = box(ui.getPopupLayer(), 0, 0, 10, 10);
        track(below, "below", true);
        track(above, "above", true);

        ui.pointerDown(3, 3, 0, 0);

        assertEquals("POINTER_DOWN above", log.get(log.size() - 1));
        assertFalse(log.contains("POINTER_DOWN below"));
    }

    @Test
    void hoverChangeSendsExitedDeepestFirstThenEnteredOutermostFirst() {
        WidgetGroup left = new WidgetGroup();
        WidgetGroup right = new WidgetGroup();
        ui.getRoot().addChild(left);
        ui.getRoot().addChild(right);
        Box leftLeaf = box(left, 0, 0, 5, 5);
        Box rightLeaf = box(right, 10, 0, 5, 5);
        track(ui.getRoot(), "root", false);
        track(left, "left", false);
        track(right, "right", false);
        track(leftLeaf, "leftLeaf", false);
        track(rightLeaf, "rightLeaf", false);

        ui.pointerMoved(1, 1);
        assertEquals(List.of("POINTER_ENTERED root", "POINTER_ENTERED left",
                "POINTER_ENTERED leftLeaf", "POINTER_MOVED leftLeaf", "POINTER_MOVED left",
                "POINTER_MOVED root"), log);
        assertSame(leftLeaf, ui.getHoveredWidget());

        log.clear();
        ui.pointerMoved(11, 1);
        assertEquals(List.of("POINTER_EXITED leftLeaf", "POINTER_EXITED left",
                "POINTER_ENTERED right", "POINTER_ENTERED rightLeaf",
                "POINTER_MOVED rightLeaf", "POINTER_MOVED right", "POINTER_MOVED root"), log);
        assertSame(rightLeaf, ui.getHoveredWidget());
    }

    @Test
    void leavingEverythingSendsExitedAndClearsHover() {
        Box only = box(ui.getRoot(), 0, 0, 5, 5);
        track(only, "only", false);
        ui.pointerMoved(1, 1);
        log.clear();

        ui.pointerMoved(30, 20);

        assertEquals(List.of("POINTER_EXITED only"), log);
        assertNull(ui.getHoveredWidget());
    }

    @Test
    void pressCaptureSendsDragUpAndCancelOutsideTheWidget() {
        Box target = box(ui.getRoot(), 0, 0, 5, 5);
        int[] dragColumn = new int[1];
        target.addListener(event -> {
            if (event.getType() == UiEventType.POINTER_ENTERED
                    || event.getType() == UiEventType.POINTER_EXITED) {
                return false;
            }
            log.add(event.getType().toString());
            if (event.getType() == UiEventType.POINTER_DRAGGED) {
                dragColumn[0] = event.getColumn();
            }
            return true;
        });

        ui.pointerDown(1, 1, 0, 0);
        assertTrue(ui.pointerDragged(40, 20, 0));
        assertEquals(40, dragColumn[0]);
        assertTrue(ui.pointerUp(40, 20, 0, 0));
        assertFalse(ui.pointerDragged(40, 20, 0));
        assertFalse(ui.pointerUp(40, 20, 0, 0));

        ui.pointerDown(1, 1, 1, 0);
        assertTrue(ui.pointerCancelled(1));
        assertFalse(ui.pointerCancelled(1));
        assertEquals(List.of("POINTER_DOWN", "POINTER_DRAGGED", "POINTER_UP", "POINTER_DOWN",
                "POINTER_CANCELLED"), log);
    }

    @Test
    void hoverStillTracksWidgetUnderPointerDuringCapture() {
        box(ui.getRoot(), 0, 0, 5, 5);
        Box second = box(ui.getRoot(), 10, 0, 5, 5);
        track(second, "second", false);
        ui.pointerDown(1, 1, 0, 0);

        ui.pointerDragged(11, 1, 0);

        assertSame(second, ui.getHoveredWidget());
        assertTrue(log.contains("POINTER_ENTERED second"));
        assertFalse(log.contains("POINTER_DRAGGED second"));
    }

    @Test
    void pointerDownFocusesDeepestFocusableAncestorOrSelf() {
        Box parentBox = box(ui.getRoot(), 0, 0, 10, 10);
        parentBox.focusable = true;
        WidgetGroup group = new WidgetGroup();
        ui.getRoot().addChild(group);
        group.setPosition(20, 0);
        box(group, 0, 0, 5, 5);
        Box focusableChild = box(group, 6, 0, 5, 5);
        focusableChild.focusable = true;

        ui.pointerDown(1, 1, 0, 0);
        assertSame(parentBox, ui.getKeyboardFocus());
        ui.pointerUp(1, 1, 0, 0);

        ui.pointerDown(26, 1, 0, 0);
        assertSame(focusableChild, ui.getKeyboardFocus());
        ui.pointerUp(26, 1, 0, 0);

        ui.pointerDown(21, 1, 0, 0);
        assertNull(ui.getKeyboardFocus());
    }

    @Test
    void keysGoToFocusAndBubble() {
        WidgetGroup group = new WidgetGroup();
        ui.getRoot().addChild(group);
        Box focused = box(group, 0, 0, 5, 5);
        track(focused, "focused", false);
        track(group, "group", true);
        ui.setKeyboardFocus(focused);

        assertTrue(ui.keyDown(66));
        assertTrue(ui.keyUp(66));
        assertTrue(ui.keyTyped('x'));

        assertEquals(List.of("KEY_DOWN focused", "KEY_DOWN group", "KEY_UP focused",
                "KEY_UP group", "KEY_TYPED focused", "KEY_TYPED group"), log);
    }

    @Test
    void keyEventCarriesKeycodeAndCharacter() {
        Box focused = box(ui.getRoot(), 0, 0, 5, 5);
        int[] seen = new int[3];
        focused.addListener(event -> {
            seen[0] = event.getKeycode();
            seen[1] = event.getCharacter();
            seen[2] = event.getPointer();
            assertSame(focused, event.getTarget());
            return true;
        });
        ui.setKeyboardFocus(focused);

        ui.keyDown(62);
        assertEquals(62, seen[0]);
        assertEquals(-1, seen[2]);
        ui.keyTyped('q');
        assertEquals('q', seen[1]);
        assertEquals(-1, seen[0]);
    }

    @Test
    void keysWithoutFocusAreNotHandled() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        track(box, "box", true);

        assertFalse(ui.keyDown(66));
        assertFalse(ui.keyTyped('a'));
        assertTrue(log.isEmpty());
    }

    @Test
    void removedFocusWidgetCountsAsNoFocus() {
        Box focused = box(ui.getRoot(), 0, 0, 5, 5);
        track(focused, "focused", true);
        ui.setKeyboardFocus(focused);
        ui.getRoot().removeChild(focused);

        assertNull(ui.getKeyboardFocus());
        assertFalse(ui.keyDown(66));
        assertTrue(log.isEmpty());
    }

    @Test
    void focusInPopupLayerCountsAsFocus() {
        Box popupBox = box(ui.getPopupLayer(), 0, 0, 5, 5);
        ui.setKeyboardFocus(popupBox);

        assertSame(popupBox, ui.getKeyboardFocus());
    }

    @Test
    void updateListenersGetTheDeltaAndCanBeRemoved() {
        List<Float> deltas = new ArrayList<>();
        UiUpdateListener listener = deltas::add;
        ui.addUpdateListener(listener);

        ui.update(0.25f);
        ui.removeUpdateListener(listener);
        ui.update(0.5f);

        assertEquals(List.of(0.25f), deltas);
    }

    @Test
    void updateListenerMayAddAnotherListenerDuringUpdate() {
        List<String> calls = new ArrayList<>();
        ui.addUpdateListener(delta -> {
            calls.add("first");
            ui.addUpdateListener(later -> calls.add("second"));
        });

        ui.update(0.1f);
        ui.update(0.1f);

        assertEquals(List.of("first", "first", "second"), calls);
    }

    @Test
    void invalidDeltaThrows() {
        assertThrows(IllegalArgumentException.class, () -> ui.update(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> ui.update(Float.NaN));
    }

    @Test
    void invalidPointerThrows() {
        assertThrows(IllegalArgumentException.class, () -> ui.pointerDown(0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ui.pointerDragged(0, 0, UiRoot.MAX_POINTERS));
        assertThrows(IllegalArgumentException.class, () -> ui.pointerUp(0, 0, 0, -1));
    }

    @Test
    void drawCopiesOnlyWhenARedrawMarkIsSet() {
        Box box = box(ui.getRoot(), 1, 1, 4, 4);

        ui.draw();
        assertEquals(BLUE, surface.getSquareGrid().getBackground(1, 1));
        assertFalse(ui.getRoot().isRedrawNeeded());

        surface.fillBackground(0, 0, 60, 30, RED);
        ui.draw();
        assertEquals(RED, surface.getSquareGrid().getBackground(1, 1));

        box.requestRedraw();
        ui.draw();
        assertEquals(BLUE, surface.getSquareGrid().getBackground(1, 1));
        assertEquals(0, surface.getSquareGrid().getBackground(0, 0));
        assertFalse(ui.getRoot().isRedrawNeeded());
    }

    @Test
    void drawPaintsPopupLayerOverRootAndClearsOldContent() {
        Box below = box(ui.getRoot(), 0, 0, 6, 6);
        Box above = new Box(2, 2, 2, 2, RED);
        ui.getPopupLayer().addChild(above);

        ui.draw();
        assertEquals(RED, surface.getSquareGrid().getBackground(2, 2));
        assertEquals(BLUE, surface.getSquareGrid().getBackground(0, 0));

        ui.getRoot().removeChild(below);
        ui.draw();
        assertEquals(0, surface.getSquareGrid().getBackground(0, 0));
    }

    @Test
    void popupLayerRedrawMarkAloneTriggersDraw() {
        Box popupBox = box(ui.getPopupLayer(), 0, 0, 2, 2);
        ui.draw();
        surface.fillBackground(0, 0, 60, 30, RED);

        popupBox.requestRedraw();
        ui.draw();

        assertEquals(BLUE, surface.getSquareGrid().getBackground(0, 0));
    }

    @Test
    void sameEventObjectIsReusedForAllEvents() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        List<UiEvent> seen = new ArrayList<>();
        box.addListener(event -> {
            seen.add(event);
            return true;
        });
        ui.setKeyboardFocus(box);

        ui.pointerDown(1, 1, 0, 0);
        ui.pointerUp(1, 1, 0, 0);
        ui.keyDown(66);

        assertTrue(seen.size() >= 3);
        for (int i = 1; i < seen.size(); i++) {
            assertSame(seen.get(0), seen.get(i));
        }
    }

    @Test
    void pressOnCapturedPointerIsIgnoredAndOtherButtonUpDoesNotEndCapture() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        track(box, "box", true);
        ui.pointerDown(1, 1, 0, 0);
        log.clear();

        assertFalse(ui.pointerDown(1, 1, 0, 1));
        assertFalse(ui.pointerUp(1, 1, 0, 1));
        assertTrue(log.isEmpty());
        assertTrue(ui.pointerUp(1, 1, 0, 0));

        assertEquals(List.of("POINTER_UP box"), log);
    }

    @Test
    void bubblingContinuesWhenAListenerRemovesItsOwnWidget() {
        WidgetGroup parent = new WidgetGroup();
        ui.getRoot().addChild(parent);
        Box child = box(parent, 0, 0, 5, 5);
        child.addListener(event -> {
            if (event.getType() == UiEventType.POINTER_DOWN) {
                parent.removeChild(child);
            }
            return false;
        });
        track(parent, "parent", true);

        assertTrue(ui.pointerDown(1, 1, 0, 0));

        assertEquals("POINTER_DOWN parent", log.get(log.size() - 1));
    }

    @Test
    void inputMethodsThrowWhileAnEventIsDelivered() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        int[] checks = new int[1];
        box.addListener(event -> {
            assertThrows(IllegalStateException.class, () -> ui.pointerMoved(1, 1));
            assertThrows(IllegalStateException.class, () -> ui.pointerDown(1, 1, 0, 0));
            assertThrows(IllegalStateException.class, () -> ui.pointerDragged(1, 1, 0));
            assertThrows(IllegalStateException.class, () -> ui.pointerUp(1, 1, 0, 0));
            assertThrows(IllegalStateException.class, () -> ui.pointerCancelled(0));
            assertThrows(IllegalStateException.class, () -> ui.keyDown(1));
            assertThrows(IllegalStateException.class, () -> ui.keyUp(1));
            assertThrows(IllegalStateException.class, () -> ui.keyTyped('a'));
            ui.setKeyboardFocus(box);
            checks[0]++;
            return false;
        });

        ui.pointerMoved(1, 1);
        ui.pointerMoved(30, 20);

        // entered, moved, then exited
        assertEquals(3, checks[0]);
    }

    @Test
    void removedCapturedWidgetGetsCancelledOnTheNextEventForItsPointer() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        track(box, "box", true);
        ui.pointerDown(1, 1, 0, 0);
        ui.getRoot().removeChild(box);
        log.clear();

        assertFalse(ui.pointerDragged(1, 1, 0));
        assertEquals(1, cancelCount());
        assertFalse(ui.pointerUp(1, 1, 0, 0));
        assertEquals(1, cancelCount());
        assertFalse(log.contains("POINTER_UP box"));
    }

    private long cancelCount() {
        return log.stream().filter(entry -> entry.equals("POINTER_CANCELLED box")).count();
    }

    @Test
    void removedCapturedWidgetIsAlsoFoundByUpCancelAndPress() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        track(box, "box", true);
        ui.pointerDown(1, 1, 0, 0);
        ui.getRoot().removeChild(box);
        assertFalse(ui.pointerUp(1, 1, 0, 0));
        assertEquals(1, cancelCount());

        ui.getRoot().addChild(box);
        ui.pointerDown(1, 1, 1, 0);
        ui.getRoot().removeChild(box);
        assertFalse(ui.pointerCancelled(1));
        assertEquals(2, cancelCount());

        ui.getRoot().addChild(box);
        ui.pointerDown(1, 1, 2, 0);
        ui.getRoot().removeChild(box);
        assertFalse(ui.pointerDown(1, 1, 2, 0));
        assertEquals(3, cancelCount());
    }

    @Test
    void hoveredWidgetIsNullWhenRemovedFromTheTrees() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        ui.pointerMoved(1, 1);
        assertSame(box, ui.getHoveredWidget());

        ui.getRoot().removeChild(box);

        assertNull(ui.getHoveredWidget());
    }

    @Test
    void movedEventReportsPointerZero() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        int[] pointer = {-5};
        box.addListener(event -> {
            if (event.getType() == UiEventType.POINTER_MOVED) {
                pointer[0] = event.getPointer();
            }
            return true;
        });

        ui.pointerMoved(1, 1);

        assertEquals(0, pointer[0]);
    }

    @Test
    void listenerAddedDuringDispatchRunsOnlyFromTheNextEvent() {
        Box box = box(ui.getRoot(), 0, 0, 5, 5);
        box.addListener(event -> {
            if (event.getType() != UiEventType.POINTER_DOWN) {
                return false;
            }
            box.addListener(later -> {
                log.add("late " + later.getType());
                return false;
            });
            return false;
        });

        ui.pointerDown(1, 1, 0, 0);
        assertTrue(log.stream().noneMatch(entry -> entry.equals("late POINTER_DOWN")));
        ui.pointerUp(1, 1, 0, 0);

        assertTrue(log.contains("late POINTER_UP"));
    }
}
