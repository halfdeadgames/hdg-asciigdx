package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.Objects;

/**
 * A row of menu titles with a drop-down list under each. The bar draws its titles itself; the
 * drop-down of the open menu lives in {@link UiRoot#getPopupLayer()}, so it appears above the main
 * tree.
 *
 * <p>Size: the bar is exactly one row high. Its width is how wide the bar's background
 * is; the titles are drawn from the left edge in the text font, each with the title style's
 * horizontal padding on both sides, and the rest of the width shows the normal background. A
 * title takes one text column (half a UI column) per code point, rounded up to whole UI cells, so
 * title offsets, widths, and hit testing are in whole UI cells. Adding a menu widens the bar to
 * fit all titles when it is narrower, and never narrows it. Titles that do not fit are cut off.
 *
 * <p>Looks: a title is drawn with the title style's normal look, its pointer-over look while
 * pointer 0 is over it, and its pressed look while its menu is open. Items use the item style's
 * normal, pointer-over, and pressed looks in the same way. The disabled looks are not used.
 *
 * <p>Using it, which needs a {@link UiRoot} to deliver input:
 * <ul>
 *   <li>Pressing the primary button on a title opens its drop-down directly below the title, with
 *       the left edges aligned. If it would leave the surface it is moved inside, to the left or
 *       up. All items are as wide as the widest item (rounded up to whole UI cells) plus the item
 *       style's padding on both sides, one row high each.
 *   <li>While a menu is open, moving pointer 0 over another title switches to that menu.
 *   <li>Pressing and releasing the primary button on the same item chooses it: the menu closes,
 *       and then the item's action runs. Releasing elsewhere chooses nothing.
 *   <li>The menu closes on the Escape key, on a press on the open title, on a press anywhere
 *       outside the drop-down and the titles, and on {@link #close()}.
 * </ul>
 * While a menu is open, an invisible widget as large as the surface sits under the drop-down. It
 * consumes every press outside the drop-down, so a press that closes the menu does not click
 * whatever is underneath.
 *
 * <p>Lifetime: a bar belongs to the root passed to {@link #of(UiRoot, MenuBarStyle)} and stays
 * registered with that root for the root's whole life; it cannot be moved to another root.
 * {@code of} does not add the bar to any tree. The bar works only while it is in that root's main
 * tree ({@link UiRoot#getRoot()}), not in the popup layer. Build it once per screen and reuse it,
 * instead of building a new one each time the screen is shown. If a bar is found outside the main
 * tree with a menu open, which is noticed on the next key event, it closes the menu without
 * running any action. At most one menu is open per root: opening a menu closes the open menu of
 * any other bar of the root, and while one bar has a menu open, the other bars ignore keys.
 *
 * <p>Keyboard focus: pressing a title focuses the bar. While a menu is open, the focus belongs to
 * an internal widget, so {@link UiRoot#getKeyboardFocus()} does not return the bar; that widget
 * closes the menu on Escape. Closing gives the focus back to the widget that had it before the
 * menu opened (before the press, for a press), or clears it if that widget has left the widget
 * trees. Menus have no disabled items, submenus, or check items.
 *
 * <p>Keyboard use: a title or item may have a trigger, an ASCII letter or digit (see
 * {@link #addMenu(String, char)} and {@link Menu#addItem(String, char, Runnable)}); keys are
 * matched without regard to case. The letter and digit keys and the numpad digits count. A focused
 * widget that handles a key gets it first; the bar acts only on keys that the focused widget and
 * its parents did not handle. Alt+trigger works whenever the bar is in the main tree, also when
 * nothing has focus. The bar tracks the Alt key from every key down and key up event, including
 * ones that a focused widget handled, so a missed release cannot leave Alt stuck. Closing a menu
 * does not change the Alt state.
 * <ul>
 *   <li>Menu closed: Alt+trigger always opens the first title, from left to right, that has
 *       that trigger, with no item highlighted. Plain letters do nothing. The bar does not
 *       remember which menu was open before.
 *   <li>Menu open: a trigger key, with or without Alt, looks for items with that trigger in the
 *       open menu. One match runs at once, as a click would. Several matches: with no item
 *       highlighted the first press highlights the first match from the top; each later press
 *       moves the highlight to the next match after the current highlight, looping, and Enter
 *       runs the highlighted item. No match: with Alt the key switches to the next title, after
 *       the open one and looping back to the left, that has that trigger; without Alt nothing
 *       happens. So repeated Alt+trigger presses cycle through titles that share a trigger.
 *   <li>Menu open: Up and Down move the highlight through all items and wrap at the ends. Down
 *       with no highlight goes to the first item, and Up to the last. Left and Right switch to
 *       the previous or next menu, wrapping, and highlight its first item; with only one menu
 *       they reopen that menu with its first item highlighted. Enter with no highlight does
 *       nothing. Escape closes the menu.
 * </ul>
 * The highlight and the pointer hover are one and the same: the pointer moving over an item
 * highlights it, and the highlighted item is drawn in the item style's pointer-over look. A menu
 * opened with the keyboard has its title drawn in the pressed look, as when it is clicked. Alt
 * exists only on hardware keyboards, which in practice means desktop.
 *
 * <p>Item actions run while an input event is being delivered. Calling a pointer or key input
 * method of the {@link UiRoot} from an action throws {@link IllegalStateException}; defer such
 * work to the next frame with a {@link UiUpdateListener}.
 *
 * <p>Example:
 * <pre>{@code
 * ButtonStyle look = ButtonStyle.of(LabelStyle.of(0xFFFFFFFF, 0x303030FF));
 * MenuBar bar = MenuBar.of(ui, MenuBarStyle.of(look, look));
 * bar.setSize(40, 0);
 * Menu file = bar.addMenu("File");
 * file.addItem("Open", () -> openFile());
 * file.addItem("Quit", () -> quitGame());
 * ui.getRoot().addChild(bar);
 * }</pre>
 *
 * <p>Not thread-safe; use it from the render thread.
 */
