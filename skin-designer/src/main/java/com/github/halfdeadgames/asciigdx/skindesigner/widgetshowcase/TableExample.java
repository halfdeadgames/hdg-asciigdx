package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.HorizontalAlignment;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.ProgressBar;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Slider;
import com.github.halfdeadgames.asciigdx.ui.Table;
import com.github.halfdeadgames.asciigdx.ui.TableCell;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.ValueRange;
import com.github.halfdeadgames.asciigdx.ui.VerticalAlignment;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Table layout: column alignment, row alignment, column spans, and cell padding. */
final class TableExample implements WidgetExample {
    private static final int RULER_WIDTH = 16;
    private static final int ROW_BAR_HEIGHT = 5;
    private static final int VERTICAL_GAP = 2;
    private static final int MAX_PADDING = 6;
    private static final int PADDING_SLIDER_LENGTH = 14;
    private static final ValueRange PADDING_RANGE = ValueRange.of(0f, MAX_PADDING, 1f);
    private static final ValueRange BAR_RANGE = ValueRange.of(0f, 1f, 1f);

    @Override
    public String id() {
        return "table";
    }

    @Override
    public String title() {
        return "Table";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        LabelStyle ruler = skin.getLabelStyle("accent");
        LabelStyle cellLook = skin.getLabelStyle("highlight");
        sheet.addHeading("Table layout");
        sheet.addDemo("columns: left, center, right", createColumnTable(ruler, cellLook));
        sheet.addDemo("rows: top, center, bottom", createRowTable(skin, cellLook));
        addPaddingDemo(sheet, skin, ruler);
        return sheet.build();
    }

    private Table createColumnTable(LabelStyle ruler, LabelStyle cellLook) {
        Table table = new Table();
        table.add(createRuler("colspan 3", 3 * RULER_WIDTH, ruler)).colspan(3);
        table.row();
        for (int i = 0; i < 3; i++) {
            table.add(createRuler("column width", RULER_WIDTH, ruler));
        }
        table.row();
        table.add(new Label("L", cellLook)).alignX(HorizontalAlignment.LEFT);
        table.add(new Label("C", cellLook)).alignX(HorizontalAlignment.CENTER);
        table.add(new Label("R", cellLook)).alignX(HorizontalAlignment.RIGHT);
        table.pack();
        return table;
    }

    private Table createRowTable(Skin skin, LabelStyle cellLook) {
        Table table = new Table();
        ProgressBar rowHeight = ProgressBar.vertical(BAR_RANGE, ROW_BAR_HEIGHT,
                skin.getProgressBarStyle("progressBar"));
        table.add(rowHeight).padRight(VERTICAL_GAP);
        table.add(new Label("TOP", cellLook)).alignY(VerticalAlignment.TOP)
                .padRight(VERTICAL_GAP);
        table.add(new Label("CENTER", cellLook)).alignY(VerticalAlignment.CENTER)
                .padRight(VERTICAL_GAP);
        table.add(new Label("BOTTOM", cellLook)).alignY(VerticalAlignment.BOTTOM);
        table.pack();
        return table;
    }

    private void addPaddingDemo(DemoSheet sheet, Skin skin, LabelStyle ruler) {
        Table table = new Table();
        TableCell first = table.add(new Label(" A ", ruler));
        TableCell second = table.add(new Label(" B ", ruler));
        table.add(new Label(" C ", ruler));
        table.pack();
        Slider padding = Slider.horizontal(PADDING_RANGE, PADDING_SLIDER_LENGTH,
                skin.getSliderStyle("slider"));
        padding.addChangeListener(changed -> {
            int cells = Math.round(changed.getValue());
            first.padRight(cells);
            second.padRight(cells);
            table.invalidate();
            table.pack();
        });
        sheet.addHeading("Padding");
        sheet.addDemo("live: space right of A and B", padding);
        sheet.addDemo("cells A, B, C", table);
    }

    private static Label createRuler(String text, int width, LabelStyle style) {
        Label label = new Label(String.format("%-" + width + "s", text), style);
        label.pack();
        return label;
    }
}
