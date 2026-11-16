package com.github.halfdeadgames.asciigdx.zone;

import com.github.halfdeadgames.asciigdx.color.HueColor;
import java.util.Objects;

/**
 * Ready-made {@link CellEffect}s. Motion amounts are in cell fractions: 0.25 is a quarter of a
 * cell, scaled by the cell width for horizontal motion and by the cell height for vertical motion.
 * Rotation is in degrees and scale is a plain factor. Cycle positions and spin angles are wrapped
 * in double precision, so effects stay smooth at large times. Every factory validates its arguments
 * once and returns an effect that allocates nothing when applied. Motion stacks with other zones,
 * and colors are last-wins, as described in {@link CellEffect}.
 */
public final class CellEffects {

    private static final float FULL_TURN_DEGREES = 360f;
    private static final int SHAKE_Y_SALT = 0x632BE5AB;

    private CellEffects() {
    }

    /**
     * Moves each glyph up and down along a sine, centered on its own position.
     *
     * @param amplitudeCells peak distance in cell heights; finite and not negative
     * @param periodSeconds seconds per cycle; finite and greater than 0
     * @param phase offset between cells
     * @throws IllegalArgumentException if a number is out of range
     */
    public static CellEffect wave(float amplitudeCells, float periodSeconds, Phase phase) {
        EffectArguments.requireNonNegative("amplitudeCells", amplitudeCells);
        EffectArguments.requirePositive("periodSeconds", periodSeconds);
        Objects.requireNonNull(phase, "phase");
        return (cell, where, time) -> {
            float position = CycleMath.position(time, periodSeconds, phase, where);
            cell.offsetBy(0f, amplitudeCells * where.getCellHeight() * CycleMath.sine(position));
        };
    }

    /**
     * Makes each glyph hop upward and land again, once per period. It never moves below its cell.
     *
     * @param heightCells peak height in cell heights; finite and not negative
     * @param periodSeconds seconds per hop; finite and greater than 0
     * @param phase offset between cells
     * @throws IllegalArgumentException if a number is out of range
     */
    public static CellEffect bounce(float heightCells, float periodSeconds, Phase phase) {
        EffectArguments.requireNonNegative("heightCells", heightCells);
        EffectArguments.requirePositive("periodSeconds", periodSeconds);
        Objects.requireNonNull(phase, "phase");
        return (cell, where, time) -> {
            float position = CycleMath.position(time, periodSeconds, phase, where);
            cell.offsetBy(0f, -heightCells * where.getCellHeight() * CycleMath.arch(position));
        };
    }

    /**
     * Jitters each glyph to a new pseudo-random spot {@code stepsPerSecond} times per second. Every
     * grid cell gets its own offset, and the same cell, step, and seed always give the same offset.
     *
     * @param amplitudeCells largest offset on each axis, in cell widths horizontally and cell
     *     heights vertically; finite and not negative
     * @param stepsPerSecond offset changes per second; finite and greater than 0
     * @param seed any int; different seeds give different patterns
     * @throws IllegalArgumentException if a number is out of range
     */
    public static CellEffect shake(float amplitudeCells, float stepsPerSecond, int seed) {
        EffectArguments.requireNonNegative("amplitudeCells", amplitudeCells);
        EffectArguments.requirePositive("stepsPerSecond", stepsPerSecond);
        return (cell, where, time) -> {
            int step = (int) Math.floor(time * stepsPerSecond);
            float x = signedUnit(CellHash.hash(where.getColumn(), where.getRow(), step, seed));
            float y = signedUnit(
                    CellHash.hash(where.getColumn(), where.getRow(), step, seed + SHAKE_Y_SALT));
            cell.offsetBy(amplitudeCells * where.getCellWidth() * x,
                    amplitudeCells * where.getCellHeight() * y);
        };
    }

