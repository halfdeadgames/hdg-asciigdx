package com.github.halfdeadgames.asciigdx.text;

import java.util.List;
import java.util.Objects;

/**
 * The timing of a typing reveal: how many letters of a {@link StyledText} are shown after some
 * seconds. Every visible letter, spaces included, takes {@code 1 / lettersPerSecond} seconds and
 * appears at the end of its slot, so nothing is shown at time 0 unless the text is empty. Each
 * {@code [wait:SECONDS]} tag of the text adds its seconds before the letter that follows it.
 * Letter {@code k} (from 0) appears at {@code (k + 1) / lettersPerSecond} plus the seconds of
 * every pause at an index of {@code k} or less.
 *
 * <p>The object holds no clock and no progress: it is stateless, immutable, and thread-safe. The
 * game passes the seconds since the reveal started, and the same input always gives the same
 * answer. To restart, pass a time from 0 again. The timing is computed once in
 * {@link #of(StyledText, float)}; the queries do not allocate, so they are safe to call every
 * frame.
 *
 * <p>Example:
 * <pre>{@code
 * // A UI label builds its own reveal, matching its tags:
 * TextReveal reveal = label.createReveal(20f);
 * // every frame:
 * label.setShownLetterCount(reveal.getShownLetterCount(secondsSinceStart));
 *
 * // Grid text:
 * StyledText text = StyledText.parse("Hello.[wait:0.5] World!", ColorNames.NONE);
 * TextReveal gridReveal = TextReveal.of(text, 20f);
 * }</pre>
 */
public final class TextReveal {

    private final int visibleLength;
    private final double[] appearanceSeconds;
    private final float totalSeconds;

    private TextReveal(int visibleLength, double[] appearanceSeconds, float totalSeconds) {
        this.visibleLength = visibleLength;
        this.appearanceSeconds = appearanceSeconds;
        this.totalSeconds = totalSeconds;
    }

    /**
     * Computes the reveal timing of a text.
     *
     * @param text the text to reveal; must not be {@code null}; its pauses are read from
     *         {@link StyledText#getPauses()}
     * @param lettersPerSecond reveal speed in letters per second; finite and greater than 0
     * @return the reveal, never {@code null}
     * @throws NullPointerException if {@code text} is {@code null}
     * @throws IllegalArgumentException if {@code lettersPerSecond} is NaN, infinite, 0, or negative
     */
    public static TextReveal of(StyledText text, float lettersPerSecond) {
        Objects.requireNonNull(text, "text");
        if (!Float.isFinite(lettersPerSecond) || lettersPerSecond <= 0f) {
            throw new IllegalArgumentException(
                    "lettersPerSecond must be finite and greater than 0: " + lettersPerSecond);
        }
        int length = text.getVisibleLength();
        double secondsPerLetter = 1.0 / lettersPerSecond;
        double[] appearances = new double[length];
        List<RevealPause> pauses = text.getPauses();
        int nextPause = 0;
        double pauseSeconds = 0.0;
        for (int letter = 0; letter < length; letter++) {
            while (nextPause < pauses.size() && pauses.get(nextPause).getLetterIndex() <= letter) {
                pauseSeconds += pauses.get(nextPause).getSeconds();
                nextPause++;
            }
            appearances[letter] = (letter + 1) * secondsPerLetter + pauseSeconds;
        }
        for (; nextPause < pauses.size(); nextPause++) {
            pauseSeconds += pauses.get(nextPause).getSeconds();
        }
        return new TextReveal(length, appearances,
                (float) (length * secondsPerLetter + pauseSeconds));
    }

    /**
     * Returns how many letters are shown after some time. Call it every frame and pass the result
     * to the drawing, for example {@code Label.setShownLetterCount}.
     *
     * @param elapsedSeconds seconds since the reveal started; finite. A negative value means
     *         "nothing shown yet" and returns 0, because a restarted clock can briefly give small
     *         negative values. NaN and infinity are bugs in the caller and throw
     * @return the number of letters shown, from 0 to the visible length of the text; the first
     *         letters of the text, in order
     * @throws IllegalArgumentException if {@code elapsedSeconds} is NaN or infinite
     */
    public int getShownLetterCount(float elapsedSeconds) {
        requireFinite(elapsedSeconds);
        int low = 0;
        int high = visibleLength;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (appearanceSeconds[middle] <= elapsedSeconds) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /**
     * Returns the seconds the whole reveal takes: the visible length divided by the speed, plus
     * every pause, including pauses at the end of the text.
     *
     * @return seconds; 0 or more
     */
    public float getTotalSeconds() {
        return totalSeconds;
    }

    /**
     * Returns whether the reveal is over, so that every letter is shown and every pause has
     * passed.
     *
     * @param elapsedSeconds seconds since the reveal started; finite. A negative value is never
     *         complete
     * @return {@code true} if {@code elapsedSeconds} is at least {@link #getTotalSeconds()}, so
     *         {@code isComplete(getTotalSeconds())} is always {@code true}
     * @throws IllegalArgumentException if {@code elapsedSeconds} is NaN or infinite
     */
    public boolean isComplete(float elapsedSeconds) {
        requireFinite(elapsedSeconds);
        return elapsedSeconds >= totalSeconds;
    }

    private static void requireFinite(float elapsedSeconds) {
        if (!Float.isFinite(elapsedSeconds)) {
            throw new IllegalArgumentException(
                    "elapsedSeconds must be finite: " + elapsedSeconds);
        }
    }
}
