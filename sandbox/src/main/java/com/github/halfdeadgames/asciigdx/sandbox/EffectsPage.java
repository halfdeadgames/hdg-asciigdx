package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelEffectZones;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;
import java.util.ArrayList;
import java.util.List;

/** Check of effect tags in label text: wave, bounce, and a color tag around an effect tag. */
class EffectsPage implements UiPage {
    private static final String WAVE_TEXT =
            "A [effect:wave phase=columns:6]wavy line of text[/effect] moves";
    private static final String BOUNCE_TEXT = "Some letters [effect:bounce]bounce[/effect] up";
    private static final String NESTED_TEXT =
            "[color:hotPink]Pink [effect:wave phase=columns:6]and waving[/effect][/color] text";
    private static final int ROOT_X = 4;
    private static final int ROOT_Y = 2;
    private static final int ROWS_BETWEEN_LABELS = 2;

    private final Skin skin;
    private final List<LabelEffectZones> effectZones = new ArrayList<>();

    EffectsPage(Skin skin) {
        this.skin = skin;
    }

    @Override
    public String id() {
        return "effects";
    }

    @Override
    public String title() {
        return "Effects";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        effectZones.clear();
        WidgetGroup root = new WidgetGroup();
        root.setPosition(ROOT_X, ROOT_Y);
        addEffectLabel(root, WAVE_TEXT, 0);
        addEffectLabel(root, BOUNCE_TEXT, ROWS_BETWEEN_LABELS);
        addEffectLabel(root, NESTED_TEXT, 2 * ROWS_BETWEEN_LABELS);
        return root;
    }

    @Override
    public void pageHidden() {
        effectZones.clear();
    }

    @Override
    public void applyEffects(Layer textLayer, float seconds) {
        for (int i = 0; i < effectZones.size(); i++) {
            effectZones.get(i).apply(textLayer, seconds);
        }
    }

    private void addEffectLabel(WidgetGroup root, String markupText, int y) {
        Label label = new Label(markupText, skin.getLabelStyle("text"));
        label.setColorNames(skin.getColorNames());
        label.setEffectNames(skin.getEffectRegistry());
        label.pack();
        label.setPosition(0, y);
        root.addChild(label);
        effectZones.add(LabelEffectZones.of(label));
    }
}
