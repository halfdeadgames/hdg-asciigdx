package com.github.halfdeadgames.asciigdx.ui;

import java.util.List;
import java.util.Objects;

/**
 * What kind of value a skin file key holds. The reader parses each key by its type, so a new key
 * needs only a line in {@link SkinSchema}. Each type carries the wording its errors need.
 */
sealed interface SkinKeyType {

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
     * @param unit what the number counts, used in the error text, such as {@code cells}
     */
    record WholeNumber(int minimum, String unit) implements SkinKeyType {
        public WholeNumber {
            Objects.requireNonNull(unit, "unit");
        }
    }

    /**
     * A number with decimals allowed, read as a float.
     *
     * @param minimum the smallest allowed value
     * @param maximum the largest allowed value
     * @param expectation completes "must be ..." when the value is not a number
     * @param rangeDescription completes "must be ..." when the number is out of range
     */
    record DecimalNumber(double minimum, double maximum, String expectation,
            String rangeDescription) implements SkinKeyType {
        public DecimalNumber {
            Objects.requireNonNull(expectation, "expectation");
            Objects.requireNonNull(rangeDescription, "rangeDescription");
        }
    }

    /** Either {@code true} or {@code false}. */
    record Flag() implements SkinKeyType {
    }

    /**
     * One of a fixed list of words. The names match the constants of the enum they select,
     * in lower case.
     *
     * @param introduction completes "must be ..." before the option list, such as
     *        {@code one of}
     * @param options the allowed words, in the order shown in errors
     */
    record Choice(String introduction, List<String> options) implements SkinKeyType {
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
        public StyleReference {
            Objects.requireNonNull(kind, "kind");
        }

        static StyleReference nameOnly(SkinStyleKind kind) {
            return new StyleReference(kind, false);
        }

        static StyleReference nameOrObject(SkinStyleKind kind) {
            return new StyleReference(kind, true);
        }
    }

    /**
     * An object nested in the entry, with its own keys.
     *
     * @param schema the keys of the nested object
     */
    record Group(SkinObjectSchema schema) implements SkinKeyType {
        public Group {
            Objects.requireNonNull(schema, "schema");
        }
    }
}
