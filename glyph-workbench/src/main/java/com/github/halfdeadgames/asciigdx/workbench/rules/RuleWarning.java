package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.Objects;

/**
 * One broken design rule of a glyph.
 *
 * @param key identifies the rule, so the message strip can replace or clear its line
 * @param message short text for the message strip
 */
public record RuleWarning(String key, String message) {

    public RuleWarning {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
