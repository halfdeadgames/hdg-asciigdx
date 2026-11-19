package com.github.halfdeadgames.asciigdx.ui;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.Arrays;
import java.util.Objects;

/**
 * The top of a widget tree on one {@link UiSurface}: it draws the tree, routes input to widgets,
 * and tracks hover, press capture, and keyboard focus. It works purely in surface cells and never
 * touches libGDX statics, so it can run headless; {@link UiInput} connects it to libGDX input.
 *
 * <p>Two widget trees exist. {@link #getRoot()} holds the main widgets. {@link #getPopupLayer()}
 * is drawn above it and its children are hit-tested first, for menus and tooltips. Both trees
 * start at the surface's top-left corner.
 *
 * <p>Drawing: the root draws into a hidden surface of the same size, then copies it into the shown
 * surface. Equal cells stay clean for the grids' dirty tracking, so only changed cells are
 * uploaded to the GPU. Nothing is drawn on frames where no widget changed.
 *
 * <p>Input routing:
 * <ul>
 *   <li>A pointer event goes to the deepest widget under the pointer, then bubbles up through its
 *       parents until a {@link UiListener} returns {@code true}.
 *   <li>Press capture: after a pointer goes down on a widget, that pointer's drag, up, and cancel
 *       events go to that widget even outside it. Capture is per pointer. It ends when the
 *       pointer comes up with the same button that started it, or is canceled. A press on a
 *       pointer that is already captured, and an up with another button, are ignored. If the
 *       captured widget leaves both trees, the next press, drag, up, or cancel for that pointer
 *       sends it {@link UiEventType#POINTER_CANCELLED} (to that widget only, without bubbling) and
 *       ends the capture; the incoming event itself is then treated as if nothing was captured.
 *   <li>Re-entrancy: while an event is being delivered to listeners, including
 *       {@link UiEventType#POINTER_ENTERED} and {@link UiEventType#POINTER_EXITED}, calling
 *       any pointer or key input method of the same root throws
 *       {@link IllegalStateException}. {@link #setKeyboardFocus} stays allowed. Defer such work
 *       to the next frame with a {@link UiUpdateListener}.
 *   <li>Hover: pointer 0 (the mouse or the first finger) drives hover. When the deepest widget
 *       under it changes from A to B, {@link UiEventType#POINTER_EXITED} goes to A and each
 *       ancestor of A that is not B or an ancestor of B, deepest first, then
 *       {@link UiEventType#POINTER_ENTERED} goes to each ancestor of B that is not A or an
 *       ancestor of A, outermost first, and then to B. These do not bubble. Hover is not cleared
 *       by up events, so a touch would leave the last widget hovered until the pointer is seen
 *       elsewhere. For touch-only devices, {@link UiInput} ends the hover itself: until it has
 *       seen a mouse move, it calls {@code pointerMoved(-1, -1)} after each up or cancel of
 *       pointer 0. Code that forwards input without UiInput can do the same.
 *   <li>Keyboard: a pointer press gives focus to the deepest widget in the target's parent chain
 *       (the target included) whose {@link Widget#isFocusable()} is {@code true}, or clears
 *       focus. Key events go to the focused widget and bubble; with no focus they are not
 *       handled.
 * </ul>
 * Every input method returns whether the input was handled. Pointer positions outside the surface
 * hit nothing.
 *
 * <p>A {@link UiSurface} never changes size. For another size, create a new surface and a new
 * root.
 *
 * <p>Not thread-safe; use it from the render thread. Input and drawing do not allocate. Listener
 * lists are copy-on-write, so adding or removing listeners from inside a listener is safe; the
 * change applies to the next dispatch or update.
 *
 * <p>Example:
 * <pre>{@code
 * UiRoot ui = UiRoot.of(surface);
 * TextButton start = new TextButton("Start", style);
 * start.setPosition(4, 4);
 * start.addClickListener(button -> startGame());
 * ui.getRoot().addChild(start);
 * // each frame, before rendering the layers:
 * ui.update(deltaSeconds);
 * ui.draw();
 * }</pre>
 */
public final class UiRoot {

    /**
     * Number of pointer indexes the root supports; valid pointers are 0 to this minus 1. It
     * matches the pointer cap of libGDX on Android. The pointer methods of the root throw
     * {@link IllegalArgumentException} for other values, while {@link UiInput} ignores them.
     */
    public static final int MAX_POINTERS = 20;

