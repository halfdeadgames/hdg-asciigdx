package com.github.halfdeadgames.asciigdx.ui;

import static com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition.optionalKey;
import static com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition.requiredKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lists every key of every built-in skin file section, with its type, default, and description.
 * The skin reader parses by this schema, so the key order here is the order of the "Known keys"
 * lists in errors.
 *
 * <p>Example, listing the keys of the {@code labels} section:
 * <pre>{@code
 * for (SkinKeyDefinition key : SkinSchema.getKeys(SkinSchema.LABELS)) {
 *     System.out.println(key.name() + (key.required() ? " (required)" : ""));
 * }
 * }</pre>
 *
 * <p>This class is static-only and cannot be instantiated. All of it is immutable and safe to
 * use from any thread.
 */
public final class SkinSchema {

    /** Section name of the color definitions. */
    public static final String COLORS = "colors";
    /** Section name of the label styles. */
    public static final String LABELS = "labels";
    /** Section name of the button styles. */
    public static final String BUTTONS = "buttons";
    /** Section name of the menu bar styles. */
    public static final String MENU_BARS = "menuBars";
    /** Section name of the check box styles. */
    public static final String CHECK_BOXES = "checkBoxes";
    /** Section name of the radio button styles. */
    public static final String RADIO_BUTTONS = "radioButtons";
    /** Section name of the text field styles. */
    public static final String TEXT_FIELDS = "textFields";
    /** Section name of the progress bar styles. */
    public static final String PROGRESS_BARS = "progressBars";
    /** Section name of the slider styles. */
    public static final String SLIDERS = "sliders";
    /** Section name of the scroll pane styles. */
    public static final String SCROLL_PANES = "scrollPanes";
    /** Section name of the select box styles. */
    public static final String SELECT_BOXES = "selectBoxes";
    /** Section name of the window styles. */
    public static final String WINDOWS = "windows";
    /** Section name of the dialog styles. */
    public static final String DIALOGS = "dialogs";

    private static final List<String> SECTION_NAMES = Collections.unmodifiableList(Arrays.asList(
            COLORS, LABELS, BUTTONS, MENU_BARS, CHECK_BOXES, RADIO_BUTTONS, TEXT_FIELDS,
            PROGRESS_BARS, SLIDERS, SCROLL_PANES, SELECT_BOXES, WINDOWS, DIALOGS));

    private static final String SAME_AS_NORMAL = "same as normal";
    private static final String REMOVED_FONT_EXPLANATION = "the \"font\" key was removed, because "
            + "the UI now picks the font by job (words use the text font, drawn parts use the "
            + "square font); delete the key";
    private static final String REMOVED_CARET_BACKGROUND_EXPLANATION = "the caret is now a glyph "
            + "in the text font and has no background; delete the \"background\" key";

    private static final SkinKeyType COLOR = new SkinKeyType.Color();
    private static final SkinKeyType LOOK =
            SkinKeyType.StyleReference.nameOrObject(SkinStyleKind.LABEL);
    private static final SkinKeyType BUTTON_REFERENCE =
            SkinKeyType.StyleReference.nameOrObject(SkinStyleKind.BUTTON);
    private static final SkinKeyType MARK_FONT = new SkinKeyType.Choice("one of",
            Arrays.asList("text", "square"));

    static final SkinObjectSchema LABEL = SkinObjectSchema.of("a label object",
            requiredKey("foreground", COLOR, "Color of the characters."),
            requiredKey("background", COLOR, "Color behind the characters."))
            .withRemovedKey("font", REMOVED_FONT_EXPLANATION);

