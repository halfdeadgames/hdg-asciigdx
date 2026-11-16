package com.github.halfdeadgames.asciigdx.layer;

import com.github.tommyettinger.colorful.rgb.ColorTools;

/**
 * CPU-side vertex cache for a grid, in the format of {@link CellVertexLayout}
 * (one quad per cell, carrying both colors).
 *
 * <p>Each cell owns a fixed quad-sized slot, so a single cell can be rewritten
 * without moving any other. Cells with nothing to draw are written as zero-area
 * quads. Row 0 is the top row. The grid's top-left corner is at (0, 0) and y
 * grows upward, so rows lie at negative y. This class has no GL or texture
 * dependency.
 */
final class CellVertexCache {

    private final int columns;
    private final int rows;
    private final float cellWidth;
    private final float cellHeight;
    private final float[] vertices;
    private final boolean[] visibleCells;
    private final boolean[] notOpaqueCells;
    private int visibleCellCount;
    private int notOpaqueCellCount;
    private boolean fullRebuildNeeded = true;

    /**
     * Creates a cache with one empty quad slot per cell.
     *
     * @param columns number of columns, positive
     * @param rows number of rows, positive
     * @param cellWidth cell width in world units, positive
     * @param cellHeight cell height in world units, positive
     */
    CellVertexCache(int columns, int rows, float cellWidth, float cellHeight) {
        if (columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException(
                    "Grid size must be positive: " + columns + "x" + rows);
        }
        if (!(cellWidth > 0f) || !(cellHeight > 0f)) {
            throw new IllegalArgumentException(
                    "Cell size must be positive: " + cellWidth + "x" + cellHeight);
        }
        this.columns = columns;
        this.rows = rows;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        vertices = new float[columns * rows * CellVertexLayout.FLOATS_PER_QUAD];
        visibleCells = new boolean[columns * rows];
        notOpaqueCells = new boolean[columns * rows];
    }

    /** Number of cells whose quad is non-empty (background or foreground alpha above 0). */
    public int getVisibleCellCount() {
        return visibleCellCount;
    }

    /** Number of visible cells whose background alpha is below 255. */
    public int getNotOpaqueCellCount() {
        return notOpaqueCellCount;
    }

    /** True until {@link #markRebuilt()} is called; set on construction. */
    public boolean isFullRebuildNeeded() {
        return fullRebuildNeeded;
    }

    /** Call after every cell has been written, to clear the full-rebuild flag. */
    public void markRebuilt() {
        fullRebuildNeeded = false;
    }

    /**
     * Writes a cell's quad. Callers pass a foreground alpha of 0 for blank glyphs.
     *
     * @param backgroundRgba RGBA8888 background color
     * @param foregroundRgba RGBA8888 glyph color
     * @param u left texture coordinate
     * @param v top texture coordinate (TextureRegion.getV())
     * @param u2 right texture coordinate
     * @param v2 bottom texture coordinate (TextureRegion.getV2())
     * @throws IndexOutOfBoundsException if the cell is outside the grid
     */
    public void writeCell(int column, int row, int backgroundRgba, int foregroundRgba,
            float u, float v, float u2, float v2) {
        Guards.requireCellInside(column, row, columns, rows);
        int cell = row * columns + column;
        boolean backgroundVisible = !isFullyTransparent(backgroundRgba);
        boolean glyphVisible = !isFullyTransparent(foregroundRgba);
        boolean visible = backgroundVisible || glyphVisible;

        writeQuad(cell, column, row, backgroundRgba, foregroundRgba, backgroundVisible,
                glyphVisible, u, v, u2, v2);
        updateVisible(cell, visible);
        updateNotOpaque(cell, visible && (backgroundRgba & 0xFF) != 0xFF);
    }

    private void updateVisible(int cell, boolean visible) {
        if (visible == visibleCells[cell]) {
            return;
        }
        visibleCells[cell] = visible;
        visibleCellCount += visible ? 1 : -1;
    }

    private void updateNotOpaque(int cell, boolean notOpaque) {
        if (notOpaque == notOpaqueCells[cell]) {
            return;
        }
        notOpaqueCells[cell] = notOpaque;
        notOpaqueCellCount += notOpaque ? 1 : -1;
    }

    /** True when the RGBA8888 alpha byte is zero, so nothing would be visible. */
    static boolean isFullyTransparent(int rgba) {
        return (rgba & 0xFF) == 0;
    }

    private void writeQuad(int cell, int column, int row, int backgroundRgba, int foregroundRgba,
            boolean backgroundVisible, boolean glyphVisible,
            float u, float v, float u2, float v2) {
        int start = cell * CellVertexLayout.FLOATS_PER_QUAD;
        int stride = CellVertexLayout.FLOATS_PER_VERTEX;
        float left = column * cellWidth;
        float bottom = -(row + 1) * cellHeight;
        if (!backgroundVisible && !glyphVisible) {
            for (int vertex = 0; vertex < CellVertexLayout.VERTICES_PER_QUAD; vertex++) {
                putVertex(start + vertex * stride, left, bottom, 0f, 0f, 0f, 0f);
            }
            return;
        }
        float background = ColorTools.fromRGBA8888(backgroundRgba);
        if (!glyphVisible) {
            putQuad(start, left, bottom, background, 0f, 0f, 0f, 0f, 0f);
            return;
        }
        putQuad(start, left, bottom, background, ColorTools.fromRGBA8888(foregroundRgba),
                u, v, u2, v2);
    }

    private void putQuad(int start, float left, float bottom, float background, float foreground,
            float u, float v, float u2, float v2) {
        int stride = CellVertexLayout.FLOATS_PER_VERTEX;
        float right = left + cellWidth;
        float top = bottom + cellHeight;

        putVertex(start, left, bottom, background, foreground, u, v2);
        putVertex(start + stride, left, top, background, foreground, u, v);
        putVertex(start + 2 * stride, right, top, background, foreground, u2, v);
        putVertex(start + 3 * stride, right, bottom, background, foreground, u2, v2);
    }

    private void putVertex(int start, float x, float y, float background, float foreground, float u,
            float v) {
        vertices[start + CellVertexLayout.POSITION_OFFSET] = x;
        vertices[start + CellVertexLayout.POSITION_OFFSET + 1] = y;
        vertices[start + CellVertexLayout.BACKGROUND_COLOR_OFFSET] = background;
        vertices[start + CellVertexLayout.FOREGROUND_COLOR_OFFSET] = foreground;
        vertices[start + CellVertexLayout.UV_OFFSET] = u;
        vertices[start + CellVertexLayout.UV_OFFSET + 1] = v;
    }

    /** Cell vertices; the live array, not a copy. */
    public float[] getVertices() {
        return vertices;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }
}
