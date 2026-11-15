package com.github.halfdeadgames.asciigdx.render;

/**
 * An axis-aligned rectangle in a {@link LayerStack}'s pixel space that layers can be placed in (see
 * {@link Layer#setPanel}). Created only by the stack.
 *
 * <p>The rectangle is measured in virtual pixels from the stack's top-left corner, with y growing
 * downward. Its width and height are never negative. Member layers read the rectangle every time
 * they draw or are queried, so moving or resizing a panel moves and clips its members at once.
 *
 * <p>Member layers follow the panel's camera, not the stack camera, scaled by their parallax. The
 * panel's clip does not move with its camera.
 *
 * <p>Once the panel is removed from its stack, getters keep working and the setters throw
 * {@link IllegalStateException}.
 */
public final class Panel {

    private LayerStack owner;
    private float x;
    private float y;
    private float width;
    private float height;
    private float cameraX;
    private float cameraY;

    Panel(LayerStack owner, float x, float y, float width, float height) {
        requireValidBounds(x, y, width, height);
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Returns the left edge in virtual pixels from the stack's left edge. */
    public float getX() {
        return x;
    }

    /** Returns the top edge in virtual pixels from the stack's top edge, growing downward. */
    public float getY() {
        return y;
    }

    /** Returns the width in virtual pixels, never negative. */
    public float getWidth() {
        return width;
    }

    /** Returns the height in virtual pixels, never negative. */
    public float getHeight() {
        return height;
    }

    /**
     * Moves the panel's top-left corner, keeping its size.
     *
     * @throws IllegalArgumentException if x or y is not finite
     * @throws IllegalStateException if the panel was removed
     */
    public void setPosition(float x, float y) {
        requireAttached();
        requireValidBounds(x, y, width, height);
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the position and size.
     *
     * @throws IllegalArgumentException if a value is not finite, or width or height is negative
     * @throws IllegalStateException if the panel was removed
     */
    public void setBounds(float x, float y, float width, float height) {
        requireAttached();
        requireValidBounds(x, y, width, height);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Sets the camera position in virtual pixels, in the same space and direction as
     * {@link LayerStack#setCamera} (y grows downward). Each member layer shifts by minus the camera
     * times its parallax factor. Default (0, 0).
     *
     * @throws IllegalArgumentException if x or y is not finite
     * @throws IllegalStateException if the panel was removed
     */
    public void setCamera(float x, float y) {
        requireAttached();
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("camera must be finite: " + x + ", " + y);
        }
        cameraX = x;
        cameraY = y;
        owner.followCameraOfLayersIn(this);
    }

    /** Returns the camera's x in virtual pixels, 0 by default. */
    public float getCameraX() {
        return cameraX;
    }

    /** Returns the camera's y in virtual pixels, growing downward, 0 by default. */
    public float getCameraY() {
        return cameraY;
    }

    LayerStack getOwner() {
        return owner;
    }

    void detach() {
        owner = null;
    }

    private void requireAttached() {
        if (owner == null) {
            throw new IllegalStateException("Panel was removed from its stack");
        }
    }

    private static void requireValidBounds(float x, float y, float width, float height) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(width) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("panel bounds must be finite: " + x + ", " + y + ", "
                    + width + ", " + height);
        }
        if (width < 0f || height < 0f) {
            throw new IllegalArgumentException("panel width and height must not be negative: "
                    + width + ", " + height);
        }
    }
}
