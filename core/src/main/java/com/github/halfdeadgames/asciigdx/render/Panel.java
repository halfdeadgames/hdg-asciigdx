package com.github.halfdeadgames.asciigdx.render;

/**
 * An axis-aligned rectangle in a {@link LayerStack}'s pixel space that layers can be placed in (see
 * {@link Layer#setPanel}). Created only by the stack.
 *
 * <p>The rectangle is measured in virtual pixels from the stack's top-left corner, with y growing
 * downward. Its width and height are never negative. Member layers read the rectangle every time
 * they draw or are queried, so moving or resizing a panel moves and clips its members at once.
 *
 * <p>Member layers currently follow the stack camera and their parallax; this will change when
 * panels get their own camera.
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
