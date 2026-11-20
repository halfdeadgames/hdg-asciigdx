package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import java.util.Arrays;
import java.util.Objects;

/**
 * A button with one line of text. It fills its whole area with the background of its current
 * state and writes the text on its top row, starting after the style's horizontal padding. The
 * text takes one UI column per code point and is cut off at the button's width.
 *
 * <p>State look, in priority order: disabled, pressed, pointer over, focused, normal. The focused
 * look shows only while the button has keyboard focus and the {@link UiRoot} shows focus (see
 * {@link UiRoot#isFocusVisible()}). Every state change requests a redraw.
 *
 * <p>Clicks, which need a {@link UiRoot} to deliver input:
 * <ul>
 *   <li>Pressing the primary button (button 0) inside the button presses it. The pointer is
 *       captured, so the button follows it even outside.
 *   <li>Dragging outside removes the pressed look but keeps the capture. Dragging back inside
 *       presses it again.
 *   <li>Releasing inside while pressed is a click. Releasing outside, or a cancel, is not.
 *   <li>Enter, numpad Enter, or Space while the button has keyboard focus is a click.
 *   <li>A disabled button ignores input, never clicks, and is not focusable.
 * </ul>
 * Other buttons than the primary one are ignored, and so are pointers other than the one that
 * pressed the button.
 *
 * <p>A new button is packed to its preferred size. Changing the text keeps the current size, so
 * call {@link #pack()} afterward to fit the new text.
 *
 * <p>Example:
 * <pre>{@code
 * LabelStyle look = LabelStyle.of(UiFont.TALL, 0xFFFFFFFF, 0x303030FF);
 * LabelStyle hover = LabelStyle.of(UiFont.TALL, 0xFFFFFFFF, 0x505050FF);
 * LabelStyle down = LabelStyle.of(UiFont.TALL, 0x000000FF, 0xFFD700FF);
 * LabelStyle off = LabelStyle.of(UiFont.TALL, 0x808080FF, 0x202020FF);
 * TextButton start = new TextButton("Start", ButtonStyle.of(look, hover, down, off));
 * start.addClickListener(button -> startGame());
 * ui.getRoot().addChild(start);
 * }</pre>
 */
public class TextButton extends Widget {

    private static final ClickListener[] NO_CLICK_LISTENERS = new ClickListener[0];
    private static final int PRIMARY_BUTTON = 0;
    private static final int NO_POINTER = -1;

    private String text;
    private ButtonStyle style;
    private ClickListener[] clickListeners = NO_CLICK_LISTENERS;
    private boolean disabled;
    private boolean pointerOver;
    private boolean pressed;
    private int armedPointer = NO_POINTER;

    /**
     * Creates a button sized to fit its text.
     *
     * @param text text to show; must not be {@code null}; may be empty
     * @param style looks and padding; must not be {@code null}
     * @throws NullPointerException if {@code text} or {@code style} is {@code null}
     */
    public TextButton(String text, ButtonStyle style) {
        this.text = Objects.requireNonNull(text, "text");
        this.style = Objects.requireNonNull(style, "style");
        pack();
    }

    /**
     * Returns the text.
     *
     * @return the text, never {@code null}
     */
    public String getText() {
        return text;
    }

    /**
     * Replaces the text. The size stays as it is; call {@link #pack()} to fit the new text. When
     * the text differs from the current one, calls {@link #invalidate()}; equal text changes no
     * marks.
     *
     * @param text new text; must not be {@code null}; may be empty
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public void setText(String text) {
        Objects.requireNonNull(text, "text");
        if (text.equals(this.text)) {
            return;
        }
        this.text = text;
        invalidate();
    }

    /**
     * Returns the looks and padding.
     *
     * @return the style, never {@code null}
     */
    public ButtonStyle getStyle() {
        return style;
    }

    /**
     * Replaces the looks and padding. The size stays as it is, so call {@link #pack()} after a
     * change of font or padding. When the style differs from the current one, the button is
     * marked: by {@link #invalidate()} if the font or the padding changed, because they affect the
     * preferred size and row snap, otherwise by {@link #requestRedraw()}. An equal style changes
     * no marks.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(ButtonStyle style) {
        Objects.requireNonNull(style, "style");
        if (style.equals(this.style)) {
            return;
        }
        boolean sizeAffected = style.getFont() != this.style.getFont()
                || style.getHorizontalPadding() != this.style.getHorizontalPadding();
        this.style = style;
        if (sizeAffected) {
            invalidate();
        } else {
            requestRedraw();
        }
    }

    /**
     * Enables or disables the button. Disabling ends a press in progress without a click. A change
     * requests a redraw; setting the same value changes nothing. A disabled button ignores keys.
     * Disabling the button that owns keyboard focus clears the focus the next time the
     * {@link UiRoot} reads it or runs {@code update} or {@code draw}, as for a hidden widget;
     * enabling it again does not restore the focus.
     *
     * @param disabled {@code true} to disable the button
     */
    public void setDisabled(boolean disabled) {
        if (this.disabled == disabled) {
            return;
        }
        this.disabled = disabled;
        if (disabled) {
            armedPointer = NO_POINTER;
            pressed = false;
        }
        requestRedraw();
    }

