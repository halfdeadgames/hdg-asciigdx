package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import java.util.Arrays;
import java.util.Objects;

/**
 * A framed panel with a title in its top border, one content widget, an optional close button, and
 * dragging by the title bar. Put windows in {@link UiRoot#getWindowLayer()}, where the last child
 * is in front. A window also works in any other group.
 *
 * <p>Layout, in window cells (column 0 and row 0 are the border):
 * <ul>
 *   <li>The border is one cell thick on all four sides, drawn with the style's
 *       line style and border look.
 *   <li>The inside, one cell away from every edge, is filled with the style's background. The
 *       content widget sits at its top-left corner, at window cell (1, 1), and keeps its own
 *       size.
 *   <li>The title sits in the top border row: the corner, one border glyph, a space, the title, a
 *       space, then border glyphs, as in {@code ┌─ Title ───[x]┐}. A title that does not fit is cut
 *       and ends with {@code U+2026} (the ellipsis); the title gets at most the cells between the
 *       space after the border glyph and the close button, or the right corner when there is no
 *       close button, minus the closing space. When that leaves no cell, no title is drawn.
 *   <li>The close button, if the style has one, sits at the right end of the top row, followed by
 *       one border glyph and the corner. It is a {@link TextButton} child, so it shows its
 *       normal, pointer-over, pressed, and focused looks, and it takes part in keyboard
 *       navigation after the content.
 * </ul>
 *
 * <p>Tall-font content: the content starts on window row 1, so a window with tall-font content must
 * sit on an odd surface row for that content to start on an even row. Dragging moves the window by
 * whole cells and does not keep that alignment.
 *
 * <p>Input:
 * <ul>
 *   <li>A press anywhere on the window moves it to the front of its parent (see
 *       {@link #toFront()}); the {@link UiRoot} does this for presses on any descendant, even one
 *       that consumes the press. A press that reaches the window itself, such as one on the border
 *       or an empty part of the inside, is consumed.
 *   <li>If the window is {@linkplain #isMovable() movable}, pressing the top border row with the
 *       primary button (but not the close button) and dragging moves the window. The window stays
 *       where the pointer takes it, but its top row always stays fully inside the drag bounds, and
 *       the window never leaves it to the left or right unless it is wider than the area (it
 *       then sits at the area's left edge). The drag bounds is the surface, or the part of the
 *       surface chosen with {@link UiRoot#setWindowDragBounds(CellArea)}. This clamping applies
 *       only to windows pressed through a {@link UiRoot}, which tells the window the area; it
 *       affects dragging only, never {@link #setPosition(int, int)}.
 *   <li>The border uses the style's focused border look while keyboard focus is inside the
 *       window, whether or not the focus look is shown (see {@link UiRoot#isFocusVisible()}), so
 *       the active window stays visible after a mouse click.
 *   <li>Escape, when it reaches the window from a focused descendant and the window is
 *       {@linkplain #isCloseOnEscape() set to close on Escape}, closes the window.
 * </ul>
 *
 * <p>The window is not focusable itself. A press that lands on the window's own area (the border,
 * the title row outside the close button, or an empty part of the inside) does not clear keyboard
 * focus that is already inside the window. If focus is elsewhere or nowhere, the press gives focus
 * to the window's first widget in focus order, without showing the focus look; a window with
 * nothing focusable clears focus, as a press on any widget that is not focusable does.
 *
 * <p>Children: only {@link #setContent} places content. Direct children added with the inherited
 * {@code addChild} are not laid out and keep the positions they are given. The close button always
 * stays the last child, so keyboard navigation reaches it after the content.
 *
 * <p>Extending: a subclass such as a dialog can override {@link #layoutContent}, the preferred
 * size methods, {@link #handleEvent}, {@link #close()}, and {@code drawContent}; the last three
 * must call super.
 *
 * <p>Example:
 * <pre>{@code
 * Window window = new Window("Settings", style);
 * window.setContent(table);
 * window.pack();
 * window.setPosition(4, 3);
 * window.addCloseListener(closed -> saveSettings());
 * ui.getWindowLayer().addChild(window);
 * }</pre>
 */
public class Window extends WidgetGroup {

    // Thickness of the border on every side, in UI cells.
    static final int BORDER_THICKNESS = 1;

