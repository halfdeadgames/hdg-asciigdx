package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.Optional;

/**
 * The blank margins around the ink of a grid.
 *
 * @param left blank columns before the first inked column
 * @param right blank columns after the last inked column
 * @param top blank rows above the first inked row
 * @param bottom blank rows below the last inked row
 */
public record InkBounds(int left, int right, int top, int bottom) {

    /**
     * Measures the margins of a grid.
     *
     * @param grid the grid to measure
     * @return the margins, or empty if the grid has no ink
     */
    public static Optional<InkBounds> measure(PixelGrid grid) {
        if (!grid.hasLitPixel()) {
            return Optional.empty();
        }
        int firstColumn = firstInkedColumn(grid);
        int lastColumn = lastInkedColumn(grid);
        int firstRow = firstInkedRow(grid);
        int lastRow = lastInkedRow(grid);
        return Optional.of(new InkBounds(
            firstColumn,
            grid.getWidth() - 1 - lastColumn,
            firstRow,
            grid.getHeight() - 1 - lastRow));
    }

    private static int firstInkedColumn(PixelGrid grid) {
        int column = 0;
        while (!columnHasInk(grid, column)) {
            column++;
        }
        return column;
    }

    private static int lastInkedColumn(PixelGrid grid) {
        int column = grid.getWidth() - 1;
        while (!columnHasInk(grid, column)) {
            column--;
        }
        return column;
    }

    private static int firstInkedRow(PixelGrid grid) {
        int row = 0;
        while (!rowHasInk(grid, row)) {
            row++;
        }
        return row;
    }

    private static int lastInkedRow(PixelGrid grid) {
        int row = grid.getHeight() - 1;
        while (!rowHasInk(grid, row)) {
            row--;
        }
        return row;
    }

    /** Whether any pixel in the column is lit. */
    public static boolean columnHasInk(PixelGrid grid, int column) {
        for (int y = 0; y < grid.getHeight(); y++) {
            if (grid.isLit(column, y)) {
                return true;
            }
        }
        return false;
    }

    /** Whether any pixel in the row is lit. */
    public static boolean rowHasInk(PixelGrid grid, int row) {
        for (int x = 0; x < grid.getWidth(); x++) {
            if (grid.isLit(x, row)) {
                return true;
            }
        }
        return false;
    }
}
