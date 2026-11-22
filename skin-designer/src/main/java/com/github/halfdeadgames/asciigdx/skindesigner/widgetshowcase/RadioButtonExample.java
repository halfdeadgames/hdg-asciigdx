package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.ButtonGroup;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.RadioButton;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.ToggleStyle;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Every radio button style of the skin: a live group of three and a disabled pair. */
final class RadioButtonExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"radioButton"};
    private static final String[] CHOICE_TEXTS = {"Choice A", "Choice B", "Choice C"};
    private static final String LABEL_STYLE = "text";

    @Override
    public String id() {
        return "radio-button";
    }

    @Override
    public String title() {
        return "RadioButton";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        Label selection = new Label("Selected: none", skin.getLabelStyle(LABEL_STYLE));
        sheet.addHeading("Live controls");
        sheet.addDisabledToggle();
        sheet.addDemo("live: selected choice", selection);
        for (String styleName : STYLE_NAMES) {
            sheet.addHeading(styleName);
            addLiveGroup(sheet, styleName, skin.getRadioButtonStyle(styleName), selection);
            addDisabledGroup(sheet, styleName, skin.getRadioButtonStyle(styleName));
        }
        return sheet.build();
    }

    private void addLiveGroup(DemoSheet sheet, String styleName, ToggleStyle style,
            Label selection) {
        ButtonGroup group = new ButtonGroup();
        group.addChangeListener(changed -> showSelection(selection, changed.getChecked()));
        RadioButton first = null;
        for (int i = 0; i < CHOICE_TEXTS.length; i++) {
            RadioButton radio = new RadioButton(CHOICE_TEXTS[i], style);
            group.add(radio);
            sheet.trackDisabled(radio::setDisabled);
            if (first == null) {
                first = radio;
                sheet.addDemo(styleName + " (group)", radio);
                continue;
            }
            sheet.addDemo(radio);
        }
        group.setChecked(first);
    }

    private void addDisabledGroup(DemoSheet sheet, String styleName, ToggleStyle style) {
        ButtonGroup group = new ButtonGroup();
        RadioButton off = new RadioButton("Disabled off", style);
        RadioButton on = new RadioButton("Disabled on", style);
        group.add(off, on);
        group.setChecked(on);
        off.setDisabled(true);
        on.setDisabled(true);
        sheet.addDemo(styleName + " (disabled)", off);
        sheet.addDemo(on);
    }

    private static void showSelection(Label selection, RadioButton checked) {
        selection.setText("Selected: " + (checked == null ? "none" : checked.getText()));
        selection.pack();
    }
}
