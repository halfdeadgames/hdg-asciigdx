package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import java.util.Objects;

/**
 * A viewport in whole cells onto one content widget that is larger than the view. The content is
 * laid out at its preferred size (it is packed) and moved by the scroll position: its position is
 * {@code (-getScrollColumn(), -getScrollRow())}, so the position of the content is managed by the
 * pane and a position set on it is overwritten at the next layout. Everything outside the view is
 * clipped.
 *
 * <p>Scrollbars use the square font. A vertical bar (one column at the right edge) shows only when
 * the content is taller than the view, and a horizontal bar (one row at the bottom edge) only when
 * it is wider; a bar takes space from the view, which can make the other bar necessary too. When
 * both show, the corner cell is drawn in the track color. The thumb moves smoothly in eighths of
 * a cell: its length is the view's share of the content, at least one cell, and the eighth-block
 * glyphs U+2581 to U+258F paint its partly covered end cells. The thumb uses the pointer-over color
 * while the pointer is over it and the pressed color while it is dragged.
 *
 * <p>The preferred size is the content's preferred size, so a packed pane shows no bars. Set a
 * smaller size to make the content scroll. The content is fixed for the pane's lifetime: removing
 * it with {@link #removeChild} is unsupported. Extra children added with {@link #addChild} are
 * clipped to the view but not scrolled or laid out by the pane.
 *
 * <p>Row snap: the vertical scroll position is always a multiple of the content's
 * {@link Widget#getRowSnap()} (2 for tall-font content), so tall glyphs never land on an odd row.
 * Every way of scrolling vertically obeys it, and so does {@link #getMaxScrollRow()}, which is
 * rounded up to a multiple, so the last rows of content are always reachable. As a result the
 * view may show up to snap-1 empty rows past the content's end. Steps (wheel, arrows, pages) move at
 * least one snap unit.
 *
 * <p>There is no notification when the scroll position changes; poll the getters.
 *
 * <p>Pointer input:
 * <ul>
 *   <li>The wheel ({@link UiEventType#SCROLLED}) moves the view by {@link #getWheelStepCells()}
 *       cells per step on that axis ({@value #DEFAULT_WHEEL_STEP_CELLS} by default). When the view
 *       cannot move, the event is not consumed and bubbles to an outer pane.
 *   <li>Dragging the thumb moves the view in proportion; the pane captures the pointer for the drag.
 *   <li>A press on the track outside the thumb scrolls one page (the view's size) toward the
 *       press.
 *   <li>Dragging the content scrolls it: a press that no widget inside consumes (on a label or
 *       empty space) starts a drag that moves the content with the pointer in whole cells. A press
 *       on a button or another widget that consumes it never scrolls.
 *   <li>Only the primary button acts.
 * </ul>
 *
 * <p>Keyboard: Page Up and Page Down move one view, Home and End jump to the start and end; they
 * work while any widget inside or the pane itself has focus and are not consumed when nothing can
 * scroll. They act on the vertical axis, or on the horizontal axis when only that one scrolls.
 * When the pane itself has keyboard focus, the arrow keys move the view by one cell (and consume
 * the key only on an axis that can scroll).
 *
 * <p>Focus: the pane is in the Tab order, and reachable by arrow navigation, only when its
 * content has no widget that takes part in focus navigation. Then the pane itself has the focus and
 * its arrow keys scroll. Otherwise the widgets inside get the focus, and the view follows the focus
 * to keep the focused widget visible (a pointer press does not scroll the view). A press on the
 * pane still gives it the focus, because it is focusable.
 *
 * <p>Changing the scroll position moves the content and so marks the ancestors for layout.
 *
 * <p>Example:
 * <pre>{@code
 * Table list = new Table();
 * // ... add many rows to list ...
 * ScrollPane pane = new ScrollPane(list, skin.getScrollPaneStyle("scrollPane"));
 * pane.setSize(20, 8);
 * ui.getRoot().addChild(pane);
 * }</pre>
 */
