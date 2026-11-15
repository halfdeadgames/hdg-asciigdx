package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;

/**
 * The GL state shared by everything drawn with the cell shader: blending, matrix, texture, and
 * effect uniforms. The shader is shared and not owned.
 */
final class CellShaderPass {

    private final ShaderProgram shader;
    private final Matrix4 combinedMatrix = new Matrix4();
    private final Matrix4 layerMatrix = new Matrix4();
    private final ColorMatrix colorMatrix = new ColorMatrix();

    CellShaderPass(ShaderProgram shader) {
        this.shader = shader;
    }

    ShaderProgram getShader() {
        return shader;
    }

    /**
     * Flushes the batch and prepares the cell shader with the batch's projection, transform, and
     * blending (or the blend requested), followed by the layer transform. Blending is switched off
     * only when the caller says everything drawn is opaque and the tint alpha is 1, or when the
     * batch has blending off and the batch blend is requested. The batch must be begun.
     */
    void begin(SpriteBatch batch, Texture glyphTexture, Affine2 layerTransform,
            LayerEffects effects, boolean allOpaque, CellBlend blend) {
        batch.flush();
        applyBlend(batch, blend, allOpaque && effects.getTintA() == 1f);
        layerMatrix.set(layerTransform);
        combinedMatrix.set(batch.getProjectionMatrix()).mul(batch.getTransformMatrix()).mul(layerMatrix);
        shader.bind();
        shader.setUniformMatrix("u_projTrans", combinedMatrix);
        shader.setUniformi("u_texture", 0);
        setEffectUniforms(effects);
        glyphTexture.bind(0);
    }

    /** Rebinds the batch's own shader. */
    void end(SpriteBatch batch) {
        batch.getShader().bind();
    }

    private void setEffectUniforms(LayerEffects effects) {
        effects.writeColorMatrix(colorMatrix);
        shader.setUniformMatrix4fv("u_colorMatrix", colorMatrix.getValues(), 0, ColorMatrix.SIZE);
        shader.setUniformf("u_alpha", effects.getTintA());
    }

    private static void applyBlend(SpriteBatch batch, CellBlend blend, boolean opaque) {
        if (opaque || (blend == CellBlend.BATCH && !batch.isBlendingEnabled())) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
            return;
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        if (blend == CellBlend.PREMULTIPLIED_TARGET) {
            Gdx.gl.glBlendFuncSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                    GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
            return;
        }
        Gdx.gl.glBlendFuncSeparate(batch.getBlendSrcFunc(), batch.getBlendDstFunc(),
                batch.getBlendSrcFuncAlpha(), batch.getBlendDstFuncAlpha());
    }
}
