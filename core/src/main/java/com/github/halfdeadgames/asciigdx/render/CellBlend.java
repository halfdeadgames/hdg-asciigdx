package com.github.halfdeadgames.asciigdx.render;

/** How a cell-shader pass combines its output with what is already in the target. */
enum CellBlend {

    /** The batch's blend functions; blending is off if the batch has it off. */
    BATCH,

    /**
     * Accumulates into a transparent offscreen target as premultiplied color: separate blend
     * functions (SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ONE_MINUS_SRC_ALPHA), applied whatever the
     * batch's blending is, so overlapping sprites build up correct alpha.
     */
    PREMULTIPLIED_TARGET
}
