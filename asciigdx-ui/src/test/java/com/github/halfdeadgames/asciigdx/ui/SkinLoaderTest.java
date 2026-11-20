package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SkinLoaderTest {

    private static final int NAVY = 0x203070FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final String COLORS_AND_LABELS = """
            "colors": { "white": "#FFFFFF", "navy": "#203070" },
            "labels": {
              "plain": { "font": "square", "foreground": "white", "background": "navy" }
            }""";

    private record StarStyle(int color, LabelStyle look) {
    }

    private static final SkinSectionReader<StarStyle> STAR_READER = (entry, context) -> {
        if (!entry.isObject()) {
            throw context.error("must be an object");
        }
        return new StarStyle(context.readColor(entry.get("color")),
                context.readLook(entry.get("look")));
    };

    private static SkinLoader starLoader() {
        return SkinLoader.create().withSection("stars", STAR_READER);
    }

    private static String withStars(String starsBody) {
        return "{" + COLORS_AND_LABELS + ", \"stars\": {" + starsBody + "}}";
    }

    @Test
    void registeredSectionReadsEntriesInFileOrder() {
        List<String> paths = new ArrayList<>();
        SkinLoader loader = SkinLoader.create().withSection("stars", (entry, context) -> {
            paths.add(context.getPath());
            return entry.name;
        });

        Skin skin = loader.fromJson(withStars("\"b\": 1, \"a\": 2, \"c\": 3"));

        assertEquals(List.of("stars.b", "stars.a", "stars.c"), paths);
        assertEquals("a", skin.getCustomStyle("stars", "a", String.class));
        assertTrue(skin.hasCustomStyle("stars", "c"));
        assertFalse(skin.hasCustomStyle("stars", "z"));
        assertFalse(skin.hasCustomStyle("other", "a"));
    }

    @Test
    void contextReadsNamedColorsHexAndLooks() {
        Skin skin = starLoader().fromJson(withStars("""
                "named": { "color": "navy", "look": "plain" },
                "hex": { "color": "#FF000080",
                  "look": { "font": "tall", "foreground": "#FFFFFF", "background": "navy" } }"""));

        StarStyle named = skin.getCustomStyle("stars", "named", StarStyle.class);
        StarStyle hex = skin.getCustomStyle("stars", "hex", StarStyle.class);

        assertEquals(NAVY, named.color());
        assertEquals(LabelStyle.of(UiFont.SQUARE, WHITE, NAVY), named.look());
        assertEquals(0xFF000080, hex.color());
        assertEquals(LabelStyle.of(UiFont.TALL, WHITE, NAVY), hex.look());
    }

    @Test
    void contextErrorsNameTheEntryPath() {
        var thrown = assertThrows(SkinFileException.class,
                () -> starLoader().fromJson(withStars("\"bad\": 5")));
        assertTrue(thrown.getMessage().startsWith("stars.bad: must be an object"),
                thrown.getMessage());

        var badColor = assertThrows(SkinFileException.class, () -> starLoader().fromJson(
                withStars("\"bad\": { \"color\": \"missing\", \"look\": \"plain\" }")));
        assertTrue(badColor.getMessage().contains("stars.bad"), badColor.getMessage());
        assertTrue(badColor.getMessage().contains("missing"), badColor.getMessage());

        var badLook = assertThrows(SkinFileException.class, () -> starLoader().fromJson(
                withStars("\"bad\": { \"color\": \"navy\", \"look\": \"nope\" }")));
        assertTrue(badLook.getMessage().contains("stars.bad"), badLook.getMessage());
    }

    @Test
    void nullFromReaderIsAnErrorNamingTheEntry() {
        SkinLoader loader = SkinLoader.create().withSection("stars", (entry, context) -> null);

        var thrown = assertThrows(SkinFileException.class,
                () -> loader.fromJson(withStars("\"x\": 1")));

        assertTrue(thrown.getMessage().contains("stars.x"), thrown.getMessage());
    }

    @Test
    void duplicateEntryNameInCustomSectionIsRejected() {
        var thrown = assertThrows(SkinFileException.class, () -> SkinLoader.create()
                .withSection("stars", (entry, context) -> entry.name)
                .fromJson("{\"stars\": {\"a\": 1, \"a\": 2}}"));

        assertTrue(thrown.getMessage().contains("stars.a"), thrown.getMessage());
    }

    @Test
    void unregisteredSectionStillThrows() {
        var plain = assertThrows(SkinFileException.class,
                () -> SkinLoader.create().fromJson("{\"stars\": {}}"));
        var viaSkin = assertThrows(SkinFileException.class,
                () -> Skin.fromJson("{\"stars\": {}}"));

        assertEquals(viaSkin.getMessage(), plain.getMessage());
        assertTrue(plain.getMessage().contains("unknown section \"stars\""), plain.getMessage());
    }

    @Test
    void builtInAndDuplicateSectionNamesAreRejected() {
        for (String builtIn : List.of("colors", "labels", "buttons", "menuBars", "checkBoxes",
                "radioButtons", "textFields", "progressBars", "sliders")) {
            assertThrows(IllegalArgumentException.class,
                    () -> SkinLoader.create().withSection(builtIn, STAR_READER));
        }
        assertThrows(IllegalArgumentException.class,
                () -> starLoader().withSection("stars", STAR_READER));
    }

    @Test
    void withSectionLeavesTheOriginalLoaderUnchanged() {
        SkinLoader original = SkinLoader.create();

        SkinLoader extended = original.withSection("stars", STAR_READER);

        assertThrows(SkinFileException.class, () -> original.fromJson("{\"stars\": {}}"));
        assertNotNull(extended.fromJson("{\"stars\": {}}"));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> SkinLoader.create().withSection(null,
                STAR_READER));
        assertThrows(NullPointerException.class, () -> SkinLoader.create().withSection("a", null));
        assertThrows(NullPointerException.class, () -> SkinLoader.create().fromJson(null));
        assertThrows(NullPointerException.class, () -> SkinLoader.create().fromFile(null));
    }

    @Test
    void getCustomStyleRejectsWrongTypeMissingNameAndUnregisteredSection() {
        Skin skin = starLoader().fromJson(withStars("\"a\": { \"color\": \"navy\","
                + " \"look\": \"plain\" }"));

        var wrongType = assertThrows(IllegalArgumentException.class,
                () -> skin.getCustomStyle("stars", "a", String.class));
        var missing = assertThrows(IllegalArgumentException.class,
                () -> skin.getCustomStyle("stars", "zzz", StarStyle.class));
        var unregistered = assertThrows(IllegalArgumentException.class,
                () -> skin.getCustomStyle("nope", "a", StarStyle.class));

        assertTrue(wrongType.getMessage().contains("java.lang.String"), wrongType.getMessage());
        assertTrue(missing.getMessage().contains("zzz") && missing.getMessage().contains("a"),
                missing.getMessage());
        assertTrue(unregistered.getMessage().contains("nope"), unregistered.getMessage());
        assertThrows(NullPointerException.class, () -> skin.getCustomStyle(null, "a", Object.class));
        assertThrows(NullPointerException.class, () -> skin.getCustomStyle("stars", null,
                Object.class));
        assertThrows(NullPointerException.class, () -> skin.getCustomStyle("stars", "a", null));
    }

    @Test
    void registeredSectionMissingFromTheFileHasNoStyles() {
        Skin skin = starLoader().fromJson("{}");

        assertFalse(skin.hasCustomStyle("stars", "a"));
        assertThrows(IllegalArgumentException.class,
                () -> skin.getCustomStyle("stars", "a", StarStyle.class));
    }

    @Test
    void contextIsInvalidAfterTheEntryIsRead() {
        SkinEntryContext[] kept = new SkinEntryContext[1];
        SkinLoader.create().withSection("stars", (entry, context) -> {
            kept[0] = context;
            return entry.name;
        }).fromJson("{\"stars\": {\"a\": 1}}");

        assertThrows(IllegalStateException.class, () -> kept[0].getPath());
    }

    @Test
    void defaultSkinLoadsTheSameThroughSkinAndThroughTheLoader() {
        // FileHandle over a plain File needs no Gdx.files; it replaces java.nio.file here.
        FileHandle file = new FileHandle(new File("..", "assets/default-skin.json"));

        Skin viaSkin = Skin.fromFile(file);
        Skin viaLoader = SkinLoader.create().fromFile(file);

        assertEquals(viaSkin.toString(), viaLoader.toString());
        assertEquals(viaSkin.getRgba("transparent"), viaLoader.getRgba("transparent"));
        assertEquals(viaSkin.getLabelStyle("title"), viaLoader.getLabelStyle("title"));
        assertEquals(viaSkin.getButtonStyle("button"), viaLoader.getButtonStyle("button"));
        assertEquals(viaSkin.getButtonStyle("tallButton"),
                viaLoader.getButtonStyle("tallButton"));
        assertEquals(viaSkin.getCheckBoxStyle("checkBox"), viaLoader.getCheckBoxStyle("checkBox"));
        assertEquals(viaSkin.getMenuBarStyle("menuBar"), viaLoader.getMenuBarStyle("menuBar"));
        assertEquals(viaSkin.getTextFieldStyle("default"), viaLoader.getTextFieldStyle("default"));
        assertEquals(viaSkin.getProgressBarStyle("progressBar"),
                viaLoader.getProgressBarStyle("progressBar"));
        assertEquals(viaSkin.getSliderStyle("slider"), viaLoader.getSliderStyle("slider"));
    }
}
