package com.github.halfdeadgames.asciigdx.shape;

/** Whether a shape covers all of its cells or only its border. */
public enum ShapeFill {
    /** Every cell of the shape. */
    FILLED,
    /**
     * Only the cells that have at least one of their four side neighbors outside the filled shape.
     */
    OUTLINE
}
