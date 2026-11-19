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
     * Returns an effect that moves each glyph up and down along a sine, centered on its own
     * position. It adds a vertical offset to the cell's handle.
     *
     * @param amplitudeCells peak distance in cell heights; finite and not negative
     * @param periodSeconds seconds per cycle; finite and greater than 0
     * @param phase offset between cells; not null
     * @return a new effect that allocates nothing when applied
     * @throws IllegalArgumentException if a number is out of range
     * @throws NullPointerException if {@code phase} is null
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
     * Returns an effect that makes each glyph hop upward and land again, once per period. It never
     * moves below its cell. It adds a vertical offset to the cell's handle.
     *
     * @param heightCells peak height in cell heights; finite and not negative
     * @param periodSeconds seconds per hop; finite and greater than 0
     * @param phase offset between cells; not null
     * @return a new effect that allocates nothing when applied
     * @throws IllegalArgumentException if a number is out of range
     * @throws NullPointerException if {@code phase} is null
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
     * Returns an effect that jitters each glyph to a new pseudo-random spot
     * {@code stepsPerSecond} times per second. Every grid cell gets its own offset, and the same
     * cell, step, and seed always give the same offset. It adds a horizontal and vertical offset to
     * the cell's handle.
     *
     * @param amplitudeCells largest offset on each axis, in cell widths horizontally and cell
     *     heights vertically; finite and not negative
     * @param stepsPerSecond offset changes per second; finite and greater than 0
     * @param seed any int; different seeds give different patterns
     * @return a new effect that allocates nothing when applied
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
     * Returns an effect that grows and shrinks each glyph along a sine, between
     * {@code 1 - amount} and {@code 1 + amount}. It multiplies the scale of the cell's handle on
     * both axes.
     *
     * @param amount largest change of the scale factor; finite and not negative. Above 1 the glyph
     *     mirrors at the low end.
     * @param periodSeconds seconds per cycle; finite and greater than 0
     * @param phase offset between cells; not null
     * @return a new effect that allocates nothing when applied
     * @throws IllegalArgumentException if a number is out of range
     * @throws NullPointerException if {@code phase} is null
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
     * Returns an effect that turns each glyph at a steady speed. The rotation added is
     * {@code degreesPerSecond * time}, reduced modulo 360 in double precision so it stays precise
     * at large times, minus {@code 360 * phase}, so a bigger phase lags behind. Positive degrees
     * turn clockwise on screen. It adds to the rotation of the cell's handle.
     *
     * @param degreesPerSecond turning speed in degrees per second; finite, may be negative
     * @param phase offset between cells, in turns; not null
     * @return a new effect that allocates nothing when applied
     * @throws IllegalArgumentException if the speed is not finite
     * @throws NullPointerException if {@code phase} is null
     */
    public static CellEffect spin(float degreesPerSecond, Phase phase) {
        EffectArguments.requireFinite("degreesPerSecond", degreesPerSecond);
        Objects.requireNonNull(phase, "phase");
        return (cell, where, time) -> {
            float turnedDegrees = (float) (((double) degreesPerSecond * time) % FULL_TURN_DEGREES);
            cell.rotateBy(turnedDegrees - FULL_TURN_DEGREES * phase.getCyclesAt(where));
        };
    }

    /**
     * Returns an effect that blinks the foreground toward a color and back. The mix amount follows
     * a sine between 0 and {@code maxAmount} and starts at half of {@code maxAmount}. It sets the
     * foreground mix of the cell's handle, so the zone applied last decides the color.
     *
     * @param rgba target color as RGBA8888; its alpha is ignored
     * @param maxAmount strongest mix; from 0 to 1
     * @param periodSeconds seconds per blink; finite and greater than 0
     * @param phase offset between cells; not null
     * @return a new effect that allocates nothing when applied
     * @throws IllegalArgumentException if a number is out of range
     * @throws NullPointerException if {@code phase} is null
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
     * Returns an effect that tints the foreground with a hue that goes around the color wheel once
     * per period, starting at red. If the time is NaN or infinite, the cell is left unchanged. It
     * sets the foreground mix of the cell's handle, so the zone applied last decides the color.
     *
     * @param amount how strongly the hue is mixed in; from 0 to 1
     * @param periodSeconds seconds per trip around the wheel; finite and greater than 0
     * @param phase offset between cells; a cell with a bigger phase shows an earlier hue; not null
     * @return a new effect that allocates nothing when applied
     * @throws IllegalArgumentException if a number is out of range
     * @throws NullPointerException if {@code phase} is null
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
     * Returns an effect that steps each cell through a list of glyphs, one after another, and
     * starts over after the last. {@code periodSeconds} is the time for the whole loop through all
     * the glyphs, so each glyph shows for {@code periodSeconds / codePoints.length} seconds. With
     * {@link Phase#sync()} every cell shows the first glyph at time 0; a bigger phase delays a
     * cell, so it shows an earlier glyph of the loop. It sets the temporary glyph of the cell's
     * handle, so the zone applied last decides the glyph. It does not lift the cell, and the
     * grid's own glyph returns once the effect stops. A code point the font lacks shows the glyph
     * source's fallback glyph. If the time is NaN or infinite, the cell is left unchanged. The
     * array is copied, so later changes to it have no effect.
     *
     * @param periodSeconds seconds for one full loop through all glyphs; finite and greater than 0
     * @param phase offset between cells; not null
     * @param codePoints glyphs to show in order, as Unicode code points; not null, and at least one
     *     entry, because an empty list throws when the effect is created (setup time, not per
     *     frame). Each must be a valid code point (0 to 0x10FFFF). Surrogate values (U+D800 to
     *     U+DFFF) are accepted, but no font has a glyph for them, so they draw the glyph source's
     *     fallback glyph.
     * @return a new effect that allocates nothing when applied
     * @throws IllegalArgumentException if {@code periodSeconds} is not finite and greater than 0,
     *     if {@code codePoints} is empty, or if any code point is not valid
     * @throws NullPointerException if {@code phase} or {@code codePoints} is null
     */
    public static CellEffect glyphCycle(float periodSeconds, Phase phase, int... codePoints) {
        EffectArguments.requirePositive("periodSeconds", periodSeconds);
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(codePoints, "codePoints");
        if (codePoints.length == 0) {
            throw new IllegalArgumentException("codePoints must not be empty");
        }
        int[] glyphs = codePoints.clone();
        for (int i = 0; i < glyphs.length; i++) {
            if (!Character.isValidCodePoint(glyphs[i])) {
                throw new IllegalArgumentException(
                        "codePoints[" + i + "] is not a valid code point: " + glyphs[i]);
            }
        }
        return (cell, where, time) -> {
            float position = CycleMath.position(time, periodSeconds, phase, where);
            if (Float.isNaN(position) || Float.isInfinite(position)) {
                return;
            }
            cell.setCodePoint(glyphs[CycleMath.stepIndex(position, glyphs.length)]);
        };
    }

    /**
     * Returns an effect that runs several effects on each cell, in the order given. The array is
     * copied, so later changes to it have no effect. An empty array gives an effect that does
     * nothing.
     *
     * @param effects effects to run, in order; not null, and no null elements
     * @return a new effect that runs each given effect in turn
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
