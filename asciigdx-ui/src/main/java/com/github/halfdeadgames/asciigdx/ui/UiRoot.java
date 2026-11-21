package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * The top of a widget tree on one {@link UiSurface}: it draws the tree, routes input to widgets,
 * and tracks hover, press capture, and keyboard focus. It works purely in surface cells and never
 * touches libGDX statics, so it can run headless; {@link UiInput} connects it to libGDX input.
 *
 * <p>Three widget trees exist. {@link #getRoot()} holds the main widgets. {@link #getWindowLayer()}
 * is drawn above it, for {@link Window}s. {@link #getPopupLayer()} is drawn above both, for menus
 * and tooltips. Hit-testing goes the other way: popup layer first, then window layer (last child
 * first), then the main tree. All trees start at the surface's top-left corner.
 *
 * <p>Drawing: the root draws into a hidden surface of the same size, then copies it into the shown
 * surface. Equal cells stay clean for the grids' dirty tracking, so only changed cells are
 * uploaded to the GPU. Nothing is drawn on frames where no widget changed.
 *
 * <p>Connecting input: connect libGDX input with {@link UiInput#forRoot}. A game
 * that routes input to this root by hand must forward every event, including {@code keyUp}
 * and {@code keyTyped}. Without {@code keyTyped}, text fields get no typed characters
 * (Backspace and Delete arrive as typed characters too). Games must forward {@code keyUp};
 * without it, a held arrow key keeps repeating. If the game's input path can lose key-up events
 * (for example when the window loses focus), call {@link #releaseHeldKeys()} instead. On Android
 * the soft keyboard stays closed unless something listens with
 * {@link #addTextInputWantedListener}, as {@link UiInput} does.
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
 *       (see {@link FocusDirection}). Only one top-level tree is searched: the window (a direct
 *       child of the window layer) that holds keyboard focus, or the main tree when focus is
 *       elsewhere or nowhere. The popup layer is never searched. Nothing moves while a menu is
 *       open. Hidden widgets (see {@link Widget#setVisible(boolean)}) and menu bars are
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
 *       focus move that the repeated key causes keeps it going. The key still counts as held
 *       after a focus change. If the same arrow key-down arrives again before its key-up, as
 *       when a platform repeats on its own, the root's repeat stays off until that key is
 *       released (or {@link #releaseHeldKeys()} is called).
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
 * <p>Modal dialogs: while a {@link Dialog} is shown (see {@link Dialog#show(UiRoot)}), the root
 * lets input reach only the top dialog and the popup layer.
 * <ul>
 *   <li>A press anywhere else, also outside the surface, is swallowed: the input method returns
 *       {@code true}, no widget hears it, focus stays, and no drag or up follows for it.
 *   <li>Widgets outside the dialog are never hovered, so their tooltips never show, and a hover
 *       over them is cleared when the dialog is shown. Scroll steps over them are consumed and
 *       ignored.
 *   <li>Menus: an open menu is closed when the dialog is shown, and menu shortcuts (Alt plus a
 *       letter, and the other unhandled-key listeners) are not called while a dialog is shown.
 *       Key observers still see every key, so a menu bar keeps track of the Alt key.
 *   <li>Keyboard navigation (Tab, the arrow keys, {@link #moveFocus}) stays inside the top
 *       dialog.
 *   <li>Drawing: the main tree, then the other windows, then the dim of the top dialog's style,
 *       then the dialogs from the lowest to the top one, then the popup layer.
 *   <li>Focus: the dialog gets focus when it is shown, and the focus from before is restored
 *       when it closes, if that widget is still shown and, with dialogs below, inside the dialog
 *       below. A dialog that leaves the window layer in any other way is treated as closed.
 * </ul>
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
    private static final TextInputWantedListener[] NO_TEXT_INPUT_LISTENERS =
            new TextInputWantedListener[0];
    static final float KEY_REPEAT_INITIAL_DELAY_SECONDS = 0.4f;
    static final float KEY_REPEAT_INTERVAL_SECONDS = 0.05f;
    private static final int NO_KEY = -1;
    private static final int PRIMARY_POINTER = 0;
    private static final int NO_BUTTON = UiEvent.NOT_APPLICABLE;

    private final UiSurface shownSurface;
    private final UiSurface hiddenSurface;
    private final WidgetGroup root = new WidgetGroup();
    private final WidgetGroup windowLayer = new WidgetGroup();
    private final WidgetGroup popupLayer = new WidgetGroup();
    private CellArea windowDragBounds;
    private final UiEvent event = new UiEvent();
    private final ArrayList<ModalEntry> modalStack = new ArrayList<>();
    private boolean hoverRefreshPending;
    private boolean hoverPositionKnown;
    private final Widget[] capturedWidgets = new Widget[MAX_POINTERS];
    private final int[] capturingButtons = new int[MAX_POINTERS];
    private UiUpdateListener[] updateListeners = NO_UPDATE_LISTENERS;
    private PressObserver[] pressObservers = NO_PRESS_OBSERVERS;
    private UiListener[] unhandledKeyListeners = NO_KEY_LISTENERS;
    private UiListener[] keyObservers = NO_KEY_LISTENERS;
    private MenuBar openMenuBar;
    private Widget focusBeforePress;
    private boolean delivering;
    private boolean hasPendingTextColumn;
    private int pendingTextColumn;
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
    private float scrollRemainderX;
    private float scrollRemainderY;
    private int repeatingKeycode = NO_KEY;
    private int heldArrowKeycode = NO_KEY;
    private boolean repeatingWithShift;
    private boolean firingKeyRepeat;
    private float secondsUntilKeyRepeat;
    private boolean autoFocusTextFields;
    private boolean textInputWanted;
    private TextInputWantedListener[] textInputWantedListeners = NO_TEXT_INPUT_LISTENERS;

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
     * Returns the group drawn above {@link #getRoot()} and below {@link #getPopupLayer()}. Its
     * children are hit-tested after the popup layer's and before the main tree's, the last child
     * first, and the group itself is never a hit target. Use it for {@link Window}s: the last
     * child is the front-most window, and a press on a window moves it to the end (see
     * {@link Window#toFront()}). Keyboard navigation inside a window stays inside it (see the
     * class description).
     *
     * @return the window layer group, never {@code null}
     */
    public WidgetGroup getWindowLayer() {
        return windowLayer;
    }

    /**
     * Limits where the user can drag windows to a part of the surface. While a window is dragged
     * by its title row, its whole width stays inside the area horizontally (a window wider than
     * the area sits at the area's left edge) and its title row stays inside the area vertically.
     * Without an area, windows are kept inside the whole surface.
     *
     * <p>The area is intersected with the surface, so it may reach past the surface edge. If it
     * lies completely outside the surface, the whole surface is used, as if no area were set.
     *
     * <p>The area affects dragging only. {@link Window#setPosition(int, int)}, the centering of a
     * {@link Dialog}, and windows that already lie outside the area are left alone until the user
     * drags them. It applies to every window in {@link #getWindowLayer()}, including dialogs, and
     * takes effect from the next press on a window. Call it at setup or whenever the layout
     * changes, for example after a resize.
     *
     * <p>The area is stored exactly as given and compared with the current surface size on every
     * press. A surface resize never moves or shrinks the stored area.
     *
     * @param area rectangle in surface cells, y-down; must not be {@code null}
     * @throws NullPointerException if {@code area} is {@code null}
     */
    public void setWindowDragBounds(CellArea area) {
        windowDragBounds = Objects.requireNonNull(area, "area");
    }

    /**
     * Removes the window drag bounds, so windows are kept inside the whole surface again. Does
     * nothing when no area is set. Takes effect from the next press on a window.
     */
    public void clearWindowDragBounds() {
        windowDragBounds = null;
    }

    /**
     * Returns whether a window drag bounds is set.
     *
     * @return {@code true} after {@link #setWindowDragBounds(CellArea)} and before
     *     {@link #clearWindowDragBounds()}; {@code false} for a new root
     */
    public boolean hasWindowDragBounds() {
        return windowDragBounds != null;
    }

    /**
     * Returns the window drag bounds exactly as it was set, without intersecting it with the
     * surface. The value is unchanged by surface size, because the area is compared with the
     * current surface size only when a window is pressed.
     *
     * @return the area in surface cells, y-down; never {@code null}
     * @throws IllegalStateException if no area is set; check {@link #hasWindowDragBounds()} first.
     *     Kotlin property access ({@code root.windowDragBounds}) throws the same way
     */
    public CellArea getWindowDragBounds() {
        if (windowDragBounds == null) {
            throw new IllegalStateException("No window drag bounds is set");
        }
        return windowDragBounds;
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

    // Focus set by code or keyboard also scrolls the new owner into view in enclosing scroll panes.
    private void assignKeyboardFocus(Widget next) {
        changeKeyboardFocus(next);
        revealInScrollPanes(next);
    }

    private static void revealInScrollPanes(Widget widget) {
        if (widget == null) {
            return;
        }
        for (Widget ancestor = widget.getParent(); ancestor != null;
                ancestor = ancestor.getParent()) {
            if (ancestor instanceof ScrollPane) {
                ((ScrollPane) ancestor).scrollIntoView(widget);
            }
        }
    }

    // Single place where the focus owner changes, so the owners and the on-screen keyboard follow.
    private void changeKeyboardFocus(Widget next) {
        Widget previous = keyboardFocus;
        if (previous == next) {
            return;
        }
        keyboardFocus = next;
        keyboardFocusWasFocusable = next != null && next.isFocusable();
        if (!firingKeyRepeat) {
            stopRepeatTimer();
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
        if (!textInputWanted) {
            return;
        }
        TextInputWantedListener[] snapshot = textInputWantedListeners;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].textInputRequestedAgain();
        }
    }

    private void notifyTextInputWanted(boolean wanted) {
        TextInputWantedListener[] snapshot = textInputWantedListeners;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].textInputWantedChanged(wanted);
        }
    }

    private void updateTextInputWanted() {
        boolean wanted = keyboardFocus instanceof TextField;
        if (wanted == textInputWanted) {
            return;
        }
        textInputWanted = wanted;
        notifyTextInputWanted(wanted);
    }

    /**
     * Hears when the root starts or stops wanting text input, that is, when a {@link TextField}
     * gains or loses keyboard focus. Use it to show or hide the Android soft keyboard.
     */
    @FunctionalInterface
    public interface TextInputWantedListener {

        /**
         * Called only when {@link UiRoot#isTextInputWanted()} actually changes. Runs on the
         * render thread, during input handling. Do not call {@link UiRoot#setKeyboardFocus} or
         * other input methods of the root from here.
         *
         * @param wanted {@code true} if a text field now has keyboard focus, {@code false} if
         *         focus left all text fields
         */
        void textInputWantedChanged(boolean wanted);

        /**
         * Called when the user presses a text field that already has keyboard focus, so a closed
         * soft keyboard can be shown again. {@link UiRoot#isTextInputWanted()} stays
         * {@code true}. The default does nothing. The same restrictions as for
         * {@link #textInputWantedChanged} apply.
         */
        default void textInputRequestedAgain() {
        }
    }

    /**
     * Returns whether a {@link TextField} of this root currently has keyboard focus, so the game
     * should offer text input (for example by showing the soft keyboard).
     *
     * @return {@code true} while a text field has keyboard focus
     */
    public boolean isTextInputWanted() {
        return textInputWanted;
    }

    /**
     * Adds a listener that hears when text input becomes wanted or unwanted (see
     * {@link #isTextInputWanted()}). Listeners are called in the order they were added:
     * {@link TextInputWantedListener#textInputWantedChanged} only when the state changes, and
     * {@link TextInputWantedListener#textInputRequestedAgain} when the user presses the already
     * focused text field. Adding the same listener twice makes it run twice. Adding or removing
     * listeners from inside a listener is safe: a notification that is already running uses the
     * listeners that existed when it started. A listener must not call
     * {@link #setKeyboardFocus} or any other input method of this root while it is called.
     * Does not call the listener at once, so read {@link #isTextInputWanted()} for the current
     * state.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addTextInputWantedListener(TextInputWantedListener listener) {
        Objects.requireNonNull(listener, "listener");
        TextInputWantedListener[] grown =
                Arrays.copyOf(textInputWantedListeners, textInputWantedListeners.length + 1);
        grown[textInputWantedListeners.length] = listener;
        textInputWantedListeners = grown;
    }

    /**
     * Removes the first occurrence of a text input listener. Does nothing if it was not added.
     * Safe to call from inside a listener, including the listener itself; the same notification
     * rules as in {@link #addTextInputWantedListener} apply.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeTextInputWantedListener(TextInputWantedListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(textInputWantedListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        TextInputWantedListener[] shrunk =
                new TextInputWantedListener[textInputWantedListeners.length - 1];
        System.arraycopy(textInputWantedListeners, 0, shrunk, 0, index);
        System.arraycopy(textInputWantedListeners, index + 1, shrunk, index,
                shrunk.length - index);
        textInputWantedListeners = shrunk.length == 0 ? NO_TEXT_INPUT_LISTENERS : shrunk;
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
        windowLayer.validate();
        popupLayer.validate();
        if (!root.isRedrawNeeded() && !windowLayer.isRedrawNeeded()
                && !popupLayer.isRedrawNeeded()) {
            return;
        }
        int columns = hiddenSurface.getColumns();
        int rows = hiddenSurface.getRows();
        hiddenSurface.clearClips();
        hiddenSurface.eraseGlyphs(0, 0, columns, rows);
        hiddenSurface.fillBackground(0, 0, columns, rows,
                TerminalGrid.TRANSPARENT_RGBA);
        root.draw(hiddenSurface);
        drawWindowLayer();
        popupLayer.draw(hiddenSurface);
        shownSurface.copyFrom(hiddenSurface);
        root.clearRedrawNeeded();
        windowLayer.clearRedrawNeeded();
        popupLayer.clearRedrawNeeded();
    }

    // With modals shown: the other windows, then one dim pass (top modal's settings), then the
    // modals in stack order.
    private void drawWindowLayer() {
        if (modalStack.isEmpty()) {
            windowLayer.draw(hiddenSurface);
            return;
        }
        if (!windowLayer.isVisible()) {
            return;
        }
        for (int i = 0; i < windowLayer.getChildCount(); i++) {
            Widget child = windowLayer.getChild(i);
            if (indexOfModal(child) < 0) {
                child.draw(hiddenSurface);
            }
        }
        ModalEntry top = modalStack.get(modalStack.size() - 1);
        if (top.dimAmount() > 0f) {
            hiddenSurface.dimAll(top.dimRgba(), top.dimAmount());
        }
        for (int i = 0; i < modalStack.size(); i++) {
            modalStack.get(i).modal().draw(hiddenSurface);
        }
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
        dropGoneModals();
        clearFocusIfOwnerGone();
        for (int pointer = 0; pointer < MAX_POINTERS; pointer++) {
            cancelCaptureIfWidgetRemoved(pointer);
        }
        if (hoverRefreshPending) {
            hoverRefreshPending = false;
            updateHover(hitAt(hoverColumn, hoverRow), hoverColumn, hoverRow);
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
     * <p>Called directly, the pointer position is a whole UI cell, so the event's text columns
     * ({@link UiEvent#getTextColumn()}) are the left half of that cell ({@code 2 * column}).
     * {@link UiInput} gives exact text columns.
     *
     * @param column pointer column on the surface, in UI cells (not text columns); any value,
     *         outside hits nothing
     * @param row pointer row on the surface, in UI cells; any value
     * @return {@code true} if a listener handled the move; {@code false} also when the pointer is
     *         over nothing
     * @throws IllegalStateException if called while an event is being delivered
     */
    public boolean pointerMoved(int column, int row) {
        requireNotDelivering();
        dropGoneModals();
        Widget target = hitAt(column, row);
        updateHover(target, column, row);
        if (target == null) {
            return false;
        }
        event.setPointer(UiEventType.POINTER_MOVED, target, column, row, PRIMARY_POINTER,
                NO_BUTTON);
        applyPendingTextColumn();
        return bubble(target);
    }

    // The package-private pointer methods below are called by UiInput, which knows the exact text
    // column. The public methods leave the text column at the left half of the cell.
    boolean pointerMovedAtTextColumn(int textColumn, int row) {
        beginTextColumn(textColumn);
        try {
            return pointerMoved(squareColumnOf(textColumn), row);
        } finally {
            hasPendingTextColumn = false;
        }
    }

    boolean pointerDownAtTextColumn(int textColumn, int row, int pointer, int button) {
        beginTextColumn(textColumn);
        try {
            return pointerDown(squareColumnOf(textColumn), row, pointer, button);
        } finally {
            hasPendingTextColumn = false;
        }
    }

    boolean pointerDraggedAtTextColumn(int textColumn, int row, int pointer) {
        beginTextColumn(textColumn);
        try {
            return pointerDragged(squareColumnOf(textColumn), row, pointer);
        } finally {
            hasPendingTextColumn = false;
        }
    }

    boolean pointerUpAtTextColumn(int textColumn, int row, int pointer, int button) {
        beginTextColumn(textColumn);
        try {
            return pointerUp(squareColumnOf(textColumn), row, pointer, button);
        } finally {
            hasPendingTextColumn = false;
        }
    }

    private static int squareColumnOf(int textColumn) {
        return Math.floorDiv(textColumn, UiSurface.TEXT_COLUMNS_PER_CELL);
    }

    private void beginTextColumn(int textColumn) {
        pendingTextColumn = textColumn;
        hasPendingTextColumn = true;
    }

    private void applyPendingTextColumn() {
        if (hasPendingTextColumn) {
            event.setSurfaceTextColumn(pendingTextColumn);
        }
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
     * <p>Called directly, the pointer position is a whole UI cell, so the event's text columns
     * ({@link UiEvent#getTextColumn()}) are the left half of that cell ({@code 2 * column}).
     * {@link UiInput} gives exact text columns.
     *
     * @param column pointer column on the surface, in UI cells (not text columns); any value,
     *         outside hits nothing
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
        dropGoneModals();
        Widget target = hitAt(column, row);
        updateHoverForPointer(pointer, target, column, row);
        if (target == null && !modalStack.isEmpty()) {
            return true;
        }
        notifyPressObservers(pointer);
        notifyWindowsPressed(target);
        focusBeforePress = getKeyboardFocus();
        // A press does not scroll the view, which would move the pressed widget under the pointer.
        changeKeyboardFocus(resolvePressFocus(target));
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
        applyPendingTextColumn();
        return bubble(target);
    }

    /**
     * Handles a pressed pointer moving. The event goes to the widget that captured the pointer,
     * even when the pointer is outside it. Hover still follows the widget under pointer 0.
     *
     * <p>Called directly, the event's text columns are the left half of the cell
     * ({@code 2 * column}); {@link UiInput} gives exact text columns.
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
        applyPendingTextColumn();
        return bubble(captured);
    }

    /**
     * Handles a pointer going up. The event goes to the widget that captured the pointer, and the
     * capture ends before the listeners run, but only when {@code button} is the one that started
     * the capture. An up with another button is ignored.
     *
     * <p>Called directly, the event's text columns are the left half of the cell
     * ({@code 2 * column}); {@link UiInput} gives exact text columns.
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
        applyPendingTextColumn();
        return bubble(captured);
    }

    /**
     * Handles the system canceling a pointer, for example when the app loses focus. The event goes
     * to the widget that captured the pointer, and the capture ends before the listeners run. The
     * event carries no position, so its text columns are 0 whether called directly or through
     * {@link UiInput}.
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
     * Handles the wheel or a trackpad scrolling. The amounts are added to a remainder kept for
     * each axis, and only whole steps are delivered, because trackpads send fractions. For each
     * axis the whole part of the sum is delivered and the fraction stays for the next call; an
     * amount with the opposite sign of the kept remainder drops that remainder first. When at
     * least one axis has a whole step, {@link UiEventType#SCROLLED} goes to the widget under
     * pointer 0 at its last known position (see {@link #getHoveredWidget()}) and bubbles up
     * through its parents until a {@link UiListener} returns {@code true}. Steps are lost when
     * nothing is hovered. Non-finite amounts are ignored. While a modal dialog is shown, whole
     * steps over anything outside the top dialog and the popup layer are consumed and delivered
     * to nobody.
     *
     * @param amountX horizontal amount, positive to the right (libGDX units, usually whole steps
     *         for a wheel); any finite value
     * @param amountY vertical amount, positive to scroll down (libGDX convention); any finite
     *         value
     * @return {@code true} if a listener handled the event, or if a modal dialog consumed it;
     *         {@code false} when there was no whole step, or when no modal dialog is shown and
     *         nothing is hovered or no listener handled it
     * @throws IllegalStateException if called while an event is being delivered
     */
    public boolean scrolled(float amountX, float amountY) {
        requireNotDelivering();
        dropGoneModals();
        if (!Float.isFinite(amountX) || !Float.isFinite(amountY)) {
            return false;
        }
        float sumX = addToRemainder(scrollRemainderX, amountX);
        float sumY = addToRemainder(scrollRemainderY, amountY);
        int stepsX = (int) sumX;
        int stepsY = (int) sumY;
        scrollRemainderX = sumX - stepsX;
        scrollRemainderY = sumY - stepsY;
        Widget target = getHoveredWidget();
        if (stepsX == 0 && stepsY == 0) {
            return false;
        }
        if (!modalStack.isEmpty() && !isReachableWhileModalShown(target)) {
            return true;
        }
        if (target == null) {
            return false;
        }
        event.setScroll(target, stepsX, stepsY);
        return bubble(target);
    }

    private static float addToRemainder(float remainder, float amount) {
        boolean oppositeSign =
                remainder != 0f && amount != 0f && (remainder < 0f) != (amount < 0f);
        return (oppositeSign ? 0f : remainder) + amount;
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
        stopRepeatTimer();
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

    // Ends the repeat and forgets the held key, so the next key-down of it starts a new repeat.
    private void stopKeyRepeat() {
        stopRepeatTimer();
        heldArrowKeycode = NO_KEY;
    }

    // Keeps the held key, so a platform's own repeat of that key-down does not restart the timer.
    private void stopRepeatTimer() {
        repeatingKeycode = NO_KEY;
    }

    /**
     * Stops any key repeat as if every held key had been released. Call it when the game's input
     * path may lose key-up events, for example when the window loses focus. Afterwards, the next
     * key-down of an arrow key starts a new repeat. Does nothing when no key is held. Does not
     * send any key event to widgets.
     */
    public void releaseHeldKeys() {
        stopKeyRepeat();
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
     * rules are those of the keys: only the window that holds focus, or else the main tree
     * ({@link #getRoot()}), is searched, the candidates are its visible, focusable widgets
     * (menu bars are skipped), and nothing happens
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
        dropGoneModals();
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
        Widget scope = focusScopeOf(previous);
        Widget from = !modalStack.isEmpty() && !isSelfOrAncestor(scope, previous) ? null : previous;
        Widget target = FocusNavigator.findTarget(scope, from, direction);
        if (target == null) {
            return false;
        }
        assignKeyboardFocus(target);
        lastInputWasKeyboard = true;
        refreshFocusShown();
        return true;
    }

    // Navigation stays in one top-level tree: the window that holds focus, or else the main tree.
    private Widget focusScopeOf(Widget focused) {
        if (!modalStack.isEmpty()) {
            return modalStack.get(modalStack.size() - 1).modal();
        }
        for (Widget current = focused; current != null; current = current.getParent()) {
            if (current.getParent() == windowLayer) {
                return current;
            }
        }
        return root;
    }

    // A press on anything inside a window raises that window; windows may also sit in other trees.
    private void notifyWindowsPressed(Widget target) {
        for (Widget current = target; current != null; current = current.getParent()) {
            if (current instanceof Window window) {
                passDragBounds(window);
            }
        }
    }

    // Clamps the drag bounds to the surface; an empty intersection falls back to the whole surface.
    private void passDragBounds(Window window) {
        int surfaceColumns = getSurfaceColumns();
        int surfaceRows = getSurfaceRows();
        if (windowDragBounds == null) {
            window.pressed(0, 0, surfaceColumns, surfaceRows);
            return;
        }
        int left = windowDragBounds.column();
        int top = windowDragBounds.row();
        if (left >= surfaceColumns || top >= surfaceRows) {
            window.pressed(0, 0, surfaceColumns, surfaceRows);
            return;
        }
        int columns = Math.min(windowDragBounds.columnCount(), surfaceColumns - left);
        int rows = Math.min(windowDragBounds.rowCount(), surfaceRows - top);
        window.pressed(left, top, columns, rows);
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
        dropGoneModals();
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
        if (modalStack.isEmpty()
                && deliverToKeyListeners(unhandledKeyListeners, root, type, keycode, character)) {
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
        if (!modalStack.isEmpty()) {
            return hitTopModal(column, row);
        }
        Widget windowHit = windowLayer.isVisible()
                ? windowLayer.hitChildren(column - windowLayer.getX(), row - windowLayer.getY())
                : null;
        if (windowHit != null) {
            return windowHit;
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
        return (top == root || top == windowLayer || top == popupLayer) && widget.isShown();
    }

    // A press on a window's own area (nothing focusable under it) keeps focus that is already
    // inside the window, or else focuses the window's first widget in focus order.
    private Widget resolvePressFocus(Widget target) {
        Widget focusable = findFocusable(target);
        if (focusable != null) {
            return focusable;
        }
        Window window = findWindowAbove(target);
        if (window == null) {
            return null;
        }
        if (isSelfOrAncestor(window, focusBeforePress)) {
            return focusBeforePress;
        }
        return FocusNavigator.findTarget(window, null, FocusDirection.NEXT);
    }

    private static Window findWindowAbove(Widget target) {
        for (Widget current = target; current != null; current = current.getParent()) {
            if (current instanceof Window window) {
                return window;
            }
        }
        return null;
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
        hoverPositionKnown = true;
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

    /** A shown modal with the focus to restore when it goes and its dim settings. */
    private record ModalEntry(Window modal, Widget focusToRestore, int dimRgba,
            float dimAmount) {
    }

    /**
     * Shows a window as a modal: it becomes the top child of the window layer, and until it is
     * hidden, pointer, scroll, key, and focus input reaches only it and the popup layer, and a
     * press elsewhere is swallowed. Closes an open menu, then moves focus into the modal. The
     * alpha of {@code dimRgba} is ignored. Allowed while an event is being delivered.
     */
    void showModal(Window modal, Widget initialFocusOrNull, int dimRgba, float dimAmount) {
        Objects.requireNonNull(modal, "modal");
        if (Float.isNaN(dimAmount) || dimAmount < 0f || dimAmount > 1f) {
            throw new IllegalArgumentException("dimAmount must be 0 to 1, was " + dimAmount);
        }
        dropGoneModals();
        if (indexOfModal(modal) >= 0) {
            throw new IllegalArgumentException("The window is already shown as a modal");
        }
        if (openMenuBar != null) {
            openMenuBar.close();
        }
        Widget focusToRestore = getKeyboardFocus();
        WidgetGroup previousParent = modal.getParent();
        if (previousParent != null) {
            previousParent.removeChild(modal);
        }
        windowLayer.addChild(modal);
        modalStack.add(new ModalEntry(modal, focusToRestore, dimRgba, dimAmount));
        assignKeyboardFocus(chooseInitialModalFocus(modal, initialFocusOrNull));
        refreshFocusShown();
        refreshHover();
    }

    /** Removes a modal from the stack and the window layer, then restores focus. */
    void hideModal(Window modal) {
        Objects.requireNonNull(modal, "modal");
        int index = indexOfModal(modal);
        if (index < 0) {
            return;
        }
        if (modal.getParent() == windowLayer) {
            windowLayer.removeChild(modal);
        }
        removeModalEntry(index);
    }

    boolean isModalShown() {
        return !modalStack.isEmpty();
    }

    Window getTopModal() {
        if (modalStack.isEmpty()) {
            throw new IllegalStateException("No modal is shown");
        }
        return modalStack.get(modalStack.size() - 1).modal();
    }

    private static Widget chooseInitialModalFocus(Window modal, Widget initialFocusOrNull) {
        if (initialFocusOrNull != null && isSelfOrAncestor(modal, initialFocusOrNull)) {
            return initialFocusOrNull;
        }
        return FocusNavigator.findTarget(modal, null, FocusDirection.NEXT);
    }

    private int indexOfModal(Widget widget) {
        for (int i = 0; i < modalStack.size(); i++) {
            if (modalStack.get(i).modal() == widget) {
                return i;
            }
        }
        return -1;
    }

    // A modal that left the window layer by removeChild or close() is dropped like a hidden one.
    private void dropGoneModals() {
        for (int i = modalStack.size() - 1; i >= 0; i--) {
            if (modalStack.get(i).modal().getParent() != windowLayer) {
                removeModalEntry(i);
            }
        }
    }

    private void removeModalEntry(int index) {
        boolean wasTop = index == modalStack.size() - 1;
        ModalEntry removed = modalStack.remove(index);
        if (wasTop) {
            restoreFocusAfterModal(removed);
        }
        refreshHover();
    }

    private void restoreFocusAfterModal(ModalEntry removed) {
        Widget scope = modalStack.isEmpty() ? null : getTopModal();
        Widget saved = removed.focusToRestore();
        boolean savedUsable = saved != null && isInTrees(saved)
                && (scope == null || isSelfOrAncestor(scope, saved));
        Widget next = null;
        if (savedUsable) {
            next = saved;
        } else if (scope != null) {
            next = FocusNavigator.findTarget(scope, null, FocusDirection.NEXT);
        }
        assignKeyboardFocus(next);
        refreshFocusShown();
    }

    // During event delivery the refresh waits for the next update or draw.
    private void refreshHover() {
        if (!hoverPositionKnown) {
            return;
        }
        if (delivering) {
            hoverRefreshPending = true;
            return;
        }
        updateHover(hitAt(hoverColumn, hoverRow), hoverColumn, hoverRow);
    }

    private Widget hitTopModal(int column, int row) {
        Window top = getTopModal();
        if (!windowLayer.isVisible() || !top.isVisible()) {
            return null;
        }
        return top.hit(column - windowLayer.getX() - top.getX(),
                row - windowLayer.getY() - top.getY());
    }

    // A null widget (the pointer over nothing) is not reachable while a modal is shown.
    private boolean isReachableWhileModalShown(Widget widget) {
        if (modalStack.isEmpty()) {
            return true;
        }
        Window top = getTopModal();
        for (Widget current = widget; current != null; current = current.getParent()) {
            if (current == top || current == popupLayer) {
                return true;
            }
        }
        return false;
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
