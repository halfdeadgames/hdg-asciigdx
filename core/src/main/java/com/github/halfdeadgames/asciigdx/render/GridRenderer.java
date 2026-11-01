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

/**
 * Draws a {@link TerminalGrid} with a {@link GlyphSource} into a caller-managed
 * {@link SpriteBatch}: all cell backgrounds first, then all glyphs.
 *
 * <p>Vertices are kept in a {@link GridVertexCache} and mirrored in GPU meshes.
 * Each frame only the cells changed since the last draw are rewritten in the
 * cache; if any changed, the meshes are re-uploaded. Moving the draw position
 * rewrites every cell. Drawing flushes the batch and renders the meshes with
 * their own shader, then rebinds the batch's shader. The renderer owns a
 * {@link DirtyTracker} on the grid and releases it on {@link #dispose()}.
 *
 * <p>All glyph regions of the source must share one texture, as is true for
 * sheet-based sources. The cell size of the glyph source is read once at
 * construction. The grid size is fixed, so no resize handling is needed.
 *
 * <p>The grid and glyph source are not owned and are not disposed.
 */
public final class GridRenderer implements Disposable {

    private final TerminalGrid grid;
    private final GlyphSource glyphSource;
    private final Texture whitePixel;
    private final Texture glyphTexture;
    private final DirtyTracker tracker;
    private final GridVertexCache cache;
    private final GridMesh mesh;
    private final DirtyCellVisitor cellWriter = this::writeCell;
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
        cache = new GridVertexCache(grid.getWidth(), grid.getHeight(),
                glyphSource.getCellWidth(), glyphSource.getCellHeight());
        mesh = new GridMesh(grid.getWidth() * grid.getHeight());
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
            mesh.upload(cache.getBackgroundVertices(), cache.getGlyphVertices());
        } else if (tracker.hasDirtyCells()) {
            tracker.forEachDirtyCell(cellWriter);
            tracker.clear();
            mesh.upload(cache.getBackgroundVertices(), cache.getGlyphVertices());
        }
        mesh.draw(batch, whitePixel, glyphTexture);
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

    /** Releases the dirty tracker and frees the meshes, shader, and background texture. */
    @Override
    public void dispose() {
        tracker.release();
        mesh.dispose();
        whitePixel.dispose();
    }
}
