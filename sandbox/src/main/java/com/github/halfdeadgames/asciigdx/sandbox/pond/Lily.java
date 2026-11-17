package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.badlogic.gdx.math.Vector2;
import com.github.halfdeadgames.asciigdx.tween.DampedSpring;
import com.github.halfdeadgames.asciigdx.tween.SoftLimit;

/**
 * Game state of one lily pad; the display reads it every frame. Motion is a sum of stored spring
 * events (pushes and rest point moves) evaluated at the scene time, so it does not depend on the
 * frame rate.
 */
public final class Lily {

    private static final int BULLET_CODE_POINT = 0x2022;
    private static final DampedSpring SPRING = DampedSpring.ofFrequency(1.2f, 0.3f);
    private static final SoftLimit LIMIT = SoftLimit.of(0.5f, 0.75f);
    /** About a sixth of a source pixel on an 8 px lily cell, so the snap to rest is invisible. */
    private static final float SETTLE_TOLERANCE = 0.02f;
    private static final int EVENT_CAPACITY = 16;

    /** The color of a lily nobody recolored, as RGBA. */
    public static final int BASE_COLOR = LilyPalette.BASE_GREEN;

    private final boolean[] isPush = new boolean[EVENT_CAPACITY];
    private final float[] eventX = new float[EVENT_CAPACITY];
    private final float[] eventY = new float[EVENT_CAPACITY];
    private final float[] startSeconds = new float[EVENT_CAPACITY];
    private final float[] settledAtSeconds = new float[EVENT_CAPACITY];
    private final Vector2 limitedOffset = new Vector2();
    private int eventCount;
    private float baseX;
    private float baseY;
    private float targetX;
    private float targetY;
    private float offsetX;
    private float offsetY;
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

    /**
     * Returns the horizontal offset in lily cells as of the last {@link #advanceTo}; 0 is centered
     * on the slot.
     */
    public float getOffsetX() {
        return offsetX;
    }

    /**
     * Returns the vertical offset in lily cells as of the last {@link #advanceTo}; 0 is centered on
     * the slot.
     */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Pushes the lily with a velocity in lily cells per second; pushes accumulate. A push that
     * starts later than the current time has no effect until then.
     *
     * @param startSeconds scene time at which the push lands
     */
    public void push(float velocityX, float velocityY, float startSeconds) {
        var pushSettleSeconds = SPRING.getImpulseSettleSeconds(
                length(velocityX, velocityY), SETTLE_TOLERANCE);
        addEvent(true, velocityX, velocityY, startSeconds, startSeconds + pushSettleSeconds);
    }

    /**
     * Moves the point the lily's spring settles at, in lily cells from the slot center; does
     * nothing when the point does not change.
     *
     * @param startSeconds scene time at which the rest point moves
     */
    public void setTarget(float x, float y, float startSeconds) {
        if (x == targetX && y == targetY) {
            return;
        }
        var deltaX = x - targetX;
        var deltaY = y - targetY;
        var moveSettleSeconds = SPRING.getStepSettleSeconds(
                length(deltaX, deltaY), SETTLE_TOLERANCE);
        addEvent(false, deltaX, deltaY, startSeconds, startSeconds + moveSettleSeconds);
        targetX = x;
        targetY = y;
    }

    /** Returns the horizontal rest point in lily cells. */
    public float getTargetX() {
        return targetX;
    }

    /** Returns the vertical rest point in lily cells. */
    public float getTargetY() {
        return targetY;
    }

    /**
     * Evaluates the lily's spring motion at a scene time and caches the offset. Events that have
     * settled are dropped.
     *
     * @param nowSeconds scene time
     */
    public void advanceTo(float nowSeconds) {
        dropSettledEvents(nowSeconds);
        if (eventCount == 0) {
            baseX = targetX;
            baseY = targetY;
        }
        var sumX = baseX;
        var sumY = baseY;
        for (int i = 0; i < eventCount; i++) {
            var elapsedSeconds = nowSeconds - startSeconds[i];
            var weight = isPush[i]
                    ? SPRING.getImpulseOffsetAt(elapsedSeconds)
                    : SPRING.getStepProgressAt(elapsedSeconds);
            sumX += eventX[i] * weight;
            sumY += eventY[i] * weight;
        }
        limitedOffset.set(sumX, sumY);
        LIMIT.limit(limitedOffset);
        offsetX = limitedOffset.x;
        offsetY = limitedOffset.y;
    }

    /** Puts the lily back at its slot center at rest, with target 0,0 and no events. */
    public void reset() {
        eventCount = 0;
        baseX = 0f;
        baseY = 0f;
        targetX = 0f;
        targetY = 0f;
        offsetX = 0f;
        offsetY = 0f;
    }

    /** Returns true when the lily has no events left and its target is exactly 0,0. */
    public boolean isHome() {
        return eventCount == 0 && targetX == 0f && targetY == 0f;
    }

    /** Returns the draw scale; 1 is the normal size. */
    public float getScale() {
        return scale;
    }

    /** Sets the draw scale. */
    public void setScale(float scale) {
        this.scale = scale;
    }

    int getEventCount() {
        return eventCount;
    }

    private static float length(float x, float y) {
        return (float) Math.sqrt((double) x * x + (double) y * y);
    }

    private void addEvent(boolean push, float x, float y, float start, float settledAt) {
        if (eventCount == EVENT_CAPACITY) {
            removeEventFoldingRestMove(0);
        }
        isPush[eventCount] = push;
        eventX[eventCount] = x;
        eventY[eventCount] = y;
        startSeconds[eventCount] = start;
        settledAtSeconds[eventCount] = settledAt;
        eventCount++;
    }

    private void dropSettledEvents(float nowSeconds) {
        for (int i = eventCount - 1; i >= 0; i--) {
            if (nowSeconds >= settledAtSeconds[i]) {
                removeEventFoldingRestMove(i);
            }
        }
    }

    private void removeEventFoldingRestMove(int index) {
        if (!isPush[index]) {
            baseX += eventX[index];
            baseY += eventY[index];
        }
        for (int i = index + 1; i < eventCount; i++) {
            isPush[i - 1] = isPush[i];
            eventX[i - 1] = eventX[i];
            eventY[i - 1] = eventY[i];
            startSeconds[i - 1] = startSeconds[i];
            settledAtSeconds[i - 1] = settledAtSeconds[i];
        }
        eventCount--;
    }
}
