package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.List;

/** Every button style of the skin, normal and disabled, with a click counter. */
final class TextButtonExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"button"};
    private static final String LABEL_STYLE = "text";

    @Override
    public String id() {
        return "text-button";
    }

    @Override
    public String title() {
        return "TextButton";
    }

    @Override
    public List<String> skinSections() {
        return List.of(SkinSchema.BUTTONS, SkinSchema.LABELS);
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        ClickCounter counter = new ClickCounter(
                new Label("Clicks: 0", skin.getLabelStyle(LABEL_STYLE)));
        sheet.addHeading("Live controls");
        sheet.addDisabledToggle();
        sheet.addDemo("live: clicks on the buttons", counter.label);
        for (String styleName : STYLE_NAMES) {
            addStyleDemos(sheet, styleName, skin.getButtonStyle(styleName), counter);
        }
        return sheet.build();
    }

    private void addStyleDemos(DemoSheet sheet, String styleName, ButtonStyle style,
            ClickCounter counter) {
        sheet.addHeading(styleName);
        TextButton normal = new TextButton("Click me", style);
        normal.addClickListener(clicked -> counter.increment());
        sheet.trackDisabled(normal::setDisabled);
        sheet.addDemo(styleName + " (normal)", normal);
        TextButton disabled = new TextButton("Not available", style);
        disabled.setDisabled(true);
        sheet.addDemo(styleName + " (disabled)", disabled);
    }

    private static final class ClickCounter {
        private final Label label;
        private int clicks;

        ClickCounter(Label label) {
            this.label = label;
        }

        void increment() {
            clicks++;
            label.setText("Clicks: " + clicks);
            label.pack();
        }
    }
}
