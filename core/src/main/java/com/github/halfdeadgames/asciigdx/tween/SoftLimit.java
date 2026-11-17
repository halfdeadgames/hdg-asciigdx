package com.github.halfdeadgames.asciigdx.tween;

import static com.github.halfdeadgames.asciigdx.tween.TweenChecks.requireFinite;

import com.badlogic.gdx.math.Vector2;
import java.util.Objects;

/**
 * A smooth distance limit that leaves small offsets untouched and eases larger ones toward a
 * maximum distance that is never exceeded. Distances up to the knee pass through unchanged. Past
 * the knee, the distance is squashed with a hyperbolic tangent curve. The curve starts with the
 * same slope as the unchanged part, so there is no visible kink, and it approaches the maximum
 * without reaching it. It is continuous and strictly increasing, so a larger input never
 * gives a smaller output.
 *
 * <p>{@link DampedSpring} motion is linear, so a game can add up the responses of many pushes.
 * A hard clamp on each push would break that. Apply the limit once, to the final summed offset
 * instead.
 *
 * <p>The knee and the maximum are in the same units as the offset the game limits (cells,
 * pixels, or any other unit). Instances are immutable and thread-safe, and the queries allocate
 * nothing, so they are safe to call per object per frame.
 *
 * <p>Example, a lily whose summed spring offset stays within 0.75 cells:
 * <pre>{@code
 * SoftLimit limit = SoftLimit.of(0.5f, 0.75f);
 * Vector2 offsetCells = new Vector2(
 *         pushX * spring.getImpulseOffsetAt(nowSeconds - pushSeconds),
 *         pushY * spring.getImpulseOffsetAt(nowSeconds - pushSeconds));
 * limit.limit(offsetCells);
 * }</pre>
 */
public final class SoftLimit {

    private final float kneeDistance;
    private final float maxDistance;

    private SoftLimit(float kneeDistance, float maxDistance) {
        this.kneeDistance = kneeDistance;
        this.maxDistance = maxDistance;
    }

    /**
     * Creates a limit from its knee and maximum distances.
     *
     * @param kneeDistance distance up to which offsets pass through unchanged, in the same units as
     *     the offsets; finite and at least 0. At 0 the squash applies at every distance above 0
     * @param maxDistance distance the limited result approaches, in the same units; finite and
     *     greater than {@code kneeDistance}
     * @return an immutable limit
     * @throws IllegalArgumentException if either argument is NaN or infinite, if
     *     {@code kneeDistance} is negative, or if {@code maxDistance} is not greater than
     *     {@code kneeDistance}
     */
    public static SoftLimit of(float kneeDistance, float maxDistance) {
        requireFinite("kneeDistance", kneeDistance);
        requireFinite("maxDistance", maxDistance);
        if (kneeDistance < 0f) {
            throw new IllegalArgumentException("kneeDistance must be >= 0: " + kneeDistance);
        }
        if (maxDistance <= kneeDistance) {
            throw new IllegalArgumentException("maxDistance must be > kneeDistance ("
                    + kneeDistance + "): " + maxDistance);
        }
        return new SoftLimit(kneeDistance, maxDistance);
    }

    /**
     * Returns the distance up to which offsets are left unchanged.
     *
     * @return the knee distance, finite and at least 0
     */
    public float getKneeDistance() {
        return kneeDistance;
    }

    /**
     * Returns the distance that limited results approach.
     *
     * @return the maximum distance, finite and greater than the knee distance
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Returns the limited version of a distance. Distances up to the knee are returned unchanged.
     * Larger distances are eased toward the maximum with
     * {@code knee + range * tanh((distance - knee) / range)}, where {@code range} is the maximum
     * minus the knee. The result never exceeds the maximum. It stays below the maximum unless
     * float rounding reaches it, which happens only for distances far beyond the maximum.
     *
     * <p>To limit a signed one-dimensional offset, limit its absolute value and restore the
     * sign: {@code Math.copySign(limit.getLimitedDistance(Math.abs(offsetX)), offsetX)}.
     *
     * @param distance the distance to limit, in the same units as the knee; finite and at least 0
     * @return the limited distance, at most the maximum distance and never larger than
     *     {@code distance}
     * @throws IllegalArgumentException if {@code distance} is NaN, infinite, or negative
     */
    public float getLimitedDistance(float distance) {
        requireFinite("distance", distance);
        if (distance < 0f) {
            throw new IllegalArgumentException("distance must be >= 0: " + distance);
        }
        if (distance <= kneeDistance) {
            return distance;
        }
        return Math.min((float) squash(distance), maxDistance);
    }

    /**
     * Limits an offset in place and returns nothing. The direction is kept and the length is
     * replaced by {@link #getLimitedDistance(float)} of the original length. A zero vector and
     * vectors no longer than the knee stay exactly unchanged. This method modifies
     * {@code offset} and allocates nothing; call it once on the final summed offset.
     *
     * @param offset the offset to limit, in the same units as the knee; not null, with finite
     *     components. It is scaled in place and left untouched if an exception is thrown
     * @throws NullPointerException if {@code offset} is null
     * @throws IllegalArgumentException if a component of {@code offset} is NaN or infinite
     */
    public void limit(Vector2 offset) {
        Objects.requireNonNull(offset, "offset");
        requireFinite("offset.x", offset.x);
        requireFinite("offset.y", offset.y);
        double length = Math.sqrt((double) offset.x * offset.x + (double) offset.y * offset.y);
        if (length <= kneeDistance) {
            return;
        }
        double scale = Math.min(squash(length), maxDistance) / length;
        offset.x = (float) (offset.x * scale);
        offset.y = (float) (offset.y * scale);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SoftLimit)) {
            return false;
        }
        var limit = (SoftLimit) other;
        return Float.compare(kneeDistance, limit.kneeDistance) == 0
                && Float.compare(maxDistance, limit.maxDistance) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kneeDistance, maxDistance);
    }

    @Override
    public String toString() {
        return "SoftLimit[kneeDistance=" + kneeDistance + ", maxDistance=" + maxDistance + "]";
    }

    private double squash(double distance) {
        double range = (double) maxDistance - kneeDistance;
        return kneeDistance + range * Math.tanh((distance - kneeDistance) / range);
    }
}