    private static final WindowCloseListener[] NO_CLOSE_LISTENERS = new WindowCloseListener[0];
    private static final int PRIMARY_BUTTON = 0;
    private static final int NO_POINTER = -1;
    private static final int UNKNOWN_SURFACE_SIZE = 0;
    private static final int TITLE_SPACE_COLUMN = 2;
    private static final int TITLE_START_COLUMN = 3;
    // Columns kept free right of the title: its closing space, and the cell before the button.
    private static final int TITLE_RESERVED_COLUMNS = 4;
    private static final int ELLIPSIS = 0x2026;
    private static final int SPACE = ' ';

    private final WindowStyle style;
    private final TextButton closeButton;
    private String title;
    private Widget content;
    private boolean movable = true;
    private boolean closeOnEscape;
    private WindowCloseListener[] closeListeners = NO_CLOSE_LISTENERS;
    private int armedPointer = NO_POINTER;
    private int grabColumn;
    private int grabRow;
    private int dragBoundsLeft;
    private int dragBoundsTop;
    private int dragBoundsColumns = UNKNOWN_SURFACE_SIZE;
    private int dragBoundsRows = UNKNOWN_SURFACE_SIZE;

    /**
     * Creates an empty window sized to fit its title and close button, at position (0, 0), with
     * no parent.
     *
     * @param title text for the top border; must not be {@code null}; may be empty
     * @param style looks of the window; must not be {@code null}
     * @throws NullPointerException if {@code title} or {@code style} is {@code null}
     */
    public Window(String title, WindowStyle style) {
        this.title = Objects.requireNonNull(title, "title");
        this.style = Objects.requireNonNull(style, "style");
        if (style.hasCloseButton()) {
            closeButton = new TextButton(style.getCloseText(), style.getCloseButton());
            closeButton.addClickListener(clicked -> close());
            addChild(closeButton);
        } else {
            closeButton = null;
        }
        setSize(contentFitWidth(), contentFitHeight());
    }

    /**
     * Returns the looks of the window.
     *
     * @return the style, never {@code null}
     */
    public WindowStyle getStyle() {
        return style;
    }

    /**
     * Returns the title.
     *
     * @return the title, never {@code null}
     */
    public String getTitle() {
        return title;
    }

    /**
     * Replaces the title. The size stays as it is, so call {@link #pack()} to fit the new title.
     * A different title requests a redraw; an equal one changes nothing.
     *
     * @param title new title; must not be {@code null}; may be empty
     * @throws NullPointerException if {@code title} is {@code null}
     */
    public void setTitle(String title) {
        Objects.requireNonNull(title, "title");
        if (title.equals(this.title)) {
            return;
        }
        this.title = title;
        requestRedraw();
    }

    /**
     * Returns whether the content area has a widget. The content area is the part of the
     * interior that a window gives to its content; a subclass may reserve the rest of the
     * interior for itself, as {@link Dialog} does for its buttons.
     *
     * @return {@code true} after {@link #setContent} and before {@link #clearContent()}
     */
    public boolean hasContent() {
        return content != null;
    }

    /**
     * Returns the widget in the content area (see {@link #hasContent()}).
     *
     * @return the content, never {@code null}
     * @throws IllegalStateException if the content area is empty; check {@link #hasContent()}
     */
    public Widget getContent() {
        if (content == null) {
            throw new IllegalStateException("The window has no content");
        }
        return content;
    }

    /**
     * Sets the widget of the content area (see {@link #hasContent()}), which replaces the
     * previous one. The previous content loses its parent. In a plain window the content area is
     * the whole interior: the new content is placed at window cell (1, 1) by the next layout and
     * keeps its size. A subclass may reserve part of the interior and place the content in the
     * rest. Call {@link #pack()} to fit the window to it. Calls {@link #invalidate()}.
     *
     * @param content widget to show inside the border; must not be {@code null}, and must have no
     *         parent
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code content} already has a parent, or is this window
     *         or one of its ancestors; the window is unchanged then, and keeps its old content
     */
    public void setContent(Widget content) {
        Objects.requireNonNull(content, "content");
        if (content == this.content) {
            return;
        }
        requireAttachable(content);
        detachContentAndCloseButton();
        addChild(content);
        this.content = content;
        attachCloseButton();
    }

    /**
     * Removes the widget of the content area (see {@link #hasContent()}), if any. It loses its
     * parent. Calls {@link #invalidate()}. Does nothing when the content area is empty.
     */
    public void clearContent() {
        if (content == null) {
            return;
        }
        detachContentAndCloseButton();
        content = null;
        attachCloseButton();
    }

