package com.github.halfdeadgames.asciigdx.text;

import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.IntArray;

/**
 * The open tags of one color channel while a text is parsed. Each entry is either a color or the
 * paint's own color ({@code default}). An empty stack means the paint's own color too.
 */
final class ColorStack {

    private final IntArray rgbaValues = new IntArray();
    private final BooleanArray isColorFlags = new BooleanArray();

    boolean isEmpty() {
        return rgbaValues.size == 0;
    }

    void pushColor(int rgba) {
        rgbaValues.add(rgba);
        isColorFlags.add(true);
    }

    void pushDefault() {
        rgbaValues.add(0);
        isColorFlags.add(false);
    }

    void pop() {
        rgbaValues.pop();
        isColorFlags.pop();
    }

    boolean hasColor() {
        return !isEmpty() && isColorFlags.peek();
    }

    int getRgba() {
        return rgbaValues.peek();
    }
}
