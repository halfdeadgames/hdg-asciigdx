package com.github.halfdeadgames.asciigdx.text;

/**
 * Decides where a line of wrapped text may end when the next character does not fit.
 *
 * <p>In every mode, spaces at a wrap point are dropped: trailing spaces at the end of a wrapped
 * line and leading spaces at the start of the next wrapped line. A newline always ends a line.
 *
 * <p>Constants may be added in later versions. A {@code switch} statement or a Kotlin
 * {@code when} expression over this enum therefore needs a default branch.
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
    CHARACTER,

    /**
     * Breaks at spaces like {@link #WORD}, and fills each line by splitting a word that does not
     * fit at the last allowed point where the piece plus a hyphen fits. The hyphen glyph comes
     * from {@link WrapRules#getHyphenCodePoint()} and is drawn in the cell after the piece.
     *
     * <p>Allowed points are the break points of the {@link Hyphenator} of the rules, and the cell
     * right after an existing {@code '-'} inside a word, where no extra hyphen is added.
     * Punctuation at the start or end of a word stays attached to its piece. When no allowed
     * point fits, the layout behaves like {@link #WORD}. Lines narrower than 3 cells never
     * hyphenate and wrap as {@code WORD}. On the last allowed line of a limited layout, the
     * ellipsis replaces the hyphen.
     *
     * <p>Without {@link WrapRules#withHyphenator(Hyphenator)}, the first layout that needs
     * {@link Hyphenator#englishUs()} loads the bundled patterns once, which takes a few
     * milliseconds. Call {@code Hyphenator.englishUs()} while loading to avoid a stall. The
     * default patterns suit US English text only; pass another {@code Hyphenator} for other
     * languages. {@code englishUs()} may be called from any thread; see its documentation.
     */
    HYPHENATE
}
