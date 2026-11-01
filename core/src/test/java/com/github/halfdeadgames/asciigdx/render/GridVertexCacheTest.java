package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tommyettinger.colorful.rgb.ColorTools;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GridVertexCacheTest {

    private static final float EPSILON = 1e-4f;
    private static final int COLUMNS = 5;
    private static final int ROWS = 4;
    private static final int OPAQUE_RED = 0xFF0000FF;
    private static final int PER_CELL = GridVertexLayout.FLOATS_PER_QUAD;

    private static GridVertexCache newCache() {
        return new GridVertexCache(COLUMNS, ROWS, 8f, 12f);
    }

    private static int offsetOf(int column, int row) {
        return (row * COLUMNS + column) * PER_CELL;
    }

    private static float[] slotOf(float[] vertices, int column, int row) {
        int offset = offsetOf(column, row);
        return Arrays.copyOfRange(vertices, offset, offset + PER_CELL);
    }

    private static void assertVertex(float[] slot, int vertex, float x, float y, float u, float v) {
        int base = vertex * 5;
        assertEquals(x, slot[base], EPSILON, "x of vertex " + vertex);
        assertEquals(y, slot[base + 1], EPSILON, "y of vertex " + vertex);
        assertEquals(u, slot[base + 3], EPSILON, "u of vertex " + vertex);
        assertEquals(v, slot[base + 4], EPSILON, "v of vertex " + vertex);
    }

    @Test
    void translucentCountTracksWritesAndOverwrites() {
        GridVertexCache cache = newCache();
        assertEquals(0, cache.getTranslucentBackgroundCount());
        cache.writeBackground(0, 0, 0xFF000080, 0f, 0f, 1f, 1f);
        cache.writeBackground(1, 0, 0x00FF0001, 0f, 0f, 1f, 1f);
        assertEquals(2, cache.getTranslucentBackgroundCount());
        cache.writeBackground(0, 0, 0xFF000080, 0f, 0f, 1f, 1f);
        assertEquals(2, cache.getTranslucentBackgroundCount());
        cache.writeBackground(0, 0, OPAQUE_RED, 0f, 0f, 1f, 1f);
        assertEquals(1, cache.getTranslucentBackgroundCount());
        cache.writeBackground(1, 0, 0, 0f, 0f, 1f, 1f);
        assertEquals(0, cache.getTranslucentBackgroundCount());
    }

    @Test
    void translucentCountIgnoresZeroAlphaWithColor() {
        GridVertexCache cache = newCache();
        cache.writeBackground(2, 2, 0xFF000000, 0f, 0f, 1f, 1f);
        assertEquals(0, cache.getTranslucentBackgroundCount());
    }

    @Test
    void translucentCountSurvivesFullRewrite() {
        GridVertexCache cache = newCache();
        for (int pass = 0; pass < 2; pass++) {
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    cache.writeBackground(column, row, 0x10203040, 0f, 0f, 1f, 1f);
                }
            }
            assertEquals(COLUMNS * ROWS, cache.getTranslucentBackgroundCount());
        }
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                cache.writeBackground(column, row, OPAQUE_RED, 0f, 0f, 1f, 1f);
            }
        }
        assertEquals(0, cache.getTranslucentBackgroundCount());
    }

    @Test
    void visibleCountsTrackWritesOverwritesAndClears() {
        GridVertexCache cache = newCache();
        assertEquals(0, cache.getVisibleBackgroundCount());
        assertEquals(0, cache.getVisibleGlyphCount());

        cache.writeBackground(0, 0, OPAQUE_RED, 0f, 0f, 1f, 1f);
        cache.writeBackground(1, 0, 0x10203040, 0f, 0f, 1f, 1f);
        cache.writeBackground(2, 0, 0xFF000000, 0f, 0f, 1f, 1f);
        cache.writeGlyph(0, 0, OPAQUE_RED, 0f, 0f, 1f, 1f);
        cache.writeGlyph(1, 0, 0xFF00FF00, 0f, 0f, 1f, 1f);
        assertEquals(2, cache.getVisibleBackgroundCount());
        assertEquals(1, cache.getVisibleGlyphCount());

        cache.writeBackground(0, 0, 0x11223344, 0f, 0f, 1f, 1f);
        cache.writeGlyph(0, 0, 0x11223344, 0f, 0f, 1f, 1f);
        assertEquals(2, cache.getVisibleBackgroundCount());
        assertEquals(1, cache.getVisibleGlyphCount());

        cache.writeBackground(0, 0, 0, 0f, 0f, 1f, 1f);
        cache.writeBackground(1, 0, 0, 0f, 0f, 1f, 1f);
        cache.writeGlyph(0, 0, 0, 0f, 0f, 1f, 1f);
        assertEquals(0, cache.getVisibleBackgroundCount());
        assertEquals(0, cache.getVisibleGlyphCount());
    }

    @Test
    void visibleCountsSurviveFullRewrite() {
        GridVertexCache cache = newCache();
        for (int pass = 0; pass < 2; pass++) {
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    cache.writeBackground(column, row, OPAQUE_RED, 0f, 0f, 1f, 1f);
                    cache.writeGlyph(column, row, OPAQUE_RED, 0f, 0f, 1f, 1f);
                }
            }
            assertEquals(COLUMNS * ROWS, cache.getVisibleBackgroundCount());
            assertEquals(COLUMNS * ROWS, cache.getVisibleGlyphCount());
        }
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                cache.writeBackground(column, row, 0, 0f, 0f, 1f, 1f);
                cache.writeGlyph(column, row, 0, 0f, 0f, 1f, 1f);
            }
        }
        assertEquals(0, cache.getVisibleBackgroundCount());
        assertEquals(0, cache.getVisibleGlyphCount());
    }

    @Test
    void constructorRejectsNonPositiveColumns() {
        assertThrows(IllegalArgumentException.class, () -> new GridVertexCache(0, 4, 8f, 12f));
        assertThrows(IllegalArgumentException.class, () -> new GridVertexCache(-1, 4, 8f, 12f));
    }

    @Test
    void constructorRejectsNonPositiveRows() {
        assertThrows(IllegalArgumentException.class, () -> new GridVertexCache(5, 0, 8f, 12f));
        assertThrows(IllegalArgumentException.class, () -> new GridVertexCache(5, -3, 8f, 12f));
    }

    @Test
    void constructorRejectsNonPositiveCellSize() {
        assertThrows(IllegalArgumentException.class, () -> new GridVertexCache(5, 4, 0f, 12f));
        assertThrows(IllegalArgumentException.class, () -> new GridVertexCache(5, 4, 8f, -1f));
        assertThrows(IllegalArgumentException.class, () -> new GridVertexCache(5, 4, Float.NaN, 12f));
    }

    @Test
    void arraysHoldTwentyFloatsPerCell() {
        GridVertexCache cache = newCache();
        int expected = COLUMNS * ROWS * 20;
        assertEquals(expected, cache.getBackgroundVertices().length);
        assertEquals(expected, cache.getGlyphVertices().length);
        assertEquals(COLUMNS, cache.getColumns());
        assertEquals(ROWS, cache.getRows());
    }

    @Test
    void cornerCellsWriteIntoTheirOwnSlots() {
        GridVertexCache cache = newCache();
        float[] glyphs = cache.getGlyphVertices();

        cache.writeGlyph(0, 0, OPAQUE_RED, 0f, 0f, 1f, 1f);
        assertEquals(0f, glyphs[0], EPSILON);
        assertEquals(-12f, glyphs[1], EPSILON);

        cache.writeGlyph(COLUMNS - 1, ROWS - 1, OPAQUE_RED, 0f, 0f, 1f, 1f);
        int last = glyphs.length - PER_CELL;
        assertEquals(4 * 8f, glyphs[last], EPSILON);
        assertEquals(-ROWS * 12f, glyphs[last + 1], EPSILON);
    }

    @Test
    void middleCellWritesAtRowMajorOffset() {
        GridVertexCache cache = newCache();
        cache.writeGlyph(2, 1, OPAQUE_RED, 0f, 0f, 1f, 1f);

        float[] glyphs = cache.getGlyphVertices();
        int offset = (1 * COLUMNS + 2) * PER_CELL;
        assertEquals(16f, glyphs[offset], EPSILON);
        assertEquals(-24f, glyphs[offset + 1], EPSILON);
        for (int i = 0; i < glyphs.length; i++) {
            if (i < offset || i >= offset + PER_CELL) {
                assertEquals(0f, glyphs[i], 0f, "index " + i);
            }
        }
    }

    @Test
    void positionsFollowVertexOrder() {
        GridVertexCache cache = newCache();
        cache.writeBackground(2, 1, OPAQUE_RED, 0f, 0f, 1f, 1f);

        float[] slot = slotOf(cache.getBackgroundVertices(), 2, 1);
        assertVertex(slot, 0, 16f, -24f, 0f, 1f);
        assertVertex(slot, 1, 16f, -12f, 0f, 0f);
        assertVertex(slot, 2, 24f, -12f, 1f, 0f);
        assertVertex(slot, 3, 24f, -24f, 1f, 1f);
    }

    @Test
    void uvPlacementUsesRegionBounds() {
        GridVertexCache cache = newCache();
        cache.writeGlyph(1, 2, OPAQUE_RED, 0.25f, 0.5f, 0.75f, 0.625f);

        float[] slot = slotOf(cache.getGlyphVertices(), 1, 2);
        assertEquals(0.25f, slot[3], 0f);
        assertEquals(0.625f, slot[4], 0f);
        assertEquals(0.25f, slot[8], 0f);
        assertEquals(0.5f, slot[9], 0f);
        assertEquals(0.75f, slot[13], 0f);
        assertEquals(0.5f, slot[14], 0f);
        assertEquals(0.75f, slot[18], 0f);
        assertEquals(0.625f, slot[19], 0f);
    }

    @Test
    void colorMatchesColorToolsOnAllFourVertices() {
        GridVertexCache cache = newCache();
        int rgba = 0x336699CC;
        cache.writeGlyph(3, 3, rgba, 0f, 0f, 1f, 1f);

        float expected = ColorTools.fromRGBA8888(rgba);
        float[] slot = slotOf(cache.getGlyphVertices(), 3, 3);
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(slot[vertex * 5 + 2]));
        }
    }

    @Test
    void zeroRgbaWritesZeroAreaQuadAtBottomLeft() {
        GridVertexCache cache = newCache();
        cache.writeBackground(2, 1, 0, 0.1f, 0.2f, 0.3f, 0.4f);

        assertDegenerateAt(slotOf(cache.getBackgroundVertices(), 2, 1), 16f, -24f);
    }

    @Test
    void zeroAlphaWithRgbWritesZeroAreaQuadWithZeroColor() {
        GridVertexCache cache = newCache();
        cache.writeGlyph(2, 1, 0xFF00FF00, 0.1f, 0.2f, 0.3f, 0.4f);

        assertDegenerateAt(slotOf(cache.getGlyphVertices(), 2, 1), 16f, -24f);
    }

    private static void assertDegenerateAt(float[] slot, float x, float y) {
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * 5;
            assertEquals(x, slot[base], EPSILON);
            assertEquals(y, slot[base + 1], EPSILON);
            assertEquals(0f, slot[base + 2], 0f);
            assertEquals(0f, slot[base + 3], 0f);
            assertEquals(0f, slot[base + 4], 0f);
        }
    }

    @Test
    void isEmptyDependsOnlyOnAlphaByte() {
        assertTrue(GridVertexCache.isEmpty(0));
        assertTrue(GridVertexCache.isEmpty(0xFF00FF00));
        assertTrue(GridVertexCache.isEmpty(0xFFFFFF00));
        assertFalse(GridVertexCache.isEmpty(0x00000001));
        assertFalse(GridVertexCache.isEmpty(0xFFFFFFFF));
    }

    @Test
    void rewritingOneCellLeavesNeighborsUntouched() {
        GridVertexCache cache = newCache();
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                cache.writeGlyph(column, row, OPAQUE_RED, 0f, 0f, 1f, 1f);
            }
        }
        float[] before = cache.getGlyphVertices().clone();

        cache.writeGlyph(2, 1, 0x00FF00FF, 0.5f, 0.5f, 0.6f, 0.6f);

        float[] after = cache.getGlyphVertices();
        int offset = offsetOf(2, 1);
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
    void backgroundAndGlyphArraysAreIndependent() {
        GridVertexCache cache = newCache();
        float[] glyphsBefore = cache.getGlyphVertices().clone();

        cache.writeBackground(1, 1, OPAQUE_RED, 0f, 0f, 1f, 1f);

        assertArrayEquals(glyphsBefore, cache.getGlyphVertices(), 0f);

        float[] backgroundsBefore = cache.getBackgroundVertices().clone();
        cache.writeGlyph(3, 2, OPAQUE_RED, 0f, 0f, 1f, 1f);
        assertArrayEquals(backgroundsBefore, cache.getBackgroundVertices(), 0f);
    }

    @Test
    void fullRebuildFlagLifecycle() {
        GridVertexCache cache = newCache();
        assertTrue(cache.isFullRebuildNeeded());

        cache.markRebuilt();
        assertFalse(cache.isFullRebuildNeeded());
    }

    @Test
    void writingDoesNotChangeRebuildFlag() {
        GridVertexCache cache = newCache();
        cache.markRebuilt();
        cache.writeGlyph(0, 0, OPAQUE_RED, 0f, 0f, 1f, 1f);
        assertFalse(cache.isFullRebuildNeeded());
    }

    @Test
    void outOfRangeCellsThrowIndexOutOfBounds() {
        GridVertexCache cache = newCache();
        assertThrows(IndexOutOfBoundsException.class,
                () -> cache.writeGlyph(-1, 0, OPAQUE_RED, 0f, 0f, 1f, 1f));
        assertThrows(IndexOutOfBoundsException.class,
                () -> cache.writeGlyph(COLUMNS, 0, OPAQUE_RED, 0f, 0f, 1f, 1f));
        assertThrows(IndexOutOfBoundsException.class,
                () -> cache.writeBackground(0, -1, OPAQUE_RED, 0f, 0f, 1f, 1f));
        assertThrows(IndexOutOfBoundsException.class,
                () -> cache.writeBackground(0, ROWS, 0, 0f, 0f, 1f, 1f));
    }
}
