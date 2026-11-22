package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes the starting value of a key that is added to a style. A seed only has to be a valid value
 * of the key's type; the document refuses a seed that the skin reader does not accept.
 */
final class SeedValues {
    private static final String FALLBACK_COLOR = "#FFFFFF";
    private static final String FALLBACK_GLYPH = " ";
    private static final String FALLBACK_MARK = "x";

    private SeedValues() {
    }

    /**
     * Returns a new value for a key of the given type.
     *
     * @throws IllegalStateException if the type needs a style name and the tree has none
     */
    static JsonValue seedFor(SkinKeyType type, JsonValue tree) {
        if (type instanceof SkinKeyType.Color) {
            List<String> palette = namesIn(tree, SkinSchema.COLORS);
            return new JsonValue(palette.isEmpty() ? FALLBACK_COLOR : palette.get(0));
        }
        if (type instanceof SkinKeyType.Glyph) {
            return new JsonValue(FALLBACK_GLYPH);
        }
        if (type instanceof SkinKeyType.Mark) {
            return new JsonValue(FALLBACK_MARK);
        }
        if (type instanceof SkinKeyType.WholeNumber whole) {
            return new JsonValue((long) whole.minimum());
        }
        if (type instanceof SkinKeyType.DecimalNumber decimal) {
            return new JsonValue(Math.min(Math.max(0.0, decimal.minimum()), decimal.maximum()));
        }
        if (type instanceof SkinKeyType.Flag) {
            return new JsonValue(false);
        }
        if (type instanceof SkinKeyType.Choice choice) {
            return new JsonValue(choice.options().get(0));
        }
        if (type instanceof SkinKeyType.StyleReference reference) {
            return seedStyleName(reference, tree);
        }
        if (type instanceof SkinKeyType.Group group) {
            return seedObject(group.schema().keys(), tree);
        }
        throw new IllegalStateException("No starting value for " + type);
    }

    static List<String> namesIn(JsonValue tree, String section) {
        List<String> names = new ArrayList<>();
        JsonValue entries = tree.get(section);
        if (entries == null) {
            return names;
        }
        for (JsonValue entry = entries.child; entry != null; entry = entry.next) {
            names.add(entry.name);
        }
        return names;
    }

    private static JsonValue seedStyleName(SkinKeyType.StyleReference reference, JsonValue tree) {
        String section = StyleReferenceEditor.sectionOf(reference.kind());
        List<String> names = namesIn(tree, section);
        if (names.isEmpty()) {
            throw new IllegalStateException("There is no style in " + section + " to refer to");
        }
        return new JsonValue(names.get(0));
    }

    // Only required keys are seeded; optional keys keep their defaults.
    private static JsonValue seedObject(List<SkinKeyDefinition> keys, JsonValue tree) {
        JsonValue object = new JsonValue(JsonValue.ValueType.object);
        for (SkinKeyDefinition key : keys) {
            if (key.required()) {
                object.addChild(key.name(), seedFor(key.type(), tree));
            }
        }
        return object;
    }
}
