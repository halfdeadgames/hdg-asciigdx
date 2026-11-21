package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns skin file text into a {@link Skin}, checking every key, type, and reference. */
final class SkinFileReader {

    private static final String COLORS = "colors";
    private static final String LABELS = "labels";
    private static final String BUTTONS = "buttons";
    private static final String MENU_BARS = "menuBars";
    private static final String CHECK_BOXES = "checkBoxes";
    private static final String RADIO_BUTTONS = "radioButtons";
    private static final String TEXT_FIELDS = "textFields";
    private static final String PROGRESS_BARS = "progressBars";
    private static final String SLIDERS = "sliders";
    private static final String SCROLL_PANES = "scrollPanes";
    private static final String WINDOWS = "windows";
    private static final String DIALOGS = "dialogs";
    private static final List<String> SECTION_NAMES = List.of(COLORS, LABELS, BUTTONS, MENU_BARS,
            CHECK_BOXES, RADIO_BUTTONS, TEXT_FIELDS, PROGRESS_BARS, SLIDERS, SCROLL_PANES, WINDOWS,
            DIALOGS);
    private static final List<String> DIALOG_KEYS = List.of("window", "message", "buttons",
            "textField", "dimColor", "dimAmount");
    private static final List<String> SCROLL_PANE_KEYS = List.of("track", "thumb",
            "thumbPointerOver", "thumbPressed");
    private static final List<String> WINDOW_KEYS = List.of("box", "border", "focusedBorder",
            "title", "background", "closeButton", "closeText");
    private static final List<String> BOX_NAMES = List.of("single", "double", "heavy", "rounded");
    private static final List<String> TEXT_FIELD_KEYS = List.of("normal", "focused", "disabled",
            "placeholder", "caret");
    private static final List<String> CARET_KEYS = List.of("glyph", "foreground", "blinkSeconds");
    private static final List<String> PROGRESS_BAR_KEYS = List.of("fill", "empty");
    private static final List<String> SLIDER_KEYS = List.of("trackGlyph", "knobGlyph", "track",
            "knob");
    private static final List<String> SLIDER_LOOK_KEYS = List.of("normal", "focused", "disabled");
    private static final List<String> TOGGLE_KEYS = List.of("button", "unchecked", "checked",
            "gap");
    private static final String REMOVED_FONT_KEY = "font";
    private static final List<String> LABEL_KEYS = List.of("foreground", "background");
    private static final List<String> BUTTON_KEYS = List.of("normal", "pointerOver", "pressed",
            "disabled", "focused", "horizontalPadding");
    private static final List<String> MENU_BAR_KEYS = List.of("titles", "items", "triggerColor",
            "triggerUnderline");
    private static final int NORMAL = 0;
    private static final int FOCUSED = 1;
    private static final int DISABLED = 2;
    private static final int OPAQUE_ALPHA = 0xFF;
    private static final int HEX_RADIX = 16;
    private static final int RGB_DIGIT_COUNT = 6;
    private static final int RGBA_DIGIT_COUNT = 8;
    private static final Pattern LINE_IN_MESSAGE = Pattern.compile("line (\\d+)");

    private final Map<String, Integer> colors = new LinkedHashMap<>();
    private final Map<String, LabelStyle> labelStyles = new LinkedHashMap<>();
    private final Map<String, ButtonStyle> buttonStyles = new LinkedHashMap<>();
    private final Map<String, MenuBarStyle> menuBarStyles = new LinkedHashMap<>();
    private final Map<String, ToggleStyle> checkBoxStyles = new LinkedHashMap<>();
    private final Map<String, ToggleStyle> radioButtonStyles = new LinkedHashMap<>();
    private final Map<String, TextFieldStyle> textFieldStyles = new LinkedHashMap<>();
    private final Map<String, ProgressBarStyle> progressBarStyles = new LinkedHashMap<>();
    private final Map<String, SliderStyle> sliderStyles = new LinkedHashMap<>();
    private final Map<String, ScrollPaneStyle> scrollPaneStyles = new LinkedHashMap<>();
    private final Map<String, WindowStyle> windowStyles = new LinkedHashMap<>();
    private final Map<String, DialogStyle> dialogStyles = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> customStyles = new LinkedHashMap<>();

