package com.github.halfdeadgames.asciigdx.ui;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A drop-down list of items that belongs to one title of a {@link MenuBar}. Create it with
 * {@link MenuBar#addMenu(String)} or {@link MenuBar#addMenu(String, char)}. Items are shown in the
 * order they were added, one text row each.
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
    private final String title;
    private final char trigger;
    private final int triggerIndex;
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

    String getItemText(int index) {
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
