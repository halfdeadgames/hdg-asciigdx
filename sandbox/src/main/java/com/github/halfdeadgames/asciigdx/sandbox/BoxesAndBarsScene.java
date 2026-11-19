package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import java.util.List;
import java.util.Locale;

/** Four box styles side by side, plus horizontal and vertical eighth-block bars. */
class BoxesAndBarsScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int ROWS = 18;

    private static final int NAVY = 0x1A2A6CFF;
    private static final int BAR_COLOR = 0x40D060FF;

    private static final int BOX_WIDTH = 8;
    private static final int BOX_HEIGHT = 5;
    private static final int BOX_SPACING = 9;
    private static final int BOX_FIRST_COLUMN = 2;
    private static final int BOX_ROW = 2;

    private static final int HORIZONTAL_BAR_COLUMN = 2;
    private static final int HORIZONTAL_BAR_FIRST_ROW = 10;
    private static final int HORIZONTAL_BAR_LENGTH = 10;
    private static final int LABEL_GAP = 1;

    private static final int VERTICAL_BAR_FIRST_COLUMN = 21;
    private static final int VERTICAL_BAR_SPACING = 5;
    private static final int VERTICAL_BAR_TOP = 10;
    private static final int VERTICAL_BAR_LENGTH = 5;
    private static final int VERTICAL_BAR_LABEL_ROW = VERTICAL_BAR_TOP + VERTICAL_BAR_LENGTH;

    private static final List<BoxStyle> BOX_STYLES = List.of(BoxStyle.values());
    private static final List<Float> BAR_FRACTIONS = List.of(0f, 0.3f, 0.55f, 1f);

    BoxesAndBarsScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        fillDemoGrid();
    }

    @Override
    public String id() {
        return "boxes-and-bars";
    }

    @Override
    public String title() {
        return "Boxes and bars";
    }

    @Override
    public SceneGroup group() {
        return SceneGroup.GRIDS_LAYERS_AND_SPRITES;
    }

    private void fillDemoGrid() {
        GridDrawing.drawText(grid, 2, 1, "Box styles", GOLD, CLEAR);
        drawBoxes();
        GridDrawing.drawText(grid, 2, 9, "Horizontal", GOLD, CLEAR);
        GridDrawing.drawText(grid, VERTICAL_BAR_FIRST_COLUMN, 9, "Vertical", GOLD, CLEAR);
        drawHorizontalBars();
        drawVerticalBars();
    }

    private void drawBoxes() {
        for (int index = 0; index < BOX_STYLES.size(); index++) {
            BoxStyle style = BOX_STYLES.get(index);
            int left = BOX_FIRST_COLUMN + index * BOX_SPACING;
            GridDrawing.drawBox(grid, left, BOX_ROW, BOX_WIDTH, BOX_HEIGHT, style, WHITE, CLEAR);
            String label = style.name().toLowerCase(Locale.ROOT);
            GridDrawing.drawText(grid, left, BOX_ROW + BOX_HEIGHT, label, CYAN, CLEAR);
        }
    }

    private void drawHorizontalBars() {
        for (int index = 0; index < BAR_FRACTIONS.size(); index++) {
            float fraction = BAR_FRACTIONS.get(index);
            int row = HORIZONTAL_BAR_FIRST_ROW + index * 2;
            GridDrawing.drawHorizontalBar(grid, HORIZONTAL_BAR_COLUMN, row, HORIZONTAL_BAR_LENGTH,
                    fraction, BAR_COLOR, NAVY);
            GridDrawing.drawText(grid,
                    HORIZONTAL_BAR_COLUMN + HORIZONTAL_BAR_LENGTH + LABEL_GAP, row,
                    formatFraction(fraction), WHITE, CLEAR);
        }
    }

    private void drawVerticalBars() {
        for (int index = 0; index < BAR_FRACTIONS.size(); index++) {
            float fraction = BAR_FRACTIONS.get(index);
            int column = VERTICAL_BAR_FIRST_COLUMN + index * VERTICAL_BAR_SPACING;
            GridDrawing.drawVerticalBar(grid, column, VERTICAL_BAR_TOP, VERTICAL_BAR_LENGTH,
                    fraction, BAR_COLOR, NAVY);
            GridDrawing.drawText(grid, column, VERTICAL_BAR_LABEL_ROW, formatFraction(fraction),
                    WHITE, CLEAR);
        }
    }

    private static String formatFraction(float fraction) {
        return String.format(Locale.ROOT, "%.2f", fraction);
    }
}
