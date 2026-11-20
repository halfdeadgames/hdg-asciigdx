package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.files.FileHandle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Reads skin files, optionally with custom sections for widgets that the library does not know.
 * Immutable: {@link #withSection} returns a new loader, so one loader can be shared and extended
 * freely. Its plain form behaves exactly like {@link Skin#fromJson} and {@link Skin#fromFile}.
 *
 * <p>Example:
 * <pre>{@code
 * SkinLoader loader = SkinLoader.create()
 *         .withSection("starRatings", StarRatingStyle::read);
 * Skin skin = loader.fromFile(Gdx.files.internal("skin.json"));
 * StarRatingStyle style = skin.getCustomStyle("starRatings", "default", StarRatingStyle.class);
 * }</pre>
 *
 * <p>Custom sections are read after all built-in sections, in the order they were registered, so
 * their entries can refer to named colors and label styles. A section that is registered but
 * missing from a file yields no styles. A top-level key that is neither built in nor registered is
 * an error, as in {@link Skin}.
 *
 * <p>A loader holds no mutable state, so any thread may use it. The registered readers run on the
 * thread that calls {@code fromJson} or {@code fromFile}.
 */
public final class SkinLoader {

    private static final char BYTE_ORDER_MARK = (char) 0xFEFF;

    private final Map<String, SkinSectionReader<?>> readers;

    private SkinLoader(Map<String, SkinSectionReader<?>> readers) {
        this.readers = Collections.unmodifiableMap(readers);
    }

    /**
     * Creates a loader with no custom sections.
     *
     * @return a new loader that reads the built-in sections only
     */
    public static SkinLoader create() {
        return new SkinLoader(new LinkedHashMap<>());
    }

    /**
     * Returns a new loader that also reads a custom section. This loader is unchanged, so use the
     * returned loader.
     *
     * @param sectionName the top-level key of the section in the skin file; must not be
     *         {@code null}, and must be neither a built-in section ({@code colors},
     *         {@code labels}, {@code buttons}, {@code menuBars}, {@code checkBoxes},
     *         {@code radioButtons}, {@code textFields}, {@code progressBars}, {@code sliders})
     *         nor already registered on this loader
     * @param reader reads each entry of the section; must not be {@code null}
     * @return a new loader with the section added
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the name is built in or already registered
     */
    public SkinLoader withSection(String sectionName, SkinSectionReader<?> reader) {
        Objects.requireNonNull(sectionName, "sectionName");
        Objects.requireNonNull(reader, "reader");
        if (SkinFileReader.isBuiltInSection(sectionName)) {
            throw new IllegalArgumentException(
                    "\"" + sectionName + "\" is a built-in section and cannot be replaced");
        }
        if (readers.containsKey(sectionName)) {
            throw new IllegalArgumentException(
                    "The section \"" + sectionName + "\" is already registered");
        }
        Map<String, SkinSectionReader<?>> extended = new LinkedHashMap<>(readers);
        extended.put(sectionName, reader);
        return new SkinLoader(extended);
    }

    /**
     * Reads a skin from skin file text, with the registered custom sections. Needs no running
     * application. A leading byte order mark (U+FEFF) is skipped.
     *
     * @param json the text of a skin file, see {@link Skin} for the format; must not be
     *         {@code null}
     * @return the skin, never {@code null}
     * @throws NullPointerException if {@code json} is {@code null}
     * @throws SkinFileException if the text is not valid JSON or breaks any rule of the format
     */
    public Skin fromJson(String json) {
        Objects.requireNonNull(json, "json");
        return SkinFileReader.read(withoutByteOrderMark(json), readers);
    }

    /**
     * Reads a skin from a file, decoded as UTF-8, with the registered custom sections. A leading
     * byte order mark (U+FEFF) is skipped. The only libGDX service it uses is the handle itself,
     * so it needs {@code Gdx.files} only if the handle came from there.
     *
     * @param file the skin file, see {@link Skin} for the format; must not be {@code null}
     * @return the skin, never {@code null}
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws SkinFileException if the text is not valid JSON or breaks any rule of the format;
     *         the message starts with the path of the file and a colon
     * @throws com.badlogic.gdx.utils.GdxRuntimeException if the file cannot be read; read errors
     *         from the handle propagate unchanged
     */
    public Skin fromFile(FileHandle file) {
        Objects.requireNonNull(file, "file");
        String text = file.readString("UTF-8");
        try {
            return fromJson(text);
        } catch (SkinFileException problem) {
            throw new SkinFileException(file.path() + ": " + problem.getMessage());
        }
    }

    private static String withoutByteOrderMark(String text) {
        if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
            return text.substring(1);
        }
        return text;
    }
}
