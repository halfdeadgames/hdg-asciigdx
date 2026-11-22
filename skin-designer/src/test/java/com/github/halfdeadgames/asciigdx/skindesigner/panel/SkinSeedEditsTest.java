package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.skindesigner.document.SkinDocument;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;
import com.github.halfdeadgames.asciigdx.ui.SkinObjectSchema;
import com.github.halfdeadgames.asciigdx.ui.SkinStyleKind;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkinSeedEditsTest {

    private static final File DEFAULT_SKIN = new File("../assets/default-skin.json");
    private static final KeyLocation INLINE_FOREGROUND =
            KeyLocation.ofStyleKey("buttons", "button", "normal").nested("foreground");

    @TempDir
    Path temporaryFolder;

    private SkinDocument document;
    private String originalText;

    @BeforeEach
    void openCopyOfDefaultSkin() throws IOException {
        Path copy = temporaryFolder.resolve("skin.json");
        Files.copy(DEFAULT_SKIN.toPath(), copy);
        document = SkinDocument.open(new FileHandle(copy.toFile()), false);
        originalText = document.toJson();
    }

    private void removeThenAdd(KeyLocation location, SkinKeyType type) {
        document.apply(SkinEdits.reset(location));
        assertNull(location.findValue(document.getTree()));
        document.apply(SkinEdits.add(location, type));
    }

    private JsonValue valueAt(KeyLocation location) {
        return location.findValue(document.getTree());
    }

    @Test
    void anInlineColorEditIsOneUndoStep() {
        document.apply(SkinEdits.setText(INLINE_FOREGROUND, "gold"));

        assertEquals("gold", valueAt(INLINE_FOREGROUND).asString());
        document.undo();
        assertEquals(originalText, document.toJson());
        assertFalse(document.canUndo());
    }

    @Test
    void addsAColorAsTheFirstPaletteName() {
        KeyLocation location = KeyLocation.ofStyleKey("scrollPanes", "scrollPane",
                "thumbPressed");

        removeThenAdd(location, new SkinKeyType.Color());

        assertEquals("black", valueAt(location).asString());
    }

    @Test
    void addsNumbersWithinTheirBounds() {
        KeyLocation padding = KeyLocation.ofStyleKey("buttons", "button", "horizontalPadding");
        document.apply(SkinEdits.add(padding, new SkinKeyType.WholeNumber(0, "cells")));
        assertEquals(0L, valueAt(padding).asLong());

        KeyLocation blink = KeyLocation.ofStyleKey("textFields", "default", "caret")
                .nested("blinkSeconds");
        removeThenAdd(blink, new SkinKeyType.DecimalNumber(0, Float.MAX_VALUE, "a", "b"));
        assertEquals(0.0, valueAt(blink).asDouble());
    }

    @Test
    void addsAFlagAsFalse() {
        KeyLocation underline = KeyLocation.ofStyleKey("menuBars", "menuBar",
                "triggerUnderline");

        removeThenAdd(underline, new SkinKeyType.Flag());

        assertFalse(valueAt(underline).asBoolean());
    }

    @Test
    void addsAChoiceAsItsFirstOption() {
        KeyLocation markFont = KeyLocation.ofStyleKey("checkBoxes", "checkBox", "markFont");

        document.apply(SkinEdits.add(markFont,
                new SkinKeyType.Choice("one of", List.of("text", "square"))));

        assertEquals("text", valueAt(markFont).asString());
    }

    @Test
    void addsAMarkAsX() {
        KeyLocation closeText = KeyLocation.ofStyleKey("windows", "window", "closeText");

        removeThenAdd(closeText, new SkinKeyType.Mark());

        assertEquals("x", valueAt(closeText).asString());
    }

    @Test
    void addsAStyleReferenceAsTheFirstStyleOfItsKind() {
        KeyLocation pointerOver = KeyLocation.ofStyleKey("buttons", "button", "pointerOver");

        removeThenAdd(pointerOver, SkinKeyType.StyleReference.nameOrObject(SkinStyleKind.LABEL));

        assertEquals("text", valueAt(pointerOver).asString());
    }

    @Test
    void groupAndGlyphSeedsHoldTheirRequiredKeys() {
        JsonValue tree = document.getTree();
        var group = new SkinKeyType.Group(SkinObjectSchema.of("an object",
                com.github.halfdeadgames.asciigdx.ui.SkinKeyDefinition.requiredKey("glyph",
                        new SkinKeyType.Glyph(), "a glyph")));

        JsonValue seed = SeedValues.seedFor(group, tree);

        assertTrue(seed.isObject());
        assertNotNull(seed.get("glyph"));
        assertEquals(" ", SeedValues.seedFor(new SkinKeyType.Glyph(), tree).asString());
    }

    @Test
    void addingOneKeyOfAPairAddsBothInOneUndoStep() {
        KeyLocation trackGlyph = KeyLocation.ofStyleKey("sliders", "slider", "trackGlyph");
        KeyLocation knobGlyph = KeyLocation.ofStyleKey("sliders", "slider", "knobGlyph");
        var partners = List.of(SkinKeyDefinition.optionalKey("knobGlyph",
                new SkinKeyType.Glyph(), "default glyphs", "Knob glyph."));

        document.apply(SkinEdits.add(trackGlyph, new SkinKeyType.Glyph(), partners));

        assertNotNull(valueAt(trackGlyph));
        assertNotNull(valueAt(knobGlyph));
        document.undo();
        assertEquals(originalText, document.toJson());
        assertFalse(document.canUndo());
    }

    @Test
    void resettingOneKeyOfAPairRemovesBothInOneUndoStep() {
        KeyLocation trackGlyph = KeyLocation.ofStyleKey("sliders", "slider", "trackGlyph");
        KeyLocation knobGlyph = KeyLocation.ofStyleKey("sliders", "slider", "knobGlyph");
        var partners = List.of(SkinKeyDefinition.optionalKey("knobGlyph",
                new SkinKeyType.Glyph(), "default glyphs", "Knob glyph."));
        document.apply(SkinEdits.add(trackGlyph, new SkinKeyType.Glyph(), partners));
        String withPair = document.toJson();

        document.apply(SkinEdits.reset(knobGlyph, List.of("trackGlyph")));

        assertNull(valueAt(trackGlyph));
        assertNull(valueAt(knobGlyph));
        assertEquals(originalText, document.toJson());
        document.undo();
        assertEquals(withPair, document.toJson());
    }

    @Test
    void aSeedTheReaderRefusesShowsANotChangedMessage() {
        var statuses = new ArrayList<String>();
        var reverted = new AtomicInteger();
        var gateway = new EditGateway(document, statuses::add);
        KeyLocation markFont = KeyLocation.ofStyleKey("checkBoxes", "checkBox", "markFont");

        gateway.apply("checkBoxes.checkBox.markFont", SkinEdits.add(markFont,
                new SkinKeyType.Choice("one of", List.of("bogus"))), reverted::incrementAndGet);

        assertEquals(1, statuses.size());
        assertTrue(statuses.get(0).startsWith("Not changed: "));
        assertEquals(1, reverted.get());
        assertEquals(originalText, document.toJson());
        assertFalse(document.canUndo());
    }

    @Test
    void aStyleReferenceSeedWithoutStylesIsRefused() {
        JsonValue emptyTree = new JsonValue(JsonValue.ValueType.object);

        assertThrows(IllegalStateException.class, () -> SeedValues.seedFor(
                SkinKeyType.StyleReference.nameOnly(SkinStyleKind.WINDOW), emptyTree));
    }
}
