package com.github.halfdeadgames.asciigdx.tween;

import static com.github.halfdeadgames.asciigdx.tween.TweenChecks.requireFinite;

import java.util.Objects;

/**
 * A damped spring whose motion is a pure function of the seconds since an event. The spring pulls a
 * value toward a rest point, and the damping ratio decides whether it overshoots and rings
 * (below 1), settles as fast as possible without overshoot (1), or creeps in slowly (above 1).
 * The formulas are the standard solution of the damped harmonic oscillator
 * {@code x'' = -omega^2 (x - rest) - 2 zeta omega x'}, where {@code omega} is the angular
 * frequency and {@code zeta} is the damping ratio.
 *
 * <p>The spring stores no state. The game keeps its own events (a velocity push at one time, or a
 * rest point moved by some distance at one time) and adds up the responses, because the motion is
 * linear. Any time can be queried in any order, so a pinned capture can jump straight to a time.
 *
 * <p>Time is always in seconds, passed in by the game; nothing reads a clock and nothing counts
 * frames. Unlike {@link Tween}, which takes an absolute time, the queries here take the seconds
 * elapsed since one event. Times before the event are valid and return 0. Keep event times and
 * the current time as {@code double} and subtract before converting to {@code float}: a
 * {@code float} clock loses precision as a session grows long (about 8 milliseconds after
 * 100,000 seconds), and a fast spring would visibly jitter. Instances are immutable and
 * thread-safe, and the queries allocate nothing, so they are safe to call per object per frame.
 *
 * <p>Example, a lily that bobs when pushed and follows a moved rest point:
 * <pre>{@code
 * DampedSpring spring = DampedSpring.ofFrequency(1.2f, 0.3f);
 * // nowSeconds, pushSeconds, and moveSeconds are doubles kept by the game.
 * // A push of 4 cells per second at pushSeconds, and a rest move of 2 cells at moveSeconds:
 * float offsetCells = 4f * spring.getImpulseOffsetAt((float) (nowSeconds - pushSeconds))
 *         + 2f * spring.getStepProgressAt((float) (nowSeconds - moveSeconds));
 * // Drop the push once its offset stays within 0.01 cells:
 * boolean finished = nowSeconds - pushSeconds > spring.getImpulseSettleSeconds(4f, 0.01f);
 * }</pre>
 */
public final class DampedSpring {

    private static final double CRITICAL_DAMPING_BAND = 1e-4;
    private static final int BISECTION_STEPS = 60;
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final float MIN_FREQUENCY_HZ = 0.001f;
    private static final float MAX_FREQUENCY_HZ = 1000f;
    private static final float MIN_DAMPING_RATIO = 0.001f;
    private static final float MAX_DAMPING_RATIO = 1000f;

    private enum Regime {
        UNDERDAMPED,
        CRITICAL,
        OVERDAMPED
    }

    private enum EventKind {
        PUSH,
        REST_MOVE
    }

    private final float frequencyHz;
    private final float dampingRatio;
    private final Regime regime;
    private final double omega;
    private final double zeta;
    /** Oscillation rate when underdamped; unused otherwise. */
    private final double dampedOmega;
    /** Exponential decay rate of the slowest component, as a positive number. */
    private final double decayRate;
    /** Overdamped roots, both negative, with {@code slowRoot > fastRoot}. */
    private final double slowRoot;
    private final double fastRoot;
    private final double peakImpulseOffset;

    private DampedSpring(float frequencyHz, float dampingRatio) {
        this.frequencyHz = frequencyHz;
        this.dampingRatio = dampingRatio;
        this.omega = TWO_PI * frequencyHz;
        this.zeta = dampingRatio;
        if (Math.abs(zeta - 1.0) < CRITICAL_DAMPING_BAND) {
            regime = Regime.CRITICAL;
            dampedOmega = 0.0;
            decayRate = omega;
            slowRoot = -omega;
            fastRoot = -omega;
        } else if (zeta < 1.0) {
            regime = Regime.UNDERDAMPED;
            dampedOmega = omega * Math.sqrt(1.0 - zeta * zeta);
            decayRate = zeta * omega;
            slowRoot = 0.0;
            fastRoot = 0.0;
        } else {
            regime = Regime.OVERDAMPED;
            var root = Math.sqrt(zeta * zeta - 1.0);
            dampedOmega = 0.0;
            slowRoot = -omega / (zeta + root);
            fastRoot = -omega * (zeta + root);
            decayRate = -slowRoot;
        }
        peakImpulseOffset = Math.abs(impulseOffset(peakImpulseSeconds()));
    }

