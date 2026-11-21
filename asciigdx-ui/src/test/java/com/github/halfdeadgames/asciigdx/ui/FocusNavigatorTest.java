package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class FocusNavigatorTest {
    private static final ButtonStyle STYLE = ButtonStyle.of(
            LabelStyle.of(0xFFFFFFFF, 0x000000FF));

    private final WidgetGroup root = new WidgetGroup();

    private TextButton buttonAt(WidgetGroup parent, int x, int y) {
        TextButton button = new TextButton("Go", STYLE);
        button.setPosition(x, y);
        parent.addChild(button);
        return button;
    }

    @Test
    void sequentialWrapsInBothDirections() {
        assertEquals(1, FocusNavigator.chooseSequential(3, 0, true));
        assertEquals(0, FocusNavigator.chooseSequential(3, 2, true));
        assertEquals(2, FocusNavigator.chooseSequential(3, 0, false));
        assertEquals(1, FocusNavigator.chooseSequential(3, 2, false));
    }

    @Test
    void sequentialWithNothingFocusedPicksFirstOrLast() {
        assertEquals(0, FocusNavigator.chooseSequential(3, -1, true));
        assertEquals(2, FocusNavigator.chooseSequential(3, -1, false));
    }

    @Test
    void noCandidatesGivesNoTarget() {
        assertNull(FocusNavigator.findTarget(root, null, FocusDirection.NEXT));
        assertNull(FocusNavigator.findTarget(root, null, FocusDirection.RIGHT));
    }

    @Test
    void treeOrderIsParentBeforeChildrenInChildOrder() {
        TextButton first = buttonAt(root, 0, 0);
        WidgetGroup group = new WidgetGroup();
        root.addChild(group);
        TextButton inner = buttonAt(group, 0, 2);
        TextButton last = buttonAt(root, 0, 4);

        assertSame(first, FocusNavigator.findTarget(root, null, FocusDirection.NEXT));
        assertSame(inner, FocusNavigator.findTarget(root, first, FocusDirection.NEXT));
        assertSame(last, FocusNavigator.findTarget(root, inner, FocusDirection.NEXT));
        assertSame(first, FocusNavigator.findTarget(root, last, FocusDirection.NEXT));
        assertSame(last, FocusNavigator.findTarget(root, null, FocusDirection.PREVIOUS));
    }

    @Test
    void disabledButtonsAreSkipped() {
        TextButton first = buttonAt(root, 0, 0);
        TextButton disabled = buttonAt(root, 0, 2);
        disabled.setDisabled(true);
        TextButton last = buttonAt(root, 0, 4);

        assertSame(last, FocusNavigator.findTarget(root, first, FocusDirection.NEXT));
    }

    @Test
    void spatialPicksNearestInEachDirection() {
        TextButton center = buttonAt(root, 10, 10);
        TextButton right = buttonAt(root, 20, 10);
        TextButton farRight = buttonAt(root, 30, 10);
        TextButton left = buttonAt(root, 2, 10);
        TextButton above = buttonAt(root, 10, 4);
        TextButton below = buttonAt(root, 10, 16);

        assertSame(right, FocusNavigator.findTarget(root, center, FocusDirection.RIGHT));
        assertSame(farRight, FocusNavigator.findTarget(root, right, FocusDirection.RIGHT));
        assertSame(left, FocusNavigator.findTarget(root, center, FocusDirection.LEFT));
        assertSame(above, FocusNavigator.findTarget(root, center, FocusDirection.UP));
        assertSame(below, FocusNavigator.findTarget(root, center, FocusDirection.DOWN));
    }

    @Test
    void spatialDoesNotWrapAndGivesNoTargetAtTheEdge() {
        TextButton left = buttonAt(root, 2, 10);
        buttonAt(root, 20, 10);

        assertNull(FocusNavigator.findTarget(root, left, FocusDirection.LEFT));
        assertNull(FocusNavigator.findTarget(root, left, FocusDirection.UP));
    }

    @Test
    void spatialPrefersTheAlignedCandidateOverASlightlyNearerOffsetOne() {
        TextButton from = buttonAt(root, 0, 10);
        buttonAt(root, 10, 2);
        TextButton aligned = buttonAt(root, 12, 10);

        assertSame(aligned, FocusNavigator.findTarget(root, from, FocusDirection.RIGHT));
    }

    @Test
    void spatialTieGoesToTheEarlierWidgetInTreeOrder() {
        TextButton from = buttonAt(root, 10, 10);
        TextButton earlier = buttonAt(root, 20, 6);
        buttonAt(root, 20, 14);

        assertSame(earlier, FocusNavigator.findTarget(root, from, FocusDirection.RIGHT));
    }

    @Test
    void spatialUsesAbsolutePositionsOfNestedWidgets() {
        WidgetGroup group = new WidgetGroup();
        group.setPosition(30, 10);
        root.addChild(group);
        TextButton nested = buttonAt(group, 0, 0);
        TextButton from = buttonAt(root, 10, 10);

        assertSame(nested, FocusNavigator.findTarget(root, from, FocusDirection.RIGHT));
    }

    @Test
    void spatialWithNothingFocusedPicksTheFirstCandidate() {
        TextButton first = buttonAt(root, 20, 10);
        buttonAt(root, 2, 2);

        assertSame(first, FocusNavigator.findTarget(root, null, FocusDirection.DOWN));
    }

    @Test
    void spatialFromAFocusedWidgetThatIsNotACandidateUsesItsRectangle() {
        TextButton disabled = buttonAt(root, 10, 10);
        disabled.setDisabled(true);
        TextButton right = buttonAt(root, 20, 10);
        buttonAt(root, 2, 10);

        assertSame(right, FocusNavigator.findTarget(root, disabled, FocusDirection.RIGHT));
    }

    @Test
    void chooseSpatialIgnoresTheSkippedIndex() {
        var bounds = List.of(new FocusNavigator.Bounds(0, 0, 4, 1),
                new FocusNavigator.Bounds(10, 0, 14, 1));

        assertEquals(1, FocusNavigator.chooseSpatial(bounds, bounds.get(0), 0,
                FocusDirection.RIGHT));
        assertEquals(-1, FocusNavigator.chooseSpatial(bounds, bounds.get(1), 1,
                FocusDirection.RIGHT));
    }
}
