package com.github.halfdeadgames.asciigdx.workbench.ui;

import java.util.List;
import java.util.function.Predicate;

/**
 * Logic for a top menu bar on the cell grid: which menu is open, which item is highlighted, which
 * title the pointer is over, and where everything sits in cells. Nothing here draws or reads input.
 * Availability comes from the caller on each call so greyed items follow live state. A menu without
 * an available item is locked: it cannot be opened and left/right movement skips it.
 */
public final class MenuBar<A> {

    /** Passed as the row offset when the pointer is not on a drop-down row (for example on the bar itself). */
    public static final int NOT_IN_DROP_DOWN = Integer.MIN_VALUE;

    private static final int FIRST_TITLE_COLUMN = 1;
    private static final int TITLE_GAP = 2;
    private static final int LABEL_SHORTCUT_GAP = 2;
    private static final int DROP_DOWN_PADDING = 2;

    public record Item<A>(String label, String shortcut, A action) {
        public Item {
            if (label == null) {
                throw new IllegalArgumentException("label");
            }
            if (shortcut == null) {
                shortcut = "";
            }
            if (action == null) {
                throw new IllegalArgumentException("action");
            }
        }
    }

    public record Menu<A>(String title, char accessKey, List<Item<A>> items) {
        public Menu {
            if (title == null) {
                throw new IllegalArgumentException("title");
            }
            items = List.copyOf(items);
        }
    }

    private final List<Menu<A>> menus;
    private int openMenu = -1;
    private int highlightedItem = -1;
    private int hoveredTitle = -1;
    private int inspectedItem = -1;
    private int inspectedTitle = -1;

    public MenuBar(List<Menu<A>> menus) {
        this.menus = List.copyOf(menus);
    }

    public int menuCount() {
        return menus.size();
    }

    public Menu<A> menu(int index) {
        return menus.get(index);
    }

    /** Index of the open menu, or -1. */
    public int openMenu() {
        return openMenu;
    }

    public boolean isOpen() {
        return openMenu >= 0;
    }

    /** Index of the highlighted item in the open menu, or -1. */
    public int highlightedItem() {
        return highlightedItem;
    }

    /** Index of the title under the pointer, or -1. Independent of which menu is open. */
    public int hoveredTitle() {
        return hoveredTitle;
    }

    /** Index of the unavailable item under the pointer in the open menu, or -1. Never selectable. */
    public int inspectedItem() {
        return inspectedItem;
    }

    /** Index of the locked title under the pointer, or -1. Never selectable. */
    public int inspectedTitle() {
        return inspectedTitle;
    }

    public boolean isItemAvailable(int menu, int item, Predicate<A> availability) {
        return availability.test(menus.get(menu).items().get(item).action());
    }

    public boolean isLocked(int menu, Predicate<A> availability) {
        return firstAvailableItem(menu, availability) < 0;
    }

    private int firstAvailableItem(int menu, Predicate<A> availability) {
        List<Item<A>> items = menus.get(menu).items();
        for (int i = 0; i < items.size(); i++) {
            if (availability.test(items.get(i).action())) {
                return i;
            }
        }
        return -1;
    }

    public int titleColumn(int menu) {
        int column = FIRST_TITLE_COLUMN;
        for (int i = 0; i < menu; i++) {
            column += menus.get(i).title().length() + TITLE_GAP;
        }
        return column;
    }

    /** Cells used by the bar, counting the leading blank cell. */
    public int barWidth() {
        if (menus.isEmpty()) {
            return FIRST_TITLE_COLUMN;
        }
        int last = menus.size() - 1;
        return titleColumn(last) + menus.get(last).title().length();
    }

    /** Index of the title covering a column, or -1. */
    public int titleAt(int column) {
        for (int i = 0; i < menus.size(); i++) {
            int start = titleColumn(i);
            // The lit fill is drawn one cell wider than the text on each side.
            if (column >= start - 1 && column <= start + menus.get(i).title().length()) {
                return i;
            }
        }
        return -1;
    }

