package com.github.halfdeadgames.asciigdx.ui;

/**
 * Finds the text field that {@link UiRoot#setAutoFocusTextFields(boolean)} focuses: the first
 * shown, enabled field in reading order, which is rows from top to bottom and, within a row,
 * columns from left to right. The position compared is the field's top row, then its left column,
 * in cells on the surface. A tie goes to the field earlier in tree order. One instance is reused
 * for every search, so a search does not allocate.
 */
final class TextFieldAutoFocus {

    private TextField bestField;
    private int bestTop;
    private int bestLeft;

    /** Returns the first field in reading order below a root group, or {@code null} for none. */
    TextField findFirst(WidgetGroup root) {
        bestField = null;
        visit(root, 0, 0);
        TextField found = bestField;
        bestField = null;
        return found;
    }

    private void visit(Widget node, int parentLeft, int parentTop) {
        if (!node.isVisible()) {
            return;
        }
        int left = parentLeft + node.getX();
        int top = parentTop + node.getY();
        if (node instanceof TextField field && field.isFocusable()) {
            considerCandidate(field, left, top);
        }
        if (node instanceof WidgetGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                visit(group.getChild(i), left, top);
            }
        }
    }

    private void considerCandidate(TextField field, int left, int top) {
        boolean earlier = bestField == null || top < bestTop
                || top == bestTop && left < bestLeft;
        if (earlier) {
            bestField = field;
            bestTop = top;
            bestLeft = left;
        }
    }
}
