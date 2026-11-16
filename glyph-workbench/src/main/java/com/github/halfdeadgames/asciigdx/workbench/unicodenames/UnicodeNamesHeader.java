package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the comment header of the generated name table, including the full Unicode license text.
 */
final class UnicodeNamesHeader {

    private static final Pattern VERSION_LINE = Pattern.compile("^#\\s*DerivedName-(\\S+?)\\.txt");
    private static final String DOWNLOAD_URL = "https://www.unicode.org/Public/UCD/latest/ucd/";

    private UnicodeNamesHeader() {
    }

    /**
     * Reads the Unicode version from the first line of DerivedName.txt, e.g. "#
     * DerivedName-18.0.0.txt".
     */
    static String readVersion(String derivedNameText) {
        String firstLine = derivedNameText.split("\\R", 2)[0];
        Matcher matcher = VERSION_LINE.matcher(firstLine);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "DerivedName.txt must start with '# DerivedName-<version>.txt'");
        }
        return matcher.group(1);
    }

    /** Returns '#' comment lines, each ending in '\n'. */
    static String build(String ucdVersion, String cutoffVersion, String licenseText) {
        StringBuilder header = new StringBuilder();
        appendComment(header,
                "Names, categories, and blocks of Unicode characters that the JDK may not know.");
        appendComment(header, "Generated from the Unicode Character Database " + ucdVersion
                + " (DerivedName, DerivedGeneralCategory, DerivedAge, and Blocks).");
        appendComment(header, "Lists only characters newer than Unicode " + cutoffVersion
                + " (the version in Java 17), plus the name pattern ranges and every block.");
        appendComment(header, "Do not edit by hand. To regenerate:");
        appendComment(header,
                "  1. Download these five files from " + DOWNLOAD_URL + " into one folder:");
        appendComment(header,
                "     DerivedName.txt and DerivedGeneralCategory.txt (both in the extracted/");
        appendComment(header, "     subfolder), DerivedAge.txt, Blocks.txt, and license.txt.");
        appendComment(header,
                "  2. Run: gradlew :glyph-workbench:updateUnicodeNames -PucdDir=<that folder>");
        appendComment(header, "Line formats:");
        appendComment(header,
                "  '<HEX> <CATEGORY> <NAME>' is one character. The category is its two-letter");
        appendComment(header, "  General_Category code, such as Lu or So.");
        appendComment(header,
                "  '<HEX>..<HEX> <CATEGORY> <PREFIX>*' is a range of characters "
                        + "with one category.");
        appendComment(header,
                "  A character's name is the prefix followed by its code point in hex.");
        appendComment(header, "  'BLOCK <HEX>..<HEX> <NAME>' is a Unicode block.");
        appendComment(header, "");
        appendComment(header, "The Unicode data in this file is covered by this license:");
        appendComment(header, "");
        appendLicense(header, licenseText);
        return header.toString();
    }

    private static void appendLicense(StringBuilder header, String licenseText) {
        for (String line : licenseText.strip().split("\\R", -1)) {
            appendComment(header, line.stripTrailing());
        }
    }

    private static void appendComment(StringBuilder header, String text) {
        header.append(text.isEmpty() ? "#" : "# " + text).append('\n');
    }
}