    public int dropDownWidth(int menu) {
        int longestLabel = 0;
        int longestShortcut = 0;
        for (Item<A> item : menus.get(menu).items()) {
            longestLabel = Math.max(longestLabel, item.label().length());
            longestShortcut = Math.max(longestShortcut, item.shortcut().length());
        }
        return longestLabel + LABEL_SHORTCUT_GAP + longestShortcut + DROP_DOWN_PADDING;
    }

    /** Drop-down column: the title column, shifted left to stay inside {@code totalColumns} (never below 0). */
    public int dropDownColumn(int menu, int totalColumns) {
        return Math.max(0, Math.min(titleColumn(menu), totalColumns - dropDownWidth(menu)));
    }

    /**
     * Item under a cell of the open drop-down, or -1 when closed or outside it.
     *
     * @param rowOffset 0 is the first item row
     */
    public int itemAt(int column, int rowOffset, int totalColumns) {
        if (openMenu < 0 || rowOffset == NOT_IN_DROP_DOWN) {
            return -1;
        }
        int start = dropDownColumn(openMenu, totalColumns);
        // The first and last columns are the panel's border, not item cells.
        if (column <= start || column >= start + dropDownWidth(openMenu) - 1) {
            return -1;
        }
        if (rowOffset < 0 || rowOffset >= menus.get(openMenu).items().size()) {
            return -1;
        }
        return rowOffset;
    }

    /**
     * Whether a cell lies on the open drop-down's panel, borders included: the panel is the item rows
     * plus a border row above and below, and the full width.
     *
     * @param rowOffset 0 is the first item row, -1 the top border row
     */
    public boolean isOnDropDown(int column, int rowOffset, int totalColumns) {
        if (openMenu < 0 || rowOffset == NOT_IN_DROP_DOWN) {
            return false;
        }
        int start = dropDownColumn(openMenu, totalColumns);
        return column >= start && column < start + dropDownWidth(openMenu)
            && rowOffset >= -1 && rowOffset <= menus.get(openMenu).items().size();
    }

    /**
     * Opens the first menu with this access key (any case) that is not locked. Does nothing when the
     * key is unknown or every menu using it is locked.
     */
    public void openByAccessKey(char key, Predicate<A> availability) {
        int menu = firstUnlockedMenuWithAccessKey(key, availability);
        if (menu < 0) {
            return;
        }
        open(menu, availability);
    }

    /** Whether {@link #openByAccessKey} would open a menu: some menu uses this access key (any case) and is not locked. */
    public boolean canOpenByAccessKey(char key, Predicate<A> availability) {
        return firstUnlockedMenuWithAccessKey(key, availability) >= 0;
    }

