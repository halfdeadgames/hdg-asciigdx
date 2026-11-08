package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.List;

/** The rules read from one rules file. */
public final class RulesFile {

    private final List<GlyphRule> rules;

    RulesFile(List<GlyphRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Reads a rules file.
     *
     * @param json the file text, lenient JSON
     * @param cellWidth glyph width in pixels, which bounds the column numbers
     * @param cellHeight glyph height in pixels, which bounds the row numbers
     * @return the rules in file order
     * @throws RulesFileException if the text is not a valid rules file
     */
    public static RulesFile parse(String json, int cellWidth, int cellHeight) {
        return RulesFileParser.parse(json, cellWidth, cellHeight);
    }

    public List<GlyphRule> rules() {
        return rules;
    }
}