    static final SkinObjectSchema BUTTON = SkinObjectSchema.of("a button object",
            requiredKey("normal", LOOK, "Look when nothing special happens."),
            optionalKey("pointerOver", LOOK, SAME_AS_NORMAL, "Look while the pointer is over it."),
            optionalKey("pressed", LOOK, SAME_AS_NORMAL, "Look while it is pressed."),
            optionalKey("disabled", LOOK, SAME_AS_NORMAL, "Look while it is disabled."),
            optionalKey("focused", LOOK, "same as pointerOver",
                    "Look while it has keyboard focus."),
            optionalKey("horizontalPadding", new SkinKeyType.WholeNumber(0, "cells"),
                    "1", "Empty cells on each side of the text."));

    private static final SkinObjectSchema MENU_BAR = SkinObjectSchema.of("a menu bar object",
            requiredKey("titles", BUTTON_REFERENCE, "Button style of the titles in the bar."),
            requiredKey("items", BUTTON_REFERENCE, "Button style of the rows in a drop-down."),
            optionalKey("triggerColor", COLOR, "no trigger color",
                    "Color of the letter that opens an entry."),
            optionalKey("triggerUnderline", new SkinKeyType.Flag(), "false",
                    "Whether the trigger letter is underlined."));

    private static final SkinObjectSchema TOGGLE = SkinObjectSchema.of("a toggle object",
            requiredKey("button", BUTTON_REFERENCE,
                    "Button style that gives the toggle its looks."),
            requiredKey("unchecked", new SkinKeyType.Mark(), "Mark drawn while unchecked."),
            requiredKey("checked", new SkinKeyType.Mark(),
                    "Mark drawn while checked; as long as the unchecked mark."),
            optionalKey("gap", new SkinKeyType.WholeNumber(0, "text columns"), "1",
                    "Text columns between the mark and the label."),
            optionalKey("markFont", MARK_FONT, "text", "Font of the mark."));

    private static final SkinObjectSchema CARET = SkinObjectSchema.of("a caret object",
            requiredKey("glyph", new SkinKeyType.Glyph(), "The character drawn as the caret."),
            optionalKey("foreground", COLOR, "the text color of the field's current look",
                    "Color of the caret glyph."),
            optionalKey("blinkSeconds", new SkinKeyType.DecimalNumber(0, Float.MAX_VALUE,
                    "a number of seconds", "zero or greater and finite"), "0, a steady caret",
                    "Seconds the caret is on, then off for as long."))
            .withRemovedKey("background", REMOVED_CARET_BACKGROUND_EXPLANATION);

    private static final SkinObjectSchema TEXT_FIELD = SkinObjectSchema.of("a text field object",
            requiredKey("normal", LOOK, "Look when nothing special happens."),
            optionalKey("focused", LOOK, SAME_AS_NORMAL, "Look while the field has focus."),
            optionalKey("disabled", LOOK, SAME_AS_NORMAL, "Look while the field is disabled."),
            optionalKey("placeholder", LOOK, SAME_AS_NORMAL,
                    "Look of the hint text shown while the field is empty."),
            optionalKey("caret", new SkinKeyType.Group(CARET),
                    "a steady thin bar in the field's text color", "How the caret looks."));

    private static final SkinObjectSchema PROGRESS_BAR = SkinObjectSchema.of(
            "a progress bar object",
            requiredKey("fill", LOOK, "Look of the filled part."),
            requiredKey("empty", LOOK, "Look of the empty part."));

    private static final SkinObjectSchema SLIDER_LOOKS = SkinObjectSchema.of("an object",
            requiredKey("normal", LOOK, "Look when nothing special happens."),
            optionalKey("focused", LOOK, SAME_AS_NORMAL, "Look while the slider has focus."),
            optionalKey("disabled", LOOK, SAME_AS_NORMAL, "Look while the slider is disabled."));

