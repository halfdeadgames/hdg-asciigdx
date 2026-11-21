package com.github.halfdeadgames.asciigdx.ui;

/**
 * One input event, as seen by a {@link UiListener}. A {@link UiRoot} owns a single instance and
 * refills it for every event, so listeners must read what they need during
 * {@link UiListener#handle} and must not keep the reference afterward. The same instance is
 * also used for {@link UiEventType#POINTER_ENTERED} and {@link UiEventType#POINTER_EXITED}.
 *
 * <p>While an event is delivered, calling an input method of the {@link UiRoot} throws
 * {@link IllegalStateException}; defer such work to the next frame with a
 * {@link UiUpdateListener}.
 *
 * <p>Fields that do not apply to the event's type hold neutral values: {@code -1} for the
 * pointer (key events only), button, and keycode, {@code 0} for the character and for all
 * coordinates. Units: every plain "column" ({@link #getColumn()}, {@link #getSurfaceColumn()}) is
 * a whole UI cell, and every "textColumn" ({@link #getTextColumn()},
 * {@link #getSurfaceTextColumn()}) is half a UI cell, two per cell. Columns grow right and rows
 * (always UI cells) grow down. Do not mix the two units. Key events and
 * {@link UiEventType#POINTER_CANCELLED} and {@link UiEventType#SCROLLED} carry no position.
 * The scroll steps are 0 for all other types.
 */
public final class UiEvent {

    static final int NOT_APPLICABLE = -1;

    private UiEventType type = UiEventType.POINTER_MOVED;
    private Widget target;
    private Widget currentWidget;
    private int surfaceColumn;
    private int surfaceTextColumn;
    private int relativeTextColumn;
    private int surfaceRow;
    private int relativeColumn;
    private int relativeRow;
    private int pointer = NOT_APPLICABLE;
    private int button = NOT_APPLICABLE;
    private int keycode = NOT_APPLICABLE;
    private char character;
    private int scrollColumns;
    private int scrollRows;

    UiEvent() {
    }

    /**
     * Returns the kind of event.
     *
     * @return the type, never {@code null}
     */
    public UiEventType getType() {
        return type;
    }

    /**
     * Returns the widget the event was aimed at: the deepest widget under the pointer, the widget
     * that captured the pointer on press, or the keyboard-focused widget. For
     * {@link UiEventType#POINTER_EXITED} it is the widget the pointer left, and for
     * {@link UiEventType#POINTER_ENTERED} the deepest widget the pointer came onto.
     *
     * @return the target, never {@code null} while a listener runs
     */
    public Widget getTarget() {
        return target;
    }

    /**
     * Returns the widget whose listener is running: the target first, then its ancestors while the
     * event bubbles.
     *
     * @return the current widget, never {@code null} while a listener runs
     */
    public Widget getCurrentWidget() {
        return currentWidget;
    }

    /**
     * Returns the pointer column relative to {@link #getCurrentWidget()}'s left edge. The value
     * changes while the event bubbles, because the current widget changes. Negative or past the
     * widget's width when the pointer is outside it, for example during a drag.
     *
     * @return the column in whole UI cells (not text columns; see {@link #getTextColumn()} for
     *         half cells); 0 for key events and cancellations
     */
    public int getColumn() {
        return relativeColumn;
    }

    /**
     * Returns the pointer row relative to {@link #getCurrentWidget()}'s top edge. The value
     * changes while the event bubbles, because the current widget changes.
     *
     * @return the row in UI cells; 0 for key events and cancellations
     */
    public int getRow() {
        return relativeRow;
    }

    /**
     * Returns the pointer column on the surface. May lie outside the surface.
     *
     * @return the column in whole UI cells (not text columns; see
     *         {@link #getSurfaceTextColumn()} for half cells); 0 for key events and
     *         cancellations
     */
    public int getSurfaceColumn() {
        return surfaceColumn;
    }

    /**
     * Returns the pointer row on the surface. May lie outside the surface.
     *
     * @return the row in UI cells; 0 for key events and cancellations
     */
    public int getSurfaceRow() {
        return surfaceRow;
    }

    /**
     * Returns the text column of the half cell the pointer hit, relative to
     * {@link #getCurrentWidget()}'s left edge, in the widget's local text columns. Text column
     * {@code 2 * c} is the left half of local UI column {@code c} and {@code 2 * c + 1} the right
     * half, so {@code floorDiv(getTextColumn(), 2) == getColumn()} for a pointer event. The value
     * changes while the event bubbles, as {@link #getColumn()} does, and is negative or past the
     * widget's width when the pointer is outside it. The value is exact when the root is fed by
     * {@link UiInput}. When a game calls the public {@code UiRoot.pointer*} methods directly, they
     * take whole cells, so it is the left half of the cell ({@code 2 * getColumn()}).
     *
     * @return the text column; 0 for key events and cancellations, as for {@link #getColumn()}
     */
    public int getTextColumn() {
        return relativeTextColumn;
    }

