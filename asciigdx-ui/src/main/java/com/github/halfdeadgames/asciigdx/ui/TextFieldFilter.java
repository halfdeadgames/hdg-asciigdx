package com.github.halfdeadgames.asciigdx.ui;

/**
 * Decides which typed characters a {@link TextField} accepts. The filter sees only characters the
 * user types; text set with {@link TextField#setText(String)} is not filtered. Control characters
 * and lone surrogate halves never reach the filter.
 *
 * <p>Example:
 * <pre>{@code
 * field.setFilter(TextFieldFilter.digits());
 * field.setFilter((source, codePoint) -> Character.isLetter(codePoint));
 * }</pre>
 */
@FunctionalInterface
public interface TextFieldFilter {

    /**
     * Returns whether a typed character may be inserted at the caret.
     *
     * @param field the field the user types into, with its current text; never {@code null}
     * @param codePoint the typed character as a Unicode code point; never a control character.
     *         Typed characters arrive as single UTF-16 units, so today it is always in the Basic
     *         Multilingual Plane
     * @return {@code true} to insert the character, {@code false} to ignore it
     */
    boolean accepts(TextField field, int codePoint);

    /**
     * Returns the filter that accepts every character. It is the default of a new field.
     *
     * @return the filter, never {@code null}
     */
    static TextFieldFilter any() {
        return (field, codePoint) -> true;
    }

    /**
     * Returns a filter that accepts the characters 0 to 9 only.
     *
     * @return the filter, never {@code null}
     */
    static TextFieldFilter digits() {
        return (field, codePoint) -> codePoint >= '0' && codePoint <= '9';
    }

    /**
     * Returns a filter for non-negative decimal numbers: the characters 0 to 9, and one period
     * {@code '.'} while the field's text has none yet. It does not check where the period goes, so
     * {@code ".5"} and {@code "5."} are possible; no sign and no exponent are accepted.
     *
     * @return the filter, never {@code null}
     */
    static TextFieldFilter decimals() {
        return (field, codePoint) -> {
            if (codePoint == '.') {
                return field.getText().indexOf('.') < 0;
            }
            return codePoint >= '0' && codePoint <= '9';
        };
    }
}
