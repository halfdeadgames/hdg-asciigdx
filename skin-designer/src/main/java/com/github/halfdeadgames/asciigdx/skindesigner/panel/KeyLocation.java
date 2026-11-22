package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Where one key sits in the skin tree: the section, the style, the group keys between the style
 * and the key, and the key itself. It finds its place in whatever tree it is given, because the
 * tree of a document is replaced after every change.
 */
record KeyLocation(String section, String styleName, List<String> groupPath, String key) {

    KeyLocation {
        groupPath = List.copyOf(groupPath);
    }

    static KeyLocation ofStyleKey(String section, String styleName, String key) {
        return new KeyLocation(section, styleName, List.of(), key);
    }

    /** Returns the location of a key inside the group this location points at. */
    KeyLocation nested(String nestedKey) {
        List<String> deeperPath = new ArrayList<>(groupPath);
        deeperPath.add(key);
        return new KeyLocation(section, styleName, deeperPath, nestedKey);
    }

    /** Returns the location of another key in the same object. */
    KeyLocation sibling(String siblingKey) {
        return new KeyLocation(section, styleName, groupPath, siblingKey);
    }

    /** Returns the object that holds the key, or {@code null} when the tree has none. */
    JsonValue findOwner(JsonValue tree) {
        JsonValue owner = childOrNull(childOrNull(tree, section), styleName);
        for (String groupKey : groupPath) {
            owner = childOrNull(owner, groupKey);
        }
        return owner;
    }

    /** Returns the value of the key, or {@code null} when the key is absent. */
    JsonValue findValue(JsonValue tree) {
        return childOrNull(findOwner(tree), key);
    }

    String describe() {
        StringBuilder path = new StringBuilder(section).append('.').append(styleName);
        for (String groupKey : groupPath) {
            path.append('.').append(groupKey);
        }
        return path.append('.').append(key).toString();
    }

    private static JsonValue childOrNull(JsonValue parent, String name) {
        if (parent == null) {
            return null;
        }
        return parent.get(name);
    }
}
