package com.github.halfdeadgames.asciigdx.zone;

/** Argument checks shared by the effect and phase factories. */
final class EffectArguments {

    private EffectArguments() {
    }

    static float requireFinite(String name, float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
        return value;
    }

    static float requirePositive(String name, float value) {
        requireFinite(name, value);
        if (value <= 0f) {
            throw new IllegalArgumentException(name + " must be greater than 0: " + value);
        }
        return value;
    }

    static float requireNonNegative(String name, float value) {
        requireFinite(name, value);
        if (value < 0f) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
        return value;
    }

    static float requireFraction(String name, float value) {
        requireNonNegative(name, value);
        if (value > 1f) {
            throw new IllegalArgumentException(name + " must be at most 1: " + value);
        }
        return value;
    }
}
