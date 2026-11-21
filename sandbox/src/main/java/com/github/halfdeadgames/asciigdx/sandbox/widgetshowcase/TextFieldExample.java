package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.TextFieldStyle;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** The text field style of the skin: empty, filled, short, and disabled, with echo labels. */
final class TextFieldExample implements WidgetExample {
    private static final String STYLE_NAME = "default";
    private static final int FIELD_COLUMNS = 24;
    private static final int SHORT_FIELD_COLUMNS = 8;
    private static final int SHORT_MAX_LENGTH = 6;

    @Override
    public String id() {
        return "text-field";
    }

    @Override
    public String title() {
        return "TextField";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, int columns, int rows) {
        DemoSheet sheet = new DemoSheet(skin);
        TextFieldStyle style = skin.getTextFieldStyle(STYLE_NAME);
        Label typed = new Label("Typed: ", skin.getLabelStyle("text"));
        Label entered = new Label("Entered: nothing yet", skin.getLabelStyle("text"));
        sheet.addHeading("Live controls");
        sheet.addDisabledToggle();
        sheet.addDemo("live: echo of the typed text", typed);
        sheet.addDemo("live: text after Enter", entered);
        sheet.addHeading(STYLE_NAME);
        addEchoField(sheet, style, typed, entered);
        sheet.addDemo(STYLE_NAME + " (with text)",
                createTrackedField(sheet, style, "Some text", FIELD_COLUMNS));
        TextField limited = createTrackedField(sheet, style, "", SHORT_FIELD_COLUMNS);
        limited.setMaxLength(SHORT_MAX_LENGTH);
        sheet.addDemo(STYLE_NAME + " (max " + SHORT_MAX_LENGTH + " characters)", limited);
        TextField disabled = new TextField(style, FIELD_COLUMNS);
        disabled.setText("Cannot edit");
        disabled.setDisabled(true);
        sheet.addDemo(STYLE_NAME + " (disabled)", disabled);
        return sheet.build();
    }

    private void addEchoField(DemoSheet sheet, TextFieldStyle style, Label typed,
            Label entered) {
        TextField field = new TextField(style, FIELD_COLUMNS);
        field.setPlaceholderText("Placeholder");
        field.addChangeListener(changed -> showText(typed, "Typed: ", changed.getText()));
        field.addEnterListener(source -> {
            showText(entered, "Entered: ", source.getText());
            return true;
        });
        sheet.trackDisabled(field::setDisabled);
        sheet.addDemo(STYLE_NAME + " (empty, placeholder)", field);
    }

    private TextField createTrackedField(DemoSheet sheet, TextFieldStyle style, String text,
            int width) {
        TextField field = new TextField(style, width);
        field.setText(text);
        sheet.trackDisabled(field::setDisabled);
        return field;
    }

    private static void showText(Label target, String prefix, String text) {
        target.setText(prefix + text);
        target.pack();
    }
}
