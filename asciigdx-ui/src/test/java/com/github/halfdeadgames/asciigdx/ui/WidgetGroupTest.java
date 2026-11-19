package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class WidgetGroupTest {
    private static final LabelStyle STYLE = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x000000FF);

    private final UiSurface surface = UiSurface.ofSize(30, 10);
    private final TerminalGrid grid = surface.getSquareGrid();

    @Test
    void nestedGroupOffsetsAddUp() {
        WidgetGroup outer = new WidgetGroup();
        outer.setPosition(2, 1);
        WidgetGroup inner = new WidgetGroup();
        inner.setPosition(3, 2);
        Label label = new Label("X", STYLE);
        label.setPosition(1, 1);
        outer.addChild(inner);
        inner.addChild(label);
        outer.draw(surface);
        assertEquals('X', grid.getCodePoint(6, 4));
    }

    @Test
    void drawingChildDrawsAtAbsolutePosition() {
        WidgetGroup group = new WidgetGroup();
        group.setPosition(5, 5);
        Label label = new Label("Y", STYLE);
        label.setPosition(2, 1);
        group.addChild(label);
        label.draw(surface);
        assertEquals('Y', grid.getCodePoint(7, 6));
    }

    @Test
    void laterChildrenDrawOnTop() {
        WidgetGroup group = new WidgetGroup();
        group.addChild(new Label("A", STYLE));
        group.addChild(new Label("B", STYLE));
        group.draw(surface);
        assertEquals('B', grid.getCodePoint(0, 0));
    }

    @Test
    void preferredSizeIsBoundingBoxOfChildEdges() {
        WidgetGroup group = new WidgetGroup();
        assertEquals(0, group.getPreferredWidth());
        Label label = new Label("abcd", STYLE);
        label.setPosition(3, 2);
        group.addChild(label);
        assertEquals(7, group.getPreferredWidth());
        assertEquals(3, group.getPreferredHeight());
        group.pack();
        assertEquals(7, group.getWidth());
    }

    @Test
    void groupPartlyOffGridDoesNotThrow() {
        WidgetGroup group = new WidgetGroup();
        group.setPosition(-5, -5);
        group.addChild(new Label("hello", STYLE));
        group.draw(surface);
        group.setPosition(28, 9);
        group.draw(surface);
        assertEquals('h', grid.getCodePoint(28, 9));
    }

    @Test
    void addChildRejectsAlreadyParented() {
        WidgetGroup first = new WidgetGroup();
        WidgetGroup second = new WidgetGroup();
        Label label = new Label("a", STYLE);
        first.addChild(label);
        assertThrows(IllegalArgumentException.class, () -> second.addChild(label));
        assertThrows(IllegalArgumentException.class, () -> first.addChild(label));
    }

    @Test
    void addChildRejectsSelfAndAncestors() {
        WidgetGroup root = new WidgetGroup();
        WidgetGroup middle = new WidgetGroup();
        WidgetGroup leaf = new WidgetGroup();
        root.addChild(middle);
        middle.addChild(leaf);
        assertThrows(IllegalArgumentException.class, () -> leaf.addChild(leaf));
        assertThrows(IllegalArgumentException.class, () -> leaf.addChild(root));
        assertThrows(IllegalArgumentException.class, () -> leaf.addChild(middle));
    }

    @Test
    void addChildRejectsNull() {
        assertThrows(NullPointerException.class, () -> new WidgetGroup().addChild(null));
    }

    @Test
    void removeChildClearsParent() {
        WidgetGroup group = new WidgetGroup();
        Label label = new Label("a", STYLE);
        group.addChild(label);
        assertSame(group, label.getParent());
        group.removeChild(label);
        assertNull(label.getParent());
        assertEquals(0, group.getChildCount());
    }

    @Test
    void removeChildRejectsNonChildAndNull() {
        WidgetGroup group = new WidgetGroup();
        Label stranger = new Label("a", STYLE);
        assertThrows(IllegalArgumentException.class, () -> group.removeChild(stranger));
        assertThrows(NullPointerException.class, () -> group.removeChild(null));
    }

    @Test
    void getChildChecksBounds() {
        WidgetGroup group = new WidgetGroup();
        Label label = new Label("a", STYLE);
        group.addChild(label);
        assertSame(label, group.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> group.getChild(1));
        assertThrows(IndexOutOfBoundsException.class, () -> group.getChild(-1));
    }

    @Test
    void setSizeRejectsNegative() {
        WidgetGroup group = new WidgetGroup();
        assertThrows(IllegalArgumentException.class, () -> group.setSize(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> group.setSize(0, -1));
    }
}
