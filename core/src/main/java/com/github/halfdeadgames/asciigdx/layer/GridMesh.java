package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Disposable;

/**
 * GPU-resident quads for a grid: one quad per cell, split into chunks that fit
 * 16-bit indices. Vertices come from a {@link CellVertexCache} and are re-uploaded
 * only when {@link #upload} is called.
 */
final class GridMesh implements Disposable {

    private final int quadCount;
    private final Mesh[] chunks;
    private final CellShaderPass pass;

    /** The shader is shared and not owned; it is not disposed with the mesh. */
    GridMesh(int quadCount, ShaderProgram shader) {
        this.quadCount = quadCount;
        this.pass = new CellShaderPass(shader);
        short[] indices = createQuadIndices(Math.min(quadCount, CellVertexLayout.MAX_QUADS_PER_MESH));
        chunks = createChunks(CellVertexLayout.chunkCountFor(quadCount), indices);
    }

    private Mesh[] createChunks(int chunkCount, short[] indices) {
        Mesh[] created = new Mesh[chunkCount];
        for (int chunk = 0; chunk < chunkCount; chunk++) {
            int quads = CellVertexLayout.quadsInChunk(quadCount, chunk);
            int indexCount = quads * CellVertexLayout.INDICES_PER_QUAD;
            created[chunk] = new Mesh(false, quads * CellVertexLayout.VERTICES_PER_QUAD, indexCount,
                    CellVertexLayout.createAttributes());
            created[chunk].setIndices(indices, 0, indexCount);
        }
        return created;
    }

    static short[] createQuadIndices(int quads) {
        short[] indices = new short[quads * CellVertexLayout.INDICES_PER_QUAD];
        for (int quad = 0, index = 0, vertex = 0; quad < quads; quad++, vertex += CellVertexLayout.VERTICES_PER_QUAD) {
            indices[index++] = (short) vertex;
            indices[index++] = (short) (vertex + 1);
            indices[index++] = (short) (vertex + 2);
            indices[index++] = (short) (vertex + 2);
            indices[index++] = (short) (vertex + 3);
            indices[index++] = (short) vertex;
        }
        return indices;
    }

    /** Copies the full vertex array to the GPU. */
    void upload(float[] vertices) {
        for (int chunk = 0; chunk < chunks.length; chunk++) {
            int start = chunk * CellVertexLayout.MAX_QUADS_PER_MESH * CellVertexLayout.FLOATS_PER_QUAD;
            int count = CellVertexLayout.quadsInChunk(quadCount, chunk) * CellVertexLayout.FLOATS_PER_QUAD;
            chunks[chunk].setVertices(vertices, start, count);
        }
    }

    /**
     * Draws every cell in one pass using the batch's projection, transform, and
     * blending, followed by the layer transform. The shader draws each glyph over its
     * background and then applies the layer effects; the batch's blend functions decide only
     * how the finished cell combines with what is below. The batch must be begun.
     * Leaves the batch's shader bound.
     */
    void draw(SpriteBatch batch, Texture glyphTexture, Affine2 layerTransform,
            ResolvedEffects effects, boolean allCellsOpaque, boolean hasVisibleCells, CellBlend blend) {
        if (!hasVisibleCells) {
            return;
        }
        pass.begin(batch, glyphTexture, layerTransform, effects, allCellsOpaque, blend);
        renderChunks();
        pass.end(batch);
    }

    private void renderChunks() {
        for (int chunk = 0; chunk < chunks.length; chunk++) {
            int indexCount = CellVertexLayout.quadsInChunk(quadCount, chunk) * CellVertexLayout.INDICES_PER_QUAD;
            chunks[chunk].render(pass.getShader(), GL20.GL_TRIANGLES, 0, indexCount);
        }
    }

    @Override
    public void dispose() {
        for (int chunk = 0; chunk < chunks.length; chunk++) {
            chunks[chunk].dispose();
        }
    }
}
