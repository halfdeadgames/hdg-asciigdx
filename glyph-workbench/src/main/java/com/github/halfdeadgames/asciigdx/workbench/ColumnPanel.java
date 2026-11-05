package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import java.util.Arrays;

/** Draws the candidate columns side by side and tells which column is under a pixel. */
final class ColumnPanel {

    static final int UNIT = 16;
    static final int MAX_SCALE = 8;
    /** Columns wrap into a second row only when that keeps the big view larger than a single row would. */
    static final int MIN_SCALE = 4;
    static final int ROW_GAP = 12;
    static final int BORDER = Painter.TEXT_SIZE;
    /** Distance between neighbouring contents: one border cell, shared by both frames. */
    static final int GAP = BORDER;
    private static final int TILE_ZOOM = 2;
    private static final int TILE_COUNT = 3;
    private static final int SECTION_GAP = 8;
    private static final int TILE_BLOCK = UNIT * TILE_ZOOM * TILE_COUNT;

    private final Painter painter;
    private final Color fill;
    private final Color labelColor;
    private final Color numberColor;
    private final Color frameColor;
    private final Array<GlyphColumn> columns = new Array<>();
    private int[] left = new int[0];
    private int[] width = new int[0];
    private int hovered = -1;
    private int picked = -1;
    private Color hoverColor;
    private Color pickedColor;
    private int viewed = -1;
    private Color viewedColor;
    private int maxOuterHeight =Integer.MAX_VALUE;
    private int bigSize = UNIT * MAX_SCALE;
    private int[] columnTop = new int[0];
    private int rowCount = 1;
    private int outerHeight;

    ColumnPanel(Painter painter, Color fill, Color labelColor, Color numberColor, Color frameColor) {
        this.frameColor = frameColor;
        this.painter = painter;
        this.fill = fill;
        this.labelColor = labelColor;
        this.numberColor = numberColor;
    }

    void setColumns(Array<GlyphColumn> newColumns) {
        columns.clear();
        columns.addAll(newColumns);
    }

    /** Column 0 is the original and never highlights; pass -1 for no hover or no pick. */
    void setHighlights(int hovered, int picked, Color hoverColor, Color pickedColor) {
        this.hovered = hovered;
        this.picked = picked;
        this.hoverColor = hoverColor;
        this.pickedColor = pickedColor;
    }

    /** Marks the column shown in the context area with a thin bar above its big glyph. */
    void setViewed(int viewed, Color viewedColor) {
        this.viewed = viewed;
        this.viewedColor = viewedColor;
    }

    private Color frameFor(int column) {
        if (column > 0 && column == hovered) {
            return hoverColor;
        }
        if (column > 0 && column == picked) {
            return pickedColor;
        }
        return frameColor;
    }

    int count() {
        return columns.size;
    }

    GlyphColumn column(int index) {
        return columns.get(index);
    }

    /**
     * Largest scale of the big view, from {@link #MAX_SCALE} down to 1, at which the columns
     * still fit. Text and the tiled block never shrink, only the big view does.
     */
    static int fitScale(int[] labelWidths, int gap, int available) {
        for (int scale = MAX_SCALE; scale > 1; scale--) {
            if (totalWidth(labelWidths, gap, scale) <= available) {
                return scale;
            }
        }
        return 1;
    }

    private static int totalWidth(int[] labelWidths, int gap, int scale) {
        int total = gap * Math.max(0, labelWidths.length - 1);
        for (int labelWidth : labelWidths) {
            total += columnWidth(labelWidth, scale);
        }
        return total;
    }

    /** How the columns are arranged: columns per row, number of rows, and the big view scale. */
    record Plan(int perRow, int rows, int scale) {
    }

    /**
     * One row unless that leaves the big view below {@link #MIN_SCALE} and two rows give a larger
     * big view. Both the width and the height budget are respected, so a wrap never costs more
     * than it gains.
     */
    static Plan plan(int[] labelWidths, int gap, int usableWidth, int maxOuterHeight) {
        int count = labelWidths.length;
        int single = Math.min(fitScaleForHeight(maxOuterHeight), fitScale(labelWidths, gap, usableWidth));
        if (count <= 1 || single >= MIN_SCALE) {
            return new Plan(Math.max(1, count), 1, single);
        }
        int perRow = (count + 1) / 2;
        int scale = fitScaleForHeight((maxOuterHeight - ROW_GAP) / 2);
        for (int first = 0; first < count; first += perRow) {
            int[] rowWidths = Arrays.copyOfRange(labelWidths, first, Math.min(count, first + perRow));
            scale = Math.min(scale, fitScale(rowWidths, gap, usableWidth));
        }
        if (scale > single) {
            return new Plan(perRow, 2, scale);
        }
        if (totalWidth(labelWidths, gap, 1) <= usableWidth) {
            return new Plan(count, 1, single);
        }
        return wrapToWidth(labelWidths, gap, usableWidth, maxOuterHeight);
    }

    // Even at scale 1 one row is too wide, so use the fewest rows whose widest row fits.
    private static Plan wrapToWidth(int[] labelWidths, int gap, int usableWidth, int maxOuterHeight) {
        int count = labelWidths.length;
        for (int rows = 3; rows <= count; rows++) {
            int perRow = (count + rows - 1) / rows;
            boolean fits = true;
            for (int first = 0; first < count && fits; first += perRow) {
                int[] rowWidths = Arrays.copyOfRange(labelWidths, first, Math.min(count, first + perRow));
                fits = totalWidth(rowWidths, gap, 1) <= usableWidth;
            }
            if (fits || rows == count) {
                int actualRows = (count + perRow - 1) / perRow;
                int scale = fitScaleForHeight((maxOuterHeight - ROW_GAP * (actualRows - 1)) / actualRows);
                return new Plan(perRow, actualRows, scale);
            }
        }
        return new Plan(count, 1, 1);
    }

