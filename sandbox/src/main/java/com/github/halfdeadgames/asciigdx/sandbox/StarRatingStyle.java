package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.SkinEntryContext;

/** Looks and glyphs of a {@link StarRating}, read from the "starRatings" skin section. */
final class StarRatingStyle {
    private static final int DEFAULT_FILLED_GLYPH = '*';
    private static final int DEFAULT_EMPTY_GLYPH = '-';

    private final LabelStyle normal;
    private final LabelStyle focused;
    private final int filledGlyph;
    private final int emptyGlyph;

    StarRatingStyle(LabelStyle normal, LabelStyle focused, int filledGlyph, int emptyGlyph) {
        this.normal = normal;
        this.focused = focused;
        this.filledGlyph = filledGlyph;
        this.emptyGlyph = emptyGlyph;
    }

    /**
     * Reads one entry: a required "normal" look, an optional "focused" look (defaults to
     * "normal"), and the optional one-character texts "filledGlyph" and "emptyGlyph".
     */
    static StarRatingStyle read(JsonValue entry, SkinEntryContext context) {
        if (!entry.isObject()) {
            throw context.error("must be an object with the keys normal, focused, filledGlyph, "
                    + "and emptyGlyph");
        }
        JsonValue normalValue = entry.get("normal");
        if (normalValue == null) {
            throw context.error("missing \"normal\"");
        }
        LabelStyle normal = context.readLook(normalValue);
        JsonValue focusedValue = entry.get("focused");
        LabelStyle focused = focusedValue == null ? normal : context.readLook(focusedValue);
        return new StarRatingStyle(normal, focused,
                readGlyph(entry, "filledGlyph", DEFAULT_FILLED_GLYPH, context),
                readGlyph(entry, "emptyGlyph", DEFAULT_EMPTY_GLYPH, context));
    }

    private static int readGlyph(JsonValue entry, String key, int fallback,
            SkinEntryContext context) {
        JsonValue value = entry.get(key);
        if (value == null) {
            return fallback;
        }
        String text = value.isString() ? value.asString() : "";
        if (text.codePointCount(0, text.length()) != 1) {
            throw context.error("\"" + key + "\" must be text of exactly one character");
        }
        return text.codePointAt(0);
    }

    LabelStyle getNormal() {
        return normal;
    }

    LabelStyle getFocused() {
        return focused;
    }

    int getFilledGlyph() {
        return filledGlyph;
    }

    int getEmptyGlyph() {
        return emptyGlyph;
    }
}
