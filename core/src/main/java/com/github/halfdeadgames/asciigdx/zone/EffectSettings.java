package com.github.halfdeadgames.asciigdx.zone;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The {@code key=value} settings of one effect tag, such as {@code amplitude=0.5 period=2}. An
 * {@link EffectFactory} reads them by name and gives a default for each one the tag may leave out.
 * Not thread-safe; it is used during one parse only.
 *
 * <p>Every getter marks its key as read. After the factory returns, {@link EffectRegistry}
 * rejects a tag that holds a key the factory never read, so a typo such as {@code amplitde=1}
 * leaves the tag visible instead of being ignored. A factory must therefore read every setting it
 * supports, even when an earlier one is not needed. Only {@link #has} leaves a key unread.
 *
 * <p>This object is only valid while the factory runs. Once {@code create} returns it is invalid
 * and must not be kept or read again.
 *
 * <p>Example:
 * <pre>{@code
 * registry.register("tilt", settings -> CellEffects.spin(settings.getFloat("degrees", 10f),
 *         settings.getPhase("phase", Phase.sync())));
 * }</pre>
 */
public final class EffectSettings {

    private static final String SYNC_PHASE = "sync";
    private static final String COLUMNS_PHASE = "columns";
    private static final String DISTANCE_PHASE = "distance";
    private static final String SCATTERED_PHASE = "scattered";
    private static final char PHASE_ARGUMENT_SEPARATOR = ':';
    private static final Pattern DECIMAL_NUMBER =
            Pattern.compile("-?[0-9]+(\\.[0-9]+)?([eE][-+]?[0-9]+)?");
    private static final Pattern WHOLE_NUMBER = Pattern.compile("-?[0-9]+");
    private static final int HEX_RADIX = 16;
    private static final int HEX_DIGITS_BEFORE_ALPHA = 6;
    private static final int HEX_DIGITS_WITH_ALPHA = 8;
    private static final int BITS_PER_HEX_DIGIT = 4;
    private static final int BITS_PER_CHANNEL = 8;
    private static final int OPAQUE_ALPHA = 0xFF;
    private static final String COLOR_FORM = "a color as RRGGBB or RRGGBBAA hex digits";

    private final Map<String, String> values;
    private final Set<String> readKeys = new HashSet<>();

    private EffectSettings(Map<String, String> values) {
        this.values = values;
    }

    static EffectSettings of(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "key"),
                    Objects.requireNonNull(entry.getValue(), "value of " + entry.getKey()));
        }
        return new EffectSettings(copy);
    }

    /**
     * Returns whether the tag has a setting with a name. Unlike the getters it does not mark the
     * key as read, so a factory that only asks still has to read the key with a getter.
     *
     * @param name the setting name, such as {@code "color"}; not {@code null}
     * @return {@code true} if the tag wrote this setting
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean has(String name) {
        Objects.requireNonNull(name, "name");
        return values.containsKey(name);
    }

    /**
     * Reads a number setting and marks the key as read. A factory must read every setting it
     * supports, because the registry rejects keys that were never read.
     *
     * @param name the setting name, such as {@code "amplitude"}; not {@code null}
     * @param defaultValue the value to return when the tag has no such setting; returned as is
     * @return the finite decimal number written in the tag, or {@code defaultValue}. The text is an
     *         optional minus, digits, an optional fraction ({@code .5}), and an optional exponent
     *         ({@code e3}); NaN, infinity, a value too large for a float, suffixes such as
     *         {@code f} or {@code d}, hex floats, and a leading plus are rejected
     * @throws IllegalArgumentException if the text is not a finite decimal number. Range checks are
     *         done later, by the effect that receives the number
     */
    public float getFloat(String name, float defaultValue) {
        String text = readText(name);
        if (text == null) {
            return defaultValue;
        }
        try {
            return parseFiniteFloat(text);
        } catch (NumberFormatException notNumber) {
            throw badValue(name, "a finite decimal number", text);
        }
    }

    /**
     * Reads a whole number setting and marks the key as read. A factory must read every setting
     * it supports, because the registry rejects keys that were never read.
     *
     * @param name the setting name, such as {@code "seed"}; not {@code null}
     * @param defaultValue the value to return when the tag has no such setting; returned as is
     * @return the int written in the tag, or {@code defaultValue}. The text is an optional minus
     *         and ASCII digits 0 to 9 only; a leading plus and non-ASCII digits are rejected
     * @throws IllegalArgumentException if the text is not a whole number that fits an int
     */
    public int getInt(String name, int defaultValue) {
        String text = readText(name);
        if (text == null) {
            return defaultValue;
        }
        try {
            return parseWholeNumber(text);
        } catch (NumberFormatException notNumber) {
            throw badValue(name, "a whole number", text);
        }
    }

    /**
     * Reads a text setting and marks the key as read. A factory must read every setting it
     * supports, because the registry rejects keys that were never read. The text has no spaces,
     * because spaces separate settings.
     *
     * @param name the setting name, such as {@code "glyphs"}; not {@code null}
     * @param defaultValue the text to return when the tag has no such setting; returned as is, may
     *         be {@code null}
     * @return the text written in the tag, never empty, or {@code defaultValue}. It is
     *         {@code null} only when {@code defaultValue} is {@code null} and the tag has no such
     *         setting
     */
    public String getString(String name, String defaultValue) {
        String text = readText(name);
        return text == null ? defaultValue : text;
    }

    /**
     * Reads a {@link Phase} setting and marks the key as read. A factory must read every setting
     * it supports, because the registry rejects keys that were never read. The text is
     * {@code sync}, {@code columns:N} (see {@link Phase#alongColumns}), {@code distance:N} (see
     * {@link Phase#byDistance}), or {@code scattered:SEED} (see {@link Phase#scattered}). N is a
     * finite decimal number as for {@link #getFloat}, and SEED is a whole number as for
     * {@link #getInt}.
     *
     * @param name the setting name, usually {@code "phase"}; not {@code null}
     * @param defaultPhase the phase to return when the tag has no such setting; returned as is
     * @return the phase written in the tag, or {@code defaultPhase}
     * @throws IllegalArgumentException if the text is not one of the forms above or its number is
     *         bad
     */
    public Phase getPhase(String name, Phase defaultPhase) {
        String text = readText(name);
        if (text == null) {
            return defaultPhase;
        }
        try {
            return parsePhase(text);
        } catch (IllegalArgumentException badPhase) {
            throw badValue(name, "sync, columns:N, distance:N, or scattered:SEED", text);
        }
    }

    /**
     * Reads a color setting and marks the key as read. A factory must read every setting it
     * supports, because the registry rejects keys that were never read. The text is six hex digits
     * {@code RRGGBB} (fully opaque) or eight hex digits {@code RRGGBBAA}, with no {@code #}. Hex
     * digits are case-insensitive and ASCII only.
     *
     * @param name the setting name, such as {@code "color"}; not {@code null}
     * @param defaultRgba the color to return when the tag has no such setting, as RGBA8888
     *         ({@code 0xRRGGBBAA}); returned as is
     * @return the color written in the tag as RGBA8888 ({@code 0xRRGGBBAA}), or
     *         {@code defaultRgba}
     * @throws IllegalArgumentException if the text is not six or eight hex digits
     */
    public int getRgba(String name, int defaultRgba) {
        String text = readText(name);
        if (text == null) {
            return defaultRgba;
        }
        return parseRgba(name, text);
    }

    /** Returns a key that no getter asked for, or {@code null} when every key was read. */
    String findUnreadKey() {
        for (String key : values.keySet()) {
            if (!readKeys.contains(key)) {
                return key;
            }
        }
        return null;
    }

    private String readText(String name) {
        Objects.requireNonNull(name, "name");
        readKeys.add(name);
        return values.get(name);
    }

    private static float parseFiniteFloat(String text) {
        if (!DECIMAL_NUMBER.matcher(text).matches()) {
            throw new NumberFormatException(text);
        }
        float value = Float.parseFloat(text);
        if (Float.isInfinite(value)) {
            throw new NumberFormatException(text);
        }
        return value;
    }

    private static int parseWholeNumber(String text) {
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            throw new NumberFormatException(text);
        }
        return Integer.parseInt(text);
    }

    private static Phase parsePhase(String text) {
        if (text.equals(SYNC_PHASE)) {
            return Phase.sync();
        }
        int separatorIndex = text.indexOf(PHASE_ARGUMENT_SEPARATOR);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException(text);
        }
        String kind = text.substring(0, separatorIndex);
        String argument = text.substring(separatorIndex + 1);
        return switch (kind) {
            case COLUMNS_PHASE -> Phase.alongColumns(parseFiniteFloat(argument));
            case DISTANCE_PHASE -> Phase.byDistance(parseFiniteFloat(argument));
            case SCATTERED_PHASE -> Phase.scattered(parseWholeNumber(argument));
            default -> throw new IllegalArgumentException(text);
        };
    }

    private static int parseRgba(String name, String text) {
        if (text.length() != HEX_DIGITS_BEFORE_ALPHA && text.length() != HEX_DIGITS_WITH_ALPHA) {
            throw badValue(name, COLOR_FORM, text);
        }
        long digits = 0;
        for (int index = 0; index < text.length(); index++) {
            int digit = hexDigitValue(text.charAt(index));
            if (digit < 0) {
                throw badValue(name, COLOR_FORM, text);
            }
            digits = (digits << BITS_PER_HEX_DIGIT) | digit;
        }
        if (text.length() == HEX_DIGITS_BEFORE_ALPHA) {
            return (int) ((digits << BITS_PER_CHANNEL) | OPAQUE_ALPHA);
        }
        return (int) digits;
    }

    // Character.digit also accepts non-ASCII digits, which a color must not contain.
    private static int hexDigitValue(char character) {
        if (character > 'f') {
            return -1;
        }
        return Character.digit(character, HEX_RADIX);
    }

    private static IllegalArgumentException badValue(String name, String expected, String text) {
        return new IllegalArgumentException(
                "Setting '" + name + "' must be " + expected + ", not '" + text + "'");
    }
}
