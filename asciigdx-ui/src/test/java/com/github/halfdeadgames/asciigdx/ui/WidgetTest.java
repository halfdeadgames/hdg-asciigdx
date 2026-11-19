package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WidgetTest {

    private static final LabelStyle SQUARE = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x000000FF);
    private static final LabelStyle TALL = LabelStyle.of(UiFont.TALL, 0xFFFFFFFF, 0x000000FF);

    private static class Probe extends Widget {
        int layoutCalls;

        @Override
        public int getPreferredWidth() {
            return 0;
        }

        @Override
        public int getPreferredHeight() {
            return 0;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
        }

        @Override
        protected void layout() {
            layoutCalls++;
        }
    }

    private static class ResizingGroup extends WidgetGroup {
        Widget child;

        @Override
        protected void layout() {
            child.setSize(child.getWidth() + 1, 1);
        }
    }

    private static void clearAll(Widget widget) {
        widget.validate();
        widget.clearRedrawNeeded();
    }

    @Test
    void newWidgetNeedsLayoutAndRedraw() {
        Probe probe = new Probe();

        assertTrue(probe.isLayoutNeeded());
        assertTrue(probe.isRedrawNeeded());
    }

    @Test
    void invalidateMarksAncestors() {
        WidgetGroup root = new WidgetGroup();
        WidgetGroup middle = new WidgetGroup();
        Probe leaf = new Probe();
        root.addChild(middle);
        middle.addChild(leaf);
        clearAll(root);

        leaf.invalidate();

        assertTrue(leaf.isLayoutNeeded() && leaf.isRedrawNeeded());
        assertTrue(middle.isLayoutNeeded() && middle.isRedrawNeeded());
        assertTrue(root.isLayoutNeeded() && root.isRedrawNeeded());
    }

    @Test
    void requestRedrawMarksAncestorsWithoutLayout() {
        WidgetGroup root = new WidgetGroup();
        Probe leaf = new Probe();
        root.addChild(leaf);
        clearAll(root);

        leaf.requestRedraw();

        assertTrue(leaf.isRedrawNeeded());
        assertTrue(root.isRedrawNeeded());
        assertFalse(leaf.isLayoutNeeded());
        assertFalse(root.isLayoutNeeded());
    }

    @Test
    void validateRunsLayoutOnceAndClearsMark() {
        Probe probe = new Probe();

        probe.validate();
        probe.validate();

        assertEquals(1, probe.layoutCalls);
        assertFalse(probe.isLayoutNeeded());
    }

    @Test
    void validateRecursesIntoChildren() {
        WidgetGroup root = new WidgetGroup();
        Probe first = new Probe();
        Probe second = new Probe();
        root.addChild(first);
        root.addChild(second);

        root.validate();

        assertEquals(1, first.layoutCalls);
        assertEquals(1, second.layoutCalls);
        assertFalse(first.isLayoutNeeded());
    }

    @Test
    void sizeChangeInsideLayoutDoesNotMarkGroupAgain() {
        ResizingGroup group = new ResizingGroup();
        group.child = new Probe();
        group.addChild(group.child);
        clearAll(group);
        group.invalidate();

        group.validate();

        assertFalse(group.isLayoutNeeded());
        assertTrue(group.isRedrawNeeded());
        assertFalse(group.child.isLayoutNeeded());
        int layoutsRunByClearAllAndValidate = 2;
        assertEquals(layoutsRunByClearAllAndValidate, group.child.getWidth());
    }

    @Test
    void sizeChangeInsideLayoutStillMarksRedrawOfAncestorsAbove() {
        WidgetGroup root = new WidgetGroup();
        ResizingGroup group = new ResizingGroup();
        group.child = new Probe();
        group.addChild(group.child);
        root.addChild(group);
        clearAll(root);
        group.invalidate();
        root.validate();
        root.clearRedrawNeeded();
        group.invalidate();

        root.validate();

        assertFalse(root.isLayoutNeeded());
        assertTrue(root.isRedrawNeeded());
    }

    @Test
    void equalValuesLeaveMarksUnchanged() {
        WidgetGroup root = new WidgetGroup();
        Label label = new Label("text", SQUARE);
        label.setPosition(2, 3);
        root.addChild(label);
        clearAll(root);

        label.setPosition(2, 3);
        label.setSize(label.getWidth(), label.getHeight());
        label.setText("text");
        label.setStyle(LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x000000FF));

        assertFalse(label.isLayoutNeeded() || label.isRedrawNeeded());
        assertFalse(root.isLayoutNeeded() || root.isRedrawNeeded());
    }

    @Test
    void changedValuesSetMarks() {
        WidgetGroup root = new WidgetGroup();
        Label label = new Label("text", SQUARE);
        root.addChild(label);
        clearAll(root);

        label.setText("other");
        assertTrue(root.isLayoutNeeded());

        clearAll(root);
        label.setStyle(TALL);
        assertTrue(label.isLayoutNeeded());
    }

    @Test
    void setPositionMarksSelfForRedrawAndAncestorsForLayout() {
        WidgetGroup root = new WidgetGroup();
        Label label = new Label("text", SQUARE);
        root.addChild(label);
        clearAll(root);

        label.setPosition(1, 1);

        assertTrue(label.isRedrawNeeded());
        assertFalse(label.isLayoutNeeded());
        assertTrue(root.isRedrawNeeded());
        assertTrue(root.isLayoutNeeded());
    }

    @Test
    void groupPlacingChildrenInLayoutIsNotMarkedAgain() {
        WidgetGroup group = new WidgetGroup() {
            @Override
            protected void layout() {
                getChild(0).setPosition(getChild(0).getX() + 1, 0);
            }
        };
        group.addChild(new Probe());
        clearAll(group);
        group.invalidate();

        group.validate();

        assertFalse(group.isLayoutNeeded());
        assertTrue(group.isRedrawNeeded());
        int layoutsRunByClearAllAndValidate = 2;
        assertEquals(layoutsRunByClearAllAndValidate, group.getChild(0).getX());
    }

    @Test
    void groupResizingItselfInLayoutIsNotLayoutNeededAfterValidate() {
        WidgetGroup group = new WidgetGroup() {
            @Override
            protected void layout() {
                setSize(5, 5);
            }
        };

        group.validate();

        assertFalse(group.isLayoutNeeded());
        assertTrue(group.isRedrawNeeded());
        assertEquals(5, group.getWidth());
    }

    @Test
    void setStyleWithSameFontOnlyRequestsRedraw() {
        Label label = new Label("text", SQUARE);
        clearAll(label);

        label.setStyle(LabelStyle.of(UiFont.SQUARE, 0xFF0000FF, 0x000000FF));

        assertTrue(label.isRedrawNeeded());
        assertFalse(label.isLayoutNeeded());
    }

    @Test
    void addAndRemoveChildInvalidateGroup() {
        WidgetGroup group = new WidgetGroup();
        Probe child = new Probe();
        clearAll(group);

        group.addChild(child);
        assertTrue(group.isLayoutNeeded());

        clearAll(group);
        group.removeChild(child);
        assertTrue(group.isLayoutNeeded() && group.isRedrawNeeded());
    }

    @Test
    void clearRedrawNeededClearsSubtree() {
        WidgetGroup root = new WidgetGroup();
        WidgetGroup nested = new WidgetGroup();
        Probe leaf = new Probe();
        root.addChild(nested);
        nested.addChild(leaf);

        root.clearRedrawNeeded();

        assertFalse(root.isRedrawNeeded());
        assertFalse(nested.isRedrawNeeded());
        assertFalse(leaf.isRedrawNeeded());
        assertTrue(leaf.isLayoutNeeded());
    }

    @Test
    void labelRowSnapFollowsFont() {
        assertEquals(2, new Label("a", TALL).getRowSnap());
        assertEquals(1, new Label("a", SQUARE).getRowSnap());
        assertEquals(1, new Probe().getRowSnap());
    }
}
