package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PondGeneratorTest {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 44;
    private static final int START_COLUMN = 40;
    private static final int START_ROW = 22;

    private static PondMap generate(long seed) {
        return PondGenerator.generate(WIDTH, HEIGHT, seed, START_COLUMN, START_ROW);
    }

    private static String fingerprint(PondMap map) {
        var builder = new StringBuilder();
        for (int row = 0; row < map.getHeight(); row++) {
            for (int column = 0; column < map.getWidth(); column++) {
                var cell = map.cell(column, row);
                builder.append(cell.getTerrain() == Terrain.LAND ? 'L' : 'w');
                for (LilySlot slot : LilySlot.values()) {
                    builder.append(cell.hasLily(slot) ? '1' : '0');
                }
            }
        }
        return builder.toString();
    }

    @Test
    void sameSeedGivesSameMap() {
        assertEquals(fingerprint(generate(7L)), fingerprint(generate(7L)));
    }

    @Test
    void differentSeedGivesDifferentMap() {
        assertNotEquals(fingerprint(generate(7L)), fingerprint(generate(8L)));
    }

    @Test
    void hasSomeLandButMostlyWater() {
        var map = generate(3L);
        int land = 0;
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                if (map.cell(column, row).getTerrain() == Terrain.LAND) {
                    land++;
                }
            }
        }
        assertTrue(land > 0);
        assertTrue(land < WIDTH * HEIGHT / 10);
    }

    @Test
    void liliesOnlyOnWater() {
        for (long seed = 0; seed < 20; seed++) {
            var map = generate(seed);
            for (int row = 0; row < HEIGHT; row++) {
                for (int column = 0; column < WIDTH; column++) {
                    var cell = map.cell(column, row);
                    if (cell.getTerrain() == Terrain.LAND) {
                        for (LilySlot slot : LilySlot.values()) {
                            assertFalse(cell.hasLily(slot));
                        }
                    }
                }
            }
        }
    }

    @Test
    void aboutAThirdOfWaterSlotsHoldLilies() {
        var map = generate(11L);
        int waterSlots = 0;
        int filledSlots = 0;
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                var cell = map.cell(column, row);
                if (cell.getTerrain() != Terrain.WATER) {
                    continue;
                }
                for (LilySlot slot : LilySlot.values()) {
                    waterSlots++;
                    if (cell.hasLily(slot)) {
                        filledSlots++;
                    }
                }
            }
        }
        double ratio = (double) filledSlots / waterSlots;
        assertTrue(ratio > 0.25 && ratio < 0.42, "ratio " + ratio);
    }

    private static String colorFingerprint(PondMap map) {
        var builder = new StringBuilder();
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                for (LilySlot slot : LilySlot.values()) {
                    var lily = map.cell(column, row).lilyAt(slot);
                    builder.append(lily == null ? 0 : lily.getColor()).append(',');
                }
            }
        }
        return builder.toString();
    }

    private static java.util.Set<Integer> paletteColors() {
        var colors = new java.util.HashSet<Integer>();
        for (int i = 0; i < LilyPalette.size(); i++) {
            colors.add(LilyPalette.colorAt(i));
        }
        return colors;
    }

    @Test
    void lilyColorsAreDeterministicForASeed() {
        assertEquals(colorFingerprint(generate(5L)), colorFingerprint(generate(5L)));
    }

    @Test
    void everyLilyColorIsAPaletteEntryAndAllEntriesAppear() {
        var map = generate(5L);
        var palette = paletteColors();
        var distinct = new java.util.HashSet<Integer>();
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                for (LilySlot slot : LilySlot.values()) {
                    var lily = map.cell(column, row).lilyAt(slot);
                    if (lily == null) {
                        continue;
                    }
                    int color = lily.getColor();
                    assertTrue(palette.contains(color),
                            "not a palette color: " + Integer.toHexString(color));
                    distinct.add(color);
                }
            }
        }
        assertEquals(palette, distinct);
    }

    @Test
    void paletteColorsAreOpaque() {
        for (int color : paletteColors()) {
            assertEquals(255, color & 255, "alpha of " + Integer.toHexString(color));
        }
    }

    @Test
    void islandsNeverTouchWorldEdge() {
        for (long seed = 0; seed < 50; seed++) {
            var map = generate(seed);
            for (int column = 0; column < WIDTH; column++) {
                assertEquals(Terrain.WATER, map.cell(column, 0).getTerrain());
                assertEquals(Terrain.WATER, map.cell(column, HEIGHT - 1).getTerrain());
            }
            for (int row = 0; row < HEIGHT; row++) {
                assertEquals(Terrain.WATER, map.cell(0, row).getTerrain());
                assertEquals(Terrain.WATER, map.cell(WIDTH - 1, row).getTerrain());
            }
        }
    }

    @Test
    void startAreaIsFreeOfLand() {
        for (long seed = 0; seed < 50; seed++) {
            var map = generate(seed);
            for (int row = START_ROW - 1; row <= START_ROW + 1; row++) {
                for (int column = START_COLUMN - 1; column <= START_COLUMN + 1; column++) {
                    assertEquals(Terrain.WATER, map.cell(column, row).getTerrain());
                }
            }
        }
    }
}
