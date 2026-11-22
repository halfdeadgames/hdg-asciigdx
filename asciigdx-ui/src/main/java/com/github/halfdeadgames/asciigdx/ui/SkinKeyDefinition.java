package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * One key of a skin file object. The components use record-style accessors, such as
 * {@code name()} and {@code required()}.
 *
 * @param name the key as written in the file
 * @param type what the value holds
 * @param required whether the key must be present
 * @param defaultDescription in words, what applies when the key is absent; for a required key it
 *        is always the word {@code required}
 * @param description one line saying what the key does
 */
public record SkinKeyDefinition(String name, SkinKeyType type, boolean required,
        String defaultDescription, String description) {

    private static final String REQUIRED_DEFAULT = "required";

    /**
     * Creates a key definition. Prefer {@link #requiredKey} and {@link #optionalKey}, which
     * cannot build an inconsistent definition.
     *
     * @param name the key as written in the file; must not be {@code null}
     * @param type what the value holds; must not be {@code null}
     * @param required whether the key must be present
     * @param defaultDescription in words, what applies when the key is absent; must not be
     *        {@code null}; must be the word {@code required} if {@code required} is {@code true}
     * @param description one line saying what the key does; must not be {@code null} or blank
     * @throws NullPointerException if {@code name}, {@code type}, {@code defaultDescription}, or
     *         {@code description} is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank, or {@code required} is
     *         {@code true} and {@code defaultDescription} is not the word {@code required}
     */
    public SkinKeyDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(defaultDescription, "defaultDescription");
        Objects.requireNonNull(description, "description");
        if (description.isBlank()) {
            throw new IllegalArgumentException("The key \"" + name + "\" needs a description");
        }
        if (required && !REQUIRED_DEFAULT.equals(defaultDescription)) {
            throw new IllegalArgumentException("The required key \"" + name
                    + "\" cannot have a default: " + defaultDescription);
        }
    }

    /**
     * Creates the definition of a key that must be present. Its default description is the word
     * {@code required}.
     *
     * @param name the key as written in the file; must not be {@code null}
     * @param type what the value holds; must not be {@code null}
     * @param description one line saying what the key does; must not be {@code null} or blank
     * @return the definition, never {@code null}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank
     */
    public static SkinKeyDefinition requiredKey(String name, SkinKeyType type,
            String description) {
        return new SkinKeyDefinition(name, type, true, REQUIRED_DEFAULT, description);
    }

    /**
     * Creates the definition of a key that may be left out.
     *
     * @param name the key as written in the file; must not be {@code null}
     * @param type what the value holds; must not be {@code null}
     * @param defaultDescription in words, what applies when the key is absent; must not be
     *        {@code null}
     * @param description one line saying what the key does; must not be {@code null} or blank
     * @return the definition, never {@code null}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank
     */
    public static SkinKeyDefinition optionalKey(String name, SkinKeyType type,
            String defaultDescription, String description) {
        return new SkinKeyDefinition(name, type, false, defaultDescription, description);
    }
}
