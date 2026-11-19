package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.HorizontalAlignment;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Table;
import com.github.halfdeadgames.asciigdx.ui.UiFont;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.VerticalAlignment;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/**
 * Static check of table layout: a centered column-spanning title, a form with right-aligned names
 * and filled values, nine alignments in expanding cells, tall and square labels in one row, and
 * cell padding.
 */
class TablePage implements UiPage {
    private static final int BLACK = 0x000000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFD700FF;
    private static final int CYAN = 0x00FFFFFF;
    private static final int PANEL_BLUE = 0x203070FF;
    private static final int PANEL_GREEN = 0x1F5030FF;
    private static final int TABLE_PADDING = 1;
    private static final int LABEL_GAP = 2;
    private static final LabelStyle TITLE = LabelStyle.of(UiFont.TALL, GOLD, BLACK);
    private static final LabelStyle TALL_PLAIN = LabelStyle.of(UiFont.TALL, WHITE, BLACK);
    private static final LabelStyle PLAIN = LabelStyle.of(UiFont.SQUARE, WHITE, BLACK);
    private static final LabelStyle ON_CYAN = LabelStyle.of(UiFont.SQUARE, BLACK, CYAN);
    private static final LabelStyle ON_GOLD = LabelStyle.of(UiFont.SQUARE, BLACK, GOLD);

    @Override
    public String id() {
        return "table";
    }

    @Override
    public String title() {
        return "Table";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        Table root = new Table();
        root.setPadding(TABLE_PADDING);
        root.setSize(columns, rows);
        addTitle(root);
        addForm(root);
        addAlignmentGrid(root);
        addMixedHeights(root);
        addPaddingExamples(root);
        return root;
    }

    private static void addTitle(Table root) {
        root.add(new Label("Table layout", TITLE)).colspan(2).alignX(HorizontalAlignment.CENTER);
        root.row();
    }

    private static void addForm(Table root) {
        addFormRow(root, "Name", "Values fill the column (expandX + fillX)");
        addFormRow(root, "Role", "Names are right-aligned in column 1");
        addFormRow(root, "Notes", "Cyan shows the filled width");
    }

    private static void addFormRow(Table root, String name, String value) {
        root.add(new Label(name, PLAIN)).alignX(HorizontalAlignment.RIGHT).padRight(LABEL_GAP);
        root.add(new Label(value, ON_CYAN)).expandX().fillX();
        root.row();
    }

    private static void addAlignmentGrid(Table root) {
        Table grid = new Table();
        int index = 0;
        for (VerticalAlignment vertical : VerticalAlignment.values()) {
            for (HorizontalAlignment horizontal : HorizontalAlignment.values()) {
                int background = index % 2 == 0 ? PANEL_BLUE : PANEL_GREEN;
                grid.add(createAlignedCell(background, horizontal, vertical))
                        .expand().fill().pad(0, 0, 1, 1);
                index++;
            }
            grid.row();
        }
        root.add(grid).colspan(2).expand().fill();
        root.row();
    }

    private static Widget createAlignedCell(int background, HorizontalAlignment horizontal,
            VerticalAlignment vertical) {
        Framed cell = new Framed(background);
        Label label = new Label(vertical + " " + horizontal, ON_GOLD);
        cell.add(label).expand().align(horizontal, vertical);
        return cell;
    }

    private static void addMixedHeights(Table root) {
        Table mixed = new Table();
        mixed.add(new Label("Tall", TALL_PLAIN)).padRight(LABEL_GAP);
        mixed.add(new Label("Square top", ON_CYAN)).padRight(LABEL_GAP);
        mixed.add(new Label("Tall", TALL_PLAIN)).padRight(LABEL_GAP);
        mixed.add(new Label("Square bottom", ON_CYAN)).alignY(VerticalAlignment.BOTTOM);
        root.add(mixed).colspan(2).padTop(1);
        root.row();
    }

    private static void addPaddingExamples(Table root) {
        Table examples = new Table();
        examples.add(createPadded("pad(1)", 1, 1, 1, 1)).padRight(LABEL_GAP);
        examples.add(createPadded("padLeft(4)", 0, 4, 0, 0)).padRight(LABEL_GAP);
        examples.add(createPadded("padTop(2)", 2, 0, 0, 0));
        root.add(examples).colspan(2).padTop(1);
    }

    private static Widget createPadded(String text, int top, int left, int bottom, int right) {
        Framed frame = new Framed(PANEL_BLUE);
        frame.add(new Label(text, ON_CYAN)).pad(top, left, bottom, right);
        return frame;
    }

    /** A table that paints a solid background under its children. */
    private static final class Framed extends Table {
        private final int backgroundRgba;

        Framed(int backgroundRgba) {
            this.backgroundRgba = backgroundRgba;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
            target.fillBackground(left, top, getWidth(), getHeight(), backgroundRgba);
            super.drawContent(target, left, top);
        }
    }
}
