package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.HorizontalAlignment;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Table;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.VerticalAlignment;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/**
 * Static check of table layout: a centered column-spanning title, a form with right-aligned names
 * and filled values, nine alignments in expanding cells, labels of one row with a bottom-aligned
 * label, and cell padding.
 */
class TablePage implements UiPage {
    private static final int TABLE_PADDING = 1;
    private static final int LABEL_GAP = 2;

    private final int panelBlueRgba;
    private final int panelGreenRgba;
    private final LabelStyle titleStyle;
    private final LabelStyle plain;
    private final LabelStyle highlightStyle;
    private final LabelStyle accentStyle;

    TablePage(Skin skin) {
        panelBlueRgba = skin.getRgba("deepBlue");
        panelGreenRgba = skin.getRgba("darkGreen");
        titleStyle = skin.getLabelStyle("title");
        plain = skin.getLabelStyle("text");
        highlightStyle = skin.getLabelStyle("highlight");
        accentStyle = skin.getLabelStyle("accent");
    }

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

    private void addTitle(Table root) {
        root.add(new Label("Table layout", titleStyle)).colspan(2)
                .alignX(HorizontalAlignment.CENTER);
        root.row();
    }

    private void addForm(Table root) {
        addFormRow(root, "Name", "Values fill the column (expandX + fillX)");
        addFormRow(root, "Role", "Names are right-aligned in column 1");
        addFormRow(root, "Notes", "Cyan shows the filled width");
    }

    private void addFormRow(Table root, String name, String value) {
        root.add(new Label(name, plain)).alignX(HorizontalAlignment.RIGHT).padRight(LABEL_GAP);
        root.add(new Label(value, highlightStyle)).expandX().fillX();
        root.row();
    }

    private void addAlignmentGrid(Table root) {
        Table grid = new Table();
        int index = 0;
        for (VerticalAlignment vertical : VerticalAlignment.values()) {
            for (HorizontalAlignment horizontal : HorizontalAlignment.values()) {
                int background = index % 2 == 0 ? panelBlueRgba : panelGreenRgba;
                grid.add(createAlignedCell(background, horizontal, vertical))
                        .expand().fill().pad(0, 0, 1, 1);
                index++;
            }
            grid.row();
        }
        root.add(grid).colspan(2).expand().fill();
        root.row();
    }

    private Widget createAlignedCell(int background, HorizontalAlignment horizontal,
            VerticalAlignment vertical) {
        Framed cell = new Framed(background);
        Label label = new Label(vertical + " " + horizontal, accentStyle);
        cell.add(label).expand().align(horizontal, vertical);
        return cell;
    }

    private void addMixedHeights(Table root) {
        Table mixed = new Table();
        mixed.add(new Label("Left", plain)).padRight(LABEL_GAP);
        mixed.add(new Label("Cyan label", highlightStyle)).padRight(LABEL_GAP);
        mixed.add(new Label("Gold label", accentStyle)).alignY(VerticalAlignment.BOTTOM);
        root.add(mixed).colspan(2).padTop(1);
        root.row();
    }

    private void addPaddingExamples(Table root) {
        Table examples = new Table();
        examples.add(createPadded("pad(1)", 1, 1, 1, 1)).padRight(LABEL_GAP);
        examples.add(createPadded("padLeft(4)", 0, 4, 0, 0)).padRight(LABEL_GAP);
        examples.add(createPadded("padTop(2)", 2, 0, 0, 0));
        root.add(examples).colspan(2).padTop(1);
    }

    private Widget createPadded(String text, int top, int left, int bottom, int right) {
        Framed frame = new Framed(panelBlueRgba);
        frame.add(new Label(text, highlightStyle)).pad(top, left, bottom, right);
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
