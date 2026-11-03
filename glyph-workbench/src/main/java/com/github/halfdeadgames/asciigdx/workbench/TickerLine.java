package com.github.halfdeadgames.asciigdx.workbench;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One news-ticker line scrolling right to left. Words are queued with one blank cell after each; new words
 * are requested whenever the queue ends before the visible width, and words that have fully left on the left
 * are dropped. Positions are in pixels from the left edge of the line.
 */
final class TickerLine {

    /** Receives each visible glyph with the pixel position of its left edge. */
    interface GlyphVisitor {
        void visit(int codePoint, float x);
    }

    private final Supplier<int[]> wordSource;
    private final int glyphAdvance;
    private final float pixelsPerSecond;
    private final List<int[]> words = new ArrayList<>();
    private float firstWordX;
    private int visibleWidth;

    /**
     * @param wordSource      supplies non-empty words
     * @param glyphAdvance    pixels from one glyph to the next
     * @param pixelsPerSecond scroll speed to the left
     */
    TickerLine(Supplier<int[]> wordSource, int glyphAdvance, float pixelsPerSecond) {
        this.wordSource = wordSource;
        this.glyphAdvance = glyphAdvance;
        this.pixelsPerSecond = pixelsPerSecond;
    }

    /** Sets how wide the visible area is and fills the queue to cover it. */
    void setVisibleWidth(int width) {
        if (width == visibleWidth) {
            return;
        }
        visibleWidth = width;
        refill();
    }

    void advance(float seconds) {
        firstWordX -= pixelsPerSecond * seconds;
        refill();
        while (!words.isEmpty() && firstWordX + slotWidth(words.get(0)) <= 0) {
            firstWordX += slotWidth(words.remove(0));
        }
        refill();
    }

    void forEachVisibleGlyph(GlyphVisitor visitor) {
        float wordX = firstWordX;
        for (int i = 0; i < words.size(); i++) {
            int[] word = words.get(i);
            for (int letter = 0; letter < word.length; letter++) {
                float glyphX = wordX + letter * glyphAdvance;
                if (glyphX + glyphAdvance > 0 && glyphX < visibleWidth) {
                    visitor.visit(word[letter], glyphX);
                }
            }
            wordX += slotWidth(word);
        }
    }

    /** Where the queued text ends, including the blank cell after the last word. */
    float contentEnd() {
        float end = firstWordX;
        for (int i = 0; i < words.size(); i++) {
            end += slotWidth(words.get(i));
        }
        return end;
    }

    float firstWordX() {
        return firstWordX;
    }

    int queuedWordCount() {
        return words.size();
    }

    private void refill() {
        if (visibleWidth <= 0) {
            return;
        }
        float end = contentEnd();
        while (end < visibleWidth) {
            int[] word = wordSource.get();
            words.add(word);
            end += slotWidth(word);
        }
    }

    private int slotWidth(int[] word) {
        return (word.length + 1) * glyphAdvance;
    }
}
