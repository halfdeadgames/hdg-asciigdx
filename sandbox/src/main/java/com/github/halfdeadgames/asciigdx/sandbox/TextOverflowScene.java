package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.text.BreakMode;
import com.github.halfdeadgames.asciigdx.text.GradientSpan;
import com.github.halfdeadgames.asciigdx.text.Hyphenator;
import com.github.halfdeadgames.asciigdx.text.TextDrawing;
import com.github.halfdeadgames.asciigdx.text.TextPaint;
import com.github.halfdeadgames.asciigdx.text.TextScroll;
import com.github.halfdeadgames.asciigdx.text.WrapLayout;
import com.github.halfdeadgames.asciigdx.text.WrapRules;

/**
 * Checks text that is longer than its area: truncation with an ellipsis, scrolling, and wrapping
 * with a hanging indent, a line limit, and word versus character breaks. Zones have a faint
 * background so their edges are visible.
 */
class TextOverflowScene extends GridScene {
    private static final int COLUMNS = 58;
    private static final int ROWS = 35;

    private static final int GRAY = 0x909090FF;
    private static final int ZONE_BACKGROUND = 0x1E2A3AFF;

    // The perf overlay is 32 px tall: 4 rows at scale 1 with 8x8 cells, fewer at larger scales.
    private static final int PERF_OVERLAY_MARGIN_ROWS = 4;
    private static final int FIRST_ROW = PERF_OVERLAY_MARGIN_ROWS;
    private static final int PANEL_HEIGHT = 5;

    private static final int LEFT_MARKER_COLUMN = 1;
    private static final int LEFT_ZONE_COLUMN = 2;
    private static final int LEFT_NOTE_COLUMN = 23;
    private static final int LEFT_ZONE_WIDTH = 20;

    private static final int RIGHT_LABEL_COLUMN = 31;
    private static final int RIGHT_ZONE_START = 32;
    private static final int RIGHT_ZONE_END = 55;
    private static final int HANGING_INDENT = 2;
    private static final int NARROW_ZONE_WIDTH = 12;
    private static final int NARROW_GAP = 1;

    private static final int TRUNCATION_LABEL_ROW = FIRST_ROW;
    private static final int SCROLL_LABEL_ROW = FIRST_ROW + 8;
    private static final int BACK_AND_FORTH_ROW = SCROLL_LABEL_ROW + 1;
    private static final int LOOP_ROW = SCROLL_LABEL_ROW + 2;

    private static final int WORD_PANEL_LABEL_ROW = FIRST_ROW;
    private static final int HANGING_PANEL_LABEL_ROW = WORD_PANEL_LABEL_ROW + PANEL_HEIGHT + 1;
    private static final int LINE_LIMIT_PANEL_LABEL_ROW =
            HANGING_PANEL_LABEL_ROW + PANEL_HEIGHT + 1;
    private static final int MODES_PANEL_LABEL_ROW = LINE_LIMIT_PANEL_LABEL_ROW + 5;
    private static final int MODES_PANEL_HEIGHT = 6;
    private static final int HYPHENATE_PANEL_LABEL_ROW =
            MODES_PANEL_LABEL_ROW + MODES_PANEL_HEIGHT + 2;

    private static final int PAINT_LABEL_ROW = SCROLL_LABEL_ROW + 4;
    private static final int KEEP_BACKGROUND_ROW = PAINT_LABEL_ROW + 1;
    private static final int TYPEWRITER_ROW = KEEP_BACKGROUND_ROW + 1;
    private static final int WHOLE_GRADIENT_LABEL_ROW = TYPEWRITER_ROW + 1;
    private static final int EACH_LINE_GRADIENT_LABEL_ROW = WHOLE_GRADIENT_LABEL_ROW + 6;

    private static final int STRIPE_BACKGROUND = 0x7A2E4AFF;
    private static final int GRADIENT_START = 0xFFD34FFF;
    private static final int GRADIENT_END = 0x4FD3FFFF;
    private static final float TYPEWRITER_LETTERS_PER_SECOND = 8f;
    private static final float TYPEWRITER_PAUSE_SECONDS = 1.5f;
    private static final String TYPEWRITER_TEXT = "The door creaks open";
    private static final String KEEP_BACKGROUND_TEXT = "Kept background";

