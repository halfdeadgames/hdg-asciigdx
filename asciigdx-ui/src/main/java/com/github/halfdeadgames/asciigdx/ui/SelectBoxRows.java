package com.github.halfdeadgames.asciigdx.ui;

/**
 * The rows of the open list of a {@link SelectBox}: the content of the scroll pane in the popup
 * layer. A row is one cell high and as wide as the view of the pane.
 */
final class SelectBoxRows extends Widget {

    private static final int NO_INDEX = -1;
    private static final int NO_POINTER = -1;
    private static final int PRIMARY_BUTTON = 0;

    private final SelectBox<?> box;
    private final SelectBoxList list;
    private int columns;
    private int highlightedIndex = NO_INDEX;
    private int pressedIndex = NO_INDEX;
    private int armedIndex = NO_INDEX;
    private int armedPointer = NO_POINTER;

    SelectBoxRows(SelectBox<?> box, SelectBoxList list) {
        this.box = box;
        this.list = list;
    }

    void showEntries(int viewColumns, int firstHighlightedIndex) {
        columns = viewColumns;
        highlightedIndex = firstHighlightedIndex;
        cancelPress();
        invalidate();
    }

    int getHighlightedIndex() {
        return highlightedIndex;
    }

    void highlight(int index) {
        if (highlightedIndex == index) {
            return;
        }
        highlightedIndex = index;
        requestRedraw();
    }

    void cancelPress() {
        armedIndex = NO_INDEX;
        armedPointer = NO_POINTER;
        setPressedIndex(NO_INDEX);
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public int getPreferredWidth() {
        return columns;
    }

    @Override
    public int getPreferredHeight() {
        return box.getItemCount();
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        ButtonStyle rowStyle = box.getStyle().getRowStyle();
        int firstRow = Math.max(0, -getY());
        int viewRows = getParent() == null ? box.getItemCount() : getParent().getHeight();
        int rowLimit = Math.min(box.getItemCount(), firstRow + viewRows);
        for (int index = firstRow; index < rowLimit; index++) {
            paintRow(target, rowStyle, lookOf(rowStyle, index), index, left, top + index);
        }
    }

    private LabelStyle lookOf(ButtonStyle rowStyle, int index) {
        if (index == pressedIndex) {
            return rowStyle.getPressed();
        }
        return index == highlightedIndex ? rowStyle.getPointerOver() : rowStyle.getNormal();
    }

    private void paintRow(UiSurface target, ButtonStyle rowStyle, LabelStyle look, int index,
            int left, int row) {
        int padding = rowStyle.getHorizontalPadding();
        boolean swatchShown = box.hasSwatches();
        target.eraseGlyphs(left, row, columns, 1);
        target.fillBackground(left, row, columns, 1, look.getBackgroundRgba());
        if (swatchShown) {
            SelectBoxEntryText.fillSwatch(target, left, row, padding, box.getItemSwatch(index));
        }
        SelectBoxEntryText.write(target,
                SelectBoxEntryText.firstTextColumn(left, padding, swatchShown), row,
                SelectBoxEntryText.textColumnRoom(columns, padding, 0, swatchShown),
                box.getItemText(index), look.getForegroundRgba());
    }

    private int entryAt(int column, int row) {
        boolean inside = column >= 0 && column < columns && row >= 0
                && row < box.getItemCount();
        return inside ? row : NO_INDEX;
    }

    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_ENTERED, POINTER_MOVED -> {
                highlightEntryUnderPointer(event);
                yield false;
            }
            case POINTER_DOWN -> handlePointerDown(event);
            case POINTER_DRAGGED -> handlePointerDragged(event);
            case POINTER_UP -> handlePointerUp(event);
            case POINTER_CANCELLED -> handlePointerCancelled(event);
            case KEY_DOWN -> list.handleKey(event.getKeycode());
            case KEY_TYPED -> list.handleTyped(event.getCharacter());
            default -> false;
        };
    }

    private void highlightEntryUnderPointer(UiEvent event) {
        int index = entryAt(event.getColumn(), event.getRow());
        if (index != NO_INDEX) {
            highlight(index);
        }
    }

    private boolean handlePointerDown(UiEvent event) {
        if (event.getButton() != PRIMARY_BUTTON) {
            return true;
        }
        armedPointer = event.getPointer();
        armedIndex = entryAt(event.getColumn(), event.getRow());
        setPressedIndex(armedIndex);
        return true;
    }

    private boolean handlePointerDragged(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        int underPointer = entryAt(event.getColumn(), event.getRow());
        setPressedIndex(underPointer == armedIndex ? armedIndex : NO_INDEX);
        return true;
    }

    private boolean handlePointerUp(UiEvent event) {
        if (event.getPointer() != armedPointer || event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        int releasedOn = entryAt(event.getColumn(), event.getRow());
        boolean picked = releasedOn != NO_INDEX && releasedOn == armedIndex;
        cancelPress();
        if (picked) {
            list.pick(releasedOn);
        }
        return true;
    }

    private boolean handlePointerCancelled(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        cancelPress();
        return true;
    }

    private void setPressedIndex(int index) {
        if (pressedIndex == index) {
            return;
        }
        pressedIndex = index;
        requestRedraw();
    }
}
