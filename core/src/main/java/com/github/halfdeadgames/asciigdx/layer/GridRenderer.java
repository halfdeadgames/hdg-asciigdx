package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.DirtyCellVisitor;
import com.github.halfdeadgames.asciigdx.grid.DirtyTracker;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;
import java.util.Arrays;
import java.util.Objects;

/**
 * Draws a {@link TerminalView} with a {@link GlyphSource} into a caller-managed
 * {@link SpriteBatch}: one quad per cell, with the shader mixing the glyph over
 * the cell's background.
 *
 * <p>Vertices are kept in a {@link CellVertexCache} and mirrored in GPU meshes.
 * Each frame only the cells changed since the last draw are rewritten in the
 * cache; if any changed, the meshes are re-uploaded. Vertices are built around
 * a fixed origin and the draw position is applied as a translation on the GPU
 * matrix, so moving the draw position is free (no vertex rewrite or upload).
 * Position values are used as given, without rounding. Drawing flushes the batch and renders the meshes with
 * their own shader, then rebinds the batch's shader. The renderer owns a
 * {@link DirtyTracker} on the grid and releases it on {@link #dispose()}.
 *
 * <p>All glyph regions of the source must share one texture, as is true for
 * sheet-based sources. The glyph texture is read at construction and in
 * {@link #setGlyphSource}; the cell size is read whenever the cache is built: at construction
 * and at the first draw after a source switch. If the grid is resized or the glyph
 * source is switched, the next draw rebuilds the cache and meshes (this allocates, once per
 * change).
 *
 * <p>The grid and glyph source are not owned and are not disposed.
 */
public final class GridRenderer implements Disposable {

    private final TerminalView grid;
    private GlyphSource glyphSource;
    private final GridRenderResources resources;
    private final boolean ownsResources;
    private Texture glyphTexture;
    private boolean glyphSourceChanged;
    private final DirtyTracker tracker;
    private CellVertexCache cache;
    private GridMesh mesh;
    private SpriteMesh spriteMesh;
    private int cachedColumns;
    private int cachedRows;
    private boolean[] hiddenGlyphs;
    private boolean uploadPending;
    private FlatCellWriter flatWriter;
    private CellHandle[] handlesByCell;
    private int indexedHandleCount;
    private Array<CellHandle> cellHandles = new Array<>(0);
    private final DirtyCellVisitor cellWriter = this::writeCell;
    private final ColorEffects tintOnlyEffects = new ColorEffects();
    private final ResolvedEffects resolvedTintOnlyEffects = new ResolvedEffects();
    private final Affine2 translation = new Affine2();

    /**
     * Creates a renderer that owns its shader and GPU meshes. Construct it on the GL thread, and
     * call {@link #dispose()} when done. Use {@link LayerStack} instead to draw several grids.
     *
     * @param grid the grid to draw; not null. Not owned and not disposed. The renderer registers a
     *     dirty tracker on it
     * @param glyphSource supplies glyph regions and the cell size; not null. Not owned and not
     *     disposed
     * @throws NullPointerException if the grid or glyph source is null
     */
    public GridRenderer(TerminalView grid, GlyphSource glyphSource) {
        this(Objects.requireNonNull(grid, "grid"), Objects.requireNonNull(glyphSource, "glyphSource"),
            new GridRenderResources(), true);
    }

    GridRenderer(TerminalView grid, GlyphSource glyphSource, GridRenderResources resources) {
        this(Objects.requireNonNull(grid, "grid"), Objects.requireNonNull(glyphSource, "glyphSource"),
            resources, false);
    }

    private GridRenderer(TerminalView grid, GlyphSource glyphSource, GridRenderResources resources,
            boolean ownsResources) {
        this.grid = grid;
        this.glyphSource = glyphSource;
        this.resources = resources;
        this.ownsResources = ownsResources;
        glyphTexture = glyphSource.getGlyphRegion(' ').getTexture();
        tracker = grid.createDirtyTracker();
        createCacheAndMesh();
    }

    private void createCacheAndMesh() {
        cachedColumns = grid.getColumns();
        cachedRows = grid.getRows();
        cache = new CellVertexCache(cachedColumns, cachedRows,
                glyphSource.getCellWidth(), glyphSource.getCellHeight());
        mesh = new GridMesh(cachedColumns * cachedRows, resources.getShader());
        hiddenGlyphs = new boolean[cachedColumns * cachedRows];
        flatWriter = new FlatCellWriter(grid, cache, glyphSource);
        handlesByCell = new CellHandle[cachedColumns * cachedRows];
        indexedHandleCount = 0;
        uploadPending = false;
    }

