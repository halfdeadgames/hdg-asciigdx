package com.github.halfdeadgames.asciigdx.workbench;

/** The Unicode name of a code point, reduced to what the uppercase ASCII text font can draw. */
final class GlyphName {

    static final String CUSTOM = "PRIVATE USE (CUSTOM GLYPH)";
    private static final String PRIVATE_USE_PREFIX = "PRIVATE USE";

    private GlyphName() {
    }

    /** Names longer than {@code maxChars} keep their last words, since Unicode names end in the part that identifies the glyph. */
    static String describe(int codePoint, int maxChars) {
        String name = Character.getName(codePoint);
        if (name == null || name.startsWith(PRIVATE_USE_PREFIX)) {
            return CUSTOM;
        }
        String clean = sanitize(name);
        if (clean.length() <= maxChars) {
            return clean;
        }
        int earliestStart = clean.length() - Math.max(0, maxChars);
        int spaceBeforeFirstWord = clean.indexOf(' ', earliestStart - 1);
        return spaceBeforeFirstWord >= 0 ? clean.substring(spaceBeforeFirstWord + 1) : clean.substring(earliestStart);
    }

    private static String sanitize(String name) {
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toUpperCase(name.charAt(i));
            boolean allowed = c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == ' ' || c == '-';
            out.append(allowed ? c : ' ');
        }
        return out.toString().trim().replaceAll(" {2,}", " ");
    }
}
