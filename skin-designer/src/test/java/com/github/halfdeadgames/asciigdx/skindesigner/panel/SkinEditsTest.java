package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.github.halfdeadgames.asciigdx.skindesigner.document.SkinDocument;
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

class SkinEditsTest {

    private static final File DEFAULT_SKIN = new File("../assets/default-skin.json");
    private static final KeyLocation LABEL_FOREGROUND =
            KeyLocation.ofStyleKey("labels", "text", "foreground");
    private static final KeyLocation LABEL_BACKGROUND =
            KeyLocation.ofStyleKey("labels", "text", "background");
    private static final KeyLocation CARET_GLYPH =
            KeyLocation.ofStyleKey("textFields", "default", "caret").nested("glyph");
    private static final KeyLocation CARET_BLINK =
            KeyLocation.ofStyleKey("textFields", "default", "caret").nested("blinkSeconds");
    private static final KeyLocation POINTER_OVER =
            KeyLocation.ofStyleKey("buttons", "button", "pointerOver");
    private static final KeyLocation HORIZONTAL_PADDING =
            KeyLocation.ofStyleKey("buttons", "button", "horizontalPadding");
    private static final KeyLocation TRIGGER_UNDERLINE =
            KeyLocation.ofStyleKey("menuBars", "menuBar", "triggerUnderline");
    private static final KeyLocation FIELD_NORMAL =
            KeyLocation.ofStyleKey("textFields", "default", "normal");
    private static final KeyLocation DIM_AMOUNT =
            KeyLocation.ofStyleKey("dialogs", "dialog", "dimAmount");

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

    private String valueText(KeyLocation location) {
        return location.findValue(document.getTree()).asString();
    }

    @Test
    void setsAColorByPaletteName() {
        document.apply(SkinEdits.setText(LABEL_FOREGROUND, "gold"));

        assertEquals("gold", valueText(LABEL_FOREGROUND));
        assertEquals(0xFFD700FF, document.getSkin().getLabelStyle("text").getForegroundRgba());
    }

    @Test
    void setsAColorByHexDigits() {
        document.apply(SkinEdits.setHexColor(LABEL_BACKGROUND, "112233"));
        assertEquals("#112233", valueText(LABEL_BACKGROUND));
        assertEquals(0x112233FF, document.getSkin().getLabelStyle("text").getBackgroundRgba());

        document.apply(SkinEdits.setHexColor(LABEL_BACKGROUND, "#aabbcc44"));
        assertEquals("#AABBCC44", valueText(LABEL_BACKGROUND));
        assertEquals(0xAABBCC44, document.getSkin().getLabelStyle("text").getBackgroundRgba());
    }

    @Test
    void refusesHexDigitsOfTheWrongLength() {
        assertThrows(IllegalArgumentException.class,
                () -> document.apply(SkinEdits.setHexColor(LABEL_BACKGROUND, "12345")));

        assertEquals(originalText, document.toJson());
        assertFalse(document.canUndo());
    }

    @Test
    void setsAGlyphFromACodePointText() {
        document.apply(SkinEdits.setGlyph(CARET_GLYPH, "U+2588"));

        assertEquals("█", valueText(CARET_GLYPH));
        assertEquals(0x2588,
                document.getSkin().getTextFieldStyle("default").getCaret().getGlyphCodePoint());
    }

    @Test
    void refusesAGlyphTextThatIsNotACodePoint() {
        assertThrows(IllegalArgumentException.class,
                () -> document.apply(SkinEdits.setGlyph(CARET_GLYPH, "U+ZZ")));
        assertThrows(IllegalArgumentException.class,
                () -> document.apply(SkinEdits.setGlyph(CARET_GLYPH, "U+110000")));
        assertEquals(originalText, document.toJson());
    }

    @Test
    void resetRemovesAnOptionalKey() {
        assertTrue(POINTER_OVER.findValue(document.getTree()) != null);

        document.apply(SkinEdits.reset(POINTER_OVER));

        assertNull(POINTER_OVER.findValue(document.getTree()));
        assertFalse(document.toJson().contains("pointerOver"));
    }

    @Test
    void numberEditsAddOrReplaceTheKey() {
        document.apply(SkinEdits.setWholeNumber(HORIZONTAL_PADDING, "3", 0));
        assertTrue(document.toJson().contains("\"horizontalPadding\": 3"));

        document.apply(SkinEdits.setDecimalNumber(CARET_BLINK, "0.25", 0, Float.MAX_VALUE));
        assertTrue(document.toJson().contains("\"blinkSeconds\": 0.25"));
    }

    @Test
    void numberEditsRespectTheBounds() {
        assertThrows(IllegalArgumentException.class, () -> document.apply(
                SkinEdits.setWholeNumber(HORIZONTAL_PADDING, "-1", 0)));
        assertThrows(IllegalArgumentException.class, () -> document.apply(
                SkinEdits.setDecimalNumber(DIM_AMOUNT, "1.5", 0, 1)));
        assertThrows(IllegalArgumentException.class, () -> document.apply(
                SkinEdits.setDecimalNumber(DIM_AMOUNT, "", 0, 1)));
        assertEquals(originalText, document.toJson());
    }

    @Test
    void setsAFlag() {
        document.apply(SkinEdits.setFlag(TRIGGER_UNDERLINE, false));

        assertTrue(document.toJson().contains("\"triggerUnderline\": false"));
    }

    @Test
    void replacingAnInlineObjectWithAStyleNameDropsTheObject() {
        document.apply(SkinEdits.setText(FIELD_NORMAL, "text"));

        assertTrue(document.toJson().contains("\"normal\": \"text\""));
    }

    @Test
    void anEditAtAMissingPlaceIsRefused() {
        KeyLocation missing = KeyLocation.ofStyleKey("labels", "noSuchStyle", "foreground");

        assertThrows(IllegalStateException.class,
                () -> document.apply(SkinEdits.setText(missing, "gold")));
        assertEquals(originalText, document.toJson());
    }

    @Test
    void oneCommittedEditIsExactlyOneUndoStep() {
        var statuses = new ArrayList<String>();
        var gateway = new EditGateway(document, statuses::add);

        gateway.apply("labels.text.foreground", SkinEdits.setText(LABEL_FOREGROUND, "gold"),
                () -> { });

        assertEquals(List.of("Changed labels.text.foreground"), statuses);
        assertTrue(document.canUndo());
        document.undo();
        assertEquals(originalText, document.toJson());
        assertFalse(document.canUndo());
    }

    @Test
    void aRefusedEditTellsTheStatusLineAndLeavesNoUndoStep() {
        var statuses = new ArrayList<String>();
        var reverted = new AtomicInteger();
        var gateway = new EditGateway(document, statuses::add);

        gateway.apply("labels.text.foreground",
                SkinEdits.setText(LABEL_FOREGROUND, "noSuchColor"), reverted::incrementAndGet);

        assertEquals(1, statuses.size());
        assertTrue(statuses.get(0).startsWith("Not changed: "));
        assertEquals(1, reverted.get());
        assertFalse(document.canUndo());
        assertEquals(originalText, document.toJson());
    }

    @Test
    void badTypedInputIsRefusedThroughTheGatewayToo() {
        var statuses = new ArrayList<String>();
        var reverted = new AtomicInteger();
        var gateway = new EditGateway(document, statuses::add);

        gateway.apply("labels.text.background", SkinEdits.setHexColor(LABEL_BACKGROUND, "xyz"),
                reverted::incrementAndGet);

        assertEquals(1, reverted.get());
        assertFalse(document.canUndo());
        assertTrue(statuses.get(0).startsWith("Not changed: "));
    }
}
