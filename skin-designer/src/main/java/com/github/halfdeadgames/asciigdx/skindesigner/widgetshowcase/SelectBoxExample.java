package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.SelectBox;
import com.github.halfdeadgames.asciigdx.ui.SelectBoxListWidth;
import com.github.halfdeadgames.asciigdx.ui.SelectBoxStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/**
 * Select boxes in the forms that matter for a skin: one near the top, one at the bottom of the
 * page (scroll down to it, so its list opens upward), one with many items and a scroll bar, one
 * with color swatches, and one whose list matches the box width and cuts long names off.
 */
final class SelectBoxExample implements WidgetExample {
    private static final String STYLE_NAME = "selectBox";
    private static final String LABEL_STYLE = "text";
    private static final int MANY_ITEM_COUNT = 40;
    private static final int MATCH_BUTTON_COLUMNS = 14;
    private static final List<String> FRUIT = List.of("Apple", "Banana", "Cherry", "Date",
            "Elderberry", "Fig");
    private static final List<String> LONG_NAMES = List.of("Short", "A medium-sized name",
            "An extraordinarily long name that cannot fit", "Another very long entry here");
    private static final List<SwatchItem> COLORS = List.of(
            new SwatchItem("Red", 0xE03030FF), new SwatchItem("Orange", 0xF08020FF),
            new SwatchItem("Gold", 0xFFD700FF), new SwatchItem("Green", 0x30C050FF),
            new SwatchItem("Cyan", 0x00D0D0FF), new SwatchItem("Blue", 0x3050E0FF),
            new SwatchItem("Violet", 0x9040D0FF));

    private final List<SelectBox<?>> createdBoxes = new ArrayList<>();

    private record SwatchItem(String name, int rgba) {
    }

    @Override
    public String id() {
        return "select-box";
    }

    @Override
    public String title() {
        return "SelectBox";
    }

    @Override
    public List<String> skinSections() {
        return List.of(SkinSchema.SELECT_BOXES, SkinSchema.BUTTONS, SkinSchema.SCROLL_PANES);
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        closeCreatedBoxes();
        SelectBoxStyle style = skin.getSelectBoxStyle(STYLE_NAME);
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        Label lastPick = new Label("Last pick: none", skin.getLabelStyle(LABEL_STYLE));
        sheet.addHeading("Live controls");
        sheet.addDisabledToggle();
        sheet.addDemo("live: last pick", lastPick);
        sheet.addHeading(STYLE_NAME);
        sheet.addDemo("near the top", live(sheet, createFruitBox(uiRoot, style), lastPick));
        sheet.addDemo("many items (scroll bar)",
                live(sheet, createManyBox(uiRoot, style), lastPick));
        sheet.addDemo("color swatches", live(sheet, createSwatchBox(uiRoot, style), lastPick));
        sheet.addDemo("list matches the box", live(sheet, createLongNameBox(uiRoot, style),
                lastPick));
        sheet.addDemo("disabled", createDisabledBox(uiRoot, style));
        sheet.addEmptyRows(rows);
        sheet.addDemo("near the bottom (opens up)",
                live(sheet, createFruitBox(uiRoot, style), lastPick));
        return sheet.build();
    }

    @Override
    public void exampleHidden() {
        closeCreatedBoxes();
    }

    private void closeCreatedBoxes() {
        for (int i = 0; i < createdBoxes.size(); i++) {
            createdBoxes.get(i).closeList();
        }
        createdBoxes.clear();
    }

    private <T> SelectBox<T> live(DemoSheet sheet, SelectBox<T> box, Label lastPick) {
        box.addChangeListener(source -> showPick(lastPick, source));
        sheet.trackDisabled(box::setDisabled);
        createdBoxes.add(box);
        return box;
    }

    private static void showPick(Label lastPick, SelectBox<?> source) {
        lastPick.setText("Last pick: " + source.getSelected());
        lastPick.pack();
    }

    private static SelectBox<String> createFruitBox(UiRoot uiRoot, SelectBoxStyle style) {
        SelectBox<String> box = SelectBox.of(uiRoot, style);
        box.setItems(FRUIT);
        box.setSelectedIndex(1);
        box.pack();
        return box;
    }

    private static SelectBox<String> createManyBox(UiRoot uiRoot, SelectBoxStyle style) {
        List<String> items = new ArrayList<>();
        for (int number = 1; number <= MANY_ITEM_COUNT; number++) {
            items.add("Item number " + number);
        }
        SelectBox<String> box = SelectBox.of(uiRoot, style);
        box.setItems(items);
        box.setSelectedIndex(0);
        box.pack();
        return box;
    }

    private static SelectBox<SwatchItem> createSwatchBox(UiRoot uiRoot, SelectBoxStyle style) {
        SelectBox<SwatchItem> box = SelectBox.of(uiRoot, style);
        box.setItemText(SwatchItem::name);
        box.setItemSwatch(SwatchItem::rgba);
        box.setItems(COLORS);
        box.setSelectedIndex(2);
        box.pack();
        return box;
    }

    private static SelectBox<String> createLongNameBox(UiRoot uiRoot, SelectBoxStyle style) {
        SelectBox<String> box = SelectBox.of(uiRoot, style);
        box.setItems(LONG_NAMES);
        box.setListWidth(SelectBoxListWidth.MATCH_BUTTON);
        box.setSelectedIndex(0);
        box.setSize(MATCH_BUTTON_COLUMNS, 0);
        return box;
    }

    private static SelectBox<String> createDisabledBox(UiRoot uiRoot, SelectBoxStyle style) {
        SelectBox<String> box = createFruitBox(uiRoot, style);
        box.setDisabled(true);
        return box;
    }
}
