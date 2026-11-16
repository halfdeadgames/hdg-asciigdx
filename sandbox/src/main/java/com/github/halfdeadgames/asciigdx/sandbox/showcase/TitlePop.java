package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.color.CellColor;
import com.github.halfdeadgames.asciigdx.tween.StaggeredTrack;
import com.github.halfdeadgames.asciigdx.tween.Tween;

/** A title whose letters pop in one at a time, each overshooting its size before settling. */
public final class TitlePop {

    private final StaggeredTrack scaleTrack;
    private final StaggeredTrack alphaTrack;
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
        scaleTrack = new StaggeredTrack(
                new Tween(startSeconds, popSeconds, 0f, 1f, new Interpolation.SwingOut(overshoot)),
                staggerSeconds);
        alphaTrack =
                new StaggeredTrack(new Tween(startSeconds, popSeconds, 0f, 1f), staggerSeconds);
        letterColors = new int[letterCount];
        for (int i = 0; i < letterCount; i++) {
            letterColors[i] = letterCount == 1
                    ? fromRgba
                    : CellColor.mixOklab(fromRgba, toRgba, i / (float) (letterCount - 1));
        }
    }

    /** Letter scale: 0 before its pop, overshoots 1 during it, exactly 1 afterward. */
    public StaggeredTrack scaleTrack() {
        return scaleTrack;
    }

    /** Letter opacity, rising linearly from 0 to 1 over its pop. */
    public StaggeredTrack alphaTrack() {
        return alphaTrack;
    }

    /** Gradient color of the letter. */
    public int colorAt(int letter) {
        return letterColors[letter];
    }
}
