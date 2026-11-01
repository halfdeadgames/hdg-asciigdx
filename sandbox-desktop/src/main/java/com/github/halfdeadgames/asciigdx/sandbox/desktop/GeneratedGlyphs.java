package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs;
import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs.Shade;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The 8x8 glyphs drawn in code for the default sheet: quadrant blocks in every shade, and
 * geometric symbols that match the dungeon.mode style (2-pixel lines on x or y = 3..4, 4-pixel
 * heavy lines on 2..5, filled shapes in the same 6x6 box as the CP437 square).
 *
 * <p>Art rows are top to bottom; {@code #} is lit, anything else is transparent.
 */
final class GeneratedGlyphs {

    static final int SIZE = 8;

    static final int SHIELD_STAGES_START = 0xE0A0;
    static final int SWORD_STAGES_START = 0xE0A4;
    static final int OUTLINED_DICE_START = 0xE0B0;
    static final int TEN = 0xE0C0;
    static final int CARD_BACK = 0xE0C1;
    static final int CARD_BACK_SELECTED = 0xE0C2;
    static final int COIN_FACE = 0xE100;
    static final int COIN_EDGE = 0xE101;

    private static final int[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};

    private GeneratedGlyphs() {
    }

    /** Returns every generated glyph in sheet order: quadrants first, then geometric symbols. */
    static Map<Integer, boolean[][]> all() {
        Map<Integer, boolean[][]> glyphs = new LinkedHashMap<>();
        for (Shade shade : Shade.values()) {
            for (int mask = 1; mask <= QuadrantGlyphs.ALL; mask++) {
                glyphs.put(QuadrantGlyphs.codePoint(shade, mask), quadrant(shade, mask));
            }
        }
        glyphs.putAll(geometric());
        glyphs.putAll(pictorial());
        glyphs.putAll(symbols());
        glyphs.putAll(hourglassAndTurnArrows());
        glyphs.putAll(gamePieces());
        return glyphs;
    }

    /** Checkers, shogi pieces, and coins. Kept last so earlier sheet positions never move. */
    private static Map<Integer, boolean[][]> gamePieces() {
        Map<Integer, boolean[][]> glyphs = new LinkedHashMap<>();
        glyphs.put(0x26C0, art("........", "........", "........", ".######.", "#......#", "########", ".######.", "........"));
        glyphs.put(0x26C1, art("........", ".######.", "#......#", "########", "#......#", "########", ".######.", "........"));
        glyphs.put(0x26C2, art("........", "........", "........", ".######.", "########", "#......#", ".######.", "........"));
        glyphs.put(0x26C3, art("........", ".######.", "########", "#......#", "########", "#......#", ".######.", "........"));
        glyphs.put(0x2616, art("...##...", "..#..#..", ".#....#.", ".#....#.", ".#....#.", ".#....#.", ".######.", "........"));
        glyphs.put(0x2617, art("...##...", "..####..", ".######.", ".######.", ".######.", ".######.", ".######.", "........"));
        glyphs.put(COIN_FACE, art("........", "..####..", ".#....#.", ".#.##.#.", ".#.##.#.", ".#....#.", "..####..", "........"));
        glyphs.put(COIN_EDGE, art("........", "...##...", "..#.##..", "..#.##..", "..#.##..", "..#.##..", "...##...", "........"));
        return glyphs;
    }

    /** Arrows, shapes, media controls, math, marks, stars, and the big heart with its meters. */
    private static Map<Integer, boolean[][]> symbols() {
        Map<Integer, boolean[][]> glyphs = new LinkedHashMap<>();
        // Arrows
        glyphs.put(0x2B05, art("........", "...#....", "..##....", ".#######", ".#######", "..##....", "...#....", "........"));
        glyphs.put(0x2B06, art("........", "...##...", "..####..", ".######.", "...##...", "...##...", "...##...", "...##..."));
        glyphs.put(0x2B07, art("...##...", "...##...", "...##...", "...##...", ".######.", "..####..", "...##...", "........"));
        glyphs.put(0x27A1, art("........", "....#...", "....##..", "#######.", "#######.", "....##..", "....#...", "........"));
        glyphs.put(0x2B08, art("........", "..#####.", "...####.", "....###.", "...####.", "..###.#.", ".###....", ".##....."));
        glyphs.put(0x2B09, art("........", ".#####..", ".####...", ".###....", ".####...", ".#.###..", "....###.", ".....##."));
        glyphs.put(0x2B0A, art(".##.....", ".###....", "..###.#.", "...####.", "....###.", "...####.", "..#####.", "........"));
        glyphs.put(0x2B0B, art(".....##.", "....###.", ".#.###..", ".####...", ".###....", ".####...", ".#####..", "........"));
        glyphs.put(0x2B60, art("........", "...#....", "..#.....", ".#######", "..#.....", "...#....", "........", "........"));
        glyphs.put(0x2B61, art("........", "...#....", "..###...", ".#.#.#..", "...#....", "...#....", "...#....", "...#...."));
        glyphs.put(0x2B62, art("........", "....#...", ".....#..", "#######.", ".....#..", "....#...", "........", "........"));
        glyphs.put(0x2B63, art("....#...", "....#...", "....#...", "....#...", "..#.#.#.", "...###..", "....#...", "........"));
        glyphs.put(0x2B64, art("........", "..#..#..", ".#....#.", "########", ".#....#.", "..#..#..", "........", "........"));
        glyphs.put(0x2B65, art("....#...", "...###..", "..#.#.#.", "....#...", "....#...", "..#.#.#.", "...###..", "....#..."));
        glyphs.put(0x2B67, art("........", "..####..", "....##..", "...#.#..", "..#..#..", ".#......", "#.......", "........"));
        glyphs.put(0x2B68, art("........", "#.......", ".#......", "..#..#..", "...#.#..", "....##..", "..####..", "........"));
        glyphs.put(0x2B69, art("........", ".......#", "......#.", "..#..#..", "..#.#...", "..##....", "..####..", "........"));
        glyphs.put(0x2B6A, art("........", "...#....", "..#.....", ".#.##.##", "..#.....", "...#....", "........", "........"));
        glyphs.put(0x2B6B, art("........", "...#....", "..#.#...", ".#.#.#..", "...#....", "........", "...#....", "...#...."));
        glyphs.put(0x2B6C, art("........", "....#...", ".....#..", "##.##.#.", ".....#..", "....#...", "........", "........"));
        glyphs.put(0x2B6D, art("....#...", "....#...", "........", "....#...", "..#.#.#.", "...#.#..", "....#...", "........"));
        glyphs.put(0x2B6E, art("..###...", ".#...#..", "#.....#.", "#....###", "#.....#.", ".#......", "..###...", "........"));
        glyphs.put(0x2B6F, art("...###..", "..#...#.", ".#.....#", "###....#", ".#.....#", "......#.", "...###..", "........"));
        // Shapes
        glyphs.put(0x2B24, art("..####..", ".######.", "########", "########", "########", "########", ".######.", "..####.."));
        glyphs.put(0x2B1C, art("........", ".######.", ".#....#.", ".#....#.", ".#....#.", ".#....#.", ".######.", "........"));
        glyphs.put(0x2B22, art("...##...", "..####..", ".######.", ".######.", ".######.", ".######.", "..####..", "...##..."));
        glyphs.put(0x2B21, art("........", "..####..", ".#....#.", "#......#", "#......#", ".#....#.", "..####..", "........"));
        glyphs.put(0x2BC3, art("........", "........", ".######.", "########", "########", ".######.", "........", "........"));
        glyphs.put(0x2BC5, art("........", "........", "...##...", "..####..", ".######.", "........", "........", "........"));
        glyphs.put(0x2BC6, art("........", "........", "........", ".######.", "..####..", "...##...", "........", "........"));
        glyphs.put(0x2BC7, art("........", "....#...", "...##...", "..###...", "..###...", "...##...", "....#...", "........"));
        glyphs.put(0x2BC8, art("........", "...#....", "...##...", "...###..", "...###..", "...##...", "...#....", "........"));
        glyphs.put(0x2316, art("...#....", ".#####..", ".#.#.#..", "#######.", ".#.#.#..", ".#####..", "...#....", "........"));
        // Media controls
        glyphs.put(0x23F4, art("........", ".....#..", "....##..", "...###..", "....##..", ".....#..", "........", "........"));
        glyphs.put(0x23F5, art("........", "..#.....", "..##....", "..###...", "..##....", "..#.....", "........", "........"));
        glyphs.put(0x23F6, art("........", "........", "...#....", "..###...", ".#####..", "........", "........", "........"));
        glyphs.put(0x23F7, art("........", "........", "........", ".#####..", "..###...", "...#....", "........", "........"));
        glyphs.put(0x23F8, art("........", ".##.##..", ".##.##..", ".##.##..", ".##.##..", ".##.##..", "........", "........"));
        // Scan lines
        glyphs.put(0x23BA, art("########", "........", "........", "........", "........", "........", "........", "........"));
        glyphs.put(0x23BB, art("........", "........", "########", "........", "........", "........", "........", "........"));
        glyphs.put(0x23BC, art("........", "........", "........", "........", "........", "########", "........", "........"));
        glyphs.put(0x23BD, art("........", "........", "........", "........", "........", "........", "........", "########"));
        // Math
        glyphs.put(0x00D7, art("........", "........", ".#...#..", "..#.#...", "...#....", "..#.#...", ".#...#..", "........"));
        glyphs.put(0x2260, art("........", ".....#..", ".#####..", "...#....", ".#####..", ".#......", "........", "........"));
        glyphs.put(0x2211, art(".######.", ".#......", "..#.....", "...#....", "..#.....", ".#......", ".######.", "........"));
        glyphs.put(0x2206, art("........", "...#....", "..#.#...", "..#.#...", ".#...#..", ".#####..", "........", "........"));
        glyphs.put(0x2207, art("........", "........", ".#####..", ".#...#..", "..#.#...", "..#.#...", "...#....", "........"));
        glyphs.put(0x2234, art("........", "...#....", "........", "........", ".#...#..", "........", "........", "........"));
        glyphs.put(0x2235, art("........", "........", "........", ".#...#..", "........", "........", "...#....", "........"));
        glyphs.put(0x2295, art("........", "..###...", ".#.#.#..", ".#####..", ".#.#.#..", "..###...", "........", "........"));
        glyphs.put(0x2297, art("........", "..###...", ".##.##..", ".#.#.#..", ".##.##..", "..###...", "........", "........"));
        glyphs.put(0x2299, art("........", "..###...", ".#...#..", ".#.#.#..", ".#...#..", "..###...", "........", "........"));
        glyphs.put(0x2205, art("........", "..###.#.", ".#..##..", ".#.#.#..", ".##..#..", "#.###...", "........", "........"));
        // Marks
        glyphs.put(0x2713, art("........", "......#.", ".....#..", "#...#...", ".#.#....", "..#.....", "........", "........"));
        glyphs.put(0x2714, art("........", "......##", ".....##.", "##..##..", ".####...", "..##....", "........", "........"));
        glyphs.put(0x2717, art("........", ".#...#..", "..#.#...", "...#....", "..#.#...", ".#...#..", "#.......", "........"));
        glyphs.put(0x2718, art("........", "##...##.", ".##.##..", "..###...", ".##.##..", "##...##.", "#.......", "........"));
        glyphs.put(0x271A, art("........", "...##...", "...##...", ".######.", ".######.", "...##...", "...##...", "........"));
        // Stars
        glyphs.put(0x2726, art("...##...", "...##...", "..####..", "########", "########", "..####..", "...##...", "...##..."));
        glyphs.put(0x2727, art("...##...", "...##...", "..#..#..", "##....##", "##....##", "..#..#..", "...##...", "...##..."));
        glyphs.put(0x2736, art("........", "...##...", ".######.", "..####..", "..####..", ".######.", "...##...", "........"));
        glyphs.put(0x2738, art("...##...", ".#.##.#.", "..####..", "########", "########", "..####..", ".#.##.#.", "...##..."));
        glyphs.put(0x2739, art("#..##..#", ".#.##.#.", "..####..", "########", "########", "..####..", ".#.##.#.", "#..##..#"));
        // Pointers and mail
        glyphs.put(0x2794, art("........", "...##...", "...###..", "#######.", "#######.", "...###..", "...##...", "........"));
        glyphs.put(0x279C, art("........", "....##..", "....###.", "#######.", "#######.", "....###.", "....##..", "........"));
        glyphs.put(0x27A4, art("........", ".##.....", "..###...", "..####..", "..###...", ".##.....", "........", "........"));
        glyphs.put(0x2709, art("........", "########", "##....##", "#.#..#.#", "#..##..#", "#......#", "########", "........"));
        // Big heart: full, hollow, filled from the bottom, and filled by a clockwise sweep
        glyphs.put(0x2764, art("........", ".##..##.", "########", "########", ".######.", "..####..", "...##...", "........"));
        glyphs.put(0xE0D0, art("........", ".##..##.", "#..##..#", "#......#", ".#....#.", "..#..#..", "...##...", "........"));
        glyphs.put(0xE0D1, art("........", ".##..##.", "#..##..#", "#......#", ".#....#.", "..####..", "...##...", "........"));
        glyphs.put(0xE0D2, art("........", ".##..##.", "#..##..#", "#......#", ".######.", "..####..", "...##...", "........"));
        glyphs.put(0xE0D3, art("........", ".##..##.", "#..##..#", "########", ".######.", "..####..", "...##...", "........"));
        glyphs.put(0xE0D8, art("........", ".##..##.", "#####..#", "#......#", ".#....#.", "..#..#..", "...##...", "........"));
        glyphs.put(0xE0D9, art("........", ".##..##.", "#####..#", "###....#", ".#....#.", "..#..#..", "...##...", "........"));
        glyphs.put(0xE0DA, art("........", ".##..##.", "#####..#", "####...#", ".##...#.", "..#..#..", "...##...", "........"));
        glyphs.put(0xE0DB, art("........", ".##..##.", "#####..#", "####...#", ".###..#.", "..##.#..", "...##...", "........"));
        glyphs.put(0xE0DC, art("........", ".##..##.", "#####..#", "####...#", ".####.#.", "..####..", "...##...", "........"));
        glyphs.put(0xE0DD, art("........", ".##..##.", "#####..#", "#####..#", ".######.", "..####..", "...##...", "........"));
        glyphs.put(0xE0DE, art("........", ".##..##.", "#####..#", "########", ".######.", "..####..", "...##...", "........"));
        return glyphs;
    }

    /**
     * U-turn arrows and hourglasses. Each hourglass pose comes merged (frame with sand), as frame only,
     * and as sand only, so a layered renderer can color them separately.
     */
    private static Map<Integer, boolean[][]> hourglassAndTurnArrows() {
        Map<Integer, boolean[][]> glyphs = new LinkedHashMap<>();
        // U-turn arrows
        glyphs.put(0x2B8C, art("........", ".#......", "#####...", ".#...#..", ".....#..", ".....#..", "#####...", "........"));
        glyphs.put(0x2B8D, art(".#...#..", ".#..###.", ".#...#..", ".#...#..", ".#...#..", "..###...", "........", "........"));
        glyphs.put(0x2B8E, art("........", "...#####", "..#.....", "..#.....", "..#...#.", "...#####", "......#.", "........"));
        glyphs.put(0x2B8F, art("........", "........", "...###..", "..#...#.", "..#...#.", "..#...#.", ".###..#.", "..#...#."));
        // Hourglass merged: sand draining stages 0..4, then tilted 45, sideways, tilted 135
        glyphs.put(0xE0E0, art("########", ".######.", ".######.", "..#..#..", "..#..#..", ".#....#.", ".#....#.", "########"));
        glyphs.put(0xE0E1, art("########", ".#.##.#.", ".######.", "..##.#..", "..##.#..", ".#....#.", ".#.##.#.", "########"));
        glyphs.put(0xE0E2, art("########", ".#....#.", ".######.", "..#.##..", "..#.##..", ".#....#.", ".######.", "########"));
        glyphs.put(0xE0E3, art("########", ".#....#.", ".#.##.#.", "..##.#..", "..##.#..", ".#.##.#.", ".######.", "########"));
        glyphs.put(0xE0E4, art("########", ".#....#.", ".#....#.", "..#..#..", "..#..#..", ".######.", ".######.", "########"));
        glyphs.put(0xE0E5, art(".....##.", "....#..#", "...#...#", "...#..#.", ".####...", "#####...", "####....", ".##....."));
        glyphs.put(0xE0E6, art("#......#", "###..###", "#####..#", "###....#", "###....#", "#####..#", "###..###", "#......#"));
        glyphs.put(0xE0E7, art(".##.....", "####....", "#####...", ".####...", "...#..#.", "...#...#", "....#..#", ".....##."));
        // Hourglass frame only: upright, tilted 45, sideways, tilted 135
        glyphs.put(0xE0E8, art("########", ".#....#.", ".#....#.", "..#..#..", "..#..#..", ".#....#.", ".#....#.", "########"));
        glyphs.put(0xE0E9, art(".....##.", "....#..#", "...#...#", "...#..#.", ".#..#...", "#...#...", "#..#....", ".##....."));
        glyphs.put(0xE0EA, art("#......#", "###..###", "#..##..#", "#......#", "#......#", "#..##..#", "###..###", "#......#"));
        glyphs.put(0xE0EB, art(".##.....", "#..#....", "#...#...", ".#..#...", "...#..#.", "...#...#", "....#..#", ".....##."));
        // Hourglass sand only, same order as the merged poses
        glyphs.put(0xE0F0, art("........", "..####..", "..####..", "........", "........", "........", "........", "........"));
        glyphs.put(0xE0F1, art("........", "...##...", "..####..", "...#....", "...#....", "........", "...##...", "........"));
        glyphs.put(0xE0F2, art("........", "........", "..####..", "....#...", "....#...", "........", "..####..", "........"));
        glyphs.put(0xE0F3, art("........", "........", "...##...", "...#....", "...#....", "...##...", "..####..", "........"));
        glyphs.put(0xE0F4, art("........", "........", "........", "........", "........", "..####..", "..####..", "........"));
        glyphs.put(0xE0F5, art("........", "........", "........", "........", "..##....", ".###....", ".##.....", "........"));
        glyphs.put(0xE0F6, art("........", "........", ".##.....", ".##.....", ".##.....", ".##.....", "........", "........"));
        glyphs.put(0xE0F7, art("........", ".##.....", ".###....", "..##....", "........", "........", "........", "........"));
        // Standard hourglass characters share the art of merged stages 4 and 2
        glyphs.put(0x231B, art("########", ".#....#.", ".#....#.", "..#..#..", "..#..#..", ".######.", ".######.", "########"));
        glyphs.put(0x23F3, art("########", ".#....#.", ".######.", "..#.##..", "..#.##..", ".#....#.", ".######.", "########"));
        return glyphs;
    }

    /** First drafts of picture glyphs, meant to be refined by hand. */
    private static Map<Integer, boolean[][]> pictorial() {
        Map<Integer, boolean[][]> glyphs = new LinkedHashMap<>();
        // Places
        glyphs.put(0x26E9, art("........", "########", ".##..##.", "########", ".#....#.", ".#....#.", ".#....#.", "........"));
        glyphs.put(0x26EA, art("...#....", "..###...", "...#....", "..###...", ".#####..", ".##.##..", ".##.##..", "........"));
        glyphs.put(0x26EB, art("........", ".#.##.#.", ".######.", ".######.", ".##..##.", ".##..##.", ".##..##.", "........"));
        glyphs.put(0x26F2, art("...##...", "..#..#..", ".#.##.#.", "...##...", "########", ".######.", "..####..", "........"));
        glyphs.put(0x2693, art("...##...", "..#..#..", "...##...", "..####..", "...##...", "#..##..#", ".######.", "..####.."));
        glyphs.put(0x26F5, art("...#....", "...##...", "...###..", "...####.", "...#....", "########", ".######.", "..####.."));
        // Items
        glyphs.put(0x2615, art("..#.#...", ".#.#....", "........", "######..", "#######.", "######.#", "#######.", ".####..."));
        glyphs.put(0x2697, art("..####..", "...##...", "...##...", "..####..", ".######.", "########", "########", ".######."));
        // Durability meters: an outline that fills from the bottom. Empty and full shields have
        // Unicode code points; the stages between them and every sword stage are private use.
        boolean[][] shieldOutline = art("........", ".######.", ".#....#.", ".#....#.", ".#....#.", "..#..#..", "...##...", "........");
        boolean[][] shieldFilled = art("........", ".######.", ".######.", ".######.", ".######.", "..####..", "...##...", "........");
        glyphs.put(0x26C9, shieldOutline);
        for (int stage = 1; stage <= 3; stage++) {
            glyphs.put(SHIELD_STAGES_START + stage - 1, fillFromBottom(shieldOutline, shieldFilled, 2, 5, stage));
        }
        glyphs.put(0x26CA, shieldFilled);
        boolean[][] swordOutline = art("...##...", "..#..#..", "..#..#..", "..#..#..", "..#..#..", ".######.", "...##...", "...##...");
        boolean[][] swordFilled = art("...##...", "..####..", "..####..", "..####..", "..####..", ".######.", "...##...", "...##...");
        for (int stage = 0; stage <= 4; stage++) {
            glyphs.put(SWORD_STAGES_START + stage, fillFromBottom(swordOutline, swordFilled, 1, 4, stage));
        }
        // Dice: solid with holes, and outlined with lit pips
        for (int face = 1; face <= 6; face++) {
            glyphs.put(0x2680 + face - 1, die(face));
        }
        for (int face = 1; face <= 6; face++) {
            glyphs.put(OUTLINED_DICE_START + face - 1, outlinedDie(face));
        }
        // Playing cards: hollow suits to pair with the filled CP437 ones, and a one-cell "10"
        glyphs.put(0x2664, art("........", "...##...", "..#..#..", ".#....#.", "#......#", "#..##..#", ".##..##.", "...##..."));
        glyphs.put(0x2661, art("........", ".##..##.", "#..##..#", "#......#", "#......#", ".#....#.", "..#..#..", "...##..."));
        glyphs.put(0x2662, art("........", "...##...", "..#..#..", ".#....#.", "#......#", ".#....#.", "..#..#..", "...##..."));
        glyphs.put(0x2667, art("........", "...##...", "..#..#..", "..#..#..", "##.##.##", "#......#", ".##..##.", "...##..."));
        glyphs.put(CARD_BACK, art(".######.", ".#....#.", ".#.##.#.", ".##..##.", ".##..##.", ".#.##.#.", ".#....#.", ".######."));
        glyphs.put(CARD_BACK_SELECTED, art(".######.", ".######.", ".##..##.", ".#.##.#.", ".#.##.#.", ".##..##.", ".######.", ".######."));
        glyphs.put(TEN, art("........", ".#..##..", "##.#..#.", ".#.#..#.", ".#.#..#.", ".#.#..#.", "###.##..", "........"));
        // Chess: white (outlined) and black (filled) pieces
        glyphs.put(0x2654, art("...##...", "..#..#..", "...##...", ".######.", "..#..#..", "..#..#..", ".######.", "........"));
        glyphs.put(0x2655, art("#..##..#", "#.#..#.#", ".#....#.", "..#..#..", "..#..#..", "..#..#..", ".######.", "........"));
        glyphs.put(0x2656, art("........", ".#.##.#.", ".#....#.", "..#..#..", "..#..#..", "..#..#..", ".######.", "........"));
        glyphs.put(0x2657, art("...##...", "..#..#..", "..#..#..", "..#..#..", "...##...", "..#..#..", ".######.", "........"));
        glyphs.put(0x2658, art("........", "..###...", ".#..##..", "##.#.#..", "..##.#..", "..#..#..", ".######.", "........"));
        glyphs.put(0x2659, art("........", "...##...", "..#..#..", "...##...", "..#..#..", "..#..#..", ".######.", "........"));
        glyphs.put(0x265A, art("...##...", "..####..", "...##...", ".######.", "..####..", "..####..", ".######.", "........"));
        glyphs.put(0x265B, art("#..##..#", "#.####.#", ".######.", "..####..", "..####..", "..####..", ".######.", "........"));
        glyphs.put(0x265C, art("........", ".#.##.#.", ".######.", "..####..", "..####..", "..####..", ".######.", "........"));
        glyphs.put(0x265D, art("...##...", "..#.##..", "..##.#..", "..####..", "...##...", "..####..", ".######.", "........"));
        glyphs.put(0x265E, art("........", "..###...", ".#####..", "##.###..", "...###..", "..####..", ".######.", "........"));
        glyphs.put(0x265F, art("........", "...##...", "..####..", "...##...", "..####..", "..####..", ".######.", "........"));
        // Effects
        glyphs.put(0x26A1, art("....###.", "...###..", "..###...", ".######.", "...###..", "..###...", ".##.....", "#......."));
        // Three tail lines, each two pixels from the next, trailing up-left from the head.
        glyphs.put(0x2604, art("#.#.....", ".#.#....", "#.#.#...", ".#.####.", "..######", "...#####", "...#####", "....###."));
        return glyphs;
    }

    // Copies rows firstRow..lastRow of the filled art over the outline, bottom row first, in
    // `stage` of 4 equal steps.
    private static boolean[][] fillFromBottom(boolean[][] outline, boolean[][] filled, int firstRow, int lastRow, int stage) {
        boolean[][] pixels = new boolean[SIZE][];
        for (int y = 0; y < SIZE; y++) {
            pixels[y] = outline[y].clone();
        }
        int rows = lastRow - firstRow + 1;
        int litRows = Math.round(rows * stage / 4f);
        for (int y = lastRow; y > lastRow - litRows; y--) {
            pixels[y] = filled[y].clone();
        }
        return pixels;
    }

    // A 7x7 die outline on the bottom row. Pips sit one pixel in from the border so they do
    // not merge with it, which leaves a 3x3 pip grid.
    private static boolean[][] outlinedDie(int face) {
        boolean[][] pixels = new boolean[SIZE][SIZE];
        for (int y = 1; y < SIZE; y++) {
            for (int x = 0; x < SIZE - 1; x++) {
                pixels[y][x] = y == 1 || y == SIZE - 1 || x == 0 || x == SIZE - 2;
            }
        }
        for (int[] pip : pipPositions(face, 2, 3, 4)) {
            pixels[pip[1] + 1][pip[0]] = true;
        }
        return pixels;
    }

    // A filled 7x7 die resting on the bottom row, with its pips cut out as holes.
    private static boolean[][] die(int face) {
        boolean[][] pixels = new boolean[SIZE][SIZE];
        for (int y = 1; y < SIZE; y++) {
            for (int x = 0; x < SIZE - 1; x++) {
                pixels[y][x] = true;
            }
        }
        for (int[] pip : pipPositions(face, 1, 3, 5)) {
            pixels[pip[1] + 1][pip[0]] = false;
        }
        return pixels;
    }

    private static int[][] pipPositions(int face, int low, int middle, int high) {
        return switch (face) {
            case 1 -> new int[][] {{middle, middle}};
            case 2 -> new int[][] {{low, low}, {high, high}};
            case 3 -> new int[][] {{low, low}, {middle, middle}, {high, high}};
            case 4 -> new int[][] {{low, low}, {high, low}, {low, high}, {high, high}};
            case 5 -> new int[][] {{low, low}, {high, low}, {middle, middle}, {low, high}, {high, high}};
            default -> new int[][] {{low, low}, {high, low}, {low, middle}, {high, middle}, {low, high}, {high, high}};
        };
    }

    static boolean[][] quadrant(Shade shade, int mask) {
        boolean[][] pixels = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int quadrant = (y >= SIZE / 2 ? QuadrantGlyphs.BOTTOM_LEFT : QuadrantGlyphs.TOP_LEFT)
                    * (x >= SIZE / 2 ? 2 : 1);
                pixels[y][x] = (mask & quadrant) != 0 && isPatternLit(shade, x, y);
            }
        }
        return pixels;
    }

    // Patterns are anchored to the cell, not the quadrant, so they tile across neighbours.
    private static boolean isPatternLit(Shade shade, int x, int y) {
        boolean slash = Math.floorMod(x + y, 4) == 0;
        boolean backslash = Math.floorMod(x - y, 4) == 0;
        int backslashLane = Math.floorMod(x - y, 4);
        return switch (shade) {
            case SOLID -> true;
            case BAYER_75 -> BAYER[y % 4][x % 4] < 12;
            case BAYER_50 -> BAYER[y % 4][x % 4] < 8;
            case BAYER_25 -> BAYER[y % 4][x % 4] < 4;
            case HATCH_75 -> !(slash || backslash);
            case HATCH_50 -> slash || backslash;
            case HATCH_25 -> slash;
            case FLIPPED_HATCH_75 -> backslashLane != 0;
            // Adjacent lanes: two alternating 1-pixel diagonal lanes would form a checkerboard.
            case FLIPPED_HATCH_50 -> backslashLane >= 2;
            case FLIPPED_HATCH_25 -> backslashLane == 3;
        };
    }

    private static Map<Integer, boolean[][]> geometric() {
        Map<Integer, boolean[][]> glyphs = new LinkedHashMap<>();

        int[] lowerEighths = {0x2581, 0x2582, 0x2583, 0x2585, 0x2586, 0x2587};
        int[] lowerRows = {1, 2, 3, 5, 6, 7};
        for (int i = 0; i < lowerEighths.length; i++) {
            glyphs.put(lowerEighths[i], lowerBlock(lowerRows[i]));
        }
        int[] leftEighths = {0x258F, 0x258E, 0x258D, 0x258B, 0x258A, 0x2589};
        int[] leftColumns = {1, 2, 3, 5, 6, 7};
        for (int i = 0; i < leftEighths.length; i++) {
            glyphs.put(leftEighths[i], transpose(lowerBlockFromTop(leftColumns[i])));
        }

        boolean[][] roundedDownRight = art(
            "........",
            "........",
            "........",
            ".....###",
            "....####",
            "...##...",
            "...##...",
            "...##...");
        glyphs.put(0x256D, roundedDownRight);
        glyphs.put(0x256E, mirrorX(roundedDownRight));
        glyphs.put(0x256F, mirrorX(mirrorY(roundedDownRight)));
        glyphs.put(0x2570, mirrorY(roundedDownRight));

        boolean[][] heavyHorizontal = art(
            "........",
            "........",
            "########",
            "########",
            "########",
            "########",
            "........",
            "........");
        glyphs.put(0x2501, heavyHorizontal);
        glyphs.put(0x2503, transpose(heavyHorizontal));
        boolean[][] heavyDownRight = art(
            "........",
            "........",
            "....####",
            "...#####",
            "..######",
            "..######",
            "..####..",
            "..####..");
        glyphs.put(0x250F, heavyDownRight);
        glyphs.put(0x2513, mirrorX(heavyDownRight));
        glyphs.put(0x2517, mirrorY(heavyDownRight));
        glyphs.put(0x251B, mirrorX(mirrorY(heavyDownRight)));

        boolean[][] dashedHorizontal = art(
            "........",
            "........",
            "........",
            ".##..##.",
            ".##..##.",
            "........",
            "........",
            "........");
        boolean[][] heavyDashedHorizontal = art(
            "........",
            "........",
            ".##..##.",
            ".##..##.",
            ".##..##.",
            ".##..##.",
            "........",
            "........");
        glyphs.put(0x2504, dashedHorizontal);
        glyphs.put(0x2505, heavyDashedHorizontal);
        glyphs.put(0x2506, transpose(dashedHorizontal));
        glyphs.put(0x2507, transpose(heavyDashedHorizontal));

        boolean[][] arrowUpRight = art(
            "........",
            "..#####.",
            "...####.",
            "....###.",
            "...#.##.",
            "..#...#.",
            ".#......",
            "........");
        glyphs.put(0x2196, mirrorX(arrowUpRight));
        glyphs.put(0x2197, arrowUpRight);
        glyphs.put(0x2198, mirrorY(arrowUpRight));
        glyphs.put(0x2199, mirrorX(mirrorY(arrowUpRight)));

        boolean[][] doubleArrowRight = art(
            "........",
            "....#...",
            ".#####..",
            "......#.",
            "......#.",
            ".#####..",
            "....#...",
            "........");
        glyphs.put(0x21D0, mirrorX(doubleArrowRight));
        glyphs.put(0x21D1, mirrorY(transpose(doubleArrowRight)));
        glyphs.put(0x21D2, doubleArrowRight);
        glyphs.put(0x21D3, transpose(doubleArrowRight));

        boolean[][] slopeLowerRight = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                slopeLowerRight[y][x] = x + y >= SIZE - 1;
            }
        }
        glyphs.put(0x25E2, slopeLowerRight);
        glyphs.put(0x25E3, mirrorX(slopeLowerRight));
        glyphs.put(0x25E4, mirrorX(mirrorY(slopeLowerRight)));
        glyphs.put(0x25E5, mirrorY(slopeLowerRight));

        boolean[][] circleLeftHalf = art(
            "........",
            "..####..",
            ".###..#.",
            ".###..#.",
            ".###..#.",
            ".###..#.",
            "..####..",
            "........");
        glyphs.put(0x25D0, circleLeftHalf);
        glyphs.put(0x25D1, mirrorX(circleLeftHalf));
        glyphs.put(0x25D2, mirrorY(transpose(circleLeftHalf)));
        glyphs.put(0x25D3, transpose(circleLeftHalf));

        glyphs.put(0x25CF, art(
            "........",
            "..####..",
            ".######.",
            ".######.",
            ".######.",
            ".######.",
            "..####..",
            "........"));
        glyphs.put(0x25AA, art(
            "........",
            "........",
            "..####..",
            "..####..",
            "..####..",
            "..####..",
            "........",
            "........"));
        glyphs.put(0x25AB, art(
            "........",
            "........",
            "..####..",
            "..#..#..",
            "..#..#..",
            "..####..",
            "........",
            "........"));

        glyphs.put(0x2610, art(
            "........",
            ".######.",
            ".#....#.",
            ".#....#.",
            ".#....#.",
            ".#....#.",
            ".######.",
            "........"));
        glyphs.put(0x2611, art(
            "........",
            ".######.",
            ".#...##.",
            ".##.#.#.",
            ".#.#..#.",
            ".#....#.",
            ".######.",
            "........"));
        glyphs.put(0x2612, art(
            "........",
            ".######.",
            ".##..##.",
            ".#.##.#.",
            ".#.##.#.",
            ".##..##.",
            ".######.",
            "........"));
        return glyphs;
    }

    static boolean[][] art(String... rows) {
        if (rows.length != SIZE) {
            throw new IllegalArgumentException("Art needs " + SIZE + " rows, got " + rows.length);
        }
        boolean[][] pixels = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            if (rows[y].length() != SIZE) {
                throw new IllegalArgumentException("Art row " + y + " needs " + SIZE + " columns: " + rows[y]);
            }
            for (int x = 0; x < SIZE; x++) {
                pixels[y][x] = rows[y].charAt(x) == '#';
            }
        }
        return pixels;
    }

    private static boolean[][] lowerBlock(int rows) {
        boolean[][] pixels = new boolean[SIZE][SIZE];
        for (int y = SIZE - rows; y < SIZE; y++) {
            Arrays.fill(pixels[y], true);
        }
        return pixels;
    }

    private static boolean[][] lowerBlockFromTop(int rows) {
        boolean[][] pixels = new boolean[SIZE][SIZE];
        for (int y = 0; y < rows; y++) {
            Arrays.fill(pixels[y], true);
        }
        return pixels;
    }

    private static boolean[][] mirrorX(boolean[][] pixels) {
        boolean[][] result = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                result[y][x] = pixels[y][SIZE - 1 - x];
            }
        }
        return result;
    }

    private static boolean[][] mirrorY(boolean[][] pixels) {
        boolean[][] result = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            result[y] = pixels[SIZE - 1 - y].clone();
        }
        return result;
    }

    private static boolean[][] transpose(boolean[][] pixels) {
        boolean[][] result = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                result[y][x] = pixels[x][y];
            }
        }
        return result;
    }
}
