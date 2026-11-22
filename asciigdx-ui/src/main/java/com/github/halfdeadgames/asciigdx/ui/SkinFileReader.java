package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns skin file text into a {@link Skin}, checking every key, type, and reference. Each object
 * is parsed by its {@link SkinSchema} entry; the section methods then only assemble styles from
 * the parsed values.
 */
final class SkinFileReader {

    private static final String NORMAL = "normal";
    private static final String FOCUSED = "focused";
    private static final String DISABLED = "disabled";
    private static final int OPAQUE_ALPHA = 0xFF;
    private static final int HEX_RADIX = 16;
    private static final int RGB_DIGIT_COUNT = 6;
    private static final int RGBA_DIGIT_COUNT = 8;
    private static final Pattern LINE_IN_MESSAGE = Pattern.compile("line (\\d+)");
    private static final SkinKeyType.StyleReference LOOK_REFERENCE =
            SkinKeyType.StyleReference.nameOrObject(SkinStyleKind.LABEL);

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
        return SkinSchema.getSectionNames().contains(sectionName);
    }

    static Skin read(String json) {
        return read(json, Map.of());
    }

    static Skin read(String json, Map<String, SkinSectionReader<?>> customReaders) {
        JsonValue root = readRoot(json);
        List<String> knownSections = new ArrayList<>(SkinSchema.getSectionNames());
        knownSections.addAll(customReaders.keySet());
        checkKnownKeys(root, knownSections, "section", "The file");
        var reader = new SkinFileReader();
        reader.readSection(root, SkinSchema.COLORS, reader.colors, reader::readColorEntry);
        reader.readSection(root, SkinSchema.LABELS, reader.labelStyles, reader::readLabelStyle);
        reader.readSection(root, SkinSchema.BUTTONS, reader.buttonStyles,
                reader::readButtonStyle);
        reader.readSection(root, SkinSchema.MENU_BARS, reader.menuBarStyles,
                reader::readMenuBarStyle);
        reader.readSection(root, SkinSchema.CHECK_BOXES, reader.checkBoxStyles,
                reader::readToggleStyle);
        reader.readSection(root, SkinSchema.RADIO_BUTTONS, reader.radioButtonStyles,
                reader::readToggleStyle);
        reader.readSection(root, SkinSchema.TEXT_FIELDS, reader.textFieldStyles,
                reader::readTextFieldStyle);
        reader.readSection(root, SkinSchema.PROGRESS_BARS, reader.progressBarStyles,
                reader::readProgressBarStyle);
        reader.readSection(root, SkinSchema.SLIDERS, reader.sliderStyles,
                reader::readSliderStyle);
        reader.readSection(root, SkinSchema.SCROLL_PANES, reader.scrollPaneStyles,
                reader::readScrollPaneStyle);
        reader.readSection(root, SkinSchema.WINDOWS, reader.windowStyles,
                reader::readWindowStyle);
        reader.readSection(root, SkinSchema.DIALOGS, reader.dialogStyles,
                reader::readDialogStyle);
        reader.readCustomSections(root, customReaders);
        return new Skin(reader.colors, reader.labelStyles, reader.buttonStyles,
                reader.menuBarStyles, reader.checkBoxStyles, reader.radioButtonStyles,
                reader.textFieldStyles, reader.progressBarStyles, reader.sliderStyles,
                reader.scrollPaneStyles, reader.windowStyles, reader.dialogStyles,
                reader.customStyles);
    }

    // ---- Parsing by schema ----

    private SkinParsedObject readObject(JsonValue object, SkinObjectSchema schema, String path) {
        return readObject(object, schema, path, requiredKeys -> { });
    }

    // Reads the required keys first, then runs the cross-key checks, then reads the optional keys.
    // An object with several problems therefore reports them in this fixed order, which is not
    // always the order in which the keys are listed.
    private SkinParsedObject readObject(JsonValue object, SkinObjectSchema schema, String path,
            Consumer<SkinParsedObject> afterRequiredKeys) {
        requireObject(object, path,
                schema.objectDescription() + " with the keys "
                        + String.join(", ", schema.getKeyNames()));
        rejectRemovedKeys(object, schema, path);
        checkKnownKeys(object, schema.getKeyNames(), "key", path);
        Map<String, Object> values = new HashMap<>();
        readKeys(object, schema, path, true, values);
        requireKeysTogether(object, schema, path);
        afterRequiredKeys.accept(new SkinParsedObject(values));
        readKeys(object, schema, path, false, values);
        return new SkinParsedObject(values);
    }

    private void readKeys(JsonValue object, SkinObjectSchema schema, String path,
            boolean required, Map<String, Object> values) {
        for (SkinKeyDefinition key : schema.keys()) {
            if (key.required() != required) {
                continue;
            }
            JsonValue value = object.get(key.name());
            if (value != null) {
                values.put(key.name(), readValue(value, key.type(), path + "." + key.name()));
            } else if (required) {
                throw new SkinFileException(path + ": missing \"" + key.name() + "\"");
            }
        }
    }

    private static void rejectRemovedKeys(JsonValue object, SkinObjectSchema schema,
            String path) {
        for (SkinObjectSchema.RemovedKey removed : schema.removedKeys()) {
            if (object.get(removed.name()) != null) {
                throw new SkinFileException(
                        path + "." + removed.name() + ": " + removed.explanation());
            }
        }
    }

    private static void requireKeysTogether(JsonValue object, SkinObjectSchema schema,
            String path) {
        for (SkinObjectSchema.KeyPair pair : schema.togetherPairs()) {
            if ((object.get(pair.first()) == null) != (object.get(pair.second()) == null)) {
                throw new SkinFileException(path + ": \"" + pair.first() + "\" and \""
                        + pair.second() + "\" must be given together");
            }
        }
    }

    private Object readValue(JsonValue value, SkinKeyType type, String path) {
        if (type instanceof SkinKeyType.Color) {
            return readColor(value, path);
        }
        if (type instanceof SkinKeyType.Glyph) {
            return readGlyph(value, path);
        }
        if (type instanceof SkinKeyType.Mark) {
            return readMark(value, path);
        }
        if (type instanceof SkinKeyType.WholeNumber wholeNumber) {
            return readWholeNumber(value, wholeNumber, path);
        }
        if (type instanceof SkinKeyType.DecimalNumber decimalNumber) {
            return readDecimalNumber(value, decimalNumber, path);
        }
        if (type instanceof SkinKeyType.Flag) {
            return readFlag(value, path);
        }
        if (type instanceof SkinKeyType.Choice choice) {
            return readChoice(value, choice, path);
        }
        if (type instanceof SkinKeyType.StyleReference reference) {
            return readStyleReference(value, reference, path);
        }
        if (type instanceof SkinKeyType.Group group) {
            return readObject(value, group.schema(), path);
        }
        throw new IllegalStateException("Unhandled key type " + type);
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

    private static String readMark(JsonValue value, String path) {
        if (!value.isString() || value.asString().isEmpty()) {
            throw new SkinFileException(path + ": must be text of at least one character");
        }
        return value.asString();
    }

    private static int readWholeNumber(JsonValue value, SkinKeyType.WholeNumber type,
            String path) {
        if (!value.isLong() || value.asLong() > Integer.MAX_VALUE) {
            throw new SkinFileException(
                    path + ": must be a whole number of " + type.unit());
        }
        if (value.asLong() < type.minimum()) {
            String limit = type.minimum() == 0 ? "must not be negative"
                    : "must be at least " + type.minimum();
            throw new SkinFileException(path + ": " + limit + ", was " + value.asLong());
        }
        return (int) value.asLong();
    }

    private static float readDecimalNumber(JsonValue value, SkinKeyType.DecimalNumber type,
            String path) {
        if (!value.isNumber()) {
            throw new SkinFileException(path + ": must be " + type.expectation());
        }
        double number = value.asDouble();
        if (!(number >= type.minimum()) || number > type.maximum()) {
            throw new SkinFileException(
                    path + ": must be " + type.rangeDescription() + ", was " + value.asString());
        }
        return (float) number;
    }

    private static boolean readFlag(JsonValue value, String path) {
        if (!value.isBoolean()) {
            throw new SkinFileException(path + ": must be true or false");
        }
        return value.asBoolean();
    }

    private static String readChoice(JsonValue value, SkinKeyType.Choice choice, String path) {
        String name = value.isString() ? value.asString() : "";
        if (!choice.options().contains(name)) {
            throw new SkinFileException(path + ": must be " + choice.introduction() + " "
                    + String.join(", ", choice.options()));
        }
        return name;
    }

    private Object readStyleReference(JsonValue value, SkinKeyType.StyleReference reference,
            String path) {
        SkinStyleKind kind = reference.kind();
        if (value.isString()) {
            return lookUpReference(getStylesOfKind(kind), value.asString(),
                    kind.getStyleName(), path);
        }
        if (reference.inlineAllowed() && value.isObject()) {
            return readInlineStyle(kind, value, path);
        }
        String expectation = "the name of a " + kind.getStyleName();
        if (reference.inlineAllowed()) {
            expectation += " or a " + kind.getObjectName();
        }
        throw new SkinFileException(path + ": must be " + expectation);
    }

    private Map<String, ?> getStylesOfKind(SkinStyleKind kind) {
        return switch (kind) {
            case LABEL -> labelStyles;
            case BUTTON -> buttonStyles;
            case WINDOW -> windowStyles;
            case TEXT_FIELD -> textFieldStyles;
        };
    }

    private Object readInlineStyle(SkinStyleKind kind, JsonValue value, String path) {
        return switch (kind) {
            case LABEL -> readLabelStyle(value, path);
            case BUTTON -> readButtonStyle(value, path);
            default -> throw new IllegalStateException("No inline form for " + kind);
        };
    }

    private static Object lookUpReference(Map<String, ?> known, String name, String kind,
            String path) {
        Object found = known.get(name);
        if (found == null) {
            throw new SkinFileException(path + ": unknown " + kind + " \"" + name + "\". Known "
                    + kind + "s: " + Skin.describeNames(known));
        }
        return found;
    }

    private static <E extends Enum<E>> E readEnum(SkinParsedObject parsed, String key,
            Class<E> enumType) {
        return Enum.valueOf(enumType, parsed.getText(key).toUpperCase(Locale.ROOT));
    }

    private static LabelStyle getLook(SkinParsedObject parsed, String key, LabelStyle fallback) {
        return parsed.getOrElse(key, LabelStyle.class, fallback);
    }

    // ---- Assembling styles ----

    private DialogStyle readDialogStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry, SkinSchema.getEntrySchema(SkinSchema.DIALOGS),
                path);
        DialogStyle style = DialogStyle.of(parsed.get("window", WindowStyle.class),
                parsed.get("message", LabelStyle.class),
                parsed.get("buttons", ButtonStyle.class),
                parsed.get("textField", TextFieldStyle.class));
        if (!parsed.has("dimColor")) {
            return style;
        }
        return style.withDim(parsed.getInt("dimColor"), parsed.getFloat("dimAmount"));
    }

    private ScrollPaneStyle readScrollPaneStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry,
                SkinSchema.getEntrySchema(SkinSchema.SCROLL_PANES), path);
        ScrollPaneStyle style = ScrollPaneStyle.of(parsed.getInt("track"),
                parsed.getInt("thumb"));
        if (parsed.has("thumbPointerOver")) {
            style = style.withThumbPointerOver(parsed.getInt("thumbPointerOver"));
        }
        if (parsed.has("thumbPressed")) {
            style = style.withThumbPressed(parsed.getInt("thumbPressed"));
        }
        return style;
    }

    private WindowStyle readWindowStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry,
                SkinSchema.getEntrySchema(SkinSchema.WINDOWS), path);
        WindowStyle style = WindowStyle.of(readEnum(parsed, "box", BoxStyle.class),
                parsed.get("border", LabelStyle.class), parsed.get("title", LabelStyle.class),
                parsed.getInt("background"));
        if (parsed.has("focusedBorder")) {
            style = style.withFocusedBorder(parsed.get("focusedBorder", LabelStyle.class));
        }
        if (parsed.has("closeButton")) {
            style = style.withCloseButton(parsed.get("closeButton", ButtonStyle.class));
        }
        if (parsed.has("closeText")) {
            style = style.withCloseText(parsed.getText("closeText"));
        }
        if (parsed.has("closeFont")) {
            style = style.withCloseFont(readEnum(parsed, "closeFont", MarkFont.class));
        }
        return style;
    }

    private TextFieldStyle readTextFieldStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry,
                SkinSchema.getEntrySchema(SkinSchema.TEXT_FIELDS), path);
        LabelStyle normal = parsed.get(NORMAL, LabelStyle.class);
        TextFieldStyle style = TextFieldStyle.of(normal)
                .withFocused(getLook(parsed, FOCUSED, normal))
                .withDisabled(getLook(parsed, DISABLED, normal))
                .withPlaceholder(getLook(parsed, "placeholder", normal));
        if (!parsed.has("caret")) {
            return style;
        }
        return style.withCaret(createCaret(parsed.get("caret", SkinParsedObject.class)));
    }

    private static CaretStyle createCaret(SkinParsedObject parsed) {
        CaretStyle style = CaretStyle.none().withGlyph(parsed.getInt("glyph"));
        if (parsed.has("foreground")) {
            style = style.withForegroundRgba(parsed.getInt("foreground"));
        }
        if (parsed.has("blinkSeconds")) {
            style = style.withBlinkSeconds(parsed.getFloat("blinkSeconds"));
        }
        return style;
    }

    private ProgressBarStyle readProgressBarStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry,
                SkinSchema.getEntrySchema(SkinSchema.PROGRESS_BARS), path);
        return ProgressBarStyle.of(parsed.get("fill", LabelStyle.class),
                parsed.get("empty", LabelStyle.class));
    }

    private SliderStyle readSliderStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry,
                SkinSchema.getEntrySchema(SkinSchema.SLIDERS), path);
        SkinParsedObject track = parsed.get("track", SkinParsedObject.class);
        SkinParsedObject knob = parsed.get("knob", SkinParsedObject.class);
        LabelStyle trackNormal = track.get(NORMAL, LabelStyle.class);
        LabelStyle knobNormal = knob.get(NORMAL, LabelStyle.class);
        SliderStyle style = SliderStyle.of(trackNormal, knobNormal)
                .withFocused(getLook(track, FOCUSED, trackNormal),
                        getLook(knob, FOCUSED, knobNormal))
                .withDisabled(getLook(track, DISABLED, trackNormal),
                        getLook(knob, DISABLED, knobNormal));
        if (!parsed.has("trackGlyph")) {
            return style;
        }
        return style.withGlyphs(parsed.getInt("trackGlyph"), parsed.getInt("knobGlyph"));
    }

    private LabelStyle readLabelStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry, SkinSchema.LABEL, path);
        return LabelStyle.of(parsed.getInt("foreground"), parsed.getInt("background"));
    }

    private ButtonStyle readButtonStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry, SkinSchema.BUTTON, path);
        LabelStyle normal = parsed.get(NORMAL, LabelStyle.class);
        ButtonStyle style = ButtonStyle.of(normal, getLook(parsed, "pointerOver", normal),
                getLook(parsed, "pressed", normal), getLook(parsed, DISABLED, normal));
        if (parsed.has(FOCUSED)) {
            style = style.withFocused(parsed.get(FOCUSED, LabelStyle.class));
        }
        if (parsed.has("horizontalPadding")) {
            style = style.withHorizontalPadding(parsed.getInt("horizontalPadding"));
        }
        return style;
    }

    private MenuBarStyle readMenuBarStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry,
                SkinSchema.getEntrySchema(SkinSchema.MENU_BARS), path);
        MenuBarStyle style = MenuBarStyle.of(parsed.get("titles", ButtonStyle.class),
                parsed.get("items", ButtonStyle.class));
        if (parsed.has("triggerColor")) {
            style = style.withTriggerRgba(parsed.getInt("triggerColor"));
        }
        if (parsed.has("triggerUnderline")) {
            style = style.withTriggerUnderline(
                    parsed.get("triggerUnderline", Boolean.class));
        }
        return style;
    }

    private ToggleStyle readToggleStyle(JsonValue entry, String path) {
        SkinParsedObject parsed = readObject(entry,
                SkinSchema.getEntrySchema(SkinSchema.CHECK_BOXES), path,
                requiredKeys -> createToggleStyle(requiredKeys, path));
        ToggleStyle style = createToggleStyle(parsed, path);
        if (parsed.has("gap")) {
            style = style.withGap(parsed.getInt("gap"));
        }
        if (parsed.has("markFont")) {
            style = style.withMarkFont(readEnum(parsed, "markFont", MarkFont.class));
        }
        return style;
    }

    private static ToggleStyle createToggleStyle(SkinParsedObject parsed, String path) {
        try {
            return ToggleStyle.of(parsed.get("button", ButtonStyle.class),
                    parsed.getText("unchecked"), parsed.getText("checked"));
        } catch (IllegalArgumentException unequalMarks) {
            throw new SkinFileException(path + ".checked: " + unequalMarks.getMessage());
        }
    }

    // ---- File structure ----

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

    private static JsonValue readRoot(String json) {
        JsonValue root;
        try {
            root = new JsonReader().parse(json);
        } catch (SerializationException syntaxError) {
            throw new SkinFileException(describeSyntaxError(syntaxError));
        }
        if (root == null || !root.isObject()) {
            throw new SkinFileException("The file must hold one object with the sections "
                    + String.join(", ", SkinSchema.getSectionNames()));
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

    LabelStyle readLook(JsonValue value, String path) {
        return (LabelStyle) readStyleReference(value, LOOK_REFERENCE, path);
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
