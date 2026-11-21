package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link TextButton} in its five states (normal, pointer over, pressed, disabled, and
 * focused), and the space left and right of its text. Immutable, so one instance can be shared by
 * any number of buttons.
 */
public final class ButtonStyle {

    /** Horizontal padding of a new style, in UI cells. */
    public static final int DEFAULT_HORIZONTAL_PADDING = 1;

    private final LabelStyle normal;
    private final LabelStyle pointerOver;
    private final LabelStyle pressed;
    private final LabelStyle disabled;
    private final LabelStyle focused;
    private final int horizontalPadding;

    private ButtonStyle(LabelStyle normal, LabelStyle pointerOver, LabelStyle pressed,
            LabelStyle disabled, LabelStyle focused, int horizontalPadding) {
        this.normal = normal;
        this.pointerOver = pointerOver;
        this.pressed = pressed;
        this.disabled = disabled;
        this.focused = focused;
        this.horizontalPadding = horizontalPadding;
    }

    /**
     * Creates a style with a look for each of four states and a horizontal padding of
     * {@link #DEFAULT_HORIZONTAL_PADDING}. The fifth look, for keyboard focus, starts as a copy of
     * the pointer-over look; change it with {@link #withFocused(LabelStyle)}.
     *
     * @param normal look when nothing special happens; must not be {@code null}
     * @param pointerOver look while the pointer is over the button; must not be {@code null}
     * @param pressed look while the button is pressed; must not be {@code null}
     * @param disabled look while the button is disabled; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if an argument is {@code null}
     */
    public static ButtonStyle of(LabelStyle normal, LabelStyle pointerOver, LabelStyle pressed,
            LabelStyle disabled) {
        Objects.requireNonNull(normal, "normal");
        Objects.requireNonNull(pointerOver, "pointerOver");
        Objects.requireNonNull(pressed, "pressed");
        Objects.requireNonNull(disabled, "disabled");
        return new ButtonStyle(normal, pointerOver, pressed, disabled, pointerOver,
                DEFAULT_HORIZONTAL_PADDING);
    }

    /**
     * Creates a style that looks the same in all five states (normal, pointer over, pressed,
     * disabled, and focused), with a horizontal padding of {@link #DEFAULT_HORIZONTAL_PADDING}.
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
        return new ButtonStyle(normal, pointerOver, pressed, disabled, focused, cells);
    }

    /**
     * Returns a copy with another look for a button that has visible keyboard focus. A style made
     * by {@code of} uses its pointer-over look as the focused look until this is called.
     *
     * @param focused look while the button has visible keyboard focus; must not be {@code null}
     * @return a style with the same other looks and padding, and that focused look
     * @throws NullPointerException if {@code focused} is {@code null}
     */
    public ButtonStyle withFocused(LabelStyle focused) {
        Objects.requireNonNull(focused, "focused");
        return new ButtonStyle(normal, pointerOver, pressed, disabled, focused,
                horizontalPadding);
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
     * Returns the look while the button has keyboard focus and focus is visible. It starts as the
     * pointer-over look and changes only through {@link #withFocused(LabelStyle)}.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getFocused() {
        return focused;
    }

    /**
     * Returns the space on each side of the text.
     *
     * @return the padding in UI cells, zero or greater
     */
    public int getHorizontalPadding() {
        return horizontalPadding;
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
                && disabled.equals(that.disabled) && focused.equals(that.focused);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normal, pointerOver, pressed, disabled, focused, horizontalPadding);
    }

    @Override
    public String toString() {
        return "ButtonStyle[normal=" + normal + ", pointerOver=" + pointerOver + ", pressed="
                + pressed + ", disabled=" + disabled + ", focused=" + focused
                + ", horizontalPadding=" + horizontalPadding + "]";
    }
}
