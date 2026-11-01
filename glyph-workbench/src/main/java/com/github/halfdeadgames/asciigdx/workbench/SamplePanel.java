package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.workbench.SampleWords.Sample;

/** Draws the glyph cloud and the sample words for one column's glyphs inside a frame. */
final class SamplePanel {

    static final int CLOUD_COLUMNS = 9;
    static final int CLOUD_ROWS = 5;
    private static final int BORDER = ColumnPanel.BORDER;
    private static final int SIZE = ColumnPanel.UNIT * 2;
    private static final int SECTION_GAP = 16;
    private static final int WORD_GAP = SECTION_GAP / 2;

    private final Painter painter;
    private final GlyphOrder order;
    private final Color fill;
    private final Color frameColor;

    SamplePanel(Painter painter, GlyphOrder order, Color fill, Color frameColor) {
        this.painter = painter;
        this.order = order;
        this.fill = fill;
        this.frameColor = frameColor;
    }

    /** Width of the frame, which is also the width of the cloud plus both borders. */
    static int outerWidth() {
        return Painter.roundUpToCells(CLOUD_COLUMNS * SIZE) + 2 * BORDER;
    }

    static int outerHeight(int wordCount) {
        int content = CLOUD_ROWS * SIZE;
        if (wordCount > 0) {
            content += SECTION_GAP + wordCount * SIZE + (wordCount - 1) * WORD_GAP;
        }
        return Painter.roundUpToCells(content) + 2 * BORDER;
    }

    /**
     * Only the centre cloud cell and the target letter of each word come from {@code target}; every
     * other cell comes from {@code base} (the default font), so the surroundings never change. All
     * cells are drawn at the same on-screen size. The centre cell uses {@code targetTint}, the word
     * target letters use {@code wordTargetTint}, the rest stays white. Returns the next free row
     * below the frame.
     */
    int draw(
        int x,
        int top,
        GlyphColumn target,
        GlyphColumn base,
        int[] cloud,
        Sample[] words,
        Color targetTint,
        Color wordTargetTint) {
        int outerHeight = outerHeight(words.length);
        painter.frame(frameColor, x, top, outerWidth() / BORDER, outerHeight / BORDER);
        int contentLeft = x + BORDER;
        int contentTop = top + BORDER;

        int centre = (CLOUD_ROWS / 2) * CLOUD_COLUMNS + CLOUD_COLUMNS / 2;
        painter.fill(fill, contentLeft, contentTop, CLOUD_COLUMNS * SIZE, CLOUD_ROWS * SIZE);
        for (int i = 0; i < cloud.length; i++) {
            boolean isTarget = i == centre;
            painter.region(
                (isTarget ? target : base).regions[cloud[i]],
                contentLeft + (i % CLOUD_COLUMNS) * SIZE,
                contentTop + (i / CLOUD_COLUMNS) * SIZE,
                SIZE,
                isTarget ? targetTint : Color.WHITE);
        }
        int wordTop = contentTop + CLOUD_ROWS * SIZE + SECTION_GAP;
        for (Sample word : words) {
            String text = word.text();
            painter.fill(fill, contentLeft, wordTop, text.length() * SIZE, SIZE);
            for (int i = 0; i < text.length(); i++) {
                int slot = order.slotOf(text.charAt(i));
                boolean isTarget = i == word.targetIndex();
                TextureRegion[] regions = (isTarget ? target : base).regions;
                if (slot >= 0 && slot < regions.length && regions[slot] != null) {
                    painter.region(regions[slot], contentLeft + i * SIZE, wordTop, SIZE,
                        isTarget ? wordTargetTint : Color.WHITE);
                }
            }
            wordTop += SIZE + WORD_GAP;
        }
        return top + outerHeight;
    }
}
