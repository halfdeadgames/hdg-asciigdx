package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

/** Unicode version numbers such as "13.0" turned into ints that compare in version order. */
final class UnicodeVersion {

    private static final int MAJOR_WEIGHT = 1000;

    private UnicodeVersion() {
    }

    /**
     * Parses "major.minor" (the minor part is optional). Throws IllegalArgumentException otherwise.
     */
    static int parse(String text) {
        String[] parts = text.strip().split("\\.");
        if (parts.length < 1 || parts.length > 2) {
            throw new IllegalArgumentException("bad Unicode version '" + text + "'");
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length == 2 ? Integer.parseInt(parts[1]) : 0;
            return major * MAJOR_WEIGHT + minor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad Unicode version '" + text + "'", e);
        }
    }
}
