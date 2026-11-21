package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.FocusDirection;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.ProgressBar;
import com.github.halfdeadgames.asciigdx.ui.ProgressBarStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Slider;
import com.github.halfdeadgames.asciigdx.ui.SliderStyle;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;
import com.github.halfdeadgames.asciigdx.ui.ValueRange;
import java.util.List;
import java.util.Locale;

/**
 * Interactive check of progress bars and sliders, horizontal and vertical. The progress bars show several fill levels, including partial eighths. The sliders
 * react to the arrow keys along their axis, Home, End, and the pointer (press to jump, drag to
 * move); one slider is disabled. A label shows the last change made by the user.
 */
class BarsPage implements UiPage {
    private static final int ROOT_X = 4;
    private static final int ROOT_Y = 2;
    private static final int LAST_CHANGE_Y = 0;
    private static final int BARS_CAPTION_Y = 2;
    private static final int BARS_Y = 4;
    private static final int SLIDERS_CAPTION_Y = 16;
    private static final int SLIDERS_Y = 18;
    // One empty row between stacked widgets, so neighbors do not merge into one shape.
    private static final int ROW_STEP = 2;
    private static final int HORIZONTAL_LENGTH = 20;
    private static final int SLIDER_LENGTH = 21;
    private static final int VERTICAL_SQUARE_LENGTH = 8;
    private static final int VERTICALS_X = 24;
    private static final List<Float> LEVELS = List.of(0f, 33f, 57f, 100f);
    private static final ValueRange PERCENT = ValueRange.of(0f, 100f, 1f);
    private static final ValueRange SLIDER_RANGE = ValueRange.of(0f, 10f, 1f);

    private final ProgressBarStyle squareBar;
    private final SliderStyle squareSlider;
    private final LabelStyle caption;
    private final UiRoot uiRoot;

    BarsPage(UiRoot uiRoot, Skin skin) {
        this.uiRoot = uiRoot;
        squareBar = skin.getProgressBarStyle("progressBar");
        squareSlider = skin.getSliderStyle("slider");
        caption = skin.getLabelStyle("text");
    }

    @Override
    public String id() {
        return "bars";
    }

    @Override
    public String title() {
        return "Bars";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        WidgetGroup content = new WidgetGroup();
        WidgetGroup root = new WidgetGroup();
        root.setPosition(ROOT_X, ROOT_Y);
        Label lastChange = new Label("Last change: none", caption);
        lastChange.setPosition(0, LAST_CHANGE_Y);
        root.addChild(lastChange);
        addProgressBars(root);
        addSliders(root, lastChange);
        content.addChild(root);
        return content;
    }

    /**
     * Gives the first slider keyboard focus through keyboard navigation, so the focused look shows
     * even when the last input was not a key. Must not be called while an event is delivered.
     */
    void focusFirstSliderVisibly() {
        uiRoot.setKeyboardFocus(null);
        uiRoot.moveFocus(FocusDirection.NEXT);
    }

    private void addProgressBars(WidgetGroup root) {
        addCaption(root, "Progress bars: 0, 33, 57, and 100 percent",
                BARS_CAPTION_Y);
        for (int i = 0; i < LEVELS.size(); i++) {
            float level = LEVELS.get(i);
            addBar(root, ProgressBar.horizontal(PERCENT, HORIZONTAL_LENGTH, squareBar), 0,
                    BARS_Y + i * ROW_STEP, level);
            addBar(root, ProgressBar.vertical(PERCENT, VERTICAL_SQUARE_LENGTH, squareBar),
                    VERTICALS_X + i * ROW_STEP, BARS_Y, level);
        }
    }

    private void addSliders(WidgetGroup root, Label lastChange) {
        addCaption(root, "Sliders: arrows, Home, End, or press and drag", SLIDERS_CAPTION_Y);
        addSlider(root, lastChange, Slider.horizontal(SLIDER_RANGE, SLIDER_LENGTH, squareSlider),
                0, SLIDERS_Y, 5f);
        Slider disabled = Slider.horizontal(SLIDER_RANGE, SLIDER_LENGTH, squareSlider);
        disabled.setDisabled(true);
        addSlider(root, lastChange, disabled, 0, SLIDERS_Y + ROW_STEP, 3f);
        addSlider(root, lastChange, Slider.vertical(SLIDER_RANGE, VERTICAL_SQUARE_LENGTH,
                squareSlider), VERTICALS_X, SLIDERS_Y, 7f);
    }

    private static void addBar(WidgetGroup root, ProgressBar bar, int x, int y, float value) {
        bar.setPosition(x, y);
        bar.setValue(value);
        root.addChild(bar);
    }

    private static void addSlider(WidgetGroup root, Label lastChange, Slider slider, int x,
            int y, float value) {
        slider.setPosition(x, y);
        slider.setValue(value);
        slider.addChangeListener(source -> showChange(lastChange, source));
        root.addChild(slider);
    }

    private void addCaption(WidgetGroup root, String text, int y) {
        Label captionLabel = new Label(text, caption);
        captionLabel.setPosition(0, y);
        root.addChild(captionLabel);
    }

    private static void showChange(Label lastChange, Slider source) {
        String axis = source.getOrientation().name().toLowerCase(Locale.ROOT);
        lastChange.setText("Last change: " + axis + " slider = " + source.getValue());
        lastChange.pack();
    }
}
