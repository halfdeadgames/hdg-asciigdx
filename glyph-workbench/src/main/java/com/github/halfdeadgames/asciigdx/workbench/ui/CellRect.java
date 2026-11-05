package com.github.halfdeadgames.asciigdx.workbench.ui;

/** A rectangle measured in UI cells. */
public record CellRect(int x, int y, int width, int height) {

    public int bottom() {
        return y + height;
    }
}
