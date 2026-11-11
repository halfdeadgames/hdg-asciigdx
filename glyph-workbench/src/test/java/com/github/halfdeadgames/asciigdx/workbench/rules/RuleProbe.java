package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.List;
import java.util.Optional;

/** Runs a single check through {@link RuleChecker#check} and returns the problem detail it reported. */
final class RuleProbe {

    private static final String RULE_NAME = "Probe";
    private static final String MESSAGE_PREFIX = "PROBE: ";
    private static final int CODE_POINT = 'A';

    private RuleProbe() {
    }

    static Optional<String> problem(RuleCheck check, CheckSettings settings, PixelGrid pixels) {
        var rule = new GlyphRule(RULE_NAME, check, settings, GlyphSet.all(), List.of("all"), List.of());
        var warnings = RuleChecker.check(new RulesFile(List.of(rule)), CODE_POINT,
            new PixelGlyph(CODE_POINT, pixels));
        if (warnings.isEmpty()) {
            return Optional.empty();
        }
        if (warnings.size() > 1) {
            throw new AssertionError("expected one warning, got " + warnings);
        }
        String message = warnings.get(0).message();
        if (!message.startsWith(MESSAGE_PREFIX)) {
            throw new AssertionError("expected the message to start with '" + MESSAGE_PREFIX + "': " + message);
        }
        return Optional.of(message.substring(MESSAGE_PREFIX.length()));
    }
}
