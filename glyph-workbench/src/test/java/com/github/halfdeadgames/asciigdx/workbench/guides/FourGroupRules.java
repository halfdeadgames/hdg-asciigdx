package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;

/** A rules file with the four groups the guide tests use, and one glyph from each. */
final class FourGroupRules {

    static final int UPPER = 'A';
    static final int LOWER = 'a';
    static final int DIGIT = '5';
    static final int PUNCT = '!';

    static final RulesFile RULES = RulesFile.parse("""
            {"groups": {
              "uppercase": ["A-Z"],
              "lowercase": ["a-z"],
              "digits": ["0-9"],
              "punctuation": ["!-/", ":-@", "[-`", "{-~"]
            }}
            """, 8, 8);

    private FourGroupRules() {
    }
}
