package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.ArrayList;
import java.util.List;

/** Builds font data text for tests, with an 8x8 cell (7 wide body). */
final class FontDataTexts {

    static final String CELL = "cell 8x8";
    static final String BLANK_BODY_ROW = ".......";
    static final String BLANK_FULL_ROW = "........";

    private FontDataTexts() {
    }

    /** A glyph block whose first rows are given; the rest of the 8 body rows are blank. */
    static String bodyGlyph(String header, String... leadingRows) {
        return glyph(header, BLANK_BODY_ROW, leadingRows);
    }

    /** A full-width glyph block whose first rows are given; the rest of the 8 rows are blank. */
    static String fullGlyph(String header, String... leadingRows) {
        return glyph(header, BLANK_FULL_ROW, leadingRows);
    }

    static String file(String... blocks) {
        List<String> all = new ArrayList<>();
        all.add(CELL);
        all.addAll(List.of(blocks));
        return String.join("\n\n", all) + "\n";
    }

    private static String glyph(String header, String blankRow, String[] leadingRows) {
        List<String> rows = new ArrayList<>(List.of(leadingRows));
        while (rows.size() < 8) {
            rows.add(blankRow);
        }
        return header + "\n" + String.join("\n", rows);
    }
}
