package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.DirtyCellVisitor;
import com.github.halfdeadgames.asciigdx.grid.DirtyTracker;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/**
 * Draws a {@link TerminalView} with a {@link GlyphSource} into a caller-managed
 * {@link SpriteBatch}: all cell backgrounds first, then all glyphs.
 *
 * <p>Vertices are kept in a {@link GridVertexCache} and mirrored in GPU meshes.
 * Each frame only the cells changed since the last draw are rewritten in the
 * cache; if any changed, the meshes are re-uploaded. Vertices are built around
 * a fixed origin and the draw position is applied as a translation on the GPU
 * matrix, so moving the draw position is free (no vertex rewrite or upload).
 * Position values are used as given, without rounding. Drawing flushes the batch and renders the meshes with
 * their own shader, then rebinds the batch's shader. The renderer owns a
 * {@link DirtyTracker} on the grid and releases it on {@link #dispose()}.
 *
 * <p>All glyph regions of the source must share one texture, as is true for
 * sheet-based sources. The cell size of the glyph source is read at
 * construction and again after {@link #setGlyphSource}. If the grid is resized or the glyph
 * source is switched, the next draw rebuilds the cache and meshes (this allocates, once per
 * change).
 *
 * <p>The grid and glyph source are not owned and are not disposed.
 */
public final class GridRenderer implements Disposable, LayerRenderer {

    private final TerminalView grid;
    private GlyphSource glyphSource;
    private final GridRenderResources resources;
    private final boolean ownsResources;
    private final Texture whitePixel;
    private Texture glyphTexture;
    private boolean glyphSourceChanged;
    private final DirtyTracker tracker;
    private GridVertexCache cache;
    private GridMesh mesh;
    private int cachedColumns;
    private int cachedRows;
    private final DirtyCellVisitor cellWriter = this::writeCell;

    /**
     * @param grid the grid to draw
     * @param glyphSource supplies glyph regions and the cell size
     */
    public GridRenderer(TerminalView grid, GlyphSource glyphSource) {
        this(grid, glyphSource, new GridRenderResources(), true);
    }

    GridRenderer(TerminalView grid, GlyphSource glyphSource, GridRenderResources resources) {
        this(grid, glyphSource, resources, false);
    }

    private GridRenderer(TerminalView grid, GlyphSource glyphSource, GridRenderResources resources,
            boolean ownsResources) {
        this.grid = grid;
        this.glyphSource = glyphSource;
        this.resources = resources;
        this.ownsResources = ownsResources;
        whitePixel = resources.getWhitePixel();
        glyphTexture = glyphSource.glyph(' ').getTexture();
        tracker = grid.createDirtyTracker();
        createCacheAndMesh();
    }

    private void createCacheAndMesh() {
        cachedColumns = grid.getWidth();
        cachedRows = grid.getHeight();
        cache = new GridVertexCache(cachedColumns, cachedRows,
                glyphSource.getCellWidth(), glyphSource.getCellHeight());
        mesh = new GridMesh(cachedColumns * cachedRows, resources.getShader());
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
        draw(batch, x, y, 1f, 1f, 1f, 1f);
    }

    /**
     * Draws the grid like {@link #draw(SpriteBatch, float, float)}, multiplying
     * every cell color by the tint on the GPU. Changing the tint costs no vertex
     * rewrite or upload.
     */
    @Override
    public void draw(SpriteBatch batch, float x, float y, float tintR, float tintG, float tintB, float tintA) {
        if (glyphSourceChanged || grid.getWidth() != cachedColumns || grid.getHeight() != cachedRows) {
            glyphSourceChanged = false;
            mesh.dispose();
            createCacheAndMesh();
        }
        if (cache.isFullRebuildNeeded()) {
            for (int row = 0; row < grid.getHeight(); row++) {
                for (int column = 0; column < grid.getWidth(); column++) {
                    writeCell(column, row);
                }
            }
            cache.markRebuilt();
            tracker.clear();
            mesh.upload(cache.getBackgroundVertices(), cache.getGlyphVertices());
        } else if (tracker.hasDirtyCells()) {
            tracker.forEachDirtyCell(cellWriter);
            tracker.clear();
            mesh.upload(cache.getBackgroundVertices(), cache.getGlyphVertices());
        }
        mesh.draw(batch, whitePixel, glyphTexture, x, y, tintR, tintG, tintB, tintA,
                cache.getTranslucentBackgroundCount() == 0,
                cache.getVisibleBackgroundCount() > 0, cache.getVisibleGlyphCount() > 0);
    }

    /**
     * Switches to another glyph source. The glyph texture and cell size are taken from it now;
     * the next draw rebuilds the cache and meshes and rewrites every cell (this allocates, once
     * per switch). The grid keeps its contents and its cell count. Glyphs missing from the new
     * source draw as its fallback. The source is not owned.
     */
    @Override
    public void setGlyphSource(GlyphSource glyphSource) {
        this.glyphSource = glyphSource;
        glyphTexture = glyphSource.glyph(' ').getTexture();
        glyphSourceChanged = true;
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

    /**
     * Releases the dirty tracker and frees the meshes. The shader and background
     * texture are freed only if this renderer created them.
     */
    @Override
    public void dispose() {
        tracker.release();
        mesh.dispose();
        if (ownsResources) {
            resources.dispose();
        }
    }
}