    // Mirrors the checks of addChild, so a rejected content leaves the window untouched.
    private void requireAttachable(Widget candidate) {
        if (candidate.getParent() != null) {
            throw new IllegalArgumentException("Widget already has a parent");
        }
        for (Widget current = this; current != null; current = current.getParent()) {
            if (current == candidate) {
                throw new IllegalArgumentException(
                        "A group cannot contain itself or its ancestors");
            }
        }
    }

    // The close button is the last child, so keyboard navigation reaches it after the content.
    private void detachContentAndCloseButton() {
        if (closeButton != null) {
            removeChild(closeButton);
        }
        if (content != null) {
            removeChild(content);
        }
    }

    private void attachCloseButton() {
        if (closeButton != null) {
            addChild(closeButton);
        }
    }

    /**
     * Returns whether the user can drag the window by its top border row. A dragged window stays
     * inside the root's drag bounds (see {@link UiRoot#setWindowDragBounds(CellArea)}).
     *
     * @return {@code true} when movable; {@code true} for a new window
     */
    public boolean isMovable() {
        return movable;
    }

    /**
     * Allows or forbids dragging the window by its top border row. A drag already in progress
     * ends at its next event.
     *
     * @param movable {@code true} to allow dragging
     */
    public void setMovable(boolean movable) {
        this.movable = movable;
        if (!movable) {
            armedPointer = NO_POINTER;
        }
    }

    /**
     * Returns whether the Escape key closes the window.
     *
     * @return {@code true} when it does; {@code false} for a new window
     */
    public boolean isCloseOnEscape() {
        return closeOnEscape;
    }

    /**
     * Sets whether the Escape key closes the window. Escape only reaches the window while keyboard
     * focus is on one of its descendants, and a descendant that consumes Escape keeps it.
     *
     * @param closeOnEscape {@code true} to close on Escape
     */
    public void setCloseOnEscape(boolean closeOnEscape) {
        this.closeOnEscape = closeOnEscape;
    }

