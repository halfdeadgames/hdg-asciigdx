package com.github.halfdeadgames.asciigdx.sandbox;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.IntPredicate;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PixelScalerTest {

    private static final String DEFAULT_SHEET =
        "/com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-extended.png";

    private static final String DEFAULT_ORDER =
        "/com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-extended.txt";

    @Test
    void scaleOneReturnsEqualPixels() {
        int[] pixels = {1, 2, 3, 4, 5, 6};
        assertArrayEquals(pixels, PixelScaler.scale(pixels, 3, 2, 1));
    }

    @Test
    void scalesEachPixelIntoABlock() {
        int[] scaled = PixelScaler.scale(new int[] {10, 20, 30, 40}, 2, 2, 2);
        assertArrayEquals(new int[] {
            10, 10, 20, 20,
            10, 10, 20, 20,
            30, 30, 40, 40,
            30, 30, 40, 40}, scaled);
    }

    @Test
    void rejectsScaleBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> PixelScaler.scale(new int[1], 1, 1, 0));
    }

    @Test
    void scale2xRoundsAStaircaseCorner() {
        int[] staircase = {
            1, 0, 0,
            1, 1, 0,
            1, 1, 1};
        int[] scaled = PixelScaler.scale2x(staircase, 3, 3, 3, 3, slot -> false);
        // Centre pixel: only its top-right quarter takes the empty neighbour's colour.
        assertEquals(1, scaled[2 * 6 + 2]);
        assertEquals(0, scaled[2 * 6 + 3]);
        assertEquals(1, scaled[3 * 6 + 2]);
        assertEquals(1, scaled[3 * 6 + 3]);
    }

    @Test
    void scale2xKeepsSolidBlocksAndIsolatedPixelsAsPlainBlocks() {
        int[] solid = {1, 1, 1, 1, 1, 1, 1, 1, 1};
        assertArrayEquals(PixelScaler.scale(solid, 3, 3, 2), PixelScaler.scale2x(solid, 3, 3, 3, 3, slot -> false));
        int[] isolated = {0, 0, 0, 0, 1, 0, 0, 0, 0};
        assertArrayEquals(PixelScaler.scale(isolated, 3, 3, 2), PixelScaler.scale2x(isolated, 3, 3, 3, 3, slot -> false));
    }

    @Test
    void scale2xNeverReadsAcrossCellEdges() {
        // Two 2x2 cells side by side; each is one solid colour, so no pixel may change.
        int[] pixels = {
            1, 1, 0, 0,
            1, 1, 0, 0};
        assertArrayEquals(PixelScaler.scale(pixels, 4, 2, 2), PixelScaler.scale2x(pixels, 4, 2, 2, 2, slot -> false));
    }

    @Test
    void lockedRangesAreRecognised() {
        assertEquals(true, PixelScaler.isScale2xLocked(0x2500));
        assertEquals(true, PixelScaler.isScale2xLocked(0x259F));
        assertEquals(true, PixelScaler.isScale2xLocked(0xE000));
        assertEquals(true, PixelScaler.isScale2xLocked(0xF8FF));
        assertEquals(false, PixelScaler.isScale2xLocked('A'));
        assertEquals(false, PixelScaler.isScale2xLocked(0x25A0));
    }

    @Test
    void scale2xOnTheDefaultSheetLocksGlyphsAndOnlyCopiesSourceColours() throws IOException {
        int cell = 8;
        BufferedImage image;
        try (InputStream stream = PixelScalerTest.class.getResourceAsStream(DEFAULT_SHEET)) {
            image = ImageIO.read(stream);
        }
        String text;
        try (InputStream stream = PixelScalerTest.class.getResourceAsStream(DEFAULT_ORDER)) {
            text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int columns = width / cell;
        GlyphOrder order = GlyphOrderText.read(text, columns);
        int[] codePoints = new int[order.size()];
        for (int slot = 0; slot < codePoints.length; slot++) {
            codePoints[slot] = order.codePointAt(slot);
        }
        int[] source = image.getRGB(0, 0, width, height, null, 0, width);
        int[] plain = PixelScaler.scale(source, width, height, 2);
        int[] smooth = PixelScaler.scale2xGlyphSheet(source, width, height, cell, codePoints);
        int scaledWidth = width * 2;
        int lockedCells = 0;

        for (int slot = 0; slot < codePoints.length; slot++) {
            int left = (slot % columns) * cell * 2;
            int top = (slot / columns) * cell * 2;
            boolean locked = PixelScaler.isScale2xLocked(codePoints[slot]);
            Set<Integer> sourceColours = new HashSet<>();
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) {
                    sourceColours.add(source[((top / 2) + y) * width + left / 2 + x]);
                }
            }
            for (int y = 0; y < cell * 2; y++) {
                for (int x = 0; x < cell * 2; x++) {
                    int index = (top + y) * scaledWidth + left + x;
                    if (locked) {
                        assertEquals(plain[index], smooth[index], "locked U+" + Integer.toHexString(codePoints[slot]));
                    }
                    assertEquals(true, sourceColours.contains(smooth[index]), "colour in slot " + slot);
                }
            }
            if (locked) {
                lockedCells++;
            }
        }
        assertEquals(true, lockedCells > 0, "sheet has locked glyphs");
    }

    private static final int LIT = 0xFFFFFFFF;

    private static int[] variant(int[] pixels, int width, int height, int cell, IntPredicate locked,
        PixelScaler.Scale2xMode mode) {
        return PixelScaler.scale2x(pixels, width, height, cell, cell, locked, mode, pixel -> (pixel >>> 24) == 0);
    }

    private static int[] randomOneBitSheet(int width, int height, long seed) {
        Random random = new Random(seed);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = random.nextInt(100) < 45 ? LIT : 0;
        }
        return pixels;
    }

    @Test
    void solidRectangleKeepsSquareCornersUnderFillAndLosesThemUnderTrim() {
        int[] pixels = new int[8 * 8];
        for (int y = 2; y <= 5; y++) {
            for (int x = 2; x <= 5; x++) {
                pixels[y * 8 + x] = LIT;
            }
        }
        int[] nearest = PixelScaler.scale(pixels, 8, 8, 2);
        int[] full = variant(pixels, 8, 8, 8, slot -> false, PixelScaler.Scale2xMode.FULL);
        int[] fill = variant(pixels, 8, 8, 8, slot -> false, PixelScaler.Scale2xMode.FILL_ONLY);
        int[] trim = variant(pixels, 8, 8, 8, slot -> false, PixelScaler.Scale2xMode.TRIM_ONLY);

        assertArrayEquals(nearest, fill);
        assertArrayEquals(full, trim);
        int changed = 0;
        for (int i = 0; i < nearest.length; i++) {
            if (nearest[i] != trim[i]) {
                changed++;
            }
        }
        assertEquals(4, changed, "one sub-pixel per convex corner");
    }

    @Test
    void fillOnlyAddsTrimOnlyRemovesAndTogetherTheyReproduceScale2x() {
        int width = 32;
        int height = 16;
        for (long seed = 1; seed <= 20; seed++) {
            int[] pixels = randomOneBitSheet(width, height, seed);
            IntPredicate locked = slot -> slot == 1;
            int[] nearest = PixelScaler.scale(pixels, width, height, 2);
            int[] full = variant(pixels, width, height, 8, locked, PixelScaler.Scale2xMode.FULL);
            int[] fill = variant(pixels, width, height, 8, locked, PixelScaler.Scale2xMode.FILL_ONLY);
            int[] trim = variant(pixels, width, height, 8, locked, PixelScaler.Scale2xMode.TRIM_ONLY);
            for (int i = 0; i < nearest.length; i++) {
                boolean nearestLit = nearest[i] != 0;
                assertEquals(true, !nearestLit || fill[i] != 0, "fill is a superset of nearest");
                assertEquals(true, nearestLit || trim[i] == 0, "trim is a subset of nearest");
                int combined = fill[i] != nearest[i] ? fill[i] : trim[i] != nearest[i] ? trim[i] : nearest[i];
                assertEquals(full[i], combined, "fill and trim reproduce scale2x at " + i);
            }
        }
    }

    @Test
    void lockedCellsStayPlainTwoTimesInEveryVariant() {
        int width = 16;
        int height = 16;
        int[] pixels = randomOneBitSheet(width, height, 7);
        int[] nearest = PixelScaler.scale(pixels, width, height, 2);
        for (PixelScaler.Scale2xMode mode : PixelScaler.Scale2xMode.values()) {
            assertArrayEquals(nearest, variant(pixels, width, height, 8, slot -> true, mode), mode.name());
        }
    }

    private interface Upscaler {
        int[] apply(int[] pixels, int width, int height, int cell, IntPredicate locked);
    }

    private static final IntPredicate BACKGROUND = pixel -> pixel == 0;

    private static final Upscaler MMPX = (pixels, width, height, cell, locked) ->
        PixelScaler.mmpx(pixels, width, height, cell, cell, locked, pixel -> pixel == 0 ? 0 : (pixel & 0xFF) + 1);

    private static final Upscaler SFX = (pixels, width, height, cell, locked) ->
        PixelScaler.scale2xSfx(pixels, width, height, cell, cell, locked);

    private static final Upscaler LIGHT = (pixels, width, height, cell, locked) ->
        PixelScaler.light(pixels, width, height, cell, cell, locked, BACKGROUND);

    private static final Upscaler[] NEW_UPSCALERS = {MMPX, SFX, LIGHT};

    private static int[] randomSheet(int width, int height, long seed, int colours) {
        Random random = new Random(seed);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = random.nextInt(100) < 45 ? 0xFF000000 | (1 + random.nextInt(colours)) : 0;
        }
        return pixels;
    }

    private static int[] block(int size, int left, int top, int right, int bottom) {
        int[] pixels = new int[size * size];
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                pixels[y * size + x] = LIT;
            }
        }
        return pixels;
    }

    @Test
    void newUpscalersOnlyCopyInputPixels() {
        for (Upscaler upscaler : NEW_UPSCALERS) {
            for (long seed = 1; seed <= 20; seed++) {
                int[] pixels = randomSheet(32, 16, seed, 3);
                Set<Integer> colours = new HashSet<>();
                for (int pixel : pixels) {
                    colours.add(pixel);
                }
                for (int pixel : upscaler.apply(pixels, 32, 16, 8, slot -> false)) {
                    assertEquals(true, colours.contains(pixel), "new colour " + Integer.toHexString(pixel));
                }
            }
        }
    }

    @Test
    void newUpscalersKeepLockedCellsPlain() {
        int[] pixels = randomSheet(32, 16, 3, 3);
        int[] nearest = PixelScaler.scale(pixels, 32, 16, 2);
        for (Upscaler upscaler : NEW_UPSCALERS) {
            assertArrayEquals(nearest, upscaler.apply(pixels, 32, 16, 8, slot -> true));
            int[] partial = upscaler.apply(pixels, 32, 16, 8, slot -> slot == 1);
            for (int y = 0; y < 16; y++) {
                for (int x = 16; x < 32; x++) {
                    assertEquals(nearest[y * 64 + x], partial[y * 64 + x], "locked cell pixel " + x + "," + y);
                }
            }
        }
    }

    @Test
    void newUpscalersScaleEachCellOnItsOwn() {
        int[] sheet = randomSheet(16, 8, 11, 2);
        for (Upscaler upscaler : NEW_UPSCALERS) {
            int[] together = upscaler.apply(sheet, 16, 8, 8, slot -> false);
            for (int cell = 0; cell < 2; cell++) {
                int[] single = new int[64];
                for (int y = 0; y < 8; y++) {
                    System.arraycopy(sheet, y * 16 + cell * 8, single, y * 8, 8);
                }
                int[] alone = upscaler.apply(single, 8, 8, 8, slot -> false);
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        assertEquals(alone[y * 16 + x], together[y * 32 + cell * 16 + x], "cell " + cell + " at " + x + "," + y);
                    }
                }
            }
        }
    }

    @Test
    void newUpscalersLeaveSolidBlockInteriorsAndIsolatedPixelsPlain() {
        int[] solid = block(8, 1, 1, 6, 6);
        int[] isolated = block(8, 4, 4, 4, 4);
        for (Upscaler upscaler : NEW_UPSCALERS) {
            int[] scaled = upscaler.apply(solid, 8, 8, 8, slot -> false);
            for (int y = 4; y < 12; y++) {
                for (int x = 4; x < 12; x++) {
                    assertEquals(LIT, scaled[y * 16 + x], "interior " + x + "," + y);
                }
            }
        }
        assertArrayEquals(PixelScaler.scale(isolated, 8, 8, 2), MMPX.apply(isolated, 8, 8, 8, slot -> false));
        assertArrayEquals(PixelScaler.scale(isolated, 8, 8, 2), SFX.apply(isolated, 8, 8, 8, slot -> false));
    }

    @Test
    void mmpxKeepsSquareCornersOfASolidRectangle() {
        int[] rectangle = block(8, 2, 2, 5, 5);
        assertArrayEquals(PixelScaler.scale(rectangle, 8, 8, 2), MMPX.apply(rectangle, 8, 8, 8, slot -> false));
    }

    @Test
    void scale2xSfxRoundsAStaircaseCornerLikeScale2x() {
        int[] staircase = {
            1, 0, 0,
            1, 1, 0,
            1, 1, 1};
        int[] scaled = SFX.apply(staircase, 3, 3, 3, slot -> false);
        assertEquals(1, scaled[2 * 6 + 2]);
        assertEquals(0, scaled[2 * 6 + 3]);
        assertEquals(1, scaled[3 * 6 + 2]);
        assertEquals(1, scaled[3 * 6 + 3]);
    }

    @Test
    void lightTurnsAOnePixelVerticalLineIntoAOnePixelLine() {
        int[] line = block(8, 3, 0, 3, 7);
        int[] scaled = LIGHT.apply(line, 8, 8, 8, slot -> false);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                assertEquals(x == 6 ? LIT : 0, scaled[y * 16 + x], "pixel " + x + "," + y);
            }
        }
    }

    @Test
    void lightKeepsAHorizontalLineTwoPixelsTall() {
        int[] line = block(8, 1, 3, 5, 3);
        int[] scaled = LIGHT.apply(line, 8, 8, 8, slot -> false);
        for (int x = 2; x <= 10; x++) {
            int lit = 0;
            for (int y = 0; y < 16; y++) {
                lit += scaled[y * 16 + x] != 0 ? 1 : 0;
            }
            assertEquals(2, lit, "column " + x);
            assertEquals(LIT, scaled[6 * 16 + x]);
            assertEquals(LIT, scaled[7 * 16 + x]);
        }
    }

    @Test
    void lightOnlyClearsPixelsWhoseLeftNeighbourIsForeground() {
        for (long seed = 1; seed <= 20; seed++) {
            int[] pixels = randomSheet(16, 16, seed, 1);
            int[] plain = PixelScaler.scale(pixels, 16, 16, 2);
            int[] light = LIGHT.apply(pixels, 16, 16, 8, slot -> false);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    int index = y * 32 + x;
                    if (plain[index] == light[index]) {
                        continue;
                    }
                    assertEquals(1, x % 2, "only right halves change");
                    assertEquals(false, plain[index - 1] == 0, "left neighbour is foreground");
                    assertEquals(0, plain[index + 1], "right neighbour is background");
                    assertEquals(0, light[index]);
                    assertEquals(true, (x + 1) % 16 != 0, "right neighbour inside the cell");
                }
            }
        }
    }

    @Test
    void everyCellOfTheDefaultSheetScaledByTwoMatchesItsSourceCell() throws IOException {
        int cell = 8;
        int scale = 2;
        BufferedImage image;
        try (InputStream stream = PixelScalerTest.class.getResourceAsStream(DEFAULT_SHEET)) {
            image = ImageIO.read(stream);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int[] source = image.getRGB(0, 0, width, height, null, 0, width);
        int[] scaled = PixelScaler.scale(source, width, height, scale);
        int scaledWidth = width * scale;
        int scaledCell = cell * scale;

        for (int top = 0; top < height; top += cell) {
            for (int left = 0; left < width; left += cell) {
                for (int y = 0; y < scaledCell; y++) {
                    for (int x = 0; x < scaledCell; x++) {
                        int expected = source[(top + y / scale) * width + left + x / scale];
                        int actual = scaled[(top * scale + y) * scaledWidth + left * scale + x];
                        assertEquals(expected, actual, "cell at " + left + "," + top + " pixel " + x + "," + y);
                    }
                }
            }
        }
    }
}
