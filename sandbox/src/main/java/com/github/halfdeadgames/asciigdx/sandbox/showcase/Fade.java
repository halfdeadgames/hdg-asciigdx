package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** Overlay opacity for a loop: opaque at the start, clear in the middle, opaque again at the end. */
public final class Fade {

    private final Beat fadeIn;
    private final Beat fadeOut;

    public Fade(float fadeInSeconds, float fadeOutStartSeconds, float fadeOutSeconds) {
        this.fadeIn = new Beat(0f, fadeInSeconds);
        this.fadeOut = new Beat(fadeOutStartSeconds, fadeOutSeconds);
    }

    public float alphaAt(float timeSeconds) {
        return Math.max(1f - fadeIn.progress(timeSeconds), fadeOut.progress(timeSeconds));
    }
}
