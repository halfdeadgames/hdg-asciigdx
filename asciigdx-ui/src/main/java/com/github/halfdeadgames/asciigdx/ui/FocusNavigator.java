package com.github.halfdeadgames.asciigdx.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses which widget of a tree receives keyboard focus next. Candidates are the visible widgets
 * (with every ancestor visible) for which {@link Widget#isInFocusOrder()} is {@code true}, in
 * depth-first tree order (a parent before its children, children in child order).
 *
 * <p>Spatial rule: a candidate lies in a direction when its whole rectangle is beyond the
 * focused rectangle's edge on that side, for example for RIGHT when the candidate's left edge is
 * at or past the focused right edge. Among those, the lowest score wins: the gap along the axis
 * times {@value #GAP_WEIGHT} plus {@value #PERPENDICULAR_WEIGHT} times the doubled distance
 * between the rectangle centers on the other axis. A tie goes to the candidate earlier in tree
 * order.
 */
final class FocusNavigator {

    private static final int NO_INDEX = -1;
    // The cross-axis offset is doubled to keep half cells whole, so the gap is doubled to match.
    static final int GAP_WEIGHT = 2;
    static final int PERPENDICULAR_WEIGHT = 2;

    private FocusNavigator() {
    }

    /** A rectangle in surface cells; right and bottom are exclusive. */
    record Bounds(int left, int top, int right, int bottom) {
    }

    private static final class Scan {
        private final Widget focused;
        private final List<Widget> candidates = new ArrayList<>();
        private final List<Bounds> bounds = new ArrayList<>();
        private int candidatesBeforeFocused = NO_INDEX;

        private Scan(Widget focused) {
            this.focused = focused;
        }
    }

    /**
     * Finds the widget that should get focus.
     *
     * @return the target, which may be {@code focused} itself, or {@code null} when there is no
     *         candidate, no candidate lies in a spatial direction, or a spatial direction starts
     *         from a focused widget outside the tree
     */
    static Widget findTarget(WidgetGroup root, Widget focused, FocusDirection direction) {
        Scan scan = new Scan(focused);
        collect(root, 0, 0, scan);
        if (scan.candidates.isEmpty()) {
            return null;
        }
        int currentIndex = scan.candidates.indexOf(focused);
        int targetIndex = switch (direction) {
            case NEXT -> chooseSequentialFor(scan, currentIndex, true);
            case PREVIOUS -> chooseSequentialFor(scan, currentIndex, false);
            default -> chooseSpatialFor(root, scan, currentIndex, direction);
        };
        return targetIndex == NO_INDEX ? null : scan.candidates.get(targetIndex);
    }

    // A focused widget in the tree that is not a candidate continues from its tree position.
    private static int chooseSequentialFor(Scan scan, int currentIndex, boolean forward) {
        int count = scan.candidates.size();
        if (currentIndex == NO_INDEX && scan.candidatesBeforeFocused != NO_INDEX) {
            return chooseAround(count, scan.candidatesBeforeFocused, forward);
        }
        return chooseSequential(count, currentIndex, forward);
    }

    private static int chooseSpatialFor(WidgetGroup root, Scan scan, int currentIndex,
            FocusDirection direction) {
        if (currentIndex != NO_INDEX) {
            return chooseSpatial(scan.bounds, scan.bounds.get(currentIndex), currentIndex,
                    direction);
        }
        if (scan.focused == null) {
            return 0;
        }
        if (isInTree(root, scan.focused)) {
            return chooseSpatial(scan.bounds, absoluteBounds(scan.focused), NO_INDEX, direction);
        }
        return NO_INDEX;
    }

    /**
     * Picks the next index in a wrapping order.
     *
     * @param count number of candidates; at least 1
     * @param currentIndex index of the focused candidate, or -1 for none
     * @param forward {@code true} for the next, {@code false} for the previous
     * @return the chosen index
     */
    static int chooseSequential(int count, int currentIndex, boolean forward) {
        if (currentIndex == NO_INDEX) {
            return forward ? 0 : count - 1;
        }
        return forward ? (currentIndex + 1) % count : (currentIndex - 1 + count) % count;
    }

    /**
     * Picks the next index from a position between candidates.
     *
     * @param count number of candidates; at least 1
     * @param candidatesBefore number of candidates that come before the focused widget in tree
     *         order
     * @param forward {@code true} for the next, {@code false} for the previous
     * @return the chosen index
     */
    static int chooseAround(int count, int candidatesBefore, boolean forward) {
        return forward ? candidatesBefore % count : (candidatesBefore - 1 + count) % count;
    }

    /**
     * Picks the nearest candidate in a spatial direction.
     *
     * @param candidates rectangles of the candidates in tree order
     * @param from rectangle of the focused widget
     * @param skippedIndex index of the focused widget in {@code candidates}, or -1
     * @param direction one of UP, DOWN, LEFT, or RIGHT
     * @return the chosen index, or -1 when none lies in that direction
     */
    static int chooseSpatial(List<Bounds> candidates, Bounds from, int skippedIndex,
            FocusDirection direction) {
        int bestIndex = NO_INDEX;
        long bestScore = Long.MAX_VALUE;
        for (int i = 0; i < candidates.size(); i++) {
            long score = i == skippedIndex ? NO_INDEX : score(from, candidates.get(i), direction);
            if (score >= 0 && score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    // Returns -1 when the candidate does not lie in the direction.
    private static long score(Bounds from, Bounds candidate, FocusDirection direction) {
        int gap;
        int centerOffset;
        switch (direction) {
            case RIGHT -> {
                gap = candidate.left() - from.right();
                centerOffset = verticalCenterOffset(from, candidate);
            }
            case LEFT -> {
                gap = from.left() - candidate.right();
                centerOffset = verticalCenterOffset(from, candidate);
            }
            case DOWN -> {
                gap = candidate.top() - from.bottom();
                centerOffset = horizontalCenterOffset(from, candidate);
            }
            default -> {
                gap = from.top() - candidate.bottom();
                centerOffset = horizontalCenterOffset(from, candidate);
            }
        }
        if (gap < 0) {
            return NO_INDEX;
        }
        return (long) GAP_WEIGHT * gap + (long) PERPENDICULAR_WEIGHT * centerOffset;
    }

    // Doubled, so that half-cell centers stay whole numbers.
    private static int verticalCenterOffset(Bounds from, Bounds candidate) {
        return Math.abs(candidate.top() + candidate.bottom() - from.top() - from.bottom());
    }

    private static int horizontalCenterOffset(Bounds from, Bounds candidate) {
        return Math.abs(candidate.left() + candidate.right() - from.left() - from.right());
    }

    private static void collect(Widget node, int parentLeft, int parentTop, Scan scan) {
        if (!node.isVisible()) {
            return;
        }
        int left = parentLeft + node.getX();
        int top = parentTop + node.getY();
        if (node == scan.focused) {
            scan.candidatesBeforeFocused = scan.candidates.size();
        }
        if (node.isInFocusOrder()) {
            scan.candidates.add(node);
            scan.bounds.add(
                    new Bounds(left, top, left + node.getWidth(), top + node.getHeight()));
        }
        if (node instanceof WidgetGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChild(i), left, top, scan);
            }
        }
    }

    private static Bounds absoluteBounds(Widget widget) {
        int left = 0;
        int top = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            left += current.getX();
            top += current.getY();
        }
        return new Bounds(left, top, left + widget.getWidth(), top + widget.getHeight());
    }

    private static boolean isInTree(WidgetGroup root, Widget widget) {
        Widget top = widget;
        while (top.getParent() != null) {
            top = top.getParent();
        }
        return top == root;
    }
}
