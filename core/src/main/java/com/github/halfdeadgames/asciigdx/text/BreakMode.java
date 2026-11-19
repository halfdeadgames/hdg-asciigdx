package com.github.halfdeadgames.asciigdx.text;

/**
 * Decides where a line of wrapped text may end when the next character does not fit.
 *
 * <p>In both modes, spaces at a wrap point are dropped: trailing spaces at the end of a wrapped
 * line and leading spaces at the start of the next wrapped line. A newline always ends a line.
 *
 * <p>Constants may be added in later versions; a hyphenation mode is planned. A {@code switch}
 * statement or a Kotlin {@code when} expression over this enum therefore needs a default branch.
 *
 * @see WrapRules#withBreakMode(BreakMode)
 */
public enum BreakMode {

    /**
     * Breaks at spaces, so each line takes as many whole words as fit. A word longer than the line
     * width is split at a character, filling the line.
     */
    WORD,

    /** Fills each line with as many code points as fit and breaks anywhere. */
    CHARACTER
}
