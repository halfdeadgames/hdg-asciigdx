package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.files.FileHandle;
import com.github.halfdeadgames.asciigdx.text.ColorNames;
import com.github.halfdeadgames.asciigdx.zone.EffectRegistry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

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
 * {@code labels}, {@code buttons}, {@code menuBars}, {@code checkBoxes}, {@code radioButtons},
 * {@code textFields}, {@code progressBars}, {@code sliders}, {@code scrollPanes},
 * {@code selectBoxes}, {@code windows}, and {@code dialogs}; all are optional, and any other top-level key is an error. The sections are
 * read in that order, and a section can only refer to names defined in an earlier one, so
 * references never form a cycle. Inside a section every name is unique. Each section is an
 * object that maps a style name to its value.
 *
 * <pre>{@code
 * {
 *   "colors": {
 *     "white": "#FFFFFF",
 *     "deepBlue": "#203070",
 *     "transparent": "#00000000"
 *   },
 *   "labels": {
 *     "title": { "foreground": "white", "background": "deepBlue" }
 *   },
 *   "buttons": {
 *     "action": {
 *       "normal": "title",
 *       "pointerOver": { "foreground": "#000000", "background": "#00A0A0" },
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
 *   },
 *   "checkBoxes": {
 *     "option": { "button": "action", "unchecked": "[ ]", "checked": "[x]", "gap": 1 }
 *   },
 *   "radioButtons": {
 *     "choice": { "button": "action", "unchecked": "( )", "checked": "(*)" }
 *   },
 *   "textFields": {
 *     "input": {
 *       "normal": "title",
 *       "caret": { "glyph": "▏", "foreground": "white", "blinkSeconds": 0.5 }
 *     }
 *   },
 *   "progressBars": {
 *     "loading": { "fill": "title", "empty": "title" }
 *   },
 *   "sliders": {
 *     "volume": {
 *       "track": { "normal": "title" },
 *       "knob": { "normal": "title", "focused": "title" },
 *       "trackGlyph": "-", "knobGlyph": "#"
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
 * <p>A {@code labels} entry has the required keys {@code foreground} and {@code background}. A
 * look has no font: the UI picks the font by job, so words (labels, button text, titles, menu
 * text, text field text) use the text font and drawn parts (borders, marks, bars, arrows) use the
 * square font. A look that still has the old {@code font} key is an error that says so.
 *
 * <p>A {@code buttons} entry has the required key {@code normal}, the optional keys
 * {@code pointerOver}, {@code pressed}, and {@code disabled}, which copy {@code normal} when
 * missing, the optional look {@code focused}, which copies {@code pointerOver} when missing, and
 * the optional whole number {@code horizontalPadding}, zero or greater, which
 * defaults to {@link ButtonStyle#DEFAULT_HORIZONTAL_PADDING}. Each look is either the name of a
 * label style or an inline label object.
 *
 * <p>A {@code menuBars} entry has the required keys {@code titles} and {@code items}, each either
 * the name of a button style or an inline button object, the optional color {@code triggerColor},
 * and the optional boolean {@code triggerUnderline}, which defaults to {@code false}.
 *
 * <p>A {@code checkBoxes} entry and a {@code radioButtons} entry have the same keys and both
 * become a {@link ToggleStyle}, so the two kinds of style are interchangeable. The keys:
 * <ul>
 *   <li>{@code button}, required: the name of a button style or an inline button object. It gives
 *       the text looks per state and the horizontal padding.
 *   <li>{@code unchecked}, required: the mark shown while unchecked, text of at least one code
 *       point.
 *   <li>{@code checked}, required: the mark shown while checked, with the same number of code
 *       points as {@code unchecked}.
 *   <li>{@code gap}, optional: a whole number of text columns (half a UI cell), zero or greater,
 *       between the mark and the text; defaults to {@link ToggleStyle#DEFAULT_GAP}.
 *   <li>{@code markFont}, optional: the font of the marks, {@code "text"} (one text column per
 *       code point) or {@code "square"} (one UI cell per code point), in lowercase; defaults to
 *       {@code "text"}. A value that is not exactly {@code "text"} or {@code "square"}, including
 *       a non-string or another letter case, fails with a {@link SkinFileException} that names
 *       the key and lists the allowed values. The gap and the words stay in text columns
 *       whichever font the mark uses.
 * </ul>
 * Example entry: {@code "option": { "button": "action", "unchecked": "[ ]", "checked": "[x]" }}.
 * With a square mark: {@code "option": { "button": "action", "unchecked": "[ ]",
 * "checked": "[x]", "markFont": "square" }}.
 *
 * <p>A {@code textFields} entry becomes a {@link TextFieldStyle}. The keys:
 * <ul>
 *   <li>{@code normal}, required: the name of a label style or an inline label object.
 *   <li>{@code focused}, {@code disabled}, and {@code placeholder}, optional looks of the same
 *       kind; each copies {@code normal} when missing.
 *   <li>{@code caret}, optional: an object with the required key {@code glyph} (text of exactly
 *       one code point, drawn in the text font), the optional {@code foreground} (a color; the
 *       field's text color when left out), and the optional {@code blinkSeconds} (a number, zero
 *       or greater, default 0 for a steady caret). A caret has no background; the old
 *       {@code background} key is an error that says so. When {@code caret} is missing, the
 *       caret is a steady {@code U+258F} bar in the normal look's foreground.
 * </ul>
 * Bar caret: {@code "caret": { "glyph": "▏", "foreground": "white", "blinkSeconds": 0.5 }}.
 * Underline caret: {@code "caret": { "glyph": "_", "blinkSeconds": 0.5 }}. The caret does not
 * recolor the text. The caret is an inline object
 * today; a section of named carets may be added in a later version, and files written with inline
 * caret objects will keep loading.
 *
 * <p>A {@code progressBars} entry has the required keys {@code fill} and {@code empty}. Each is
 * either the name of a label style or an inline label object. The foreground of {@code fill}
 * colors the filled part, and the background of {@code empty} colors the empty part;
 * the other two colors are unused. Example:
 * {@code "loading": { "fill": "accent", "empty": "text" }}.
 *
 * <p>A {@code sliders} entry has the keys below. Each look is either the name of a label style or
 * an inline label object.
 * <ul>
 *   <li>{@code track}, required: an object with the required look {@code normal} and the optional
 *       looks {@code focused} and {@code disabled} of the track cells; a missing look copies
 *       {@code normal}.
 *   <li>{@code knob}, required: an object with the same keys for the knob cell.
 *   <li>{@code trackGlyph} and {@code knobGlyph}, optional but only together: text of exactly one
 *       code point each, drawn in every track cell and in the knob cell. Without them the style
 *       has no glyphs of its own, and a slider uses the defaults of {@link SliderStyle}: a line
 *       for the track (horizontal or vertical, by the slider's orientation) and a full block for
 *       the knob.
 * </ul>
 * Example: {@code "volume": { "track": { "normal": "text" }, "knob": { "normal": "accent" } }}.
 *
 * <p>A {@code scrollPanes} entry becomes a {@link ScrollPaneStyle}. The keys are colors:
 * {@code track} and {@code thumb}, required, and the optional {@code thumbPointerOver} and
 * {@code thumbPressed}, which copy {@code thumb} when missing. The optional {@code stepButtons}
 * ({@code true} shows an arrow in the first and last cell of each bar; {@code false} when
 * missing), {@code stepArrow}, {@code stepArrowPointerOver}, and {@code stepArrowPressed} (colors
 * that follow the matching thumb color when missing), {@code upArrow}, {@code downArrow},
 * {@code leftArrow}, and {@code rightArrow} (one glyph each; U+25B2, U+25BC, U+25C4, and U+25BA when missing), and
 * {@code arrowFont} ({@code "square"} or {@code "text"}; square when missing) shape the step
 * buttons. Example:
 * {@code "scrollPane": { "track": "darkGray", "thumb": "gray", "thumbPressed": "gold",
 * "stepButtons": true }}.
 *
 * <p>A {@code selectBoxes} entry becomes a {@link SelectBoxStyle}. The section comes after
 * {@code scrollPanes}, so an entry can name a scroll pane style. The keys:
 * <ul>
 *   <li>{@code button}, required: a button style name or an inline button object; the looks and
 *       padding of the closed box.
 *   <li>{@code list}, required: a button style name or an inline button object; the looks and
 *       padding of the rows of the open list.
 *   <li>{@code scrollPane}, required: the name of an entry in the {@code scrollPanes} section (it
 *       must name one; an inline object is an error); the scroll bar of the open list.
 *   <li>{@code arrow}, optional: text of exactly one code point drawn in the far-right cell of the
 *       closed box; defaults to {@code "▼"}.
 *   <li>{@code arrowFont}, optional: {@code "text"} or {@code "square"}, in lowercase; defaults to
 *       {@code "square"}.
 * </ul>
 * Example: {@code "selectBox": { "button": "button", "list": "button",
 * "scrollPane": "scrollPane", "arrow": "▼", "arrowFont": "square" }}.
 *
 * <p>A {@code windows} entry becomes a {@link WindowStyle}. The keys:
 * <ul>
 *   <li>{@code box}, required: the line style of the border, one of {@code "single"},
 *       {@code "double"}, {@code "heavy"}, and {@code "rounded"}.
 *   <li>{@code border}, required: the look of the border, a label style name or an inline label
 *       object. The border is drawn in the square font.
 *   <li>{@code focusedBorder}, optional: a look of the same kind, used while focus is inside the
 *       window; copies {@code border} when missing.
 *   <li>{@code title}, required: the look of the title text, drawn in the text font.
 *   <li>{@code background}, required: a color for the area inside the border.
 *   <li>{@code closeButton}, optional: the name of an entry in the {@code buttons} section (it
 *       must name one; an inline button object is an error). Without it the window has no close
 *       button.
 *   <li>{@code closeText}, optional: the text on the close button, at least one character;
 *       defaults to {@code "x"}.
 *   <li>{@code closeFont}, optional: the font of the close text, {@code "text"} (one text column
 *       per code point) or {@code "square"} (one UI cell per code point), in lowercase; defaults
 *       to {@code "square"}. A value that is not exactly {@code "text"} or {@code "square"},
 *       including a non-string or another letter case, fails with a {@link SkinFileException}
 *       that names the key and lists the allowed values. A {@code dialogs} entry uses the close font of the window style it names.
 * </ul>
 * Example: {@code "window": { "box": "rounded", "border": "gray", "title": "gold",
 * "background": "charcoal", "closeButton": "button", "closeText": "[x]", "closeFont": "text" }}.
 *
 * <p>A {@code dialogs} entry becomes a {@link DialogStyle}. The keys:
 * <ul>
 *   <li>{@code window}, required: the name of an entry in the {@code windows} section.
 *   <li>{@code message}, required: the name of an entry in the {@code labels} section.
 *   <li>{@code buttons}, required: the name of an entry in the {@code buttons} section.
 *   <li>{@code textField}, required: the name of an entry in the {@code textFields} section.
 *   <li>{@code dimColor} and {@code dimAmount}, optional but only together: a color (its alpha
 *       is ignored) and a number from 0 to 1 that dim everything under a shown dialog. Giving
 *       only one of them is an error. Without them, or with an amount of 0, the style does
 *       not dim.
 * </ul>
 * Example: {@code "dialog": { "window": "window", "message": "text", "buttons": "button",
 * "textField": "default", "dimColor": "black", "dimAmount": 0.5 }}.
 *
 * <p>Custom sections: to read sections for your own widgets, register a
 * {@link SkinSectionReader} on a {@link SkinLoader} and load through it. Skins made by
 * {@link #fromJson} and {@link #fromFile} have none.
 *
 * <pre>{@code
 * Skin skin = SkinLoader.create()
 *         .withSection("starRatings", StarRatingStyle::read)
 *         .fromFile(Gdx.files.internal("skin.json"));
 * StarRatingStyle style = skin.getCustomStyle("starRatings", "default", StarRatingStyle.class);
 * }</pre>
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
    private final Map<String, ToggleStyle> checkBoxStyles;
    private final Map<String, ToggleStyle> radioButtonStyles;
    private final Map<String, TextFieldStyle> textFieldStyles;
    private final Map<String, ProgressBarStyle> progressBarStyles;
    private final Map<String, SliderStyle> sliderStyles;
    private final Map<String, ScrollPaneStyle> scrollPaneStyles;
    private final Map<String, SelectBoxStyle> selectBoxStyles;
    private final Map<String, WindowStyle> windowStyles;
    private final Map<String, DialogStyle> dialogStyles;
    private final Map<String, Map<String, Object>> customStyles;
    private final ColorNames colorNames;
    private final EffectRegistry effects = EffectRegistry.withBuiltIns();

    Skin(Map<String, Integer> colors, Map<String, LabelStyle> labelStyles,
            Map<String, ButtonStyle> buttonStyles, Map<String, MenuBarStyle> menuBarStyles,
            Map<String, ToggleStyle> checkBoxStyles, Map<String, ToggleStyle> radioButtonStyles,
            Map<String, TextFieldStyle> textFieldStyles,
            Map<String, ProgressBarStyle> progressBarStyles,
            Map<String, SliderStyle> sliderStyles,
            Map<String, ScrollPaneStyle> scrollPaneStyles,
            Map<String, SelectBoxStyle> selectBoxStyles,
            Map<String, WindowStyle> windowStyles,
            Map<String, DialogStyle> dialogStyles,
            Map<String, Map<String, Object>> customStyles) {
        Map<String, Map<String, Object>> customCopy = new LinkedHashMap<>();
        customStyles.forEach((section, styles) -> customCopy.put(section, copyOf(styles)));
        this.customStyles = Collections.unmodifiableMap(customCopy);
        this.textFieldStyles = copyOf(textFieldStyles);
        this.progressBarStyles = copyOf(progressBarStyles);
        this.sliderStyles = copyOf(sliderStyles);
        this.scrollPaneStyles = copyOf(scrollPaneStyles);
        this.selectBoxStyles = copyOf(selectBoxStyles);
        this.windowStyles = copyOf(windowStyles);
        this.dialogStyles = copyOf(dialogStyles);
        this.colors = copyOf(colors);
        this.labelStyles = copyOf(labelStyles);
        this.buttonStyles = copyOf(buttonStyles);
        this.menuBarStyles = copyOf(menuBarStyles);
        this.checkBoxStyles = copyOf(checkBoxStyles);
        this.radioButtonStyles = copyOf(radioButtonStyles);
        this.colorNames = this::findColor;
    }

    private OptionalInt findColor(String name) {
        Integer rgba = colors.get(name);
        return rgba == null ? OptionalInt.empty() : OptionalInt.of(rgba);
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
        return SkinLoader.create().fromJson(json);
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
        return SkinLoader.create().fromFile(file);
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
     * Returns the check box style with a name.
     *
     * @param name name in the {@code checkBoxes} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasCheckBoxStyle(String)} first.
     */
    public ToggleStyle getCheckBoxStyle(String name) {
        return lookUp(checkBoxStyles, name, "check box style");
    }

    /**
     * Returns the radio button style with a name.
     *
     * @param name name in the {@code radioButtons} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasRadioButtonStyle(String)} first.
     */
    public ToggleStyle getRadioButtonStyle(String name) {
        return lookUp(radioButtonStyles, name, "radio button style");
    }

    /**
     * Returns the text field style with a name.
     *
     * @param name name in the {@code textFields} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasTextFieldStyle(String)} first.
     */
    public TextFieldStyle getTextFieldStyle(String name) {
        return lookUp(textFieldStyles, name, "text field style");
    }

    /**
     * Returns whether the skin defines a text field style with a name.
     *
     * @param name name in the {@code textFields} section; must not be {@code null}
     * @return {@code true} if {@link #getTextFieldStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasTextFieldStyle(String name) {
        return contains(textFieldStyles, name);
    }

    /**
     * Returns the progress bar style with a name.
     *
     * @param name name in the {@code progressBars} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasProgressBarStyle(String)} first.
     */
    public ProgressBarStyle getProgressBarStyle(String name) {
        return lookUp(progressBarStyles, name, "progress bar style");
    }

    /**
     * Returns the slider style with a name.
     *
     * @param name name in the {@code sliders} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasSliderStyle(String)} first.
     */
    public SliderStyle getSliderStyle(String name) {
        return lookUp(sliderStyles, name, "slider style");
    }

    /**
     * Returns the scroll pane style with a name.
     *
     * @param name name in the {@code scrollPanes} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasScrollPaneStyle(String)} first.
     */
    public ScrollPaneStyle getScrollPaneStyle(String name) {
        return lookUp(scrollPaneStyles, name, "scroll pane style");
    }

    /**
     * Returns whether the skin defines a scroll pane style with a name.
     *
     * @param name name in the {@code scrollPanes} section; must not be {@code null}
     * @return {@code true} if {@link #getScrollPaneStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasScrollPaneStyle(String name) {
        return contains(scrollPaneStyles, name);
    }

    /**
     * Returns the select box style with a name.
     *
     * @param name name in the {@code selectBoxes} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasSelectBoxStyle(String)} first.
     */
    public SelectBoxStyle getSelectBoxStyle(String name) {
        return lookUp(selectBoxStyles, name, "select box style");
    }

    /**
     * Returns whether the skin defines a select box style with a name.
     *
     * @param name name in the {@code selectBoxes} section; must not be {@code null}
     * @return {@code true} if {@link #getSelectBoxStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasSelectBoxStyle(String name) {
        return contains(selectBoxStyles, name);
    }

    /**
     * Returns the window style with a name.
     *
     * @param name name in the {@code windows} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasWindowStyle(String)} first.
     */
    public WindowStyle getWindowStyle(String name) {
        return lookUp(windowStyles, name, "window style");
    }

    /**
     * Returns the dialog style with a name.
     *
     * @param name name in the {@code dialogs} section; must not be {@code null}
     * @return the style, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if there is no such style; the message lists the known
     *         names. Check with {@link #hasDialogStyle(String)} first.
     */
    public DialogStyle getDialogStyle(String name) {
        return lookUp(dialogStyles, name, "dialog style");
    }

    /**
     * Returns whether the skin defines a dialog style with a name.
     *
     * @param name name in the {@code dialogs} section; must not be {@code null}
     * @return {@code true} if {@link #getDialogStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasDialogStyle(String name) {
        return contains(dialogStyles, name);
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
     * Returns the colors of the {@code colors} section as a lookup for inline color tags, so
     * {@code [color:deepBlue]} in a {@link Label} text finds the skin's color named
     * {@code deepBlue}. Names are compared exactly, as in {@link #getRgba(String)}. The lookup is
     * a view of this skin: it copies nothing per call, never changes, and is the same object on
     * every call. Use it with {@link Label#setColorNames}.
     *
     * @return the lookup, never {@code null}; it returns each color as 0xRRGGBBAA, and an empty
     *         value for a name that is not in the {@code colors} section
     */
    public ColorNames getColorNames() {
        return colorNames;
    }

    /**
     * Returns the effects that {@code [effect:NAME ...]} tags can name, for use with
     * {@link Label#setEffectNames}. It starts with the built-in effects of
     * {@link EffectRegistry}. The skin file has no effect section: a game adds its own effects in
     * code, with {@link EffectRegistry#register}, before it parses the labels that use them. A
     * label sees an effect registered after the label was parsed only once
     * {@link Label#setEffectNames} is called again. The registry is the one thing about a skin
     * that can change; it belongs to this skin object only (nothing is shared between skins), so
     * a reloaded skin starts again with the built-ins. Call this, and register, from the game
     * (GL) thread only, like every skin, widget, and label call.
     *
     * @return the registry, never {@code null}; the same object on every call
     */
    public EffectRegistry getEffectRegistry() {
        return effects;
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
     * Returns whether the skin defines a check box style with a name.
     *
     * @param name name in the {@code checkBoxes} section; must not be {@code null}
     * @return {@code true} if {@link #getCheckBoxStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasCheckBoxStyle(String name) {
        return contains(checkBoxStyles, name);
    }

    /**
     * Returns whether the skin defines a radio button style with a name.
     *
     * @param name name in the {@code radioButtons} section; must not be {@code null}
     * @return {@code true} if {@link #getRadioButtonStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasRadioButtonStyle(String name) {
        return contains(radioButtonStyles, name);
    }

    /**
     * Returns whether the skin defines a progress bar style with a name.
     *
     * @param name name in the {@code progressBars} section; must not be {@code null}
     * @return {@code true} if {@link #getProgressBarStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasProgressBarStyle(String name) {
        return contains(progressBarStyles, name);
    }

    /**
     * Returns whether the skin defines a slider style with a name.
     *
     * @param name name in the {@code sliders} section; must not be {@code null}
     * @return {@code true} if {@link #getSliderStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasSliderStyle(String name) {
        return contains(sliderStyles, name);
    }

    /**
     * Returns whether the skin defines a window style with a name.
     *
     * @param name name in the {@code windows} section; must not be {@code null}
     * @return {@code true} if {@link #getWindowStyle(String)} would succeed for that name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public boolean hasWindowStyle(String name) {
        return contains(windowStyles, name);
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

    /**
     * Returns a style from a custom section registered with
     * {@link SkinLoader#withSection(String, SkinSectionReader)}.
     *
     * @param section the custom section name; must not be {@code null}
     * @param name the entry name in that section; must not be {@code null}
     * @param type the runtime class of the style, or a supertype of it; generic type arguments
     *         cannot be checked. Must not be {@code null}
     * @param <T> the style type
     * @return the style, never {@code null}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the section was not registered on the loader that made
     *         this skin, the section has no such name (the message lists the known names), or the
     *         stored style is not an instance of {@code type}
     */
    public <T> T getCustomStyle(String section, String name, Class<T> type) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Map<String, Object> styles = customStyles.get(section);
        if (styles == null) {
            throw new IllegalArgumentException("Unknown custom section \"" + section
                    + "\". Known custom sections: " + describeNames(customStyles));
        }
        Object style = styles.get(name);
        if (style == null) {
            throw new IllegalArgumentException("Unknown custom style \"" + name
                    + "\" in section \"" + section + "\". Known custom styles: "
                    + describeNames(styles));
        }
        if (!type.isInstance(style)) {
            throw new IllegalArgumentException("The custom style \"" + name + "\" in section \""
                    + section + "\" is a " + style.getClass().getName() + ", not a "
                    + type.getName());
        }
        return type.cast(style);
    }

    /**
     * Returns whether a custom section has an entry with a name.
     *
     * @param section the custom section name; must not be {@code null}
     * @param name the entry name; must not be {@code null}
     * @return {@code true} exactly when {@link #getCustomStyle} would not throw for a missing
     *         section or name; the type check is separate
     * @throws NullPointerException if an argument is {@code null}
     */
    public boolean hasCustomStyle(String section, String name) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(name, "name");
        Map<String, Object> styles = customStyles.get(section);
        return styles != null && styles.containsKey(name);
    }

    private static boolean contains(Map<String, ?> namedValues, String name) {
        Objects.requireNonNull(name, "name");
        return namedValues.containsKey(name);
    }

    @Override
    public String toString() {
        return "Skin[colors=" + colors.size() + ", labelStyles=" + labelStyles.size()
                + ", buttonStyles=" + buttonStyles.size() + ", menuBarStyles="
                + menuBarStyles.size() + ", checkBoxStyles=" + checkBoxStyles.size()
                + ", radioButtonStyles=" + radioButtonStyles.size() + ", textFieldStyles="
                + textFieldStyles.size() + ", progressBarStyles=" + progressBarStyles.size()
                + ", sliderStyles=" + sliderStyles.size() + ", scrollPaneStyles="
                + scrollPaneStyles.size() + ", windowStyles=" + windowStyles.size()
                + ", customSections="
                + customStyles.keySet() + "]";
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
