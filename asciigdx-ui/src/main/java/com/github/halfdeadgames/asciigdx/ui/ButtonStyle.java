package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link TextButton} in its four states, and the space left and right of its text.
 * Immutable, so one instance can be shared by any number of buttons. All four looks use the same
 * font, so a button never changes height when its state changes.
 */
public final class ButtonStyle {

    /** Horizontal padding of a new style, in UI cells. */
    public static final int DEFAULT_HORIZONTAL_PADDING = 1;

    private final LabelStyle normal;
    private final LabelStyle pointerOver;
    private final LabelStyle pressed;
    private final LabelStyle disabled;
    private final int horizontalPadding;

    private ButtonStyle(LabelStyle normal, LabelStyle pointerOver, LabelStyle pressed,
            LabelStyle disabled, int horizontalPadding) {
        this.normal = normal;
        this.pointerOver = pointerOver;
        this.pressed = pressed;
        this.disabled = disabled;
        this.horizontalPadding = horizontalPadding;
    }

    /**
     * Creates a style with a look for each state and a horizontal padding of
     * {@link #DEFAULT_HORIZONTAL_PADDING}.
     *
     * @param normal look when nothing special happens; must not be {@code null}
     * @param pointerOver look while the pointer is over the button; must not be {@code null}
     * @param pressed look while the button is pressed; must not be {@code null}
     * @param disabled look while the button is disabled; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the four looks do not all use the same font
     */
    public static ButtonStyle of(LabelStyle normal, LabelStyle pointerOver, LabelStyle pressed,
            LabelStyle disabled) {
        Objects.requireNonNull(normal, "normal");
        Objects.requireNonNull(pointerOver, "pointerOver");
        Objects.requireNonNull(pressed, "pressed");
        Objects.requireNonNull(disabled, "disabled");
        UiFont font = normal.getFont();
        if (pointerOver.getFont() != font || pressed.getFont() != font
                || disabled.getFont() != font) {
            throw new IllegalArgumentException("All looks must use the same font, were " + font
                    + ", " + pointerOver.getFont() + ", " + pressed.getFont() + ", and "
                    + disabled.getFont());
        }
        return new ButtonStyle(normal, pointerOver, pressed, disabled,
                DEFAULT_HORIZONTAL_PADDING);
    }

    /**
     * Creates a style that looks the same in all four states, with a horizontal padding of
     * {@link #DEFAULT_HORIZONTAL_PADDING}.
     *
     * @param all look for every state; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if {@code all} is {@code null}
     */
    public static ButtonStyle of(LabelStyle all) {
        return of(all, all, all, all);
    }

    /**
     * Returns a copy with another horizontal padding.
     *
     * @param cells space on each side of the text, in UI cells; zero or greater
     * @return a style with the same looks and that padding
     * @throws IllegalArgumentException if {@code cells} is negative
     */
    public ButtonStyle withHorizontalPadding(int cells) {
        if (cells < 0) {
            throw new IllegalArgumentException("Padding must not be negative, was " + cells);
        }
        return new ButtonStyle(normal, pointerOver, pressed, disabled, cells);
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
     * Returns the look while the pointer is over the button.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getPointerOver() {
        return pointerOver;
    }

    /**
     * Returns the look while the button is pressed.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getPressed() {
        return pressed;
    }

    /**
     * Returns the look while the button is disabled.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getDisabled() {
        return disabled;
    }

    /**
     * Returns the space on each side of the text.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getHorizontalPadding() {
        return horizontalPadding;
    }

    /**
     * Returns the font shared by all four looks.
     *
     * @return the font, never {@code null}
     */
    public UiFont getFont() {
        return normal.getFont();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ButtonStyle)) {
            return false;
        }
        ButtonStyle that = (ButtonStyle) other;
        return horizontalPadding == that.horizontalPadding && normal.equals(that.normal)
                && pointerOver.equals(that.pointerOver) && pressed.equals(that.pressed)
                && disabled.equals(that.disabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normal, pointerOver, pressed, disabled, horizontalPadding);
    }

    @Override
    public String toString() {
        return "ButtonStyle[normal=" + normal + ", pointerOver=" + pointerOver + ", pressed="
                + pressed + ", disabled=" + disabled + ", horizontalPadding=" + horizontalPadding
                + "]";
    }
}
