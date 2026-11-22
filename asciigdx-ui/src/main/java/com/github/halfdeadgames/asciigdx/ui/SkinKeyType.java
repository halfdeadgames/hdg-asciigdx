package com.github.halfdeadgames.asciigdx.ui;

import java.util.List;
import java.util.Objects;

/**
 * What kind of value a skin file key holds. The reader parses each key by its type, so a new key
 * needs only a line in {@link SkinSchema}. Each type carries the wording its errors need. The
 * permitted types are the nested records; test for them with {@code instanceof}. New key types
 * may be added in later releases, so a {@code switch} over them needs a {@code default} branch.
 * Record components use record-style accessors, such as {@code minimum()}. Wording components
 * ({@code unit}, {@code expectation}, {@code rangeDescription}, {@code introduction}) are
 * display text for messages with no fixed format.
 */
public sealed interface SkinKeyType {

    /** A color: hex text such as {@code #RRGGBB} or {@code #RRGGBBAA}, or the name of a color. */
    record Color() implements SkinKeyType {
    }

    /** Text of exactly one code point. */
    record Glyph() implements SkinKeyType {
    }

    /** Text of at least one character. */
    record Mark() implements SkinKeyType {
    }

    /**
     * A whole number.
     *
     * @param minimum the smallest allowed value
     * @param unit what the number counts, display text for messages with no fixed format, such as
     *        {@code cells}
     */
    record WholeNumber(int minimum, String unit) implements SkinKeyType {
        /**
         * Creates a whole number type.
         *
         * @param minimum the smallest allowed value
         * @param unit what the number counts; must not be {@code null}
         * @throws NullPointerException if {@code unit} is {@code null}
         */
        public WholeNumber {
            Objects.requireNonNull(unit, "unit");
        }
    }

    /**
     * A number with decimals allowed, read as a float. The bounds apply after the value is
     * converted to float.
     *
     * @param minimum the smallest allowed value
     * @param maximum the largest allowed value
     * @param expectation display text that completes "must be ..." when the value is not a
     *        number; no fixed format
     * @param rangeDescription display text that completes "must be ..." when the number is out
     *        of range; no fixed format
     */
    record DecimalNumber(double minimum, double maximum, String expectation,
            String rangeDescription) implements SkinKeyType {
        /**
         * Creates a decimal number type.
         *
         * @param minimum the smallest allowed value
         * @param maximum the largest allowed value
         * @param expectation completes "must be ..." when the value is not a number; must not be
         *        {@code null}
         * @param rangeDescription completes "must be ..." when the number is out of range; must
         *        not be {@code null}
         * @throws NullPointerException if {@code expectation} or {@code rangeDescription} is
         *         {@code null}
         */
        public DecimalNumber {
            Objects.requireNonNull(expectation, "expectation");
            Objects.requireNonNull(rangeDescription, "rangeDescription");
        }
    }

    /** Either {@code true} or {@code false}. */
    record Flag() implements SkinKeyType {
    }

    /**
     * One of a fixed list of words.
     *
     * @param introduction display text that completes "must be ..." before the option list, such
     *        as {@code one of}; no fixed format
     * @param options the allowed words, in the order shown in errors; an unmodifiable copy
     */
    record Choice(String introduction, List<String> options) implements SkinKeyType {
        /**
         * Creates a choice type. The options are copied.
         *
         * @param introduction completes "must be ..." before the option list; must not be
         *        {@code null}
         * @param options the allowed words; must not be {@code null} or hold {@code null}
         * @throws NullPointerException if an argument, or an element of {@code options}, is
         *         {@code null}
         */
        public Choice {
            Objects.requireNonNull(introduction, "introduction");
            options = List.copyOf(options);
        }
    }

    /**
     * The name of a style from an earlier section, or, where allowed, an inline object.
     *
     * @param kind which kind of style the reference points at
     * @param inlineAllowed whether an object written in place may stand for the name
     */
    record StyleReference(SkinStyleKind kind, boolean inlineAllowed) implements SkinKeyType {
        /**
         * Creates a style reference type.
         *
         * @param kind which kind of style the reference points at; must not be {@code null}
         * @param inlineAllowed whether an object written in place may stand for the name
         * @throws NullPointerException if {@code kind} is {@code null}
         */
        public StyleReference {
            Objects.requireNonNull(kind, "kind");
        }

        /**
         * Returns a reference that accepts only the name of a style.
         *
         * @param kind which kind of style the reference points at; must not be {@code null}
         * @return the type, never {@code null}
         * @throws NullPointerException if {@code kind} is {@code null}
         */
        public static StyleReference nameOnly(SkinStyleKind kind) {
            return new StyleReference(kind, false);
        }

        /**
         * Returns a reference that accepts the name of a style or an inline object.
         *
         * @param kind which kind of style the reference points at; must not be {@code null}
         * @return the type, never {@code null}
         * @throws NullPointerException if {@code kind} is {@code null}
         */
        public static StyleReference nameOrObject(SkinStyleKind kind) {
            return new StyleReference(kind, true);
        }
    }

    /**
     * An object nested in the entry, with its own keys.
     *
     * @param schema the keys of the nested object
     */
    record Group(SkinObjectSchema schema) implements SkinKeyType {
        /**
         * Creates a group type.
         *
         * @param schema the keys of the nested object; must not be {@code null}
         * @throws NullPointerException if {@code schema} is {@code null}
         */
        public Group {
            Objects.requireNonNull(schema, "schema");
        }
    }
}
