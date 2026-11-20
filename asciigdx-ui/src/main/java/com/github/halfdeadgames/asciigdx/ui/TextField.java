package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.text.TextDrawing;
import java.util.Arrays;
import java.util.Objects;

/**
 * A one-line text input. It fills its whole area with the background of its current look and
 * writes the text on its top row, one UI column per code point. When the text is longer than the
 * field, the field scrolls to keep the caret visible, and a cut end shows an ellipsis (U+2026) in
 * its outermost cell, for example {@code …ello wor…}. The caret never sits on an ellipsis cell.
 * A field narrower than 3 cells has no room for ellipses and shows none.
 *
 * <p>Keys, while the field has keyboard focus (they need a {@link UiRoot} to deliver input):
 * <ul>
 *   <li>Typed characters are inserted at the caret when they are not control characters, pass the
 *       {@linkplain #setFilter filter}, and the {@linkplain #setMaxLength max length} is not
 *       reached. A typed character that is refused is still consumed. Characters outside the
 *       Basic Multilingual Plane cannot be typed, because libGDX reports typed characters as
 *       single {@code char}s; {@link #setText(String)} accepts them.
 *   <li>Backspace and Delete remove the code point before or at the caret.
 *   <li>Left, Right, Home, and End move the caret. Left at the start and Right at the end are
 *       consumed and do nothing. Up and Down are consumed and do nothing, because the field has
 *       one line, so they never move focus.
 *   <li>Tab and Shift+Tab are not consumed, so focus navigation moves on.
 *   <li>Enter and numpad Enter call the {@linkplain #addEnterListener enter listeners}. If none
 *       handles the key, it is not consumed.
 *   <li>A primary-button press focuses the field (it is focusable unless disabled) and puts the
 *       caret at the pressed cell, or at the end of the text if that cell is past it.
 * </ul>
 * A disabled field ignores input and is not focusable.
 *
 * <p>Looks: the disabled look while disabled, otherwise the focused look while the field has
 * keyboard focus and the {@link UiRoot} shows focus (see {@link UiRoot#isFocusVisible()}),
 * otherwise the normal look. While the text is empty, the placeholder text is written in the
 * foreground of the style's placeholder look on the background of the current look. The caret is
 * drawn whenever the field has keyboard focus, whatever the focus look; see {@link CaretStyle}. It
 * blinks with time from {@link UiRoot#update(float)} and is on again after every edit or caret
 * move.
 *
 * <p>A {@link ChangeListener} is called only after an edit by the user (typing, Backspace,
 * Delete), never by {@link #setText(String)}.
 *
 * <p>Example:
 * <pre>{@code
 * TextField name = new TextField(skin.getTextFieldStyle("textField"), 20);
 * name.setPlaceholderText("Your name");
 * name.setMaxLength(16);
 * name.addEnterListener(field -> { submit(field.getText()); return true; });
 * ui.getRoot().addChild(name);
 * }</pre>
 */
public final class TextField extends Widget {

    private static final ChangeListener<?>[] NO_CHANGE_LISTENERS = new ChangeListener<?>[0];
    private static final EnterListener[] NO_ENTER_LISTENERS = new EnterListener[0];
    private static final int PRIMARY_BUTTON = 0;
    private static final char BACKSPACE_CHARACTER = '\b';
    private static final char DELETE_CHARACTER = 127;

    /** The max length value that means no limit. It is the default of a new field. */
    public static final int UNLIMITED_LENGTH = 0;

    private final TextFieldModel model = new TextFieldModel();
    private final int preferredWidth;
    private TextFieldStyle style;
    private TextFieldFilter filter = TextFieldFilter.any();
    private int[] placeholderCodePoints = new int[0];
    private ChangeListener<?>[] changeListeners = NO_CHANGE_LISTENERS;
    private EnterListener[] enterListeners = NO_ENTER_LISTENERS;
    private boolean disabled;
    private boolean caretOn = true;
    private float blinkElapsedSeconds;

    /**
     * Creates an empty field, sized to the given width and the height of the style's font.
     *
     * @param style looks and caret; must not be {@code null}
     * @param widthCells width in UI cells; 1 or more
     * @throws NullPointerException if {@code style} is {@code null}
     * @throws IllegalArgumentException if {@code widthCells} is less than 1
     */
    public TextField(TextFieldStyle style, int widthCells) {
        this.style = Objects.requireNonNull(style, "style");
        if (widthCells < 1) {
            throw new IllegalArgumentException("Width must be at least 1, was " + widthCells);
        }
        this.preferredWidth = widthCells;
        pack();
    }

