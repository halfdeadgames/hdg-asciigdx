package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
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
    private static final List<String> SECTION_NAMES = List.of(COLORS, LABELS, BUTTONS, MENU_BARS,
            CHECK_BOXES, RADIO_BUTTONS);
    private static final List<String> TOGGLE_KEYS = List.of("button", "unchecked", "checked",
            "gap");
    private static final List<String> LABEL_KEYS = List.of("font", "foreground", "background");
    private static final List<String> BUTTON_KEYS = List.of("normal", "pointerOver", "pressed",
            "disabled", "focused", "horizontalPadding");
    private static final List<String> MENU_BAR_KEYS = List.of("titles", "items", "triggerColor",
            "triggerUnderline");
    private static final List<String> FONT_NAMES = List.of("tall", "square");
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

    private SkinFileReader() {
    }

    static Skin read(String json) {
        JsonValue root = readRoot(json);
        checkKnownKeys(root, SECTION_NAMES, "section", "The file");
        var reader = new SkinFileReader();
        reader.readSection(root, COLORS, reader.colors, reader::readColorEntry);
        reader.readSection(root, LABELS, reader.labelStyles, reader::readLabelStyle);
        reader.readSection(root, BUTTONS, reader.buttonStyles, reader::readButtonStyle);
        reader.readSection(root, MENU_BARS, reader.menuBarStyles, reader::readMenuBarStyle);
        reader.readSection(root, CHECK_BOXES, reader.checkBoxStyles, reader::readToggleStyle);
        reader.readSection(root, RADIO_BUTTONS, reader.radioButtonStyles,
                reader::readToggleStyle);
        return new Skin(reader.colors, reader.labelStyles, reader.buttonStyles,
                reader.menuBarStyles, reader.checkBoxStyles, reader.radioButtonStyles);
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
        checkKnownKeys(entry, LABEL_KEYS, "key", path);
        UiFont font = readFont(requireKey(entry, "font", path), path + ".font");
        int foreground = readColor(requireKey(entry, "foreground", path), path + ".foreground");
        int background = readColor(requireKey(entry, "background", path), path + ".background");
        return LabelStyle.of(font, foreground, background);
    }

    private ButtonStyle readButtonStyle(JsonValue entry, String path) {
        requireObject(entry, path,
                "a button object with the keys " + String.join(", ", BUTTON_KEYS));
        checkKnownKeys(entry, BUTTON_KEYS, "key", path);
        LabelStyle normal = readLook(requireKey(entry, "normal", path), path + ".normal");
        LabelStyle pointerOver = readOptionalLook(entry, "pointerOver", normal, path);
        LabelStyle pressed = readOptionalLook(entry, "pressed", normal, path);
        LabelStyle disabled = readOptionalLook(entry, "disabled", normal, path);
        ButtonStyle style = createButtonStyle(normal, pointerOver, pressed, disabled, path);
        style = applyFocusedLook(style, entry, path);
        return applyHorizontalPadding(style, entry, path);
    }

    private ButtonStyle applyFocusedLook(ButtonStyle style, JsonValue button, String path) {
        JsonValue focused = button.get("focused");
        if (focused == null) {
            return style;
        }
        LabelStyle look = readLook(focused, path + ".focused");
        try {
            return style.withFocused(look);
        } catch (IllegalArgumentException mixedFonts) {
            throw new SkinFileException(path + ": " + mixedFonts.getMessage());
        }
    }

    private static ButtonStyle createButtonStyle(LabelStyle normal, LabelStyle pointerOver,
            LabelStyle pressed, LabelStyle disabled, String path) {
        try {
            return ButtonStyle.of(normal, pointerOver, pressed, disabled);
        } catch (IllegalArgumentException mixedFonts) {
            throw new SkinFileException(path + ": " + mixedFonts.getMessage());
        }
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
        MenuBarStyle style = createMenuBarStyle(titles, items, path);
        style = applyTriggerColor(style, entry, path);
        return applyTriggerUnderline(style, entry, path);
    }

    private static MenuBarStyle createMenuBarStyle(ButtonStyle titles, ButtonStyle items,
            String path) {
        try {
            return MenuBarStyle.of(titles, items);
        } catch (IllegalArgumentException mixedFonts) {
            throw new SkinFileException(path + ": " + mixedFonts.getMessage());
        }
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

    private LabelStyle readLook(JsonValue value, String path) {
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

    private static UiFont readFont(JsonValue value, String path) {
        if (!value.isString()) {
            throw new SkinFileException(path + ": must be text, one of "
                    + String.join(", ", FONT_NAMES));
        }
        String name = value.asString();
        if (name.equals("tall")) {
            return UiFont.TALL;
        }
        if (name.equals("square")) {
            return UiFont.SQUARE;
        }
        throw new SkinFileException(path + ": unknown font \"" + name + "\". Known fonts: "
                + String.join(", ", FONT_NAMES));
    }

    private int readColor(JsonValue value, String path) {
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
