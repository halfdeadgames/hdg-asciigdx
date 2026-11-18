package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/**
 * GPU quads for a layer's lifted cells and sprites, one quad per drawn lifted cell and per drawn
 * sprite (visible, color alpha above 0), lifted cells first. The quads are split into several
 * meshes (chunks) that fit 16-bit indices, like {@link GridMesh}. The chunks are created on the
 * first upload and the capacity grows by doubling, without an upper limit. Frames that need no
 * growth allocate nothing.
 */
final class SpriteMesh implements Disposable {

    static final int MIN_CAPACITY = 16;

    private final CellShaderPass pass;
    private float[] vertices = new float[0];
    private Mesh[] chunks;
    private int capacity;
    private int usedQuads;

    /** The shader is shared and not owned; it is not disposed with the mesh. */
    SpriteMesh(ShaderProgram shader) {
        pass = new CellShaderPass(shader);
    }

    /**
     * Returns the quad capacity to use so that {@code needed} quads fit, never less than
     * {@code current}. Up to one chunk it doubles from {@code current}, stopping at a full chunk;
     * past that it rounds up to whole chunks, so at most one chunk is partly empty.
     */
    static int grownCapacity(int current, int needed) {
        if (needed > CellVertexLayout.MAX_QUADS_PER_MESH) {
            return Math.max(current, wholeChunksFor(needed));
        }
        int grown = Math.max(current, MIN_CAPACITY);
        while (grown < needed) {
            grown *= 2;
        }
        return Math.max(current, Math.min(grown, CellVertexLayout.MAX_QUADS_PER_MESH));
    }

    private static int wholeChunksFor(int quads) {
        return CellVertexLayout.chunkCountFor(quads) * CellVertexLayout.MAX_QUADS_PER_MESH;
    }

    /** A sprite draws only when it is visible and its color alpha is above 0. */
    static boolean isDrawn(GlyphSprite sprite) {
        return sprite.isVisible() && sprite.getAlpha() > 0f;
    }

    static int countDrawn(Array<GlyphSprite> sprites) {
        int drawn = 0;
        for (int i = 0; i < sprites.size; i++) {
            if (isDrawn(sprites.get(i))) {
                drawn++;
            }
        }
        return drawn;
    }

