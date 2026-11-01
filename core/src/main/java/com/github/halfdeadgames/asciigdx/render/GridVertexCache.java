package com.github.halfdeadgames.asciigdx.render;

import com.github.tommyettinger.colorful.rgb.ColorTools;

/**
 * CPU-side vertex cache for a grid, in the format of {@link GridVertexLayout}
 * (four vertices per cell).
 *
 * <p>Each cell owns a fixed quad-sized slot in both the background and glyph arrays,
 * so a single cell can be rewritten without moving any other. Empty cells are
 * written as zero-area quads. Row 0 is the top row. The grid's top-left corner is at (0, 0) and y grows upward, so rows
 * lie at negative y. This class has no GL or
 * texture dependency.
 */
public final class GridVertexCache {

    private final int columns;
    private final int rows;
    private final float cellWidth;
    private final float cellHeight;
    private final float[] backgrounds;
    private final float[] glyphs;
    private final boolean[] translucentCells;
    private final boolean[] visibleBackgroundCells;
    private final boolean[] visibleGlyphCells;
    private int translucentBackgroundCount;
    private int visibleBackgroundCount;
    private int visibleGlyphCount;
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
        backgrounds = new float[columns * rows * GridVertexLayout.FLOATS_PER_QUAD];
        glyphs = new float[columns * rows * GridVertexLayout.FLOATS_PER_QUAD];
        translucentCells = new boolean[columns * rows];
        visibleBackgroundCells = new boolean[columns * rows];
        visibleGlyphCells = new boolean[columns * rows];
    }

    /** Number of cells whose background quad is non-empty (alpha above 0). */
    public int getVisibleBackgroundCount() {
        return visibleBackgroundCount;
    }

    /** Number of cells whose glyph quad is non-empty (alpha above 0). */
    public int getVisibleGlyphCount() {
        return visibleGlyphCount;
    }

    /** True until {@link #markRebuilt()} is called; set on construction. */
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
        int cell = row * columns + column;
        boolean translucent = isTranslucent(rgba);
        if (translucent != translucentCells[cell]) {
            translucentCells[cell] = translucent;
            translucentBackgroundCount += translucent ? 1 : -1;
        }
        boolean visible = !isEmpty(rgba);
        if (visible != visibleBackgroundCells[cell]) {
            visibleBackgroundCells[cell] = visible;
            visibleBackgroundCount += visible ? 1 : -1;
        }
    }

    /**
     * Number of cells whose background alpha is strictly between 0 and 255. Alpha 0
     * is always written as a zero-area quad, so those cells never count.
     */
    public int getTranslucentBackgroundCount() {
        return translucentBackgroundCount;
    }

    /** True when the RGBA8888 alpha byte is neither 0 nor 255. */
    public static boolean isTranslucent(int rgba) {
        int alpha = rgba & 0xFF;
        return alpha != 0 && alpha != 0xFF;
    }

    /**
     * Writes a cell's glyph quad; parameters as in {@link #writeBackground}.
     *
     * @throws IndexOutOfBoundsException if the cell is outside the grid
     */
    public void writeGlyph(int column, int row, int rgba, float u, float v, float u2, float v2) {
        writeQuad(glyphs, column, row, rgba, u, v, u2, v2);
        int cell = row * columns + column;
        boolean visible = !isEmpty(rgba);
        if (visible != visibleGlyphCells[cell]) {
            visibleGlyphCells[cell] = visible;
            visibleGlyphCount += visible ? 1 : -1;
        }
    }

    /** True when the RGBA8888 alpha byte is zero, so nothing would be visible. */
    public static boolean isEmpty(int rgba) {
        return (rgba & 0xFF) == 0;
    }

    private void writeQuad(float[] target, int column, int row, int rgba, float u, float v, float u2, float v2) {
        if (column < 0 || column >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("Cell (" + column + ", " + row + ") outside " + columns + "x" + rows);
        }
        int i = (row * columns + column) * GridVertexLayout.FLOATS_PER_QUAD;
        float left = column * cellWidth;
        float bottom = -(row + 1) * cellHeight;
        if (isEmpty(rgba)) {
            for (int vertex = 0; vertex < GridVertexLayout.VERTICES_PER_QUAD; vertex++) {
                putVertex(target, i + vertex * GridVertexLayout.FLOATS_PER_VERTEX, left, bottom, 0f, 0f, 0f);
            }
            return;
        }
        float color = ColorTools.fromRGBA8888(rgba);
        float right = left + cellWidth;
        float top = bottom + cellHeight;
        int stride = GridVertexLayout.FLOATS_PER_VERTEX;

        putVertex(target, i, left, bottom, color, u, v2);
        putVertex(target, i + stride, left, top, color, u, v);
        putVertex(target, i + 2 * stride, right, top, color, u2, v);
        putVertex(target, i + 3 * stride, right, bottom, color, u2, v2);
    }

    private static void putVertex(float[] target, int start, float x, float y, float color, float u, float v) {
        target[start + GridVertexLayout.POSITION_OFFSET] = x;
        target[start + GridVertexLayout.POSITION_OFFSET + 1] = y;
        target[start + GridVertexLayout.COLOR_OFFSET] = color;
        target[start + GridVertexLayout.UV_OFFSET] = u;
        target[start + GridVertexLayout.UV_OFFSET + 1] = v;
    }

    /** Background vertices; the live array, not a copy. */
    public float[] getBackgroundVertices() {
        return backgrounds;
    }

    /** Glyph vertices; the live array, not a copy. */
    public float[] getGlyphVertices() {
        return glyphs;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }
}