    private static final String DEFAULT_GLYPHS = "line glyphs for the track, a full block for "
            + "the knob";
    private static final SkinObjectSchema SLIDER = SkinObjectSchema.of("a slider object",
            optionalKey("trackGlyph", new SkinKeyType.Glyph(), DEFAULT_GLYPHS,
                    "Character of the track; given together with knobGlyph."),
            optionalKey("knobGlyph", new SkinKeyType.Glyph(), DEFAULT_GLYPHS,
                    "Character of the knob; given together with trackGlyph."),
            requiredKey("track", new SkinKeyType.Group(SLIDER_LOOKS), "Looks of the track."),
            requiredKey("knob", new SkinKeyType.Group(SLIDER_LOOKS), "Looks of the knob."))
            .withKeysTogether("trackGlyph", "knobGlyph");

    private static final SkinObjectSchema SCROLL_PANE = SkinObjectSchema.of(
            "a scroll pane object",
            requiredKey("track", COLOR, "Color of the scroll bar track."),
            requiredKey("thumb", COLOR, "Color of the scroll bar thumb."),
            optionalKey("thumbPointerOver", COLOR, "same as thumb",
                    "Thumb color while the pointer is over it."),
            optionalKey("thumbPressed", COLOR, "same as thumb",
                    "Thumb color while it is dragged."),
            optionalKey("stepButtons", new SkinKeyType.Flag(), "false",
                    "Whether each scroll bar shows an arrow at both ends."),
            optionalKey("stepArrow", COLOR, "same as thumb", "Arrow color."),
            optionalKey("stepArrowPointerOver", COLOR, "same as thumbPointerOver",
                    "Arrow color while the pointer is over it."),
            optionalKey("stepArrowPressed", COLOR, "same as thumbPressed",
                    "Arrow color while it is held."),
            optionalKey("upArrow", new SkinKeyType.Glyph(), "U+25B2",
                    "Character of the arrow at the top of a vertical bar."),
            optionalKey("downArrow", new SkinKeyType.Glyph(), "U+25BC",
                    "Character of the arrow at the bottom of a vertical bar."),
            optionalKey("leftArrow", new SkinKeyType.Glyph(), "U+25C4",
                    "Character of the arrow at the left of a horizontal bar."),
            optionalKey("rightArrow", new SkinKeyType.Glyph(), "U+25BA",
                    "Character of the arrow at the right of a horizontal bar."),
            optionalKey("arrowFont", MARK_FONT, "square", "Font of the arrows."));

    private static final SkinObjectSchema SELECT_BOX = SkinObjectSchema.of(
            "a select box object",
            requiredKey("button", BUTTON_REFERENCE,
                    "Button style of the closed box."),
            requiredKey("list", BUTTON_REFERENCE,
                    "Button style of the rows in the open list."),
            requiredKey("scrollPane", SkinKeyType.StyleReference.nameOnly(
                    SkinStyleKind.SCROLL_PANE), "Scroll pane style of the open list."),
            optionalKey("arrow", new SkinKeyType.Glyph(), "▼",
                    "The character drawn in the far-right cell of the closed box."),
            optionalKey("arrowFont", MARK_FONT, "square", "Font of the arrow."));

