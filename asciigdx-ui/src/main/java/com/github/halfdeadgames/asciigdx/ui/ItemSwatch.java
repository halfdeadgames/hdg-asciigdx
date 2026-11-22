package com.github.halfdeadgames.asciigdx.ui;

/**
 * Tells a {@link SelectBox} which color to show in the small swatch in front of an item, set with
 * {@link SelectBox#setItemSwatch(ItemSwatch)}.
 *
 * <p>Example:
 * <pre>{@code
 * ItemSwatch<Team> swatch = team -> team.getColorRgba();
 * }</pre>
 *
 * @param <T> the type of the select box items
 */
@FunctionalInterface
public interface ItemSwatch<T> {

    /**
     * Returns the swatch color of an item. The select box calls it once for each item when the
     * swatch function or the items are set, not while drawing, so the color is not read again when
     * the item changes later.
     *
     * @param item the item; never {@code null}
     * @return the color as RGBA8888, with alpha as in {@link LabelStyle}
     */
    int getSwatchRgba(T item);
}
