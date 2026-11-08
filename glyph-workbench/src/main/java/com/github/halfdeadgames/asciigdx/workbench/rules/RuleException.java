package com.github.halfdeadgames.asciigdx.workbench.rules;

/**
 * Glyphs a rule skips, with the reason the designer gave.
 *
 * @param glyphs the skipped glyphs
 * @param why the reason shown in the rules panel
 */
public record RuleException(GlyphSet glyphs, String why) {}
