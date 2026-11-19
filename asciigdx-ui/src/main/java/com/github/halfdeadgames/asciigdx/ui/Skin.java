package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.files.FileHandle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Named colors and widget styles read from a JSON skin file. Immutable, so one skin can be shared
 * by any number of widgets and scenes. Load it once, then look styles up by name.
 *
 * <pre>{@code
 * Skin skin = Skin.fromFile(Gdx.files.internal("default-skin.json"));
 * Label title = new Label("Welcome", skin.getLabelStyle("title"));
 * TextButton start = new TextButton("Start", skin.getButtonStyle("action"));
 * int panelRgba = skin.getRgba("deepBlue");
 * }</pre>
 *
 * <p>The file format: the file is one JSON object. It may have the sections {@code colors},
 * {@code labels}, {@code buttons}, and {@code menuBars}; all are optional, and any other
 * top-level key is an error. The sections are read in that order, and a section can only refer
 * to names defined in an earlier one, so references never form a cycle. Inside a section every
 * name is unique. Each section is an object that maps a style name to its value.
 *
 * <pre>{@code
 * {
 *   "colors": {
 *     "white": "#FFFFFF",
 *     "deepBlue": "#203070",
 *     "transparent": "#00000000"
 *   },
 *   "labels": {
 *     "title": { "font": "tall", "foreground": "white", "background": "deepBlue" }
 *   },
 *   "buttons": {
 *     "action": {
 *       "normal": "title",
 *       "pointerOver": { "font": "tall", "foreground": "#000000", "background": "#00A0A0" },
 *       "horizontalPadding": 2
 *     }
 *   },
 *   "menuBars": {
 *     "main": {
 *       "titles": "action",
 *       "items": "action",
 *       "triggerColor": "#FF40C0",
 *       "triggerUnderline": true
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Names are free text. Naming colors after how they look, such as {@code "deepBlue"}, and
 * styles after the job they do, such as {@code "title"} or {@code "tooltip"}, lets a style keep
 * its name when its colors change.
 *
 * <p>A color value is {@code "#RRGGBB"} (fully opaque) or {@code "#RRGGBBAA"}, with hex digits in
 * either case, or the name of an entry in {@code colors}. A value that starts with {@code #} is
 * always hex. Entries in {@code colors} must themselves be hex.
 *
 * <p>A {@code labels} entry has the required keys {@code font} ({@code "tall"} or
 * {@code "square"}), {@code foreground}, and {@code background}.
 *
 * <p>A {@code buttons} entry has the required key {@code normal}, the optional keys
 * {@code pointerOver}, {@code pressed}, and {@code disabled}, which copy {@code normal} when
 * missing, the optional look {@code focused}, which copies {@code pointerOver} when missing, and
 * the optional whole number {@code horizontalPadding}, zero or greater, which
 * defaults to {@link ButtonStyle#DEFAULT_HORIZONTAL_PADDING}. Each look is either the name of a
 * label style or an inline label object. All five looks must use the same font.
 *
 * <p>A {@code menuBars} entry has the required keys {@code titles} and {@code items}, each either
 * the name of a button style or an inline button object, the optional color {@code triggerColor},
 * and the optional boolean {@code triggerUnderline}, which defaults to {@code false}. Titles and
 * items must use the same font.
 *
 * <p>A missing {@code triggerColor} means the menu bar style has no trigger color, so
 * {@link MenuBarStyle#hasTriggerRgba()} returns {@code false}.
 *
 * <p>Standard JSON always works. The reader also accepts some relaxed forms (comments, unquoted
 * words), but those are not guaranteed to keep loading in later versions.
 *
 * <p>Unknown keys and sections are errors, so a skin file that uses keys or sections added in a
 * later version needs at least that library version.
 *
 * <p>Every problem in the text is reported as a {@link SkinFileException} whose message names the
 * place, such as {@code buttons.action.normal}.
 *
 * <p>All parameters and return values in this class are non-null.
 */
public final class Skin {

    private final Map<String, Integer> colors;
    private final Map<String, LabelStyle> labelStyles;
    private final Map<String, ButtonStyle> buttonStyles;
    private final Map<String, MenuBarStyle> menuBarStyles;

    Skin(Map<String, Integer> colors, Map<String, LabelStyle> labelStyles,
            Map<String, ButtonStyle> buttonStyles, Map<String, MenuBarStyle> menuBarStyles) {
        this.colors = copyOf(colors);
        this.labelStyles = copyOf(labelStyles);
        this.buttonStyles = copyOf(buttonStyles);
        this.menuBarStyles = copyOf(menuBarStyles);
    }

    /**
     * Reads a skin from skin file text. Needs no running application. A leading byte order mark
     * (U+FEFF) is skipped.
     *
     * @param json the text of a skin file, see the class description for the format; must not be
     *         {@code null}
     * @return the skin, never {@code null}
     * @throws NullPointerException if {@code json} is {@code null}
     * @throws SkinFileException if the text is not valid JSON or breaks any rule of the format
     */
    public static Skin fromJson(String json) {
        Objects.requireNonNull(json, "json");
        return SkinFileReader.read(withoutByteOrderMark(json));
    }

    /**
     * Reads a skin from a file, decoded as UTF-8. A leading byte order mark (U+FEFF) is skipped.
     * The only libGDX service it uses is the handle itself, so it needs {@code Gdx.files} only if
     * the handle came from there.
     *
     * @param file the skin file, see the class description for the format; must not be
     *         {@code null}
     * @return the skin, never {@code null}
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws SkinFileException if the text is not valid JSON or breaks any rule of the format;
     *         the message starts with the path of the file and a colon
     * @throws com.badlogic.gdx.utils.GdxRuntimeException if the file cannot be read; read errors
     *         from the handle propagate unchanged
     */
    public static Skin fromFile(FileHandle file) {
        Objects.requireNonNull(file, "file");
        String text = file.readString("UTF-8");
        try {
            return fromJson(text);
        } catch (SkinFileException problem) {
            throw new SkinFileException(file.path() + ": " + problem.getMessage());
        }
    }

    /**
     * Returns the label style with a name.
     *
     * @param name name in the {@code labels} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasLabelStyle(String)} first.
     */
    public LabelStyle getLabelStyle(String name) {
        return lookUp(labelStyles, name, "label style");
    }

    /**
     * Returns the button style with a name.
     *
     * @param name name in the {@code buttons} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasButtonStyle(String)} first.
     */
    public ButtonStyle getButtonStyle(String name) {
        return lookUp(buttonStyles, name, "button style");
    }

    /**
     * Returns the menu bar style with a name.
     *
     * @param name name in the {@code menuBars} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasMenuBarStyle(String)} first.
     */
    public MenuBarStyle getMenuBarStyle(String name) {
        return lookUp(menuBarStyles, name, "menu bar style");
    }

    /**
     * Returns the color with a name.
     *
     * @param name name in the {@code colors} section; must not be {@code null}
     * @return the color packed as 0xRRGGBBAA (red in the highest byte); entries written as
     *         #RRGGBB get alpha 0xFF
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such color; the message lists the known
     *         names. Check with {@link #hasColor(String)} first.
     */
    public int getRgba(String name) {
        return lookUp(colors, name, "color");
    }

    /**
     * Returns whether the skin defines a label style with a name.
     *
     * @param name name in the {@code labels} section; must not be {@code null}
     * @return {@code true} if {@link #getLabelStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasLabelStyle(String name) {
        return contains(labelStyles, name);
    }

    /**
     * Returns whether the skin defines a button style with a name.
     *
     * @param name name in the {@code buttons} section; must not be {@code null}
     * @return {@code true} if {@link #getButtonStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasButtonStyle(String name) {
        return contains(buttonStyles, name);
    }

    /**
     * Returns whether the skin defines a menu bar style with a name.
     *
     * @param name name in the {@code menuBars} section; must not be {@code null}
     * @return {@code true} if {@link #getMenuBarStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasMenuBarStyle(String name) {
        return contains(menuBarStyles, name);
    }

    /**
     * Returns whether the skin defines a color with a name.
     *
     * @param name name in the {@code colors} section; must not be {@code null}
     * @return {@code true} if {@link #getRgba(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasColor(String name) {
        return contains(colors, name);
    }

    private static boolean contains(Map<String, ?> namedValues, String name) {
        Objects.requireNonNull(name, "name");
        return namedValues.containsKey(name);
    }

    private static String withoutByteOrderMark(String text) {
        if (!text.isEmpty() && text.charAt(0) == '﻿') {
            return text.substring(1);
        }
        return text;
    }

    @Override
    public String toString() {
        return "Skin[colors=" + colors.size() + ", labelStyles=" + labelStyles.size()
                + ", buttonStyles=" + buttonStyles.size() + ", menuBarStyles="
                + menuBarStyles.size() + "]";
    }

    static String describeNames(Map<String, ?> namedValues) {
        if (namedValues.isEmpty()) {
            return "none";
        }
        return String.join(", ", namedValues.keySet());
    }

    private static <T> T lookUp(Map<String, T> namedValues, String name, String kind) {
        Objects.requireNonNull(name, "name");
        T value = namedValues.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + kind + " \"" + name + "\". Known "
                    + kind + "s: " + describeNames(namedValues));
        }
        return value;
    }

    private static <T> Map<String, T> copyOf(Map<String, T> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