public final class ScrollPane extends WidgetGroup {

    /** Cells that the view moves per wheel step until {@link #setWheelStepCells} changes it: 3. */
    public static final int DEFAULT_WHEEL_STEP_CELLS = 3;

    private static final int PRIMARY_BUTTON = 0;
    private static final int NO_POINTER = -1;
    private static final int EIGHTHS = EighthBlocks.EIGHTHS_PER_CELL;

    private enum DragKind {
        NONE,
        THUMB,
        CONTENT
    }

    private final Widget content;
    private ScrollPaneStyle style;
    private int wheelStepCells = DEFAULT_WHEEL_STEP_CELLS;
    private int scrollColumn;
    private int scrollRow;
    private int rowSnap = 1;
    private int contentColumns;
    private int contentRows;
    private int viewColumns;
    private int viewRows;
    private int maxScrollColumn;
    private int maxScrollRow;
    private boolean verticalBarShown;
    private boolean horizontalBarShown;
    private DragKind dragKind = DragKind.NONE;
    private int dragPointer = NO_POINTER;
    private BarOrientation dragBar;
    private BarOrientation hoveredBar;
    private int thumbGrabEighths;
    private int dragStartSurfaceColumn;
    private int dragStartSurfaceRow;
    private int dragStartScrollColumn;
    private int dragStartScrollRow;

    /**
     * Creates a pane that shows a content widget, packed to the content's preferred size and
     * scrolled to the top-left corner.
     *
     * @param content the widget to scroll; must not be {@code null} and must have no parent
     * @param style colors of the scrollbars; must not be {@code null}
     * @throws NullPointerException if {@code content} or {@code style} is {@code null}
     * @throws IllegalArgumentException if {@code content} already has a parent
     */
    public ScrollPane(Widget content, ScrollPaneStyle style) {
        this.content = Objects.requireNonNull(content, "content");
        this.style = Objects.requireNonNull(style, "style");
        addChild(content);
        pack();
        refreshGeometry();
    }

    /**
     * Returns the scrolled widget.
     *
     * @return the content, never {@code null}
     */
    public Widget getContent() {
        return content;
    }

    /**
     * Returns the looks of the scrollbars.
     *
     * @return the style, never {@code null}
     */
    public ScrollPaneStyle getStyle() {
        return style;
    }

    /**
     * Replaces the colors of the scrollbars and requests a redraw. An equal style changes
     * nothing.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(ScrollPaneStyle style) {
        Objects.requireNonNull(style, "style");
        if (style.equals(this.style)) {
            return;
        }
        this.style = style;
        requestRedraw();
    }

    /**
     * Returns the number of content columns scrolled out on the left.
     *
     * @return the scroll position, from 0 to {@link #getMaxScrollColumn()}
     */
    public int getScrollColumn() {
        return scrollColumn;
    }

    /**
     * Returns the number of content rows scrolled out at the top.
     *
     * @return the scroll position, from 0 to {@link #getMaxScrollRow()}
     */
    public int getScrollRow() {
        return scrollRow;
    }

    /**
     * Returns the largest horizontal scroll position: the content's width minus the view's width,
     * or 0 when the content fits. It follows the current size and content, even before the next
     * layout.
     *
     * @return the largest scroll column, zero or greater
     */
    public int getMaxScrollColumn() {
        refreshGeometryIfStale();
        return maxScrollColumn;
    }

    /**
     * Returns the largest vertical scroll position: the content's height minus the view's height,
     * or 0 when the content fits. It follows the current size and content, even before the next
     * layout.
     *
     * @return the largest scroll row, zero or greater
     */
    public int getMaxScrollRow() {
        refreshGeometryIfStale();
        return maxScrollRow;
    }

    /**
     * Returns the cells that the view moves per wheel step.
     *
     * @return the step, 1 or greater; {@link #DEFAULT_WHEEL_STEP_CELLS} for a new pane
     */
    public int getWheelStepCells() {
        return wheelStepCells;
    }

