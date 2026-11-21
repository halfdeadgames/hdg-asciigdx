package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class HitTest {
    private static final LabelStyle STYLE = LabelStyle.of(1, 2);

    private static Label label(String text, int x, int y) {
        Label label = new Label(text, STYLE);
        label.setPosition(x, y);
        return label;
    }

    @Test
    void plainWidgetHitsInsideItsBoundsOnly() {
        Label label = label("abcde", 4, 4);

        assertSame(label, label.hit(0, 0));
        assertSame(label, label.hit(2, 0));
        assertNull(label.hit(3, 0));
        assertNull(label.hit(0, 1));
        assertNull(label.hit(-1, 0));
    }

    @Test
    void groupChecksChildrenTopmostFirst() {
        WidgetGroup group = new WidgetGroup();
        Label below = label("abcdefgh", 0, 0);
        Label above = label("ab", 1, 0);
        group.addChild(below);
        group.addChild(above);

        assertSame(above, group.hit(1, 0));
        assertSame(below, group.hit(0, 0));
        assertSame(below, group.hit(3, 0));
    }

    @Test
    void groupHitsChildrenOutsideItsOwnBounds() {
        WidgetGroup group = new WidgetGroup();
        group.setSize(2, 2);
        Label outside = label("abcd", 10, 10);
        group.addChild(outside);

        assertSame(outside, group.hit(11, 10));
    }

    @Test
    void groupHitsItselfInsideItsBoundsWhenNoChildIsHit() {
        WidgetGroup group = new WidgetGroup();
        group.setSize(5, 5);
        group.addChild(label("a", 0, 0));

        assertSame(group, group.hit(3, 3));
        assertNull(group.hit(5, 5));
    }

    @Test
    void nestedGroupsUseLocalCoordinates() {
        WidgetGroup outer = new WidgetGroup();
        WidgetGroup inner = new WidgetGroup();
        inner.setPosition(5, 5);
        Label leaf = label("ab", 2, 1);
        inner.addChild(leaf);
        outer.addChild(inner);

        assertSame(leaf, outer.hit(7, 6));
        assertNull(outer.hit(2, 1));
    }

    @Test
    void focusableIsFalseByDefault() {
        assertFalse(label("a", 0, 0).isFocusable());
    }
}