    /**
     * Returns the text.
     *
     * @return the text, never {@code null}
     */
    public String getText() {
        return model.getText();
    }

    /**
     * Replaces the text from code. The caret goes to the end. The filter and the max length are
     * not applied, and listeners are not called. Requests a redraw.
     *
     * @param text new text; must not be {@code null}; may be empty
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public void setText(String text) {
        Objects.requireNonNull(text, "text");
        model.setText(text);
        restartBlink();
    }

    /**
     * Returns the caret position. The index counts code points, not {@code String} (UTF-16)
     * positions, so it differs from a {@code String} index when the text holds characters outside
     * the Basic Multilingual Plane. To get the {@code String} position, use
     * {@code text.offsetByCodePoints(0, index)}.
     *
     * @return the number of code points before the caret, from 0 to the text's length in code
     *         points
     */
    public int getCaretIndex() {
        return model.getCaret();
    }

    /**
     * Returns the text shown while the field is empty.
     *
     * @return the placeholder text, never {@code null}; empty for none
     */
    public String getPlaceholderText() {
        return new String(placeholderCodePoints, 0, placeholderCodePoints.length);
    }

    /**
     * Sets the text shown, with the placeholder look, while the field has no text. Requests a
     * redraw.
     *
     * @param placeholderText the text; must not be {@code null}; empty for none
     * @throws NullPointerException if {@code placeholderText} is {@code null}
     */
    public void setPlaceholderText(String placeholderText) {
        Objects.requireNonNull(placeholderText, "placeholderText");
        placeholderCodePoints = placeholderText.codePoints().toArray();
        requestRedraw();
    }

    /**
     * Returns the filter for typed characters.
     *
     * @return the filter, never {@code null}
     */
    public TextFieldFilter getFilter() {
        return filter;
    }

    /**
     * Sets the filter that decides which typed characters are inserted. The default accepts all
     * printable characters. Text already in the field is not checked.
     *
     * @param filter the new filter; must not be {@code null}
     * @throws NullPointerException if {@code filter} is {@code null}
     */
    public void setFilter(TextFieldFilter filter) {
        this.filter = Objects.requireNonNull(filter, "filter");
    }

    /**
     * Returns the limit on typed text.
     *
     * @return the most code points the user can reach by typing; {@link #UNLIMITED_LENGTH} for
     *         unlimited
     */
    public int getMaxLength() {
        return model.getMaxLength();
    }

