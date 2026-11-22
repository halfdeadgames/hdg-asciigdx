package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import com.github.halfdeadgames.asciigdx.ui.Slider;
import com.github.halfdeadgames.asciigdx.ui.SliderStyle;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.ValueRange;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.List;

/** Every slider style of the skin, horizontal and vertical, normal and disabled. */
final class SliderExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"slider"};
    private static final ValueRange PERCENT = ValueRange.of(0f, 100f, 1f);
    private static final int HORIZONTAL_LENGTH = 30;
    private static final int VERTICAL_LENGTH = 8;
    private static final float DISABLED_VALUE = 40f;

    @Override
    public String id() {
        return "slider";
    }

    @Override
    public String title() {
        return "Slider";
    }

    @Override
    public List<String> skinSections() {
        return List.of(SkinSchema.SLIDERS);
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        Label lastValue = new Label("Last value: none", skin.getLabelStyle("text"));
        sheet.addHeading("Live controls");
        sheet.addDisabledToggle();
        sheet.addDemo("live: last slider moved", lastValue);
        for (String styleName : STYLE_NAMES) {
            addStyleDemos(sheet, styleName, skin.getSliderStyle(styleName), lastValue);
        }
        return sheet.build();
    }

    private void addStyleDemos(DemoSheet sheet, String styleName, SliderStyle style,
            Label lastValue) {
        sheet.addHeading(styleName);
        Slider horizontal = Slider.horizontal(PERCENT, HORIZONTAL_LENGTH, style);
        addLive(sheet, styleName + " (horizontal)", horizontal, lastValue);
        sheet.addDemo(styleName + " (horizontal, disabled)",
                createDisabled(Slider.horizontal(PERCENT, HORIZONTAL_LENGTH, style)));
        Slider vertical = Slider.vertical(PERCENT, VERTICAL_LENGTH, style);
        addLive(sheet, styleName + " (vertical)", vertical, lastValue);
        sheet.addDemo(styleName + " (vertical, disabled)",
                createDisabled(Slider.vertical(PERCENT, VERTICAL_LENGTH, style)));
    }

    private static void addLive(DemoSheet sheet, String caption, Slider slider,
            Label lastValue) {
        slider.addChangeListener(changed -> showValue(lastValue, caption, changed.getValue()));
        sheet.trackDisabled(slider::setDisabled);
        sheet.addDemo(caption, slider);
    }

    private static Slider createDisabled(Slider slider) {
        slider.setValue(DISABLED_VALUE);
        slider.setDisabled(true);
        return slider;
    }

    private static void showValue(Label lastValue, String caption, float value) {
        lastValue.setText(caption + " = " + Math.round(value));
        lastValue.pack();
    }
}
