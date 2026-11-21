package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.CheckBox;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Slider;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.Tooltip;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.ValueRange;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/** Every tooltip label style of the skin on buttons, a check box, and a disabled button. */
final class TooltipExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"tooltip"};
    private static final ValueRange DELAY_RANGE = ValueRange.of(0f, 2f, 0.1f);
    private static final int DELAY_SLIDER_COLUMNS = 20;

    private final List<Tooltip> attachedTooltips = new ArrayList<>();

    @Override
    public String id() {
        return "tooltip";
    }

    @Override
    public String title() {
        return "Tooltip";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        attachedTooltips.clear();
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        addLiveControls(sheet, skin);
        for (String styleName : STYLE_NAMES) {
            addStyleDemos(sheet, skin, uiRoot, styleName, skin.getLabelStyle(styleName));
        }
        return sheet.build();
    }

    @Override
    public void exampleHidden() {
        for (int i = 0; i < attachedTooltips.size(); i++) {
            attachedTooltips.get(i).detach();
        }
        attachedTooltips.clear();
    }

    private void addLiveControls(DemoSheet sheet, Skin skin) {
        Label delayLabel = new Label("Delay: " + Tooltip.DEFAULT_DELAY_SECONDS + " s",
                skin.getLabelStyle("text"));
        Slider delay = Slider.horizontal(DELAY_RANGE, DELAY_SLIDER_COLUMNS,
                skin.getSliderStyle("slider"));
        delay.setValue(Tooltip.DEFAULT_DELAY_SECONDS);
        delay.addChangeListener(changed -> {
            float seconds = changed.getValue();
            for (int i = 0; i < attachedTooltips.size(); i++) {
                attachedTooltips.get(i).setDelaySeconds(seconds);
            }
            delayLabel.setText("Delay: " + String.format("%.1f", seconds) + " s");
            delayLabel.pack();
        });
        sheet.addHeading("Live controls");
        sheet.addDemo("live: delay before a hint", delay);
        sheet.addDemo("live: delay", delayLabel);
    }

    private void addStyleDemos(DemoSheet sheet, Skin skin, UiRoot uiRoot, String styleName,
            LabelStyle hintStyle) {
        sheet.addHeading(styleName);
        TextButton button = new TextButton("Hover me", skin.getButtonStyle("button"));
        attach(uiRoot, button, "A short hint", hintStyle);
        sheet.addDemo(styleName + " (on a button)", button);
        CheckBox box = new CheckBox("Hover me too", skin.getCheckBoxStyle("checkBox"));
        attach(uiRoot, box, "A hint on a check box", hintStyle);
        sheet.addDemo(styleName + " (on a check box)", box);
        TextButton disabled = new TextButton("Disabled", skin.getButtonStyle("button"));
        disabled.setDisabled(true);
        attach(uiRoot, disabled, "A disabled widget still shows its hint", hintStyle);
        sheet.addDemo(styleName + " (on a disabled button)", disabled);
        TextButton wide = new TextButton("Long hint", skin.getButtonStyle("button"));
        attach(uiRoot, wide, "A longer hint that needs many more cells than the short one",
                hintStyle);
        sheet.addDemo(styleName + " (long text)", wide);
    }

    private void attach(UiRoot uiRoot, Widget target, String text, LabelStyle style) {
        attachedTooltips.add(Tooltip.attachText(uiRoot, target, text, style));
    }
}
