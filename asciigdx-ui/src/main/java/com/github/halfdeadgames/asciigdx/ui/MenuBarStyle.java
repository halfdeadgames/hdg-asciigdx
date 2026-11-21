package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link MenuBar}: one {@link ButtonStyle} for the titles in the bar and one for the
 * items in a drop-down. Immutable, so one instance can be shared by any number of menu bars.
 * Titles and items are words, so they are drawn in the text font.
 *
 * <p>Only the normal, pointer-over, and pressed looks and the horizontal padding of the two styles
 * are used; menus have no disabled state, so the disabled looks are ignored.
 *
 * <p>A style may also have a trigger color, set with {@link #withTriggerRgba(int)}. The trigger
 * letter of a title or item is then drawn in that foreground color in every look. Without it the
 * trigger letter looks like the other letters.
 *
 * <p>A style may also underline trigger letters, set with {@link #withTriggerUnderline(boolean)}.
 * The underline is drawn in the surface's overlay grid and is off by default.
 */
public final class MenuBarStyle {

    private final ButtonStyle titles;
    private final ButtonStyle items;
    private final boolean hasTriggerRgba;
    private final int triggerRgba;

    private final boolean triggerUnderline;

    private MenuBarStyle(ButtonStyle titles, ButtonStyle items, boolean hasTriggerRgba,
            int triggerRgba, boolean triggerUnderline) {
        this.titles = titles;
        this.items = items;
        this.hasTriggerRgba = hasTriggerRgba;
        this.triggerRgba = triggerRgba;
        this.triggerUnderline = triggerUnderline;
    }

    /**
     * Creates a style without a trigger color.
     *
     * @param titles looks and padding of the titles in the bar; must not be {@code null}
     * @param items looks and padding of the items in a drop-down; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if an argument is {@code null}
     */
    public static MenuBarStyle of(ButtonStyle titles, ButtonStyle items) {
        Objects.requireNonNull(titles, "titles");
        Objects.requireNonNull(items, "items");
        return new MenuBarStyle(titles, items, false, 0, false);
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
        return new MenuBarStyle(titles, items, true, rgba, triggerUnderline);
    }

    /**
     * Returns a new style that does or does not underline trigger letters, replacing any earlier
     * setting. The underline is an underscore glyph drawn in the surface's overlay, in the trigger
     * color if the style has one and otherwise in the foreground of the look being drawn. This
     * style is not changed. The underline is U+005F from the text font, so that font must hold
     * it. It sits on the same row as the text, under the trigger letter's text column. It shows
     * only on a surface that was added to a layer stack with {@link UiSurface#addTo}.
     *
     * @param underline {@code true} to underline trigger letters
     * @return a style with the same looks and trigger color and that underline setting
     */
    public MenuBarStyle withTriggerUnderline(boolean underline) {
        return new MenuBarStyle(titles, items, hasTriggerRgba, triggerRgba, underline);
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

    /**
     * Returns whether trigger letters are underlined.
     *
     * @return {@code true} after {@code withTriggerUnderline(true)}; {@code false} for a new style
     */
    public boolean isTriggerUnderline() {
        return triggerUnderline;
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
                && triggerUnderline == that.triggerUnderline
                && hasTriggerRgba == that.hasTriggerRgba
                && (!hasTriggerRgba || triggerRgba == that.triggerRgba);
    }

    @Override
    public int hashCode() {
        return hasTriggerRgba ? Objects.hash(titles, items, triggerUnderline, triggerRgba)
                : Objects.hash(titles, items, triggerUnderline);
    }

    @Override
    public String toString() {
        String trigger = hasTriggerRgba ? Integer.toHexString(triggerRgba) : "none";
        return "MenuBarStyle[titles=" + titles + ", items=" + items
                + ", triggerUnderline=" + triggerUnderline + ", triggerRgba=" + trigger + "]";
    }
}
