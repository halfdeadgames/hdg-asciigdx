package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.tween.TweenSequence;

/**
 * The overlay opacity over a loop: fully covered, fading out at the start, then fading back in
 * before the wrap.
 */
public final class LoopFade {

    private LoopFade() {
    }

    public static TweenSequence track(float fadeInSeconds, float fadeOutStartSeconds,
            float fadeOutSeconds) {
        return TweenSequence.startingAt(0f, 1f)
                .to(0f, fadeInSeconds)
                .hold(fadeOutStartSeconds - fadeInSeconds)
                .to(1f, fadeOutSeconds)
                .build();
    }
}
