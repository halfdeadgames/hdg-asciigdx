package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.ButtonGroup;
import com.github.halfdeadgames.asciigdx.ui.ChangeListener;
import com.github.halfdeadgames.asciigdx.ui.CheckBox;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.RadioButton;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.ToggleStyle;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Interactive check of check boxes and radio buttons: a checked box, a disabled box, a group of
 * three radio buttons, and a label that shows the last change made by the user. Click with the
 * mouse, or focus a widget by pressing it and use Enter or Space.
 */
class TogglesPage implements UiPage {
    private static final int ROOT_X = 4;
    private static final int ROOT_Y = 2;
    private static final int LAST_CHANGE_Y = 0;
    private static final int SECTIONS_Y = 2;
    private static final int CHECK_BOXES_Y = 2;
    private static final int RADIO_CAPTION_Y = 9;
    private static final int RADIOS_Y = 12;
    private static final int ROW_STEP = 2;

    private final ToggleStyle checkBox;
    private final ToggleStyle radioButton;
    private final LabelStyle caption;

    TogglesPage(Skin skin) {
        checkBox = skin.getCheckBoxStyle("checkBox");
        radioButton = skin.getRadioButtonStyle("radioButton");
        caption = skin.getLabelStyle("text");
    }

    @Override
    public String id() {
        return "toggles";
    }

    @Override
    public String title() {
        return "Toggles";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        WidgetGroup content = new WidgetGroup();
        WidgetGroup root = new WidgetGroup();
        root.setPosition(ROOT_X, ROOT_Y);
        Label lastChange = new Label("Last change: none", caption);
        lastChange.setPosition(0, LAST_CHANGE_Y);
        root.addChild(lastChange);
        ChangeListener<Widget> showChange = source -> showChange(lastChange, source);
        addSection(root, "Toggles", checkBox, radioButton, 0, SECTIONS_Y, showChange);
        content.addChild(root);
        return content;
    }

    private void addSection(WidgetGroup root, String title, ToggleStyle checkBoxStyle,
            ToggleStyle radioStyle, int x, int y, ChangeListener<Widget> listener) {
        addCaption(root, title + ": check boxes", x, y);
        addCheckBox(root, "Sound", checkBoxStyle, x, y + CHECK_BOXES_Y, true, false, listener);
        addCheckBox(root, "Music", checkBoxStyle, x, y + CHECK_BOXES_Y + ROW_STEP, false, false,
                listener);
        addCheckBox(root, "Disabled", checkBoxStyle, x, y + CHECK_BOXES_Y + 2 * ROW_STEP, false,
                true, listener);
        addCaption(root, title + ": radio group", x, y + RADIO_CAPTION_Y);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < 3; i++) {
            String text = "Choice " + (char) ('A' + i);
            RadioButton radio = new RadioButton(text, radioStyle);
            radio.setPosition(x, y + RADIOS_Y + i * ROW_STEP);
            radio.addChangeListener(listener);
            group.add(radio);
            radio.setChecked(i == 0);
            root.addChild(radio);
        }
    }

    private static void addCheckBox(WidgetGroup root, String text, ToggleStyle style, int x, int y,
            boolean checked, boolean disabled, ChangeListener<Widget> listener) {
        CheckBox box = new CheckBox(text, style);
        box.setPosition(x, y);
        box.setChecked(checked);
        box.setDisabled(disabled);
        box.addChangeListener(listener);
        root.addChild(box);
    }

    private void addCaption(WidgetGroup root, String text, int x, int y) {
        Label captionLabel = new Label(text, caption);
        captionLabel.setPosition(x, y);
        root.addChild(captionLabel);
    }

    private static void showChange(Label lastChange, Widget source) {
        lastChange.setText("Last change: " + describe(source));
        lastChange.pack();
    }

    private static String describe(Widget source) {
        if (source instanceof CheckBox) {
            CheckBox box = (CheckBox) source;
            return box.getText() + (box.isChecked() ? " checked" : " unchecked");
        }
        if (source instanceof RadioButton) {
            RadioButton radio = (RadioButton) source;
            return radio.getText() + (radio.isChecked() ? " on" : " off");
        }
        return source.toString();
    }
}
