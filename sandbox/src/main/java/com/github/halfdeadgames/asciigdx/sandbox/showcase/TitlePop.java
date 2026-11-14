package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.color.CellColor;

/** A title whose letters pop in one at a time, each overshooting its size before settling. */
public final class TitlePop {

    private final Beat[] scaleBeats;
    private final Beat[] alphaBeats;
    private final int[] letterColors;

    /**
     * Creates the pop schedule.
     *
     * @param startSeconds   when the first letter starts to pop
     * @param staggerSeconds delay between consecutive letter starts
     * @param popSeconds     how long each letter takes to pop
     * @param overshoot      libGDX SwingOut scale parameter; 3.5 peaks at about 1.31
     * @param letterCount    number of letters in the title
     * @param fromRgba       color of the first letter
     * @param toRgba         color of the last letter
     */
    public TitlePop(float startSeconds, float staggerSeconds, float popSeconds, float overshoot,
                    int letterCount, int fromRgba, int toRgba) {
        if (letterCount < 1) {
            throw new IllegalArgumentException("letterCount must be >= 1: " + letterCount);
        }
        if (!(popSeconds > 0f)) {
            throw new IllegalArgumentException("popSeconds must be > 0: " + popSeconds);
        }
        if (!(staggerSeconds >= 0f)) {
            throw new IllegalArgumentException("staggerSeconds must be >= 0: " + staggerSeconds);
        }
        scaleBeats = new Beat[letterCount];
        alphaBeats = new Beat[letterCount];
        letterColors = new int[letterCount];
        var swing = new Interpolation.SwingOut(overshoot);
        for (int i = 0; i < letterCount; i++) {
            float letterStart = startSeconds + i * staggerSeconds;
            scaleBeats[i] = new Beat(letterStart, popSeconds, swing);
            alphaBeats[i] = new Beat(letterStart, popSeconds);
            letterColors[i] = letterCount == 1
                    ? fromRgba
                    : CellColor.mixOklab(fromRgba, toRgba, i / (float) (letterCount - 1));
        }
    }

    /** Whether the letter's pop has started. */
    public boolean isShownAt(int letter, float timeSeconds) {
        return timeSeconds >= scaleBeats[letter].start();
    }

    /** Letter scale: 0 before its pop, overshoots 1 during it, exactly 1 afterward. */
    public float scaleAt(int letter, float timeSeconds) {
        return scaleBeats[letter].progress(timeSeconds);
    }

    /** Letter opacity, rising linearly from 0 to 1 over its pop. */
    public float alphaAt(int letter, float timeSeconds) {
        return alphaBeats[letter].progress(timeSeconds);
    }

    /** Gradient color of the letter. */
    public int colorAt(int letter) {
        return letterColors[letter];
    }

    /** When the last letter settles. */
    public float endSeconds() {
        return scaleBeats[scaleBeats.length - 1].end();
    }
}
