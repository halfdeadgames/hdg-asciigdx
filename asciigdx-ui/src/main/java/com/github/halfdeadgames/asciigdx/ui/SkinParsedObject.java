package com.github.halfdeadgames.asciigdx.ui;

import java.util.Map;

/**
 * The values of one skin file object after parsing, by key. Only keys that were present in the
 * file have a value; the reader has already checked each value against its schema type.
 */
final class SkinParsedObject {

    private final Map<String, Object> values;

    SkinParsedObject(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    boolean has(String key) {
        return values.containsKey(key);
    }

    <T> T get(String key, Class<T> expectedType) {
        Object value = values.get(key);
        if (value == null) {
            throw new IllegalStateException("The key \"" + key + "\" was not given");
        }
        return expectedType.cast(value);
    }

    <T> T getOrElse(String key, Class<T> expectedType, T fallback) {
        if (!has(key)) {
            return fallback;
        }
        return get(key, expectedType);
    }

    int getInt(String key) {
        return get(key, Integer.class);
    }

    float getFloat(String key) {
        return get(key, Float.class);
    }

    String getText(String key) {
        return get(key, String.class);
    }
}
