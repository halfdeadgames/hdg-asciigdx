package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;

/**
 * The open list of a {@link SelectBox}: a press catcher and a scroll pane of rows in the popup
 * layer of the root, the highlight, and the keys that move it. The parts are built when the list
 * is first opened and reused.
 */
final class SelectBoxList {

    private static final int NO_INDEX = -1;

    private final SelectBox<?> box;
    private final UiRoot uiRoot;
    private final UiUpdateListener watcher = this::watchWhileOpen;
    private SelectBoxRows rows;
    private ScrollPane pane;
    private SelectBoxPressCatcher catcher;
    private boolean open;

    SelectBoxList(SelectBox<?> box, UiRoot uiRoot) {
        this.box = box;
        this.uiRoot = uiRoot;
    }

    boolean isOpen() {
        return open;
    }

    void open() {
        if (open || !box.canShowList()) {
            return;
        }
        createPartsIfMissing();
        placeParts();
        WidgetGroup popupLayer = uiRoot.getPopupLayer();
        catcher.setPosition(-popupLayer.getX(), -popupLayer.getY());
        popupLayer.addChild(catcher);
        popupLayer.addChild(pane);
        pane.validate();
        pane.setScrollRow(0);
        showHighlight(rows.getHighlightedIndex());
        uiRoot.setKeyboardFocus(catcher);
        uiRoot.addUpdateListener(watcher);
        open = true;
        box.requestRedraw();
    }

    void close() {
        if (!open) {
            return;
        }
        open = false;
        uiRoot.removeUpdateListener(watcher);
        WidgetGroup popupLayer = uiRoot.getPopupLayer();
        popupLayer.removeChild(pane);
        popupLayer.removeChild(catcher);
        rows.cancelPress();
        box.handleListClosed();
    }

    void pick(int index) {
        close();
        box.pickItem(index);
    }

    boolean handleKey(int keycode) {
        return switch (keycode) {
            case Input.Keys.ESCAPE -> {
                close();
                yield true;
            }
            case Input.Keys.UP -> moveHighlight(-1);
            case Input.Keys.DOWN -> moveHighlight(1);
            case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER -> pickHighlighted();
            default -> false;
        };
    }

    boolean handleTyped(char typed) {
        if (!Character.isLetterOrDigit(typed)) {
            return false;
        }
        int count = box.getItemCount();
        int start = rows.getHighlightedIndex();
        for (int step = 1; step <= count; step++) {
            int candidate = Math.floorMod(start + step, count);
            if (startsWithCharacter(box.getItemText(candidate), typed)) {
                showHighlight(candidate);
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithCharacter(String text, char typed) {
        return !text.isEmpty()
                && Character.toLowerCase(text.codePointAt(0)) == Character.toLowerCase(typed);
    }

    private boolean moveHighlight(int direction) {
        int highlighted = rows.getHighlightedIndex();
        int moved = Math.max(0, Math.min(box.getItemCount() - 1, highlighted + direction));
        showHighlight(moved);
        return true;
    }

    private boolean pickHighlighted() {
        int highlighted = rows.getHighlightedIndex();
        if (highlighted != NO_INDEX) {
            pick(highlighted);
        }
        return true;
    }

    private void showHighlight(int index) {
        rows.highlight(index);
        if (index == NO_INDEX) {
            return;
        }
        int firstShownRow = pane.getScrollRow();
        int shownRows = pane.getHeight();
        if (index < firstShownRow) {
            pane.setScrollRow(index);
        } else if (index >= firstShownRow + shownRows) {
            pane.setScrollRow(index - shownRows + 1);
        }
    }

    // Closes the list when its box cannot show it anymore, and keeps the focus on the list, which
    // a press on the scroll bar would otherwise take.
    private void watchWhileOpen(float deltaSeconds) {
        if (!open) {
            return;
        }
        if (!box.canShowList()) {
            close();
        } else if (uiRoot.getKeyboardFocus() != catcher) {
            uiRoot.setKeyboardFocus(catcher);
        }
    }

    private void createPartsIfMissing() {
        if (pane != null) {
            return;
        }
        rows = new SelectBoxRows(box, this);
        pane = new ScrollPane(rows, box.getStyle().getScrollPane());
        catcher = new SelectBoxPressCatcher(this, uiRoot.getSurfaceColumns(),
                uiRoot.getSurfaceRows());
    }

    private void placeParts() {
        int itemCount = box.getItemCount();
        int wantedRows = Math.min(itemCount, box.getMaxVisibleRows());
        SelectBoxPlacement.Span vertical = SelectBoxPlacement.placeVertically(
                box.getAbsoluteY(), wantedRows, uiRoot.getSurfaceRows());
        boolean scrollBarShown = itemCount > vertical.length();
        SelectBoxPlacement.Span horizontal = SelectBoxPlacement.placeHorizontally(
                box.getAbsoluteX(), box.getWidth(), box.getListEntryWidth(), scrollBarShown,
                box.getListWidth(), uiRoot.getSurfaceColumns());
        WidgetGroup popupLayer = uiRoot.getPopupLayer();
        rows.showEntries(horizontal.length() - (scrollBarShown ? 1 : 0), box.getSelectedIndex());
        pane.setStyle(box.getStyle().getScrollPane());
        pane.setSize(horizontal.length(), vertical.length());
        pane.setPosition(horizontal.start() - popupLayer.getX(),
                vertical.start() - popupLayer.getY());
    }
}
