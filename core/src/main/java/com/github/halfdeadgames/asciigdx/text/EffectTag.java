package com.github.halfdeadgames.asciigdx.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The name and settings of one {@code [effect:name key=value ...]} tag, after the prefix. */
record EffectTag(String name, Map<String, String> settings) {

    private static final String SETTING_SEPARATOR = " ";
    private static final char KEY_VALUE_SEPARATOR = '=';

    /**
     * Reads the text after {@code effect:}. Empty when the tag is malformed: empty name, empty
     * key or value, a missing {@code =}, a repeated key, or extra spaces.
     */
    static Optional<EffectTag> read(String body) {
        String[] tokens = body.split(SETTING_SEPARATOR, -1);
        if (tokens[0].isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> settings = new LinkedHashMap<>();
        for (int index = 1; index < tokens.length; index++) {
            if (!addSetting(settings, tokens[index])) {
                return Optional.empty();
            }
        }
        return Optional.of(new EffectTag(tokens[0], Collections.unmodifiableMap(settings)));
    }

    private static boolean addSetting(Map<String, String> settings, String token) {
        int separatorIndex = token.indexOf(KEY_VALUE_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == token.length() - 1) {
            return false;
        }
        String key = token.substring(0, separatorIndex);
        return settings.putIfAbsent(key, token.substring(separatorIndex + 1)) == null;
    }
}