public final class MenuBar extends Widget {

    private static final int PRIMARY_BUTTON = 0;
    private static final int NO_INDEX = -1;
    private static final int BAR_HEIGHT = 1;

    private final UiRoot uiRoot;
    private final MenuBarStyle style;
    private final ArrayList<Menu> menus = new ArrayList<>();
    private final MenuPressCatcher pressCatcher;
    private final MenuDropDown dropDown;
    private Menu openMenu;
    private int hoveredTitleIndex = NO_INDEX;
    private Widget focusBeforeOpen;
    private boolean altHeld;

    private MenuBar(UiRoot uiRoot, MenuBarStyle style) {
        this.uiRoot = uiRoot;
        this.style = style;
        this.pressCatcher = new MenuPressCatcher(this, uiRoot.getSurfaceColumns(),
                uiRoot.getSurfaceRows());
        this.dropDown = new MenuDropDown(this);
        setSize(0, 0);
    }

    /**
     * Creates an empty bar, 0 columns wide. Add menus with {@link #addMenu(String)}, position it,
     * and add it to a widget group of the same root's main tree.
     *
     * @param root root whose popup layer shows the drop-downs; must not be {@code null}. The
     *         drop-down and the press catcher use the size of the root's surface, which is fixed
     *         for the life of the root because a {@link UiSurface} never resizes
     * @param style looks and padding; must not be {@code null}
     * @return a new bar that is not in any tree yet
     * @throws NullPointerException if {@code root} or {@code style} is {@code null}
     */
    public static MenuBar of(UiRoot root, MenuBarStyle style) {
        MenuBar bar = new MenuBar(Objects.requireNonNull(root, "root"),
                Objects.requireNonNull(style, "style"));
        root.addKeyObserver(bar::observeKey);
        root.addUnhandledKeyListener(bar::handleUnhandledKey);
        return bar;
    }