    private SkinFileReader() {
    }

    static boolean isBuiltInSection(String sectionName) {
        return SECTION_NAMES.contains(sectionName);
    }

    static Skin read(String json) {
        return read(json, Map.of());
    }

    static Skin read(String json, Map<String, SkinSectionReader<?>> customReaders) {
        JsonValue root = readRoot(json);
        List<String> knownSections = new ArrayList<>(SECTION_NAMES);
        knownSections.addAll(customReaders.keySet());
        checkKnownKeys(root, knownSections, "section", "The file");
        var reader = new SkinFileReader();
        reader.readSection(root, COLORS, reader.colors, reader::readColorEntry);
        reader.readSection(root, LABELS, reader.labelStyles, reader::readLabelStyle);
        reader.readSection(root, BUTTONS, reader.buttonStyles, reader::readButtonStyle);
        reader.readSection(root, MENU_BARS, reader.menuBarStyles, reader::readMenuBarStyle);
        reader.readSection(root, CHECK_BOXES, reader.checkBoxStyles, reader::readToggleStyle);
        reader.readSection(root, RADIO_BUTTONS, reader.radioButtonStyles,
                reader::readToggleStyle);
        reader.readSection(root, TEXT_FIELDS, reader.textFieldStyles,
                reader::readTextFieldStyle);
        reader.readSection(root, PROGRESS_BARS, reader.progressBarStyles,
                reader::readProgressBarStyle);
        reader.readSection(root, SLIDERS, reader.sliderStyles, reader::readSliderStyle);
        reader.readSection(root, SCROLL_PANES, reader.scrollPaneStyles,
                reader::readScrollPaneStyle);
        reader.readSection(root, WINDOWS, reader.windowStyles, reader::readWindowStyle);
        reader.readSection(root, DIALOGS, reader.dialogStyles, reader::readDialogStyle);
        reader.readCustomSections(root, customReaders);
        return new Skin(reader.colors, reader.labelStyles, reader.buttonStyles,
                reader.menuBarStyles, reader.checkBoxStyles, reader.radioButtonStyles,
                reader.textFieldStyles, reader.progressBarStyles, reader.sliderStyles,
                reader.scrollPaneStyles, reader.windowStyles, reader.dialogStyles,
                reader.customStyles);
    }

