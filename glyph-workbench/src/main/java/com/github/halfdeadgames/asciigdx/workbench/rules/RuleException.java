package com.github.halfdeadgames.asciigdx.workbench.rules;

/**
 * Glyphs a rule skips, with the reason the designer gave.
 *
 * @param glyphs the skipped glyphs
 * @param source the group name, or the glyph tokens as written, joined with ", "
 * @param why the reason shown in the rules panel
 */
public record RuleException(GlyphSet glyphs, String source, String why) {}