    /**
     * Sets the most code points that typing can reach. Text already longer, or set with
     * {@link #setText(String)}, is kept as it is; typing then inserts nothing until it is shorter.
     *
     * @param maxLength limit in code points; {@link #UNLIMITED_LENGTH} (the default) for
     *         unlimited
     * @throws IllegalArgumentException if {@code maxLength} is negative
     */
    public void setMaxLength(int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length must not be negative, was " + maxLength);
        }
        model.setMaxLength(maxLength);
    }

    /**
     * Returns the looks and caret.
     *
     * @return the style, never {@code null}
     */
    public TextFieldStyle getStyle() {
        return style;
    }

    /**
     * Replaces the looks and caret. The width stays as it is. A different style is marked by
     * {@link #invalidate()} if the font changed, because it affects the height, otherwise by
     * {@link #requestRedraw()}. An equal style changes no marks.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(TextFieldStyle style) {
        Objects.requireNonNull(style, "style");
        if (style.equals(this.style)) {
            return;
        }
        boolean fontChanged = style.getFont() != this.style.getFont();
        this.style = style;
        if (fontChanged) {
            invalidate();
        }
        restartBlink();
    }

    /**
     * Enables or disables the field. A change requests a redraw; setting the same value changes
     * nothing. A disabled field ignores keys and pointer presses. Disabling the field that owns
     * keyboard focus clears the focus the next time the {@link UiRoot} reads it or runs
     * {@code update} or {@code draw}, as for a hidden widget; enabling it again does not restore
     * the focus.
     *
     * @param disabled {@code true} to disable the field
     */
    public void setDisabled(boolean disabled) {
        if (this.disabled == disabled) {
            return;
        }
        this.disabled = disabled;
        requestRedraw();
    }

    /**
     * Returns whether the field is disabled.
     *
     * @return {@code true} when disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Adds a listener that is called after every edit by the user, in the order the listeners
     * were added. Safe to call from inside a listener; the change applies from the next edit.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addChangeListener(ChangeListener<? super TextField> listener) {
        Objects.requireNonNull(listener, "listener");
        ChangeListener<?>[] grown = Arrays.copyOf(changeListeners, changeListeners.length + 1);
        grown[changeListeners.length] = listener;
        changeListeners = grown;
    }

    /**
     * Removes the first occurrence of a change listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeChangeListener(ChangeListener<? super TextField> listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(changeListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        ChangeListener<?>[] shrunk = new ChangeListener<?>[changeListeners.length - 1];
        System.arraycopy(changeListeners, 0, shrunk, 0, index);
        System.arraycopy(changeListeners, index + 1, shrunk, index, shrunk.length - index);
        changeListeners = shrunk.length == 0 ? NO_CHANGE_LISTENERS : shrunk;
    }

    /**
     * Adds a listener for Enter. Listeners run in the order they were added and stop at the first
     * that returns {@code true}. Safe to call from inside a listener; the change applies from the
     * next Enter.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addEnterListener(EnterListener listener) {
        Objects.requireNonNull(listener, "listener");
        EnterListener[] grown = Arrays.copyOf(enterListeners, enterListeners.length + 1);
        grown[enterListeners.length] = listener;
        enterListeners = grown;
    }

    /**
     * Removes the first occurrence of an Enter listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeEnterListener(EnterListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(enterListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        EnterListener[] shrunk = new EnterListener[enterListeners.length - 1];
        System.arraycopy(enterListeners, 0, shrunk, 0, index);
        System.arraycopy(enterListeners, index + 1, shrunk, index, shrunk.length - index);
        enterListeners = shrunk.length == 0 ? NO_ENTER_LISTENERS : shrunk;
    }

    /**
     * Returns whether the field can take keyboard focus, which is the case unless it is disabled.
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
     * Returns the width given to the constructor.
     *
     * @return the width in UI cells, 1 or more
     */
    @Override
    public int getPreferredWidth() {
        return preferredWidth;
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
        model.scrollToCaret(width);
        boolean showingPlaceholder = isShowingPlaceholder();
        int textRgba = showingPlaceholder ? style.getPlaceholder().getForegroundRgba()
                : look.getForegroundRgba();
        int visibleCells = Math.min(width, target.getColumns() - left);
        for (int cell = 0; cell < visibleCells; cell++) {
            int codePoint = codePointInCell(cell, width, showingPlaceholder);
            if (codePoint != TextFieldModel.NO_GLYPH) {
                target.writeGlyph(style.getFont(), left + cell, top, codePoint, textRgba);
            }
        }
        if (hasKeyboardFocus() && !disabled && caretOn) {
            drawCaret(target, left, top, width, height, textRgba, showingPlaceholder);
        }
    }

    private LabelStyle currentLook() {
        if (disabled) {
            return style.getDisabled();
        }
        return isFocusShown() ? style.getFocused() : style.getNormal();
    }

    private boolean isShowingPlaceholder() {
        return model.getLength() == 0 && placeholderCodePoints.length > 0;
    }

    private int codePointInCell(int cell, int width, boolean showingPlaceholder) {
        if (!showingPlaceholder) {
            return model.getCellCodePoint(cell, width);
        }
        boolean cutAtEnd = placeholderCodePoints.length > width
                && width >= TextFieldModel.MIN_WIDTH_FOR_ELLIPSIS;
        if (cutAtEnd && cell == width - 1) {
            return TextDrawing.DEFAULT_ELLIPSIS;
        }
        return cell < placeholderCodePoints.length ? placeholderCodePoints[cell]
                : TextFieldModel.NO_GLYPH;
    }

    private void drawCaret(UiSurface target, int left, int top, int width, int height,
            int textRgba, boolean showingPlaceholder) {
        int cell = model.getCaretCell();
        if (cell < 0 || cell >= width) {
            return;
        }
        CaretStyle caret = style.getCaret();
        int column = left + cell;
        if (caret.hasBackground()) {
            target.fillBackground(column, top, 1, height, caret.getBackgroundRgba());
        }
        int codePoint = codePointInCell(cell, width, showingPlaceholder);
        if (caret.hasForeground() && codePoint != TextFieldModel.NO_GLYPH) {
            target.writeGlyph(style.getFont(), column, top, codePoint, caret.getForegroundRgba());
        }
        if (caret.hasGlyph()) {
            int glyphRgba = caret.hasForeground() ? caret.getForegroundRgba() : textRgba;
            target.writeOverlayGlyph(column, top + height - 1, caret.getGlyphCodePoint(),
                    glyphRgba);
        }
    }

    @Override
    boolean handleEvent(UiEvent event) {
        if (disabled) {
            return false;
        }
        return switch (event.getType()) {
            case POINTER_DOWN -> handlePointerDown(event);
            case KEY_DOWN -> handleKeyDown(event.getKeycode());
            case KEY_TYPED -> handleKeyTyped(event.getCharacter());
            default -> false;
        };
    }

    @Override
    void onKeyboardFocusChanged() {
        restartBlink();
    }

    @Override
    void updateWhileFocused(float deltaSeconds) {
        float blinkSeconds = style.getCaret().getBlinkSeconds();
        if (blinkSeconds <= 0f) {
            return;
        }
        blinkElapsedSeconds = (blinkElapsedSeconds + deltaSeconds) % (2 * blinkSeconds);
        boolean shouldBeOn = blinkElapsedSeconds < blinkSeconds;
        if (shouldBeOn != caretOn) {
            caretOn = shouldBeOn;
            requestRedraw();
        }
    }

    private boolean handlePointerDown(UiEvent event) {
        if (event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        model.placeCaret(model.getWindowStart() + event.getColumn());
        restartBlink();
        return true;
    }

    // Deleting is done by KEY_TYPED, because that is the only event a held key repeats on desktop.
    // Their key-downs are consumed without action, so one press never deletes twice.
    private boolean handleKeyDown(int keycode) {
        if (moveCaretByKey(keycode)) {
            restartBlink();
            return true;
        }
        switch (keycode) {
            case Input.Keys.BACKSPACE, Input.Keys.FORWARD_DEL, Input.Keys.UP, Input.Keys.DOWN -> {
                return true;
            }
            case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER -> {
                return fireEnter();
            }
            default -> {
                return false;
            }
        }
    }

    private boolean moveCaretByKey(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT -> model.moveCaretLeft();
            case Input.Keys.RIGHT -> model.moveCaretRight();
            case Input.Keys.HOME -> model.moveCaretToStart();
            case Input.Keys.END -> model.moveCaretToEnd();
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean handleKeyTyped(char character) {
        if (character == BACKSPACE_CHARACTER) {
            deleteBeforeCaretByUser();
            return true;
        }
        if (character == DELETE_CHARACTER) {
            deleteAtCaretByUser();
            return true;
        }
        if (Character.isISOControl(character) || Character.isSurrogate(character)) {
            return false;
        }
        if (model.isFull() || !filter.accepts(this, character)) {
            return true;
        }
        model.insert(character);
        afterEditByUser();
        return true;
    }

    private void deleteBeforeCaretByUser() {
        if (model.getCaret() == 0) {
            return;
        }
        model.deleteBeforeCaret();
        afterEditByUser();
    }

    private void deleteAtCaretByUser() {
        if (model.getCaret() == model.getLength()) {
            return;
        }
        model.deleteAtCaret();
        afterEditByUser();
    }

    private void afterEditByUser() {
        restartBlink();
        ChangeListener<?>[] snapshot = changeListeners;
        for (int i = 0; i < snapshot.length; i++) {
            @SuppressWarnings("unchecked")
            ChangeListener<? super TextField> listener = (ChangeListener<? super TextField>)
                    snapshot[i];
            listener.changed(this);
        }
    }

    private boolean fireEnter() {
        EnterListener[] snapshot = enterListeners;
        for (int i = 0; i < snapshot.length; i++) {
            if (snapshot[i].enterPressed(this)) {
                return true;
            }
        }
        return false;
    }

    private void restartBlink() {
        blinkElapsedSeconds = 0f;
        caretOn = true;
        requestRedraw();
    }
}
