package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Rows grouped under headings with a cursor that rests on choosable rows only, and a scrolling window for when
 * the dialog has no room for every row. Each row has a parallel entry; a null entry marks a row that cannot be chosen.
 *
 * @param <T> what a choosable row stands for
 */
public final class GroupedPickList<T> {

    private static final int MARKER_CELLS = 2;
    private static final int NOTHING = -1;

    private List<SelectList.Row> rows = List.of();
    // Parallel to rows: null where the row cannot be chosen.
    private List<T> entries = List.of();
    private int selected = NOTHING;
    // Rows the dialog has room for; unlimited until the caller reports the area.
    private int capacity = Integer.MAX_VALUE;
    private final ScrollState window = new ScrollState(0);

    /** Creates an empty list with no selection. */
    public GroupedPickList() {
    }

    /**
     * Replaces the rows and entries, clears the selection, and scrolls back to the top. Call {@link #move(int)}
     * afterwards to put the cursor on a row.
     *
     * @throws IllegalArgumentException when rows and entries differ in size
     */
    public void setRows(List<SelectList.Row> newRows, List<T> newEntries) {
        if (newRows.size() != newEntries.size()) {
            throw new IllegalArgumentException(
                "rows and entries must be the same size: " + newRows.size() + " vs " + newEntries.size());
        }
        rows = new ArrayList<>(newRows);
        entries = new ArrayList<>(newEntries);
        selected = NOTHING;
        window.setTotalRows(rows.size());
        window.setVisibleRows(shownRows());
        window.toTop();
    }

    /** Empties the list, drops the selection, and lifts the capacity limit. */
    public void clear() {
        rows = List.of();
        entries = List.of();
        selected = NOTHING;
        capacity = Integer.MAX_VALUE;
        window.setTotalRows(0);
    }

    /** Sets how many rows the dialog has room for; the window follows the cursor when this changes. */
    public void setCapacity(int roomForRows) {
        if (roomForRows == capacity) {
            return;
        }
        capacity = roomForRows;
        window.setVisibleRows(shownRows());
        followCursor();
    }

    /** Rows drawn: all of them when they fit. */
    public int shownRows() {
        return Math.min(rows.size(), capacity);
    }

    /** Whether some rows are out of view. */
    public boolean scrolls() {
        return window.needed();
    }

    /** The scroll window over the rows. */
    public ScrollState window() {
        return window;
    }

    /** The rows currently in view, top first. */
    public List<SelectList.Row> visibleRows() {
        int first = window.offset();
        return rows.subList(first, Math.min(rows.size(), first + shownRows()));
    }

    /** The cursor's place in {@link #visibleRows()}, or -1 when it has been scrolled out of view. */
    public int visibleSelectedIndex() {
        int inView = selected - window.offset();
        return selected == NOTHING || inView < 0 || inView >= shownRows() ? NOTHING : inView;
    }

    /** Scrolls the window by whole rows; the cursor stays where it is. */
    public void wheel(int ticks) {
        window.scrollBy(ticks);
    }

    /** Moves the cursor to a row of {@link #rows()}; unchoosable and out-of-range rows are ignored. */
    public void hover(int rowIndex) {
        if (isChoosable(rowIndex)) {
            selected = rowIndex;
        }
    }

    /** Whether the row is in range and has an entry. */
    public boolean isChoosable(int rowIndex) {
        return rowIndex >= 0 && rowIndex < entries.size() && entries.get(rowIndex) != null;
    }

    /** All rows, in order. */
    public List<SelectList.Row> rows() {
        return rows;
    }

    /** The cursor's row index, or -1 when there is none. */
    public int selectedIndex() {
        return selected;
    }

    /** The entry under the cursor, or null when there is none. */
    public T selected() {
        return selected == NOTHING ? null : entries.get(selected);
    }

    /** Whether the cursor is on a row. */
    public boolean hasSelection() {
        return selected != NOTHING;
    }

    /** Moves to the next choosable row in the direction of {@code step}; stays put at either end. */
    public void move(int step) {
        int direction = Integer.signum(step);
        if (direction == 0) {
            return;
        }
        for (int i = selected == NOTHING && direction > 0 ? 0 : selected + direction;
             i >= 0 && i < entries.size(); i += direction) {
            if (entries.get(i) != null) {
                selected = i;
                followCursor();
                return;
            }
        }
    }

    /** Width in cells of the widest row, counting the selection marker on selectable rows. */
    public int listWidth() {
        int width = 0;
        for (SelectList.Row row : rows) {
            width = Math.max(width, row.text().length() + (row.selectable() ? MARKER_CELLS : 0));
        }
        return width;
    }

    // Keeps the cursor in view, and its group heading too when the heading sits right above it.
    private void followCursor() {
        if (selected == NOTHING) {
            return;
        }
        boolean headingAbove = selected > 0 && isHeading(selected - 1);
        window.keepVisible(headingAbove ? selected - 1 : selected);
        window.keepVisible(selected);
    }

    private boolean isHeading(int rowIndex) {
        return entries.get(rowIndex) == null && !rows.get(rowIndex).dimmed();
    }
}
