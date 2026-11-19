package com.github.halfdeadgames.asciigdx.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * A group that arranges its children in rows and columns. Add a widget with
 * {@link #add(Widget)}, tune its {@link TableCell}, and end each row with {@link #row()}:
 *
 * <pre>{@code
 * Table table = new Table();
 * table.setPadding(1);
 * table.add(new Label("Name", style)).alignX(HorizontalAlignment.RIGHT);
 * table.add(new Label("Ada", style)).expandX().fillX();
 * table.row();
 * table.add(new Label("Title", tallStyle)).colspan(2).alignX(HorizontalAlignment.CENTER);
 * table.setSize(40, 12);
 * table.validate();
 * }</pre>
 *
 * <p>All sizes and positions are in square UI cells, with rows growing downward. Child positions
 * and sizes are set during layout, relative to the table, so do not set them yourself.
 *
 * <p>Layout rules (run by {@link #validate()} when the table is marked for layout):
 * <ol>
 *   <li>A column is as wide as its widest cell, where a cell needs its widget's preferred width
 *       plus its left and right padding. A cell that spans several columns is first compared with
 *       the total of the columns it covers; any shortfall is added to those columns in equal
 *       shares, with the remainder one cell each to the leftmost columns.
 *   <li>A row is as high as its highest cell, where a cell needs its widget's preferred height
 *       plus its top and bottom padding.
 *   <li>Tall snapping: a row that holds a widget whose {@link Widget#getRowSnap()} is {@code s}
 *       greater than 1 starts on a row of the table (counted from the table's top edge) that is a
 *       multiple of {@code s}. Empty gap rows are inserted above it when needed. Gap rows count
 *       in the preferred height. Place the table itself on a multiple of its own
 *       {@link #getRowSnap()}. A widget's top is also rounded up to such a multiple inside its
 *       row, and its cell's top padding is rounded up the same way when the row is measured.
 *   <li>If the table is wider than its preferred width, the extra width is shared equally among
 *       the columns that hold at least one cell with {@link TableCell#expandX()} (a spanning cell
 *       counts for every column it covers), with the remainder one cell each to the leftmost
 *       such columns. The same applies to height and {@link TableCell#expandY()}. Without any
 *       expanding column or row, the extra stays at the right or bottom. Heights are shared
 *       before the snapping gaps are measured again, so a gap may change after growth.
 *   <li>If the table is smaller than its preferred size, nothing shrinks and the content may
 *       extend past the table.
 *   <li>A cell's area is its covered columns by its row, minus the cell padding. On each axis a
 *       filling widget takes the whole area; any other widget gets the smaller of its preferred
 *       size and the area. The widget is then placed in the area by the cell's alignment, where
 *       center puts the leftover half, rounded down, before the widget.
 * </ol>
 *
 * <p>Layout does not allocate after the internal arrays have grown to fit the cell count.
 */
public class Table extends WidgetGroup {

    private final ArrayList<TableCell> cells = new ArrayList<>();
    private int currentRow;
    private int paddingTop;
    private int paddingLeft;
    private int paddingBottom;
    private int paddingRight;

    private int[] columnWidths = new int[0];
    private int[] columnLefts = new int[0];
    private boolean[] columnExpands = new boolean[0];
    private int[] rowHeights = new int[0];
    private int[] rowTops = new int[0];
    private int[] rowSnaps = new int[0];
    private boolean[] rowExpands = new boolean[0];
    private int measuredColumnCount;
    private int measuredRowCount;
    private int measuredBottom;

    /** Creates an empty table with no padding, at position (0, 0), with size 0 x 0. */
    public Table() {
    }

    /**
     * Adds a widget as a child, in a new cell at the end of the current row. Calls
     * {@link #invalidate()} on this table. Each widget has exactly one cell. A widget that
     * belongs to another parent, including another table, must be removed there first. The
     * cell's row and column come from the order of {@code add} and {@link #row()} calls, and a
     * {@link TableCell#colspan(int) colspan} shifts the later cells in its row to the right.
     *
     * @param widget widget to add; must not be {@code null}
     * @return the new cell, for chaining settings
     * @throws NullPointerException if {@code widget} is {@code null}
     * @throws IllegalArgumentException if {@code widget} already has a parent, or is this table
     *         or one of its ancestors
     */
    public TableCell add(Widget widget) {
        super.addChild(widget);
        TableCell cell = new TableCell(this, widget, currentRow);
        cells.add(cell);
        return cell;
    }

    /**
     * Adds a widget in a new cell at the end of the current row, like {@link #add(Widget)} but
     * without returning the cell.
     *
     * @param child widget to add; must not be {@code null}
     * @throws NullPointerException if {@code child} is {@code null}
     * @throws IllegalArgumentException if {@code child} already has a parent, or is this table or
     *         one of its ancestors
     */
    @Override
    public void addChild(Widget child) {
        add(child);
    }

    /**
     * Removes a child and its cell. Later cells in the same row shift left. A row left empty
     * disappears, and the rows below move up. Calls {@link #invalidate()} on this table. The
     * widget's {@link TableCell} becomes dead: its setters throw
     * {@link IllegalStateException}, while its getters keep working.
     *
     * @param child widget to remove; must not be {@code null}
     * @throws NullPointerException if {@code child} is {@code null}
     * @throws IllegalArgumentException if {@code child} is not a child of this table
     */
    @Override
    public void removeChild(Widget child) {
        super.removeChild(child);
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).getWidget() == child) {
                removeCellAt(i);
                return;
            }
        }
    }

    /**
     * Returns the cell of a child, for changing its layout settings later.
     *
     * @param widget a child of this table; must not be {@code null}
     * @return the cell that holds {@code widget}
     * @throws NullPointerException if {@code widget} is {@code null}
     * @throws IllegalArgumentException if {@code widget} is not a child of this table
     */
    public TableCell getCell(Widget widget) {
        Objects.requireNonNull(widget, "widget");
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).getWidget() == widget) {
                return cells.get(i);
            }
        }
        throw new IllegalArgumentException("Widget is not a child of this table");
    }

    private void removeCellAt(int index) {
        TableCell removedCell = cells.remove(index);
        removedCell.markRemoved();
        int removedRow = removedCell.getRow();
        if (isRowOccupied(removedRow)) {
            return;
        }
        for (int i = 0; i < cells.size(); i++) {
            TableCell cell = cells.get(i);
            if (cell.getRow() > removedRow) {
                cell.setRow(cell.getRow() - 1);
            }
        }
        if (currentRow > removedRow) {
            currentRow--;
        }
    }

    private boolean isRowOccupied(int row) {
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).getRow() == row) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ends the current row, so the next {@link #add(Widget)} starts a new row. Does nothing when
     * the current row has no cells. Marks no layout, because the row count only changes when a
     * widget is added.
     */
    public void row() {
        if (!cells.isEmpty() && cells.get(cells.size() - 1).getRow() == currentRow) {
            currentRow++;
        }
    }

    /**
     * Sets the space between the table's edges and its content, and marks this table for layout
     * with {@link #invalidate()}.
     *
     * @param top space above the first row, in UI cells; zero or greater
     * @param left space left of the first column; zero or greater
     * @param bottom space below the last row; zero or greater
     * @param right space right of the last column; zero or greater
     * @throws IllegalArgumentException if any value is negative
     */
    public void setPadding(int top, int left, int bottom, int right) {
        requireNotNegative(top, "top");
        requireNotNegative(left, "left");
        requireNotNegative(bottom, "bottom");
        requireNotNegative(right, "right");
        paddingTop = top;
        paddingLeft = left;
        paddingBottom = bottom;
        paddingRight = right;
        invalidate();
    }

    /**
     * Sets the same table padding on all four sides, like
     * {@link #setPadding(int, int, int, int)}.
     *
     * @param all space in UI cells; zero or greater
     * @throws IllegalArgumentException if {@code all} is negative
     */
    public void setPadding(int all) {
        setPadding(all, all, all, all);
    }

    /**
     * Returns the space above the first row.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getPaddingTop() {
        return paddingTop;
    }

    /**
     * Returns the space left of the first column.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getPaddingLeft() {
        return paddingLeft;
    }

    /**
     * Returns the space below the last row.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getPaddingBottom() {
        return paddingBottom;
    }

    /**
     * Returns the space right of the last column.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getPaddingRight() {
        return paddingRight;
    }

    /**
     * Returns the number of rows that hold at least one cell.
     *
     * @return the row count, zero or greater
     */
    public int getRowCount() {
        return cells.isEmpty() ? 0 : cells.get(cells.size() - 1).getRow() + 1;
    }

    /**
     * Returns the number of columns: the width of the widest row, counting column spans.
     *
     * @return the column count, zero or greater
     */
    public int getColumnCount() {
        return assignColumns();
    }

    /**
     * Returns the width the content needs: the column widths plus the table's left and right
     * padding. See the class description for how the column widths are found.
     *
     * @return the preferred width in UI cells, zero or greater
     */
    @Override
    public int getPreferredWidth() {
        measureColumns();
        return paddingLeft + paddingRight + sum(columnWidths, measuredColumnCount);
    }

    /**
     * Returns the height the content needs: the row heights, including the gap rows that tall
     * snapping adds, plus the table's top and bottom padding.
     *
     * @return the preferred height in UI cells, zero or greater
     */
    @Override
    public int getPreferredHeight() {
        measureRows();
        return measuredBottom + paddingBottom;
    }

    /**
     * Returns the largest {@link Widget#getRowSnap()} of the children, or 1 when there are none.
     * The table's owner should place the table on a row that is a multiple of this value, so
     * that tall children stay aligned. If the owner places the table off such a multiple, tall
     * children land off the tall grid, and the surface snaps them up, so they overlap the row
     * above.
     *
     * <p>The value is live: it is computed from the current children on every call, which is
     * cheap, and it may be called at any time, including before {@link #validate()}.
     *
     * @return the row multiple, 1 or greater
     */
    @Override
    public int getRowSnap() {
        int snap = 1;
        for (int i = 0; i < getChildCount(); i++) {
            snap = Math.max(snap, getChild(i).getRowSnap());
        }
        return snap;
    }

    /**
     * Places and sizes every child by the rules in the class description. Called by
     * {@link #validate()}; do not call it directly.
     */
    @Override
    protected void layout() {
        measureColumns();
        measureRows();
        int extraWidth = getWidth() - paddingLeft - paddingRight
                - sum(columnWidths, measuredColumnCount);
        distribute(columnWidths, columnExpands, 0, measuredColumnCount, extraWidth);
        int extraHeight = getHeight() - measuredBottom - paddingBottom;
        if (extraHeight > 0) {
            distribute(rowHeights, rowExpands, 0, measuredRowCount, extraHeight);
            measureRowTops();
        }
        measureColumnLefts();
        for (int i = 0; i < cells.size(); i++) {
            placeCell(cells.get(i));
        }
    }

    private void placeCell(TableCell cell) {
        Widget widget = cell.getWidget();
        int snap = widget.getRowSnap();
        int areaLeft = columnLefts[cell.getColumn()] + cell.getPaddingLeft();
        int areaWidth = Math.max(0, sum(columnWidths, cell.getColumn(), cell.getColspan())
                - cell.getHorizontalPadding());
        int areaTop = roundUp(rowTops[cell.getRow()] + cell.getPaddingTop(), snap);
        int areaBottom = rowTops[cell.getRow()] + rowHeights[cell.getRow()]
                - cell.getPaddingBottom();
        int areaHeight = Math.max(0, areaBottom - areaTop);
        int width = cell.isFillX() ? areaWidth : Math.min(widget.getPreferredWidth(), areaWidth);
        int height = cell.isFillY() ? areaHeight
                : Math.min(widget.getPreferredHeight(), areaHeight);
        int left = areaLeft + horizontalOffset(cell.getHorizontalAlignment(), areaWidth - width);
        int top = areaTop
                + verticalOffset(cell.getVerticalAlignment(), areaHeight - height, snap);
        widget.setSize(width, height);
        widget.setPosition(left, top);
    }

    private static int horizontalOffset(HorizontalAlignment alignment, int spare) {
        return switch (alignment) {
            case LEFT -> 0;
            case CENTER -> spare / 2;
            case RIGHT -> spare;
        };
    }

    /** Rounds down to a snap multiple so the widget's bottom stays inside its row. */
    private static int verticalOffset(VerticalAlignment alignment, int spare, int snap) {
        return switch (alignment) {
            case TOP -> 0;
            case CENTER -> spare / 2 / snap * snap;
            case BOTTOM -> spare / snap * snap;
        };
    }

    /** Sets each cell's first column and returns the column count. */
    private int assignColumns() {
        int widest = 0;
        int column = 0;
        int previousRow = -1;
        for (int i = 0; i < cells.size(); i++) {
            TableCell cell = cells.get(i);
            if (cell.getRow() != previousRow) {
                previousRow = cell.getRow();
                column = 0;
            }
            cell.setColumn(column);
            column += cell.getColspan();
            widest = Math.max(widest, column);
        }
        return widest;
    }

    private void measureColumns() {
        measuredColumnCount = assignColumns();
        if (columnWidths.length < measuredColumnCount) {
            columnWidths = new int[measuredColumnCount];
            columnLefts = new int[measuredColumnCount];
            columnExpands = new boolean[measuredColumnCount];
        }
        Arrays.fill(columnWidths, 0, measuredColumnCount, 0);
        Arrays.fill(columnExpands, 0, measuredColumnCount, false);
        for (int i = 0; i < cells.size(); i++) {
            TableCell cell = cells.get(i);
            if (cell.getColspan() == 1) {
                int need = cell.getWidget().getPreferredWidth() + cell.getHorizontalPadding();
                columnWidths[cell.getColumn()] = Math.max(columnWidths[cell.getColumn()], need);
            }
            if (cell.isExpandX()) {
                Arrays.fill(columnExpands, cell.getColumn(),
                        cell.getColumn() + cell.getColspan(), true);
            }
        }
        for (int i = 0; i < cells.size(); i++) {
            spreadSpanShortfall(cells.get(i));
        }
    }

    private void spreadSpanShortfall(TableCell cell) {
        if (cell.getColspan() == 1) {
            return;
        }
        int need = cell.getWidget().getPreferredWidth() + cell.getHorizontalPadding();
        int shortfall = need - sum(columnWidths, cell.getColumn(), cell.getColspan());
        distribute(columnWidths, null, cell.getColumn(), cell.getColspan(), shortfall);
    }

    private void measureRows() {
        measuredRowCount = getRowCount();
        if (rowHeights.length < measuredRowCount) {
            rowHeights = new int[measuredRowCount];
            rowTops = new int[measuredRowCount];
            rowSnaps = new int[measuredRowCount];
            rowExpands = new boolean[measuredRowCount];
        }
        Arrays.fill(rowHeights, 0, measuredRowCount, 0);
        Arrays.fill(rowSnaps, 0, measuredRowCount, 1);
        Arrays.fill(rowExpands, 0, measuredRowCount, false);
        for (int i = 0; i < cells.size(); i++) {
            measureCellRow(cells.get(i));
        }
        measureRowTops();
    }

    private void measureCellRow(TableCell cell) {
        Widget widget = cell.getWidget();
        int snap = widget.getRowSnap();
        int need = roundUp(cell.getPaddingTop(), snap) + widget.getPreferredHeight()
                + cell.getPaddingBottom();
        int row = cell.getRow();
        rowHeights[row] = Math.max(rowHeights[row], need);
        rowSnaps[row] = leastCommonMultiple(rowSnaps[row], snap);
        rowExpands[row] |= cell.isExpandY();
    }

    private void measureRowTops() {
        int cursor = paddingTop;
        for (int row = 0; row < measuredRowCount; row++) {
            cursor = roundUp(cursor, rowSnaps[row]);
            rowTops[row] = cursor;
            cursor += rowHeights[row];
        }
        measuredBottom = cursor;
    }

    private void measureColumnLefts() {
        int cursor = paddingLeft;
        for (int column = 0; column < measuredColumnCount; column++) {
            columnLefts[column] = cursor;
            cursor += columnWidths[column];
        }
    }

    /**
     * Adds {@code amount} to the sizes in the range, in equal shares among the eligible entries
     * (all when {@code eligible} is null), the remainder one each to the leftmost.
     */
    private static void distribute(int[] sizes, boolean[] eligible, int from, int count,
            int amount) {
        if (amount <= 0) {
            return;
        }
        int eligibleCount = 0;
        for (int i = from; i < from + count; i++) {
            if (eligible == null || eligible[i]) {
                eligibleCount++;
            }
        }
        if (eligibleCount == 0) {
            return;
        }
        int share = amount / eligibleCount;
        int remainder = amount % eligibleCount;
        for (int i = from; i < from + count; i++) {
            if (eligible == null || eligible[i]) {
                sizes[i] += share + (remainder > 0 ? 1 : 0);
                remainder--;
            }
        }
    }

    private static int sum(int[] values, int count) {
        return sum(values, 0, count);
    }

    private static int sum(int[] values, int from, int count) {
        int total = 0;
        for (int i = from; i < from + count; i++) {
            total += values[i];
        }
        return total;
    }

    private static int roundUp(int value, int multiple) {
        return (value + multiple - 1) / multiple * multiple;
    }

    private static int leastCommonMultiple(int first, int second) {
        int a = first;
        int b = second;
        while (b != 0) {
            int rest = a % b;
            a = b;
            b = rest;
        }
        return first / a * second;
    }

    private static void requireNotNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "Padding " + name + " must not be negative, was " + value);
        }
    }

}
