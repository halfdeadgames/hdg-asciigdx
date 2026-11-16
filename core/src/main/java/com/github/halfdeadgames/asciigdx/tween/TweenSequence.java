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

    public static Builder startingAt(float startSeconds, float value) {
        return new Builder(startSeconds, value);
    }

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

    public int getPieceCount() {
        return pieces.length;
    }

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

        public Builder to(float value, float durationSeconds) {
            return to(value, durationSeconds, Interpolation.linear);
        }

        public Builder to(float value, float durationSeconds, Interpolation easing) {
            pieces.add(Tween.eased(currentEnd(), durationSeconds, currentValue(), value, easing));
            return this;
        }

        public Builder hold(float durationSeconds) {
            return to(currentValue(), durationSeconds);
        }

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