    /**
     * Creates a spring from its natural frequency and damping ratio.
     *
     * @param frequencyHz undamped natural frequency in cycles per second; from 0.001 to 1000
     *     inclusive
     * @param dampingRatio damping ratio; from 0.001 to 1000 inclusive. Below 1 the spring
     *     overshoots and rings, at 1 it is critically damped, and above 1 it is overdamped and
     *     never overshoots. Ratios within 0.0001 of 1 are treated as exactly 1
     * @return an immutable spring
     * @throws IllegalArgumentException if either argument is NaN, infinite, or outside its range
     */
    public static DampedSpring ofFrequency(float frequencyHz, float dampingRatio) {
        requireFinite("frequencyHz", frequencyHz);
        requireFinite("dampingRatio", dampingRatio);
        requireInRange("frequencyHz", frequencyHz, MIN_FREQUENCY_HZ, MAX_FREQUENCY_HZ);
        requireInRange("dampingRatio", dampingRatio, MIN_DAMPING_RATIO, MAX_DAMPING_RATIO);
        return new DampedSpring(frequencyHz, dampingRatio);
    }

    /**
     * Returns the undamped natural frequency.
     *
     * @return cycles per second, finite and greater than 0
     */
    public float getFrequencyHz() {
        return frequencyHz;
    }

    /**
     * Returns the damping ratio as it was passed to {@link #ofFrequency}. A ratio within 0.0001
     * of 1 is returned unchanged even though the spring behaves as if it were exactly 1.
     *
     * @return the ratio, finite and greater than 0
     */
    public float getDampingRatio() {
        return dampingRatio;
    }

    /**
     * Returns the offset from the rest point after a push of 1 unit per second. The spring starts
     * at the rest point and is pushed at elapsed time 0; multiply the result by the push
     * velocity. The slope at time 0 is 1, so the result has units of "units per (units per
     * second)". Non-finite times are rejected, as for the other time arguments in this package.
     *
     * @param elapsedSeconds seconds since the push; finite. Zero or negative times return 0
     * @return the offset from the rest point, in the game's units per unit of push velocity
     * @throws IllegalArgumentException if {@code elapsedSeconds} is NaN or infinite
     */
    public float getImpulseOffsetAt(float elapsedSeconds) {
        requireFinite("elapsedSeconds", elapsedSeconds);
        if (elapsedSeconds <= 0f) {
            return 0f;
        }
        return (float) impulseOffset(elapsedSeconds);
    }

    /**
     * Returns the progress toward a rest point that moved at elapsed time 0. The value starts at
     * 0, tends to 1, and overshoots past 1 when the damping ratio is below 1. Multiply the result
     * by the distance the rest point moved. Non-finite times are rejected, as for the other time
     * arguments in this package.
     *
     * @param elapsedSeconds seconds since the rest point moved; finite. Zero or negative times
     *     return 0
     * @return the fraction of the rest move that has been covered
     * @throws IllegalArgumentException if {@code elapsedSeconds} is NaN or infinite
     */
    public float getStepProgressAt(float elapsedSeconds) {
        requireFinite("elapsedSeconds", elapsedSeconds);
        if (elapsedSeconds <= 0f) {
            return 0f;
        }
        return (float) stepProgress(elapsedSeconds);
    }

    /**
     * Returns the seconds after a push from which its offset stays within a tolerance. For every
     * later elapsed time {@code t}, {@code |pushSpeed * getImpulseOffsetAt(t)|} is at most
     * {@code toleranceDistance}, so a game can drop the push event once that much time has
     * passed. The result is a guaranteed bound rather than the exact crossing time, so it can be
     * slightly later than strictly needed. A push too small to ever exceed the tolerance, such
     * as a push speed of 0, returns 0.
     *
     * @param pushSpeed the push velocity in the game's units per second; finite, and either sign
     *     (only its size matters)
     * @param toleranceDistance the largest leftover offset the game can ignore, in the same units;
     *     finite and greater than 0
     * @return seconds since the push, finite and at least 0
     * @throws IllegalArgumentException if either argument is NaN or infinite, or if
     *     {@code toleranceDistance} is not greater than 0
     */
    public float getImpulseSettleSeconds(float pushSpeed, float toleranceDistance) {
        requireFinite("pushSpeed", pushSpeed);
        requireTolerance(toleranceDistance);
        var size = Math.abs((double) pushSpeed);
        if (size * peakImpulseOffset <= toleranceDistance) {
            return 0f;
        }
        var settleSeconds = switch (regime) {
            case UNDERDAMPED -> envelopeSettleSeconds(size / dampedOmega, toleranceDistance);
            case OVERDAMPED -> envelopeSettleSeconds(size / (slowRoot - fastRoot),
                    toleranceDistance);
            case CRITICAL -> criticalSettleSeconds(EventKind.PUSH, size, toleranceDistance);
        };
        return (float) Math.max(0.0, settleSeconds);
    }