    /**
     * Sets the cells that the view moves per wheel step.
     *
     * @param cells cells per step; 1 or greater
     * @throws IllegalArgumentException if {@code cells} is less than 1
     */
    public void setWheelStepCells(int cells) {
        if (cells < 1) {
            throw new IllegalArgumentException("cells must be at least 1, was " + cells);
        }
        wheelStepCells = cells;
    }

    /**
     * Scrolls horizontally, as {@link #setScrollPosition} does, and keeps the vertical position.
     *
     * @param column content column at the left edge of the view, in UI cells; any value
     */
    public void setScrollColumn(int column) {
        setScrollPosition(column, scrollRow);
    }

    /**
     * Scrolls vertically, as {@link #setScrollPosition} does, and keeps the horizontal position.
     *
     * @param row content row at the top edge of the view, in UI cells; any value
     */
    public void setScrollRow(int row) {
        setScrollPosition(scrollColumn, row);
    }

    /**
     * Scrolls to a position, clamped to 0 to {@link #getMaxScrollColumn()} and 0 to
     * {@link #getMaxScrollRow()}, with the row rounded to the nearest multiple of the content's
     * row snap (see the class description). A change moves the content and requests a redraw;
     * listeners are not involved.
     *
     * @param column content column at the left edge of the view, in UI cells; any value (column
     *         comes first, then row)
     * @param row content row at the top edge of the view, in UI cells; any value
     */
    public void setScrollPosition(int column, int row) {
        refreshGeometryIfStale();
        int clampedColumn = Math.max(0, Math.min(column, maxScrollColumn));
        int clampedRow = snapRow(row);
        if (clampedColumn == scrollColumn && clampedRow == scrollRow) {
            return;
        }
        scrollColumn = clampedColumn;
        scrollRow = clampedRow;
        content.setPosition(-scrollColumn, -scrollRow);
        requestRedraw();
    }

    /**
     * Returns the content's preferred width, so a packed pane shows all of the content.
     *
     * @return the preferred width in UI cells
     */
    @Override
    public int getPreferredWidth() {
        return content.getPreferredWidth();
    }

    /**
     * Returns the content's preferred height, so a packed pane shows all of the content.
     *
     * @return the preferred height in UI cells
     */
    @Override
    public int getPreferredHeight() {
        return content.getPreferredHeight();
    }

    /**
     * Returns whether the pane takes keyboard focus, which is always the case: a press on the pane
     * focuses it. Tab and arrow navigation reach it only under the rule in the class description.
     *
     * @return {@code true}
     */
    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    protected boolean isInFocusOrder() {
        return super.isInFocusOrder() && !hasFocusableDescendant(content);
    }

