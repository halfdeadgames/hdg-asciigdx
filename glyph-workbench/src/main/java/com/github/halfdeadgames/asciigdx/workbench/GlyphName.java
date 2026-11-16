package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.unicodenames.UnicodeFacts;
import java.util.function.IntFunction;

/** The Unicode name of a code point, reduced to what the uppercase ASCII text font can draw. */
public final class GlyphName {

    static final String CUSTOM = "PRIVATE USE (CUSTOM GLYPH)";
    static final String UNNAMED = "UNNAMED (NEWER UNICODE)";
    private static final String PRIVATE_USE_PREFIX = "PRIVATE USE";

    private GlyphName() {
    }

    /**
     * The complete sanitized name: the JDK's name, else the bundled table's, else {@link #UNNAMED}.
     * Private use code points give {@link #CUSTOM}.
     */
    public static String fullName(int codePoint) {
        return fullName(codePoint, Character::getName, BundledNames.TABLE);
    }

    static String fullName(int codePoint, IntFunction<String> jdkNames,
            IntFunction<String> tableNames) {
        String name = jdkNames.apply(codePoint);
        if (isPrivateUse(codePoint, name)) {
            return CUSTOM;
        }
        if (name == null) {
            name = tableNames.apply(codePoint);
        }
        return name == null ? UNNAMED : sanitize(name);
    }

    private static boolean isPrivateUse(int codePoint, String jdkName) {
        return Character.getType(codePoint) == Character.PRIVATE_USE
                || jdkName != null && jdkName.startsWith(PRIVATE_USE_PREFIX);
    }

    /** Loads the table on first use only. */
    private static final class BundledNames {
        static final IntFunction<String> TABLE = UnicodeFacts.bundled()::name;
    }

    private static String sanitize(String name) {
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toUpperCase(name.charAt(i));
            boolean allowed = c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == ' ' || c == '-';
            appendCollapsingSpaces(out, allowed ? c : ' ');
        }
        trimTrailingSpace(out);
        return out.toString();
    }

    private static void appendCollapsingSpaces(StringBuilder out, char c) {
        boolean redundantSpace =
                c == ' ' && (out.length() == 0 || out.charAt(out.length() - 1) == ' ');
        if (!redundantSpace) {
            out.append(c);
        }
    }

    private static void trimTrailingSpace(StringBuilder out) {
        int last = out.length() - 1;
        if (last >= 0 && out.charAt(last) == ' ') {
            out.setLength(last);
        }
    }
}