    /**
     * Adds a listener that is called every time the window closes, in the order the listeners
     * were added. Safe to call from inside a listener; the change applies from the next close.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addCloseListener(WindowCloseListener listener) {
        Objects.requireNonNull(listener, "listener");
        WindowCloseListener[] grown = Arrays.copyOf(closeListeners, closeListeners.length + 1);
        grown[closeListeners.length] = listener;
        closeListeners = grown;
    }

    /**
     * Removes the first occurrence of a close listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeCloseListener(WindowCloseListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(closeListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        WindowCloseListener[] shrunk = new WindowCloseListener[closeListeners.length - 1];
        System.arraycopy(closeListeners, 0, shrunk, 0, index);
        System.arraycopy(closeListeners, index + 1, shrunk, index, shrunk.length - index);
        closeListeners = shrunk.length == 0 ? NO_CLOSE_LISTENERS : shrunk;
    }

    /**
     * Closes the window: removes it from its parent, if it has one, and then calls every close
     * listener. The window keeps its content, position, and size, so adding it to a group again
     * reopens it. Listeners are called even when the window had no parent. Keyboard focus inside
     * the window is cleared by the {@link UiRoot} the next time it reads focus.
     *
     * <p>The close button and the Escape key both call this method, so an override changes what
     * they do. An override must call {@code super.close()}.
     */
    public void close() {
        armedPointer = NO_POINTER;
        WidgetGroup parent = getParent();
        if (parent != null) {
            parent.removeChild(this);
        }
        WindowCloseListener[] snapshot = closeListeners;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].windowClosed(this);
        }
    }

    /**
     * Moves the window to the end of its parent's children, so it draws on top of its siblings and
     * is hit first. Does nothing without a parent or when it is already last. Keyboard focus and
     * hover are not changed.
     */
    public void toFront() {
        WidgetGroup parent = getParent();
        if (parent == null || parent.getChild(parent.getChildCount() - 1) == this) {
            return;
        }
        parent.removeChild(this);
        parent.addChild(this);
    }

    // Called by UiRoot for a press on this window or a descendant; also tells the surface size,
    // which drag clamping needs: the effective rectangle in surface cells.
    void pressed(int dragBoundsLeft, int dragBoundsTop, int dragBoundsColumns, int dragBoundsRows) {
        this.dragBoundsLeft = dragBoundsLeft;
        this.dragBoundsTop = dragBoundsTop;
        this.dragBoundsColumns = dragBoundsColumns;
        this.dragBoundsRows = dragBoundsRows;
        toFront();
    }

    /**
     * Sets the size to the smallest one that shows the content, the border, the whole title, and
     * the close button. The content is packed first (see {@link Widget#pack()}).
     */
    @Override
    public void pack() {
        if (content != null) {
            content.pack();
        }
        super.pack();
    }

    /**
     * Returns the content's preferred width plus the border, but at least the width that shows
     * the whole title and the close button.
     *
     * @return the width in UI cells, 2 or greater
     */
    @Override
    public int getPreferredWidth() {
        return contentFitWidth();
    }

    /**
     * Returns the content's preferred height plus the border.
     *
     * @return the height in UI cells, 2 or greater
     */
    @Override
    public int getPreferredHeight() {
        return contentFitHeight();
    }

    // Private, so the constructor can size the window without calling overridable methods.
    private int contentFitWidth() {
        int contentWidth = content == null ? 0 : content.getPreferredWidth();
        return Math.max(contentWidth + 2 * BORDER_THICKNESS, minimumWidthForTitleRow());
    }

    private int contentFitHeight() {
        int contentHeight = content == null ? 0 : content.getPreferredHeight();
        return contentHeight + 2 * BORDER_THICKNESS;
    }

    private int minimumWidthForTitleRow() {
        int buttonWidth = closeButton == null ? 0 : closeButton.getPreferredWidth();
        if (title.isEmpty()) {
            return closeButton == null ? 2 : buttonWidth + 3;
        }
        int titleCells = title.codePointCount(0, title.length());
        return closeButton == null
                ? titleCells + TITLE_START_COLUMN + 2
                : titleCells + buttonWidth + TITLE_START_COLUMN + 3;
    }

    /**
     * Places the content and the close button. Subclasses override {@link #layoutContent} to
     * change the content's place; they need not override this method.
     */
    @Override
    protected void layout() {
        layoutContent(BORDER_THICKNESS, BORDER_THICKNESS,
                Math.max(0, getWidth() - 2 * BORDER_THICKNESS),
                Math.max(0, getHeight() - 2 * BORDER_THICKNESS));
        layoutCloseButton();
    }

    /**
     * Places the content inside the border. The default puts it at the top-left corner of the
     * area and keeps its size. Called from {@link #layout()}, so the rules of
     * {@link Widget#layout()} apply. A subclass can override it to reserve space in the area, for
     * example for a row of buttons.
     *
     * @param left column of the area's left edge, in window cells
     * @param top row of the area's top edge, in window cells
     * @param width width of the area inside the border, in UI cells, zero or greater
     * @param height height of the area inside the border, in UI cells, zero or greater
     */
    protected void layoutContent(int left, int top, int width, int height) {
        if (content != null) {
            content.setPosition(left, top);
        }
    }

    private void layoutCloseButton() {
        if (closeButton == null) {
            return;
        }
        closeButton.setSize(closeButton.getPreferredWidth(), 1);
        closeButton.setPosition(closeButtonLeft(), 0);
    }

    // Right end of the top row: the button, one border glyph, then the corner.
    private int closeButtonLeft() {
        return Math.max(BORDER_THICKNESS,
                getWidth() - 2 * BORDER_THICKNESS - closeButton.getPreferredWidth());
    }

    private int titleCellLimit() {
        int rightLimit = closeButton == null ? getWidth() - BORDER_THICKNESS : closeButtonLeft();
        return rightLimit - TITLE_RESERVED_COLUMNS;
    }

    /**
     * Draws the window in this order: erases the glyphs under it, fills its whole area with the
     * style's background, draws the border, draws the title, and then draws the children (content
     * and close button). A subclass that overrides this must call {@code super.drawContent}, and
     * anything it draws before that call is covered by the window.
     *
     * @param target surface to draw into; never {@code null}
     * @param left column of the window's left edge in {@code target}, in UI cells
     * @param top row of the window's top edge in {@code target}, in UI cells
     */
    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        LabelStyle borderLook = holdsKeyboardFocus(this)
                ? style.getFocusedBorder()
                : style.getBorder();
        target.eraseGlyphs(left, top, width, height);
        target.fillBackground(left, top, width, height, style.getBackgroundRgba());
        target.drawBox(left, top, width, height, style.getBox(), borderLook);
        drawTitle(target, left, top);
        super.drawContent(target, left, top);
    }

    private static boolean holdsKeyboardFocus(Widget widget) {
        if (widget.hasKeyboardFocus()) {
            return true;
        }
        if (widget instanceof WidgetGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                if (holdsKeyboardFocus(group.getChild(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void drawTitle(UiSurface target, int left, int top) {
        int maxCells = titleCellLimit();
        if (title.isEmpty() || maxCells < 1) {
            return;
        }
        LabelStyle look = style.getTitle();
        int titleCells = title.codePointCount(0, title.length());
        int shownCells = Math.min(titleCells, maxCells);
        boolean cut = titleCells > maxCells;
        target.fillBackground(left + TITLE_SPACE_COLUMN, top, shownCells + 2, 1,
                look.getBackgroundRgba());
        target.writeGlyph(UiFont.SQUARE, left + TITLE_SPACE_COLUMN, top, SPACE,
                look.getForegroundRgba());
        writeTitleText(target, look, left + TITLE_START_COLUMN, top, shownCells, cut);
        target.writeGlyph(UiFont.SQUARE, left + TITLE_START_COLUMN + shownCells, top, SPACE,
                look.getForegroundRgba());
    }

    private void writeTitleText(UiSurface target, LabelStyle look, int column, int row,
            int shownCells, boolean cut) {
        int index = 0;
        for (int cell = 0; cell < shownCells; cell++) {
            int codePoint = title.codePointAt(index);
            if (cut && cell == shownCells - 1) {
                codePoint = ELLIPSIS;
            }
            target.writeGlyph(UiFont.SQUARE, column + cell, row, codePoint,
                    look.getForegroundRgba());
            index += Character.charCount(codePoint);
        }
    }

    /**
     * Handles the events that reach the window, either aimed at it or bubbling up from a
     * descendant. It consumes every {@link UiEventType#POINTER_DOWN} that reaches it (and starts a
     * drag when the press is on the top border row), the drag events of its own drag, and
     * Escape key-downs while {@linkplain #isCloseOnEscape() close on Escape} is set. All other
     * events are not consumed. A subclass that overrides this must call
     * {@code super.handleEvent(event)} to keep that behavior.
     *
     * @param event event being delivered; never {@code null}
     * @return {@code true} if the event was consumed
     */
    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_DOWN -> handlePointerDown(event);
            case POINTER_DRAGGED -> handlePointerDragged(event);
            case POINTER_UP -> handlePointerUp(event);
            case POINTER_CANCELLED -> handlePointerCancelled(event);
            case KEY_DOWN -> handleKeyDown(event);
            default -> false;
        };
    }

    private boolean handlePointerDown(UiEvent event) {
        boolean onTitleRow = event.getTarget() == this && event.getRow() == 0
                && event.getButton() == PRIMARY_BUTTON;
        if (movable && onTitleRow) {
            armedPointer = event.getPointer();
            grabColumn = event.getColumn();
            grabRow = event.getRow();
        }
        return true;
    }

    private boolean handlePointerDragged(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        moveToSurfacePosition(event.getSurfaceColumn() - grabColumn,
                event.getSurfaceRow() - grabRow);
        return true;
    }

    private boolean handlePointerUp(UiEvent event) {
        if (event.getPointer() != armedPointer || event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        armedPointer = NO_POINTER;
        return true;
    }

    private boolean handlePointerCancelled(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        armedPointer = NO_POINTER;
        return true;
    }

    private boolean handleKeyDown(UiEvent event) {
        if (!closeOnEscape || event.getKeycode() != Input.Keys.ESCAPE) {
            return false;
        }
        close();
        return true;
    }

    private void moveToSurfacePosition(int desiredLeft, int desiredTop) {
        int left = desiredLeft;
        int top = desiredTop;
        if (dragBoundsColumns != UNKNOWN_SURFACE_SIZE) {
            int rightmostLeft = dragBoundsLeft + dragBoundsColumns - getWidth();
            left = Math.max(dragBoundsLeft, Math.min(left, rightmostLeft));
            top = Math.max(dragBoundsTop, Math.min(top, dragBoundsTop + dragBoundsRows - 1));
        }
        int parentLeft = 0;
        int parentTop = 0;
        for (Widget ancestor = getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            parentLeft += ancestor.getX();
            parentTop += ancestor.getY();
        }
        setPosition(left - parentLeft, top - parentTop);
    }
}
