package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.ProgressBar;
import com.github.halfdeadgames.asciigdx.ui.ProgressBarStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Slider;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.ValueRange;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/** Every progress bar style of the skin, horizontal and vertical, driven by a slider. */
final class ProgressBarExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"progressBar"};
    private static final ValueRange PERCENT = ValueRange.of(0f, 100f, 1f);
    private static final int HORIZONTAL_LENGTH = 30;
    private static final int VERTICAL_LENGTH = 8;
    private static final float HALF = 50f;
    private static final float FULL = 100f;

    @Override
    public String id() {
        return "progress-bar";
    }

    @Override
    public String title() {
        return "ProgressBar";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, int columns, int rows) {
        DemoSheet sheet = new DemoSheet(skin);
        List<ProgressBar> liveBars = new ArrayList<>();
        addLiveControls(sheet, skin, liveBars);
        for (String styleName : STYLE_NAMES) {
            addStyleDemos(sheet, styleName, skin.getProgressBarStyle(styleName), liveBars);
        }
        return sheet.build();
    }

    private void addLiveControls(DemoSheet sheet, Skin skin, List<ProgressBar> liveBars) {
        Label valueLabel = new Label("Value: 0", skin.getLabelStyle("text"));
        Slider driver = Slider.horizontal(PERCENT, HORIZONTAL_LENGTH,
                skin.getSliderStyle("slider"));
        driver.addChangeListener(changed -> {
            showValue(valueLabel, changed.getValue());
            setAll(liveBars, changed.getValue());
        });
        sheet.addHeading("Live controls");
        sheet.addDemo("live: value of the live bars", driver);
        sheet.addDemo("live: slider value", valueLabel);
    }

    private void addStyleDemos(DemoSheet sheet, String styleName, ProgressBarStyle style,
            List<ProgressBar> liveBars) {
        sheet.addHeading(styleName);
        ProgressBar live = ProgressBar.horizontal(PERCENT, HORIZONTAL_LENGTH, style);
        liveBars.add(live);
        sheet.addDemo(styleName + " (horizontal, live)", live);
        sheet.addDemo(styleName + " (horizontal, 0%)",
                createFixed(ProgressBar.horizontal(PERCENT, HORIZONTAL_LENGTH, style), 0f));
        sheet.addDemo(styleName + " (horizontal, 50%)",
                createFixed(ProgressBar.horizontal(PERCENT, HORIZONTAL_LENGTH, style), HALF));
        sheet.addDemo(styleName + " (horizontal, 100%)",
                createFixed(ProgressBar.horizontal(PERCENT, HORIZONTAL_LENGTH, style), FULL));
        ProgressBar liveVertical = ProgressBar.vertical(PERCENT, VERTICAL_LENGTH, style);
        liveBars.add(liveVertical);
        sheet.addDemo(styleName + " (vertical, live)", liveVertical);
        sheet.addDemo(styleName + " (vertical, 50%)",
                createFixed(ProgressBar.vertical(PERCENT, VERTICAL_LENGTH, style), HALF));
    }

    private static ProgressBar createFixed(ProgressBar bar, float value) {
        bar.setValue(value);
        return bar;
    }

    private static void setAll(List<ProgressBar> bars, float value) {
        for (int i = 0; i < bars.size(); i++) {
            bars.get(i).setValue(value);
        }
    }

    private static void showValue(Label valueLabel, float value) {
        valueLabel.setText("Value: " + Math.round(value));
        valueLabel.pack();
    }
}