    /**
     * Returns whether any handle is lifted; used by the fade rule, so a blank lifted cell counts
     * too.
     */
    static boolean hasLifted(Array<CellHandle> cells) {
        for (int i = 0; i < cells.size; i++) {
            if (cells.get(i).isLifted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts lifted cells that have a glyph to draw; blank cells and cells at cell alpha 0 draw
     * nothing.
     */
    static int countDrawnLifted(Array<CellHandle> cells, TerminalView grid) {
        int drawn = 0;
        for (int i = 0; i < cells.size; i++) {
            if (isDrawnLifted(cells.get(i), grid)) {
                drawn++;
            }
        }
        return drawn;
    }

    private static boolean isDrawnLifted(CellHandle cell, TerminalView grid) {
        if (!cell.isLifted() || !(cell.getCellAlpha() > 0f)) {
            return false;
        }
        return !GridRenderer.isBlankGlyph(resolvedCodePoint(cell, grid),
                resolvedForeground(cell, grid));
    }

    private static int resolvedCodePoint(CellHandle cell, TerminalView grid) {
        return CellResolver.codePoint(grid.getCodePoint(cell.getColumn(), cell.getRow()), cell);
    }

    private static int resolvedForeground(CellHandle cell, TerminalView grid) {
        return CellResolver.foreground(grid.getForeground(cell.getColumn(), cell.getRow()), cell);
    }

    /**
     * Writes the quads of the lifted cells (in handle creation order) and then every drawn
     * sprite (in order), and uploads only the chunks that hold used quads. {@code quadCount} must
     * be the number of drawn quads.
     */
    void upload(Array<CellHandle> cells, Array<GlyphSprite> sprites, TerminalView grid,
            int quadCount,
            GlyphSource glyphSource, boolean pixelSnapping) {
        ensureCapacity(quadCount);
        float cellWidth = glyphSource.getCellWidth();
        float cellHeight = glyphSource.getCellHeight();
        int quad = writeLiftedCells(cells, grid, glyphSource, pixelSnapping);
        quad = writeSprites(quad, sprites, glyphSource, cellWidth, cellHeight, pixelSnapping);
        usedQuads = quad;
        uploadChunks();
    }

    private int writeLiftedCells(Array<CellHandle> cells, TerminalView grid,
            GlyphSource glyphSource, boolean pixelSnapping) {
        int quad = 0;
        for (int i = 0; i < cells.size; i++) {
            CellHandle cell = cells.get(i);
            if (!isDrawnLifted(cell, grid)) {
                continue;
            }
            writeLiftedCell(vertices, quad++, cell, grid, glyphSource, pixelSnapping);
        }
        return quad;
    }

    private int writeSprites(int firstQuad, Array<GlyphSprite> sprites,
            GlyphSource glyphSource,
            float cellWidth, float cellHeight, boolean pixelSnapping) {
        int quad = firstQuad;
        for (int i = 0; i < sprites.size; i++) {
            GlyphSprite sprite = sprites.get(i);
            if (!isDrawn(sprite)) {
                continue;
            }
            writeSprite(quad++, sprite, glyphSource, cellWidth, cellHeight, pixelSnapping);
        }
        return quad;
    }

    /**
     * Writes one quad for a lifted cell: the cell's current glyph and foreground with the handle's
     * temporary overrides applied, placed at the cell's position plus the handle's offset, with the
     * handle's rotation, scale, and pivot. The handle's foreground alpha is already part of the
     * resolved foreground, so it is not applied again here; the handle's cell alpha is written into
     * the quad. The cell must not be blank.
     */
    static void writeLiftedCell(float[] vertices, int quad, CellHandle cell, TerminalView grid,
            GlyphSource glyphSource, boolean pixelSnapping) {
        float cellWidth = glyphSource.getCellWidth();
        float cellHeight = glyphSource.getCellHeight();
        TextureRegion region = glyphSource.getGlyphRegion(resolvedCodePoint(cell, grid));
        int color = resolvedForeground(cell, grid);
        float x = cell.getColumn() * cellWidth + cell.getOffsetX();
        float y = cell.getRow() * cellHeight + cell.getOffsetY();
        if (pixelSnapping) {
            x = ClipMath.snap(x);
            y = ClipMath.snap(y);
        }
        SpriteQuadWriter.writeQuad(vertices, quad * CellVertexLayout.FLOATS_PER_QUAD, cell, color,
                x, y,
                cellWidth, cellHeight, region.getU(), region.getV(), region.getU2(),
                region.getV2(), cell.getCellAlpha());
    }

    private void writeSprite(int quad, GlyphSprite sprite, GlyphSource glyphSource, float cellWidth,
            float cellHeight, boolean pixelSnapping) {
        TextureRegion region = glyphSource.getGlyphRegion(sprite.getCodePoint());
        float x = pixelSnapping ? ClipMath.snap(sprite.getX()) : sprite.getX();
        float y = pixelSnapping ? ClipMath.snap(sprite.getY()) : sprite.getY();
        SpriteQuadWriter.writeQuad(vertices, quad * CellVertexLayout.FLOATS_PER_QUAD, sprite, x, y,
                cellWidth, cellHeight, region.getU(), region.getV(), region.getU2(),
                region.getV2());
    }

    private void ensureCapacity(int quads) {
        if (chunks != null && quads <= capacity) {
            return;
        }
        dispose();
        capacity = grownCapacity(capacity, quads);
        vertices = new float[capacity * CellVertexLayout.FLOATS_PER_QUAD];
        chunks = createChunks();
    }

    private Mesh[] createChunks() {
        short[] indices = GridMesh.createQuadIndices(
                Math.min(capacity, CellVertexLayout.MAX_QUADS_PER_MESH));
        Mesh[] created = new Mesh[CellVertexLayout.chunkCountFor(capacity)];
        for (int chunk = 0; chunk < created.length; chunk++) {
            int quads = CellVertexLayout.quadsInChunk(capacity, chunk);
            int indexCount = quads * CellVertexLayout.INDICES_PER_QUAD;
            created[chunk] = new Mesh(false, quads * CellVertexLayout.VERTICES_PER_QUAD,
                    indexCount, CellVertexLayout.createAttributes());
            created[chunk].setIndices(indices, 0, indexCount);
        }
        return created;
    }

    private void uploadChunks() {
        int usedChunks = CellVertexLayout.chunkCountFor(usedQuads);
        for (int chunk = 0; chunk < usedChunks; chunk++) {
            int offset = chunk * CellVertexLayout.MAX_QUADS_PER_MESH
                    * CellVertexLayout.FLOATS_PER_QUAD;
            int count = CellVertexLayout.quadsInChunk(usedQuads, chunk)
                    * CellVertexLayout.FLOATS_PER_QUAD;
            chunks[chunk].setVertices(vertices, offset, count);
        }
    }

    /**
     * Draws the uploaded quads like {@link GridMesh#draw}, always blended (never treated as
     * opaque). Draws nothing before the first upload or when no quad is used.
     */
    void draw(SpriteBatch batch, Texture glyphTexture, Affine2 layerTransform,
            ResolvedEffects effects,
            CellBlend blend) {
        if (usedQuads == 0) {
            return;
        }
        pass.begin(batch, glyphTexture, layerTransform, effects, false, blend);
        int usedChunks = CellVertexLayout.chunkCountFor(usedQuads);
        for (int chunk = 0; chunk < usedChunks; chunk++) {
            int indexCount = CellVertexLayout.quadsInChunk(usedQuads, chunk)
                    * CellVertexLayout.INDICES_PER_QUAD;
            chunks[chunk].render(pass.getShader(), GL20.GL_TRIANGLES, 0, indexCount);
        }
        pass.end(batch);
    }

    @Override
    public void dispose() {
        if (chunks == null) {
            return;
        }
        for (int chunk = 0; chunk < chunks.length; chunk++) {
            chunks[chunk].dispose();
        }
        chunks = null;
        usedQuads = 0;
    }
}
