package com.github.halfdeadgames.asciigdx.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The keys of one skin file object, in the order the errors list them, plus the rules that span
 * keys. The lists are unmodifiable copies; the schema never changes after it is created.
 *
 * <p>Record components are read with record-style accessors, such as {@code keys()}. Values
 * derived from them are read with getters, such as {@link #getKeyNames()}.
 *
 * @param objectDescription what the object is, with its article, such as {@code a label object}
 * @param keys the known keys, in file order
 * @param removedKeys keys that once existed and get a special message
 * @param togetherPairs pairs of keys that must both be given or both left out
 */
public record SkinObjectSchema(String objectDescription, List<SkinKeyDefinition> keys,
        List<RemovedKey> removedKeys, List<KeyPair> togetherPairs) {

    /**
     * A key that was removed from the format.
     *
     * @param name the old key
     * @param explanation what to tell the author, shown after the key's path
     */
    public record RemovedKey(String name, String explanation) {
        /**
         * Creates a removed key entry.
         *
         * @param name the old key; must not be {@code null}
         * @param explanation what to tell the author; must not be {@code null}
         * @throws NullPointerException if an argument is {@code null}
         */
        public RemovedKey {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(explanation, "explanation");
        }
    }

    /**
     * Two keys that depend on each other.
     *
     * @param first the first key
     * @param second the second key
     */
    public record KeyPair(String first, String second) {
        /**
         * Creates a key pair.
         *
         * @param first the first key; must not be {@code null}
         * @param second the second key; must not be {@code null}
         * @throws NullPointerException if an argument is {@code null}
         */
        public KeyPair {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");
        }
    }

    /**
     * Creates a schema. The lists are copied, so later changes to the arguments do not show here.
     *
     * @param objectDescription what the object is, with its article; must not be {@code null}
     * @param keys the known keys; must not be {@code null} or hold {@code null}
     * @param removedKeys the removed keys; must not be {@code null} or hold {@code null}
     * @param togetherPairs the key pairs; must not be {@code null} or hold {@code null}
     * @throws NullPointerException if an argument, or an element of a list, is {@code null}
     */
    public SkinObjectSchema {
        Objects.requireNonNull(objectDescription, "objectDescription");
        keys = List.copyOf(keys);
        removedKeys = List.copyOf(removedKeys);
        togetherPairs = List.copyOf(togetherPairs);
    }

    /**
     * Creates a schema with the given keys and no removed keys or key pairs.
     *
     * @param objectDescription what the object is, with its article; must not be {@code null}
     * @param keys the known keys, in file order; must not be {@code null} or hold {@code null}
     * @return the schema, never {@code null}
     * @throws NullPointerException if an argument, or an element of {@code keys}, is {@code null}
     */
    public static SkinObjectSchema of(String objectDescription, SkinKeyDefinition... keys) {
        return new SkinObjectSchema(objectDescription, List.of(keys), List.of(), List.of());
    }

    /**
     * Returns a copy of this schema with one more removed key. This schema is not changed.
     *
     * @param name the old key; must not be {@code null}
     * @param explanation what to tell the author, shown after the key's path; must not be
     *        {@code null}
     * @return the new schema, never {@code null}
     * @throws NullPointerException if {@code name} or {@code explanation} is {@code null}
     */
    public SkinObjectSchema withRemovedKey(String name, String explanation) {
        var allRemoved = new ArrayList<>(removedKeys);
        allRemoved.add(new RemovedKey(name, explanation));
        return new SkinObjectSchema(objectDescription, keys, allRemoved, togetherPairs);
    }

    /**
     * Returns a copy of this schema with one more pair of keys that must both be given or both
     * left out. This schema is not changed.
     *
     * @param first the first key; must be one of the names in {@link #keys()}
     * @param second the second key; must be one of the names in {@link #keys()}
     * @return the new schema, never {@code null}
     * @throws NullPointerException if {@code first} or {@code second} is {@code null}
     * @throws IllegalArgumentException if {@code first} or {@code second} is not the name of a
     *         key in {@link #keys()}
     */
    public SkinObjectSchema withKeysTogether(String first, String second) {
        var keyNames = getKeyNames();
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (!keyNames.contains(first) || !keyNames.contains(second)) {
            throw new IllegalArgumentException("Both \"" + first + "\" and \"" + second
                    + "\" must be keys of " + objectDescription);
        }
        var allPairs = new ArrayList<>(togetherPairs);
        allPairs.add(new KeyPair(first, second));
        return new SkinObjectSchema(objectDescription, keys, removedKeys, allPairs);
    }

    /**
     * Returns the names of the known keys, in the same order as {@link #keys()}.
     *
     * @return a new list the caller may change, never {@code null}
     */
    public List<String> getKeyNames() {
        return keys.stream().map(SkinKeyDefinition::name).collect(Collectors.toList());
    }
}
