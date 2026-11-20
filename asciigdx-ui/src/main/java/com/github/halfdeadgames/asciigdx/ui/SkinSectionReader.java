package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.utils.JsonValue;

/**
 * Reads the entries of one custom skin file section into style objects. Register it with
 * {@link SkinLoader#withSection(String, SkinSectionReader)}.
 *
 * <p>The loader calls {@link #readEntry} once for every entry of the section, in file order, on
 * the thread that loads the skin. Custom sections are read after all built-in sections, so an
 * entry can use named colors and label styles through the {@link SkinEntryContext}.
 *
 * <p>Example:
 * <pre>{@code
 * SkinSectionReader<StarStyle> reader = (entry, context) -> {
 *     if (!entry.isObject()) {
 *         throw context.error("must be an object");
 *     }
 *     return new StarStyle(context.readLook(entry.get("normal")));
 * };
 * }</pre>
 *
 * @param <T> the style type this reader produces
 */
@FunctionalInterface
public interface SkinSectionReader<T> {

    /**
     * Reads one entry of the section.
     *
     * @param entry the entry's JSON value; its {@code name} field is the style name. Do not keep a
     *         reference to it after returning.
     * @param context helps read colors and looks and build errors for this entry; valid only
     *         during this call
     * @return the style for this entry; never {@code null}. A {@code null} makes loading throw a
     *         {@link SkinFileException} that names the entry.
     * @throws SkinFileException for a bad entry, preferably built with
     *         {@link SkinEntryContext#error(String)} so the message names the entry
     */
    T readEntry(JsonValue entry, SkinEntryContext context);
}
