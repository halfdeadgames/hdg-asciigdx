package com.github.halfdeadgames.asciigdx.render;

import com.github.tommyettinger.colorful.rgb.ColorTools;

/**
 * CPU-side vertex cache for a grid, in SpriteBatch vertex format
 * (x, y, packedColor, u, v per vertex, four vertices per cell).
 *
 * <p>Each cell owns a fixed 20-float slot in both the background and glyph arrays,
 * so a single cell can be rewritten without moving any other. Empty cells are
 * written as zero-area quads. Row 0 is the top row. This class has no GL or
 * texture dependency.
 */
public final class GridVertexCache {

    /** Floats per cell: 4 vertices of x, y, color, u, v. */
    public static final int FLOATS_PER_CELL = 20;

    private final int columns;
    private final int rows;
    private final float cellWidth;
    private final float cellHeight;
    private final float[] backgrounds;
    private final float[] glyphs;
    private float originX;
    private float originY;
    private boolean fullRebuildNeeded = true;

    /**
     * @param columns number of columns, positive
     * @param rows number of rows, positive
     * @param cellWidth cell width in world units, positive
     * @param cellHeight cell height in world units, positive
     */
    public GridVertexCache(int columns, int rows, float cellWidth, float cellHeight) {
        if (columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException("Grid size must be positive: " + columns + "x" + rows);
        }
        if (!(cellWidth > 0f) || !(cellHeight > 0f)) {
            throw new IllegalArgumentException("Cell size must be positive: " + cellWidth + "x" + cellHeight);
        }
        this.columns = columns;
        this.rows = rows;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        backgrounds = new float[columns * rows * FLOATS_PER_CELL];
        glyphs = new float[columns * rows * FLOATS_PER_CELL];
    }

    /**
     * Sets the screen position of the grid's top-left corner (y grows upward).
     * Cached positions become stale, so a full rebuild is flagged.
     */
    public void setOrigin(float x, float y) {
        originX = x;
        originY = y;
        fullRebuildNeeded = true;
    }

    /** True until {@link #markRebuilt()} is called; set on construction and by {@link #setOrigin}. */
    public boolean isFullRebuildNeeded() {
        return fullRebuildNeeded;
    }

    /** Call after every cell has been rewritten for the current origin. */
    public void markRebuilt() {
        fullRebuildNeeded = false;
    }

    /**
     * Writes a cell's background quad.
     *
     * @param rgba RGBA8888 color; zero alpha writes a zero-area quad
     * @param u left texture coordinate
     * @param v top texture coordinate (TextureRegion.getV())
     * @param u2 right texture coordinate
     * @param v2 bottom texture coordinate (TextureRegion.getV2())
     * @throws IndexOutOfBoundsException if the cell is outside the grid
     */
    public void writeBackground(int column, int row, int rgba, float u, float v, float u2, float v2) {
        writeQuad(backgrounds, column, row, rgba, u, v, u2, v2);
    }

    /**
     * Writes a cell's glyph quad; parameters as in {@link #writeBackground}.
     *
     * @throws IndexOutOfBoundsException if the cell is outside the grid
     */
    public void writeGlyph(int column, int row, int rgba, float u, float v, float u2, float v2) {
        writeQuad(glyphs, column, row, rgba, u, v, u2, v2);
    }

    /** True when the RGBA8888 alpha byte is zero, so nothing would be visible. */
    public static boolean isEmpty(int rgba) {
        return (rgba & 0xFF) == 0;
    }

    private void writeQuad(float[] target, int column, int row, int rgba, float u, float v, float u2, float v2) {
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("Cell (" + column + ", " + row + ") outside " + columns + "x" + rows);
        }
        int i = (row * columns + column) * FLOATS_PER_CELL;
        float left = originX + column * cellWidth;
        float bottom = originY - (row + 1) * cellHeight;
        if (isEmpty(rgba)) {
            for (int vertex = 0; vertex < 4; vertex++) {
                target[i++] = left;
                target[i++] = bottom;
                target[i++] = 0f;
                target[i++] = 0f;
                target[i++] = 0f;
            }
            return;
        }
        float color = ColorTools.fromRGBA8888(rgba);
        float right = left + cellWidth;
        float top = bottom + cellHeight;

        target[i] = left;
        target[i + 1] = bottom;
        target[i + 2] = color;
        target[i + 3] = u;
        target[i + 4] = v2;

        target[i + 5] = left;
        target[i + 6] = top;
        target[i + 7] = color;
        target[i + 8] = u;
        target[i + 9] = v;

        target[i + 10] = right;
        target[i + 11] = top;
        target[i + 12] = color;
        target[i + 13] = u2;
        target[i + 14] = v;

        target[i + 15] = right;
        target[i + 16] = bottom;
        target[i + 17] = color;
        target[i + 18] = u2;
        target[i + 19] = v2;
    }

    /** Background vertices; the live array, not a copy. */
    public float[] getBackgroundVertices() {
        return backgrounds;
    }

    /** Glyph vertices; the live array, not a copy. */
    public float[] getGlyphVertices() {
        return glyphs;
    }

    /** Length of each vertex array: columns * rows * {@link #FLOATS_PER_CELL}. */
    public int getVertexFloatCount() {
        return backgrounds.length;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }
}
