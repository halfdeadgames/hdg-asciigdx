package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.badlogic.gdx.utils.IntArray;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * EXPERIMENT: draws the boundary ring around every popup, window, and dialog. It sits last in the
 * popup layer, finds the rectangles by scanning the window layer and the popup layer, and never
 * takes input. A zero-size group, such as the one that holds a tooltip, is looked through, and a
 * child as large as the surface, such as the press catcher of an open menu, is skipped.
 */
public final class PopupBoundaryOverlay extends Widget {
    private static final int VALUES_PER_RECTANGLE = 4;

    private final WidgetGroup popupLayer;
    private final WidgetGroup windowLayer;
    private final BoundaryState state;
    private final int surfaceColumns;
    private final int surfaceRows;
    private final IntArray previousRectangles = new IntArray();
    private final IntArray scannedRectangles = new IntArray();

    private PopupBoundaryOverlay(UiRoot uiRoot, BoundaryState state, int surfaceColumns,
            int surfaceRows) {
        this.popupLayer = uiRoot.getPopupLayer();
        this.windowLayer = uiRoot.getWindowLayer();
        this.state = state;
        this.surfaceColumns = surfaceColumns;
        this.surfaceRows = surfaceRows;
    }

    /**
     * Adds an overlay to the popup layer of the root and keeps it the last child, so it draws
     * above every popup.
     */
    public static void installOn(UiRoot uiRoot, BoundaryState state, int surfaceColumns,
            int surfaceRows) {
        PopupBoundaryOverlay overlay =
                new PopupBoundaryOverlay(uiRoot, state, surfaceColumns, surfaceRows);
        overlay.popupLayer.addChild(overlay);
        state.setOverlay(overlay);
        uiRoot.addUpdateListener(deltaSeconds -> overlay.followPopups());
    }

    @Override
    public int getPreferredWidth() {
        return 0;
    }

    @Override
    public int getPreferredHeight() {
        return 0;
    }

    @Override
    public Widget hit(int column, int row) {
        return null;
    }

    private void followPopups() {
        keepLastInPopupLayer();
        scanRectangles();
        if (scannedRectangles.equals(previousRectangles)) {
            return;
        }
        previousRectangles.clear();
        previousRectangles.addAll(scannedRectangles);
        requestRedraw();
    }

    private void keepLastInPopupLayer() {
        if (popupLayer.getChild(popupLayer.getChildCount() - 1) == this) {
            return;
        }
        popupLayer.removeChild(this);
        popupLayer.addChild(this);
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        if (!state.isShown()) {
            return;
        }
        scanRectangles();
        int ringRgba = state.getRgba();
        for (int i = 0; i < scannedRectangles.size; i += VALUES_PER_RECTANGLE) {
            BoundaryRing.draw(target,
                    scannedRectangles.get(i) - BoundaryRing.RING_CELLS,
                    scannedRectangles.get(i + 1) - BoundaryRing.RING_CELLS,
                    scannedRectangles.get(i + 2) + 2 * BoundaryRing.RING_CELLS,
                    scannedRectangles.get(i + 3) + 2 * BoundaryRing.RING_CELLS,
                    ringRgba);
        }
    }

    // Stores left, top, width, and height of each popup rectangle in surface cells.
    private void scanRectangles() {
        scannedRectangles.clear();
        scanLayer(windowLayer);
        scanLayer(popupLayer);
    }

    private void scanLayer(WidgetGroup layer) {
        if (!layer.isVisible()) {
            return;
        }
        scanChildren(layer, absoluteLeft(layer), absoluteTop(layer));
    }

    private void scanChildren(WidgetGroup group, int groupLeft, int groupTop) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Widget child = group.getChild(i);
            if (child == this || !child.isVisible()) {
                continue;
            }
            int left = groupLeft + child.getX();
            int top = groupTop + child.getY();
            if (isLookThroughGroup(child)) {
                scanChildren((WidgetGroup) child, left, top);
                continue;
            }
            if (isPopupRectangle(child)) {
                scannedRectangles.add(left);
                scannedRectangles.add(top);
                scannedRectangles.add(child.getWidth());
                scannedRectangles.add(child.getHeight());
            }
        }
    }

    private static boolean isLookThroughGroup(Widget child) {
        return child instanceof WidgetGroup group && group.getWidth() == 0
                && group.getHeight() == 0 && group.getChildCount() > 0;
    }

    private boolean isPopupRectangle(Widget child) {
        if (child.getWidth() <= 0 || child.getHeight() <= 0) {
            return false;
        }
        boolean coversSurface = child.getWidth() == surfaceColumns
                && child.getHeight() == surfaceRows;
        return !coversSurface;
    }

    private static int absoluteLeft(Widget widget) {
        int left = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            left += current.getX();
        }
        return left;
    }

    private static int absoluteTop(Widget widget) {
        int top = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            top += current.getY();
        }
        return top;
    }
}
