package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.HorizontalAlignment;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Static check of text labels: styles, text cut off at a set width, nesting, setText with and
 * without pack, and left, center, and right text alignment.
 */
class LabelsPage implements UiPage {
    private static final int ROOT_X = 4;
    private static final int ROOT_Y = 2;
    private static final int PANEL_X = 3;
    private static final int PANEL_WIDTH = 46;
    private static final int PANEL_HEIGHT = 1;
    private static final int ALIGNED_WIDTH = 24;
    private static final int CUT_OFF_WIDTH = 8;
    private static final int CAPTION_X = 24;

    private final int panelRgba;
    private final LabelStyle titleStyle;
    private final LabelStyle plain;
    private final LabelStyle highlightStyle;
    private final LabelStyle overlayStyle;

    LabelsPage(Skin skin) {
        panelRgba = skin.getRgba("deepBlue");
        titleStyle = skin.getLabelStyle("title");
        plain = skin.getLabelStyle("text");
        highlightStyle = skin.getLabelStyle("highlight");
        overlayStyle = skin.getLabelStyle("overlayText");
    }

    @Override
    public String id() {
        return "labels";
    }

    @Override
    public String title() {
        return "Labels";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        WidgetGroup content = new WidgetGroup();
        content.addChild(createPanel());
        content.addChild(buildRoot());
        return content;
    }

    private Widget createPanel() {
        Widget panel = new BlueBackground(panelRgba);
        panel.setPosition(PANEL_X, ROOT_Y + 6);
        // Aligned with the overlay label, which sits at the root's x offset.
        panel.setSize(PANEL_WIDTH, PANEL_HEIGHT);
        return panel;
    }

    private WidgetGroup buildRoot() {
        WidgetGroup root = new WidgetGroup();
        root.setPosition(ROOT_X, ROOT_Y);
        addLabel(root, "UI labels", titleStyle, 0, 0);
        addLabel(root, "White on black", plain, 0, 2);
        addLabel(root, "Black on cyan", highlightStyle, 0, 4);
        addLabel(root, "Gold on transparent", overlayStyle, 0, 6);
        addLabel(root, "keeps the blue panel", overlayStyle, CAPTION_X, 6);
        addCutOffLabel(root, 8);
        addNestedGroup(root, 10);
        addTextChanges(root, 13);
        addAlignedLabels(root, 18);
        return root;
    }

    private void addAlignedLabels(WidgetGroup root, int y) {
        addAlignedLabel(root, "Left", HorizontalAlignment.LEFT, y);
        addAlignedLabel(root, "Center", HorizontalAlignment.CENTER, y + 1);
        addAlignedLabel(root, "Right", HorizontalAlignment.RIGHT, y + 2);
        addLabel(root, "Text alignment in a wider label", plain, CAPTION_X + 4, y + 1);
    }

    private void addAlignedLabel(WidgetGroup root, String text, HorizontalAlignment alignment,
            int y) {
        Label label = addLabel(root, text, highlightStyle, 0, y);
        label.setSize(ALIGNED_WIDTH, 1);
        label.setTextAlignment(alignment);
    }

    private void addCutOffLabel(WidgetGroup root, int y) {
        Label cutOff = addLabel(root, "This text is too long", highlightStyle, 0, y);
        cutOff.setSize(CUT_OFF_WIDTH, 1);
        addLabel(root, "Cut off at " + CUT_OFF_WIDTH + " cells", plain, 12, y);
    }

    private void addNestedGroup(WidgetGroup root, int y) {
        WidgetGroup nested = new WidgetGroup();
        nested.setPosition(0, y);
        addLabel(nested, "Inside the group", plain, 2, 0);
        addLabel(nested, "Offsets add up", highlightStyle, 4, 1);
        root.addChild(nested);
        addLabel(root, "This group is nested", plain, CAPTION_X, y);
    }

    private void addTextChanges(WidgetGroup root, int y) {
        Label notPacked = addLabel(root, "short", highlightStyle, 0, y);
        notPacked.setText("much longer text");
        addLabel(root, "setText without pack keeps the old size", plain, 20, y);

        Label packed = addLabel(root, "short", highlightStyle, 0, y + 2);
        packed.setText("much longer text");
        packed.pack();
        addLabel(root, "setText with pack fits the new text", plain, 20, y + 2);
    }

    private static Label addLabel(WidgetGroup parent, String text, LabelStyle style, int x,
            int y) {
        Label label = new Label(text, style);
        label.setPosition(x, y);
        parent.addChild(label);
        return label;
    }

    /** Fills its rectangle with a blue background, to show that clear labels keep it. */
    private static final class BlueBackground extends Widget {
        private final int fillRgba;

        BlueBackground(int fillRgba) {
            this.fillRgba = fillRgba;
        }

        @Override
        public int getPreferredWidth() {
            return PANEL_WIDTH;
        }

        @Override
        public int getPreferredHeight() {
            return PANEL_HEIGHT;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
            target.fillBackground(left, top, getWidth(), getHeight(), fillRgba);
        }
    }
}
