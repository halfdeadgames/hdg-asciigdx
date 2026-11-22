package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;
import java.util.List;
import java.util.function.Consumer;

/**
 * The changes the panel makes to a skin tree. Each method returns an edit to pass to
 * {@code SkinDocument.apply}, which runs it on the document's current tree, so one edit is one
 * undo step. An edit throws {@link IllegalArgumentException} or {@link IllegalStateException} when
 * the input or the place is wrong; the document then keeps its old tree.
 */
final class SkinEdits {

    private SkinEdits() {
    }

    /** Sets a key to text: a color name, a glyph, a choice, a mark, or a style name. */
    static Consumer<JsonValue> setText(KeyLocation location, String text) {
        return tree -> replaceValue(location, tree, new JsonValue(text));
    }

    /** Sets a color key to hex digits typed as RRGGBB or RRGGBBAA. */
    static Consumer<JsonValue> setHexColor(KeyLocation location, String typedHex) {
        return tree -> replaceValue(location, tree,
                new JsonValue(HexColorText.toSkinText(typedHex)));
    }

    /** Sets a glyph key to the character named by {@code U+XXXX} text. */
    static Consumer<JsonValue> setGlyph(KeyLocation location, String typedGlyph) {
        return tree -> replaceValue(location, tree,
                new JsonValue(GlyphText.toSkinText(typedGlyph)));
    }

    /** Sets a whole number key; the edit throws when the number is below the minimum. */
    static Consumer<JsonValue> setWholeNumber(KeyLocation location, String typedNumber,
            int minimum) {
        return tree -> {
            long number = parseWholeNumber(typedNumber);
            if (number < minimum) {
                throw new IllegalArgumentException("The number must be at least " + minimum);
            }
            replaceValue(location, tree, new JsonValue(number));
        };
    }

    /** Sets a decimal key; the edit throws when the number is outside the given range. */
    static Consumer<JsonValue> setDecimalNumber(KeyLocation location, String typedNumber,
            double minimum, double maximum) {
        return tree -> {
            double number = parseDecimalNumber(typedNumber);
            if (number < minimum || number > maximum) {
                throw new IllegalArgumentException(describeRange(minimum, maximum));
            }
            replaceValue(location, tree, new JsonValue(number));
        };
    }

    static Consumer<JsonValue> setFlag(KeyLocation location, boolean flag) {
        return tree -> replaceValue(location, tree, new JsonValue(flag));
    }

    /**
     * Adds an absent optional key with a starting value of its type. The edit throws when the
     * tree has no style the value could refer to.
     */
    static Consumer<JsonValue> add(KeyLocation location, SkinKeyType type) {
        return add(location, type, List.of());
    }

    /**
     * Adds an absent optional key and, in the same edit, every partner key it must be given
     * together with (a pair from {@code withKeysTogether}). A partner that is already present is
     * left alone.
     */
    static Consumer<JsonValue> add(KeyLocation location, SkinKeyType type,
            List<SkinKeyDefinition> partners) {
        return tree -> {
            replaceValue(location, tree, SeedValues.seedFor(type, tree));
            for (SkinKeyDefinition partner : partners) {
                KeyLocation partnerLocation = location.sibling(partner.name());
                if (partnerLocation.findValue(tree) == null) {
                    replaceValue(partnerLocation, tree, SeedValues.seedFor(partner.type(), tree));
                }
            }
        };
    }

    /** Removes an optional key, so its default applies again. */
    static Consumer<JsonValue> reset(KeyLocation location) {
        return reset(location, List.of());
    }

    /** Removes an optional key and, in the same edit, the partner keys it is paired with. */
    static Consumer<JsonValue> reset(KeyLocation location, List<String> partnerNames) {
        return tree -> {
            JsonValue owner = requireOwner(location, tree);
            owner.remove(location.key());
            for (String partnerName : partnerNames) {
                owner.remove(partnerName);
            }
        };
    }

    private static void replaceValue(KeyLocation location, JsonValue tree, JsonValue newValue) {
        JsonValue owner = requireOwner(location, tree);
        JsonValue existing = owner.get(location.key());
        if (existing == null) {
            owner.addChild(location.key(), newValue);
            return;
        }
        overwrite(existing, newValue);
    }

    // The node stays in its place, so the key keeps its position in the file.
    private static void overwrite(JsonValue target, JsonValue source) {
        switch (source.type()) {
            case stringValue -> target.set(source.asString());
            case booleanValue -> target.set(source.asBoolean());
            case longValue -> target.set(source.asLong(), source.asString());
            case doubleValue -> target.set(source.asDouble(), source.asString());
            default -> throw new IllegalArgumentException("Cannot write a " + source.type());
        }
        target.child = null;
        target.size = 0;
    }

    private static JsonValue requireOwner(KeyLocation location, JsonValue tree) {
        JsonValue owner = location.findOwner(tree);
        if (owner == null) {
            throw new IllegalStateException("There is no " + location.describe());
        }
        return owner;
    }

    private static String describeRange(double minimum, double maximum) {
        if (maximum >= Float.MAX_VALUE) {
            return "The number must be at least " + minimum;
        }
        return "The number must be from " + minimum + " to " + maximum;
    }

    private static long parseWholeNumber(String typedNumber) {
        try {
            return Long.parseLong(typedNumber.trim());
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("Enter a whole number, such as 2");
        }
    }

    private static double parseDecimalNumber(String typedNumber) {
        try {
            double number = Double.parseDouble(typedNumber.trim());
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("Enter a number, such as 0.5");
        }
    }
}
