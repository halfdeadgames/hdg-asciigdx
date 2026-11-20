package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.FocusDirection;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.TextFieldFilter;
import com.github.halfdeadgames.asciigdx.ui.TextFieldStyle;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Interactive check of text fields: a short field with prefilled long text that scrolls and shows
 * ellipses, a digits-only field, a field with placeholder text, and a disabled field. Click a field
 * or press Tab to focus it, then type, use the arrow keys, Home, End, Backspace, and Delete. A
 * label shows the last change and the last Enter.
 */
class TextFieldsPage implements UiPage {
    private static final int ROOT_X = 4;
    private static final int ROOT_Y = 2;
    private static final int SCROLLING_WIDTH = 16;
    private static final int DIGITS_WIDTH = 12;
    private static final int PLACEHOLDER_WIDTH = 24;
    private static final int DISABLED_WIDTH = 20;
    private static final int ROW_STEP = 3;
    private static final String LONG_TEXT = "The quick brown fox jumps over the lazy dog";

    private final TextFieldStyle fieldStyle;
    private final LabelStyle caption;
    private final UiRoot uiRoot;
    private TextField firstField;

    TextFieldsPage(UiRoot uiRoot, Skin skin) {
        this.uiRoot = uiRoot;
        fieldStyle = skin.getTextFieldStyle("default");
        caption = skin.getLabelStyle("text");
    }

    @Override
    public String id() {
        return "text-fields";
    }

    @Override
    public String title() {
        return "Text fields";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        WidgetGroup content = new WidgetGroup();
        WidgetGroup root = new WidgetGroup();
        root.setPosition(ROOT_X, ROOT_Y);
        Label lastEvent = new Label("Last change: none", caption);
        lastEvent.setPosition(0, 4 * ROW_STEP);
        root.addChild(lastEvent);
        firstField = addField(root, "Long text scrolls, with ellipses at cut ends", 0,
                SCROLLING_WIDTH, lastEvent);
        firstField.setText(LONG_TEXT);
        TextField digits = addField(root, "Digits only, at most 8", ROW_STEP, DIGITS_WIDTH,
                lastEvent);
        digits.setFilter(TextFieldFilter.digits());
        digits.setMaxLength(8);
        TextField placeholder = addField(root, "Placeholder text while empty; Enter reports",
                2 * ROW_STEP, PLACEHOLDER_WIDTH, lastEvent);
        placeholder.setPlaceholderText("Type your name");
        placeholder.addEnterListener(field -> reportEnter(lastEvent, field));
        TextField disabled = addField(root, "Disabled", 3 * ROW_STEP, DISABLED_WIDTH, lastEvent);
        disabled.setText("Not editable");
        disabled.setDisabled(true);
        content.addChild(root);
        return content;
    }

    /** Gives the first field keyboard focus. It looks focused when the last input was a key. */
    void focusFirstField() {
        uiRoot.setKeyboardFocus(firstField);
    }

    /**
     * Gives the first field keyboard focus through keyboard navigation, so the focused look shows
     * even when the last input was not a key. Must not be called while an event is delivered.
     */
    void focusFirstFieldVisibly() {
        uiRoot.setKeyboardFocus(null);
        uiRoot.moveFocus(FocusDirection.NEXT);
    }

    private TextField addField(WidgetGroup root, String captionText, int y, int width,
            Label lastEvent) {
        Label captionLabel = new Label(captionText, caption);
        captionLabel.setPosition(0, y);
        root.addChild(captionLabel);
        TextField field = new TextField(fieldStyle, width);
        field.setPosition(0, y + 1);
        field.addChangeListener(source -> showChange(lastEvent, source));
        root.addChild(field);
        return field;
    }

    private static void showChange(Label lastEvent, TextField source) {
        lastEvent.setText("Last change: " + source.getText());
        lastEvent.pack();
    }

    private static boolean reportEnter(Label lastEvent, TextField source) {
        lastEvent.setText("Last Enter: " + source.getText());
        lastEvent.pack();
        return true;
    }
}
