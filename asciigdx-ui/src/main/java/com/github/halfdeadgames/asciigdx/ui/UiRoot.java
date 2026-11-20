package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
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
 *   <li>Keyboard navigation: a key-down that no widget in the focused chain and no unhandled-key
 *       listener handled moves focus. Tab moves to the next focusable widget in tree order,
 *       Shift+Tab to the previous one, and the arrow keys to the nearest one in that direction
 *       (see {@link FocusDirection}). Only the main tree is searched, and nothing moves while a
 *       menu is open. Hidden widgets (see {@link Widget#setVisible(boolean)}) and menu bars are
 *       skipped. A widget that handles an arrow key itself keeps it. Games can call
 *       {@link #moveFocus(FocusDirection)} for the same effect, for example from a gamepad.
 *   <li>Key repeat: libGDX does not repeat key-downs on desktop, so the root repeats a held
 *       arrow key (Left, Right, Up, or Down) itself, timed by {@link #update(float)} in seconds.
 *       After a key-down of an arrow it waits 0.4 seconds, then repeats every 0.05 seconds. Each
 *       repeat takes the
 *       same path as a real key-down (the focused chain, then the unhandled-key listeners, then
 *       focus navigation), with the Shift state of the original press, so a held arrow also moves
 *       a text field's caret, focus, or the highlight of an open menu. Enter, Space, letters, and
 *       all other keys never repeat. The repeat stops on the key-up of that key, on any other
 *       key-down, and when the focus owner changes for any reason other than the repeat itself
 *       (a pointer press, {@link #setKeyboardFocus}, or an owner that was hidden or disabled); a
 *       focus move that the repeated key causes keeps it going. If the same arrow key-down
 *       arrives a second time before its key-up, as when a platform repeats on its own, the
 *       root's repeat stays off until that key is released.
 *   <li>Focus look: focus is shown (see {@link #isFocusVisible()}) while the last input was
 *       keyboard navigation or a key that the focused chain handled, and hidden after an
 *       accepted pointer press and before any input. Hiding a widget acts like removing it for
 *       focus, hover, and press capture: focus is cleared and does not return when the widget is
 *       shown or added again, a capture is canceled, and hover moves to what is under the
 *       pointer. The root notices this when it reads focus, and in {@link #update(float)} and
 *       {@link #draw()}.
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
    private static final PressObserver[] NO_PRESS_OBSERVERS = new PressObserver[0];
    private static final UiListener[] NO_KEY_LISTENERS = new UiListener[0];
    static final float KEY_REPEAT_INITIAL_DELAY_SECONDS = 0.4f;
    static final float KEY_REPEAT_INTERVAL_SECONDS = 0.05f;
    private static final int NO_KEY = -1;
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
    private PressObserver[] pressObservers = NO_PRESS_OBSERVERS;
    private UiListener[] unhandledKeyListeners = NO_KEY_LISTENERS;
    private UiListener[] keyObservers = NO_KEY_LISTENERS;
    private MenuBar openMenuBar;
    private Widget focusBeforePress;
    private boolean delivering;
    private Widget hoveredWidget;
    private Widget keyboardFocus;
    private Widget focusShownWidget;
    private boolean lastInputWasKeyboard;
    private Widget sweptHover;
    private int hoverColumn;
    private int hoverRow;
    private boolean leftShiftDown;
    private boolean rightShiftDown;
    private final TextFieldAutoFocus autoFocusFinder = new TextFieldAutoFocus();
    private boolean keyboardFocusWasFocusable;
    private int repeatingKeycode = NO_KEY;
    private int heldArrowKeycode = NO_KEY;
    private boolean repeatingWithShift;
    private boolean firingKeyRepeat;
    private float secondsUntilKeyRepeat;
    private boolean autoFocusTextFields;
    private boolean textInputWanted;
    private TextInputWantedListener textInputWantedListener;

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
     * Advances time by one frame: repeats a held arrow key (see the class description), applies
     * automatic text field focus (see {@link #setAutoFocusTextFields(boolean)}), calls every update
     * listener with the frame time, and lets the focused widget animate, such as a blinking caret.
     * Call it once per frame, before {@link #draw()}. Key repeats are key input, so this throws
     * {@link IllegalStateException} like the input methods when called while an event is being
     * delivered and a repeat is due.
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
        sweepGoneWidgets();
        repeatHeldKey(deltaSeconds);
        focusFirstTextFieldIfWanted();
        UiUpdateListener[] snapshot = updateListeners;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].update(deltaSeconds);
        }
        Widget focused = getKeyboardFocus();
        if (focused != null) {
            focused.updateWhileFocused(deltaSeconds);
        }
    }

    /**
     * Turns on or off the automatic focus for text fields: when on, the first text field gets
     * focus whenever nothing has focus, including right after a click on empty space. Each
     * {@link #update(float)} that finds nothing with keyboard focus gives focus to the first
     * shown, enabled {@link TextField} of the main tree ({@link #getRoot()}) in reading order:
     * rows from top to bottom, and within a row columns from left to right, comparing each
     * field's top row and then its left column on the surface. The popup layer is not searched.
     * This does not make the focused look show (see {@link #isFocusVisible()}). The search walks
     * the main tree once per update while nothing has focus, without allocating. Off by default.
     *
     * @param autoFocusTextFields {@code true} to focus the first text field automatically
     */
    public void setAutoFocusTextFields(boolean autoFocusTextFields) {
        this.autoFocusTextFields = autoFocusTextFields;
    }

    /**
     * Returns whether text fields are focused automatically, as set by
     * {@link #setAutoFocusTextFields(boolean)}.
     *
     * @return {@code true} when automatic focus is on
     */
    public boolean isAutoFocusTextFields() {
        return autoFocusTextFields;
    }

    private void focusFirstTextFieldIfWanted() {
        if (!autoFocusTextFields || delivering || root.getChildCount() == 0
                || getKeyboardFocus() != null) {
            return;
        }
        TextField first = autoFocusFinder.findFirst(root);
        if (first != null) {
            assignKeyboardFocus(first);
            refreshFocusShown();
        }
    }

    // Single place where the focus owner changes, so the owners and the on-screen keyboard follow.
    private void assignKeyboardFocus(Widget next) {
        Widget previous = keyboardFocus;
        if (previous == next) {
            return;
        }
        keyboardFocus = next;
        keyboardFocusWasFocusable = next != null && next.isFocusable();
        if (!firingKeyRepeat) {
            stopKeyRepeat();
        }
        if (previous != null) {
            previous.setKeyboardFocused(false);
        }
        if (next != null) {
            next.setKeyboardFocused(true);
        }
        updateTextInputWanted();
    }

    // A press on a text field that already has focus asks again, because the user may have hidden
    // the on-screen keyboard with the Back key.
    private void requestTextInputAgain() {
        if (textInputWanted && textInputWantedListener != null) {
            textInputWantedListener.textInputWantedChanged(true);
        }
    }

    private void updateTextInputWanted() {
        boolean wanted = keyboardFocus instanceof TextField;
        if (wanted == textInputWanted) {
            return;
        }
        textInputWanted = wanted;
        if (textInputWantedListener != null) {
            textInputWantedListener.textInputWantedChanged(wanted);
        }
    }

    /** Hears when a text field gains or loses keyboard focus, for the on-screen keyboard. */
    interface TextInputWantedListener {
        void textInputWantedChanged(boolean wanted);
    }

    // Only UiInput uses this, to keep the libGDX calls in one class.
    void setTextInputWantedListener(TextInputWantedListener listener) {
        textInputWantedListener = listener;
    }

    /**
     * Lays out both trees and, if any widget changed, redraws everything. A redraw clears the
     * hidden surface (no glyphs, fully transparent backgrounds), draws the main tree and then the
     * popup layer into it, copies it into the shown surface, and clears the redraw marks. When no
     * widget changed, nothing is drawn. Call it once per frame after {@link #update(float)} and
     * before rendering the layers, on the render thread. Does not allocate.
     */
    public void draw() {
        sweepGoneWidgets();
        refreshFocusShown();
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
     *         is hidden or no longer in the main tree or the popup layer
     */
    public Widget getHoveredWidget() {
        return isInTrees(hoveredWidget) ? hoveredWidget : null;
    }

    /**
     * Gives keyboard focus to a widget, or clears it. The widget does not need to be focusable;
     * the focusable check applies only to presses, and menu bars are accepted. A widget that is
     * hidden or not in the main tree or popup layer is accepted too, but {@link #getKeyboardFocus}
     * then treats it as no focus and clears it, so it does not get focus back by being shown or
     * added later. A widget that is focusable when it receives focus (see
     * {@link Widget#isFocusable()}) keeps focus only while it stays focusable: if it becomes
     * disabled, for example, {@link #getKeyboardFocus} clears the focus, and it does not return
     * when the widget is enabled again. A widget that was not focusable when it received focus is
     * not affected by this. No events are sent for a focus change. Unlike the input methods, this
     * is allowed while an event is being delivered. Any change of the focus owner stops a key
     * repeat in progress, except one made by the repeat itself (see the class description).
     *
     * <p>Setting focus does not change whether the focused look shows; see
     * {@link #isFocusVisible()}.
     *
     * @param widgetOrNull the new focus owner, or {@code null} for no focus
     */
    public void setKeyboardFocus(Widget widgetOrNull) {
        assignKeyboardFocus(widgetOrNull);
        refreshFocusShown();
    }

    /**
     * Returns the widget that receives key events. If the stored focus owner is hidden (see
     * {@link Widget#setVisible(boolean)}), no longer in the main tree or the popup layer, or was
     * focusable when it got focus and no longer is (for example, it was disabled), this clears the
     * focus first and returns {@code null}; the widget does not get focus back when it is shown,
     * added, or enabled again.
     *
     * @return the focused widget, or {@code null} for no focus
     */
    public Widget getKeyboardFocus() {
        clearFocusIfOwnerGone();
        return keyboardFocus;
    }

    private void clearFocusIfOwnerGone() {
        if (keyboardFocus == null) {
            return;
        }
        boolean lostFocusability = keyboardFocusWasFocusable && !keyboardFocus.isFocusable();
        if (lostFocusability || !isInTrees(keyboardFocus)) {
            assignKeyboardFocus(null);
        }
    }

    // Widgets cannot tell the root that they were hidden or removed, so the root notices here.
    // Captures are canceled as for a removed widget, and hover moves to what is under the pointer.
    private void sweepGoneWidgets() {
        if (delivering) {
            return;
        }
        clearFocusIfOwnerGone();
        for (int pointer = 0; pointer < MAX_POINTERS; pointer++) {
            cancelCaptureIfWidgetRemoved(pointer);
        }
        if (hoveredWidget != null && !isInTrees(hoveredWidget)) {
            Widget gone = hoveredWidget;
            updateHover(hitAt(hoverColumn, hoverRow), hoverColumn, hoverRow);
            sweptHover = gone;
        } else if (sweptHover != null && isInTrees(sweptHover)) {
            // A widget that returns under a pointer that never moved is hovered again.
            Widget back = sweptHover;
            if (hitAt(hoverColumn, hoverRow) == back) {
                updateHover(back, hoverColumn, hoverRow);
            }
            sweptHover = null;
        }
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
        notifyPressObservers(pointer);
        focusBeforePress = getKeyboardFocus();
        assignKeyboardFocus(findFocusable(target));
        if (keyboardFocus == focusBeforePress && keyboardFocus instanceof TextField) {
            requestTextInputAgain();
        }
        lastInputWasKeyboard = false;
        refreshFocusShown();
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
     * Handles a key going down. It goes to the focused widget and bubbles. A real key-down of an
     * arrow key starts the key repeat (see the class description).
     *
     * @param keycode key code, as in {@code com.badlogic.gdx.Input.Keys}
     * @return {@code true} if a listener handled it; {@code false} also when nothing has focus
     */
    public boolean keyDown(int keycode) {
        return keyDown(keycode, leftShiftDown || rightShiftDown);
    }

    // For UiInput, which knows the real Shift state; keyDown(int) uses the tracked state.
    boolean keyDown(int keycode, boolean shiftHeld) {
        boolean handled = dispatchKey(UiEventType.KEY_DOWN, keycode, '\0', shiftHeld);
        startOrStopKeyRepeat(keycode, shiftHeld);
        return handled;
    }

    // The repeat starts after the dispatch, so a focus change that the first press causes (Tab or
    // an arrow moving focus) does not cancel it.
    private void startOrStopKeyRepeat(int keycode, boolean shiftHeld) {
        stopKeyRepeat();
        if (!isArrowKey(keycode)) {
            return;
        }
        if (heldArrowKeycode == keycode) {
            // The platform repeats this held key by itself, so the timer stays off for the hold.
            return;
        }
        heldArrowKeycode = keycode;
        repeatingKeycode = keycode;
        repeatingWithShift = shiftHeld;
        secondsUntilKeyRepeat = KEY_REPEAT_INITIAL_DELAY_SECONDS;
    }

    private void stopKeyRepeat() {
        repeatingKeycode = NO_KEY;
    }

    private static boolean isArrowKey(int keycode) {
        return keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT
                || keycode == Input.Keys.UP || keycode == Input.Keys.DOWN;
    }

    private void repeatHeldKey(float deltaSeconds) {
        if (repeatingKeycode == NO_KEY) {
            return;
        }
        secondsUntilKeyRepeat -= deltaSeconds;
        while (secondsUntilKeyRepeat <= 0f && repeatingKeycode != NO_KEY) {
            secondsUntilKeyRepeat += KEY_REPEAT_INTERVAL_SECONDS;
            firingKeyRepeat = true;
            try {
                dispatchKey(UiEventType.KEY_DOWN, repeatingKeycode, '\0', repeatingWithShift);
            } finally {
                firingKeyRepeat = false;
            }
        }
    }

    /**
     * Handles a key going up. It goes to the focused widget and bubbles. Releasing the key that
     * is held down ends its key repeat.
     *
     * @param keycode key code, as in {@code com.badlogic.gdx.Input.Keys}
     * @return {@code true} if a listener handled it; {@code false} also when nothing has focus
     */
    public boolean keyUp(int keycode) {
        if (keycode == heldArrowKeycode) {
            heldArrowKeycode = NO_KEY;
            stopKeyRepeat();
        }
        return dispatchKey(UiEventType.KEY_UP, keycode, '\0', false);
    }

    /**
     * Handles a typed character. It goes to the focused widget and bubbles.
     *
     * @param character the typed character
     * @return {@code true} if a listener handled it; {@code false} also when nothing has focus
     */
    public boolean keyTyped(char character) {
        return dispatchKey(UiEventType.KEY_TYPED, UiEvent.NOT_APPLICABLE, character, false);
    }

    /**
     * Moves keyboard focus the way the navigation keys do, for example to map a gamepad. The
     * rules are those of the keys: only the main tree ({@link #getRoot()}) is searched, the
     * candidates are its visible, focusable widgets (menu bars are skipped), and nothing happens
     * while a menu is open. A move that finds a target counts as keyboard input, so focus becomes
     * visible (see {@link #isFocusVisible()}).
     *
     * <p>{@link FocusDirection#NEXT} and {@link FocusDirection#PREVIOUS} wrap around in tree order.
     * They pick the first or last candidate when nothing has focus or the focus owner is outside
     * the main tree, and continue from the owner's tree position when it is in the main tree but
     * not a candidate. The four spatial directions pick the nearest candidate on the surface in
     * that direction and never wrap. They pick the first candidate when nothing has focus, and do
     * nothing when the focus owner is outside the main tree. No event is sent for a focus change.
     *
     * <p>Like an input handler, this returns a result while it changes state, so a game can tell
     * whether the input was used. To find out whether the focus owner changed, compare
     * {@link #getKeyboardFocus()} before and after the call.
     *
     * @param direction where to move focus; must not be {@code null}
     * @return whether a target was found: {@code true} if a target now has focus, which includes a
     *         target that already had focus; {@code false} when a menu is open or when no widget
     *         qualifies
     * @throws NullPointerException if {@code direction} is {@code null}
     * @throws IllegalStateException if called while an event is being delivered
     */
    public boolean moveFocus(FocusDirection direction) {
        Objects.requireNonNull(direction, "direction");
        requireNotDelivering();
        return navigateFocus(direction);
    }

    /**
     * Returns whether keyboard focus is shown, so that widgets draw their focused look. Focus
     * follows the last input. Navigation that finds a target (Tab, Shift+Tab, the arrow keys, and
     * {@link #moveFocus(FocusDirection)}) and a key-down that a widget in the focused chain
     * handled, such as Enter on a button, make it visible. Any accepted pointer press hides it.
     * Key-ups, modifier keys, typed characters, and keys that no widget in the focused chain
     * handled do not change it. Before any input it is hidden. Setting the focus with
     * {@link #setKeyboardFocus} does not change this, so game-set focus shows when the last
     * input was one of the keyboard inputs above.
     *
     * @return {@code true} if some widget has keyboard focus and the last input was keyboard input
     */
    public boolean isFocusVisible() {
        return lastInputWasKeyboard && getKeyboardFocus() != null;
    }

    private boolean navigateFocus(FocusDirection direction) {
        if (openMenuBar != null) {
            return false;
        }
        Widget previous = getKeyboardFocus();
        Widget target = FocusNavigator.findTarget(root, previous, direction);
        if (target == null) {
            return false;
        }
        assignKeyboardFocus(target);
        lastInputWasKeyboard = true;
        refreshFocusShown();
        return true;
    }

    private void refreshFocusShown() {
        Widget wanted = isFocusVisible() ? getKeyboardFocus() : null;
        if (wanted == focusShownWidget) {
            return;
        }
        if (focusShownWidget != null) {
            focusShownWidget.setFocusShown(false);
        }
        focusShownWidget = wanted;
        if (wanted != null) {
            wanted.setFocusShown(true);
        }
    }

    private void trackShift(UiEventType type, int keycode) {
        boolean down = type == UiEventType.KEY_DOWN;
        if (keycode == Input.Keys.SHIFT_LEFT) {
            leftShiftDown = down;
        } else if (keycode == Input.Keys.SHIFT_RIGHT) {
            rightShiftDown = down;
        }
    }

    private static FocusDirection directionForKey(int keycode, boolean shiftHeld) {
        return switch (keycode) {
            case Input.Keys.TAB -> shiftHeld ? FocusDirection.PREVIOUS : FocusDirection.NEXT;
            case Input.Keys.UP -> FocusDirection.UP;
            case Input.Keys.DOWN -> FocusDirection.DOWN;
            case Input.Keys.LEFT -> FocusDirection.LEFT;
            case Input.Keys.RIGHT -> FocusDirection.RIGHT;
            default -> null;
        };
    }

    private boolean navigateByKey(UiEventType type, int keycode, boolean shiftHeld) {
        if (type != UiEventType.KEY_DOWN) {
            return false;
        }
        FocusDirection direction = directionForKey(keycode, shiftHeld);
        return direction != null && navigateFocus(direction);
    }

    private boolean dispatchKey(UiEventType type, int keycode, char character,
            boolean shiftHeld) {
        requireNotDelivering();
        trackShift(type, keycode);
        Widget focused = getKeyboardFocus();
        deliverToKeyListeners(keyObservers, focused != null ? focused : root, type, keycode,
                character);
        if (focused != null) {
            event.setKey(type, focused, keycode, character);
            if (bubble(focused)) {
                showFocusAfterHandledKey(type, keycode);
                return true;
            }
        }
        if (deliverToKeyListeners(unhandledKeyListeners, root, type, keycode, character)) {
            return true;
        }
        return navigateByKey(type, keycode, shiftHeld);
    }

    private void showFocusAfterHandledKey(UiEventType type, int keycode) {
        if (type != UiEventType.KEY_DOWN || isModifierKey(keycode)) {
            return;
        }
        lastInputWasKeyboard = true;
        refreshFocusShown();
    }

    private static boolean isModifierKey(int keycode) {
        return keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT
                || keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.CONTROL_RIGHT
                || keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT;
    }

    private boolean deliverToKeyListeners(UiListener[] snapshot, Widget target,
            UiEventType type, int keycode, char character) {
        if (snapshot.length == 0) {
            return false;
        }
        event.setKey(type, target, keycode, character);
        delivering = true;
        try {
            for (int i = 0; i < snapshot.length; i++) {
                if (snapshot[i].handle(event)) {
                    return true;
                }
            }
            return false;
        } finally {
            delivering = false;
        }
    }

    // Both key hooks are for menu bars and live as long as the root; they never unregister.
    // Observers see every key event first and cannot consume it; unhandled-key listeners see only
    // the key events that the focused widget chain did not handle.
    void addUnhandledKeyListener(UiListener listener) {
        unhandledKeyListeners = appendListener(unhandledKeyListeners, listener);
    }

    void addKeyObserver(UiListener listener) {
        keyObservers = appendListener(keyObservers, listener);
    }

    private static UiListener[] appendListener(UiListener[] listeners, UiListener listener) {
        UiListener[] grown = Arrays.copyOf(listeners, listeners.length + 1);
        grown[listeners.length] = listener;
        return grown;
    }

    MenuBar getOpenMenuBar() {
        return openMenuBar;
    }

    void setOpenMenuBar(MenuBar menuBarOrNull) {
        openMenuBar = menuBarOrNull;
    }

    Widget getFocusBeforePress() {
        return focusBeforePress;
    }

    int getSurfaceColumns() {
        return hiddenSurface.getColumns();
    }

    int getSurfaceRows() {
        return hiddenSurface.getRows();
    }

    /** Receives every accepted press, even one that a widget consumes before its listeners. */
    interface PressObserver {
        /** Called after hover is updated, before the press is delivered; must not call input. */
        void pointerPressed(int pointer);
    }

    void addPressObserver(PressObserver observer) {
        PressObserver[] grown = Arrays.copyOf(pressObservers, pressObservers.length + 1);
        grown[pressObservers.length] = observer;
        pressObservers = grown;
    }

    void removePressObserver(PressObserver observer) {
        int index = Arrays.asList(pressObservers).indexOf(observer);
        if (index < 0) {
            return;
        }
        PressObserver[] shrunk = new PressObserver[pressObservers.length - 1];
        System.arraycopy(pressObservers, 0, shrunk, 0, index);
        System.arraycopy(pressObservers, index + 1, shrunk, index, shrunk.length - index);
        pressObservers = shrunk.length == 0 ? NO_PRESS_OBSERVERS : shrunk;
    }

    private void notifyPressObservers(int pointer) {
        PressObserver[] snapshot = pressObservers;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].pointerPressed(pointer);
        }
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
        Widget popupHit = popupLayer.isVisible()
                ? popupLayer.hitChildren(column - popupLayer.getX(), row - popupLayer.getY())
                : null;
        if (popupHit != null) {
            return popupHit;
        }
        return root.isVisible() ? root.hit(column - root.getX(), row - root.getY()) : null;
    }

    private boolean isInTrees(Widget widget) {
        if (widget == null) {
            return false;
        }
        Widget top = widget;
        while (top.getParent() != null) {
            top = top.getParent();
        }
        return (top == root || top == popupLayer) && widget.isShown();
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
        hoverColumn = column;
        hoverRow = row;
        sweptHover = null;
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
