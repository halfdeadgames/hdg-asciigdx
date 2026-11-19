package com.github.halfdeadgames.asciigdx.ui;

/**
 * Thrown when skin file text cannot be turned into a {@link Skin}: the text is not valid JSON, a
 * key is unknown or missing, a value has the wrong type or form, a name is defined twice, or a
 * reference points to a name that does not exist. The message names the place in the file, for
 * example {@code buttons.bar.normal}, and says what is wrong and, where it helps, which names or
 * keys would have been accepted. Only the skin reader creates it.
 */
public final class SkinFileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    SkinFileException(String message) {
        super(message);
    }
}
