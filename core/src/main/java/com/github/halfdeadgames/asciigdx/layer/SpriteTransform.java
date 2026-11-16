package com.github.halfdeadgames.asciigdx.layer;

/** The rotation, scale, and pivot of a drawn glyph quad; what {@link SpriteQuadWriter} needs besides position and color. */
interface SpriteTransform {

    /** Rotation in degrees, positive is clockwise on screen. */
    float getRotation();

    float getScaleX();

    float getScaleY();

    /** Pivot as a fraction of the cell box, 0 is left. */
    float getPivotX();

    /** Pivot as a fraction of the cell box, 0 is top. */
    float getPivotY();
}