    private static final String REENTRANT_MESSAGE =
            "Input methods must not be called while an event is being delivered; defer the work "
                    + "to the next frame, for example with a UiUpdateListener";
    private static final UiUpdateListener[] NO_UPDATE_LISTENERS = new UiUpdateListener[0];
    private static final int PRIMARY_POINTER = 0;
    private static final int NO_BUTTON = UiEvent.NOT_APPLICABLE;

    private final UiSurface shownSurface;
    private final UiSurface hiddenSurface;
    private final WidgetGroup root = new WidgetGroup();
    private final WidgetGroup popupLayer = new WidgetGroup();
    private final UiEvent event = new UiEvent();
    private final Widget[] capturedWidgets = new Widget[MAX_POINTERS];
    private final int[] capturingButtons = new int[MAX_POINTERS];
    private UiUpdateListener[] updateListeners = NO_UPDATE_LISTENERS;
    private boolean delivering;
    private Widget hoveredWidget;
    private Widget keyboardFocus;

    private UiRoot(UiSurface shownSurface) {
        this.shownSurface = shownSurface;
        this.hiddenSurface =
                UiSurface.ofSize(shownSurface.getColumns(), shownSurface.getRows());
    }

    /**
     * Creates a root that shows its widgets on a surface. It creates a hidden surface of the same
     * size to draw into.
     *
     * <p>A surface never changes size, so for a new size create a new surface and a new root.
     *
     * @param shownSurface surface that the game puts in its layers; must not be {@code null}
     * @return a new root with empty widget trees
     * @throws NullPointerException if {@code shownSurface} is {@code null}
     */
    public static UiRoot of(UiSurface shownSurface) {
        return new UiRoot(Objects.requireNonNull(shownSurface, "shownSurface"));
    }

    /**
     * Returns the main widget tree. Add the game's widgets here. The group starts at the surface's
     * top-left corner with size 0 x 0, so by default only its children are hit, not the group.
     *
     * @return the root group, never {@code null}
     */
    public WidgetGroup getRoot() {
        return root;
    }

    /**
     * Returns the group drawn above {@link #getRoot()}. Its children are hit-tested before the
     * main tree's, and the group itself is never a hit target. Use it for popups.
     *
     * @return the popup layer group, never {@code null}
     */
    public WidgetGroup getPopupLayer() {
        return popupLayer;
    }