    /**
     * Returns the seconds after a rest move from which the remaining distance stays within a
     * tolerance. For every later elapsed time {@code t},
     * {@code |moveDistance * (1 - getStepProgressAt(t))|} is at most {@code toleranceDistance},
     * so a game can treat the move as finished once that much time has passed. The result is a
     * guaranteed bound rather than the exact crossing time, so it can be slightly later than
     * strictly needed. A move no longer than the tolerance, such as a move of 0, returns 0.
     *
     * @param moveDistance how far the rest point moved, in the game's units; finite, and either
     *     sign (only its size matters)
     * @param toleranceDistance the largest leftover distance the game can ignore, in the same
     *     units; finite and greater than 0
     * @return seconds since the rest move, finite and at least 0
     * @throws IllegalArgumentException if either argument is NaN or infinite, or if
     *     {@code toleranceDistance} is not greater than 0
     */
    public float getStepSettleSeconds(float moveDistance, float toleranceDistance) {
        requireFinite("moveDistance", moveDistance);
        requireTolerance(toleranceDistance);
        var size = Math.abs((double) moveDistance);
        if (size <= toleranceDistance) {
            return 0f;
        }
        var settleSeconds = switch (regime) {
            case UNDERDAMPED -> envelopeSettleSeconds(size * omega / dampedOmega,
                    toleranceDistance);
            case OVERDAMPED -> envelopeSettleSeconds(size * -fastRoot / (slowRoot - fastRoot),
                    toleranceDistance);
            case CRITICAL -> criticalSettleSeconds(EventKind.REST_MOVE, size, toleranceDistance);
        };
        return (float) Math.max(0.0, settleSeconds);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DampedSpring)) {
            return false;
        }
        var spring = (DampedSpring) other;
        return Float.compare(frequencyHz, spring.frequencyHz) == 0
                && Float.compare(dampingRatio, spring.dampingRatio) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(frequencyHz, dampingRatio);
    }

    @Override
    public String toString() {
        return "DampedSpring[frequencyHz=" + frequencyHz + ", dampingRatio=" + dampingRatio + "]";
    }

    private static void requireInRange(String name, float value, float min, float max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " must be between " + min + " and " + max + ": " + value);
        }
    }

    private static void requireTolerance(float toleranceDistance) {
        requireFinite("toleranceDistance", toleranceDistance);
        if (toleranceDistance <= 0f) {
            throw new IllegalArgumentException(
                    "toleranceDistance must be > 0: " + toleranceDistance);
        }
    }

    private double impulseOffset(double seconds) {
        return switch (regime) {
            case UNDERDAMPED -> Math.exp(-decayRate * seconds) * Math.sin(dampedOmega * seconds)
                    / dampedOmega;
            case CRITICAL -> seconds * Math.exp(-omega * seconds);
            case OVERDAMPED -> (Math.exp(slowRoot * seconds) - Math.exp(fastRoot * seconds))
                    / (slowRoot - fastRoot);
        };
    }

    private double stepProgress(double seconds) {
        return switch (regime) {
            case UNDERDAMPED -> 1.0 - Math.exp(-decayRate * seconds)
                    * (Math.cos(dampedOmega * seconds)
                    + decayRate / dampedOmega * Math.sin(dampedOmega * seconds));
            case CRITICAL -> 1.0 - Math.exp(-omega * seconds) * (1.0 + omega * seconds);
            case OVERDAMPED -> 1.0 + (fastRoot * Math.exp(slowRoot * seconds)
                    - slowRoot * Math.exp(fastRoot * seconds)) / (slowRoot - fastRoot);
        };
    }

    private double peakImpulseSeconds() {
        return switch (regime) {
            case UNDERDAMPED -> Math.atan2(dampedOmega, decayRate) / dampedOmega;
            case CRITICAL -> 1.0 / omega;
            case OVERDAMPED -> Math.log(fastRoot / slowRoot) / (slowRoot - fastRoot);
        };
    }

    /** The leftover stays under {@code amplitude * exp(-decayRate * t)}. */
    private double envelopeSettleSeconds(double amplitude, double toleranceDistance) {
        return Math.log(amplitude / toleranceDistance) / decayRate;
    }

    /**
     * Both critical leftovers fall monotonically after their start point (the impulse peak, or
     * time 0 for a rest move), so bisection finds the earliest time from which they hold.
     */
    private double criticalSettleSeconds(EventKind kind, double size, double toleranceDistance) {
        var low = kind == EventKind.PUSH ? peakImpulseSeconds() : 0.0;
        var high = low + 1.0 / omega;
        while (criticalLeftover(kind, size, high) > toleranceDistance) {
            low = high;
            high *= 2.0;
        }
        for (int i = 0; i < BISECTION_STEPS; i++) {
            var middle = 0.5 * (low + high);
            if (criticalLeftover(kind, size, middle) > toleranceDistance) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    private double criticalLeftover(EventKind kind, double size, double seconds) {
        return switch (kind) {
            case PUSH -> size * Math.abs(impulseOffset(seconds));
            case REST_MOVE -> size * Math.abs(1.0 - stepProgress(seconds));
        };
    }
}
