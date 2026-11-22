package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import org.junit.jupiter.api.Test;

class LabelAbsolutePositionTest {
    private static final LabelStyle LOOK = LabelStyle.of(0xFFFFFFFF, 0x111111FF);
    private static final WindowStyle WINDOW_STYLE =
            WindowStyle.of(BoxStyle.SINGLE, LOOK, LOOK, 0x333333FF);
    private static final ScrollPaneStyle PANE_STYLE =
            ScrollPaneStyle.of(0x111111FF, 0x222222FF);
    private static final int LABEL_COLUMN_IN_CONTENT = 12;
    private static final int LABEL_ROW_IN_CONTENT = 1;
    private static final int PANE_COLUMN = 2;
    private static final int PANE_ROW = 1;

    private final UiSurface surface = UiSurface.ofSize(40, 20);
    private final UiRoot ui = UiRoot.of(surface);

    private static Label labelAt(int column, int row) {
        Label label = new Label("abcd", LOOK);
        label.setPosition(column, row);
        return label;
    }

    private static WidgetGroup groupAt(int column, int row, Widget... children) {
        WidgetGroup group = new WidgetGroup();
        group.setPosition(column, row);
        for (Widget child : children) {
            group.addChild(child);
        }
        return group;
    }

    private static final class ScrolledLabel {
        private final Label label = labelAt(LABEL_COLUMN_IN_CONTENT, LABEL_ROW_IN_CONTENT);
        private final ScrollPane pane;

        ScrolledLabel(UiRoot ui) {
            Label filler = new Label("", LOOK);
            filler.setSize(40, 8);
            pane = new ScrollPane(groupAt(0, 0, filler, label), PANE_STYLE);
            pane.setPosition(PANE_COLUMN, PANE_ROW);
            pane.setSize(10, 5);
            ui.getRoot().addChild(pane);
            ui.draw();
        }

        // The label's first UI cell lands on this column of the surface.
        void scrollLabelToCell(int surfaceCell) {
            pane.setScrollColumn(PANE_COLUMN + LABEL_COLUMN_IN_CONTENT - surfaceCell);
        }

        int lastViewCell() {
            return PANE_COLUMN + pane.getViewColumns() - 1;
        }
    }

    @Test
    void absolutePositionAddsUpTheParentChain() {
        Label label = labelAt(1, 1);
        WidgetGroup inner = groupAt(3, 2, label);
        groupAt(4, 1, inner);

        assertEquals(8, label.getAbsoluteX());
        assertEquals(4, label.getAbsoluteY());
        assertEquals(7, inner.getAbsoluteX());
        assertEquals(3, inner.getAbsoluteY());
    }

    @Test
    void widgetWithoutParentReturnsItsOwnPosition() {
        Label label = labelAt(5, 6);

        assertEquals(5, label.getAbsoluteX());
        assertEquals(6, label.getAbsoluteY());
    }

    @Test
    void windowBorderOffsetsTheContent() {
        Label label = labelAt(0, 0);
        Window window = new Window("", WINDOW_STYLE);
        window.setContent(label);
        window.setPosition(5, 3);
        window.pack();
        window.validate();

        assertEquals(6, label.getAbsoluteX());
        assertEquals(4, label.getAbsoluteY());
        assertEquals(12 + 2, label.getAbsoluteTextColumnOfLetter(2));
    }

    @Test
    void absoluteColumnIsTwiceTheCellPlusTheColumnInTheLabel() {
        Label label = labelAt(1, 0);
        groupAt(3, 2, label);

        assertEquals(8, label.getAbsoluteTextColumnOfLetter(0));
        assertEquals(10, label.getAbsoluteTextColumnOfLetter(2));
        assertEquals(Label.NOT_SHOWN,label.getAbsoluteTextColumnOfLetter(4));
        assertEquals(Label.NOT_SHOWN,label.getAbsoluteTextColumnOfLetter(-1));
    }

    @Test
    void realColumnMinusOneIsAShownLetter() {
        Label label = labelAt(-1, 0);

        assertEquals(-2, label.getAbsoluteTextColumnOfLetter(0));
        assertEquals(-1, label.getAbsoluteTextColumnOfLetter(1));
        assertTrue(label.getAbsoluteTextColumnOfLetter(1) != Label.NOT_SHOWN);
    }