    /**
     * Returns whether the button is disabled.
     *
     * @return {@code true} when disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Returns whether the button currently shows its pressed look: it was pressed and the pointer
     * is still inside it.
     *
     * @return {@code true} while pressed
     */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Returns whether the hovering pointer is over this button, as reported by the
     * {@link UiRoot}.
     *
     * @return {@code true} while the pointer is over the button
     */
    public boolean isPointerOver() {
        return pointerOver;
    }

    /**
     * Adds a listener that is called on every click, in the order the listeners were added. Safe
     * to call from inside a click listener; the change applies from the next click.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addClickListener(ClickListener listener) {
        Objects.requireNonNull(listener, "listener");
        ClickListener[] grown = Arrays.copyOf(clickListeners, clickListeners.length + 1);
        grown[clickListeners.length] = listener;
        clickListeners = grown;
    }

    /**
     * Removes the first occurrence of a click listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeClickListener(ClickListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(clickListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        ClickListener[] shrunk = new ClickListener[clickListeners.length - 1];
        System.arraycopy(clickListeners, 0, shrunk, 0, index);
        System.arraycopy(clickListeners, index + 1, shrunk, index, shrunk.length - index);
        clickListeners = shrunk.length == 0 ? NO_CLICK_LISTENERS : shrunk;
    }

    /**
     * Returns whether the button can take keyboard focus, which is the case unless it is disabled.
     *
     * @return {@code true} when enabled
     */
    @Override
    public boolean isFocusable() {
        return !disabled;
    }

    /**
     * Returns 2 when the style's font is {@link UiFont#TALL}, otherwise 1.
     *
     * @return the row multiple, 1 or 2
     */
    @Override
    public int getRowSnap() {
        return style.getFont() == UiFont.TALL ? 2 : 1;
    }

    /**
     * Returns the number of code points in the text plus the padding on both sides.
     *
     * @return the width in UI cells, zero or greater
     */
    @Override
    public int getPreferredWidth() {
        return text.codePointCount(0, text.length()) + 2 * style.getHorizontalPadding();
    }

    /**
     * Returns 2 for the tall font and 1 for the square font.
     *
     * @return the height in UI cells, 1 or 2
     */
    @Override
    public int getPreferredHeight() {
        return style.getFont() == UiFont.TALL ? 2 : 1;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        LabelStyle look = currentLook();
        target.eraseGlyphs(left, top, width, height);
        target.fillBackground(left, top, width, height, look.getBackgroundRgba());
        writeClippedText(target, look, left, top, width);
    }

    private LabelStyle currentLook() {
        if (disabled) {
            return style.getDisabled();
        }
        if (pressed) {
            return style.getPressed();
        }
        if (pointerOver) {
            return style.getPointerOver();
        }
        return isFocusShown() ? style.getFocused() : style.getNormal();
    }

    private void writeClippedText(UiSurface target, LabelStyle look, int left, int top,
            int width) {
        int length = text.length();
        int start = style.getHorizontalPadding();
        int index = 0;
        int cellOffset = start;
        while (index < length && cellOffset < width && left + cellOffset < target.getColumns()) {
            int codePoint = text.codePointAt(index);
            target.writeGlyph(look.getFont(), left + cellOffset, top, codePoint,
                    look.getForegroundRgba());
            index += Character.charCount(codePoint);
            cellOffset++;
        }
    }

    @Override
    boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_ENTERED -> {
                setPointerOver(true);
                yield false;
            }
            case POINTER_EXITED -> {
                setPointerOver(false);
                yield false;
            }
            case POINTER_DOWN -> handlePointerDown(event);
            case POINTER_DRAGGED -> handlePointerDragged(event);
            case POINTER_UP -> handlePointerUp(event);
            case POINTER_CANCELLED -> handlePointerCancelled(event);
            case KEY_DOWN -> handleKeyDown(event);
            default -> false;
        };
    }

    private boolean handlePointerDown(UiEvent event) {
        if (disabled || event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        armedPointer = event.getPointer();
        setPressed(true);
        return true;
    }

    private boolean handlePointerDragged(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        setPressed(isInsideBounds(event.getColumn(), event.getRow()));
        return true;
    }

    private boolean handlePointerUp(UiEvent event) {
        if (event.getPointer() != armedPointer || event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        boolean inside = isInsideBounds(event.getColumn(), event.getRow());
        armedPointer = NO_POINTER;
        setPressed(false);
        if (inside) {
            fireClick();
        }
        return true;
    }

    private boolean handlePointerCancelled(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        armedPointer = NO_POINTER;
        setPressed(false);
        return true;
    }

    private boolean handleKeyDown(UiEvent event) {
        int keycode = event.getKeycode();
        boolean clickKey = keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER
                || keycode == Input.Keys.SPACE;
        if (disabled || !clickKey) {
            return false;
        }
        fireClick();
        return true;
    }

    private void setPointerOver(boolean pointerOver) {
        if (this.pointerOver == pointerOver) {
            return;
        }
        this.pointerOver = pointerOver;
        requestRedraw();
    }

    private void setPressed(boolean pressed) {
        if (this.pressed == pressed) {
            return;
        }
        this.pressed = pressed;
        requestRedraw();
    }

    private void fireClick() {
        ClickListener[] snapshot = clickListeners;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].clicked(this);
        }
    }
}
