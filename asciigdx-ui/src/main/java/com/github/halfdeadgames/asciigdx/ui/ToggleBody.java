package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import java.util.Arrays;
import java.util.Objects;

/**
 * The state, input handling, and drawing that {@link CheckBox} and {@link RadioButton} share. Each
 * of them owns one body and forwards to it. A user activation (click, Enter, or Space) is handed
 * to the owner through the activation callback, so the owner decides what it does.
 */
final class ToggleBody<T extends Widget> {

    private static final ChangeListener<?>[] NO_LISTENERS = new ChangeListener<?>[0];
    private static final int PRIMARY_BUTTON = 0;
    private static final int NO_POINTER = -1;

    private final T owner;
    private final Runnable onActivation;
    private String text;
    private ToggleStyle style;
    // Every element accepts T, because only addChangeListener(ChangeListener<? super T>) adds.
    private ChangeListener<?>[] changeListeners = NO_LISTENERS;
    private boolean checked;
    private boolean disabled;
    private boolean pointerOver;
    private boolean pressed;
    private int armedPointer = NO_POINTER;

    ToggleBody(T owner, String text, ToggleStyle style, Runnable onActivation) {
        this.owner = owner;
        this.text = Objects.requireNonNull(text, "text");
        this.style = Objects.requireNonNull(style, "style");
        this.onActivation = onActivation;
    }

    String getText() {
        return text;
    }

    void setText(String newText) {
        Objects.requireNonNull(newText, "text");
        if (newText.equals(text)) {
            return;
        }
        text = newText;
        owner.invalidate();
    }

    ToggleStyle getStyle() {
        return style;
    }

    void setStyle(ToggleStyle newStyle) {
        Objects.requireNonNull(newStyle, "style");
        if (newStyle.equals(style)) {
            return;
        }
        boolean sizeAffected = isSizeAffected(newStyle);
        style = newStyle;
        if (sizeAffected) {
            owner.invalidate();
        } else {
            owner.requestRedraw();
        }
    }

    private boolean isSizeAffected(ToggleStyle newStyle) {
        return newStyle.getButtonStyle().getHorizontalPadding()
                        != style.getButtonStyle().getHorizontalPadding()
                || newStyle.getMarkLength() != style.getMarkLength()
                || newStyle.getGap() != style.getGap();
    }

    boolean isChecked() {
        return checked;
    }

    void setChecked(boolean newChecked) {
        if (checked == newChecked) {
            return;
        }
        checked = newChecked;
        owner.requestRedraw();
    }

    boolean isDisabled() {
        return disabled;
    }

    void setDisabled(boolean newDisabled) {
        if (disabled == newDisabled) {
            return;
        }
        disabled = newDisabled;
        if (disabled) {
            armedPointer = NO_POINTER;
            pressed = false;
        }
        owner.requestRedraw();
    }

    boolean isPressed() {
        return pressed;
    }

    boolean isPointerOver() {
        return pointerOver;
    }

    void addChangeListener(ChangeListener<? super T> listener) {
        Objects.requireNonNull(listener, "listener");
        ChangeListener<?>[] grown = Arrays.copyOf(changeListeners, changeListeners.length + 1);
        grown[changeListeners.length] = listener;
        changeListeners = grown;
    }

    void removeChangeListener(ChangeListener<? super T> listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(changeListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        ChangeListener<?>[] shrunk = new ChangeListener<?>[changeListeners.length - 1];
        System.arraycopy(changeListeners, 0, shrunk, 0, index);
        System.arraycopy(changeListeners, index + 1, shrunk, index, shrunk.length - index);
        changeListeners = shrunk.length == 0 ? NO_LISTENERS : shrunk;
    }

    @SuppressWarnings("unchecked")
    void fireChanged() {
        ChangeListener<?>[] snapshot = changeListeners;
        for (int i = 0; i < snapshot.length; i++) {
            ((ChangeListener<? super T>) snapshot[i]).changed(owner);
        }
    }

    int getPreferredWidth() {
        int textColumns = text.codePointCount(0, text.length());
        int columns = style.getMarkLength() + style.getGap() + textColumns;
        int contentCells = (columns + UiSurface.TEXT_COLUMNS_PER_CELL - 1)
                / UiSurface.TEXT_COLUMNS_PER_CELL;
        return 2 * style.getButtonStyle().getHorizontalPadding() + contentCells;
    }

    int getPreferredHeight() {
        return 1;
    }

    void draw(UiSurface target, int left, int top) {
        int width = owner.getWidth();
        int height = owner.getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        LabelStyle look = currentLook();
        target.eraseGlyphs(left, top, width, height);
        target.fillBackground(left, top, width, height, look.getBackgroundRgba());
        String mark = checked ? style.getCheckedMark() : style.getUncheckedMark();
        int leftTextColumn = UiSurface.TEXT_COLUMNS_PER_CELL * left;
        int textColumnLimit = UiSurface.TEXT_COLUMNS_PER_CELL * width;
        int markStart = UiSurface.TEXT_COLUMNS_PER_CELL
                * style.getButtonStyle().getHorizontalPadding();
        int markEnd = writeClippedTextRun(target, look, mark, leftTextColumn, top,
                textColumnLimit, markStart);
        writeClippedTextRun(target, look, text, leftTextColumn, top, textColumnLimit,
                markEnd + style.getGap());
    }

    private LabelStyle currentLook() {
        ButtonStyle looks = style.getButtonStyle();
        if (disabled) {
            return looks.getDisabled();
        }
        if (pressed) {
            return looks.getPressed();
        }
        if (pointerOver) {
            return looks.getPointerOver();
        }
        return owner.isFocusShown() ? looks.getFocused() : looks.getNormal();
    }

    // EXPERIMENT: mark and words are text-font runs, one text column per code point. Returns the
    // first text column after the run, counted even for clipped columns.
    private static int writeClippedTextRun(UiSurface target, LabelStyle look, String run,
            int leftTextColumn, int top, int textColumnLimit, int startColumn) {
        int index = 0;
        int column = startColumn;
        while (index < run.length()) {
            int codePoint = run.codePointAt(index);
            if (column < textColumnLimit && leftTextColumn + column < target.getTextColumns()) {
                target.writeTextGlyph(leftTextColumn + column, top, codePoint,
                        look.getForegroundRgba());
            }
            index += Character.charCount(codePoint);
            column++;
        }
        return column;
    }

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
        setPressed(owner.isInsideBounds(event.getColumn(), event.getRow()));
        return true;
    }

    private boolean handlePointerUp(UiEvent event) {
        if (event.getPointer() != armedPointer || event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        boolean inside = owner.isInsideBounds(event.getColumn(), event.getRow());
        armedPointer = NO_POINTER;
        setPressed(false);
        if (inside) {
            onActivation.run();
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
        boolean activationKey = keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER
                || keycode == Input.Keys.SPACE;
        if (disabled || !activationKey) {
            return false;
        }
        onActivation.run();
        return true;
    }

    private void setPointerOver(boolean newPointerOver) {
        if (pointerOver == newPointerOver) {
            return;
        }
        pointerOver = newPointerOver;
        owner.requestRedraw();
    }

    private void setPressed(boolean newPressed) {
        if (pressed == newPressed) {
            return;
        }
        pressed = newPressed;
        owner.requestRedraw();
    }
}
