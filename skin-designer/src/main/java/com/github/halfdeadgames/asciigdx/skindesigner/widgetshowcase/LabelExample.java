package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.text.TextReveal;
import com.github.halfdeadgames.asciigdx.ui.HorizontalAlignment;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/** Every label style of the skin, with a text field that changes the text of all of them. */
final class LabelExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {
        "text", "windowText", "title", "highlight", "accent", "overlayText", "tooltip"
    };
    private static final int ALIGNED_LABEL_COLUMNS = 30;
    private static final String SAMPLE_TEXT = "The quick brown fox";
    private static final String COLORED_WORD_TEXT =
            "The [color:hotPink]quick[color:default] brown fox";
    private static final String HIGHLIGHTED_WORD_TEXT =
            "Highlight [bg:deepBlue]this word[bg:default] in a phrase";
    private static final String WAVE_TEXT =
            "A [effect:wave phase=columns:6]wavy line of text[/effect] moves";
    private static final String BOUNCE_TEXT = "Some letters [effect:bounce]bounce[/effect] up";
    private static final String NESTED_TEXT =
            "[color:hotPink]Pink [effect:wave phase=columns:6]and waving[/effect][/color] text";
    private static final String TEXT_FIELD_STYLE = "default";
    private static final int TEXT_FIELD_COLUMNS = 24;
    private static final String REVEAL_TEXT =
            "Hello...[wait:0.6] the [color:hotPink]door[/color] creaks open.";
    private static final float REVEAL_LETTERS_PER_SECOND = 20f;
    private static final float REVEAL_HOLD_SECONDS = 2f;
    private static final float REVEAL_NOT_STARTED = -1f;

    private final List<LabelEffectZones> effectZones = new ArrayList<>();
    private Label revealLabel;
    private TextReveal reveal;
    private float revealStartSeconds = REVEAL_NOT_STARTED;

    @Override
    public String id() {
        return "label";
    }

    @Override
    public String title() {
        return "Label";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        effectZones.clear();
        revealLabel = null;
        reveal = null;
        revealStartSeconds = REVEAL_NOT_STARTED;
        addLiveField(sheet, skin, new ArrayList<>());
        return sheet.build();
    }

    @Override
    public void exampleHidden() {
        effectZones.clear();
    }

    @Override
    public void applyEffects(Layer textLayer, float seconds) {
        for (int i = 0; i < effectZones.size(); i++) {
            effectZones.get(i).apply(textLayer, seconds);
        }
        applyTypingReveal(seconds);
    }

    private void applyTypingReveal(float seconds) {
        if (reveal == null) {
            return;
        }
        if (revealStartSeconds == REVEAL_NOT_STARTED) {
            revealStartSeconds = seconds;
        }
        float cycleSeconds = reveal.getTotalSeconds() + REVEAL_HOLD_SECONDS;
        float elapsedSeconds = (seconds - revealStartSeconds) % cycleSeconds;
        revealLabel.setShownLetterCount(reveal.getShownLetterCount(elapsedSeconds));
    }

    private void addLiveField(DemoSheet sheet, Skin skin, List<Label> demoLabels) {
        TextField field = new TextField(skin.getTextFieldStyle(TEXT_FIELD_STYLE),
                TEXT_FIELD_COLUMNS);
        field.setPlaceholderText("Type to change every label");
        field.addChangeListener(changed -> showText(demoLabels, changed.getText()));
        sheet.addHeading("Live control");
        sheet.addDemo("live: text of all labels", field);
        sheet.addHeading("Label styles");
        for (String styleName : STYLE_NAMES) {
            Label label = new Label(SAMPLE_TEXT, skin.getLabelStyle(styleName));
            demoLabels.add(label);
            sheet.addDemo(styleName, label);
        }
        addAlignmentDemos(sheet, skin);
        addMarkupDemos(sheet, skin);
        addEffectDemos(sheet, skin);
        addRevealDemo(sheet, skin);
    }

    private void addRevealDemo(DemoSheet sheet, Skin skin) {
        sheet.addHeading("Typing reveal");
        Label label = new Label(REVEAL_TEXT, skin.getLabelStyle("text"));
        label.setColorNames(skin.getColorNames());
        label.pack();
        reveal = label.createReveal(REVEAL_LETTERS_PER_SECOND);
        revealLabel = label;
        sheet.addDemo("typing", label);
    }

    private void addEffectDemos(DemoSheet sheet, Skin skin) {
        sheet.addHeading("Effect tags");
        sheet.addDemo("wave", createEffectLabel(skin, WAVE_TEXT));
        sheet.addDemo("bounce", createEffectLabel(skin, BOUNCE_TEXT));
        sheet.addDemo("color and effect", createEffectLabel(skin, NESTED_TEXT));
    }

    private Label createEffectLabel(Skin skin, String markupText) {
        Label label = new Label(markupText, skin.getLabelStyle("text"));
        label.setColorNames(skin.getColorNames());
        label.setEffectNames(skin.getEffectRegistry());
        label.pack();
        effectZones.add(new LabelEffectZones(label));
        return label;
    }

    private void addMarkupDemos(DemoSheet sheet, Skin skin) {
        sheet.addHeading("Markup tags");
        sheet.addDemo("colored word", createMarkupLabel(skin, COLORED_WORD_TEXT));
        sheet.addDemo("highlighted word", createMarkupLabel(skin, HIGHLIGHTED_WORD_TEXT));
    }

    private Label createMarkupLabel(Skin skin, String markupText) {
        Label label = new Label(markupText, skin.getLabelStyle("text"));
        label.setColorNames(skin.getColorNames());
        label.pack();
        return label;
    }

    private void addAlignmentDemos(DemoSheet sheet, Skin skin) {
        sheet.addHeading("Text alignment in a wider label");
        for (HorizontalAlignment alignment : HorizontalAlignment.values()) {
            Label label = new Label("Aligned " + alignment, skin.getLabelStyle("highlight"));
            label.setSize(ALIGNED_LABEL_COLUMNS, 1);
            label.setTextAlignment(alignment);
            sheet.addDemo(alignment.name(), label);
        }
    }

    private static void showText(List<Label> demoLabels, String typedText) {
        String text = typedText.isEmpty() ? SAMPLE_TEXT : typedText;
        for (int i = 0; i < demoLabels.size(); i++) {
            Label label = demoLabels.get(i);
            label.setText(text);
            label.pack();
        }
    }
}
