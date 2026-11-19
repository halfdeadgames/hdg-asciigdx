package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link MenuBar}: one {@link ButtonStyle} for the titles in the bar and one for the
 * items in a drop-down. Immutable, so one instance can be shared by any number of menu bars. Both
 * styles use the same font, so the bar and its drop-downs have the same row height.
 *
 * <p>Only the normal, pointer-over, and pressed looks and the horizontal padding of the two styles
 * are used; menus have no disabled state, so the disabled looks are ignored.
 *
 * <p>A style may also have a trigger color, set with {@link #withTriggerRgba(int)}. The trigger
 * letter of a title or item is then drawn in that foreground color in every look. Without it the
 * trigger letter looks like the other letters.
 */
public final class MenuBarStyle {

    private final ButtonStyle titles;
    private final ButtonStyle items;
    private final boolean hasTriggerRgba;
    private final int triggerRgba;

    private MenuBarStyle(ButtonStyle titles, ButtonStyle items, boolean hasTriggerRgba,
            int triggerRgba) {
        this.titles = titles;
        this.items = items;
        this.hasTriggerRgba = hasTriggerRgba;
        this.triggerRgba = triggerRgba;
    }

    /**
     * Creates a style without a trigger color.
     *
     * @param titles looks and padding of the titles in the bar; must not be {@code null}
     * @param items looks and padding of the items in a drop-down; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the two styles do not use the same font
     */
    public static MenuBarStyle of(ButtonStyle titles, ButtonStyle items) {
        Objects.requireNonNull(titles, "titles");
        Objects.requireNonNull(items, "items");
        if (titles.getFont() != items.getFont()) {
            throw new IllegalArgumentException("Titles and items must use the same font, were "
                    + titles.getFont() + " and " + items.getFont());
        }
        return new MenuBarStyle(titles, items, false, 0);
    }

    /**
     * Returns a new style with a trigger color, replacing any earlier one. Only the foreground of
     * the trigger letter changes, in every look; its background never does.
     *
     * @param rgba foreground color of trigger letters as RGBA8888, with alpha as in
     *         {@link LabelStyle}
     * @return a style with the same looks and that trigger color
     */
    public MenuBarStyle withTriggerRgba(int rgba) {
        return new MenuBarStyle(titles, items, true, rgba);
    }

    /**
     * Returns the looks and padding of the titles in the bar.
     *
     * @return the style, never {@code null}
     */
    public ButtonStyle getTitles() {
        return titles;
    }

    /**
     * Returns the looks and padding of the items in a drop-down.
     *
     * @return the style, never {@code null}
     */
    public ButtonStyle getItems() {
        return items;
    }

    /**
     * Returns the font shared by the titles and the items.
     *
     * @return the font, never {@code null}
     */
    public UiFont getFont() {
        return titles.getFont();
    }

    /**
     * Returns whether trigger letters have their own color.
     *
     * @return {@code true} after {@link #withTriggerRgba(int)}; {@code false} for a new style
     */
    public boolean hasTriggerRgba() {
        return hasTriggerRgba;
    }

    /**
     * Returns the foreground color of trigger letters.
     *
     * @return the color as RGBA8888
     * @throws IllegalStateException if the style has no trigger color; check
     *         {@link #hasTriggerRgba()} first
     */
    public int getTriggerRgba() {
        if (!hasTriggerRgba) {
            throw new IllegalStateException("This style has no trigger color");
        }
        return triggerRgba;
    }

    int getRowHeight() {
        return getFont() == UiFont.TALL ? 2 : 1;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MenuBarStyle)) {
            return false;
        }
        MenuBarStyle that = (MenuBarStyle) other;
        return titles.equals(that.titles) && items.equals(that.items)
                && hasTriggerRgba == that.hasTriggerRgba
                && (!hasTriggerRgba || triggerRgba == that.triggerRgba);
    }

    @Override
    public int hashCode() {
        return hasTriggerRgba ? Objects.hash(titles, items, triggerRgba)
                : Objects.hash(titles, items);
    }

    @Override
    public String toString() {
        String trigger = hasTriggerRgba ? Integer.toHexString(triggerRgba) : "none";
        return "MenuBarStyle[titles=" + titles + ", items=" + items + ", triggerRgba=" + trigger
                + "]";
    }
}
