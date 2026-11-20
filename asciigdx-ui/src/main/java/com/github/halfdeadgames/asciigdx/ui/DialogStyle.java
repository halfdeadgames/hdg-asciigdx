package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link Dialog}: the style of its window, the look of its message text, the style of
 * its buttons, the style of the text field of a text input dialog, and an optional dim that
 * darkens everything under the dialog while it is shown. Immutable, so one instance can be shared
 * by any number of dialogs. Colors are packed RGBA8888 ints, as in {@code TerminalGrid}.
 *
 * <p>A style made by {@link #of} does not dim. Add a dim with {@link #withDim(int, float)}.
 *
 * <p>Example:
 * <pre>{@code
 * DialogStyle style = DialogStyle.of(windowStyle, messageLook, buttonStyle, fieldStyle)
 *         .withDim(0x000000FF, 0.5f);
 * }</pre>
 */
public final class DialogStyle {

    private static final int OPAQUE_ALPHA = 0xFF;

    private final WindowStyle window;
    private final LabelStyle message;
    private final ButtonStyle buttons;
    private final TextFieldStyle textField;
    private final int dimRgba;
    private final float dimAmount;

    private DialogStyle(WindowStyle window, LabelStyle message, ButtonStyle buttons,
            TextFieldStyle textField, int dimRgba, float dimAmount) {
        this.window = window;
        this.message = message;
        this.buttons = buttons;
        this.textField = textField;
        this.dimRgba = dimRgba;
        this.dimAmount = dimAmount;
    }

    /**
     * Creates a style that does not dim.
     *
     * @param window style of the dialog's window; must not be {@code null}
     * @param message look of the message text; must not be {@code null}
     * @param buttons style of the dialog's buttons; must not be {@code null}
     * @param textField style of the text field of a text input dialog; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if an argument is {@code null}
     */
    public static DialogStyle of(WindowStyle window, LabelStyle message, ButtonStyle buttons,
            TextFieldStyle textField) {
        return new DialogStyle(Objects.requireNonNull(window, "window"),
                Objects.requireNonNull(message, "message"),
                Objects.requireNonNull(buttons, "buttons"),
                Objects.requireNonNull(textField, "textField"), 0, 0f);
    }

    /**
     * Returns a copy that dims everything under a shown dialog. Each color channel of every cell
     * moves toward the dim color by the amount (see {@link Dialog#show(UiRoot)}). An amount of 0
     * gives a style that does not dim, whatever the color.
     *
     * @param dimRgba dim color as RGBA8888; its alpha is ignored
     * @param dimAmount how far the colors move toward the dim color, from 0 (not at all) to 1
     *         (fully replaced); not NaN
     * @return a style with the same other parts and this dim
     * @throws IllegalArgumentException if {@code dimAmount} is NaN or outside 0 to 1
     */
    public DialogStyle withDim(int dimRgba, float dimAmount) {
        if (Float.isNaN(dimAmount) || dimAmount < 0f || dimAmount > 1f) {
            throw new IllegalArgumentException("dimAmount must be 0 to 1, was " + dimAmount);
        }
        if (dimAmount == 0f) {
            return new DialogStyle(window, message, buttons, textField, 0, 0f);
        }
        return new DialogStyle(window, message, buttons, textField, dimRgba | OPAQUE_ALPHA,
                dimAmount);
    }

    /**
     * Returns the style of the dialog's window.
     *
     * @return the window style, never {@code null}
     */
    public WindowStyle getWindow() {
        return window;
    }

    /**
     * Returns the look of the message text.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getMessage() {
        return message;
    }

    /**
     * Returns the style of the dialog's buttons.
     *
     * @return the button style, never {@code null}
     */
    public ButtonStyle getButtons() {
        return buttons;
    }

    /**
     * Returns the style of the text field of a text input dialog.
     *
     * @return the text field style, never {@code null}
     */
    public TextFieldStyle getTextField() {
        return textField;
    }

    /**
     * Returns whether the style dims what is under a shown dialog.
     *
     * @return {@code true} after {@link #withDim(int, float)} with an amount above 0
     */
    public boolean hasDim() {
        return dimAmount > 0f;
    }

    /**
     * Returns the dim color.
     *
     * @return the color as RGBA8888, always with alpha 0xFF
     * @throws IllegalStateException if the style does not dim; check {@link #hasDim()} first
     */
    public int getDimRgba() {
        requireDim();
        return dimRgba;
    }

    /**
     * Returns how far the colors move toward the dim color.
     *
     * @return the amount, above 0 and at most 1
     * @throws IllegalStateException if the style does not dim; check {@link #hasDim()} first
     */
    public float getDimAmount() {
        requireDim();
        return dimAmount;
    }

    private void requireDim() {
        if (!hasDim()) {
            throw new IllegalStateException("The style does not dim");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DialogStyle)) {
            return false;
        }
        DialogStyle that = (DialogStyle) other;
        return window.equals(that.window) && message.equals(that.message)
                && buttons.equals(that.buttons) && textField.equals(that.textField)
                && dimRgba == that.dimRgba && dimAmount == that.dimAmount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(window, message, buttons, textField, dimRgba, dimAmount);
    }

    @Override
    public String toString() {
        return "DialogStyle[window=" + window + ", message=" + message + ", buttons=" + buttons
                + ", textField=" + textField + ", dim="
                + (hasDim() ? Integer.toHexString(dimRgba) + " x " + dimAmount : "none") + "]";
    }
}