    private int firstUnlockedMenuWithAccessKey(char key, Predicate<A> availability) {
        char wanted = Character.toLowerCase(key);
        for (int i = 0; i < menus.size(); i++) {
            if (Character.toLowerCase(menus.get(i).accessKey()) == wanted && !isLocked(i, availability)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Brings the state in line with current availability: closes the open menu if it became locked, and
     * moves a highlight that became unavailable to the first available item. Call before acting on state.
     */
    public void refresh(Predicate<A> availability) {
        if (inspectedTitle >= 0 && !isLocked(inspectedTitle, availability)) {
            inspectedTitle = -1;
        }
        if (inspectedItem >= 0 && (openMenu < 0 || isItemAvailable(openMenu, inspectedItem, availability))) {
            inspectedItem = -1;
        }
        if (openMenu < 0) {
            return;
        }
        int first = firstAvailableItem(openMenu, availability);
        if (first < 0) {
            close();
        } else if (highlightedItem >= 0 && !isItemAvailable(openMenu, highlightedItem, availability)) {
            highlightedItem = first;
        }
    }

    /** Opens the first menu that is not locked. */
    public void openFirst(Predicate<A> availability) {
        for (int i = 0; i < menus.size(); i++) {
            if (open(i, availability)) {
                return;
            }
        }
    }

    /** Moves the open drop-down left or right, wrapping and skipping locked menus. Does nothing when closed. */
    public void moveMenu(int direction, Predicate<A> availability) {
        int sign = Integer.signum(direction);
        refresh(availability);
        clearInspected();
        if (openMenu < 0 || sign == 0) {
            return;
        }
        int count = menus.size();
        for (int step = 1; step < count; step++) {
            int candidate = Math.floorMod(openMenu + sign * step, count);
            if (open(candidate, availability)) {
                return;
            }
        }
    }

    /** Moves the highlight up or down, wrapping and skipping unavailable items. Does nothing when closed. */
    public void moveItem(int direction, Predicate<A> availability) {
        int sign = Integer.signum(direction);
        refresh(availability);
        clearInspected();
        if (openMenu < 0 || sign == 0) {
            return;
        }
        int count = menus.get(openMenu).items().size();
        int origin = highlightedItem >= 0 ? highlightedItem : (sign > 0 ? -1 : 0);
        for (int step = 1; step <= count; step++) {
            int candidate = Math.floorMod(origin + sign * step, count);
            if (isItemAvailable(openMenu, candidate, availability)) {
                highlightedItem = candidate;
                return;
            }
        }
    }

    public void close() {
        openMenu = -1;
        highlightedItem = -1;
        clearInspected();
    }

    /**
     * The highlighted item's action, closing the menu; null when nothing usable is highlighted. The
     * menu then stays open, unless the {@link #refresh} that runs first closed it for being locked.
     */
    public A chooseHighlighted(Predicate<A> availability) {
        refresh(availability);
        if (openMenu < 0 || highlightedItem < 0) {
            return null;
        }
        return choose(openMenu, highlightedItem, availability);
    }

    /**
     * Updates hover state for a pointer cell.
     *
     * @param rowOffset item row inside the open drop-down (0 is the first), or {@link #NOT_IN_DROP_DOWN}
     *                  when the pointer is on the bar row
     */
    public void hover(int column, int rowOffset, int totalColumns, Predicate<A> availability) {
        refresh(availability);
        clearInspected();
        if (rowOffset == NOT_IN_DROP_DOWN) {
            hoverBarTitle(column, availability);
            return;
        }
        hoveredTitle = -1;
        hoverDropDownItem(itemAt(column, rowOffset, totalColumns), availability);
    }

    private void hoverBarTitle(int column, Predicate<A> availability) {
        int title = titleAt(column);
        boolean locked = title >= 0 && isLocked(title, availability);
        hoveredTitle = title >= 0 && !locked ? title : -1;
        inspectedTitle = locked ? title : -1;
        if (hoveredTitle >= 0 && openMenu >= 0 && hoveredTitle != openMenu) {
            open(hoveredTitle, availability);
        }
    }

    private void hoverDropDownItem(int item, Predicate<A> availability) {
        if (item < 0) {
            return;
        }
        boolean available = isItemAvailable(openMenu, item, availability);
        highlightedItem = available ? item : -1;
        inspectedItem = available ? -1 : item;
    }

    /** Opens the menu, or closes it if it is already open. Locked menus ignore the press. */
    public void pressTitle(int menu, Predicate<A> availability) {
        if (menu == openMenu) {
            close();
            return;
        }
        open(menu, availability);
    }

    /**
     * Press-drag-release: the action under the pointer, closing the menu; null when the pointer is off
     * the items or on an unavailable one. The menu then stays open, unless the {@link #refresh} that
     * runs first closed it for being locked.
     */
    public A releaseOnItem(int column, int rowOffset, int totalColumns, Predicate<A> availability) {
        refresh(availability);
        int item = itemAt(column, rowOffset, totalColumns);
        if (item < 0) {
            return null;
        }
        return choose(openMenu, item, availability);
    }

    private boolean open(int menu, Predicate<A> availability) {
        if (menu < 0 || menu >= menus.size()) {
            return false;
        }
        int first = firstAvailableItem(menu, availability);
        if (first < 0) {
            return false;
        }
        openMenu = menu;
        highlightedItem = first;
        clearInspected();
        return true;
    }

    private void clearInspected() {
        inspectedItem = -1;
        inspectedTitle = -1;
    }

    private A choose(int menu, int item, Predicate<A> availability) {
        Item<A> chosen = menus.get(menu).items().get(item);
        if (!availability.test(chosen.action())) {
            return null;
        }
        close();
        return chosen.action();
    }
}
