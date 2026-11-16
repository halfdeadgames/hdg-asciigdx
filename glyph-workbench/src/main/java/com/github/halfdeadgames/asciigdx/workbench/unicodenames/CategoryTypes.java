package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

/**
 * Questions about a {@link Character} type constant, as given by {@link UnicodeFacts#type(int)}.
 */
public final class CategoryTypes {

    private CategoryTypes() {
    }

    /** True for the letter categories Lu, Ll, Lt, Lm, and Lo. */
    public static boolean isLetter(int type) {
        return type == Character.UPPERCASE_LETTER || type == Character.LOWERCASE_LETTER
                || type == Character.TITLECASE_LETTER || type == Character.MODIFIER_LETTER
                || type == Character.OTHER_LETTER;
    }

    /** True for the separator categories Zs, Zl, and Zp. */
    public static boolean isSeparator(int type) {
        return type == Character.SPACE_SEPARATOR || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR;
    }
}
