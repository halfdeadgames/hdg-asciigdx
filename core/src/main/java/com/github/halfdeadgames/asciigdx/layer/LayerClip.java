package com.github.halfdeadgames.asciigdx.layer;

/** Mutable clip state of one layer: no clip, a fixed rectangle, or the stack's area. */
final class LayerClip {

    private enum Mode { NONE, RECT, STACK }

    private Mode mode = Mode.NONE;
    private float x;
    private float y;
    private float width;
    private float height;

    void setRect(float x, float y, float width, float height) {
        this.mode = Mode.RECT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    void setToStack() {
        resetTo(Mode.STACK);
    }

    void clear() {
        resetTo(Mode.NONE);
    }

    boolean isClipping() {
        return mode != Mode.NONE;
    }

    boolean isStackArea() {
        return mode == Mode.STACK;
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }

    float getWidth() {
        return width;
    }

    float getHeight() {
        return height;
    }

    private void resetTo(Mode newMode) {
        mode = newMode;
        x = 0f;
        y = 0f;
        width = 0f;
        height = 0f;
    }
}
