package com.github.halfdeadgames.asciigdx.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WrapRulesTest {

    @Test
    void betweenSetsDefaults() {
        WrapRules rules = WrapRules.between(4, 20);

        assertEquals(4, rules.getFirstLineStartColumn());
        assertEquals(20, rules.getFirstLineEndColumn());
        assertEquals(4, rules.getWrappedLineStartColumn());
        assertEquals(20, rules.getWrappedLineEndColumn());
        assertEquals(WrapRules.NO_LINE_LIMIT, rules.getMaxLines());
        assertEquals(BreakMode.WORD, rules.getBreakMode());
        assertEquals(TextDrawing.DEFAULT_ELLIPSIS, rules.getEllipsisCodePoint());
    }

    @Test
    void withCopiesLeaveTheOriginalUntouched() {
        WrapRules original = WrapRules.between(0, 9);

        WrapRules changed = original.withWrappedLineColumns(2, 8).withMaxLines(3)
                .withBreakMode(BreakMode.CHARACTER).withEllipsis('~');

        assertEquals(2, changed.getWrappedLineStartColumn());
        assertEquals(8, changed.getWrappedLineEndColumn());
        assertEquals(3, changed.getMaxLines());
        assertEquals(BreakMode.CHARACTER, changed.getBreakMode());
        assertEquals('~', changed.getEllipsisCodePoint());
        assertEquals(0, original.getWrappedLineStartColumn());
        assertEquals(WrapRules.NO_LINE_LIMIT, original.getMaxLines());
        assertEquals(BreakMode.WORD, original.getBreakMode());
        assertEquals(TextDrawing.DEFAULT_ELLIPSIS, original.getEllipsisCodePoint());
    }

    @Test
    void singleColumnIsAllowed() {
        assertEquals(3, WrapRules.between(3, 3).getFirstLineEndColumn());
    }

    @Test
    void startAfterEndIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> WrapRules.between(5, 4));
        assertThrows(IllegalArgumentException.class,
                () -> WrapRules.between(0, 9).withWrappedLineColumns(5, 4));
    }

    @Test
    void widthThatDoesNotFitAnIntIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> WrapRules.between(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> WrapRules.between(0, 9)
                        .withWrappedLineColumns(-1, Integer.MAX_VALUE));
    }

    @Test
    void negativeAndHugeColumnsAreAllowed() {
        assertEquals(-5, WrapRules.between(-5, -1).getFirstLineStartColumn());
        assertEquals(Integer.MAX_VALUE,
                WrapRules.between(0, Integer.MAX_VALUE - 1).getFirstLineEndColumn() + 1);
    }

    @Test
    void maxLinesBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> WrapRules.between(0, 9).withMaxLines(0));
    }

    @Test
    void nullBreakModeIsRejected() {
        assertThrows(NullPointerException.class, () -> WrapRules.between(0, 9).withBreakMode(null));
    }

    @Test
    void invalidEllipsisIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> WrapRules.between(0, 9).withEllipsis(-1));
    }

    @Test
    void hyphenDefaultsAreSet() {
        WrapRules rules = WrapRules.between(0, 9);

        assertNull(rules.getHyphenator());
        assertEquals(TextDrawing.DEFAULT_HYPHEN, rules.getHyphenCodePoint());
        assertEquals('-', TextDrawing.DEFAULT_HYPHEN);
    }

    @Test
    void withHyphenAndWithHyphenatorReturnChangedCopies() {
        Hyphenator hyphenator = Hyphenator.fromTexPatterns("\\patterns{ 1b }");
        WrapRules original = WrapRules.between(0, 9);

        WrapRules changed = original.withHyphen('~').withHyphenator(hyphenator);

        assertEquals('~', changed.getHyphenCodePoint());
        assertSame(hyphenator, changed.getHyphenator());
        assertNull(original.getHyphenator());
        assertEquals(TextDrawing.DEFAULT_HYPHEN, original.getHyphenCodePoint());
    }

    @Test
    void invalidHyphenIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> WrapRules.between(0, 9).withHyphen(-1));
        assertThrows(IllegalArgumentException.class,
                () -> WrapRules.between(0, 9).withHyphen(Character.MAX_CODE_POINT + 1));
    }

    @Test
    void nullHyphenatorIsRejected() {
        assertThrows(NullPointerException.class, () -> WrapRules.between(0, 9).withHyphenator(null));
    }

    @Test
    void hyphenSettingsTakePartInEqualsAndHashCode() {
        Hyphenator hyphenator = Hyphenator.fromTexPatterns("\\patterns{ 1b }");
        WrapRules first = WrapRules.between(0, 9).withHyphen('~').withHyphenator(hyphenator);
        WrapRules second = WrapRules.between(0, 9).withHyphen('~').withHyphenator(hyphenator);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, second.withHyphen('-'));
        assertNotEquals(first, WrapRules.between(0, 9).withHyphen('~'));
    }

    @Test
    void equalRulesHaveEqualHashCodes() {
        WrapRules first = WrapRules.between(1, 9).withMaxLines(2);
        WrapRules second = WrapRules.between(1, 9).withMaxLines(2);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, second.withMaxLines(3));
        assertNotEquals(first, second.withBreakMode(BreakMode.CHARACTER));
        assertNotEquals(first, null);
    }

    @Test
    void toStringNamesTheSettings() {
        String text = WrapRules.between(1, 9).toString();

        assertEquals(true, text.contains("1..9"));
    }
}
