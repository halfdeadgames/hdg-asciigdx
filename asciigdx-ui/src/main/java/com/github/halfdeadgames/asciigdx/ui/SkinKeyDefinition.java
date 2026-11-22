package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * One key of a skin file object.
 *
 * @param name the key as written in the file
 * @param type what the value holds
 * @param required whether the key must be present
 * @param defaultDescription in words, what applies when the key is absent
 * @param description one line saying what the key does
 */
record SkinKeyDefinition(String name, SkinKeyType type, boolean required,
        String defaultDescription, String description) {

    private static final String REQUIRED_DEFAULT = "required";

    SkinKeyDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(defaultDescription, "defaultDescription");
        Objects.requireNonNull(description, "description");
        if (description.isBlank()) {
            throw new IllegalArgumentException("The key \"" + name + "\" needs a description");
        }
    }

    static SkinKeyDefinition required(String name, SkinKeyType type, String description) {
        return new SkinKeyDefinition(name, type, true, REQUIRED_DEFAULT, description);
    }

    static SkinKeyDefinition optional(String name, SkinKeyType type, String defaultDescription,
            String description) {
        return new SkinKeyDefinition(name, type, false, defaultDescription, description);
    }
}
