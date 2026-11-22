package com.github.halfdeadgames.asciigdx.text;

import com.github.halfdeadgames.asciigdx.zone.CellEffect;
import java.util.Objects;

/**
 * A range of letters of a {@link StyledText} that share one effect. Immutable and thread-safe.
 * Letters are code point indexes into the visible text, the same index space as
 * {@link StyledText#hasForegroundAt(int)}.
 */
public final class EffectRun {

    private final int startLetter;
    private final int endLetterExclusive;
    private final CellEffect effect;

    private EffectRun(int startLetter, int endLetterExclusive, CellEffect effect) {
        this.startLetter = startLetter;
        this.endLetterExclusive = endLetterExclusive;
        this.effect = effect;
    }

    /**
     * Creates a run.
     *
     * @param startLetter first letter of the run; 0 or more
     * @param endLetterExclusive the letter after the last one of the run; greater than
     *         {@code startLetter}
     * @param effect the effect of the run; not {@code null}
     * @return the run, never {@code null}
     * @throws IllegalArgumentException if {@code startLetter} is negative or the range is empty
     * @throws NullPointerException if {@code effect} is {@code null}
     */
    public static EffectRun of(int startLetter, int endLetterExclusive, CellEffect effect) {
        if (startLetter < 0) {
            throw new IllegalArgumentException("startLetter must not be negative: " + startLetter);
        }
        if (endLetterExclusive <= startLetter) {
            throw new IllegalArgumentException("endLetterExclusive " + endLetterExclusive
                    + " must be greater than startLetter " + startLetter);
        }
        Objects.requireNonNull(effect, "effect");
        return new EffectRun(startLetter, endLetterExclusive, effect);
    }

    /**
     * Returns the first letter of the run.
     *
     * @return a code point index into the visible text; 0 or more
     */
    public int getStartLetter() {
        return startLetter;
    }

    /**
     * Returns the letter after the last one of the run.
     *
     * @return a code point index into the visible text; greater than {@link #getStartLetter()}
     */
    public int getEndLetterExclusive() {
        return endLetterExclusive;
    }

    /**
     * Returns the effect of the run. Where tags nest, this is the combination of every open
     * effect, outermost first.
     *
     * @return the effect, never {@code null}
     */
    public CellEffect getEffect() {
        return effect;
    }
}
