package com.github.halfdeadgames.asciigdx.layer;

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
    private long resolveId;

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
    void resolveOwn(ColorEffects own) {
        own.writeColorMatrix(matrix);
        alpha = own.getTintAlpha();
    }

    /**
     * Resolves the layer's own effects, then the effects of every group it reaches, directly or as an
     * ancestor: the color transforms run in sequence and the alphas multiply. A group's effects run
     * after those of every descendant group the layer is in; groups that are not ancestors of each
     * other run in the order the layer joined them. Each reached group is applied once.
     */
    void resolve(ColorEffects own, Array<LayerGroup> groups) {
        resolveOwn(own);
        resolveId++;
        for (int i = 0; i < groups.size; i++) {
            markChain(groups.get(i));
        }
        for (int i = 0; i < groups.size; i++) {
            applyWhenReady(groups.get(i));
        }
    }

    /**
     * Marks the group and its ancestors as reached and counts, on each parent, how many reached
     * children must be applied before it. Stops at an already marked group, since its ancestors are
     * already counted.
     */
    private void markChain(LayerGroup start) {
        for (LayerGroup group = start; group != null && !group.isMarkedIn(resolveId); group = group.getParent()) {
            group.markIn(resolveId);
            LayerGroup parent = group.getParent();
            if (parent != null) {
                parent.addPendingChildIn(resolveId);
            }
        }
    }

    /**
     * Applies the group unless it is already applied or still waits on a child, then moves up to each
     * parent that has no children left to wait on.
     */
    private void applyWhenReady(LayerGroup start) {
        LayerGroup group = start;
        while (group != null && !group.isAppliedIn(resolveId) && !group.hasPendingChildrenIn(resolveId)) {
            applyGroup(group);
            LayerGroup parent = group.getParent();
            if (parent != null) {
                parent.finishPendingChild();
            }
            group = parent;
        }
    }

    private void applyGroup(LayerGroup group) {
        group.markAppliedIn(resolveId);
        foldGroup(group.getColorEffects());
    }

    private void foldGroup(ColorEffects groupEffects) {
        groupEffects.writeColorMatrix(groupMatrix);
        matrix.then(groupMatrix);
        alpha *= groupEffects.getTintAlpha();
    }
}
