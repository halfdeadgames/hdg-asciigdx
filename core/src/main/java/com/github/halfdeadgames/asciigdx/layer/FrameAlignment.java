package com.github.halfdeadgames.asciigdx.layer;

/**
 * Says where a map that is smaller than the view sits inside the view along one axis. Used by
 * {@link CameraFraming#centerOn}. It has no effect when the map is at least as large as the
 * view.
 */
public enum FrameAlignment {

    /** The map touches the left edge of the view on x, or the top edge on y (y grows down). */
    START,

    /** The map is centered in the view, with equal empty space on both sides. */
    CENTER,

    /** The map touches the right edge of the view on x, or the bottom edge on y. */
    END
}
