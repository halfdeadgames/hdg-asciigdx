package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.DirtyCellVisitor;
import com.github.halfdeadgames.asciigdx.grid.DirtyTracker;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.tommyettinger.colorful.rgb.ColorTools;

/**
 * Draws a {@link TerminalGrid} with a {@link GlyphSource} into a caller-managed
 * {@link SpriteBatch}: all cell backgrounds first, then all glyphs.
 *
 * <p>Vertices are kept in a {@link GridVertexCache}. Each frame only the cells
 * changed since the last draw are rewritten; moving the draw position rewrites
 * every cell. When more than half the cells changed, the cache is skipped and
 * cells are drawn straight into the batch, and the next cached frame rebuilds
 * the cache. The renderer owns a {@link DirtyTracker} on the grid and releases
 * it on {@link #dispose()}.
 *
 * <p>All glyph regions of the source must share one texture, as is true for
 * sheet-based sources. The cell size of the glyph source is read once at
 * construction. The grid size is fixed, so no resize handling is needed.
 *
 * <p>The grid and glyph source are not owned and are not disposed.
 */
public final class GridRenderer implements Disposable {

    private static final float DIRECT_DRAW_DIRTY_FRACTION = 0.5f;

    private final TerminalGrid grid;
    private final GlyphSource glyphSource;
    private final Texture whitePixel;
    private final Texture glyphTexture;
    private final DirtyTracker tracker;
    private final GridVertexCache cache;
    private final DirtyCellVisitor cellWriter = this::writeCell;
    private final float cellWidth;
    private final float cellHeight;
    private final int cellCount;
    private float lastX;
    private float lastY;

    /**
     * @param grid the grid to draw
     * @param glyphSource supplies glyph regions and the cell size
     */
    public GridRenderer(TerminalGrid grid, GlyphSource glyphSource) {
        this.grid = grid;
        this.glyphSource = glyphSource;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
        glyphTexture = glyphSource.glyph(' ').getTexture();
        tracker = grid.createDirtyTracker();
        cellWidth = glyphSource.getCellWidth();
        cellHeight = glyphSource.getCellHeight();
        cellCount = grid.getWidth() * grid.getHeight();
        cache = new GridVertexCache(grid.getWidth(), grid.getHeight(), cellWidth, cellHeight);
    }

    /**
     * Draws the grid. The batch must already be begun; this method never begins
     * or ends it. The batch color is ignored and left unchanged.
     *
     * @param batch the active batch
     * @param x screen x of the grid's top-left corner
     * @param y screen y of the grid's top-left corner (y grows upward)
     */
    public void draw(SpriteBatch batch, float x, float y) {
        if (tracker.getDirtyCellCount() > DIRECT_DRAW_DIRTY_FRACTION * cellCount) {
            drawDirect(batch, x, y);
            tracker.clear();
            cache.markStale();
            return;
        }
        if (cache.isFullRebuildNeeded() || x != lastX || y != lastY) {
            lastX = x;
            lastY = y;
            cache.setOrigin(x, y);
            for (int row = 0; row < grid.getHeight(); row++) {
                for (int column = 0; column < grid.getWidth(); column++) {
                    writeCell(column, row);
                }
            }
            cache.markRebuilt();
            tracker.clear();
        } else if (tracker.hasDirtyCells()) {
            tracker.forEachDirtyCell(cellWriter);
            tracker.clear();
        }
        int floatCount = cache.getVertexFloatCount();
        batch.draw(whitePixel, cache.getBackgroundVertices(), 0, floatCount);
        batch.draw(glyphTexture, cache.getGlyphVertices(), 0, floatCount);
    }

    private void drawDirect(SpriteBatch batch, float x, float y) {
        float savedColor = batch.getPackedColor();
        int columns = grid.getWidth();
        int rows = grid.getHeight();
        for (int row = 0; row < rows; row++) {
            float bottom = y - (row + 1) * cellHeight;
            for (int column = 0; column < columns; column++) {
                int background = grid.getBackground(column, row);
                if (GridVertexCache.isEmpty(background)) {
                    continue;
                }
                batch.setPackedColor(ColorTools.fromRGBA8888(background));
                batch.draw(whitePixel, x + column * cellWidth, bottom, cellWidth, cellHeight);
            }
        }
        for (int row = 0; row < rows; row++) {
            float bottom = y - (row + 1) * cellHeight;
            for (int column = 0; column < columns; column++) {
                int codePoint = grid.getGlyph(column, row);
                int foreground = grid.getForeground(column, row);
                if (isBlankGlyph(codePoint, foreground)) {
                    continue;
                }
                batch.setPackedColor(ColorTools.fromRGBA8888(foreground));
                batch.draw(glyphSource.glyph(codePoint), x + column * cellWidth, bottom, cellWidth, cellHeight);
            }
        }
        batch.setPackedColor(savedColor);
    }

    private static boolean isBlankGlyph(int codePoint, int foreground) {
        return codePoint == ' ' || codePoint == TerminalGrid.EMPTY_GLYPH || GridVertexCache.isEmpty(foreground);
    }

    private void writeCell(int column, int row) {
        cache.writeBackground(column, row, grid.getBackground(column, row), 0f, 0f, 1f, 1f);
        int codePoint = grid.getGlyph(column, row);
        int foreground = grid.getForeground(column, row);
        if (isBlankGlyph(codePoint, foreground)) {
            cache.writeGlyph(column, row, TerminalGrid.TRANSPARENT_RGBA, 0f, 0f, 0f, 0f);
            return;
        }
        TextureRegion region = glyphSource.glyph(codePoint);
        cache.writeGlyph(column, row, foreground, region.getU(), region.getV(), region.getU2(), region.getV2());
    }

    /** Releases the dirty tracker and frees the background texture. */
    @Override
    public void dispose() {
        tracker.release();
        whitePixel.dispose();
    }
}