    /**
     * Draws the grid. The batch must already be begun; this method never begins
     * or ends it. The batch color is ignored and left unchanged. Call it once per frame on the GL
     * thread.
     *
     * @param batch the active batch; not null
     * @param x screen x of the grid's top-left corner, in the batch's coordinate space
     * @param y screen y of the grid's top-left corner (y grows upward); the grid extends downward
     *     from it
     */
    public void draw(SpriteBatch batch, float x, float y) {
        draw(batch, x, y, 1f, 1f, 1f, 1f);
    }

    /**
     * Draws the grid like {@link #draw(SpriteBatch, float, float)}, multiplying
     * every cell color by the tint on the GPU. Alpha fades each cell as one flat
     * color. Changing the tint costs no vertex rewrite or upload. The batch must already be begun,
     * and the batch color is ignored. Call it once per frame on the GL thread.
     *
     * @param batch the active batch; not null
     * @param x screen x of the grid's top-left corner
     * @param y screen y of the grid's top-left corner (y grows upward)
     * @param tintRed red multiplier, 1 for unchanged
     * @param tintGreen green multiplier, 1 for unchanged
     * @param tintBlue blue multiplier, 1 for unchanged
     * @param tintAlpha alpha multiplier, 1 for opaque and 0 for invisible
     */
    public void draw(SpriteBatch batch, float x, float y,
            float tintRed, float tintGreen, float tintBlue, float tintAlpha) {
        tintOnlyEffects.reset();
        tintOnlyEffects.setTint(tintRed, tintGreen, tintBlue, tintAlpha);
        translation.setToTranslation(x, y);
        resolvedTintOnlyEffects.resolveOwn(tintOnlyEffects);
        draw(batch, translation, resolvedTintOnlyEffects);
    }

