package com.github.halfdeadgames.asciigdx.ui;

/** The list of items of the open menu of a {@link MenuBar}, drawn in the popup layer. */
final class MenuDropDown extends Widget {

    private static final int NO_INDEX = -1;
    private static final int NO_POINTER = -1;
    private static final int PRIMARY_BUTTON = 0;

    private final MenuBar bar;
    private Menu menu;
    private int hoveredItem = NO_INDEX;
    private int pressedItem = NO_INDEX;
    private int armedItem = NO_INDEX;
    private int armedPointer = NO_POINTER;

    MenuDropDown(MenuBar bar) {
        this.bar = bar;
    }

    void showMenu(Menu newMenu) {
        menu = newMenu;
        reset();
    }

    int getHighlightedItem() {
        return hoveredItem;
    }

    void highlightItem(int index) {
        setHoveredItem(index);
    }

    void reset() {
        hoveredItem = NO_INDEX;
        pressedItem = NO_INDEX;
        armedItem = NO_INDEX;
        armedPointer = NO_POINTER;
        requestRedraw();
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public int getRowSnap() {
        return bar.getBarStyle().getRowHeight();
    }

    @Override
    public int getPreferredWidth() {
        if (menu == null) {
            return 0;
        }
        int padding = bar.getBarStyle().getItems().getHorizontalPadding();
        return menu.getWidestItemLength() + 2 * padding;
    }

    @Override
    public int getPreferredHeight() {
        return menu == null ? 0 : menu.getItemCount() * bar.getBarStyle().getRowHeight();
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        if (menu == null) {
            return;
        }
        MenuBarStyle barStyle = bar.getBarStyle();
        ButtonStyle itemStyle = barStyle.getItems();
        int rowHeight = barStyle.getRowHeight();
        for (int i = 0; i < menu.getItemCount(); i++) {
            int rowTop = top + i * rowHeight;
            if (rowTop >= target.getRows()) {
                return;
            }
            MenuRowPainter.paint(target, barStyle, lookOf(itemStyle, i), menu.getItemText(i),
                    itemStyle.getHorizontalPadding(), menu.getItemTriggerCell(i), left, rowTop,
                    getWidth(), rowHeight);
        }
    }

    private LabelStyle lookOf(ButtonStyle itemStyle, int index) {
        if (index == pressedItem) {
            return itemStyle.getPressed();
        }
        return index == hoveredItem ? itemStyle.getPointerOver() : itemStyle.getNormal();
    }

    private int itemAt(int column, int row) {
        if (!isInsideBounds(column, row)) {
            return NO_INDEX;
        }
        return row / bar.getBarStyle().getRowHeight();
    }

    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_ENTERED, POINTER_MOVED -> {
                setHoveredItem(itemAt(event.getColumn(), event.getRow()));
                yield false;
            }
            case POINTER_EXITED -> {
                setHoveredItem(NO_INDEX);
                yield false;
            }
            case POINTER_DOWN -> handlePointerDown(event);
            case POINTER_DRAGGED -> handlePointerDragged(event);
            case POINTER_UP -> handlePointerUp(event);
            case POINTER_CANCELLED -> handlePointerCancelled(event);
            case KEY_DOWN -> bar.closeOnEscape(event);
            default -> false;
        };
    }

    private boolean handlePointerDown(UiEvent event) {
        if (event.getButton() == PRIMARY_BUTTON) {
            armedPointer = event.getPointer();
            armedItem = itemAt(event.getColumn(), event.getRow());
            setPressedItem(armedItem);
        }
        return true;
    }

    private boolean handlePointerDragged(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        int underPointer = itemAt(event.getColumn(), event.getRow());
        setPressedItem(underPointer == armedItem ? armedItem : NO_INDEX);
        return true;
    }

    private boolean handlePointerUp(UiEvent event) {
        if (event.getPointer() != armedPointer || event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        int chosenItem = itemAt(event.getColumn(), event.getRow());
        boolean chosen = chosenItem != NO_INDEX && chosenItem == armedItem;
        armedPointer = NO_POINTER;
        armedItem = NO_INDEX;
        setPressedItem(NO_INDEX);
        if (chosen) {
            bar.chooseItem(menu, chosenItem);
        }
        return true;
    }

    private boolean handlePointerCancelled(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        armedPointer = NO_POINTER;
        armedItem = NO_INDEX;
        setPressedItem(NO_INDEX);
        return true;
    }

    private void setHoveredItem(int item) {
        if (hoveredItem == item) {
            return;
        }
        hoveredItem = item;
        requestRedraw();
    }

    private void setPressedItem(int item) {
        if (pressedItem == item) {
            return;
        }
        pressedItem = item;
        requestRedraw();
    }
}