    /**
     * Adds a menu with a title at the right end of the bar. The bar widens to fit the title if it
     * is too narrow. Add items to the returned menu.
     *
     * @param title text of the title, one text column per code point; must not be {@code null};
     *         may be empty
     * @return the new, empty menu
     * @throws NullPointerException if {@code title} is {@code null}
     */
    public Menu addMenu(String title) {
        return addMenu(title, MenuTrigger.NONE);
    }

    /**
     * Adds a menu with a title and a trigger letter at the right end of the bar. Alt plus the
     * trigger opens the menu; see the class description. The first case-insensitive match of the
     * trigger in the title is drawn in the style's trigger color, if it has one. The bar widens to
     * fit the title if it is too narrow.
     *
     * @param title text of the title, one text column per code point; must not be {@code null};
     *         may be empty
     * @param trigger an ASCII letter (A-Z or a-z) or digit (0-9) that appears in {@code title};
     *         matching ignores case
     * @return the new, empty menu
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code trigger} is not an ASCII letter or digit, or does
     *         not appear in {@code title} when compared without regard to case
     */
    public Menu addMenu(String title, char trigger) {
        Objects.requireNonNull(title, "title");
        Menu menu = new Menu(this, title, trigger, MenuTrigger.requireCellOf(title, trigger));
        menus.add(menu);
        setSize(Math.max(getWidth(), getPreferredWidth()), 0);
        invalidate();
        return menu;
    }

    /**
     * Returns the number of menus.
     *
     * @return the menu count, zero or greater
     */
    public int getMenuCount() {
        return menus.size();
    }

    /**
     * Returns a menu by its place in the bar.
     *
     * @param index position from the left, from 0 to {@code getMenuCount() - 1}
     * @return the menu at that position
     * @throws IndexOutOfBoundsException if {@code index} is outside that range
     */
    public Menu getMenu(int index) {
        return menus.get(index);
    }

    /**
     * Returns whether a drop-down is showing.
     *
     * @return {@code true} while a menu is open
     */
    public boolean isOpen() {
        return openMenu != null;
    }

    /**
     * Closes the open menu, removes its drop-down from the popup layer, gives the keyboard focus
     * back to the widget that had it before the menu opened (or clears it if that widget is no
     * longer in the widget trees), and requests a redraw. It never runs an item action. Does
     * nothing when no menu is open, so it is safe to call from an item action, where the menu is
     * already closed. Safe to call while an event is being delivered.
     *
     * <p>Call it when the game pauses or changes screen, so no menu stays open over the new
     * content.
     */
    public void close() {
        if (openMenu == null) {
            return;
        }
        openMenu = null;
        if (uiRoot.getOpenMenuBar() == this) {
            uiRoot.setOpenMenuBar(null);
        }
        hoveredTitleIndex = NO_INDEX;
        WidgetGroup popupLayer = uiRoot.getPopupLayer();
        popupLayer.removeChild(dropDown);
        popupLayer.removeChild(pressCatcher);
        dropDown.reset();
        uiRoot.setKeyboardFocus(isInTrees(focusBeforeOpen) ? focusBeforeOpen : null);
        focusBeforeOpen = null;
        requestRedraw();
    }

