package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/**
 * Rings that spread from one point at a fixed speed, one after another. Each ring is a smooth bump around its
 * radius whose height fades out over the ring's life. Every value is a pure function of the distance and the time.
 */
public final class RippleRings {

    private final float firstStartSeconds;
    private final float staggerSeconds;
    private final int ringCount;
    private final float lifeSeconds;
    private final float speedCellsPerSecond;
    private final float halfWidthCells;

    public RippleRings(float firstStartSeconds, float staggerSeconds, int ringCount, float lifeSeconds,
                       float speedCellsPerSecond, float halfWidthCells) {
        if (!Float.isFinite(firstStartSeconds)) {
            throw new IllegalArgumentException("firstStartSeconds must be finite: " + firstStartSeconds);
        }
        if (!Float.isFinite(staggerSeconds) || staggerSeconds < 0f) {
            throw new IllegalArgumentException("staggerSeconds must be finite and >= 0: " + staggerSeconds);
        }
        if (ringCount < 1) {
            throw new IllegalArgumentException("ringCount must be >= 1: " + ringCount);
        }
        requireFinitePositive(lifeSeconds, "lifeSeconds");
        requireFinitePositive(speedCellsPerSecond, "speedCellsPerSecond");
        requireFinitePositive(halfWidthCells, "halfWidthCells");
        this.firstStartSeconds = firstStartSeconds;
        this.staggerSeconds = staggerSeconds;
        this.ringCount = ringCount;
        this.lifeSeconds = lifeSeconds;
        this.speedCellsPerSecond = speedCellsPerSecond;
        this.halfWidthCells = halfWidthCells;
    }

    private static void requireFinitePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0f) {
            throw new IllegalArgumentException(name + " must be finite and > 0: " + value);
        }
    }

    public float endSeconds() {
        return ringStartSeconds(ringCount - 1) + lifeSeconds;
    }

    public boolean isActiveAt(float timeSeconds) {
        return timeSeconds >= firstStartSeconds && timeSeconds < endSeconds();
    }

    /** The strongest ring bump at the distance, 0 to 1. */
    public float liftAt(float distanceCells, float timeSeconds) {
        float strongest = 0f;
        for (int ring = 0; ring < ringCount; ring++) {
            strongest = Math.max(strongest, ringLiftAt(ring, distanceCells, timeSeconds));
        }
        return strongest;
    }

    private float ringLiftAt(int ring, float distanceCells, float timeSeconds) {
        float age = timeSeconds - ringStartSeconds(ring);
        if (age < 0f || age >= lifeSeconds) {
            return 0f;
        }
        float distanceFromRing = Math.abs(distanceCells - age * speedCellsPerSecond);
        if (distanceFromRing >= halfWidthCells) {
            return 0f;
        }
        float bump = 0.5f + 0.5f * (float) Math.cos(Math.PI * distanceFromRing / halfWidthCells);
        return bump * (1f - age / lifeSeconds);
    }

    private float ringStartSeconds(int ring) {
        return firstStartSeconds + ring * staggerSeconds;
    }
}
