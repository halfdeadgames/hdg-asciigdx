package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.utils.Array;

/**
 * The color effects the cell shader applies to one layer this frame: a layer's own effects with the
 * effects of its groups folded on top. One instance is reused for every layer drawn, so resolving
 * allocates nothing.
 */
final class ResolvedEffects {

    private final ColorMatrix matrix = new ColorMatrix();
    private final ColorMatrix groupMatrix = new ColorMatrix();
    private float alpha = 1f;

    ColorMatrix getMatrix() {
        return matrix;
    }

    float getAlpha() {
        return alpha;
    }

    void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    /** Resolves effects that belong to no group. */
    void resolveOwn(LayerEffects own) {
        own.writeColorMatrix(matrix);
        alpha = own.getTintA();
    }

    /**
     * Resolves the layer's own effects, then each group's in the order the layer joined them: the
     * color transforms run in that sequence and the alphas multiply.
     */
    void resolve(LayerEffects own, Array<LayerGroup> groups) {
        resolveOwn(own);
        for (int i = 0; i < groups.size; i++) {
            foldGroup(groups.get(i).getEffects());
        }
    }

    private void foldGroup(LayerEffects groupEffects) {
        groupEffects.writeColorMatrix(groupMatrix);
        matrix.then(groupMatrix);
        alpha *= groupEffects.getTintA();
    }
}
