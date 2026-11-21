package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link TextField}: its normal, focused, disabled, and placeholder looks, and its
 * caret. Immutable, so one instance can be shared by any number of fields. The field draws its
 * text in the text font.
 *
 * <p>The focused look shows only while the field has keyboard focus and the {@link UiRoot} shows
 * focus (see {@link UiRoot#isFocusVisible()}); the caret shows whenever the field has keyboard
 * focus. The placeholder look draws the placeholder text while the field is empty.
 *
 * <p>The caret is a text-font glyph (see {@link CaretStyle}); it has no background and does not
 * recolor the text.
 *
 * <p>Examples of the two common carets:
 * <pre>{@code
 * LabelStyle look = LabelStyle.of(0xFFFFFFFF, 0x203070FF);
 * // default caret: a thin bar in the field's text color, steady
 * TextFieldStyle bar = TextFieldStyle.of(look);
 * // underline caret: an underscore glyph, blinking
 * TextFieldStyle underline = TextFieldStyle.of(look)
 *         .withCaret(CaretStyle.none().withGlyph('_').withBlinkSeconds(0.5f));
 * }</pre>
 */
public final class TextFieldStyle {

    /** Caret glyph of a style made by {@link #of}: U+258F, a thin bar. */
    public static final int DEFAULT_CARET_GLYPH = 0x258F;

    private final LabelStyle normal;
    private final LabelStyle focused;
    private final LabelStyle disabled;
    private final LabelStyle placeholder;
    private final CaretStyle caret;

    private TextFieldStyle(LabelStyle normal, LabelStyle focused, LabelStyle disabled,
            LabelStyle placeholder, CaretStyle caret) {
        this.normal = normal;
        this.focused = focused;
        this.disabled = disabled;
        this.placeholder = placeholder;
        this.caret = caret;
    }

    /**
     * Creates a style whose focused, disabled, and placeholder looks are copies of the normal
     * look. The caret is a steady thin bar (U+258F) in the normal
     * foreground color. Change the parts with the {@code with} methods.
     *
     * @param normal look when nothing special happens; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if {@code normal} is {@code null}
     */
    public static TextFieldStyle of(LabelStyle normal) {
        Objects.requireNonNull(normal, "normal");
        CaretStyle bar = CaretStyle.none().withGlyph(DEFAULT_CARET_GLYPH)
                .withForegroundRgba(normal.getForegroundRgba());
        return new TextFieldStyle(normal, normal, normal, normal, bar);
    }

    /**
     * Returns a copy with another focused look.
     *
     * @param focused look while the field has visible keyboard focus; must not be {@code null}
     * @return the changed copy
     * @throws NullPointerException if {@code focused} is {@code null}
     */
    public TextFieldStyle withFocused(LabelStyle focused) {
        Objects.requireNonNull(focused, "focused");
        return new TextFieldStyle(normal, focused, disabled, placeholder, caret);
    }

    /**
     * Returns a copy with another disabled look.
     *
     * @param disabled look while the field is disabled; must not be {@code null}
     * @return the changed copy
     * @throws NullPointerException if {@code disabled} is {@code null}
     */
    public TextFieldStyle withDisabled(LabelStyle disabled) {
        Objects.requireNonNull(disabled, "disabled");
        return new TextFieldStyle(normal, focused, disabled, placeholder, caret);
    }

    /**
     * Returns a copy with another placeholder look.
     *
     * @param placeholder look of the placeholder text; must not be {@code null}
     * @return the changed copy
     * @throws NullPointerException if {@code placeholder} is {@code null}
     */
    public TextFieldStyle withPlaceholder(LabelStyle placeholder) {
        Objects.requireNonNull(placeholder, "placeholder");
        return new TextFieldStyle(normal, focused, disabled, placeholder, caret);
    }

    /**
     * Returns a copy with another caret.
     *
     * @param caret how the caret looks; must not be {@code null}
     * @return the changed copy
     * @throws NullPointerException if {@code caret} is {@code null}
     */
    public TextFieldStyle withCaret(CaretStyle caret) {
        Objects.requireNonNull(caret, "caret");
        return new TextFieldStyle(normal, focused, disabled, placeholder, caret);
    }

    /**
     * Returns the look when no other state applies.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getNormal() {
        return normal;
    }

    /**
     * Returns the look while the field has keyboard focus and focus is visible.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getFocused() {
        return focused;
    }

    /**
     * Returns the look while the field is disabled.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getDisabled() {
        return disabled;
    }

    /**
     * Returns the look of the placeholder text.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getPlaceholder() {
        return placeholder;
    }

    /**
     * Returns the caret.
     *
     * @return the caret style, never {@code null}
     */
    public CaretStyle getCaret() {
        return caret;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TextFieldStyle)) {
            return false;
        }
        TextFieldStyle that = (TextFieldStyle) other;
        return normal.equals(that.normal) && focused.equals(that.focused)
                && disabled.equals(that.disabled) && placeholder.equals(that.placeholder)
                && caret.equals(that.caret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normal, focused, disabled, placeholder, caret);
    }

    @Override
    public String toString() {
        return "TextFieldStyle[normal=" + normal + ", focused=" + focused + ", disabled="
                + disabled + ", placeholder=" + placeholder + ", caret=" + caret + "]";
    }
}
