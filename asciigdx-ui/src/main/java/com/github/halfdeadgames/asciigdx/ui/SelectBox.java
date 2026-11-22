package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A drop-down that lets the user pick one item of a list. The closed box is one row high and looks
 * like a button: it shows the text of the chosen item and an arrow in its far-right cell. Opening
 * it shows the items as rows of a list in the popup layer of the {@link UiRoot}, so scroll panes
 * and other widgets never clip it.
 *
 * <p>Items: the items are any objects. The text of an item comes from an item text function (see
 * {@link #setItemText(Function)}), {@link String#valueOf(Object)} by default. An optional swatch
 * function (see {@link #setItemSwatch(ItemSwatch)}) adds a color block before the text. The texts
 * and swatch colors are read once when the items or the functions are set, so changing an item
 * afterward does not change what the box shows. The text takes one text column (half a UI cell)
 * per code point.
 *
 * <p>Selection: the selection is an index, or {@value #NO_SELECTION} for nothing. A box with
 * nothing selected shows its placeholder text (see {@link #setPlaceholder(String)}), empty by
 * default. A new box has nothing selected.
 *
 * <p>Closed box: it fills its area with the background of its current look and writes the swatch,
 * the text, and the arrow. The text starts after the horizontal padding of the style's button
 * look. With swatches, a one-cell block filled with the item's color comes first, then one blank
 * text column, then the text. The text is cut off, ending with an ellipsis ({@code U+2026}), when
 * it does not fit before the arrow cell. The look is, in priority order: disabled, pressed (while
 * the list is open), pointer over, focused, normal. The focused look shows only while the box has
 * keyboard focus and the {@link UiRoot} shows focus (see {@link UiRoot#isFocusVisible()}).
 *
 * <p>Size: a new box is packed to its preferred size, which fits the widest item or the
 * placeholder, so the box does not change size when the choice changes. Changing the items, the
 * functions, the placeholder, or the style keeps the current size, so call {@link #pack()}
 * afterward. The height is always 1.
 *
 * <p>Open list: it needs a {@link UiRoot} to deliver input.
 * <ul>
 *   <li>It opens downward, right under the box, when all wanted rows fit under it on the surface.
 *       The wanted rows are the item count, or {@link #getMaxVisibleRows()} when that is less.
 *       Otherwise it opens below or above, on the side with more room, and shrinks to fit it.
 *   <li>With more items than rows, the list scrolls and shows the scroll bar of the style's
 *       {@link ScrollPaneStyle}. The wheel over the open list scrolls it.
 *   <li>The width follows {@link #getListWidth()}.
 *   <li>The list opens with the chosen item highlighted and scrolled into view. The highlighted row
 *       uses the pointer-over look of the list style, a pressed row the pressed look, and other
 *       rows the normal look. The pointer over a row highlights it.
 * </ul>
 *
 * <p>Input:
 * <ul>
 *   <li>A press of the primary button on the box, or Enter, numpad Enter, or Space while the box
 *       has keyboard focus, opens the list. A disabled box does not open.
 *   <li>While the list is open, Up and Down move the highlight (it stops at the ends and scrolls
 *       into view), Enter picks the highlighted item, and Escape closes the list without a change.
 *       Typing a letter or digit highlights the next item whose text starts with it, without
 *       regard to case, and wraps around from the last item to the first.
 *   <li>Pressing and releasing the primary button on the same row picks that item. Releasing
 *       elsewhere picks nothing.
 *   <li>A press anywhere else, including on the box itself, closes the list. An invisible widget as
 *       large as the surface sits under the list and consumes that press, so it does not click
 *       whatever is underneath.
 *   <li>The mouse wheel over a closed box is not consumed, so an outer {@link ScrollPane} scrolls.
 * </ul>
 * Closing the list gives the keyboard focus back to the box.
 *
 * <p>Change listeners are called only when the user picks an item that differs from the current
 * one. Picking the current item again, and every {@code set} method called from code, call no
 * listeners, as for {@link CheckBox}. In particular, {@link #setItems(List)} clears the selection
 * without calling a listener.
 *
 * <p>Lifetime: the list closes when the box is disabled, or when the items are replaced. It also
 * closes when the box is hidden or removed from the trees of its root while it is open; this is
 * noticed on the next {@link UiRoot#update(float)} call. Call {@link #closeList()} when the game
 * pauses or changes screen.
 *
 * <p>Example:
 * <pre>{@code
 * SelectBox<Difficulty> box = SelectBox.of(ui, skin.getSelectBoxStyle("selectBox"));
 * box.setItems(List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD));
 * box.setSelected(Difficulty.NORMAL);
 * box.addChangeListener(source -> setDifficulty(source.getSelected()));
 * ui.getRoot().addChild(box);
 * }</pre>
 *
 * <p>Not thread-safe; use it from the render thread.
 *
 * @param <T> the type of the items
 */
public final class SelectBox<T> extends Widget {

    /** The selection index that means nothing is selected: {@value}. */
    public static final int NO_SELECTION = -1;

    /** Rows of the open list shown at most, until {@link #setMaxVisibleRows} changes it: 8. */
    public static final int DEFAULT_MAX_VISIBLE_ROWS = 8;

    private static final Function<Object, String> DEFAULT_ITEM_TEXT = String::valueOf;
    private static final int PRIMARY_BUTTON = 0;
    private static final int BOX_HEIGHT = 1;
    private static final int ARROW_CELLS = 1;

    private final UiRoot uiRoot;
    private final SelectBoxList list;
    private SelectBoxStyle style;
    private List<T> items = List.of();
    private String[] itemTexts = new String[0];
    private int[] itemSwatches = new int[0];
    private Function<? super T, String> itemTextFunction = DEFAULT_ITEM_TEXT;
    private ItemSwatch<? super T> itemSwatch;
    private String placeholder = "";
    private int widestItemColumns;
    private int placeholderColumns;
    private int selectedIndex = NO_SELECTION;
    private int maxVisibleRows = DEFAULT_MAX_VISIBLE_ROWS;
    private SelectBoxListWidth listWidth = SelectBoxListWidth.FIT_ENTRIES;
    private List<ChangeListener<? super SelectBox<T>>> changeListeners = List.of();
    private boolean disabled;
    private boolean pointerOver;

    private SelectBox(UiRoot root, SelectBoxStyle style) {
        this.uiRoot = root;
        this.style = style;
        this.list = new SelectBoxList(this, root);
        pack();
    }

    /**
     * Creates a box with no items, packed to its preferred size. It is in no tree yet; add it to
     * a widget group of the root's main tree or window layer. The box is bound to the given root
     * for its whole life and cannot be moved to another root.
     *
     * @param <T> the type of the items
     * @param root the root whose popup layer shows the list; must not be {@code null}. The press
     *         catcher under the list uses the size of the root's surface, which is fixed for the
     *         life of the root
     * @param style looks, scroll bar, and arrow; must not be {@code null}
     * @return a new box
     * @throws NullPointerException if {@code root} or {@code style} is {@code null}
     */
    public static <T> SelectBox<T> of(UiRoot root, SelectBoxStyle style) {
        return new SelectBox<>(Objects.requireNonNull(root, "root"),
                Objects.requireNonNull(style, "style"));
    }

    /**
     * Returns the items.
     *
     * @return an unmodifiable list, never {@code null}; empty for a new box
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * Replaces the items with a copy of a list. The selection is cleared, an open list is closed,
     * and the texts and swatch colors are read again. The size stays as it is; call {@link #pack()}
     * to fit the new items. Calls {@link #invalidate()}. No listener is called.
     *
     * @param newItems the items, in the order the list shows them; must not be {@code null} and
     *         must not contain {@code null}
     * @throws NullPointerException if {@code newItems} or an item is {@code null}, or the item
     *         text function returns {@code null} for an item
     */
    public void setItems(List<? extends T> newItems) {
        List<T> copy = List.copyOf(newItems);
        list.close();
        items = copy;
        selectedIndex = NO_SELECTION;
        refreshEntries();
    }

    /**
     * Replaces the function that turns an item into its shown text, and reads the texts of all
     * items again. The size stays as it is; call {@link #pack()} afterward. Calls
     * {@link #invalidate()}.
     *
     * @param function the function; must not be {@code null} and must not return {@code null}
     * @throws NullPointerException if {@code function} is {@code null} or returns {@code null} for
     *         an item
     */
    public void setItemText(Function<? super T, String> function) {
        itemTextFunction = Objects.requireNonNull(function, "function");
        refreshEntries();
    }

    /**
     * Sets or removes the function that gives each item a swatch, and reads the swatch colors of
     * all items again. With a function, every entry starts with a one-cell block in the item's
     * color and one blank text column; without it, entries have no swatch. The size stays as it
     * is; call {@link #pack()} afterward. Calls {@link #invalidate()}.
     *
     * @param swatch the function, or {@code null} for no swatches
     */
    public void setItemSwatch(ItemSwatch<? super T> swatch) {
        itemSwatch = swatch;
        refreshEntries();
    }

    /**
     * Returns the text shown while nothing is selected.
     *
     * @return the text, never {@code null}; empty for a new box
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Sets the text shown while nothing is selected, such as a value that is not one of the
     * items. It counts toward the preferred width. With swatches on, it is drawn without a swatch
     * and starts where item texts start. The size stays as it is; call {@link #pack()} afterward.
     * A different text calls {@link #invalidate()}.
     *
     * @param text the text; must not be {@code null}; may be empty
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public void setPlaceholder(String text) {
        Objects.requireNonNull(text, "text");
        if (text.equals(placeholder)) {
            return;
        }
        placeholder = text;
        placeholderColumns = text.codePointCount(0, text.length());
        invalidate();
    }

    /**
     * Returns the index of the selected item.
     *
     * @return the index, or {@link #NO_SELECTION} when nothing is selected
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Returns the selected item.
     *
     * @return the item, or {@code null} when nothing is selected (that is, when
     *         {@link #getSelectedIndex()} is {@link #NO_SELECTION})
     */
    public T getSelected() {
        return selectedIndex == NO_SELECTION ? null : items.get(selectedIndex);
    }

    /**
     * Selects an item by its index from code. A change requests a redraw; listeners are not
     * called.
     *
     * @param index the index, from 0 to the item count minus 1, or {@link #NO_SELECTION}
     * @throws IndexOutOfBoundsException if {@code index} is outside that range
     */
    public void setSelectedIndex(int index) {
        if (index < NO_SELECTION || index >= items.size()) {
            throw new IndexOutOfBoundsException(
                    "index " + index + " is outside -1 to " + (items.size() - 1));
        }
        if (index == selectedIndex) {
            return;
        }
        selectedIndex = index;
        requestRedraw();
    }

    /**
     * Selects an item from code, the first one that is equal to {@code item}. A change requests
     * a redraw; listeners are not called.
     *
     * @param item an item of the list, or {@code null} to clear the selection
     * @throws IllegalArgumentException if {@code item} is not {@code null} and not in the list
     */
    public void setSelected(T item) {
        if (item == null) {
            setSelectedIndex(NO_SELECTION);
            return;
        }
        int index = items.indexOf(item);
        if (index == NO_SELECTION) {
            throw new IllegalArgumentException("The item is not in the list: " + item);
        }
        setSelectedIndex(index);
    }

    /**
     * Returns the most rows the open list shows before it scrolls.
     *
     * @return the row count, 1 or greater; {@link #DEFAULT_MAX_VISIBLE_ROWS} for a new box
     */
    public int getMaxVisibleRows() {
        return maxVisibleRows;
    }

    /**
     * Sets the most rows the open list shows before it scrolls. Takes effect the next time the
     * list opens. The list can show fewer rows when the surface has less room.
     *
     * @param rows row count; 1 or greater
     * @throws IllegalArgumentException if {@code rows} is less than 1
     */
    public void setMaxVisibleRows(int rows) {
        if (rows < 1) {
            throw new IllegalArgumentException("rows must be at least 1, was " + rows);
        }
        maxVisibleRows = rows;
    }

    /**
     * Returns how wide the open list is.
     *
     * @return the mode, never {@code null}; {@link SelectBoxListWidth#FIT_ENTRIES} for a new box
     */
    public SelectBoxListWidth getListWidth() {
        return listWidth;
    }

    /**
     * Sets how wide the open list is. Takes effect the next time the list opens.
     *
     * @param mode the mode; must not be {@code null}
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setListWidth(SelectBoxListWidth mode) {
        listWidth = Objects.requireNonNull(mode, "mode");
    }

    /**
     * Returns the looks, scroll bar, and arrow.
     *
     * @return the style, never {@code null}
     */
    public SelectBoxStyle getStyle() {
        return style;
    }

    /**
     * Replaces the looks, scroll bar, and arrow. The size stays as it is, so call {@link #pack()}
     * after a change of padding. A different style is marked by {@link #invalidate()} if a padding
     * changed, otherwise by {@link #requestRedraw()}. An equal style changes nothing. An open list
     * keeps its looks until it is opened again.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(SelectBoxStyle style) {
        Objects.requireNonNull(style, "style");
        if (style.equals(this.style)) {
            return;
        }
        boolean sizeAffected = style.getBoxStyle().getHorizontalPadding()
                != this.style.getBoxStyle().getHorizontalPadding()
                || style.getRowStyle().getHorizontalPadding()
                        != this.style.getRowStyle().getHorizontalPadding();
        this.style = style;
        if (sizeAffected) {
            invalidate();
        } else {
            requestRedraw();
        }
    }

    /**
     * Enables or disables the box. Disabling closes an open list. A change requests a redraw. A
     * disabled box ignores input and is not focusable. Disabling the box that owns keyboard focus
     * clears the focus the next time the {@link UiRoot} reads it or runs {@code update} or
     * {@code draw}, as for a hidden widget; enabling it again does not restore the focus.
     *
     * @param disabled {@code true} to disable the box
     */
    public void setDisabled(boolean disabled) {
        if (this.disabled == disabled) {
            return;
        }
        this.disabled = disabled;
        if (disabled) {
            list.close();
        }
        requestRedraw();
    }

    /**
     * Returns whether the box is disabled.
     *
     * @return {@code true} when disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Returns whether the hovering pointer is over the closed box, as reported by the
     * {@link UiRoot}. It is {@code false} while the list is open, because the list covers the box.
     *
     * @return {@code true} while the pointer is over the box
     */
    public boolean isPointerOver() {
        return pointerOver;
    }

    /**
     * Adds a listener that is called when the user picks an item that differs from the current
     * one, in the order the listeners were added. The box already shows the new item. Safe to call
     * from inside a listener; the change applies from the next pick.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addChangeListener(ChangeListener<? super SelectBox<T>> listener) {
        Objects.requireNonNull(listener, "listener");
        List<ChangeListener<? super SelectBox<T>>> grown = new ArrayList<>(changeListeners);
        grown.add(listener);
        changeListeners = List.copyOf(grown);
    }

    /**
     * Removes the first occurrence of a change listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeChangeListener(ChangeListener<? super SelectBox<T>> listener) {
        Objects.requireNonNull(listener, "listener");
        List<ChangeListener<? super SelectBox<T>>> shrunk = new ArrayList<>(changeListeners);
        if (shrunk.remove(listener)) {
            changeListeners = List.copyOf(shrunk);
        }
    }

    /**
     * Opens the list, as a press on the box does. Does nothing when the list is already open, the
     * box is disabled, has no items, or is not shown in the main tree, the window layer, or the
     * popup layer of its root. Safe to call while an event is being delivered.
     */
    public void openList() {
        list.open();
    }

    /**
     * Closes the open list without a change and gives the keyboard focus back to the box. Does
     * nothing when the list is closed. Safe to call while an event is being delivered.
     */
    public void closeList() {
        list.close();
    }

    /**
     * Returns whether the list is open.
     *
     * @return {@code true} while the list is in the popup layer
     */
    public boolean isListOpen() {
        return list.isOpen();
    }

    /**
     * Returns whether the box can take keyboard focus, which is the case unless it is disabled.
     *
     * @return {@code true} when enabled
     */
    @Override
    public boolean isFocusable() {
        return !disabled;
    }

    /**
     * Sets the width of the box. The height becomes 1 whatever is passed, and a negative
     * {@code height} does not throw.
     *
     * @param width width in UI cells; zero or greater
     * @param height ignored
     * @throws IllegalArgumentException if {@code width} is negative
     */
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, BOX_HEIGHT);
    }

    /**
     * Returns the width that fits the widest item, or the placeholder if that is wider: the
     * padding on both sides, the swatch if there is one, the text rounded up to whole UI cells,
     * and the arrow cell. It does not depend on the selection.
     *
     * @return the width in UI cells
     */
    @Override
    public int getPreferredWidth() {
        int widestColumns = Math.max(widestItemColumns, placeholderColumns);
        int textCells = SelectBoxEntryText.cellsOf(
                SelectBoxEntryText.prefixColumns(hasSwatches()) + widestColumns);
        return textCells + 2 * style.getBoxStyle().getHorizontalPadding() + ARROW_CELLS;
    }

    /**
     * Returns one row.
     *
     * @return 1 UI cell
     */
    @Override
    public int getPreferredHeight() {
        return BOX_HEIGHT;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        int width = getWidth();
        if (width == 0) {
            return;
        }
        LabelStyle look = currentLook();
        target.eraseGlyphs(left, top, width, BOX_HEIGHT);
        target.fillBackground(left, top, width, BOX_HEIGHT, look.getBackgroundRgba());
        drawChoice(target, look, left, top, width);
        drawArrow(target, look, left + width - ARROW_CELLS, top);
    }

    private LabelStyle currentLook() {
        ButtonStyle button = style.getBoxStyle();
        if (disabled) {
            return button.getDisabled();
        }
        if (list.isOpen()) {
            return button.getPressed();
        }
        if (pointerOver) {
            return button.getPointerOver();
        }
        return isFocusShown() ? button.getFocused() : button.getNormal();
    }

    private void drawChoice(UiSurface target, LabelStyle look, int left, int top, int width) {
        int padding = style.getBoxStyle().getHorizontalPadding();
        boolean swatchShown = hasSwatches();
        if (swatchShown && selectedIndex != NO_SELECTION) {
            SelectBoxEntryText.fillSwatch(target, left, top, padding,
                    itemSwatches[selectedIndex]);
        }
        String shownText = selectedIndex == NO_SELECTION ? placeholder
                : itemTexts[selectedIndex];
        SelectBoxEntryText.write(target,
                SelectBoxEntryText.firstTextColumn(left, padding, swatchShown), top,
                SelectBoxEntryText.textColumnRoom(width, padding, ARROW_CELLS, swatchShown),
                shownText, look.getForegroundRgba());
    }

    private void drawArrow(UiSurface target, LabelStyle look, int arrowColumn, int top) {
        if (style.getArrowFont() == MarkFont.SQUARE) {
            target.writeSquareGlyph(arrowColumn, top, style.getArrowGlyph(),
                    look.getForegroundRgba());
            return;
        }
        target.writeTextGlyph(UiSurface.TEXT_COLUMNS_PER_CELL * arrowColumn, top,
                style.getArrowGlyph(), look.getForegroundRgba());
    }

    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_ENTERED -> {
                setPointerOver(true);
                yield false;
            }
            case POINTER_EXITED -> {
                setPointerOver(false);
                yield false;
            }
            case POINTER_DOWN -> handlePointerDown(event);
            case KEY_DOWN -> handleKeyDown(event.getKeycode());
            default -> false;
        };
    }

    private boolean handlePointerDown(UiEvent event) {
        if (disabled || event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        list.open();
        return true;
    }

    private boolean handleKeyDown(int keycode) {
        boolean openKey = keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER
                || keycode == Input.Keys.SPACE;
        if (disabled || !openKey) {
            return false;
        }
        list.open();
        return true;
    }

    private void setPointerOver(boolean pointerOver) {
        if (this.pointerOver == pointerOver) {
            return;
        }
        this.pointerOver = pointerOver;
        requestRedraw();
    }

    private void refreshEntries() {
        int count = items.size();
        itemTexts = new String[count];
        itemSwatches = new int[count];
        widestItemColumns = 0;
        for (int index = 0; index < count; index++) {
            T item = items.get(index);
            String text = Objects.requireNonNull(itemTextFunction.apply(item), "item text");
            itemTexts[index] = text;
            widestItemColumns = Math.max(widestItemColumns, text.codePointCount(0, text.length()));
            itemSwatches[index] = itemSwatch == null ? 0 : itemSwatch.getSwatchRgba(item);
        }
        invalidate();
    }

    private void fireChanged() {
        List<ChangeListener<? super SelectBox<T>>> snapshot = changeListeners;
        for (int index = 0; index < snapshot.size(); index++) {
            snapshot.get(index).changed(this);
        }
    }

    // ---- used by the open list ----

    int getItemCount() {
        return items.size();
    }

    String getItemText(int index) {
        return itemTexts[index];
    }

    boolean hasSwatches() {
        return itemSwatch != null;
    }

    int getItemSwatch(int index) {
        return itemSwatches[index];
    }

    int getListEntryWidth() {
        int textCells = SelectBoxEntryText.cellsOf(
                SelectBoxEntryText.prefixColumns(hasSwatches()) + widestItemColumns);
        return textCells + 2 * style.getRowStyle().getHorizontalPadding();
    }

    boolean canShowList() {
        return !disabled && !items.isEmpty() && isInRootTrees();
    }

    // The user picked an item in the list, which is already closed.
    void pickItem(int index) {
        if (index == selectedIndex) {
            return;
        }
        selectedIndex = index;
        requestRedraw();
        fireChanged();
    }

    void handleListClosed() {
        if (canShowList()) {
            uiRoot.setKeyboardFocus(this);
        }
        requestRedraw();
    }

    private boolean isInRootTrees() {
        Widget top = this;
        while (top.getParent() != null) {
            top = top.getParent();
        }
        boolean inTree = top == uiRoot.getRoot() || top == uiRoot.getWindowLayer()
                || top == uiRoot.getPopupLayer();
        return inTree && isShown();
    }
}
