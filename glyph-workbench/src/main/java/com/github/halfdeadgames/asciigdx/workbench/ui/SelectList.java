package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;
import java.util.List;

/**
 * Rows on the cell grid, two cells apart. The selected row gets a full-width highlight bar and a
 * "►"; headings use the title colour and dimmed rows are greyed. Selection itself is kept by the caller.
 */
public final class SelectList {

    /** A line to show; {@code selectable} false with {@code dimmed} false makes it a heading. */
    public record Row(String text, boolean selectable, boolean dimmed, int indent) {
    }

    public static final int PITCH = 2;
    private static final int INDENT_CELLS = 2;

    private SelectList() {
    }

    /** Row index under a cell row, for a list starting at {@code topRow}; -1 outside. Both cells of a row count. */
    public static int rowAt(int cellRow, int topRow, int rowCount) {
        int offset = cellRow - topRow;
        if (offset < 0 || offset >= rowCount * PITCH) {
            return -1;
        }
        return offset / PITCH;
    }

    /** Whether the row under the pointer can be chosen. */
    public static boolean isSelectable(List<Row> rows, int index) {
        return index >= 0 && index < rows.size() && rows.get(index).selectable();
    }

    /** The cells a list of {@code rowCount} rows covers, from its first text line to its last. */
    public static CellRect areaOf(int x, int topRow, int widthCells, int rowCount) {
        return new CellRect(x, topRow, widthCells, Math.max(0, rowCount * PITCH - 1));
    }

    /** @param selectedRow row index to highlight, or -1 */
    public static void draw(Painter painter, CellRect area, List<Row> rows, int selectedRow) {
        int x = area.x();
        int topRow = area.y();
        int widthCells = area.width();
        int cell = painter.cell();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = (topRow + i * PITCH) * cell;
            int rowX = (x + row.indent() * INDENT_CELLS) * cell;
            if (i == selectedRow) {
                painter.fill(UiPalette.KEYBG, x * cell, y, widthCells * cell, cell);
                painter.text("► " + row.text(), rowX, y, UiPalette.ACCENT);
                continue;
            }
            Color color = row.dimmed() ? UiPalette.DIM : row.selectable() ? UiPalette.TEXT : UiPalette.TITLE;
            painter.text(row.selectable() ? "  " + row.text() : row.text(), rowX, y, color);
        }
    }
}