    @Test
    void cutOffLettersHaveNoAbsoluteColumn() {
        Label label = labelAt(1, 0);
        label.setSize(1, 1);

        assertEquals(2, label.getAbsoluteTextColumnOfLetter(0));
        assertEquals(3, label.getAbsoluteTextColumnOfLetter(1));
        assertEquals(Label.NOT_SHOWN,label.getAbsoluteTextColumnOfLetter(2));
    }

    @Test
    void hiddenLabelHasNoAbsoluteColumn() {
        Label label = labelAt(1, 0);

        label.setVisible(false);

        assertEquals(Label.NOT_SHOWN,label.getAbsoluteTextColumnOfLetter(0));
    }

    @Test
    void hiddenAncestorHidesTheLetters() {
        Label label = labelAt(1, 0);
        WidgetGroup inner = groupAt(0, 0, label);
        WidgetGroup outer = groupAt(2, 2, inner);

        outer.setVisible(false);

        assertTrue(label.isVisible());
        assertEquals(Label.NOT_SHOWN,label.getAbsoluteTextColumnOfLetter(0));

        outer.setVisible(true);
        inner.setVisible(false);
        assertEquals(Label.NOT_SHOWN,label.getAbsoluteTextColumnOfLetter(3));

        inner.setVisible(true);
        assertEquals(6, label.getAbsoluteTextColumnOfLetter(0));
    }

    @Test
    void letterOutsideTheScrollViewHasNoColumn() {
        ScrolledLabel scrolled = new ScrolledLabel(ui);

        assertEquals(Label.NOT_SHOWN,scrolled.label.getAbsoluteTextColumnOfLetter(0));
        assertEquals(Label.NOT_SHOWN,scrolled.label.getAbsoluteTextColumnOfLetter(3));
    }

    @Test
    void scrolledLabelInsideTheViewKeepsItsColumns() {
        ScrolledLabel scrolled = new ScrolledLabel(ui);
        scrolled.scrollLabelToCell(PANE_COLUMN + 2);

        int firstCell = PANE_COLUMN + 2;
        assertEquals(2 * firstCell, scrolled.label.getAbsoluteTextColumnOfLetter(0));
        assertEquals(2 * firstCell + 3, scrolled.label.getAbsoluteTextColumnOfLetter(3));
    }

    @Test
    void bothHalvesOfTheLastViewCellCountAsInside() {
        ScrolledLabel scrolled = new ScrolledLabel(ui);
        scrolled.scrollLabelToCell(scrolled.lastViewCell());

        int cell = scrolled.lastViewCell();
        assertEquals(2 * cell, scrolled.label.getAbsoluteTextColumnOfLetter(0));
        assertEquals(2 * cell + 1, scrolled.label.getAbsoluteTextColumnOfLetter(1));
        assertEquals(Label.NOT_SHOWN,scrolled.label.getAbsoluteTextColumnOfLetter(2));
        assertEquals(Label.NOT_SHOWN,scrolled.label.getAbsoluteTextColumnOfLetter(3));
    }

    @Test
    void bothHalvesOfTheCellBeforeTheViewCountAsOutside() {
        ScrolledLabel scrolled = new ScrolledLabel(ui);
        scrolled.scrollLabelToCell(PANE_COLUMN - 1);

        assertEquals(Label.NOT_SHOWN,scrolled.label.getAbsoluteTextColumnOfLetter(0));
        assertEquals(Label.NOT_SHOWN,scrolled.label.getAbsoluteTextColumnOfLetter(1));
        assertEquals(2 * PANE_COLUMN, scrolled.label.getAbsoluteTextColumnOfLetter(2));
        assertEquals(2 * PANE_COLUMN + 1, scrolled.label.getAbsoluteTextColumnOfLetter(3));
    }

    @Test
    void letterScrolledAboveTheViewHasNoColumn() {
        ScrolledLabel scrolled = new ScrolledLabel(ui);
        scrolled.scrollLabelToCell(PANE_COLUMN);
        assertEquals(2 * PANE_COLUMN, scrolled.label.getAbsoluteTextColumnOfLetter(0));

        scrolled.pane.setScrollRow(LABEL_ROW_IN_CONTENT + 1);

        assertEquals(Label.NOT_SHOWN,scrolled.label.getAbsoluteTextColumnOfLetter(0));
    }

    @Test
    void textLayerIsNotAvailableBeforeAddTo() {
        IllegalStateException failure =
                assertThrows(IllegalStateException.class, surface::getTextLayer);

        assertTrue(failure.getMessage().contains("addTo"));
    }
}
