package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.List;

/**
 * One rule from a rules file.
 *
 * @param name the unique name shown in warnings
 * @param check the check to run
 * @param appliesTo every glyph the rule covers
 * @param settings the rows, columns, or ring size the check uses
 * @param exceptions glyphs the rule skips
 */
public record GlyphRule(String name, RuleCheck check, CheckSettings settings, GlyphSet appliesTo,
        List<RuleException> exceptions) {

    public GlyphRule {
        exceptions = List.copyOf(exceptions);
    }

    /**
     * Tells whether the rule must check this glyph: it is covered and not excepted.
     *
     * @param codePoint the glyph to look up
     * @return true if the check applies
     */
    public boolean appliesTo(int codePoint) {
        if (!appliesTo.contains(codePoint)) {
            return false;
        }
        for (RuleException exception : exceptions) {
            if (exception.glyphs().contains(codePoint)) {
                return false;
            }
        }
        return true;
    }
}
