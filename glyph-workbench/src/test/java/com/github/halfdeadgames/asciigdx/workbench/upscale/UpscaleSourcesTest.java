package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSources.Font;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSources.Option;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class UpscaleSourcesTest {

    private static Font square(String folder, int size) {
        return new Font(folder, size, size);
    }

    @Test
    void sixteenFromEightHasFactorTwo() {
        List<Option> options = UpscaleSources.options(square("f", 16), List.of(square("f", 8)));
        assertEquals(1, options.size());
        assertTrue(options.get(0).usable());
        assertEquals(2, options.get(0).factor());
    }

    @Test
    void twentyFourIsUsableFromEightAtFactorThree() {
        List<Option> options = UpscaleSources.options(square("f", 24), List.of(square("f", 8), square("f", 16)));
        assertTrue(options.get(0).usable());
        assertEquals(3, options.get(0).factor());
    }

    @Test
    void twentyFourFromSixteenIsNotAnEvenDivision() {
        List<Option> options = UpscaleSources.options(square("f", 24), List.of(square("f", 8), square("f", 16)));
        assertFalse(options.get(1).usable());
        assertEquals("NOT EVEN", options.get(1).problem());
        assertEquals(0, options.get(1).factor());
    }

    @Test
    void defaultPrefersTheLargerSourceOfTheSameFamily() {
        List<Font> fonts = List.of(square("a", 8), square("b", 16), square("a", 16));
        Font target = square("a", 32);
        List<Option> options = UpscaleSources.options(target, fonts);
        assertEquals(2, UpscaleSources.defaultIndex(target, options));
    }

    @Test
    void defaultPrefersSameFamilyEvenWhenItIsSmaller() {
        List<Font> fonts = List.of(square("b", 16), square("a", 8));
        Font target = square("a", 32);
        List<Option> options = UpscaleSources.options(target, fonts);
        assertEquals(1, UpscaleSources.defaultIndex(target, options));
    }

    @Test
    void tallFontIsUsableFromHalfSizeTallFont() {
        Font target = new Font("f", 16, 32);
        List<Option> options = UpscaleSources.options(target, List.of(new Font("f", 8, 16)));
        assertTrue(options.get(0).usable());
        assertEquals(2, options.get(0).factor());
    }

    @Test
    void tallFontFromSquareFontIsADifferentShape() {
        Font target = new Font("f", 16, 32);
        List<Option> options = UpscaleSources.options(target, List.of(square("f", 8)));
        assertFalse(options.get(0).usable());
        assertEquals("SHAPE", options.get(0).problem());
    }

    @Test
    void largerSourceIsNotSmaller() {
        List<Option> options = UpscaleSources.options(square("f", 16), List.of(square("g", 24)));
        assertEquals("TOO BIG", options.get(0).problem());
    }

    @Test
    void equalSizeFromAnotherFamilyIsNotSmaller() {
        List<Option> options = UpscaleSources.options(square("f", 16), List.of(square("g", 16)));
        assertEquals("TOO BIG", options.get(0).problem());
    }

    @Test
    void targetItselfIsLeftOut() {
        Font target = square("f", 16);
        List<Option> options = UpscaleSources.options(target, List.of(square("f", 8), target));
        assertEquals(1, options.size());
        assertEquals(square("f", 8), options.get(0).font());
    }

    @Test
    void sameSizeInAnotherFolderIsKept() {
        List<Option> options = UpscaleSources.options(square("f", 16), List.of(square("g", 16)));
        assertEquals(1, options.size());
    }

    @Test
    void defaultFallsBackToFirstUsableOfAnotherFamily() {
        List<Font> fonts = List.of(square("b", 24), square("c", 8), square("d", 16));
        Font target = square("a", 32);
        List<Option> options = UpscaleSources.options(target, fonts);
        assertEquals(1, UpscaleSources.defaultIndex(target, options));
    }

    @Test
    void defaultIsMinusOneWhenNothingIsUsable() {
        Font target = square("a", 24);
        List<Option> options = UpscaleSources.options(target, List.of(square("a", 16), square("b", 32)));
        assertEquals(-1, UpscaleSources.defaultIndex(target, options));
    }

    @Test
    void defaultIsMinusOneForNoOptions() {
        assertEquals(-1, UpscaleSources.defaultIndex(square("a", 16), List.of()));
    }

    @Test
    void optionsKeepTheOrderOfTheFonts() {
        List<Font> fonts = List.of(square("f", 16), square("f", 8), square("f", 4));
        List<Option> options = UpscaleSources.options(square("f", 32), fonts);
        assertEquals(fonts, options.stream().map(Option::font).collect(Collectors.toList()));
    }
}