    private static final SkinObjectSchema WINDOW = SkinObjectSchema.of("a window object",
            requiredKey("box", new SkinKeyType.Choice("text, one of",
                    Arrays.asList("single", "double", "heavy", "rounded")),
                    "Line style of the border."),
            requiredKey("border", LOOK, "Look of the border."),
            optionalKey("focusedBorder", LOOK, "same as border",
                    "Look of the border while the window has focus."),
            requiredKey("title", LOOK, "Look of the title."),
            requiredKey("background", COLOR, "Color of the window's inside."),
            optionalKey("closeButton", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.BUTTON),
                    "no close button", "Button style of the close button."),
            optionalKey("closeText", new SkinKeyType.Mark(), "x", "Text of the close button."),
            optionalKey("closeFont", MARK_FONT, "square", "Font of the close text."));

    private static final SkinObjectSchema DIALOG = SkinObjectSchema.of("a dialog object",
            requiredKey("window", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.WINDOW),
                    "Window style of the dialog."),
            requiredKey("message", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.LABEL),
                    "Label style of the message."),
            requiredKey("buttons", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.BUTTON),
                    "Button style of the choice buttons."),
            requiredKey("textField", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.TEXT_FIELD),
                    "Text field style of the input field."),
            optionalKey("dimColor", COLOR, "no dimming",
                    "Color that dims the screen behind the dialog; given together with dimAmount."),
            optionalKey("dimAmount", new SkinKeyType.DecimalNumber(0, 1, "a number from 0 to 1",
                    "from 0 to 1"), "no dimming",
                    "How strongly the screen is dimmed; given together with dimColor."))
            .withKeysTogether("dimColor", "dimAmount");

    private static final Map<String, SkinObjectSchema> ENTRY_SCHEMAS = createEntrySchemas();

    private SkinSchema() {
    }

    private static Map<String, SkinObjectSchema> createEntrySchemas() {
        var schemas = new LinkedHashMap<String, SkinObjectSchema>();
        schemas.put(LABELS, LABEL);
        schemas.put(BUTTONS, BUTTON);
        schemas.put(MENU_BARS, MENU_BAR);
        schemas.put(CHECK_BOXES, TOGGLE);
        schemas.put(RADIO_BUTTONS, TOGGLE);
        schemas.put(TEXT_FIELDS, TEXT_FIELD);
        schemas.put(PROGRESS_BARS, PROGRESS_BAR);
        schemas.put(SLIDERS, SLIDER);
        schemas.put(SCROLL_PANES, SCROLL_PANE);
        schemas.put(SELECT_BOXES, SELECT_BOX);
        schemas.put(WINDOWS, WINDOW);
        schemas.put(DIALOGS, DIALOG);
        return schemas;
    }

    /**
     * Returns the built-in section names in file order: {@code colors} first, then the style
     * sections, with {@code dialogs} last. Sections may be added in later releases; the name
     * constants of this class are stable.
     *
     * @return an unmodifiable list of the section names, never {@code null}
     */
    public static List<String> getSectionNames() {
        return SECTION_NAMES;
    }

    /**
     * Returns the keys of an entry of the given section, in the order the errors list them. The
     * {@code colors} section has no keys, because each entry in it is one color. For that reason
     * this method returns an empty list for {@code colors}, while {@link #getEntrySchema} throws:
     * a list of keys can be empty, but there is no object schema for a single color.
     *
     * @param sectionName a built-in section name, such as {@link #LABELS}; must not be
     *        {@code null}
     * @return an unmodifiable list, empty for {@code colors}, never {@code null}
     * @throws NullPointerException if {@code sectionName} is {@code null}
     * @throws IllegalArgumentException if the name is not a built-in section
     */
    public static List<SkinKeyDefinition> getKeys(String sectionName) {
        Objects.requireNonNull(sectionName, "sectionName");
        if (COLORS.equals(sectionName)) {
            return List.of();
        }
        return getEntrySchema(sectionName).keys();
    }

    /**
     * Returns the schema of an entry of the given section. The {@code checkBoxes} and
     * {@code radioButtons} sections share one schema. Unlike {@link #getKeys}, this method throws
     * for {@code colors}, because the entries of that section are single colors, not objects, so
     * no object schema describes them.
     *
     * @param sectionName a built-in section name other than {@code colors}; must not be
     *        {@code null}
     * @return the immutable schema, never {@code null}
     * @throws NullPointerException if {@code sectionName} is {@code null}
     * @throws IllegalArgumentException if the name is not a built-in section, or is
     *         {@code colors}
     */
    public static SkinObjectSchema getEntrySchema(String sectionName) {
        Objects.requireNonNull(sectionName, "sectionName");
        SkinObjectSchema schema = ENTRY_SCHEMAS.get(sectionName);
        if (schema == null) {
            throw new IllegalArgumentException("No entry schema for section \"" + sectionName
                    + "\"");
        }
        return schema;
    }
}
