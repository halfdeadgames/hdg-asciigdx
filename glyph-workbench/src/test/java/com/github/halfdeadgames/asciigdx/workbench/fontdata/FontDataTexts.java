package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.ArrayList;
import java.util.List;

/** Builds font data text for tests, with an 8x8 cell. */
final class FontDataTexts {

    static final String CELL = "cell 8x8";
    static final String BLANK_ROW = "........";

    private FontDataTexts() {
    }

    /** A glyph block whose first rows are given; the rest of the 8 rows are blank. */
    static String glyph(String header, String... leadingRows) {
        List<String> rows = new ArrayList<>(List.of(leadingRows));
        while (rows.size() < 8) {
            rows.add(BLANK_ROW);
        }
        return header + "\n" + String.join("\n", rows);
    }

    static String file(String... blocks) {
        List<String> all = new ArrayList<>();
        all.add(CELL);
        all.addAll(List.of(blocks));
        return String.join("\n\n", all) + "\n";
    }
}
