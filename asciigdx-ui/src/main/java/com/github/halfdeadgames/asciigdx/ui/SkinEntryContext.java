package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.utils.JsonValue;
import java.util.Objects;

/**
 * Gives a {@link SkinSectionReader} the same reading rules as the built-in sections: colors,
 * looks, and errors that name the entry. It has no public constructor, and it is valid only while
 * {@link SkinSectionReader#readEntry} runs; afterward every method throws
 * {@link IllegalStateException}. Do not keep it. This class is final and not thread-safe; use it
 * only on the thread that loads the skin.
 */
public final class SkinEntryContext {

    private final SkinFileReader reader;
    private final String path;
    private boolean valid = true;

    SkinEntryContext(SkinFileReader reader, String path) {
        this.reader = reader;
        this.path = path;
    }

    void invalidate() {
        valid = false;
    }

    private void requireValid() {
        if (!valid) {
            throw new IllegalStateException(
                    "The context is valid only while the entry is being read");
        }
    }

    /**
     * Returns the place of the entry being read, in the form used by built-in errors: the section
     * name, a dot, and the entry name, such as {@code starRatings.default}.
     *
     * @return the path, never {@code null}
     * @throws IllegalStateException if called after {@code readEntry} returned
     */
    public String getPath() {
        requireValid();
        return path;
    }

    /**
     * Reads a color value with the rules of the built-in sections: text that is {@code #RRGGBB}
     * (fully opaque) or {@code #RRGGBBAA}, or the name of an entry in the {@code colors} section.
     *
     * @param value the JSON value; must not be {@code null}
     * @return the color packed as 0xRRGGBBAA (red in the highest byte)
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws SkinFileException if the value is not text, is malformed hex, or names an unknown
     *         color; the message starts with the entry's path
     * @throws IllegalStateException if called after {@code readEntry} returned
     */
    public int readColor(JsonValue value) {
        requireValid();
        Objects.requireNonNull(value, "value");
        return reader.readColor(value, path);
    }

    /**
     * Reads a look with the rules of the built-in sections: the name of an entry in the
     * {@code labels} section, or an inline object with the keys {@code font}, {@code foreground},
     * and {@code background}.
     *
     * @param value the JSON value; must not be {@code null}
     * @return the label style, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws SkinFileException if the value is neither, or the object breaks the label rules; the
     *         message starts with the entry's path
     * @throws IllegalStateException if called after {@code readEntry} returned
     */
    public LabelStyle readLook(JsonValue value) {
        requireValid();
        Objects.requireNonNull(value, "value");
        return reader.readLook(value, path);
    }

    /**
     * Creates an exception for a bad entry. Throw it: {@code throw context.error("...")}. The
     * message is the entry's path, a colon, a space, and the given text. The path is the
     * only location the message gives.
     *
     * @param message what is wrong, for example {@code must be a whole number}; must not be
     *         {@code null}
     * @return the exception to throw, never {@code null}
     * @throws NullPointerException if {@code message} is {@code null}
     * @throws IllegalStateException if called after {@code readEntry} returned
     */
    public SkinFileException error(String message) {
        requireValid();
        Objects.requireNonNull(message, "message");
        return new SkinFileException(path + ": " + message);
    }
}
