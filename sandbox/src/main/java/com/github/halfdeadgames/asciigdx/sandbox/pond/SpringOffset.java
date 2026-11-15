package com.github.halfdeadgames.asciigdx.sandbox.pond;

/**
 * A 2D offset in lily cells pulled toward a movable rest point (0,0 by default) by an underdamped
 * spring, so a push or a moved rest point makes it swing, wobble, and settle.
 */
public final class SpringOffset {

    /** Default spring frequency for a lily, in hertz. */
    public static final float DEFAULT_FREQUENCY_HZ = 1.2f;
    /** Default damping ratio for a lily; below 1 the spring wobbles. */
    public static final float DEFAULT_DAMPING_RATIO = 0.3f;
    /** Default farthest distance from center for a lily, in lily cells. */
    public static final float DEFAULT_MAX_DISTANCE = 0.75f;

    private static final float MAX_DELTA_SECONDS = 0.1f;
    private static final float SUBSTEP_SECONDS = 1f / 120f;
    /** About a sixth of a source pixel on an 8 px lily cell, so the snap to rest is invisible. */
    private static final float REST_OFFSET = 0.02f;
    private static final float REST_SPEED = 0.1f;

    private final float omegaSquared;
    private final float dampingCoefficient;
    private final float maxDistance;

    private float offsetX;
    private float offsetY;
    private float velocityX;
    private float velocityY;
    private float targetX;
    private float targetY;

    /** Creates a spring with the lily defaults. */
    public SpringOffset() {
        this(DEFAULT_FREQUENCY_HZ, DEFAULT_DAMPING_RATIO, DEFAULT_MAX_DISTANCE);
    }

    /**
     * Creates a spring.
     *
     * @param frequencyHz how many swings per second, greater than 0
     * @param dampingRatio how fast the swing dies out, greater than 0; below 1 it wobbles
     * @param maxDistance farthest distance from center in lily cells, greater than 0
     * @throws IllegalArgumentException if any value is not greater than 0
     */
    public SpringOffset(float frequencyHz, float dampingRatio, float maxDistance) {
        requirePositive(frequencyHz, "frequencyHz");
        requirePositive(dampingRatio, "dampingRatio");
        requirePositive(maxDistance, "maxDistance");
        var omega = (float) (2.0 * Math.PI * frequencyHz);
        this.omegaSquared = omega * omega;
        this.dampingCoefficient = 2f * dampingRatio * omega;
        this.maxDistance = maxDistance;
    }

    private static void requirePositive(float value, String name) {
        if (!(value > 0f)) {
            throw new IllegalArgumentException(name + " must be greater than 0 but was " + value);
        }
    }

    /** Adds to the current velocity, in lily cells per second; pushes accumulate. */
    public void push(float velocityX, float velocityY) {
        this.velocityX += velocityX;
        this.velocityY += velocityY;
    }

    /** Steps the spring forward; ignores a delta of 0 or less and caps it at 0.1 seconds. */
    public void advance(float deltaSeconds) {
        if (!(deltaSeconds > 0f)) {
            return;
        }
        var remaining = Math.min(deltaSeconds, MAX_DELTA_SECONDS);
        while (remaining > 0f) {
            var step = Math.min(remaining, SUBSTEP_SECONDS);
            integrate(step);
            clampToMaxDistance();
            remaining -= step;
        }
        snapToRestIfSettled();
    }

    private void integrate(float step) {
        var accelerationX = -omegaSquared * (offsetX - targetX) - dampingCoefficient * velocityX;
        var accelerationY = -omegaSquared * (offsetY - targetY) - dampingCoefficient * velocityY;
        velocityX += accelerationX * step;
        velocityY += accelerationY * step;
        offsetX += velocityX * step;
        offsetY += velocityY * step;
    }

    private void clampToMaxDistance() {
        var distanceSquared = offsetX * offsetX + offsetY * offsetY;
        if (distanceSquared <= maxDistance * maxDistance) {
            return;
        }
        var distance = (float) Math.sqrt(distanceSquared);
        var directionX = offsetX / distance;
        var directionY = offsetY / distance;
        offsetX = directionX * maxDistance;
        offsetY = directionY * maxDistance;
        var outwardSpeed = velocityX * directionX + velocityY * directionY;
        if (outwardSpeed > 0f) {
            velocityX -= outwardSpeed * directionX;
            velocityY -= outwardSpeed * directionY;
        }
    }

    private void snapToRestIfSettled() {
        var distanceX = offsetX - targetX;
        var distanceY = offsetY - targetY;
        var distanceSquared = distanceX * distanceX + distanceY * distanceY;
        var speedSquared = velocityX * velocityX + velocityY * velocityY;
        if (distanceSquared < REST_OFFSET * REST_OFFSET && speedSquared < REST_SPEED * REST_SPEED) {
            offsetX = targetX;
            offsetY = targetY;
            velocityX = 0f;
            velocityY = 0f;
        }
    }

    /**
     * Moves the rest point the spring is pulled toward. A target farther than the maximum
     * distance from 0,0 is shortened to the maximum distance.
     */
    public void setTarget(float x, float y) {
        var distanceSquared = x * x + y * y;
        if (distanceSquared <= maxDistance * maxDistance) {
            targetX = x;
            targetY = y;
            return;
        }
        var scale = maxDistance / (float) Math.sqrt(distanceSquared);
        targetX = x * scale;
        targetY = y * scale;
    }

    /** Returns the horizontal rest point in lily cells. */
    public float getTargetX() {
        return targetX;
    }

    /** Returns the vertical rest point in lily cells. */
    public float getTargetY() {
        return targetY;
    }

    /** Sets the offset, velocity, and rest point to 0. */
    public void reset() {
        offsetX = 0f;
        offsetY = 0f;
        velocityX = 0f;
        velocityY = 0f;
        targetX = 0f;
        targetY = 0f;
    }

    /** Returns the horizontal offset in lily cells. */
    public float getOffsetX() {
        return offsetX;
    }

    /** Returns the vertical offset in lily cells. */
    public float getOffsetY() {
        return offsetY;
    }

    /** Returns the horizontal velocity in lily cells per second. */
    public float getVelocityX() {
        return velocityX;
    }

    /** Returns the vertical velocity in lily cells per second. */
    public float getVelocityY() {
        return velocityY;
    }

    /** Returns true when the offset is exactly on the rest point and the velocity is exactly 0. */
    public boolean isAtRest() {
        return offsetX == targetX && offsetY == targetY && velocityX == 0f && velocityY == 0f;
    }
}
