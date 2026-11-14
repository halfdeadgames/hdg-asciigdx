package com.github.halfdeadgames.asciigdx.tween;

final class TweenChecks {

    private TweenChecks() {
    }

    static void requireFinite(String name, float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