    /** Outer width of a row of framed columns that share their neighbouring borders. */
    static int framedWidth(int[] labelWidths, int gap, int scale) {
        return totalWidth(labelWidths, gap, scale) + 2 * BORDER;
    }

    /** Outer height of all rows of a plan. */
    static int framedHeight(Plan plan) {
        return plan.rows() * (contentHeight(plan.scale()) + 2 * BORDER) + (plan.rows() - 1) * ROW_GAP;
    }

    /** Largest scale whose framed height fits, down to 1. */
    static int fitScaleForHeight(int availableOuterHeight) {
        for (int scale = MAX_SCALE; scale > 1; scale--) {
            if (contentHeight(scale) + 2 * BORDER <= availableOuterHeight) {
                return scale;
            }
        }
        return 1;
    }

    void setMaxOuterHeight(int height) {
        maxOuterHeight = height;
    }

    // Whole text cells, so the border glyphs line up with the content.
    private static int columnWidth(int labelWidth, int scale) {
        return Painter.roundUpToCells(Math.max(labelWidth, Math.max(UNIT * scale, TILE_BLOCK)));
    }

    /** Height inside the border: two text lines, the big view, the 1x and 2x pair, and the tiles. */
    static int contentHeight(int scale) {
        int lines = Painter.TEXT_SIZE * 2 + SECTION_GAP;
        return Painter.roundUpToCells(lines + UNIT * scale + SECTION_GAP + UNIT * 2 + SECTION_GAP + TILE_BLOCK);
    }

    /**
     * Draws all framed columns for one slot starting at the given spot and returns the next free
     * row. {@code available} is the width from {@code x} to the right edge.
     */
    int draw(int x, int startTop, int available, int slot) {
        layout(x, startTop, available);
        // Neighbouring frames overlap on one border cell; highlighted frames go last so their colour wins.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < columns.size; i++) {
                if ((frameFor(i) != frameColor) == (pass == 1)) {
                    int outerWidth = width[i] + 2 * BORDER;
                    painter.frame(
                        frameFor(i), left[i] - BORDER, columnTop[i], outerWidth / BORDER, outerHeight / BORDER);
                }
            }
        }
        for (int i = 0; i < columns.size; i++) {
            drawColumn(columns.get(i), left[i], columnTop[i] + BORDER, slot, i == viewed ? width[i] : 0);
        }
        return startTop + rowCount * outerHeight + (rowCount - 1) * ROW_GAP;
    }

    private void layout(int x, int startTop, int available) {
        int count = columns.size;
        int[] labelWidths = new int[count];
        for (int i = 0; i < count; i++) {
            labelWidths[i] = columns.get(i).labelWidth();
        }
        int usable = available - 2 * BORDER;
        Plan plan = plan(labelWidths, GAP, usable, maxOuterHeight);
        int perRow = plan.perRow();
        int scale = plan.scale();
        rowCount = plan.rows();
        bigSize = UNIT * scale;
        outerHeight = contentHeight(scale) + 2 * BORDER;
        left = new int[count];
        width = new int[count];
        columnTop = new int[count];
        int cursor = x + BORDER;
        for (int i = 0; i < count; i++) {
            if (i % perRow == 0) {
                cursor = x + BORDER;
            }
            left[i] = cursor;
            width[i] = columnWidth(labelWidths[i], scale);
            columnTop[i] = startTop + (i / perRow) * (outerHeight + ROW_GAP);
            cursor += width[i] + GAP;
        }
    }

    private int drawColumn(GlyphColumn column, int x, int startTop, int slot, int viewBarWidth) {
        int rowTop = startTop;
        painter.text(column.number, x, rowTop, numberColor);
        rowTop += Painter.TEXT_SIZE;
        painter.text(column.label, x, rowTop, column.labelColor != null ? column.labelColor : labelColor);
        rowTop += Painter.TEXT_SIZE + SECTION_GAP;
        if (viewBarWidth > 0) {
            painter.fill(viewedColor, x, rowTop - SECTION_GAP + 2, viewBarWidth, 4);
        }

        painter.fill(fill, x, rowTop, bigSize, bigSize);
        painter.region(column.regions[slot], x, rowTop, bigSize, Color.WHITE);
        rowTop += bigSize + SECTION_GAP;

        int doubled = UNIT * 2;
        painter.fill(fill, x, rowTop, UNIT + SECTION_GAP + doubled, doubled);
        painter.region(column.regions[slot], x, rowTop, UNIT, Color.WHITE);
        painter.region(column.regions[slot], x + UNIT + SECTION_GAP, rowTop, doubled, Color.WHITE);
        rowTop += doubled + SECTION_GAP;

        painter.fill(fill, x, rowTop, TILE_BLOCK, TILE_BLOCK);
        for (int row = 0; row < TILE_COUNT; row++) {
            for (int col = 0; col < TILE_COUNT; col++) {
                painter.region(
                    column.regions[slot], x + col * doubled, rowTop + row * doubled, doubled, Color.WHITE);
            }
        }
        return rowTop + TILE_BLOCK;
    }

    /** Returns the column under a window pixel from the last draw, or -1. */
    int columnAt(int pixelX, int pixelY) {
        for (int i = 0; i < left.length; i++) {
            if (pixelY >= columnTop[i] && pixelY < columnTop[i] + outerHeight
                && pixelX >= left[i] - BORDER / 2 && pixelX < left[i] + width[i] + BORDER / 2) {
                return i;
            }
        }
        return -1;
    }
}
