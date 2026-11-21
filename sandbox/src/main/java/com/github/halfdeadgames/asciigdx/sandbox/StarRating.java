package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.ui.ChangeListener;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.UiEvent;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/**
 * Example of a custom widget: five stars with a value from 0 to 5. Left and Right change the value
 * while focused, and a click on a star sets it. Each star takes one cell and is followed by a one
 * cell gap.
 */
final class StarRating extends Widget {
    static final int STAR_COUNT = 5;
    private static final int CELLS_PER_STAR = 2;
    private static final int WIDTH = STAR_COUNT * CELLS_PER_STAR - 1;
    private static final int PRIMARY_BUTTON = 0;

    private final StarRatingStyle style;
    private final List<ChangeListener<? super StarRating>> changeListeners = new ArrayList<>();
    private int value;

    StarRating(StarRatingStyle style) {
        this.style = style;
        setSize(WIDTH, getPreferredHeight());
    }

    int getValue() {
        return value;
    }

    void setValue(int newValue) {
        value = Math.max(0, Math.min(STAR_COUNT, newValue));
        requestRedraw();
    }

    void addChangeListener(ChangeListener<? super StarRating> listener) {
        changeListeners.add(listener);
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public int getPreferredWidth() {
        return WIDTH;
    }

    @Override
    public int getPreferredHeight() {
        return 1;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        LabelStyle look = isFocusShown() ? style.getFocused() : style.getNormal();
        target.eraseGlyphs(left, top, getWidth(), getHeight());
        target.fillBackground(left, top, getWidth(), getHeight(), look.getBackgroundRgba());
        for (int star = 0; star < STAR_COUNT; star++) {
            int glyph = star < value ? style.getFilledGlyph() : style.getEmptyGlyph();
            target.writeSquareGlyph(left + star * CELLS_PER_STAR, top, glyph,
                    look.getForegroundRgba());
        }
    }

    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_DOWN -> handlePointerDown(event);
            case KEY_DOWN -> handleKeyDown(event.getKeycode());
            default -> false;
        };
    }

    private boolean handlePointerDown(UiEvent event) {
        if (event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        int clickedStar = Math.min(STAR_COUNT - 1, event.getColumn() / CELLS_PER_STAR);
        changeByUser(clickedStar + 1);
        return true;
    }

    private boolean handleKeyDown(int keycode) {
        if (keycode == Input.Keys.RIGHT) {
            changeByUser(value + 1);
            return true;
        }
        if (keycode == Input.Keys.LEFT) {
            changeByUser(value - 1);
            return true;
        }
        return false;
    }

    private void changeByUser(int newValue) {
        int clamped = Math.max(0, Math.min(STAR_COUNT, newValue));
        if (clamped == value) {
            return;
        }
        setValue(clamped);
        for (int i = 0; i < changeListeners.size(); i++) {
            changeListeners.get(i).changed(this);
        }
    }
}