    private static final int LINE_LIMIT = 3;
    private static final int CUSTOM_ELLIPSIS = '>';
    private static final int CUSTOM_ELLIPSIS_WIDTH = 10;
    private static final float SCROLL_CELLS_PER_SECOND = 4f;
    private static final float SCROLL_PAUSE_SECONDS = 1f;
    private static final int LOOP_GAP_CELLS = 3;

    private static final String LONG_SENTENCE =
            "The quick brown fox jumps over the lazy dog near the river bank";
    private static final String SHORT_TEXT = "Short text";
    private static final String WRAP_SAMPLE =
            "The lantern flickered twice, then a cold wind swept through the hall.";
    private static final String LONG_PARAGRAPH =
            "Explorers found an ancient staircase beneath the broken tower and followed it down "
                    + "into the dark, where something old was waiting for them.";
    private static final String LONG_WORD_SAMPLE =
            "Extraordinarily long words like internationalization wrap oddly";
    private static final String HYPHENATION_SAMPLE = "Extraordinary hyphenation, well-known.";

    private static final TextScroll BACK_AND_FORTH_SCROLL =
            TextScroll.backAndForth(SCROLL_CELLS_PER_SECOND, SCROLL_PAUSE_SECONDS);
    private static final TextScroll LOOP_SCROLL =
            TextScroll.loop(SCROLL_CELLS_PER_SECOND, SCROLL_PAUSE_SECONDS, LOOP_GAP_CELLS);

    private final WrapLayout wordLayout = new WrapLayout();
    private final WrapLayout hangingLayout = new WrapLayout();
    private final WrapLayout lineLimitLayout = new WrapLayout();
    private final WrapLayout narrowWordLayout = new WrapLayout();
    private final WrapLayout narrowCharacterLayout = new WrapLayout();
    private final WrapLayout hyphenatedLayout = new WrapLayout();
    private final WrapLayout unhyphenatedLayout = new WrapLayout();
    private final WrapLayout paragraphLayout = new WrapLayout();
    private final TextPaint keepBackgroundPaint = TextPaint.of(WHITE, STRIPE_BACKGROUND);
    private final TextPaint typewriterPaint = TextPaint.of(WHITE, ZONE_BACKGROUND);
    private final TextPaint wholeGradientPaint = TextPaint.of(GRADIENT_START, ZONE_BACKGROUND);
    private final TextPaint eachLineGradientPaint = TextPaint.of(GRADIENT_START, ZONE_BACKGROUND);
    private float elapsedSeconds;

    TextOverflowScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        drawTruncationPanel();
        drawScrollLabels();
        configurePaints();
        drawPaintPanel();
        drawTypewriter();
        Hyphenator.englishUs();
        layOutWrapPanels();
        layOutHyphenatePanel();
        drawWrapLabels();
        drawScrollingPanel();
        drawWrapPanels();
    }

    @Override
    public String id() {
        return "text-overflow";
    }

    @Override
    public String title() {
        return "Text overflow: truncate, scroll, wrap";
    }

    @Override
    public SceneGroup group() {
        return SceneGroup.GRIDS_LAYERS_AND_SPRITES;
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    protected void update(float delta) {
        elapsedSeconds += delta;
        drawScrollingPanel();
        drawTypewriter();
        drawWrapPanels();
    }

    private void configurePaints() {
        keepBackgroundPaint.setKeepBackground(true);
        wholeGradientPaint.setGradient(GRADIENT_END, GradientSpan.WHOLE_TEXT);
        eachLineGradientPaint.setGradient(GRADIENT_END, GradientSpan.EACH_LINE);
        paragraphLayout.layOut(WRAP_SAMPLE,
                WrapRules.between(LEFT_ZONE_COLUMN, LEFT_ZONE_COLUMN + LEFT_ZONE_WIDTH - 1));
    }

    private void drawPaintPanel() {
        drawLabel(LEFT_MARKER_COLUMN, PAINT_LABEL_ROW, "Text paint");
        GridDrawing.fillRectangle(grid, LEFT_ZONE_COLUMN, KEEP_BACKGROUND_ROW, LEFT_ZONE_WIDTH, 1,
                ' ', WHITE, STRIPE_BACKGROUND);
        TextDrawing.drawTruncated(grid, LEFT_ZONE_COLUMN, KEEP_BACKGROUND_ROW, LEFT_ZONE_WIDTH,
                KEEP_BACKGROUND_TEXT, TextDrawing.DEFAULT_ELLIPSIS, keepBackgroundPaint);
        drawLabel(LEFT_NOTE_COLUMN, KEEP_BACKGROUND_ROW, "keep");
        drawLabel(LEFT_NOTE_COLUMN, TYPEWRITER_ROW, "typing");
        drawLabel(LEFT_MARKER_COLUMN, WHOLE_GRADIENT_LABEL_ROW, "Gradient: whole text");
        TextDrawing.drawWrapped(grid, WHOLE_GRADIENT_LABEL_ROW + 1, paragraphLayout,
                wholeGradientPaint);
        drawLabel(LEFT_MARKER_COLUMN, EACH_LINE_GRADIENT_LABEL_ROW, "Gradient: each line");
        TextDrawing.drawWrapped(grid, EACH_LINE_GRADIENT_LABEL_ROW + 1, paragraphLayout,
                eachLineGradientPaint);
    }

    private void drawTypewriter() {
        float cycleSeconds = TYPEWRITER_TEXT.length() / TYPEWRITER_LETTERS_PER_SECOND
                + TYPEWRITER_PAUSE_SECONDS;
        float secondsInCycle = elapsedSeconds % cycleSeconds;
        typewriterPaint.setVisibleLetterCount(
                (int) (secondsInCycle * TYPEWRITER_LETTERS_PER_SECOND));
        TextDrawing.drawTruncated(grid, LEFT_ZONE_COLUMN, TYPEWRITER_ROW, LEFT_ZONE_WIDTH,
                TYPEWRITER_TEXT, TextDrawing.DEFAULT_ELLIPSIS, typewriterPaint);
    }

    private void drawTruncationPanel() {
        drawLabel(LEFT_MARKER_COLUMN, TRUNCATION_LABEL_ROW, "Truncation");
        int row = TRUNCATION_LABEL_ROW + 1;
        drawTruncatedRow(row++, LEFT_ZONE_WIDTH, LONG_SENTENCE, "w=20");
        drawTruncatedRow(row++, 8, LONG_SENTENCE, "w=8");
        drawTruncatedRow(row++, 1, LONG_SENTENCE, "w=1");
        drawTruncatedRow(row++, 0, LONG_SENTENCE, "w=0");
        drawMarker(row);
        TextDrawing.drawTruncated(grid, LEFT_ZONE_COLUMN, row, CUSTOM_ELLIPSIS_WIDTH,
                LONG_SENTENCE, CUSTOM_ELLIPSIS, WHITE, ZONE_BACKGROUND);
        drawLabel(LEFT_NOTE_COLUMN, row++, "w=10 '>'");
        drawTruncatedRow(row, LEFT_ZONE_WIDTH, SHORT_TEXT, "fits");
    }

    private void drawTruncatedRow(int row, int widthCells, String text, String note) {
        drawMarker(row);
        TextDrawing.drawTruncated(grid, LEFT_ZONE_COLUMN, row, widthCells, text, WHITE,
                ZONE_BACKGROUND);
        drawLabel(LEFT_NOTE_COLUMN, row, note);
    }

    private void drawMarker(int row) {
        GridDrawing.drawText(grid, LEFT_MARKER_COLUMN, row, "[", GRAY, CLEAR);
    }

    private void drawScrollLabels() {
        drawLabel(LEFT_MARKER_COLUMN, SCROLL_LABEL_ROW, "Scrolling");
        drawMarker(BACK_AND_FORTH_ROW);
        drawLabel(LEFT_NOTE_COLUMN, BACK_AND_FORTH_ROW, "back");
        drawMarker(LOOP_ROW);
        drawLabel(LEFT_NOTE_COLUMN, LOOP_ROW, "loop");
    }

    private void drawScrollingPanel() {
        TextDrawing.drawScrolling(grid, LEFT_ZONE_COLUMN, BACK_AND_FORTH_ROW, LEFT_ZONE_WIDTH,
                LONG_SENTENCE, BACK_AND_FORTH_SCROLL, elapsedSeconds, WHITE, ZONE_BACKGROUND);
        TextDrawing.drawScrolling(grid, LEFT_ZONE_COLUMN, LOOP_ROW, LEFT_ZONE_WIDTH,
                LONG_SENTENCE, LOOP_SCROLL, elapsedSeconds, WHITE, ZONE_BACKGROUND);
    }

    private void layOutWrapPanels() {
        WrapRules zoneRules = WrapRules.between(RIGHT_ZONE_START, RIGHT_ZONE_END);
        wordLayout.layOut(WRAP_SAMPLE, zoneRules);
        hangingLayout.layOut(WRAP_SAMPLE, zoneRules.withWrappedLineColumns(
                RIGHT_ZONE_START + HANGING_INDENT, RIGHT_ZONE_END));
        lineLimitLayout.layOut(LONG_PARAGRAPH, zoneRules.withMaxLines(LINE_LIMIT));
        int characterZoneStart = RIGHT_ZONE_START + NARROW_ZONE_WIDTH + NARROW_GAP;
        WrapRules wordRules = WrapRules.between(RIGHT_ZONE_START,
                RIGHT_ZONE_START + NARROW_ZONE_WIDTH - 1);
        WrapRules characterRules = WrapRules.between(characterZoneStart,
                characterZoneStart + NARROW_ZONE_WIDTH - 1)
                .withBreakMode(BreakMode.CHARACTER);
        narrowWordLayout.layOut(LONG_WORD_SAMPLE, wordRules);
        narrowCharacterLayout.layOut(LONG_WORD_SAMPLE, characterRules);
    }

    private void layOutHyphenatePanel() {
        int wordZoneStart = RIGHT_ZONE_START + NARROW_ZONE_WIDTH + NARROW_GAP;
        WrapRules hyphenateRules = WrapRules.between(RIGHT_ZONE_START,
                RIGHT_ZONE_START + NARROW_ZONE_WIDTH - 1)
                .withBreakMode(BreakMode.HYPHENATE);
        WrapRules wordRules = WrapRules.between(wordZoneStart,
                wordZoneStart + NARROW_ZONE_WIDTH - 1);
        hyphenatedLayout.layOut(HYPHENATION_SAMPLE, hyphenateRules);
        unhyphenatedLayout.layOut(HYPHENATION_SAMPLE, wordRules);
    }

    private void drawWrapLabels() {
        drawLabel(RIGHT_LABEL_COLUMN, WORD_PANEL_LABEL_ROW, "Wrap: word mode");
        drawLabel(RIGHT_LABEL_COLUMN, HANGING_PANEL_LABEL_ROW, "Wrap: hanging indent");
        drawLabel(RIGHT_LABEL_COLUMN, LINE_LIMIT_PANEL_LABEL_ROW, "Wrap: 3 lines at most");
        drawLabel(RIGHT_LABEL_COLUMN, MODES_PANEL_LABEL_ROW, "Word | character");
        drawLabel(RIGHT_LABEL_COLUMN, HYPHENATE_PANEL_LABEL_ROW, "Hyphenate | word");
    }

    private void drawWrapPanels() {
        drawWrapped(WORD_PANEL_LABEL_ROW + 1, wordLayout);
        drawWrapped(HANGING_PANEL_LABEL_ROW + 1, hangingLayout);
        drawWrapped(LINE_LIMIT_PANEL_LABEL_ROW + 1, lineLimitLayout);
        drawWrapped(MODES_PANEL_LABEL_ROW + 1, narrowWordLayout);
        drawWrapped(MODES_PANEL_LABEL_ROW + 1, narrowCharacterLayout);
        drawWrapped(HYPHENATE_PANEL_LABEL_ROW + 1, hyphenatedLayout);
        drawWrapped(HYPHENATE_PANEL_LABEL_ROW + 1, unhyphenatedLayout);
    }

    private void drawWrapped(int topRow, WrapLayout layout) {
        TextDrawing.drawWrapped(grid, topRow, layout, WHITE, ZONE_BACKGROUND);
    }

    private void drawLabel(int column, int row, String text) {
        GridDrawing.drawText(grid, column, row, text, GRAY, CLEAR);
    }
}
