package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import java.util.Map;

/** Maps the two-letter General_Category codes of the Unicode data files to {@link Character} type constants. */
final class GeneralCategories {

    private static final Map<String, Byte> TYPES_BY_CODE = Map.ofEntries(
        Map.entry("Lu", Character.UPPERCASE_LETTER),
        Map.entry("Ll", Character.LOWERCASE_LETTER),
        Map.entry("Lt", Character.TITLECASE_LETTER),
        Map.entry("Lm", Character.MODIFIER_LETTER),
        Map.entry("Lo", Character.OTHER_LETTER),
        Map.entry("Mn", Character.NON_SPACING_MARK),
        Map.entry("Mc", Character.COMBINING_SPACING_MARK),
        Map.entry("Me", Character.ENCLOSING_MARK),
        Map.entry("Nd", Character.DECIMAL_DIGIT_NUMBER),
        Map.entry("Nl", Character.LETTER_NUMBER),
        Map.entry("No", Character.OTHER_NUMBER),
        Map.entry("Pc", Character.CONNECTOR_PUNCTUATION),
        Map.entry("Pd", Character.DASH_PUNCTUATION),
        Map.entry("Ps", Character.START_PUNCTUATION),
        Map.entry("Pe", Character.END_PUNCTUATION),
        Map.entry("Pi", Character.INITIAL_QUOTE_PUNCTUATION),
        Map.entry("Pf", Character.FINAL_QUOTE_PUNCTUATION),
        Map.entry("Po", Character.OTHER_PUNCTUATION),
        Map.entry("Sm", Character.MATH_SYMBOL),
        Map.entry("Sc", Character.CURRENCY_SYMBOL),
        Map.entry("Sk", Character.MODIFIER_SYMBOL),
        Map.entry("So", Character.OTHER_SYMBOL),
        Map.entry("Zs", Character.SPACE_SEPARATOR),
        Map.entry("Zl", Character.LINE_SEPARATOR),
        Map.entry("Zp", Character.PARAGRAPH_SEPARATOR),
        Map.entry("Cc", Character.CONTROL),
        Map.entry("Cf", Character.FORMAT),
        Map.entry("Cs", Character.SURROGATE),
        Map.entry("Co", Character.PRIVATE_USE),
        Map.entry("Cn", Character.UNASSIGNED));

    private GeneralCategories() {
    }

    static boolean isKnown(String code) {
        return TYPES_BY_CODE.containsKey(code);
    }

    /** Returns the Character type constant for a known code; check {@link #isKnown} first. */
    static byte typeOf(String code) {
        return TYPES_BY_CODE.get(code);
    }
}
