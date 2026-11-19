package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Layout settings of one widget inside a {@link Table}. Instances only come from
 * {@link Table#add(Widget)}; find the cell of a child with {@link Table#getCell(Widget)}.
 *
 * <p>The setters are fluent: each returns this cell so calls can be chained, as in
 * {@code table.add(label).pad(1).expandX().fillX()}. This is a deliberate exception to the
 * command-query rule, and the short names ({@code pad}, {@code expandX}) are deliberate too.
 * Every setter marks the owning table for layout with {@link Widget#invalidate()}. A cell
 * starts with no padding, no expand, no fill, {@link HorizontalAlignment#LEFT},
 * {@link VerticalAlignment#TOP}, and a column span of 1. All sizes are in square UI cells.
 *
 * <p>When the widget is removed from the table (see {@link Table#removeChild(Widget)}), its cell
 * is dead: every setter throws {@link IllegalStateException}, while the getters keep returning
 * the last values. Add the widget again to get a new cell.
 *
 * <p>The expand and fill settings follow the layout rules in {@link Table}. Expand claims extra
 * table space for the cell's column or row; fill makes the widget use all of its cell's area.
 * Neither does anything extra while the table is at or below its preferred size.
 */
public final class TableCell {

    private final Table table;
    private final Widget widget;
    private int padTop;
    private int padLeft;
    private int padBottom;
    private int padRight;
    private boolean expandX;
    private boolean expandY;
    private boolean fillX;
    private boolean fillY;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
    private VerticalAlignment verticalAlignment = VerticalAlignment.TOP;
    private int colspan = 1;
    private int row;
    private int column;
    private boolean removed;

    TableCell(Table table, Widget widget, int row) {
        this.table = table;
        this.widget = widget;
        this.row = row;
    }

    /**
     * Sets the same padding on all four sides.
     *
     * @param all space in UI cells around the widget; zero or greater
     * @return this cell
     * @throws IllegalArgumentException if {@code all} is negative
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell pad(int all) {
        return pad(all, all, all, all);
    }

    /**
     * Sets the padding on each side. Padding is empty space inside the cell, around the widget.
     *
     * @param top space above the widget; zero or greater
     * @param left space left of the widget; zero or greater
     * @param bottom space below the widget; zero or greater
     * @param right space right of the widget; zero or greater
     * @return this cell
     * @throws IllegalArgumentException if any value is negative
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell pad(int top, int left, int bottom, int right) {
        requireActive();
        requireNotNegative(top, "top");
        requireNotNegative(left, "left");
        requireNotNegative(bottom, "bottom");
        requireNotNegative(right, "right");
        padTop = top;
        padLeft = left;
        padBottom = bottom;
        padRight = right;
        table.invalidate();
        return this;
    }

    /**
     * Sets the space above the widget.
     *
     * @param top space in UI cells; zero or greater
     * @return this cell
     * @throws IllegalArgumentException if {@code top} is negative
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell padTop(int top) {
        return pad(top, padLeft, padBottom, padRight);
    }

    /**
     * Sets the space left of the widget.
     *
     * @param left space in UI cells; zero or greater
     * @return this cell
     * @throws IllegalArgumentException if {@code left} is negative
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell padLeft(int left) {
        return pad(padTop, left, padBottom, padRight);
    }

    /**
     * Sets the space below the widget.
     *
     * @param bottom space in UI cells; zero or greater
     * @return this cell
     * @throws IllegalArgumentException if {@code bottom} is negative
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell padBottom(int bottom) {
        return pad(padTop, padLeft, bottom, padRight);
    }

    /**
     * Sets the space right of the widget.
     *
     * @param right space in UI cells; zero or greater
     * @return this cell
     * @throws IllegalArgumentException if {@code right} is negative
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell padRight(int right) {
        return pad(padTop, padLeft, padBottom, right);
    }

    /**
     * Lets the cell's column(s) share the table's extra width. Same as {@code expandX(true)}.
     * See the layout rules in {@link Table}.
     *
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell expandX() {
        return expandX(true);
    }

    /**
     * Sets whether the cell's column(s) share the table's extra width. A column expands when
     * any of its cells does. Nothing extra happens when the table is at or below its preferred
     * width. See the layout rules in {@link Table}.
     *
     * @param expand {@code true} to claim extra width
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell expandX(boolean expand) {
        requireActive();
        expandX = expand;
        table.invalidate();
        return this;
    }

    /**
     * Lets the cell's row share the table's extra height. Same as {@code expandY(true)}. See
     * the layout rules in {@link Table}.
     *
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell expandY() {
        return expandY(true);
    }

    /**
     * Sets whether the cell's row shares the table's extra height. A row expands when any of its
     * cells does. Nothing extra happens when the table is at or below its preferred height. See
     * the layout rules in {@link Table}.
     *
     * @param expand {@code true} to claim extra height
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell expandY(boolean expand) {
        requireActive();
        expandY = expand;
        table.invalidate();
        return this;
    }

    /**
     * Same as calling {@link #expandX()} and {@link #expandY()}.
     *
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell expand() {
        expandX(true);
        return expandY(true);
    }

    /**
     * Makes the widget as wide as the cell's area (the spanned columns minus the padding). Same
     * as {@code fillX(true)}. See the layout rules in {@link Table}.
     *
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell fillX() {
        return fillX(true);
    }

    /**
     * Sets whether the widget is as wide as the cell's area (the spanned columns minus the
     * padding). When {@code false}, the widget keeps its preferred width, cut to the area. The
     * area is not larger than the cell needs unless the table is wider than its preferred width.
     * See the layout rules in {@link Table}.
     *
     * @param fill {@code true} to fill the width
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell fillX(boolean fill) {
        requireActive();
        fillX = fill;
        table.invalidate();
        return this;
    }

    /**
     * Makes the widget as high as the cell's area (the row height minus the padding). Same as
     * {@code fillY(true)}. See the layout rules in {@link Table}.
     *
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell fillY() {
        return fillY(true);
    }

    /**
     * Sets whether the widget is as high as the cell's area (the row height minus the padding).
     * When {@code false}, the widget keeps its preferred height, cut to the area. The area is
     * not larger than the cell needs unless the table is higher than its preferred height. See
     * the layout rules in {@link Table}.
     *
     * @param fill {@code true} to fill the height
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell fillY(boolean fill) {
        requireActive();
        fillY = fill;
        table.invalidate();
        return this;
    }

    /**
     * Same as calling {@link #fillX()} and {@link #fillY()}.
     *
     * @return this cell
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell fill() {
        fillX(true);
        return fillY(true);
    }

    /**
     * Sets where the widget sits horizontally when the cell's area is wider than the widget.
     *
     * @param alignment the horizontal alignment; must not be {@code null}
     * @return this cell
     * @throws NullPointerException if {@code alignment} is {@code null}
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell alignX(HorizontalAlignment alignment) {
        requireActive();
        horizontalAlignment = Objects.requireNonNull(alignment, "alignment");
        table.invalidate();
        return this;
    }

    /**
     * Sets where the widget sits vertically when the cell's area is higher than the widget.
     *
     * @param alignment the vertical alignment; must not be {@code null}
     * @return this cell
     * @throws NullPointerException if {@code alignment} is {@code null}
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell alignY(VerticalAlignment alignment) {
        requireActive();
        verticalAlignment = Objects.requireNonNull(alignment, "alignment");
        table.invalidate();
        return this;
    }

    /**
     * Sets both alignments.
     *
     * @param horizontal the horizontal alignment; must not be {@code null}
     * @param vertical the vertical alignment; must not be {@code null}
     * @return this cell
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell align(HorizontalAlignment horizontal, VerticalAlignment vertical) {
        requireActive();
        Objects.requireNonNull(horizontal, "horizontal");
        Objects.requireNonNull(vertical, "vertical");
        horizontalAlignment = horizontal;
        verticalAlignment = vertical;
        table.invalidate();
        return this;
    }

    /**
     * Sets how many columns the cell covers. The cells after it in the row start after the
     * covered columns.
     *
     * @param columnCount number of columns; 1 or greater
     * @return this cell
     * @throws IllegalArgumentException if {@code columnCount} is less than 1
     * @throws IllegalStateException if the widget was removed from the table
     */
    public TableCell colspan(int columnCount) {
        requireActive();
        if (columnCount < 1) {
            throw new IllegalArgumentException("Colspan must be at least 1, was " + columnCount);
        }
        colspan = columnCount;
        table.invalidate();
        return this;
    }

    /**
     * Returns the widget in this cell.
     *
     * @return the widget, never {@code null}
     */
    public Widget getWidget() {
        return widget;
    }

    /**
     * Returns the space above the widget.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getPaddingTop() {
        return padTop;
    }

    /**
     * Returns the space left of the widget.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getPaddingLeft() {
        return padLeft;
    }

    /**
     * Returns the space below the widget.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getPaddingBottom() {
        return padBottom;
    }

    /**
     * Returns the space right of the widget.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getPaddingRight() {
        return padRight;
    }

    /**
     * Returns whether the cell's column(s) share extra table width.
     *
     * @return {@code true} when expanding horizontally
     */
    public boolean isExpandX() {
        return expandX;
    }

    /**
     * Returns whether the cell's row shares extra table height.
     *
     * @return {@code true} when expanding vertically
     */
    public boolean isExpandY() {
        return expandY;
    }

    /**
     * Returns whether the widget is stretched to the cell's area width.
     *
     * @return {@code true} when filling horizontally
     */
    public boolean isFillX() {
        return fillX;
    }

    /**
     * Returns whether the widget is stretched to the cell's area height.
     *
     * @return {@code true} when filling vertically
     */
    public boolean isFillY() {
        return fillY;
    }

    /**
     * Returns the horizontal alignment.
     *
     * @return the alignment, never {@code null}
     */
    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    /**
     * Returns the vertical alignment.
     *
     * @return the alignment, never {@code null}
     */
    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    /**
     * Returns the number of columns the cell covers.
     *
     * @return the column span, 1 or greater
     */
    public int getColspan() {
        return colspan;
    }

    int getRow() {
        return row;
    }

    void setRow(int row) {
        this.row = row;
    }

    int getColumn() {
        return column;
    }

    void setColumn(int column) {
        this.column = column;
    }

    void markRemoved() {
        removed = true;
    }

    int getHorizontalPadding() {
        return padLeft + padRight;
    }

    private void requireActive() {
        if (removed) {
            throw new IllegalStateException("The widget was removed from the table");
        }
    }

    private static void requireNotNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "Padding " + name + " must not be negative, was " + value);
        }
    }
}
