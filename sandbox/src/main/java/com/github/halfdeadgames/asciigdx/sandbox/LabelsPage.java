package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.UiFont;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Static check of text labels in two fonts: styles, text cut off at a set width, nesting, setText
 * with and without pack, square text drawn over tall text, and tall text on an odd row.
 */
class LabelsPage implements UiPage {
    private static final int ROOT_X = 4;
    private static final int ROOT_Y = 2;
    private static final int PANEL_X = 3;
    private static final int PANEL_WIDTH = 46;
    private static final int PANEL_HEIGHT = 2;
    private static final int CUT_OFF_WIDTH = 8;
    private static final int CAPTION_X = 24;
    private static final int BLACK = 0x000000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFD700FF;
    private static final int CYAN = 0x00FFFFFF;
    private static final int PANEL_BLUE = 0x203070FF;
    private static final int CLEAR = TerminalGrid.TRANSPARENT_RGBA;
    private static final LabelStyle TALL_GOLD = LabelStyle.of(UiFont.TALL, GOLD, BLACK);
    private static final LabelStyle TALL_PLAIN = LabelStyle.of(UiFont.TALL, WHITE, BLACK);
    private static final LabelStyle TALL_GOLD_TEXT = LabelStyle.of(UiFont.TALL, GOLD, CLEAR);
    private static final LabelStyle PLAIN = LabelStyle.of(UiFont.SQUARE, WHITE, BLACK);
    private static final LabelStyle ON_CYAN = LabelStyle.of(UiFont.SQUARE, BLACK, CYAN);
    private static final LabelStyle GOLD_TEXT = LabelStyle.of(UiFont.SQUARE, GOLD, CLEAR);

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

    private static Widget createPanel() {
        Widget panel = new BlueBackground();
        panel.setPosition(PANEL_X, ROOT_Y + 6);
        panel.setSize(PANEL_WIDTH, PANEL_HEIGHT);
        return panel;
    }

    private static WidgetGroup buildRoot() {
        WidgetGroup root = new WidgetGroup();
        root.setPosition(ROOT_X, ROOT_Y);
        addLabel(root, "UI labels", TALL_GOLD, 0, 0);
        addLabel(root, "White on black", TALL_PLAIN, 0, 2);
        addLabel(root, "Black on cyan", ON_CYAN, 0, 4);
        addLabel(root, "Gold on transparent", TALL_GOLD_TEXT, 0, 6);
        addLabel(root, "keeps the blue panel", GOLD_TEXT, CAPTION_X, 6);
        addCutOffLabel(root, 9);
        addNestedGroup(root, 11);
        addTextChanges(root, 15);
        addSquareOverTall(root, 20);
        addOddRowTall(root, 23);
        return root;
    }

    private static void addCutOffLabel(WidgetGroup root, int y) {
        Label cutOff = addLabel(root, "This text is too long", ON_CYAN, 0, y);
        cutOff.setSize(CUT_OFF_WIDTH, 1);
        addLabel(root, "Cut off at " + CUT_OFF_WIDTH + " cells", PLAIN, 12, y);
    }

    private static void addNestedGroup(WidgetGroup root, int y) {
        WidgetGroup nested = new WidgetGroup();
        nested.setPosition(0, y);
        addLabel(nested, "Inside the group", PLAIN, 2, 0);
        addLabel(nested, "Offsets add up", ON_CYAN, 4, 1);
        root.addChild(nested);
        addLabel(root, "This group is nested", PLAIN, CAPTION_X, y);
    }

    private static void addTextChanges(WidgetGroup root, int y) {
        Label notPacked = addLabel(root, "short", ON_CYAN, 0, y);
        notPacked.setText("much longer text");
        addLabel(root, "setText without pack keeps the old size", PLAIN, 20, y);

        Label packed = addLabel(root, "short", ON_CYAN, 0, y + 2);
        packed.setText("much longer text");
        packed.pack();
        addLabel(root, "setText with pack fits the new text", PLAIN, 20, y + 2);
    }

    private static void addSquareOverTall(WidgetGroup root, int y) {
        addLabel(root, "Tall text under square", TALL_PLAIN, 0, y);
        addLabel(root, "Square", ON_CYAN, 0, y);
        addLabel(root, "Square drawn over tall", PLAIN, CAPTION_X, y);
    }

    private static void addOddRowTall(WidgetGroup root, int y) {
        addLabel(root, "Tall text", TALL_PLAIN, 0, y);
        addLabel(root, "Tall text on an odd row snaps up", PLAIN, CAPTION_X, y);
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
            target.fillBackground(left, top, getWidth(), getHeight(), PANEL_BLUE);
        }
    }
}