    private static boolean hasFocusableDescendant(Widget widget) {
        if (!widget.isVisible()) {
            return false;
        }
        if (widget.isInFocusOrder()) {
            return true;
        }
        if (widget instanceof WidgetGroup) {
            WidgetGroup group = (WidgetGroup) widget;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasFocusableDescendant(group.getChild(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- layout ----

    @Override
    protected void layout() {
        refreshGeometry();
        scrollColumn = Math.max(0, Math.min(scrollColumn, maxScrollColumn));
        scrollRow = snapRow(scrollRow);
        content.pack();
        content.setPosition(-scrollColumn, -scrollRow);
    }

    private void refreshGeometryIfStale() {
        if (isLayoutNeeded()) {
            refreshGeometry();
        }
    }

    // Derived from the size and the content's preferred size; a bar takes a column or row from
    // the view, which can make the other bar necessary, so the flags settle in three steps.
    private void refreshGeometry() {
        contentColumns = content.getPreferredWidth();
        contentRows = content.getPreferredHeight();
        boolean vertical = contentRows > getHeight();
        boolean horizontal = contentColumns > getWidth() - (vertical ? 1 : 0);
        vertical = contentRows > getHeight() - (horizontal ? 1 : 0);
        verticalBarShown = vertical;
        horizontalBarShown = horizontal;
        viewColumns = Math.max(0, getWidth() - (vertical ? 1 : 0));
        viewRows = Math.max(0, getHeight() - (horizontal ? 1 : 0));
        maxScrollColumn = Math.max(0, contentColumns - viewColumns);
        rowSnap = Math.max(1, content.getRowSnap());
        maxScrollRow = (Math.max(0, contentRows - viewRows) + rowSnap - 1) / rowSnap * rowSnap;
    }

    // Clamps to the valid range, then rounds to the nearest multiple of the row snap. The maximum
    // is itself a multiple, so the result never exceeds it.
    private int snapRow(int row) {
        int clamped = Math.max(0, Math.min(row, maxScrollRow));
        return Math.min((clamped + rowSnap / 2) / rowSnap * rowSnap, maxScrollRow);
    }

    // A step of at least one snap unit, rounded up so that a small step never rounds to nothing.
    private int rowStepFor(int cells) {
        return (Math.max(cells, 1) + rowSnap - 1) / rowSnap * rowSnap;
    }

    // A page is the view's height rounded down to the snap, but at least one snap unit.
    private int pageRows() {
        return Math.max(rowSnap, viewRows / rowSnap * rowSnap);
    }

    // ---- hit testing ----

    /**
     * Returns the deepest widget at a point. A point outside the pane hits nothing, even where
     * the content reaches past the view. A scrollbar cell, including the corner, returns the pane
     * itself. A point in the view returns the deepest widget of the content under the scroll
     * offset, or the pane when the content does not cover it.
     *
     * @param column column in this pane's local coordinates, in UI cells; any value
     * @param row row in this pane's local coordinates, in UI cells; any value
     * @return the deepest widget hit, or {@code null} when nothing is
     */
    @Override
    public Widget hit(int column, int row) {
        if (!isVisible() || !isInsideBounds(column, row)) {
            return null;
        }
        if (isBarArea(column, row)) {
            return this;
        }
        Widget childHit = hitChildren(column, row);
        return childHit != null ? childHit : this;
    }

    private boolean isBarArea(int column, int row) {
        boolean onVerticalBar = verticalBarShown && column == getWidth() - 1;
        boolean onHorizontalBar = horizontalBarShown && row == getHeight() - 1;
        return onVerticalBar || onHorizontalBar;
    }

    private BarOrientation barAt(int column, int row) {
        if (verticalBarShown && column == getWidth() - 1 && row >= 0 && row < viewRows) {
            return BarOrientation.VERTICAL;
        }
        if (horizontalBarShown && row == getHeight() - 1 && column >= 0 && column < viewColumns) {
            return BarOrientation.HORIZONTAL;
        }
        return null;
    }

    // ---- drawing ----

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        target.pushClip(left, top, viewColumns, viewRows);
        try {
            super.drawContent(target, left, top);
        } finally {
            target.popClip();
        }
        if (verticalBarShown) {
            drawBar(target, BarOrientation.VERTICAL, left, top);
        }
        if (horizontalBarShown) {
            drawBar(target, BarOrientation.HORIZONTAL, left, top);
        }
        if (verticalBarShown && horizontalBarShown) {
            drawCorner(target, left, top);
        }
    }

    private void drawCorner(UiSurface target, int left, int top) {
        int column = left + getWidth() - 1;
        int row = top + getHeight() - 1;
        target.eraseGlyphs(column, row, 1, 1);
        target.fillBackground(column, row, 1, 1, style.getTrackRgba());
    }

    private void drawBar(UiSurface target, BarOrientation bar, int left, int top) {
        boolean vertical = bar == BarOrientation.VERTICAL;
        int cells = cellsAlong(bar);
        int barLeft = vertical ? left + getWidth() - 1 : left;
        int barTop = vertical ? top : top + getHeight() - 1;
        target.eraseGlyphs(barLeft, barTop, vertical ? 1 : cells, vertical ? cells : 1);
        target.fillBackground(barLeft, barTop, vertical ? 1 : cells, vertical ? cells : 1,
                style.getTrackRgba());
        int thumbStart = thumbStartEighths(bar);
        int thumbEnd = thumbStart + thumbLengthEighths(bar);
        int thumbRgba = thumbRgbaOf(bar);
        for (int index = 0; index < cells; index++) {
            int glyph = EighthBlocks.thumbGlyphForCell(bar, thumbStart, thumbEnd, index);
            if (glyph == EighthBlocks.EMPTY_GLYPH) {
                continue;
            }
            int column = vertical ? barLeft : barLeft + index;
            int row = vertical ? barTop + index : barTop;
            boolean trackGlyph = EighthBlocks.isTrackForeground(bar, thumbStart, thumbEnd, index);
            if (trackGlyph) {
                target.fillBackground(column, row, 1, 1, thumbRgba);
            }
            target.writeGlyph(UiFont.SQUARE, column, row, glyph,
                    trackGlyph ? style.getTrackRgba() : thumbRgba);
        }
    }

    private int thumbRgbaOf(BarOrientation bar) {
        if (dragKind == DragKind.THUMB && dragBar == bar) {
            return style.getThumbPressedRgba();
        }
        return hoveredBar == bar ? style.getThumbPointerOverRgba() : style.getThumbRgba();
    }

    // ---- scrollbar math, in eighths of a cell along the bar ----

    private int cellsAlong(BarOrientation bar) {
        return bar == BarOrientation.VERTICAL ? viewRows : viewColumns;
    }

    private int pageAlong(BarOrientation bar) {
        return bar == BarOrientation.VERTICAL ? pageRows() : viewColumns;
    }

    private int contentAlong(BarOrientation bar) {
        return bar == BarOrientation.VERTICAL ? contentRows : contentColumns;
    }

    private int scrollAlong(BarOrientation bar) {
        return bar == BarOrientation.VERTICAL ? scrollRow : scrollColumn;
    }

    private int maxScrollAlong(BarOrientation bar) {
        return bar == BarOrientation.VERTICAL ? maxScrollRow : maxScrollColumn;
    }

    private void setScrollAlong(BarOrientation bar, int value) {
        if (bar == BarOrientation.VERTICAL) {
            setScrollPosition(scrollColumn, value);
        } else {
            setScrollPosition(value, scrollRow);
        }
    }

    int thumbLengthEighths(BarOrientation bar) {
        int cells = cellsAlong(bar);
        int total = contentAlong(bar);
        if (cells == 0 || total <= 0) {
            return 0;
        }
        int trackEighths = cells * EIGHTHS;
        long proportional = Math.round((double) trackEighths * cells / total);
        return (int) Math.min(trackEighths, Math.max(EIGHTHS, proportional));
    }

    int thumbStartEighths(BarOrientation bar) {
        int room = cellsAlong(bar) * EIGHTHS - thumbLengthEighths(bar);
        int maxScroll = maxScrollAlong(bar);
        if (room <= 0 || maxScroll == 0) {
            return 0;
        }
        return (int) Math.round((double) room * scrollAlong(bar) / maxScroll);
    }

    private boolean overlapsThumb(BarOrientation bar, int index) {
        int thumbStart = thumbStartEighths(bar);
        int thumbEnd = thumbStart + thumbLengthEighths(bar);
        return index * EIGHTHS < thumbEnd && (index + 1) * EIGHTHS > thumbStart;
    }

    // ---- focus follow ----

    // Called by UiRoot for each pane above a widget that just got keyboard focus.
    void scrollIntoView(Widget descendant) {
        refreshGeometryIfStale();
        int offsetColumn = 0;
        int offsetRow = 0;
        Widget current = descendant;
        while (current != content) {
            if (current == null || current == this) {
                return;
            }
            offsetColumn += current.getX();
            offsetRow += current.getY();
            current = current.getParent();
        }
        int newColumn = scrollToShow(scrollColumn, viewColumns, offsetColumn,
                descendant.getWidth());
        int wantedRow = scrollToShow(scrollRow, viewRows, offsetRow, descendant.getHeight());
        // Round away from the old position, so the widget stays inside the view.
        int newRow = wantedRow < scrollRow
                ? wantedRow / rowSnap * rowSnap
                : (wantedRow + rowSnap - 1) / rowSnap * rowSnap;
        setScrollPosition(newColumn, newRow);
    }

    // The smallest move that shows the whole span; a span larger than the view shows its start.
    private static int scrollToShow(int scroll, int viewSize, int spanStart, int spanSize) {
        if (spanStart < scroll) {
            return spanStart;
        }
        if (spanStart + spanSize > scroll + viewSize) {
            return Math.min(spanStart, spanStart + spanSize - viewSize);
        }
        return scroll;
    }

    // ---- input ----

    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_MOVED -> handlePointerMoved(event);
            case POINTER_EXITED -> handlePointerExited();
            case POINTER_DOWN -> handlePointerDown(event);
            case POINTER_DRAGGED -> handlePointerDragged(event);
            case POINTER_UP -> handlePointerUp(event);
            case POINTER_CANCELLED -> endDrag(event.getPointer());
            case SCROLLED -> handleScrolled(event);
            case KEY_DOWN -> handleKeyDown(event.getKeycode());
            default -> false;
        };
    }

    private boolean handlePointerMoved(UiEvent event) {
        BarOrientation bar = null;
        if (event.getTarget() == this) {
            bar = barAt(event.getColumn(), event.getRow());
            if (bar != null && !overlapsThumb(bar, alongBar(bar, event))) {
                bar = null;
            }
        }
        setHoveredBar(bar);
        return false;
    }

    private boolean handlePointerExited() {
        setHoveredBar(null);
        return false;
    }

    private void setHoveredBar(BarOrientation bar) {
        if (hoveredBar == bar) {
            return;
        }
        hoveredBar = bar;
        requestRedraw();
    }

    private static int alongBar(BarOrientation bar, UiEvent event) {
        return bar == BarOrientation.VERTICAL ? event.getRow() : event.getColumn();
    }

    private boolean handlePointerDown(UiEvent event) {
        if (event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        if (event.getTarget() != this) {
            // Bubbled up from content that did not consume the press.
            beginContentDrag(event);
            return true;
        }
        BarOrientation bar = barAt(event.getColumn(), event.getRow());
        if (bar != null) {
            pressBar(bar, event);
            return true;
        }
        if (!isBarArea(event.getColumn(), event.getRow())) {
            beginContentDrag(event);
        }
        return true;
    }

    private void pressBar(BarOrientation bar, UiEvent event) {
        int index = alongBar(bar, event);
        if (overlapsThumb(bar, index)) {
            int thumbStart = thumbStartEighths(bar);
            int grab = index * EIGHTHS + EIGHTHS / 2 - thumbStart;
            thumbGrabEighths = Math.max(0, Math.min(grab, thumbLengthEighths(bar)));
            beginDrag(DragKind.THUMB, event.getPointer());
            dragBar = bar;
            return;
        }
        int direction = index * EIGHTHS < thumbStartEighths(bar) ? -1 : 1;
        setScrollAlong(bar, scrollAlong(bar) + direction * pageAlong(bar));
    }

    private void beginContentDrag(UiEvent event) {
        dragStartSurfaceColumn = event.getSurfaceColumn();
        dragStartSurfaceRow = event.getSurfaceRow();
        dragStartScrollColumn = scrollColumn;
        dragStartScrollRow = scrollRow;
        beginDrag(DragKind.CONTENT, event.getPointer());
    }

    private void beginDrag(DragKind kind, int pointer) {
        dragKind = kind;
        dragPointer = pointer;
        dragBar = null;
        requestRedraw();
    }

    private boolean handlePointerDragged(UiEvent event) {
        if (dragKind == DragKind.NONE || event.getPointer() != dragPointer) {
            return false;
        }
        if (dragKind == DragKind.CONTENT) {
            setScrollPosition(
                    dragStartScrollColumn - (event.getSurfaceColumn() - dragStartSurfaceColumn),
                    dragStartScrollRow - (event.getSurfaceRow() - dragStartSurfaceRow));
            return true;
        }
        dragThumbTo(alongBar(dragBar, event));
        return true;
    }

    private void dragThumbTo(int index) {
        int room = cellsAlong(dragBar) * EIGHTHS - thumbLengthEighths(dragBar);
        if (room <= 0) {
            return;
        }
        int wantedStart = index * EIGHTHS + EIGHTHS / 2 - thumbGrabEighths;
        int clampedStart = Math.max(0, Math.min(wantedStart, room));
        long scroll = Math.round((double) clampedStart * maxScrollAlong(dragBar) / room);
        setScrollAlong(dragBar, (int) scroll);
    }

    private boolean handlePointerUp(UiEvent event) {
        if (event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        return endDrag(event.getPointer());
    }

    private boolean endDrag(int pointer) {
        if (dragKind == DragKind.NONE || pointer != dragPointer) {
            return false;
        }
        dragKind = DragKind.NONE;
        dragPointer = NO_POINTER;
        dragBar = null;
        requestRedraw();
        return true;
    }

    private boolean handleScrolled(UiEvent event) {
        int beforeColumn = scrollColumn;
        int beforeRow = scrollRow;
        int rowCells = event.getScrollStepsY() * rowStepFor(wheelStepCells);
        setScrollPosition(scrollColumn + event.getScrollStepsX() * wheelStepCells,
                scrollRow + rowCells);
        return scrollColumn != beforeColumn || scrollRow != beforeRow;
    }

    private boolean handleKeyDown(int keycode) {
        return switch (keycode) {
            case Input.Keys.PAGE_UP -> scrollPage(-1);
            case Input.Keys.PAGE_DOWN -> scrollPage(1);
            case Input.Keys.HOME -> scrollToEnd(false);
            case Input.Keys.END -> scrollToEnd(true);
            case Input.Keys.UP -> scrollByArrow(0, -1);
            case Input.Keys.DOWN -> scrollByArrow(0, 1);
            case Input.Keys.LEFT -> scrollByArrow(-1, 0);
            case Input.Keys.RIGHT -> scrollByArrow(1, 0);
            default -> false;
        };
    }

    // Vertical first; null when nothing can scroll.
    private BarOrientation primaryAxis() {
        if (getMaxScrollRow() > 0) {
            return BarOrientation.VERTICAL;
        }
        return getMaxScrollColumn() > 0 ? BarOrientation.HORIZONTAL : null;
    }

    private boolean scrollPage(int direction) {
        BarOrientation axis = primaryAxis();
        if (axis == null) {
            return false;
        }
        setScrollAlong(axis, scrollAlong(axis) + direction * pageAlong(axis));
        return true;
    }

    private boolean scrollToEnd(boolean end) {
        BarOrientation axis = primaryAxis();
        if (axis == null) {
            return false;
        }
        setScrollAlong(axis, end ? maxScrollAlong(axis) : 0);
        return true;
    }

    private boolean scrollByArrow(int columnStep, int rowStep) {
        if (!hasKeyboardFocus()) {
            return false;
        }
        boolean canScroll = rowStep != 0 ? getMaxScrollRow() > 0 : getMaxScrollColumn() > 0;
        if (!canScroll) {
            return false;
        }
        setScrollPosition(scrollColumn + columnStep, scrollRow + rowStep * rowStepFor(1));
        return true;
    }
}
