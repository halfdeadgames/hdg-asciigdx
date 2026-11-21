package com.github.halfdeadgames.asciigdx.ui;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A drop-down list of items that belongs to one title of a {@link MenuBar}. Create it with
 * {@link MenuBar#addMenu(String)} or {@link MenuBar#addMenu(String, char)}. Items are shown in the
 * order they were added, one text row each.
 *
 * <p>The title and the text of each item can be changed later with {@link #setTitle(String)} and
 * {@link #setItemText(int, String)}; their trigger letters stay.
 *
 * <p>A menu can be opened while it has no items; its drop-down is then empty. Items cannot be
 * removed. Several items may have the same trigger; typing the trigger then cycles through them
 * (see {@link MenuBar}).
 *
 * <p>Not thread-safe; use it from the render thread.
 */
public final class Menu {

    private record Item(String text, Runnable action, char trigger, int triggerIndex) {
    }

    private final MenuBar bar;
    private final char trigger;
    private String title;
    private int triggerIndex;
    private final ArrayList<Item> items = new ArrayList<>();

    Menu(MenuBar bar, String title, char trigger, int triggerIndex) {
        this.bar = bar;
        this.title = title;
        this.trigger = trigger;
        this.triggerIndex = triggerIndex;
    }

    /**
     * Returns the text shown in the bar.
     *
     * @return the title, never {@code null}; may be empty
     */
    public String getTitle() {
        return title;
    }

    /**
     * Replaces the text shown in the bar. The trigger letter of this menu, if it has one, stays;
     * the first case-insensitive match of it in the new title is the one drawn in the trigger
     * color. The bar lays out again, so the titles to the right move and
     * the bar widens if the titles no longer fit; it never narrows. If this menu is open, its
     * drop-down is moved to line up with the title again. Setting the same text changes nothing.
     *
     * <p>Call it on the render thread only. It is safe to call while this menu is open and from
     * inside an item action.
     *
     * @param title new text of the title, one text column per code point; must not be
     *         {@code null}; may be empty
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if this menu has a trigger letter that does not appear in
     *         {@code title} when compared without regard to case; the title is then unchanged
     */
    public void setTitle(String title) {
        Objects.requireNonNull(title, "title");
        if (title.equals(this.title)) {
            return;
        }
        int newTriggerIndex = MenuTrigger.requireCellOf(title, trigger);
        this.title = title;
        this.triggerIndex = newTriggerIndex;
        bar.refreshTitles();
    }

    /**
     * Adds an item without a trigger at the end of the list. If this menu is open, its drop-down
     * grows to fit the new item right away.
     *
     * <p>The action runs when the item is chosen, after the menu has closed and the keyboard focus
     * has been restored. It runs while an input event is being delivered, so calling a pointer or
     * key input method of the {@link UiRoot} from the action throws
     * {@link IllegalStateException}; defer such work to the next frame with a
     * {@link UiUpdateListener}.
     *
     * @param text text of the item, one text column per code point; must not be {@code null};
     *         may be empty
     * @param action code to run when the item is chosen; must not be {@code null}
     * @throws NullPointerException if {@code text} or {@code action} is {@code null}
     */
    public void addItem(String text, Runnable action) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(action, "action");
        items.add(new Item(text, action, MenuTrigger.NONE, MenuTrigger.NO_CELL));
        bar.refreshDropDown(this);
    }

    /**
     * Adds an item with a trigger letter at the end of the list. If this menu is open, its
     * drop-down grows to fit the new item right away.
     *
     * <p>While this menu is open, typing the trigger, with or without Alt, chooses the item; see
     * {@link MenuBar} for the keyboard rules. The first case-insensitive match of the trigger in
     * the text is drawn in the style's trigger color, if it has one.
     *
     * <p>The action runs when the item is chosen, after the menu has closed and the keyboard focus
     * has been restored. It runs while an input event is being delivered, so calling a pointer or
     * key input method of the {@link UiRoot} from the action throws
     * {@link IllegalStateException}; defer such work to the next frame with a
     * {@link UiUpdateListener}.
     *
     * @param text text of the item, one text column per code point; must not be {@code null};
     *         may be empty
     * @param trigger an ASCII letter (A-Z or a-z) or digit (0-9) that appears in {@code text};
     *         matching ignores case
     * @param action code to run when the item is chosen; must not be {@code null}
     * @throws NullPointerException if {@code text} or {@code action} is {@code null}
     * @throws IllegalArgumentException if {@code trigger} is not an ASCII letter or digit, or does
     *         not appear in {@code text} when compared without regard to case
     */
    public void addItem(String text, char trigger, Runnable action) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(action, "action");
        int cell = MenuTrigger.requireCellOf(text, trigger);
        items.add(new Item(text, action, trigger, cell));
        bar.refreshDropDown(this);
    }

    /**
     * Returns the number of items.
     *
     * @return the item count, zero or greater
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Replaces the text of one item. The item keeps its position, its action, and its trigger
     * letter, if it has one. The first case-insensitive match of the trigger letter in the new
     * text is the one drawn in the trigger color. If this menu is open, its drop-down width
     * follows the widest item right away, so it can grow or shrink, and a highlighted item stays
     * highlighted. Setting the same text changes nothing.
     *
     * <p>Call it on the render thread only. It is safe to call while this menu is open and from
     * inside an item action.
     *
     * @param index position of the item, from 0 to {@code getItemCount() - 1}
     * @param text new text of the item, one text column per code point; must not be {@code null};
     *         may be empty
     * @throws NullPointerException if {@code text} is {@code null}; this is checked before
     *         {@code index}, so a {@code null} text with a bad index throws this exception
     * @throws IndexOutOfBoundsException if {@code index} is outside that range
     * @throws IllegalArgumentException if the item has a trigger letter that does not appear in
     *         {@code text} when compared without regard to case; the item is then unchanged
     */
    public void setItemText(int index, String text) {
        Objects.requireNonNull(text, "text");
        Objects.checkIndex(index, items.size());
        Item item = items.get(index);
        if (text.equals(item.text())) {
            return;
        }
        int newTriggerIndex = MenuTrigger.requireCellOf(text, item.trigger());
        items.set(index, new Item(text, item.action(), item.trigger(), newTriggerIndex));
        bar.refreshDropDown(this);
    }

    /**
     * Returns the current text of one item, as set by {@code addItem} or
     * {@link #setItemText(int, String)}.
     *
     * @param index position of the item, from 0 to {@code getItemCount() - 1}
     * @return the item text, never {@code null}; may be empty
     * @throws IndexOutOfBoundsException if {@code index} is outside that range
     */
    public String getItemText(int index) {
        return items.get(index).text();
    }

    Runnable getItemAction(int index) {
        return items.get(index).action();
    }

    // Index of the trigger letter among the code points of the item text, or MenuTrigger.NO_CELL.
    int getItemTriggerIndex(int index) {
        return items.get(index).triggerIndex();
    }

    char getTrigger() {
        return trigger;
    }

    // Index of the trigger letter among the code points of the title, or MenuTrigger.NO_CELL.
    int getTriggerIndex() {
        return triggerIndex;
    }

    int countItemsWithTrigger(char key) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            if (MenuTrigger.matches(items.get(i).trigger(), key)) {
                count++;
            }
        }
        return count;
    }

    /** Returns the first item after {@code after} (wrapping around) with the trigger, or -1. */
    int findItemWithTrigger(char key, int after) {
        int size = items.size();
        for (int step = 1; step <= size; step++) {
            int index = Math.floorMod(after + step, size);
            if (MenuTrigger.matches(items.get(index).trigger(), key)) {
                return index;
            }
        }
        return -1;
    }

    int getWidestItemLength() {
        int widest = 0;
        for (int i = 0; i < items.size(); i++) {
            String text = items.get(i).text();
            widest = Math.max(widest, text.codePointCount(0, text.length()));
        }
        return widest;
    }
}
