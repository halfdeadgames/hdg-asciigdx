package com.github.halfdeadgames.asciigdx.tween;

import static com.github.halfdeadgames.asciigdx.tween.TweenChecks.requireFinite;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;

/**
 * Tweens played back to back. Each piece starts where the previous one ended, from the value the
 * previous one reached. An envelope (rise, hold, fall) is built as a sequence.
 */
public final class TweenSequence implements FloatTrack {

    private final Tween[] pieces;

    private TweenSequence(Tween[] pieces) {
        this.pieces = pieces;
    }

    /**
     * Starts building a sequence.
     *
     * @param startSeconds time at which the first piece begins, in seconds; must be finite
     * @param value the value held before and at the start; must be finite
     * @return a new builder with no pieces yet
     * @throws IllegalArgumentException if a number is NaN or infinite
     */
    public static Builder startingAt(float startSeconds, float value) {
        return new Builder(startSeconds, value);
    }

    /**
     * Returns the value at the given time: the first value before the start, the last value from
     * the end on, and the active piece's value in between. Any time can be queried in any order.
     *
     * @param timeSeconds the time to query, in seconds
     * @return the value at that time
     */
    @Override
    public float getValueAt(float timeSeconds) {
        for (int i = pieces.length - 1; i >= 0; i--) {
            if (pieces[i].getStartSeconds() <= timeSeconds) {
                return pieces[i].getValueAt(timeSeconds);
            }
        }
        return pieces[0].getValueAt(timeSeconds);
    }

    @Override
    public float getStartSeconds() {
        return pieces[0].getStartSeconds();
    }

    @Override
    public float getEndSeconds() {
        return pieces[pieces.length - 1].getEndSeconds();
    }

    /**
     * Returns the number of pieces, including holds.
     *
     * @return the piece count, at least 1
     */
    public int getPieceCount() {
        return pieces.length;
    }

    /**
     * Collects the pieces of a {@link TweenSequence}. Not thread-safe; use it from one thread.
     */
    public static final class Builder {

        private final Array<Tween> pieces = new Array<>();
        private final float startSeconds;
        private final float startValue;

        private Builder(float startSeconds, float value) {
            requireFinite("startSeconds", startSeconds);
            requireFinite("value", value);
            this.startSeconds = startSeconds;
            this.startValue = value;
        }

        /**
         * Adds a piece that changes linearly from the current value to {@code value}.
         *
         * @param value the value reached at the end of the piece; must be finite
         * @param durationSeconds length of the piece in seconds; finite and at least 0
         * @return this builder
         * @throws IllegalArgumentException if a number is NaN or infinite, or the duration is
         *     negative
         */
        public Builder to(float value, float durationSeconds) {
            return to(value, durationSeconds, Interpolation.linear);
        }

        /**
         * Adds a piece that changes from the current value to {@code value} along an easing
         * curve. The piece starts where the previous piece ended.
         *
         * @param value the value reached at the end of the piece; must be finite
         * @param durationSeconds length of the piece in seconds; finite and at least 0
         * @param easing curve applied to the progress; not null
         * @return this builder
         * @throws IllegalArgumentException if a number is NaN or infinite, the duration is
         *     negative, or the easing is null
         */
        public Builder to(float value, float durationSeconds, Interpolation easing) {
            pieces.add(Tween.eased(currentEnd(), durationSeconds, currentValue(), value, easing));
            return this;
        }

        /**
         * Adds a piece that keeps the current value for the given duration.
         *
         * @param durationSeconds length of the hold in seconds; finite and at least 0
         * @return this builder
         * @throws IllegalArgumentException if the duration is NaN, infinite, or negative
         */
        public Builder hold(float durationSeconds) {
            return to(currentValue(), durationSeconds);
        }

        /**
         * Creates the sequence from the pieces added so far. The builder stays usable and later
         * changes do not affect sequences already built.
         *
         * @return a new immutable sequence
         * @throws IllegalStateException if no piece was added
         */
        public TweenSequence build() {
            if (pieces.size == 0) {
                throw new IllegalStateException("a sequence needs at least one piece");
            }
            return new TweenSequence(pieces.toArray(Tween.class));
        }

        private float currentEnd() {
            return pieces.size == 0 ? startSeconds : pieces.peek().getEndSeconds();
        }

        private float currentValue() {
            return pieces.size == 0 ? startValue : pieces.peek().getToValue();
        }
    }
}