    /**
     * Draws the grid like {@link #draw(SpriteBatch, float, float)}, applying the effects on
     * the GPU in this order: saturation, tint, mix, invert, then alpha. Alpha fades each
     * cell as one flat color; mix, saturation, and invert never change it. Changing the effects costs no vertex
     * rewrite or upload.
     */
    void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects) {
        drawGrid(batch, layerTransform, effects, CellBlend.BATCH);
    }

    /**
     * Draws the grid, then the lifted cells, then the drawn sprites in order with the same
     * effects. Sprites are drawn even when no cell is visible, and always with the batch's
     * blending. With pixel snapping on, sprite and lifted cell positions are rounded; rotation and
     * scale are not.
     */
    void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects, Array<GlyphSprite> sprites,
            boolean pixelSnapping) {
        drawGrid(batch, layerTransform, effects, CellBlend.BATCH);
        drawSprites(batch, layerTransform, effects, sprites, pixelSnapping, CellBlend.BATCH);
    }

    /**
     * Like {@link #draw(SpriteBatch, Affine2, ResolvedEffects, Array, boolean)}, but blends into
     * a transparent offscreen target as premultiplied color, whatever the batch's blending is.
     */
    void drawPremultiplied(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
            Array<GlyphSprite> sprites, boolean pixelSnapping) {
        drawGrid(batch, layerTransform, effects, CellBlend.PREMULTIPLIED_TARGET);
        drawSprites(batch, layerTransform, effects, sprites, pixelSnapping, CellBlend.PREMULTIPLIED_TARGET);
    }

    private void drawSprites(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
            Array<GlyphSprite> sprites, boolean pixelSnapping, CellBlend blend) {
        int quadCount = SpriteMesh.countDrawnLifted(cellHandles, grid) + SpriteMesh.countDrawn(sprites);
        if (quadCount == 0) {
            return;
        }
        if (spriteMesh == null) {
            spriteMesh = new SpriteMesh(resources.getShader());
        }
        spriteMesh.upload(cellHandles, sprites, grid, quadCount, glyphSource, pixelSnapping);
        spriteMesh.draw(batch, glyphTexture, layerTransform, effects, blend);
    }

    private void rebuildIfStale() {
        if (glyphSourceChanged || grid.getColumns() != cachedColumns || grid.getRows() != cachedRows) {
            glyphSourceChanged = false;
            mesh.dispose();
            createCacheAndMesh();
        }
        indexNewHandles();
    }

    /**
     * Handles are only appended to the live list, and any removal comes with a grid size change,
     * which rebuilds the index from scratch. So indexing the tail since the last call is enough.
     */
    private void indexNewHandles() {
        if (cellHandles.size < indexedHandleCount) {
            Arrays.fill(handlesByCell, null);
            indexedHandleCount = 0;
        }
        for (int i = indexedHandleCount; i < cellHandles.size; i++) {
            indexHandle(cellHandles.get(i));
        }
        indexedHandleCount = cellHandles.size;
    }

    private void indexHandle(CellHandle handle) {
        if (handle.isDetached() || handle.getColumn() >= cachedColumns || handle.getRow() >= cachedRows) {
            return;
        }
        handlesByCell[handle.getRow() * cachedColumns + handle.getColumn()] = handle;
    }

    /**
     * Hides or shows one cell's glyph in the cell mesh; the background stays. Only that cell is
     * rewritten, and the meshes are uploaded at the next draw. If the grid size or glyph source
     * changed, the rebuild happens first and clears every hidden flag, so callers re-apply theirs.
     *
     * @throws IndexOutOfBoundsException if the cell is outside the grid
     */
    void setCellGlyphHidden(int column, int row, boolean hidden) {
        rebuildIfStale();
        requireInsideGrid(column, row);
        int cell = row * cachedColumns + column;
        if (hiddenGlyphs[cell] == hidden) {
            return;
        }
        hiddenGlyphs[cell] = hidden;
        rewriteSingleCell(column, row);
    }

    /**
     * Rewrites one cell from the grid and its handle's current overrides, and uploads at the next
     * draw. Does nothing if a full rebuild is pending, since that rewrites every cell anyway.
     *
     * @throws IndexOutOfBoundsException if the cell is outside the grid
     */
    void refreshCell(int column, int row) {
        rebuildIfStale();
        requireInsideGrid(column, row);
        rewriteSingleCell(column, row);
    }

    private void requireInsideGrid(int column, int row) {
        Guards.requireCellInside(column, row, cachedColumns, cachedRows);
    }

    private void rewriteSingleCell(int column, int row) {
        if (cache.isFullRebuildNeeded()) {
            return;
        }
        writeCell(column, row);
        uploadPending = true;
    }

    /** Sets the layer's live handle list; lifted ones draw as sprites. Not owned. */
    void setCellHandles(Array<CellHandle> cellHandles) {
        this.cellHandles = Objects.requireNonNull(cellHandles, "cellHandles");
        indexedHandleCount = 0;
        Arrays.fill(handlesByCell, null);
    }

    private void drawGrid(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects, CellBlend blend) {
        rebuildIfStale();
        if (cache.isFullRebuildNeeded()) {
            for (int row = 0; row < grid.getRows(); row++) {
                for (int column = 0; column < grid.getColumns(); column++) {
                    writeCell(column, row);
                }
            }
            cache.markRebuilt();
            tracker.markAllClean();
            uploadPending = false;
            mesh.upload(cache.getVertices());
        } else if (tracker.hasDirtyCells() || uploadPending) {
            tracker.forEachDirtyCell(cellWriter);
            tracker.markAllClean();
            uploadPending = false;
            mesh.upload(cache.getVertices());
        }
        mesh.draw(batch, glyphTexture, layerTransform, effects,
                cache.getNotOpaqueCellCount() == 0, cache.getVisibleCellCount() > 0, blend);
    }

    /**
     * Switches to another glyph source. The glyph texture is read from it now; its cell size is
     * read at the next draw, which rebuilds the cache and meshes and rewrites every cell (this
     * allocates, once per switch). The grid keeps its contents and its cell count. Glyphs
     * missing from the new source draw as its fallback. The source is not owned. Call it on the
     * GL thread.
     *
     * @param glyphSource the new source; not null. All its glyph regions must share one texture
     * @throws NullPointerException if the glyph source is null
     */
    public void setGlyphSource(GlyphSource glyphSource) {
        Objects.requireNonNull(glyphSource, "glyphSource");
        Texture newGlyphTexture = glyphSource.getGlyphRegion(' ').getTexture();
        this.glyphSource = glyphSource;
        glyphTexture = newGlyphTexture;
        glyphSourceChanged = true;
    }

    static boolean isBlankGlyph(int codePoint, int foreground) {
        return codePoint == ' ' || codePoint == TerminalGrid.EMPTY_CODE_POINT || CellVertexCache.isFullyTransparent(foreground);
    }

    private void writeCell(int column, int row) {
        int cell = row * cachedColumns + column;
        flatWriter.write(column, row, hiddenGlyphs[cell], handlesByCell[cell]);
    }

    /**
     * Releases the dirty tracker and frees the meshes. The shader is freed only if this renderer
     * created it. The grid and glyph source are not disposed. Call it once on the GL thread; the
     * renderer must not be drawn afterward.
     */
    @Override
    public void dispose() {
        tracker.release();
        mesh.dispose();
        if (spriteMesh != null) {
            spriteMesh.dispose();
        }
        if (ownsResources) {
            resources.dispose();
        }
    }
}
