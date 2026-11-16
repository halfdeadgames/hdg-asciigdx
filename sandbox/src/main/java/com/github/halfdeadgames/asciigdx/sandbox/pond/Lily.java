package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** Game state of one lily pad; the display reads it every frame. */
public final class Lily {

    private static final int BULLET_CODE_POINT = 0x2022;

    /** The color of a lily nobody recolored, as RGBA. */
    public static final int BASE_COLOR = LilyPalette.BASE_GREEN;

    private final SpringOffset motion = new SpringOffset();
    private float pendingPushX;
    private float pendingPushY;
    private float pendingPushDelaySeconds;
    private boolean hasPendingPush;
    private float scale = 1f;
    private int color = BASE_COLOR;

    /** Returns the lily's color as RGBA. */
    public int getColor() {
        return color;
    }

    /** Sets the lily's color as RGBA. */
    public void setColor(int color) {
        this.color = color;
    }

    /** Returns the glyph code point a lily is drawn with. */
    public int codePoint() {
        return BULLET_CODE_POINT;
    }

    /** Returns the horizontal offset in lily cells; 0 is centered on the slot. */
    public float getOffsetX() {
        return motion.getOffsetX();
    }

    /** Returns the vertical offset in lily cells; 0 is centered on the slot. */
    public float getOffsetY() {
        return motion.getOffsetY();
    }

    /** Pushes the lily, adding to its velocity in lily cells per second. */
    public void push(float velocityX, float velocityY) {
        motion.push(velocityX, velocityY);
    }

    /**
     * Pushes the lily after a delay; a new call replaces a push that is still pending.
     *
     * @param delaySeconds seconds of {@link #advance} time before the push lands
     */
    public void pushLater(float velocityX, float velocityY, float delaySeconds) {
        pendingPushX = velocityX;
        pendingPushY = velocityY;
        pendingPushDelaySeconds = delaySeconds;
        hasPendingPush = true;
    }

    /** Moves the point the lily's spring settles at, in lily cells from the slot center. */
    public void setTarget(float x, float y) {
        motion.setTarget(x, y);
    }

    /** Returns the horizontal rest point in lily cells. */
    public float getTargetX() {
        return motion.getTargetX();
    }

    /** Returns the vertical rest point in lily cells. */
    public float getTargetY() {
        return motion.getTargetY();
    }

    /** Returns the horizontal velocity in lily cells per second. */
    public float getVelocityX() {
        return motion.getVelocityX();
    }

    /** Returns the vertical velocity in lily cells per second. */
    public float getVelocityY() {
        return motion.getVelocityY();
    }

    /** Steps the lily's spring motion forward by the given seconds. */
    public void advance(float deltaSeconds) {
        if (deltaSeconds > 0f) {
            applyPendingPushWhenDue(deltaSeconds);
        }
        motion.advance(deltaSeconds);
    }

    private void applyPendingPushWhenDue(float deltaSeconds) {
        if (!hasPendingPush) {
            return;
        }
        pendingPushDelaySeconds -= deltaSeconds;
        if (pendingPushDelaySeconds > 0f) {
            return;
        }
        motion.push(pendingPushX, pendingPushY);
        hasPendingPush = false;
    }

    /** Returns true when the lily is on its rest point, not moving, and has no push pending. */
    public boolean isAtRest() {
        return !hasPendingPush && motion.isAtRest();
    }

    /** Puts the lily back at its slot center at rest, with target 0,0 and no push pending. */
    public void reset() {
        motion.reset();
        hasPendingPush = false;
        pendingPushX = 0f;
        pendingPushY = 0f;
        pendingPushDelaySeconds = 0f;
    }

    /**
     * Returns true when the lily is at rest, has no push pending, and its target is exactly 0,0.
     */
    public boolean isHome() {
        return isAtRest() && motion.getTargetX() == 0f && motion.getTargetY() == 0f;
    }

    /** Returns the draw scale; 1 is the normal size. */
    public float getScale() {
        return scale;
    }

    /** Sets the draw scale. */
    public void setScale(float scale) {
        this.scale = scale;
    }
}