    /**
     * Sets the width of the bar. The height becomes 1 whatever is passed, and a negative
     * {@code height} does not throw. A width below
     * {@link #getPreferredWidth()} is allowed and cuts off the titles that do not fit. Later
     * {@link #addMenu(String)} calls only widen the bar and never narrow it. Like the position
     * (see {@link #setPosition(int, int)}), the size takes effect on the next draw; the position
     * is relative to the parent's top-left corner.
     *
     * @param width width of the bar's background in UI cells; zero or greater
     * @param height ignored
     * @throws IllegalArgumentException if {@code width} is negative
     */
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, BAR_HEIGHT);
    }

    /**
     * Returns the total width of all titles with their padding.
     *
     * @return the width in UI cells, zero or greater
     */
    @Override
    public int getPreferredWidth() {
        int total = 0;
        for (int i = 0; i < menus.size(); i++) {
            total += titleWidth(menus.get(i));
        }
        return total;
    }

    /**
     * Returns one row.
     *
     * @return 1 UI cell
     */
    @Override
    public int getPreferredHeight() {
        return BAR_HEIGHT;
    }

    /**
     * Returns whether the bar can take keyboard focus, which it always can.
     *
     * @return {@code true}
     */
    @Override
    public boolean isFocusable() {
        return true;
    }

    // A bar draws no focus look and has its own keys, so Tab and the arrows skip it.
    @Override
    protected boolean isInFocusOrder() {
        return false;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        ButtonStyle titleStyle = style.getTitles();
        MenuRowPainter.paint(target, style, titleStyle.getNormal(), "", 0, MenuTrigger.NO_CELL,
                left, top, getWidth(), getHeight());
        int offset = 0;
        for (int i = 0; i < menus.size(); i++) {
            Menu menu = menus.get(i);
            int visibleWidth = Math.min(titleWidth(menu), getWidth() - offset);
            if (visibleWidth <= 0) {
                return;
            }
            MenuRowPainter.paint(target, style, titleLook(titleStyle, i), menu.getTitle(),
                    titleStyle.getHorizontalPadding(), menu.getTriggerIndex(), left + offset,
                    top, visibleWidth, getHeight());
            offset += titleWidth(menu);
        }
    }

    private LabelStyle titleLook(ButtonStyle titleStyle, int index) {
        if (menus.get(index) == openMenu) {
            return titleStyle.getPressed();
        }
        return index == hoveredTitleIndex ? titleStyle.getPointerOver() : titleStyle.getNormal();
    }

    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_ENTERED, POINTER_MOVED -> {
                setHoveredTitle(titleIndexAt(event.getSurfaceColumn(), event.getSurfaceRow()));
                yield false;
            }
            case POINTER_EXITED -> {
                setHoveredTitle(NO_INDEX);
                yield false;
            }
            case POINTER_DOWN -> handlePointerDown(event);
            default -> false;
        };
    }

    private boolean handlePointerDown(UiEvent event) {
        if (event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        int index = titleIndexAt(event.getSurfaceColumn(), event.getSurfaceRow());
        if (index == NO_INDEX) {
            return false;
        }
        openOrSwitch(menus.get(index), true);
        return true;
    }

    // Sees every key event, also the ones a focused widget handles, so the Alt state never goes
    // stale. It also notices a bar that left the main tree while its menu was open.
    private boolean observeKey(UiEvent event) {
        int keycode = event.getKeycode();
        if (keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT) {
            altHeld = event.getType() == UiEventType.KEY_DOWN;
        }
        if (!isInMainTree()) {
            close();
        }
        return false;
    }

    private boolean handleUnhandledKey(UiEvent event) {
        if (!isInMainTree() || event.getType() != UiEventType.KEY_DOWN) {
            return false;
        }
        MenuBar openBar = uiRoot.getOpenMenuBar();
        if (openBar != null && openBar != this) {
            return false;
        }
        int keycode = event.getKeycode();
        if (keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT) {
            return false;
        }
        return openMenu == null ? handleKeyWhileClosed(keycode) : handleKeyWhileOpen(keycode);
    }

    private boolean handleKeyWhileClosed(int keycode) {
        char key = MenuTrigger.keyToCharacter(keycode);
        return altHeld && key != MenuTrigger.NONE && showNextMenuWithTrigger(key);
    }

    private boolean handleKeyWhileOpen(int keycode) {
        switch (keycode) {
            case Input.Keys.UP:
                moveHighlight(-1);
                return true;
            case Input.Keys.DOWN:
                moveHighlight(1);
                return true;
            case Input.Keys.LEFT:
                showNeighborMenu(-1);
                return true;
            case Input.Keys.RIGHT:
                showNeighborMenu(1);
                return true;
            case Input.Keys.ENTER:
            case Input.Keys.NUMPAD_ENTER:
                return chooseHighlightedItem();
            default:
                return handleTriggerKeyWhileOpen(MenuTrigger.keyToCharacter(keycode));
        }
    }

    private boolean handleTriggerKeyWhileOpen(char key) {
        if (key == MenuTrigger.NONE) {
            return false;
        }
        int matches = openMenu.countItemsWithTrigger(key);
        if (matches == 0) {
            return altHeld && showNextMenuWithTrigger(key);
        }
        int next = openMenu.findItemWithTrigger(key, dropDown.getHighlightedItem());
        if (matches == 1) {
            chooseItem(openMenu, next);
        } else {
            dropDown.highlightItem(next);
        }
        return true;
    }

    private boolean chooseHighlightedItem() {
        int highlighted = dropDown.getHighlightedItem();
        if (highlighted == NO_INDEX) {
            return false;
        }
        chooseItem(openMenu, highlighted);
        return true;
    }

    private void moveHighlight(int direction) {
        int count = openMenu.getItemCount();
        if (count == 0) {
            return;
        }
        int highlighted = dropDown.getHighlightedItem();
        if (highlighted == NO_INDEX) {
            dropDown.highlightItem(direction > 0 ? 0 : count - 1);
            return;
        }
        dropDown.highlightItem(Math.floorMod(highlighted + direction, count));
    }

    private void showNeighborMenu(int direction) {
        int index = menus.indexOf(openMenu);
        showMenu(menus.get(Math.floorMod(index + direction, menus.size())));
        if (openMenu.getItemCount() > 0) {
            dropDown.highlightItem(0);
        }
    }

    private boolean showNextMenuWithTrigger(char key) {
        int start = openMenu == null ? NO_INDEX : menus.indexOf(openMenu);
        for (int step = 1; step <= menus.size(); step++) {
            Menu candidate = menus.get(Math.floorMod(start + step, menus.size()));
            if (MenuTrigger.matches(candidate.getTrigger(), key)) {
                openOrSwitch(candidate, false);
                return true;
            }
        }
        return false;
    }

    MenuBarStyle getBarStyle() {
        return style;
    }

    void followPointerWhileOpen(int surfaceColumn, int surfaceRow) {
        if (!isInMainTree()) {
            close();
            return;
        }
        int index = titleIndexAt(surfaceColumn, surfaceRow);
        setHoveredTitle(index);
        if (index != NO_INDEX && menus.get(index) != openMenu) {
            showMenu(menus.get(index));
        }
    }

    void handleOutsidePress(int surfaceColumn, int surfaceRow, int button) {
        if (!isInMainTree()) {
            close();
            return;
        }
        int index = titleIndexAt(surfaceColumn, surfaceRow);
        boolean otherTitle = index != NO_INDEX && menus.get(index) != openMenu;
        if (button == PRIMARY_BUTTON && otherTitle) {
            showMenu(menus.get(index));
        } else {
            close();
        }
    }

    boolean closeOnEscape(UiEvent event) {
        if (event.getType() != UiEventType.KEY_DOWN || event.getKeycode() != Input.Keys.ESCAPE) {
            return false;
        }
        close();
        return true;
    }

    void chooseItem(Menu menu, int itemIndex) {
        Runnable action = menu.getItemAction(itemIndex);
        close();
        action.run();
    }

    void refreshDropDown(Menu menu) {
        if (menu != openMenu) {
            return;
        }
        layoutDropDown();
    }

    // Opens the menu, or switches to it when another of this bar is open. The focus to restore is
    // only remembered when this call opens the first menu. A press moves the focus to the bar
    // before it is dispatched, so a press must use the focus the root kept from before it.
    private void openOrSwitch(Menu menu, boolean openedByPress) {
        if (openMenu == null) {
            closeMenuOfOtherBar();
            focusBeforeOpen = openedByPress ? uiRoot.getFocusBeforePress()
                    : uiRoot.getKeyboardFocus();
        }
        showMenu(menu);
    }

    private void closeMenuOfOtherBar() {
        MenuBar other = uiRoot.getOpenMenuBar();
        if (other != null && other != this) {
            other.close();
        }
    }

    private void showMenu(Menu menu) {
        boolean wasClosed = openMenu == null;
        openMenu = menu;
        uiRoot.setOpenMenuBar(this);
        dropDown.showMenu(menu);
        layoutDropDown();
        if (wasClosed) {
            attachPopups();
        }
        requestRedraw();
    }

    private void attachPopups() {
        WidgetGroup popupLayer = uiRoot.getPopupLayer();
        pressCatcher.setPosition(-popupLayer.getX(), -popupLayer.getY());
        popupLayer.addChild(pressCatcher);
        popupLayer.addChild(dropDown);
        uiRoot.setKeyboardFocus(pressCatcher);
    }

    private void layoutDropDown() {
        int width = dropDown.getPreferredWidth();
        int height = dropDown.getPreferredHeight();
        int column = absoluteLeft() + titleOffset(openMenu);
        column = Math.max(0, Math.min(column, uiRoot.getSurfaceColumns() - width));
        int row = absoluteTop() + BAR_HEIGHT;
        if (row + height > uiRoot.getSurfaceRows()) {
            row = Math.max(0, uiRoot.getSurfaceRows() - height);
        }
        WidgetGroup popupLayer = uiRoot.getPopupLayer();
        dropDown.setSize(width, height);
        dropDown.setPosition(column - popupLayer.getX(), row - popupLayer.getY());
    }

    private void setHoveredTitle(int index) {
        if (hoveredTitleIndex == index) {
            return;
        }
        hoveredTitleIndex = index;
        requestRedraw();
    }

    private int titleIndexAt(int surfaceColumn, int surfaceRow) {
        int localColumn = surfaceColumn - absoluteLeft();
        int localRow = surfaceRow - absoluteTop();
        if (!isInsideBounds(localColumn, localRow)) {
            return NO_INDEX;
        }
        int titleEnd = 0;
        for (int i = 0; i < menus.size(); i++) {
            titleEnd += titleWidth(menus.get(i));
            if (localColumn < titleEnd) {
                return i;
            }
        }
        return NO_INDEX;
    }

    private int titleOffset(Menu menu) {
        int offset = 0;
        for (int i = 0; menus.get(i) != menu; i++) {
            offset += titleWidth(menus.get(i));
        }
        return offset;
    }

    private int titleWidth(Menu menu) {
        String title = menu.getTitle();
        int padding = style.getTitles().getHorizontalPadding();
        int titleColumns = title.codePointCount(0, title.length());
        int titleCells = (titleColumns + UiSurface.TEXT_COLUMNS_PER_CELL - 1)
                / UiSurface.TEXT_COLUMNS_PER_CELL;
        return titleCells + 2 * padding;
    }

    private int absoluteLeft() {
        int left = 0;
        for (Widget current = this; current != null; current = current.getParent()) {
            left += current.getX();
        }
        return left;
    }

    private int absoluteTop() {
        int top = 0;
        for (Widget current = this; current != null; current = current.getParent()) {
            top += current.getY();
        }
        return top;
    }

    private boolean isInMainTree() {
        Widget top = this;
        while (top.getParent() != null) {
            top = top.getParent();
        }
        return top == uiRoot.getRoot();
    }

    private boolean isInTrees(Widget widget) {
        if (widget == null) {
            return false;
        }
        Widget top = widget;
        while (top.getParent() != null) {
            top = top.getParent();
        }
        return top == uiRoot.getRoot() || top == uiRoot.getPopupLayer();
    }
}
