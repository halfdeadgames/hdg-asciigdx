package com.github.halfdeadgames.asciigdx.render;

/**
 * Gives the glyph and colors to draw for a cell: the grid's value with the cell handle's temporary
 * overrides applied. Flat cells and lifted cells share it so both look the same. A null or
 * detached handle means no overrides.
 */
final class CellResolver {

    private CellResolver() {
    }

    static int codePoint(int gridCodePoint, CellHandle handle) {
        if (!isActive(handle)) {
            return gridCodePoint;
        }
        return handle.codePointFor(gridCodePoint);
    }

    static int foreground(int gridForeground, CellHandle handle) {
        if (!isActive(handle)) {
            return gridForeground;
        }
        return handle.foregroundFor(gridForeground);
    }

    static int background(int gridBackground, CellHandle handle) {
        if (!isActive(handle)) {
            return gridBackground;
        }
        return handle.backgroundFor(gridBackground);
    }

    private static boolean isActive(CellHandle handle) {
        return handle != null && !handle.isDetached();
    }
}