    /**
     * Adds a listener that is called by every {@link #update(float)}. Listeners run in the order
     * they were added; adding the same listener twice makes it run twice. Safe to call from
     * inside a listener.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addUpdateListener(UiUpdateListener listener) {
        Objects.requireNonNull(listener, "listener");
        UiUpdateListener[] grown = Arrays.copyOf(updateListeners, updateListeners.length + 1);
        grown[updateListeners.length] = listener;
        updateListeners = grown;
    }

    /**
     * Removes the first occurrence of an update listener. Does nothing if it was not added. Safe
     * to call from inside a listener; a running update still calls the listeners that existed when
     * it started.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeUpdateListener(UiUpdateListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(updateListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        UiUpdateListener[] shrunk = new UiUpdateListener[updateListeners.length - 1];
        System.arraycopy(updateListeners, 0, shrunk, 0, index);
        System.arraycopy(updateListeners, index + 1, shrunk, index, shrunk.length - index);
        updateListeners = shrunk.length == 0 ? NO_UPDATE_LISTENERS : shrunk;
    }

    /**
     * Calls every update listener with the frame time. Call it once per frame, before
     * {@link #draw()}.
     *
     * @param deltaSeconds time since the previous frame, in seconds, passed in by the game; zero
     *         or greater and finite
     * @throws IllegalArgumentException if {@code deltaSeconds} is negative, NaN, or infinite
     */
    public void update(float deltaSeconds) {
        if (!(deltaSeconds >= 0f) || Float.isInfinite(deltaSeconds)) {
            throw new IllegalArgumentException(
                    "deltaSeconds must be finite and not negative, was " + deltaSeconds);
        }
        UiUpdateListener[] snapshot = updateListeners;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].update(deltaSeconds);
        }
    }

    /**
     * Lays out both trees and, if any widget changed, redraws everything. A redraw clears the
     * hidden surface (no glyphs, fully transparent backgrounds), draws the main tree and then the
     * popup layer into it, copies it into the shown surface, and clears the redraw marks. When no
     * widget changed, nothing is drawn. Call it once per frame after {@link #update(float)} and
     * before rendering the layers, on the render thread. Does not allocate.
     */
    public void draw() {
        root.validate();
        popupLayer.validate();
        if (!root.isRedrawNeeded() && !popupLayer.isRedrawNeeded()) {
            return;
        }
        int columns = hiddenSurface.getColumns();
        int rows = hiddenSurface.getRows();
        hiddenSurface.eraseGlyphs(0, 0, columns, rows);
        hiddenSurface.fillBackground(0, 0, columns, rows,
                TerminalGrid.TRANSPARENT_RGBA);
        root.draw(hiddenSurface);
        popupLayer.draw(hiddenSurface);
        shownSurface.copyFrom(hiddenSurface);
        root.clearRedrawNeeded();
        popupLayer.clearRedrawNeeded();
    }

    /**
     * Returns the deepest widget under pointer 0 at its last known position.
     *
     * @return the hovered widget, or {@code null} when the pointer is over nothing or the widget
     *         is no longer in the main tree or the popup layer
     */
    public Widget getHoveredWidget() {
        return isInTrees(hoveredWidget) ? hoveredWidget : null;
    }

    /**
     * Gives keyboard focus to a widget, or clears it. The widget does not need to be focusable;
     * the focusable check applies only to presses. No events are sent for a focus change. Unlike
     * the input methods, this is allowed while an event is being delivered.
     *
     * @param widgetOrNull the new focus owner, or {@code null} for no focus
     */
    public void setKeyboardFocus(Widget widgetOrNull) {
        keyboardFocus = widgetOrNull;
    }

    /**
     * Returns the widget that receives key events.
     *
     * @return the focused widget, or {@code null} for no focus, including when the focused widget
     *         is no longer in the main tree or the popup layer
     */
    public Widget getKeyboardFocus() {
        return isInTrees(keyboardFocus) ? keyboardFocus : null;
    }

    /**
     * Handles the pointer moving with no button down. Updates hover, then sends
     * {@link UiEventType#POINTER_MOVED} to the widget under the pointer, as pointer 0.
     *
     * <p>libGDX sends mouse moves on desktop only. On touch-only devices hover changes only with
     * the down, drag, and up events of pointer 0.
     *
     * @param column pointer column on the surface, in UI cells; any value, outside hits nothing
     * @param row pointer row on the surface, in UI cells; any value
     * @return {@code true} if a listener handled the move; {@code false} also when the pointer is
     *         over nothing
     * @throws IllegalStateException if called while an event is being delivered
     */
    public boolean pointerMoved(int column, int row) {
        requireNotDelivering();
        Widget target = hitAt(column, row);
        updateHover(target, column, row);
        if (target == null) {
            return false;
        }
        event.setPointer(UiEventType.POINTER_MOVED, target, column, row, PRIMARY_POINTER,
                NO_BUTTON);
        return bubble(target);
    }

    /**
     * Handles a pointer going down. Updates hover for pointer 0, moves keyboard focus (see the
     * class description), starts press capture on the widget under the pointer, and sends
     * {@link UiEventType#POINTER_DOWN} to it.
     *
     * <p>Capture lifetime: it lasts until this pointer comes up with the same button, or is
     * canceled, or the captured widget leaves both trees (see the class description; a press
     * also finds that out and then starts a fresh capture). While a
     * pointer is captured, another press on it is ignored: nothing changes, no event is sent, and
     * the result is {@code false}.
     *
     * <p>A press on empty space, or one that no listener handles, returns {@code false}. Games can
     * use that for outside-click handling, such as closing a popup.
     *
     * @param column pointer column on the surface, in UI cells; any value, outside hits nothing
     * @param row pointer row on the surface, in UI cells; any value
     * @param pointer pointer index, 0 to {@link #MAX_POINTERS} - 1
     * @param button button index, 0 for the primary button; zero or greater
     * @return {@code true} if a listener handled the press
     * @throws IllegalArgumentException if {@code pointer} or {@code button} is out of range
     * @throws IllegalStateException if called while an event is being delivered
     */
    public boolean pointerDown(int column, int row, int pointer, int button) {
        requireNotDelivering();
        requirePointer(pointer);
        requireButton(button);
        cancelCaptureIfWidgetRemoved(pointer);
        if (capturedWidgets[pointer] != null) {
            return false;
        }
        Widget target = hitAt(column, row);
        updateHoverForPointer(pointer, target, column, row);
        keyboardFocus = findFocusable(target);
        if (target == null) {
            return false;
        }
        capturedWidgets[pointer] = target;
        capturingButtons[pointer] = button;
        event.setPointer(UiEventType.POINTER_DOWN, target, column, row, pointer, button);
        return bubble(target);
    }

    /**
     * Handles a pressed pointer moving. The event goes to the widget that captured the pointer,
     * even when the pointer is outside it. Hover still follows the widget under pointer 0.
     *
     * @param column pointer column on the surface, in UI cells; any value
     * @param row pointer row on the surface, in UI cells; any value
     * @param pointer pointer index, 0 to {@link #MAX_POINTERS} - 1
     * @return {@code true} if a listener handled the drag; {@code false} when the pointer has no
     *         captured widget, including when the captured widget was just found removed
     * @throws IllegalArgumentException if {@code pointer} is out of range
     * @throws IllegalStateException if called while an event is being delivered
     */
    public boolean pointerDragged(int column, int row, int pointer) {
        requireNotDelivering();
        requirePointer(pointer);
        cancelCaptureIfWidgetRemoved(pointer);
        updateHoverForPointer(pointer, hitAt(column, row), column, row);
        Widget captured = capturedWidgets[pointer];
        if (captured == null) {
            return false;
        }
        event.setPointer(UiEventType.POINTER_DRAGGED, captured, column, row, pointer, NO_BUTTON);
        return bubble(captured);
    }

    /**
     * Handles a pointer going up. The event goes to the widget that captured the pointer, and the
     * capture ends before the listeners run, but only when {@code button} is the one that started
     * the capture. An up with another button is ignored.
     *
     * @param column pointer column on the surface, in UI cells; any value
     * @param row pointer row on the surface, in UI cells; any value
     * @param pointer pointer index, 0 to {@link #MAX_POINTERS} - 1
     * @param button button index, 0 for the primary button; zero or greater
     * @return {@code true} if a listener handled the release; {@code false} when the pointer has
     *         no captured widget, the button differs, or the captured widget was just found
     *         removed
     * @throws IllegalArgumentException if {@code pointer} or {@code button} is out of range
     * @throws IllegalStateException if called while an event is being delivered
     */
    public boolean pointerUp(int column, int row, int pointer, int button) {
        requireNotDelivering();
        requirePointer(pointer);
        requireButton(button);
        cancelCaptureIfWidgetRemoved(pointer);
        updateHoverForPointer(pointer, hitAt(column, row), column, row);
        Widget captured = capturedWidgets[pointer];
        if (captured == null || capturingButtons[pointer] != button) {
            return false;
        }
        capturedWidgets[pointer] = null;
        event.setPointer(UiEventType.POINTER_UP, captured, column, row, pointer, button);
        return bubble(captured);
    }

    /**
     * Handles the system canceling a pointer, for example when the app loses focus. The event goes
     * to the widget that captured the pointer, and the capture ends before the listeners run.
     *
     * @param pointer pointer index, 0 to {@link #MAX_POINTERS} - 1
     * @return {@code true} if a listener handled it; {@code false} when the pointer has no
     *         captured widget, including when the captured widget was just found removed
     * @throws IllegalArgumentException if {@code pointer} is out of range
     * @throws IllegalStateException if called while an event is being delivered
     */
    public boolean pointerCancelled(int pointer) {
        requireNotDelivering();
        requirePointer(pointer);
        cancelCaptureIfWidgetRemoved(pointer);
        Widget captured = capturedWidgets[pointer];
        if (captured == null) {
            return false;
        }
        capturedWidgets[pointer] = null;
        event.setCancelled(captured, pointer);
        return bubble(captured);
    }

    /**
     * Handles a key going down. It goes to the focused widget and bubbles.
     *
     * @param keycode key code, as in {@code com.badlogic.gdx.Input.Keys}
     * @return {@code true} if a listener handled it; {@code false} also when nothing has focus
     */
    public boolean keyDown(int keycode) {
        return dispatchKey(UiEventType.KEY_DOWN, keycode, '\0');
    }

    /**
     * Handles a key going up. It goes to the focused widget and bubbles.
     *
     * @param keycode key code, as in {@code com.badlogic.gdx.Input.Keys}
     * @return {@code true} if a listener handled it; {@code false} also when nothing has focus
     */
    public boolean keyUp(int keycode) {
        return dispatchKey(UiEventType.KEY_UP, keycode, '\0');
    }

    /**
     * Handles a typed character. It goes to the focused widget and bubbles.
     *
     * @param character the typed character
     * @return {@code true} if a listener handled it; {@code false} also when nothing has focus
     */
    public boolean keyTyped(char character) {
        return dispatchKey(UiEventType.KEY_TYPED, UiEvent.NOT_APPLICABLE, character);
    }

    private boolean dispatchKey(UiEventType type, int keycode, char character) {
        requireNotDelivering();
        Widget focused = getKeyboardFocus();
        if (focused == null) {
            return false;
        }
        event.setKey(type, focused, keycode, character);
        return bubble(focused);
    }

    private void requireNotDelivering() {
        if (delivering) {
            throw new IllegalStateException(REENTRANT_MESSAGE);
        }
    }

    private static void requireButton(int button) {
        if (button < 0) {
            throw new IllegalArgumentException("button must not be negative, was " + button);
        }
    }

    private void cancelCaptureIfWidgetRemoved(int pointer) {
        Widget captured = capturedWidgets[pointer];
        if (captured == null || isInTrees(captured)) {
            return;
        }
        capturedWidgets[pointer] = null;
        event.setCancelled(captured, pointer);
        event.setCurrent(captured, 0, 0);
        deliver(captured);
    }

    private boolean deliver(Widget widget) {
        delivering = true;
        try {
            return widget.dispatch(event);
        } finally {
            delivering = false;
        }
    }

    private static void requirePointer(int pointer) {
        if (pointer < 0 || pointer >= MAX_POINTERS) {
            throw new IllegalArgumentException(
                    "pointer must be 0 to " + (MAX_POINTERS - 1) + ", was " + pointer);
        }
    }

    private Widget hitAt(int column, int row) {
        if (column < 0 || column >= hiddenSurface.getColumns() || row < 0
                || row >= hiddenSurface.getRows()) {
            return null;
        }
        Widget popupHit = popupLayer.hitChildren(column - popupLayer.getX(),
                row - popupLayer.getY());
        if (popupHit != null) {
            return popupHit;
        }
        return root.hit(column - root.getX(), row - root.getY());
    }

    private boolean isInTrees(Widget widget) {
        if (widget == null) {
            return false;
        }
        Widget top = widget;
        while (top.getParent() != null) {
            top = top.getParent();
        }
        return top == root || top == popupLayer;
    }

    private static Widget findFocusable(Widget target) {
        for (Widget current = target; current != null; current = current.getParent()) {
            if (current.isFocusable()) {
                return current;
            }
        }
        return null;
    }

    private boolean bubble(Widget target) {
        boolean hasPosition = event.hasPosition();
        Widget current = target;
        while (current != null) {
            // Read before dispatch, so a listener that removes its widget does not stop bubbling.
            Widget parent = current.getParent();
            int relativeColumn = 0;
            int relativeRow = 0;
            if (hasPosition) {
                relativeColumn = event.getSurfaceColumn() - absoluteLeft(current);
                relativeRow = event.getSurfaceRow() - absoluteTop(current);
            }
            event.setCurrent(current, relativeColumn, relativeRow);
            if (deliver(current)) {
                return true;
            }
            current = parent;
        }
        return false;
    }

    private static int absoluteLeft(Widget widget) {
        int left = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            left += current.getX();
        }
        return left;
    }

    private static int absoluteTop(Widget widget) {
        int top = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            top += current.getY();
        }
        return top;
    }

    private void updateHoverForPointer(int pointer, Widget newHover, int column, int row) {
        if (pointer == PRIMARY_POINTER) {
            updateHover(newHover, column, row);
        }
    }

    private void updateHover(Widget newHover, int column, int row) {
        Widget oldHover = hoveredWidget;
        if (newHover == oldHover) {
            return;
        }
        hoveredWidget = newHover;
        if (oldHover != null) {
            event.setPointer(UiEventType.POINTER_EXITED, oldHover, column, row, PRIMARY_POINTER,
                    NO_BUTTON);
            fireExited(oldHover, newHover);
        }
        if (newHover != null) {
            event.setPointer(UiEventType.POINTER_ENTERED, newHover, column, row, PRIMARY_POINTER,
                    NO_BUTTON);
            fireEntered(newHover, oldHover);
        }
    }

    private void fireExited(Widget widget, Widget newHover) {
        for (Widget current = widget; current != null && !isSelfOrAncestor(current, newHover);
                current = current.getParent()) {
            deliverWithoutBubbling(current);
        }
    }

    private void fireEntered(Widget widget, Widget oldHover) {
        if (widget == null || isSelfOrAncestor(widget, oldHover)) {
            return;
        }
        fireEntered(widget.getParent(), oldHover);
        deliverWithoutBubbling(widget);
    }

    private void deliverWithoutBubbling(Widget widget) {
        event.setCurrent(widget, event.getSurfaceColumn() - absoluteLeft(widget),
                event.getSurfaceRow() - absoluteTop(widget));
        deliver(widget);
    }

    private static boolean isSelfOrAncestor(Widget candidate, Widget widget) {
        for (Widget current = widget; current != null; current = current.getParent()) {
            if (current == candidate) {
                return true;
            }
        }
        return false;
    }
}