    private DialogStyle readDialogStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a dialog object with the keys " + String.join(", ", DIALOG_KEYS));
        checkKnownKeys(entry, DIALOG_KEYS, "key", path);
        WindowStyle window = readNamedReference(entry, "window", windowStyles, "window style",
                path);
        LabelStyle message = readNamedReference(entry, "message", labelStyles, "label style",
                path);
        ButtonStyle buttons = readNamedReference(entry, "buttons", buttonStyles, "button style",
                path);
        TextFieldStyle textField = readNamedReference(entry, "textField", textFieldStyles,
                "text field style", path);
        DialogStyle style = DialogStyle.of(window, message, buttons, textField);
        JsonValue dimColor = entry.get("dimColor");
        JsonValue dimAmount = entry.get("dimAmount");
        if ((dimColor == null) != (dimAmount == null)) {
            throw new SkinFileException(path
                    + ": \"dimColor\" and \"dimAmount\" must be given together");
        }
        if (dimColor == null) {
            return style;
        }
        return style.withDim(readColor(dimColor, path + ".dimColor"),
                readDimAmount(dimAmount, path + ".dimAmount"));
    }

    private static <T> T readNamedReference(JsonValue entry, String key, Map<String, T> known,
            String kind, String path) {
        JsonValue value = requireKey(entry, key, path);
        String keyPath = path + "." + key;
        if (!value.isString()) {
            throw new SkinFileException(keyPath + ": must be the name of a " + kind);
        }
        return lookUpReference(known, value.asString(), kind, keyPath);
    }

    private static float readDimAmount(JsonValue value, String path) {
        if (!value.isNumber()) {
            throw new SkinFileException(path + ": must be a number from 0 to 1");
        }
        double amount = value.asDouble();
        if (!(amount >= 0) || amount > 1) {
            throw new SkinFileException(
                    path + ": must be from 0 to 1, was " + value.asString());
        }
        return (float) amount;
    }

    private ScrollPaneStyle readScrollPaneStyle(JsonValue entry, String path) {
        requireObject(entry, path, "a scroll pane object with the keys "
                + String.join(", ", SCROLL_PANE_KEYS));
        checkKnownKeys(entry, SCROLL_PANE_KEYS, "key", path);
        int track = readColor(requireKey(entry, "track", path), path + ".track");
        int thumb = readColor(requireKey(entry, "thumb", path), path + ".thumb");
        ScrollPaneStyle style = ScrollPaneStyle.of(track, thumb);
        JsonValue pointerOver = entry.get("thumbPointerOver");
        if (pointerOver != null) {
            style = style.withThumbPointerOver(
                    readColor(pointerOver, path + ".thumbPointerOver"));
        }
        JsonValue pressed = entry.get("thumbPressed");
        if (pressed != null) {
            style = style.withThumbPressed(readColor(pressed, path + ".thumbPressed"));
        }
        return style;
    }

    private WindowStyle readWindowStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a window object with the keys " + String.join(", ", WINDOW_KEYS));
        checkKnownKeys(entry, WINDOW_KEYS, "key", path);
        BoxStyle box = readBox(requireKey(entry, "box", path), path + ".box");
        LabelStyle border = readLook(requireKey(entry, "border", path), path + ".border");
        LabelStyle title = readLook(requireKey(entry, "title", path), path + ".title");
        int background = readColor(requireKey(entry, "background", path), path + ".background");
        WindowStyle style = WindowStyle.of(box, border, title, background);
        JsonValue focusedBorder = entry.get("focusedBorder");
        if (focusedBorder != null) {
            style = style.withFocusedBorder(
                    readLook(focusedBorder, path + ".focusedBorder"));
        }
        JsonValue closeButton = entry.get("closeButton");
        if (closeButton != null) {
            style = style.withCloseButton(
                    readCloseButton(closeButton, path + ".closeButton"));
        }
        JsonValue closeText = entry.get("closeText");
        if (closeText != null) {
            style = style.withCloseText(readMark(closeText, path + ".closeText"));
        }
        return style;
    }

    private static BoxStyle readBox(JsonValue value, String path) {
        String name = value.isString() ? value.asString() : "";
        return switch (name) {
            case "single" -> BoxStyle.SINGLE;
            case "double" -> BoxStyle.DOUBLE;
            case "heavy" -> BoxStyle.HEAVY;
            case "rounded" -> BoxStyle.ROUNDED;
            default -> throw new SkinFileException(path + ": must be text, one of "
                    + String.join(", ", BOX_NAMES));
        };
    }

    private ButtonStyle readCloseButton(JsonValue value, String path) {
        if (!value.isString()) {
            throw new SkinFileException(path + ": must be the name of a button style");
        }
        return lookUpReference(buttonStyles, value.asString(), "button style", path);
    }

    private void readCustomSections(JsonValue root,
            Map<String, SkinSectionReader<?>> customReaders) {
        for (Map.Entry<String, SkinSectionReader<?>> registered : customReaders.entrySet()) {
            Map<String, Object> styles = new LinkedHashMap<>();
            customStyles.put(registered.getKey(), styles);
            SkinSectionReader<?> sectionReader = registered.getValue();
            readSection(root, registered.getKey(), styles,
                    (entry, path) -> readCustomEntry(sectionReader, entry, path));
        }
    }

    private Object readCustomEntry(SkinSectionReader<?> sectionReader, JsonValue entry,
            String path) {
        var context = new SkinEntryContext(this, path);
        Object style;
        try {
            style = sectionReader.readEntry(entry, context);
        } finally {
            context.invalidate();
        }
        if (style == null) {
            throw new SkinFileException(path + ": the section reader returned no style");
        }
        return style;
    }

    private TextFieldStyle readTextFieldStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a text field object with the keys " + String.join(", ", TEXT_FIELD_KEYS));
        checkKnownKeys(entry, TEXT_FIELD_KEYS, "key", path);
        LabelStyle normal = readLook(requireKey(entry, "normal", path), path + ".normal");
        TextFieldStyle style = TextFieldStyle.of(normal)
                .withFocused(readOptionalLook(entry, "focused", normal, path))
                .withDisabled(readOptionalLook(entry, "disabled", normal, path))
                .withPlaceholder(readOptionalLook(entry, "placeholder", normal, path));
        JsonValue caret = entry.get("caret");
        if (caret == null) {
            return style;
        }
        return style.withCaret(readCaret(caret, path + ".caret"));
    }

    private CaretStyle readCaret(JsonValue caret, String path) {
        requireObject(caret, path, "a caret object with the keys " + String.join(", ", CARET_KEYS));
        if (caret.get("background") != null) {
            throw new SkinFileException(path + ".background: the caret is now a glyph in the "
                    + "text font and has no background; delete the \"background\" key");
        }
        checkKnownKeys(caret, CARET_KEYS, "key", path);
        CaretStyle style = CaretStyle.none().withGlyph(
                readGlyph(requireKey(caret, "glyph", path), path + ".glyph"));
        JsonValue foreground = caret.get("foreground");
        if (foreground != null) {
            style = style.withForegroundRgba(readColor(foreground, path + ".foreground"));
        }
        JsonValue blink = caret.get("blinkSeconds");
        if (blink != null) {
            style = style.withBlinkSeconds(readBlinkSeconds(blink, path + ".blinkSeconds"));
        }
        return style;
    }

    private static float readBlinkSeconds(JsonValue value, String path) {
        if (!value.isNumber()) {
            throw new SkinFileException(path + ": must be a number of seconds");
        }
        double seconds = value.asDouble();
        if (!(seconds >= 0) || Double.isInfinite(seconds) || seconds > Float.MAX_VALUE) {
            throw new SkinFileException(
                    path + ": must be zero or greater and finite, was " + value.asString());
        }
        return (float) seconds;
    }

    private ProgressBarStyle readProgressBarStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a progress bar object with the keys " + String.join(", ", PROGRESS_BAR_KEYS));
        checkKnownKeys(entry, PROGRESS_BAR_KEYS, "key", path);
        LabelStyle fill = readLook(requireKey(entry, "fill", path), path + ".fill");
        LabelStyle empty = readLook(requireKey(entry, "empty", path), path + ".empty");
        return ProgressBarStyle.of(fill, empty);
    }

    private SliderStyle readSliderStyle(JsonValue entry, String path) {
        requireObject(entry, path, "a slider object with the keys "
                + String.join(", ", SLIDER_KEYS));
        checkKnownKeys(entry, SLIDER_KEYS, "key", path);
        LabelStyle[] track = readSliderLooks(requireKey(entry, "track", path), path + ".track");
        LabelStyle[] knob = readSliderLooks(requireKey(entry, "knob", path), path + ".knob");
        SliderStyle style = SliderStyle.of(track[NORMAL], knob[NORMAL])
                .withFocused(track[FOCUSED], knob[FOCUSED])
                .withDisabled(track[DISABLED], knob[DISABLED]);
        return applyGlyphs(style, entry, path);
    }

    // Returns the normal, focused, and disabled looks in that order.
    private LabelStyle[] readSliderLooks(JsonValue group, String path) {
        requireObject(group, path,
                "an object with the keys " + String.join(", ", SLIDER_LOOK_KEYS));
        checkKnownKeys(group, SLIDER_LOOK_KEYS, "key", path);
        LabelStyle normal = readLook(requireKey(group, "normal", path), path + ".normal");
        LabelStyle focused = readOptionalLook(group, "focused", normal, path);
        LabelStyle disabled = readOptionalLook(group, "disabled", normal, path);
        return new LabelStyle[] {normal, focused, disabled};
    }

    private static SliderStyle applyGlyphs(SliderStyle style, JsonValue slider, String path) {
        JsonValue trackGlyph = slider.get("trackGlyph");
        JsonValue knobGlyph = slider.get("knobGlyph");
        if (trackGlyph == null && knobGlyph == null) {
            return style;
        }
        if (trackGlyph == null || knobGlyph == null) {
            throw new SkinFileException(
                    path + ": \"trackGlyph\" and \"knobGlyph\" must be given together");
        }
        return style.withGlyphs(readGlyph(trackGlyph, path + ".trackGlyph"),
                readGlyph(knobGlyph, path + ".knobGlyph"));
    }

    private static int readGlyph(JsonValue value, String path) {
        boolean isSingleCodePoint = value.isString() && value.asString().codePointCount(0,
                value.asString().length()) == 1;
        if (!isSingleCodePoint) {
            throw new SkinFileException(
                    path + ": must be exactly one character, as text of exactly one code point");
        }
        return value.asString().codePointAt(0);
    }

    private static JsonValue readRoot(String json) {
        JsonValue root;
        try {
            root = new JsonReader().parse(json);
        } catch (SerializationException syntaxError) {
            throw new SkinFileException(describeSyntaxError(syntaxError));
        }
        if (root == null || !root.isObject()) {
            throw new SkinFileException("The file must hold one object with the sections "
                    + String.join(", ", SECTION_NAMES));
        }
        return root;
    }

    private static String describeSyntaxError(SerializationException syntaxError) {
        String detail = String.valueOf(syntaxError.getMessage());
        Matcher line = LINE_IN_MESSAGE.matcher(detail);
        if (line.find()) {
            return "Line " + line.group(1) + ": the file is not valid JSON (" + detail + ")";
        }
        return "The file is not valid JSON (" + detail + ")";
    }

    private <T> void readSection(JsonValue root, String sectionName, Map<String, T> target,
            BiFunction<JsonValue, String, T> entryReader) {
        JsonValue section = root.get(sectionName);
        if (section == null) {
            return;
        }
        if (!section.isObject()) {
            throw new SkinFileException(
                    sectionName + ": must be an object that maps each name to its definition");
        }
        for (JsonValue entry = section.child; entry != null; entry = entry.next) {
            String path = sectionName + "." + entry.name;
            if (target.containsKey(entry.name)) {
                throw new SkinFileException(path + ": defined twice");
            }
            target.put(entry.name, entryReader.apply(entry, path));
        }
    }

    private Integer readColorEntry(JsonValue entry, String path) {
        if (!entry.isString() || !entry.asString().startsWith("#")) {
            throw new SkinFileException(path + ": must be hex text such as \"#RRGGBB\" or "
                    + "\"#RRGGBBAA\"; a color cannot be defined as the name of another color");
        }
        return parseHex(entry.asString(), path);
    }

    private LabelStyle readLabelStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a label object with the keys " + String.join(", ", LABEL_KEYS));
        rejectRemovedFontKey(entry, path);
        checkKnownKeys(entry, LABEL_KEYS, "key", path);
        int foreground = readColor(requireKey(entry, "foreground", path), path + ".foreground");
        int background = readColor(requireKey(entry, "background", path), path + ".background");
        return LabelStyle.of(foreground, background);
    }

    private static void rejectRemovedFontKey(JsonValue label, String path) {
        if (label.get(REMOVED_FONT_KEY) != null) {
            throw new SkinFileException(path + "." + REMOVED_FONT_KEY + ": the \""
                    + REMOVED_FONT_KEY + "\" key was removed, because the UI now picks the font "
                    + "by job (words use the text font, drawn parts use the square font); delete "
                    + "the key");
        }
    }

    private ButtonStyle readButtonStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a button object with the keys " + String.join(", ", BUTTON_KEYS));
        checkKnownKeys(entry, BUTTON_KEYS, "key", path);
        LabelStyle normal = readLook(requireKey(entry, "normal", path), path + ".normal");
        LabelStyle pointerOver = readOptionalLook(entry, "pointerOver", normal, path);
        LabelStyle pressed = readOptionalLook(entry, "pressed", normal, path);
        LabelStyle disabled = readOptionalLook(entry, "disabled", normal, path);
        ButtonStyle style = ButtonStyle.of(normal, pointerOver, pressed, disabled);
        style = applyFocusedLook(style, entry, path);
        return applyHorizontalPadding(style, entry, path);
    }

    private ButtonStyle applyFocusedLook(ButtonStyle style, JsonValue button, String path) {
        JsonValue focused = button.get("focused");
        if (focused == null) {
            return style;
        }
        return style.withFocused(readLook(focused, path + ".focused"));
    }

    private static ButtonStyle applyHorizontalPadding(ButtonStyle style, JsonValue button,
            String path) {
        JsonValue padding = button.get("horizontalPadding");
        if (padding == null) {
            return style;
        }
        String paddingPath = path + ".horizontalPadding";
        if (!padding.isLong() || padding.asLong() > Integer.MAX_VALUE) {
            throw new SkinFileException(paddingPath + ": must be a whole number of cells");
        }
        if (padding.asLong() < 0) {
            throw new SkinFileException(
                    paddingPath + ": must not be negative, was " + padding.asLong());
        }
        return style.withHorizontalPadding((int) padding.asLong());
    }

    private MenuBarStyle readMenuBarStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a menu bar object with the keys " + String.join(", ", MENU_BAR_KEYS));
        checkKnownKeys(entry, MENU_BAR_KEYS, "key", path);
        ButtonStyle titles = readButtonReference(requireKey(entry, "titles", path),
                path + ".titles");
        ButtonStyle items = readButtonReference(requireKey(entry, "items", path), path + ".items");
        MenuBarStyle style = MenuBarStyle.of(titles, items);
        style = applyTriggerColor(style, entry, path);
        return applyTriggerUnderline(style, entry, path);
    }

    private MenuBarStyle applyTriggerColor(MenuBarStyle style, JsonValue menuBar, String path) {
        JsonValue triggerColor = menuBar.get("triggerColor");
        if (triggerColor == null) {
            return style;
        }
        return style.withTriggerRgba(readColor(triggerColor, path + ".triggerColor"));
    }

    private static MenuBarStyle applyTriggerUnderline(MenuBarStyle style, JsonValue menuBar,
            String path) {
        JsonValue underline = menuBar.get("triggerUnderline");
        if (underline == null) {
            return style;
        }
        if (!underline.isBoolean()) {
            throw new SkinFileException(path + ".triggerUnderline: must be true or false");
        }
        return style.withTriggerUnderline(underline.asBoolean());
    }

    private ToggleStyle readToggleStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a toggle object with the keys " + String.join(", ", TOGGLE_KEYS));
        checkKnownKeys(entry, TOGGLE_KEYS, "key", path);
        ButtonStyle button = readButtonReference(requireKey(entry, "button", path),
                path + ".button");
        String unchecked = readMark(requireKey(entry, "unchecked", path), path + ".unchecked");
        String checked = readMark(requireKey(entry, "checked", path), path + ".checked");
        ToggleStyle style = createToggleStyle(button, unchecked, checked, path);
        return applyGap(style, entry, path);
    }

    private static String readMark(JsonValue value, String path) {
        if (!value.isString() || value.asString().isEmpty()) {
            throw new SkinFileException(path + ": must be text of at least one character");
        }
        return value.asString();
    }

    private static ToggleStyle createToggleStyle(ButtonStyle button, String unchecked,
            String checked, String path) {
        try {
            return ToggleStyle.of(button, unchecked, checked);
        } catch (IllegalArgumentException unequalMarks) {
            throw new SkinFileException(path + ".checked: " + unequalMarks.getMessage());
        }
    }

    private static ToggleStyle applyGap(ToggleStyle style, JsonValue toggle, String path) {
        JsonValue gap = toggle.get("gap");
        if (gap == null) {
            return style;
        }
        String gapPath = path + ".gap";
        if (!gap.isLong() || gap.asLong() > Integer.MAX_VALUE) {
            throw new SkinFileException(gapPath + ": must be a whole number of cells");
        }
        if (gap.asLong() < 0) {
            throw new SkinFileException(gapPath + ": must not be negative, was " + gap.asLong());
        }
        return style.withGap((int) gap.asLong());
    }

    private LabelStyle readOptionalLook(JsonValue button, String key, LabelStyle fallback,
            String path) {
        JsonValue look = button.get(key);
        if (look == null) {
            return fallback;
        }
        return readLook(look, path + "." + key);
    }

    LabelStyle readLook(JsonValue value, String path) {
        if (value.isString()) {
            return lookUpReference(labelStyles, value.asString(), "label style", path);
        }
        if (value.isObject()) {
            return readLabelStyle(value, path);
        }
        throw new SkinFileException(
                path + ": must be the name of a label style or a label object");
    }

    private ButtonStyle readButtonReference(JsonValue value, String path) {
        if (value.isString()) {
            return lookUpReference(buttonStyles, value.asString(), "button style", path);
        }
        if (value.isObject()) {
            return readButtonStyle(value, path);
        }
        throw new SkinFileException(
                path + ": must be the name of a button style or a button object");
    }

    private static <T> T lookUpReference(Map<String, T> known, String name, String kind,
            String path) {
        T found = known.get(name);
        if (found == null) {
            throw new SkinFileException(path + ": unknown " + kind + " \"" + name + "\". Known "
                    + kind + "s: " + Skin.describeNames(known));
        }
        return found;
    }

    int readColor(JsonValue value, String path) {
        if (!value.isString()) {
            throw new SkinFileException(path
                    + ": must be text, either hex such as \"#RRGGBB\" or the name of a color");
        }
        String text = value.asString();
        if (text.startsWith("#")) {
            return parseHex(text, path);
        }
        Integer named = colors.get(text);
        if (named == null) {
            throw new SkinFileException(path + ": unknown color \"" + text + "\". Known colors: "
                    + Skin.describeNames(colors));
        }
        return named;
    }

    private static int parseHex(String text, String path) {
        String digits = text.substring(1);
        boolean validLength = digits.length() == RGB_DIGIT_COUNT
                || digits.length() == RGBA_DIGIT_COUNT;
        if (!validLength || !hasOnlyHexDigits(digits)) {
            throw new SkinFileException(path + ": \"" + text + "\" is not a color; use "
                    + "\"#RRGGBB\" or \"#RRGGBBAA\" with the digits 0 to 9 and A to F");
        }
        int parsed = (int) Long.parseLong(digits, HEX_RADIX);
        if (digits.length() == RGB_DIGIT_COUNT) {
            return (parsed << Byte.SIZE) | OPAQUE_ALPHA;
        }
        return parsed;
    }

    private static boolean hasOnlyHexDigits(String digits) {
        for (int i = 0; i < digits.length(); i++) {
            char digit = digits.charAt(i);
            boolean isHex = (digit >= '0' && digit <= '9') || (digit >= 'a' && digit <= 'f')
                    || (digit >= 'A' && digit <= 'F');
            if (!isHex) {
                return false;
            }
        }
        return true;
    }

    private static void requireObject(JsonValue value, String path, String expectation) {
        if (!value.isObject()) {
            throw new SkinFileException(path + ": must be " + expectation);
        }
    }

    private static JsonValue requireKey(JsonValue object, String key, String path) {
        JsonValue value = object.get(key);
        if (value == null) {
            throw new SkinFileException(path + ": missing \"" + key + "\"");
        }
        return value;
    }

    private static void checkKnownKeys(JsonValue object, List<String> knownKeys, String noun,
            String path) {
        Set<String> seen = new HashSet<>();
        for (JsonValue entry = object.child; entry != null; entry = entry.next) {
            if (!knownKeys.contains(entry.name)) {
                throw new SkinFileException(path + ": unknown " + noun + " \"" + entry.name
                        + "\". Known " + noun + "s: " + String.join(", ", knownKeys));
            }
            if (!seen.add(entry.name)) {
                throw new SkinFileException(
                        path + ": \"" + entry.name + "\" appears twice");
            }
        }
    }
}