    /**
     * Grows and shrinks each glyph along a sine, between {@code 1 - amount} and {@code 1 + amount}.
     *
     * @param amount largest change of the scale factor; finite and not negative. Above 1 the glyph
     *     mirrors at the low end.
     * @param periodSeconds seconds per cycle; finite and greater than 0
     * @param phase offset between cells
     * @throws IllegalArgumentException if a number is out of range
     */
    public static CellEffect pulse(float amount, float periodSeconds, Phase phase) {
        EffectArguments.requireNonNegative("amount", amount);
        EffectArguments.requirePositive("periodSeconds", periodSeconds);
        Objects.requireNonNull(phase, "phase");
        return (cell, where, time) -> {
            float position = CycleMath.position(time, periodSeconds, phase, where);
            cell.scaleBy(1f + amount * CycleMath.sine(position));
        };
    }

    /**
     * Turns each glyph at a steady speed. The rotation added is {@code degreesPerSecond * time},
     * reduced modulo 360 in double precision so it stays precise at large times, minus
     * {@code 360 * phase}, so a bigger phase lags behind. Positive degrees turn clockwise on
     * screen.
     *
     * @param degreesPerSecond turning speed; finite, may be negative
     * @param phase offset between cells, in turns
     * @throws IllegalArgumentException if the speed is not finite
     */
    public static CellEffect spin(float degreesPerSecond, Phase phase) {
        EffectArguments.requireFinite("degreesPerSecond", degreesPerSecond);
        Objects.requireNonNull(phase, "phase");
        return (cell, where, time) -> {
            float turnedDegrees = (float) (((double) degreesPerSecond * time) % FULL_TURN_DEGREES);
            cell.rotateBy(turnedDegrees - FULL_TURN_DEGREES * phase.cyclesAt(where));
        };
    }

    /**
     * Blinks the foreground toward a color and back. The mix amount follows a sine between 0 and
     * {@code maxAmount} and starts at half of {@code maxAmount}.
     *
     * @param rgba target color as RGBA8888; its alpha is ignored
     * @param maxAmount strongest mix; from 0 to 1
     * @param periodSeconds seconds per blink; finite and greater than 0
     * @param phase offset between cells
     * @throws IllegalArgumentException if a number is out of range
     */
    public static CellEffect flash(int rgba, float maxAmount, float periodSeconds, Phase phase) {
        EffectArguments.requireFraction("maxAmount", maxAmount);
        EffectArguments.requirePositive("periodSeconds", periodSeconds);
        Objects.requireNonNull(phase, "phase");
        return (cell, where, time) -> {
            float position = CycleMath.position(time, periodSeconds, phase, where);
            cell.setForegroundMix(rgba, maxAmount * CycleMath.sineUnit(position));
        };
    }

    /**
     * Tints the foreground with a hue that goes around the color wheel once per period, starting at
     * red. If the time is NaN or infinite, the cell is left unchanged.
     *
     * @param amount how strongly the hue is mixed in; from 0 to 1
     * @param periodSeconds seconds per trip around the wheel; finite and greater than 0
     * @param phase offset between cells; a cell with a bigger phase shows an earlier hue
     * @throws IllegalArgumentException if a number is out of range
     */
    public static CellEffect rainbow(float amount, float periodSeconds, Phase phase) {
        EffectArguments.requireFraction("amount", amount);
        EffectArguments.requirePositive("periodSeconds", periodSeconds);
        Objects.requireNonNull(phase, "phase");
        return (cell, where, time) -> {
            float position = CycleMath.position(time, periodSeconds, phase, where);
            if (Float.isNaN(position) || Float.isInfinite(position)) {
                return;
            }
            cell.setForegroundMix(HueColor.fromHue(position), amount);
        };
    }

    /**
     * Runs several effects on each cell, in the order given. The array is copied, so later changes
     * to it have no effect.
     *
     * @param effects effects to run, in order
     * @throws NullPointerException if the array or any effect is null
     */
    public static CellEffect combine(CellEffect... effects) {
        Objects.requireNonNull(effects, "effects");
        CellEffect[] copy = effects.clone();
        for (int i = 0; i < copy.length; i++) {
            Objects.requireNonNull(copy[i], "effects[" + i + "]");
        }
        return (cell, where, time) -> {
            for (int i = 0; i < copy.length; i++) {
                copy[i].apply(cell, where, time);
            }
        };
    }

    private static float signedUnit(int hash) {
        return CellHash.toUnit(hash) * 2f - 1f;
    }
}
