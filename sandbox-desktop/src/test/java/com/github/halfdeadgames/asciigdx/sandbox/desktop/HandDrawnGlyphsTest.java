package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HandDrawnGlyphsTest {

    private static final String DEFAULT_LIST =
        "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-extended.txt";

    @Test
    void everyGlyphIsSixteenBySixteen() {
        Map<Integer, boolean[][]> glyphs = HandDrawnGlyphs.all();
        assertFalse(glyphs.isEmpty());
        for (Map.Entry<Integer, boolean[][]> glyph : glyphs.entrySet()) {
            assertEquals(16, glyph.getValue().length, "rows of U+" + Integer.toHexString(glyph.getKey()));
            for (boolean[] row : glyph.getValue()) {
                assertEquals(16, row.length, "columns of U+" + Integer.toHexString(glyph.getKey()));
            }
        }
    }

    @Test
    void everyCodePointExistsInTheDefaultSheet() throws IOException {
        GlyphOrder order = readDefaultOrder();
        for (int codePoint : HandDrawnGlyphs.all().keySet()) {
            assertTrue(order.contains(codePoint), "default sheet lacks U+" + Integer.toHexString(codePoint));
        }
    }

    private static GlyphOrder readDefaultOrder() throws IOException {
        try (InputStream stream = HandDrawnGlyphsTest.class.getClassLoader().getResourceAsStream(DEFAULT_LIST)) {
            assertNotNull(stream, "missing resource " + DEFAULT_LIST);
            return GlyphOrderText.read(new String(stream.readAllBytes(), StandardCharsets.UTF_8), 16);
        }
    }
}
