package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** The game's own map: a grid of pond cells, all water at first. */
public final class PondMap {

    private final int width;
    private final int height;
    private final PondCell[] cells;

    /** Creates a map of water cells without lilies. */
    public PondMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "map size must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.cells = new PondCell[width * height];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new PondCell();
        }
    }

    /** Returns the cell at the position; throws when it is outside the map. */
    public PondCell cell(int column, int row) {
        if (!contains(column, row)) {
            throw new IndexOutOfBoundsException(
                    "cell (" + column + ", " + row + ") outside " + width + "x" + height);
        }
        return cells[row * width + column];
    }

    /** Forgets every visited mark, as when a walk starts over. */
    public void clearVisited() {
        for (PondCell cell : cells) {
            cell.clearVisited();
        }
    }

    /** Returns whether the position is inside the map. */
    public boolean contains(int column, int row) {
        return column >= 0 && column < width && row >= 0 && row < height;
    }

    /** Returns the map width in cells. */
    public int getWidth() {
        return width;
    }

    /** Returns the map height in cells. */
    public int getHeight() {
        return height;
    }
}
