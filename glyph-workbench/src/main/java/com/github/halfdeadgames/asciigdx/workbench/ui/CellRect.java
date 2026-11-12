package com.github.halfdeadgames.asciigdx.workbench.ui;

/** A rectangle measured in UI cells. */
public record CellRect(int x, int y, int width, int height) {

    public int bottom() {
        return y + height;
    }

    /** Whether the cell lies inside; left and top edges count, right and bottom do not. */
    public boolean contains(int cellX, int cellY) {
        return cellX >= x && cellX < x + width && cellY >= y && cellY < bottom();
    }
}