    /**
     * Returns the text column of the half cell the pointer hit on the surface. May lie outside the
     * surface. Text column {@code 2 * c} is the left half of surface column {@code c}. Exact when
     * fed by {@link UiInput}; the left half of the cell when the public {@code UiRoot.pointer*}
     * methods are called directly.
     *
     * @return the text column; 0 for key events and cancellations
     */
    public int getSurfaceTextColumn() {
        return surfaceTextColumn;
    }

    /**
     * Returns the pointer index, 0 for the mouse or the first finger.
     *
     * @return the pointer; 0 for {@link UiEventType#POINTER_MOVED}, since hover is driven by
     *         pointer 0; -1 for key events
     */
    public int getPointer() {
        return pointer;
    }

    /**
     * Returns the pointer button, 0 for the primary button.
     *
     * @return the button for {@link UiEventType#POINTER_DOWN} and {@link UiEventType#POINTER_UP};
     *         -1 for every other type
     */
    public int getButton() {
        return button;
    }

    /**
     * Returns the key code, as in {@code com.badlogic.gdx.Input.Keys}.
     *
     * @return the key code for {@link UiEventType#KEY_DOWN} and {@link UiEventType#KEY_UP}; -1 for
     *         every other type
     */
    public int getKeycode() {
        return keycode;
    }

    /**
     * Returns the typed character.
     *
     * @return the character for {@link UiEventType#KEY_TYPED}; {@code '\0'} for every other type
     */
    public char getCharacter() {
        return character;
    }

    /**
     * Returns the horizontal wheel steps. They are wheel steps, not cells; a widget decides how
     * far a step moves it.
     *
     * @return for {@link UiEventType#SCROLLED}, the signed whole steps, positive to the right, as
     *         in the libGDX {@code amountX}; 0 for every other type
     */
    public int getScrollStepsX() {
        return scrollColumns;
    }

    /**
     * Returns the vertical wheel steps. They are wheel steps, not cells.
     *
     * @return for {@link UiEventType#SCROLLED}, the signed whole steps, positive down, as in the
     *         libGDX {@code amountY}; 0 for every other type
     */
    public int getScrollStepsY() {
        return scrollRows;
    }

    void setScroll(Widget target, int scrollColumns, int scrollRows) {
        reset(UiEventType.SCROLLED, target);
        this.scrollColumns = scrollColumns;
        this.scrollRows = scrollRows;
    }

    void setPointer(UiEventType type, Widget target, int surfaceColumn, int surfaceRow,
            int pointer, int button) {
        reset(type, target);
        this.surfaceColumn = surfaceColumn;
        this.surfaceTextColumn = UiSurface.TEXT_COLUMNS_PER_CELL * surfaceColumn;
        this.surfaceRow = surfaceRow;
        this.pointer = pointer;
        this.button = button;
    }

    void setSurfaceTextColumn(int surfaceTextColumn) {
        this.surfaceTextColumn = surfaceTextColumn;
    }

    void setCancelled(Widget target, int pointer) {
        reset(UiEventType.POINTER_CANCELLED, target);
        this.pointer = pointer;
    }

    void setKey(UiEventType type, Widget target, int keycode, char character) {
        reset(type, target);
        this.keycode = keycode;
        this.character = character;
    }

    void setCurrent(Widget currentWidget, int relativeColumn, int relativeRow) {
        this.currentWidget = currentWidget;
        this.relativeColumn = relativeColumn;
        this.relativeRow = relativeRow;
        int absoluteLeft = surfaceColumn - relativeColumn;
        this.relativeTextColumn =
                surfaceTextColumn - UiSurface.TEXT_COLUMNS_PER_CELL * absoluteLeft;
    }

    boolean hasPosition() {
        return type != UiEventType.POINTER_CANCELLED && type != UiEventType.KEY_DOWN
                && type != UiEventType.KEY_UP && type != UiEventType.KEY_TYPED
                && type != UiEventType.SCROLLED;
    }

    private void reset(UiEventType type, Widget target) {
        this.type = type;
        this.target = target;
        this.currentWidget = target;
        surfaceColumn = 0;
        surfaceTextColumn = 0;
        relativeTextColumn = 0;
        surfaceRow = 0;
        relativeColumn = 0;
        relativeRow = 0;
        pointer = NOT_APPLICABLE;
        button = NOT_APPLICABLE;
        keycode = NOT_APPLICABLE;
        character = 0;
        scrollColumns = 0;
        scrollRows = 0;
    }
}
