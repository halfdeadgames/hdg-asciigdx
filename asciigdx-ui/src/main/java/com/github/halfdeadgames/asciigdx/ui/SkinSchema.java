package com.github.halfdeadgames.asciigdx.ui;

import static com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition.optional;
import static com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition.required;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists every key of every built-in skin file section, with its type, default, and description.
 * The skin reader parses by this schema, so the key order here is the order of the "Known keys"
 * lists in errors.
 */
final class SkinSchema {

    static final String COLORS = "colors";
    static final String LABELS = "labels";
    static final String BUTTONS = "buttons";
    static final String MENU_BARS = "menuBars";
    static final String CHECK_BOXES = "checkBoxes";
    static final String RADIO_BUTTONS = "radioButtons";
    static final String TEXT_FIELDS = "textFields";
    static final String PROGRESS_BARS = "progressBars";
    static final String SLIDERS = "sliders";
    static final String SCROLL_PANES = "scrollPanes";
    static final String WINDOWS = "windows";
    static final String DIALOGS = "dialogs";

    private static final List<String> SECTION_NAMES = Collections.unmodifiableList(Arrays.asList(
            COLORS, LABELS, BUTTONS, MENU_BARS, CHECK_BOXES, RADIO_BUTTONS, TEXT_FIELDS,
            PROGRESS_BARS, SLIDERS, SCROLL_PANES, WINDOWS, DIALOGS));

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
            required("foreground", COLOR, "Color of the characters."),
            required("background", COLOR, "Color behind the characters."))
            .withRemovedKey("font", REMOVED_FONT_EXPLANATION);

    static final SkinObjectSchema BUTTON = SkinObjectSchema.of("a button object",
            required("normal", LOOK, "Look when nothing special happens."),
            optional("pointerOver", LOOK, SAME_AS_NORMAL, "Look while the pointer is over it."),
            optional("pressed", LOOK, SAME_AS_NORMAL, "Look while it is pressed."),
            optional("disabled", LOOK, SAME_AS_NORMAL, "Look while it is disabled."),
            optional("focused", LOOK, "same as pointerOver", "Look while it has keyboard focus."),
            optional("horizontalPadding", new SkinKeyType.WholeNumber(0, "cells"),
                    "1", "Empty cells on each side of the text."));

    private static final SkinObjectSchema MENU_BAR = SkinObjectSchema.of("a menu bar object",
            required("titles", BUTTON_REFERENCE, "Button style of the titles in the bar."),
            required("items", BUTTON_REFERENCE, "Button style of the rows in a drop-down."),
            optional("triggerColor", COLOR, "no trigger color",
                    "Color of the letter that opens an entry."),
            optional("triggerUnderline", new SkinKeyType.Flag(), "false",
                    "Whether the trigger letter is underlined."));

    private static final SkinObjectSchema TOGGLE = SkinObjectSchema.of("a toggle object",
            required("button", BUTTON_REFERENCE, "Button style that gives the toggle its looks."),
            required("unchecked", new SkinKeyType.Mark(), "Mark drawn while unchecked."),
            required("checked", new SkinKeyType.Mark(),
                    "Mark drawn while checked; as long as the unchecked mark."),
            optional("gap", new SkinKeyType.WholeNumber(0, "text columns"), "1",
                    "Text columns between the mark and the label."),
            optional("markFont", MARK_FONT, "text", "Font of the mark."));

    private static final SkinObjectSchema CARET = SkinObjectSchema.of("a caret object",
            required("glyph", new SkinKeyType.Glyph(), "The character drawn as the caret."),
            optional("foreground", COLOR, "the text color of the field's current look",
                    "Color of the caret glyph."),
            optional("blinkSeconds", new SkinKeyType.DecimalNumber(0, Float.MAX_VALUE,
                    "a number of seconds", "zero or greater and finite"), "0, a steady caret",
                    "Seconds the caret is on, then off for as long."))
            .withRemovedKey("background", REMOVED_CARET_BACKGROUND_EXPLANATION);

    private static final SkinObjectSchema TEXT_FIELD = SkinObjectSchema.of("a text field object",
            required("normal", LOOK, "Look when nothing special happens."),
            optional("focused", LOOK, SAME_AS_NORMAL, "Look while the field has focus."),
            optional("disabled", LOOK, SAME_AS_NORMAL, "Look while the field is disabled."),
            optional("placeholder", LOOK, SAME_AS_NORMAL,
                    "Look of the hint text shown while the field is empty."),
            optional("caret", new SkinKeyType.Group(CARET),
                    "a steady thin bar in the field's text color", "How the caret looks."));

    private static final SkinObjectSchema PROGRESS_BAR = SkinObjectSchema.of(
            "a progress bar object",
            required("fill", LOOK, "Look of the filled part."),
            required("empty", LOOK, "Look of the empty part."));

    private static final SkinObjectSchema SLIDER_LOOKS = SkinObjectSchema.of("an object",
            required("normal", LOOK, "Look when nothing special happens."),
            optional("focused", LOOK, SAME_AS_NORMAL, "Look while the slider has focus."),
            optional("disabled", LOOK, SAME_AS_NORMAL, "Look while the slider is disabled."));

    private static final String DEFAULT_GLYPHS = "line glyphs for the track, a full block for "
            + "the knob";
    private static final SkinObjectSchema SLIDER = SkinObjectSchema.of("a slider object",
            optional("trackGlyph", new SkinKeyType.Glyph(), DEFAULT_GLYPHS,
                    "Character of the track; given together with knobGlyph."),
            optional("knobGlyph", new SkinKeyType.Glyph(), DEFAULT_GLYPHS,
                    "Character of the knob; given together with trackGlyph."),
            required("track", new SkinKeyType.Group(SLIDER_LOOKS), "Looks of the track."),
            required("knob", new SkinKeyType.Group(SLIDER_LOOKS), "Looks of the knob."))
            .withKeysTogether("trackGlyph", "knobGlyph");

    private static final SkinObjectSchema SCROLL_PANE = SkinObjectSchema.of(
            "a scroll pane object",
            required("track", COLOR, "Color of the scroll bar track."),
            required("thumb", COLOR, "Color of the scroll bar thumb."),
            optional("thumbPointerOver", COLOR, "same as thumb",
                    "Thumb color while the pointer is over it."),
            optional("thumbPressed", COLOR, "same as thumb",
                    "Thumb color while it is dragged."));

    private static final SkinObjectSchema WINDOW = SkinObjectSchema.of("a window object",
            required("box", new SkinKeyType.Choice("text, one of",
                    Arrays.asList("single", "double", "heavy", "rounded")),
                    "Line style of the border."),
            required("border", LOOK, "Look of the border."),
            optional("focusedBorder", LOOK, "same as border",
                    "Look of the border while the window has focus."),
            required("title", LOOK, "Look of the title."),
            required("background", COLOR, "Color of the window's inside."),
            optional("closeButton", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.BUTTON),
                    "no close button", "Button style of the close button."),
            optional("closeText", new SkinKeyType.Mark(), "x", "Text of the close button."),
            optional("closeFont", MARK_FONT, "square", "Font of the close text."));

    private static final SkinObjectSchema DIALOG = SkinObjectSchema.of("a dialog object",
            required("window", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.WINDOW),
                    "Window style of the dialog."),
            required("message", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.LABEL),
                    "Label style of the message."),
            required("buttons", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.BUTTON),
                    "Button style of the choice buttons."),
            required("textField", SkinKeyType.StyleReference.nameOnly(SkinStyleKind.TEXT_FIELD),
                    "Text field style of the input field."),
            optional("dimColor", COLOR, "no dimming",
                    "Color that dims the screen behind the dialog; given together with dimAmount."),
            optional("dimAmount", new SkinKeyType.DecimalNumber(0, 1, "a number from 0 to 1",
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
        schemas.put(WINDOWS, WINDOW);
        schemas.put(DIALOGS, DIALOG);
        return schemas;
    }

    /** Returns the built-in section names in file order. */
    static List<String> getSectionNames() {
        return SECTION_NAMES;
    }

    /**
     * Returns the keys of an entry of the given section, in order. The {@code colors} section has
     * no keys, because each entry in it is one color.
     *
     * @throws IllegalArgumentException if the name is not a built-in section
     */
    static List<SkinKeyDefinition> getKeys(String sectionName) {
        if (COLORS.equals(sectionName)) {
            return List.of();
        }
        return getEntrySchema(sectionName).keys();
    }

    /**
     * Returns the schema of an entry of the given section.
     *
     * @throws IllegalArgumentException if the name is not a built-in section with object entries
     */
    static SkinObjectSchema getEntrySchema(String sectionName) {
        SkinObjectSchema schema = ENTRY_SCHEMAS.get(sectionName);
        if (schema == null) {
            throw new IllegalArgumentException("No entry schema for section \"" + sectionName
                    + "\"");
        }
        return schema;
    }
}
