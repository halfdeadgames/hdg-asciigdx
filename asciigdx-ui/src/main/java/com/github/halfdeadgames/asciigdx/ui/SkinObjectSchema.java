package com.github.halfdeadgames.asciigdx.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The keys of one skin file object, in the order the errors list them, plus the rules that span
 * keys.
 *
 * @param objectDescription what the object is, with its article, such as {@code a label object}
 * @param keys the known keys
 * @param removedKeys keys that once existed and get a special message
 * @param togetherPairs pairs of keys that must both be given or both left out
 */
record SkinObjectSchema(String objectDescription, List<SkinKeyDefinition> keys,
        List<RemovedKey> removedKeys, List<KeyPair> togetherPairs) {

    /**
     * A key that was removed from the format.
     *
     * @param name the old key
     * @param explanation what to tell the author, shown after the key's path
     */
    record RemovedKey(String name, String explanation) {
    }

    /**
     * Two keys that depend on each other.
     *
     * @param first the first key
     * @param second the second key
     */
    record KeyPair(String first, String second) {
    }

    SkinObjectSchema {
        Objects.requireNonNull(objectDescription, "objectDescription");
        keys = List.copyOf(keys);
        removedKeys = List.copyOf(removedKeys);
        togetherPairs = List.copyOf(togetherPairs);
    }

    static SkinObjectSchema of(String objectDescription, SkinKeyDefinition... keys) {
        return new SkinObjectSchema(objectDescription, List.of(keys), List.of(), List.of());
    }

    SkinObjectSchema withRemovedKey(String name, String explanation) {
        var allRemoved = new ArrayList<>(removedKeys);
        allRemoved.add(new RemovedKey(name, explanation));
        return new SkinObjectSchema(objectDescription, keys, allRemoved, togetherPairs);
    }

    SkinObjectSchema withKeysTogether(String first, String second) {
        var allPairs = new ArrayList<>(togetherPairs);
        allPairs.add(new KeyPair(first, second));
        return new SkinObjectSchema(objectDescription, keys, removedKeys, allPairs);
    }

    List<String> getKeyNames() {
        return keys.stream().map(SkinKeyDefinition::name).collect(Collectors.toList());
    }
}
