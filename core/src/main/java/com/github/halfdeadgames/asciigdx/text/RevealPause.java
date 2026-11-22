package com.github.halfdeadgames.asciigdx.text;

/**
 * A pause in the typing reveal of a {@link StyledText}, written as {@code [wait:0.5]} in the
 * text. The pause happens before the letter at {@link #getLetterIndex()} appears. Immutable and
 * thread-safe. Pauses come only from parsing: read them with {@link StyledText#getPauses()};
 * {@link TextReveal} applies them. Two pauses are equal when their letter index and seconds
 * match.
 */
public final class RevealPause {

    private final int letterIndex;
    private final float seconds;

    private RevealPause(int letterIndex, float seconds) {
        this.letterIndex = letterIndex;
        this.seconds = seconds;
    }

    /**
     * Creates a pause.
     *
     * @param letterIndex index of the visible letter that appears after the pause, as a code point
     *         index into the visible text; 0 or more. A pause at the end of the text has the
     *         visible length as its index
     * @param seconds length of the pause in seconds; finite and 0 or more
     * @return the pause, never {@code null}
     * @throws IllegalArgumentException if {@code letterIndex} is negative, or {@code seconds} is
     *         negative, NaN, or infinite
     */
    static RevealPause of(int letterIndex, float seconds) {
        if (letterIndex < 0) {
            throw new IllegalArgumentException("letterIndex must not be negative: " + letterIndex);
        }
        if (!Float.isFinite(seconds) || seconds < 0f) {
            throw new IllegalArgumentException(
                    "seconds must be finite and not negative: " + seconds);
        }
        return new RevealPause(letterIndex, seconds);
    }

    /**
     * Returns the index of the letter that appears after this pause.
     *
     * @return a code point index into the visible text; 0 or more, and equal to the visible length
     *         for a pause at the end of the text
     */
    public int getLetterIndex() {
        return letterIndex;
    }

    /**
     * Returns the length of the pause.
     *
     * @return seconds; finite and 0 or more
     */
    public float getSeconds() {
        return seconds;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RevealPause pause)) {
            return false;
        }
        return letterIndex == pause.letterIndex
                && Float.compare(seconds, pause.seconds) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * letterIndex + Float.hashCode(seconds);
    }

    @Override
    public String toString() {
        return "RevealPause[letterIndex=" + letterIndex + ", seconds=" + seconds + "]";
    }
}
