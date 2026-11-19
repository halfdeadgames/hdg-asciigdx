package com.github.halfdeadgames.asciigdx.ui;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A widget that holds other widgets and places them freely. Children keep the position they were
 * given, relative to this group's top-left corner, and draw in the order they were added, so later
 * children appear on top. Children are not clipped to the group's size.
 *
 * <p>This class does not override {@link Widget#layout()}: children keep the positions they are
 * given. Changes to children reach the group through the layout and redraw marks (see
 * {@link Widget#invalidate()}). Subclasses that arrange children override
 * {@link Widget#layout()} and place them there.
 */
public class WidgetGroup extends Widget {

    private final ArrayList<Widget> children = new ArrayList<>();

    /** Creates an empty group at position (0, 0), with size 0 x 0 and no parent. */
    public WidgetGroup() {
    }

    /**
     * Adds a child at the end of the draw order, on top of earlier children. Calls
     * {@link #invalidate()} on this group.
     *
     * @param child widget to add; must not be {@code null}
     * @throws NullPointerException if {@code child} is {@code null}
     * @throws IllegalArgumentException if {@code child} already has a parent (including this
     *         group), or is this group or one of its ancestors
     */
    public void addChild(Widget child) {
        Objects.requireNonNull(child, "child");
        if (child.getParent() != null) {
            throw new IllegalArgumentException("Widget already has a parent");
        }
        if (isSelfOrAncestor(child)) {
            throw new IllegalArgumentException("A group cannot contain itself or its ancestors");
        }
        children.add(child);
        child.attachTo(this);
        invalidate();
    }

    /**
     * Removes a child. The child's parent becomes {@code null}. Calls {@link #invalidate()} on
     * this group.
     *
     * @param child widget to remove; must not be {@code null}
     * @throws NullPointerException if {@code child} is {@code null}
     * @throws IllegalArgumentException if {@code child} is not a child of this group
     */
    public void removeChild(Widget child) {
        Objects.requireNonNull(child, "child");
        if (child.getParent() != this) {
            throw new IllegalArgumentException("Widget is not a child of this group");
        }
        children.remove(child);
        child.attachTo(null);
        invalidate();
    }

    /**
     * Returns the number of children.
     *
     * @return the child count, zero or greater
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Returns a child by its place in the draw order.
     *
     * @param index position, from 0 (drawn first) to {@code getChildCount() - 1}
     * @return the child at that position
     * @throws IndexOutOfBoundsException if {@code index} is outside that range
     */
    public Widget getChild(int index) {
        return children.get(index);
    }

    /**
     * Returns the distance from this group's left edge to the farthest right edge of any child,
     * or 0 when there are no children or no child reaches past this group's left edge.
     *
     * @return the width in UI cells, zero or greater
     */
    @Override
    public int getPreferredWidth() {
        int widest = 0;
        for (int i = 0; i < children.size(); i++) {
            Widget child = children.get(i);
            widest = Math.max(widest, child.getX() + child.getWidth());
        }
        return widest;
    }

    /**
     * Returns the distance from this group's top edge to the lowest bottom edge of any child, or
     * 0 when there are no children or no child reaches below this group's top edge.
     *
     * @return the height in UI cells, zero or greater
     */
    @Override
    public int getPreferredHeight() {
        int tallest = 0;
        for (int i = 0; i < children.size(); i++) {
            Widget child = children.get(i);
            tallest = Math.max(tallest, child.getY() + child.getHeight());
        }
        return tallest;
    }

    /**
     * Returns the deepest widget at a point. Children are checked topmost first (the last added,
     * which draws last), each one even if the point lies outside this group's bounds, since
     * children are not clipped. If no child is hit, the group itself is returned when the point
     * lies in its bounds.
     *
     * @param column column in this group's local coordinates, in UI cells; any value
     * @param row row in this group's local coordinates, in UI cells; any value
     * @return the deepest widget hit, or {@code null} when nothing is
     */
    @Override
    public Widget hit(int column, int row) {
        Widget childHit = hitChildren(column, row);
        return childHit != null ? childHit : super.hit(column, row);
    }

    Widget hitChildren(int column, int row) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            Widget childHit = child.hit(column - child.getX(), row - child.getY());
            if (childHit != null) {
                return childHit;
            }
        }
        return null;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        for (int i = 0; i < children.size(); i++) {
            Widget child = children.get(i);
            child.drawContent(target, left + child.getX(), top + child.getY());
        }
    }

    @Override
    void validateChildren() {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).validate();
        }
    }

    @Override
    void clearRedrawNeeded() {
        super.clearRedrawNeeded();
        for (int i = 0; i < children.size(); i++) {
            children.get(i).clearRedrawNeeded();
        }
    }

    private boolean isSelfOrAncestor(Widget candidate) {
        for (Widget current = this; current != null; current = current.getParent()) {
            if (current == candidate) {
                return true;
            }
        }
        return false;
    }
}
