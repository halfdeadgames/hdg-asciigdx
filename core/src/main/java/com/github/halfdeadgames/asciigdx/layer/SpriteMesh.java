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
import com.github.halfdeadgames.asciigdx.color.CellColor;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/**
 * GPU quads for a layer's lifted cells and sprites, one quad per drawn lifted cell and per drawn
 * sprite (visible, color alpha above 0), lifted cells first. The mesh is created on the first
 * upload and grows by doubling. Frames that need no growth allocate nothing.
 */
final class SpriteMesh implements Disposable {

    static final int MIN_CAPACITY = 16;

    private final CellShaderPass pass;
    private float[] vertices = new float[0];
    private Mesh mesh;
    private int capacity;
    private int usedQuads;

    /** The shader is shared and not owned; it is not disposed with the mesh. */
    SpriteMesh(ShaderProgram shader) {
        pass = new CellShaderPass(shader);
    }

    /** Returns the quad capacity to use so that {@code needed} quads fit, doubling from {@code current}. */
    static int grownCapacity(int current, int needed) {
        int grown = Math.max(current, MIN_CAPACITY);
        while (grown < needed && grown < CellVertexLayout.MAX_QUADS_PER_MESH) {
            grown *= 2;
        }
        return Math.min(grown, CellVertexLayout.MAX_QUADS_PER_MESH);
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

    /** Returns whether any handle is lifted; used by the fade rule, so a blank lifted cell counts too. */
    static boolean hasLifted(Array<CellHandle> cells) {
        for (int i = 0; i < cells.size; i++) {
            if (cells.get(i).isLifted()) {
                return true;
            }
        }
        return false;
    }

    /** Counts lifted cells that have a glyph to draw; blank cells draw nothing. */
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
        if (!cell.isLifted()) {
            return false;
        }
        return !GridRenderer.isBlankGlyph(resolvedCodePoint(cell, grid), resolvedForeground(cell, grid));
    }

    private static int resolvedCodePoint(CellHandle cell, TerminalView grid) {
        return CellResolver.codePoint(grid.getCodePoint(cell.getColumn(), cell.getRow()), cell);
    }

    private static int resolvedForeground(CellHandle cell, TerminalView grid) {
        return CellResolver.foreground(grid.getForeground(cell.getColumn(), cell.getRow()), cell);
    }

    /**
     * Writes the quads of the lifted cells (in handle creation order) and then every drawn
     * sprite (in order), and uploads only the used floats. Quads beyond {@code quadCount}, which
     * is capped at the mesh limit, are dropped.
     */
    void upload(Array<CellHandle> cells, Array<GlyphSprite> sprites, TerminalView grid, int quadCount,
            GlyphSource glyphSource, boolean pixelSnapping) {
        int limit = Math.min(quadCount, CellVertexLayout.MAX_QUADS_PER_MESH);
        ensureCapacity(limit);
        float cellWidth = glyphSource.getCellWidth();
        float cellHeight = glyphSource.getCellHeight();
        int quad = writeLiftedCells(cells, grid, limit, glyphSource, pixelSnapping);
        quad = writeSprites(quad, sprites, limit, glyphSource, cellWidth, cellHeight, pixelSnapping);
        usedQuads = quad;
        mesh.setVertices(vertices, 0, usedQuads * CellVertexLayout.FLOATS_PER_QUAD);
    }

    private int writeLiftedCells(Array<CellHandle> cells, TerminalView grid, int limit,
            GlyphSource glyphSource, boolean pixelSnapping) {
        int quad = 0;
        for (int i = 0; i < cells.size && quad < limit; i++) {
            CellHandle cell = cells.get(i);
            if (!isDrawnLifted(cell, grid)) {
                continue;
            }
            writeLiftedCell(vertices, quad++, cell, grid, glyphSource, pixelSnapping);
        }
        return quad;
    }

    private int writeSprites(int firstQuad, Array<GlyphSprite> sprites, int limit, GlyphSource glyphSource,
            float cellWidth, float cellHeight, boolean pixelSnapping) {
        int quad = firstQuad;
        for (int i = 0; i < sprites.size && quad < limit; i++) {
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
     * handle's rotation, scale, and pivot, and its alpha multiplied into the glyph alpha. The cell
     * must not be blank.
     */
    static void writeLiftedCell(float[] vertices, int quad, CellHandle cell, TerminalView grid,
            GlyphSource glyphSource, boolean pixelSnapping) {
        float cellWidth = glyphSource.getCellWidth();
        float cellHeight = glyphSource.getCellHeight();
        TextureRegion region = glyphSource.getGlyphRegion(resolvedCodePoint(cell, grid));
        int color = CellColor.withAlphaScaled(resolvedForeground(cell, grid), cell.getAlphaMultiplier());
        float x = cell.getColumn() * cellWidth + cell.getOffsetX();
        float y = cell.getRow() * cellHeight + cell.getOffsetY();
        if (pixelSnapping) {
            x = ClipMath.snap(x);
            y = ClipMath.snap(y);
        }
        SpriteQuadWriter.writeQuad(vertices, quad * CellVertexLayout.FLOATS_PER_QUAD, cell, color, x, y,
                cellWidth, cellHeight, region.getU(), region.getV(), region.getU2(), region.getV2());
    }

    private void writeSprite(int quad, GlyphSprite sprite, GlyphSource glyphSource, float cellWidth,
            float cellHeight, boolean pixelSnapping) {
        TextureRegion region = glyphSource.getGlyphRegion(sprite.getCodePoint());
        float x = pixelSnapping ? ClipMath.snap(sprite.getX()) : sprite.getX();
        float y = pixelSnapping ? ClipMath.snap(sprite.getY()) : sprite.getY();
        SpriteQuadWriter.writeQuad(vertices, quad * CellVertexLayout.FLOATS_PER_QUAD, sprite, x, y,
                cellWidth, cellHeight, region.getU(), region.getV(), region.getU2(), region.getV2());
    }

    private void ensureCapacity(int quads) {
        if (mesh != null && quads <= capacity) {
            return;
        }
        if (mesh != null) {
            mesh.dispose();
        }
        capacity = grownCapacity(capacity, quads);
        vertices = new float[capacity * CellVertexLayout.FLOATS_PER_QUAD];
        int indexCount = capacity * CellVertexLayout.INDICES_PER_QUAD;
        mesh = new Mesh(false, capacity * CellVertexLayout.VERTICES_PER_QUAD, indexCount,
                CellVertexLayout.createAttributes());
        mesh.setIndices(GridMesh.createQuadIndices(capacity), 0, indexCount);
    }

    /** Draws the uploaded quads like {@link GridMesh#draw}, always blended (never treated as opaque). */
    void draw(SpriteBatch batch, Texture glyphTexture, Affine2 layerTransform, ResolvedEffects effects,
            CellBlend blend) {
        pass.begin(batch, glyphTexture, layerTransform, effects, false, blend);
        mesh.render(pass.getShader(), GL20.GL_TRIANGLES, 0, usedQuads * CellVertexLayout.INDICES_PER_QUAD);
        pass.end(batch);
    }

    @Override
    public void dispose() {
        if (mesh == null) {
            return;
        }
        mesh.dispose();
        mesh = null;
    }
}
