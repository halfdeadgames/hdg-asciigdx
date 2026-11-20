package com.github.halfdeadgames.asciigdx.ui;

import com.github.halfdeadgames.asciigdx.text.TextDrawing;
import java.util.Arrays;

/**
 * The editing state of a {@link TextField}: the text as code points, the caret between code
 * points, and the visible window onto the text. Plain Java, no drawing and no input.
 *
 * <p>The caret index runs from 0 (before the first code point) to the length (after the last).
 * The window has a width in cells. When the whole text plus the caret slot at its end fits, the
 * window starts at 0. Otherwise it scrolls just far enough to keep the caret visible, and a cut
 * end shows {@link TextDrawing#DEFAULT_ELLIPSIS} in its outermost cell. The caret never sits on an
 * ellipsis cell. A window narrower than {@value #MIN_WIDTH_FOR_ELLIPSIS} cells has no room for
 * ellipses and shows none.
 */
final class TextFieldModel {

    static final int NO_GLYPH = -1;
    static final int MIN_WIDTH_FOR_ELLIPSIS = 3;
    static final int UNLIMITED = TextField.UNLIMITED_LENGTH;

    private int[] codePoints = new int[16];
    private int length;
    private int caret;
    private int windowStart;
    private int maxLength = UNLIMITED;

    String getText() {
        return new String(codePoints, 0, length);
    }

    int getLength() {
        return length;
    }

    int getCaret() {
        return caret;
    }

    int getWindowStart() {
        return windowStart;
    }

    int getMaxLength() {
        return maxLength;
    }

    boolean isFull() {
        return maxLength != UNLIMITED && length >= maxLength;
    }

    void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    /** Replaces the text and puts the caret at its end. Max length is not applied. */
    void setText(String text) {
        codePoints = text.codePoints().toArray();
        length = codePoints.length;
        caret = length;
        windowStart = 0;
    }

    void insert(int codePoint) {
        if (length == codePoints.length) {
            codePoints = Arrays.copyOf(codePoints, Math.max(16, length * 2));
        }
        System.arraycopy(codePoints, caret, codePoints, caret + 1, length - caret);
        codePoints[caret] = codePoint;
        length++;
        caret++;
    }

    void deleteBeforeCaret() {
        if (caret == 0) {
            return;
        }
        removeAt(caret - 1);
        caret--;
    }

    void deleteAtCaret() {
        if (caret == length) {
            return;
        }
        removeAt(caret);
    }

    private void removeAt(int index) {
        System.arraycopy(codePoints, index + 1, codePoints, index, length - index - 1);
        length--;
    }

    void moveCaretLeft() {
        caret = Math.max(0, caret - 1);
    }

    void moveCaretRight() {
        caret = Math.min(length, caret + 1);
    }

    void moveCaretToStart() {
        caret = 0;
    }

    void moveCaretToEnd() {
        caret = length;
    }

    /** Puts the caret at an index, clamped to 0 through the length. */
    void placeCaret(int index) {
        caret = Math.max(0, Math.min(length, index));
    }

    /** Scrolls the window, if needed, so the caret is visible in a window of this width. */
    void scrollToCaret(int width) {
        if (width <= 0) {
            return;
        }
        windowStart = Math.max(0, Math.min(windowStart, length + 1 - width));
        while (caret < windowStart + leftMargin(width)) {
            windowStart--;
        }
        while (caret > windowStart + width - 1 - rightMargin(width)) {
            windowStart++;
        }
    }

    private int leftMargin(int width) {
        return width >= MIN_WIDTH_FOR_ELLIPSIS && windowStart > 0 ? 1 : 0;
    }

    private int rightMargin(int width) {
        return isRightCut(width) ? 1 : 0;
    }

    private boolean isRightCut(int width) {
        return width >= MIN_WIDTH_FOR_ELLIPSIS && windowStart + width < length;
    }

    /**
     * Returns what a window cell shows, for the window as last scrolled.
     *
     * @return a code point, the ellipsis for a cut end, or {@link #NO_GLYPH} for an empty cell
     */
    int getCellCodePoint(int cell, int width) {
        if (cell == 0 && leftMargin(width) == 1 || cell == width - 1 && isRightCut(width)) {
            return TextDrawing.DEFAULT_ELLIPSIS;
        }
        int index = windowStart + cell;
        return index < length ? codePoints[index] : NO_GLYPH;
    }

    /** Returns the caret's cell in the window, as last scrolled. */
    int getCaretCell() {
        return caret - windowStart;
    }
}
