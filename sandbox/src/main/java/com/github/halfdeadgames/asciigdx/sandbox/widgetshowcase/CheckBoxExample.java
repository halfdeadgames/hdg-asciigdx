package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.CheckBox;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.ToggleStyle;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Every check box style of the skin: unchecked, checked, and both disabled. */
final class CheckBoxExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"checkBox"};
    private static final String LABEL_STYLE = "text";

    @Override
    public String id() {
        return "check-box";
    }

    @Override
    public String title() {
        return "CheckBox";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        Label lastChange = new Label("Last change: none", skin.getLabelStyle(LABEL_STYLE));
        sheet.addHeading("Live controls");
        sheet.addDisabledToggle();
        sheet.addDemo("live: last change", lastChange);
        for (String styleName : STYLE_NAMES) {
            addStyleDemos(sheet, styleName, skin.getCheckBoxStyle(styleName), lastChange);
        }
        return sheet.build();
    }

    private void addStyleDemos(DemoSheet sheet, String styleName, ToggleStyle style,
            Label lastChange) {
        sheet.addHeading(styleName);
        CheckBox unchecked = createLive("Unchecked", style, false, lastChange, sheet);
        sheet.addDemo(styleName + " (unchecked)", unchecked);
        CheckBox checked = createLive("Checked", style, true, lastChange, sheet);
        sheet.addDemo(styleName + " (checked)", checked);
        sheet.addDemo(styleName + " (disabled)", createDisabled("Disabled off", style, false));
        sheet.addDemo(styleName + " (disabled, checked)",
                createDisabled("Disabled on", style, true));
    }

    private static CheckBox createLive(String text, ToggleStyle style, boolean checked,
            Label lastChange, DemoSheet sheet) {
        CheckBox box = new CheckBox(text, style);
        box.setChecked(checked);
        box.addChangeListener(source -> showChange(lastChange, source));
        sheet.trackDisabled(box::setDisabled);
        return box;
    }

    private static CheckBox createDisabled(String text, ToggleStyle style, boolean checked) {
        CheckBox box = new CheckBox(text, style);
        box.setChecked(checked);
        box.setDisabled(true);
        return box;
    }

    private static void showChange(Label lastChange, CheckBox source) {
        lastChange.setText("Last change: " + source.getText()
                + (source.isChecked() ? " checked" : " unchecked"));
        lastChange.pack();
    }
}
