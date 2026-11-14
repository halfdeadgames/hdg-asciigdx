package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tommyettinger.colorful.rgb.ColorTools;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CellVertexCacheTest {

    private static final float EPSILON = 1e-4f;
    private static final int COLUMNS = 5;
    private static final int ROWS = 4;
    private static final int OPAQUE_RED = 0xFF0000FF;
    private static final int OPAQUE_GREEN = 0x00FF00FF;
    private static final int TRANSLUCENT_BLUE = 0x0000FF80;
    private static final int PER_CELL = CellVertexLayout.FLOATS_PER_QUAD;
    private static final int STRIDE = CellVertexLayout.FLOATS_PER_VERTEX;

    private static CellVertexCache newCache() {
        return new CellVertexCache(COLUMNS, ROWS, 8f, 12f);
    }

    private static void write(CellVertexCache cache, int column, int row, int background, int foreground) {
        cache.writeCell(column, row, background, foreground, 0f, 0f, 1f, 1f);
    }

    private static float[] slotOf(float[] vertices, int column, int row) {
        int offset = (row * COLUMNS + column) * PER_CELL;
        return Arrays.copyOfRange(vertices, offset, offset + PER_CELL);
    }

    private static void assertVertex(float[] slot, int vertex, float x, float y, float u, float v) {
        int base = vertex * STRIDE;
        assertEquals(x, slot[base + CellVertexLayout.POSITION_OFFSET], EPSILON, "x of vertex " + vertex);
        assertEquals(y, slot[base + CellVertexLayout.POSITION_OFFSET + 1], EPSILON, "y of vertex " + vertex);
        assertEquals(u, slot[base + CellVertexLayout.UV_OFFSET], EPSILON, "u of vertex " + vertex);
        assertEquals(v, slot[base + CellVertexLayout.UV_OFFSET + 1], EPSILON, "v of vertex " + vertex);
    }

    private static void assertCounts(CellVertexCache cache, int visible, int notOpaque) {
        assertEquals(visible, cache.getVisibleCellCount(), "visible");
        assertEquals(notOpaque, cache.getNotOpaqueCellCount(), "not opaque");
    }

    @Test
    void constructorRejectsNonPositiveColumns() {
        assertThrows(IllegalArgumentException.class, () -> new CellVertexCache(0, 4, 8f, 12f));
        assertThrows(IllegalArgumentException.class, () -> new CellVertexCache(-1, 4, 8f, 12f));
    }

    @Test
    void constructorRejectsNonPositiveRows() {
        assertThrows(IllegalArgumentException.class, () -> new CellVertexCache(5, 0, 8f, 12f));
        assertThrows(IllegalArgumentException.class, () -> new CellVertexCache(5, -3, 8f, 12f));
    }

    @Test
    void constructorRejectsNonPositiveCellSize() {
        assertThrows(IllegalArgumentException.class, () -> new CellVertexCache(5, 4, 0f, 12f));
        assertThrows(IllegalArgumentException.class, () -> new CellVertexCache(5, 4, 8f, -1f));
        assertThrows(IllegalArgumentException.class, () -> new CellVertexCache(5, 4, Float.NaN, 12f));
    }

    @Test
    void arrayHoldsTwentyFourFloatsPerCell() {
        CellVertexCache cache = newCache();
        assertEquals(COLUMNS * ROWS * 24, cache.getVertices().length);
        assertEquals(COLUMNS, cache.getColumns());
        assertEquals(ROWS, cache.getRows());
    }

    @Test
    void cornerCellsWriteIntoTheirOwnSlots() {
        CellVertexCache cache = newCache();
        float[] vertices = cache.getVertices();

        write(cache, 0, 0, OPAQUE_RED, OPAQUE_GREEN);
        assertEquals(0f, vertices[0], EPSILON);
        assertEquals(-12f, vertices[1], EPSILON);

        write(cache, COLUMNS - 1, ROWS - 1, OPAQUE_RED, OPAQUE_GREEN);
        int last = vertices.length - PER_CELL;
        assertEquals(4 * 8f, vertices[last], EPSILON);
        assertEquals(-ROWS * 12f, vertices[last + 1], EPSILON);
    }

    @Test
    void middleCellWritesAtRowMajorOffsetOnly() {
        CellVertexCache cache = newCache();
        write(cache, 2, 1, OPAQUE_RED, OPAQUE_GREEN);

        float[] vertices = cache.getVertices();
        int offset = (1 * COLUMNS + 2) * PER_CELL;
        assertEquals(16f, vertices[offset], EPSILON);
        assertEquals(-24f, vertices[offset + 1], EPSILON);
        for (int i = 0; i < vertices.length; i++) {
            if (i < offset || i >= offset + PER_CELL) {
                assertEquals(0f, vertices[i], 0f, "index " + i);
            }
        }
    }

    @Test
    void positionsFollowVertexOrder() {
        CellVertexCache cache = newCache();
        write(cache, 2, 1, OPAQUE_RED, OPAQUE_GREEN);

        float[] slot = slotOf(cache.getVertices(), 2, 1);
        assertVertex(slot, 0, 16f, -24f, 0f, 1f);
        assertVertex(slot, 1, 16f, -12f, 0f, 0f);
        assertVertex(slot, 2, 24f, -12f, 1f, 0f);
        assertVertex(slot, 3, 24f, -24f, 1f, 1f);
    }

    @Test
    void uvPlacementUsesRegionBounds() {
        CellVertexCache cache = newCache();
        cache.writeCell(1, 2, OPAQUE_RED, OPAQUE_GREEN, 0.25f, 0.5f, 0.75f, 0.625f);

        float[] slot = slotOf(cache.getVertices(), 1, 2);
        assertEquals(0.25f, slot[4], 0f);
        assertEquals(0.625f, slot[5], 0f);
        assertEquals(0.25f, slot[10], 0f);
        assertEquals(0.5f, slot[11], 0f);
        assertEquals(0.75f, slot[16], 0f);
        assertEquals(0.5f, slot[17], 0f);
        assertEquals(0.75f, slot[22], 0f);
        assertEquals(0.625f, slot[23], 0f);
    }

    @Test
    void bothColorsMatchColorToolsOnAllFourVertices() {
        CellVertexCache cache = newCache();
        int background = 0x336699CC;
        int foreground = 0xCC9966AA;
        write(cache, 3, 3, background, foreground);

        int expectedBackground = Float.floatToIntBits(ColorTools.fromRGBA8888(background));
        int expectedForeground = Float.floatToIntBits(ColorTools.fromRGBA8888(foreground));
        float[] slot = slotOf(cache.getVertices(), 3, 3);
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * STRIDE;
            assertEquals(expectedBackground,
                    Float.floatToIntBits(slot[base + CellVertexLayout.BACKGROUND_COLOR_OFFSET]));
            assertEquals(expectedForeground,
                    Float.floatToIntBits(slot[base + CellVertexLayout.FOREGROUND_COLOR_OFFSET]));
        }
    }

    @Test
    void backgroundOnlyCellWritesBackgroundColorAndNoGlyph() {
        CellVertexCache cache = newCache();
        cache.writeCell(2, 1, OPAQUE_RED, 0xFFFFFF00, 0.1f, 0.2f, 0.3f, 0.4f);

        float expectedBackground = ColorTools.fromRGBA8888(OPAQUE_RED);
        float[] slot = slotOf(cache.getVertices(), 2, 1);
        assertVertex(slot, 0, 16f, -24f, 0f, 0f);
        assertVertex(slot, 1, 16f, -12f, 0f, 0f);
        assertVertex(slot, 2, 24f, -12f, 0f, 0f);
        assertVertex(slot, 3, 24f, -24f, 0f, 0f);
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * STRIDE;
            assertEquals(Float.floatToIntBits(expectedBackground),
                    Float.floatToIntBits(slot[base + CellVertexLayout.BACKGROUND_COLOR_OFFSET]));
            assertEquals(0f, slot[base + CellVertexLayout.FOREGROUND_COLOR_OFFSET], 0f);
        }
    }

    @Test
    void glyphOnlyCellKeepsGlyphColorAndUv() {
        CellVertexCache cache = newCache();
        cache.writeCell(0, 0, 0x11223300, OPAQUE_GREEN, 0.1f, 0.2f, 0.3f, 0.4f);

        float[] slot = slotOf(cache.getVertices(), 0, 0);
        assertVertex(slot, 0, 0f, -12f, 0.1f, 0.4f);
        assertVertex(slot, 2, 8f, 0f, 0.3f, 0.2f);
        assertEquals(Float.floatToIntBits(ColorTools.fromRGBA8888(OPAQUE_GREEN)),
                Float.floatToIntBits(slot[CellVertexLayout.FOREGROUND_COLOR_OFFSET]));
    }

    @Test
    void bothTransparentCellIsZeroAreaAtBottomLeft() {
        CellVertexCache cache = newCache();
        cache.writeCell(2, 1, 0xFF00FF00, 0x00FF0000, 0.1f, 0.2f, 0.3f, 0.4f);

        float[] slot = slotOf(cache.getVertices(), 2, 1);
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * STRIDE;
            assertEquals(16f, slot[base], EPSILON);
            assertEquals(-24f, slot[base + 1], EPSILON);
            assertEquals(0f, slot[base + CellVertexLayout.BACKGROUND_COLOR_OFFSET], 0f);
            assertEquals(0f, slot[base + CellVertexLayout.FOREGROUND_COLOR_OFFSET], 0f);
            assertEquals(0f, slot[base + CellVertexLayout.UV_OFFSET], 0f);
            assertEquals(0f, slot[base + CellVertexLayout.UV_OFFSET + 1], 0f);
        }
    }

    @Test
    void countersFollowOneCellThroughEveryCombination() {
        CellVertexCache cache = newCache();
        assertCounts(cache, 0, 0);

        write(cache, 1, 1, OPAQUE_RED, OPAQUE_GREEN);
        assertCounts(cache, 1, 0);

        write(cache, 1, 1, TRANSLUCENT_BLUE, OPAQUE_GREEN);
        assertCounts(cache, 1, 1);

        write(cache, 1, 1, TRANSLUCENT_BLUE, OPAQUE_GREEN);
        assertCounts(cache, 1, 1);

        write(cache, 1, 1, 0x11223300, OPAQUE_GREEN);
        assertCounts(cache, 1, 1);

        write(cache, 1, 1, 0, 0);
        assertCounts(cache, 0, 0);

        write(cache, 1, 1, 0, OPAQUE_GREEN);
        assertCounts(cache, 1, 1);

        write(cache, 1, 1, OPAQUE_RED, 0);
        assertCounts(cache, 1, 0);

        write(cache, 1, 1, TRANSLUCENT_BLUE, 0);
        assertCounts(cache, 1, 1);

        write(cache, 1, 1, 0xFF000000, 0);
        assertCounts(cache, 0, 0);
    }

    @Test
    void countersSurviveFullRewrite() {
        CellVertexCache cache = newCache();
        for (int pass = 0; pass < 2; pass++) {
            fillGrid(cache, TRANSLUCENT_BLUE, OPAQUE_GREEN);
            assertCounts(cache, COLUMNS * ROWS, COLUMNS * ROWS);
        }
        fillGrid(cache, OPAQUE_RED, OPAQUE_GREEN);
        assertCounts(cache, COLUMNS * ROWS, 0);
        fillGrid(cache, 0, 0);
        assertCounts(cache, 0, 0);
    }

    private static void fillGrid(CellVertexCache cache, int background, int foreground) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                write(cache, column, row, background, foreground);
            }
        }
    }

    @Test
    void isFullyTransparentDependsOnlyOnAlphaByte() {
        assertTrue(CellVertexCache.isFullyTransparent(0));
        assertTrue(CellVertexCache.isFullyTransparent(0xFF00FF00));
        assertFalse(CellVertexCache.isFullyTransparent(0x00000001));
        assertFalse(CellVertexCache.isFullyTransparent(0xFFFFFFFF));
    }

    @Test
    void rewritingOneCellLeavesNeighborsUntouched() {
        CellVertexCache cache = newCache();
        fillGrid(cache, OPAQUE_RED, OPAQUE_GREEN);
        float[] before = cache.getVertices().clone();

        cache.writeCell(2, 1, 0x00FF00FF, 0xFF00FFFF, 0.5f, 0.5f, 0.6f, 0.6f);

        float[] after = cache.getVertices();
        int offset = (1 * COLUMNS + 2) * PER_CELL;
        for (int i = 0; i < after.length; i++) {
            if (i < offset || i >= offset + PER_CELL) {
                assertEquals(before[i], after[i], 0f, "index " + i);
            }
        }
        assertFalse(Arrays.equals(
                Arrays.copyOfRange(before, offset, offset + PER_CELL),
                Arrays.copyOfRange(after, offset, offset + PER_CELL)));
    }

    @Test
    void fullRebuildFlagLifecycle() {
        CellVertexCache cache = newCache();
        assertTrue(cache.isFullRebuildNeeded());

        cache.markRebuilt();
        assertFalse(cache.isFullRebuildNeeded());
    }

    @Test
    void writingDoesNotChangeRebuildFlag() {
        CellVertexCache cache = newCache();
        cache.markRebuilt();
        write(cache, 0, 0, OPAQUE_RED, OPAQUE_GREEN);
        assertFalse(cache.isFullRebuildNeeded());
    }

    @Test
    void outOfRangeCellsThrowIndexOutOfBounds() {
        CellVertexCache cache = newCache();
        assertThrows(IndexOutOfBoundsException.class, () -> write(cache, -1, 0, OPAQUE_RED, OPAQUE_GREEN));
        assertThrows(IndexOutOfBoundsException.class, () -> write(cache, COLUMNS, 0, OPAQUE_RED, OPAQUE_GREEN));
        assertThrows(IndexOutOfBoundsException.class, () -> write(cache, 0, -1, OPAQUE_RED, OPAQUE_GREEN));
        assertThrows(IndexOutOfBoundsException.class, () -> write(cache, 0, ROWS, 0, 0));
        assertCounts(cache, 0, 0);
    }
}
