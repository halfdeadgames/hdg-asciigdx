package com.github.halfdeadgames.asciigdx.render;

/** How a layer's zoom factor is applied. */
public enum ZoomMode {
    /**
     * Uses the zoom as given. With nearest filtering, a fractional zoom gives uneven pixel widths.
     */
    SMOOTH,

    /**
     * Rounds the zoom to the nearest whole number, never below 1, and snaps the layer's top-left
     * corner to a whole pixel, so every font pixel is exactly N by N. Crisp only at rotations that
     * are multiples of 90 degrees.
     */
    WHOLE
}
