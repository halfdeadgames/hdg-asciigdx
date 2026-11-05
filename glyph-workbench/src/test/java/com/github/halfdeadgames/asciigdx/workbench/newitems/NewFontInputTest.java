package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.halfdeadgames.asciigdx.workbench.newitems.NewFontInput.Shape;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewFontInputTest {

    private static final List<String> NONE = List.of();

    @Test
    void ordinaryNamesPass() {
        assertNull(NewFontInput.nameError("saiksistt", NONE));
        assertNull(NewFontInput.nameError("my-font-2", NONE));
        assertNull(NewFontInput.nameError("a", NONE));
    }

    @Test
    void lengthLimitsApply() {
        assertNotNull(NewFontInput.nameError("", NONE));
        assertNotNull(NewFontInput.nameError(null, NONE));
        assertNull(NewFontInput.nameError("a".repeat(32), NONE));
        assertNotNull(NewFontInput.nameError("a".repeat(33), NONE));
    }

    @Test
    void onlyLowercaseDigitsAndHyphenAreAllowed() {
        assertNotNull(NewFontInput.nameError("My-Font", NONE));
        assertNotNull(NewFontInput.nameError("my font", NONE));
        assertNotNull(NewFontInput.nameError("my_font", NONE));
        assertNotNull(NewFontInput.nameError("font.v2", NONE));
        assertNotNull(NewFontInput.nameError("café", NONE));
        assertNotNull(NewFontInput.nameError("a/b", NONE));
    }

    @Test
    void duplicatesAreRefused() {
        assertNotNull(NewFontInput.nameError("saiksistt", List.of("saiksistt")));
        assertNull(NewFontInput.nameError("saiksistt", List.of("other")));
    }

    @Test
    void duplicatesDifferingOnlyInCaseAreRefused() {
        assertNotNull(NewFontInput.nameError("saiksistt", List.of("Saiksistt")));
        assertNotNull(NewFontInput.nameError("saiksistt", List.of("SAIKSISTT")));
    }

    @Test
    void windowsReservedNamesAreRefused() {
        for (var reserved : List.of("con", "prn", "aux", "nul", "com1", "com9", "lpt1", "lpt9")) {
            assertNotNull(NewFontInput.nameError(reserved, NONE), reserved);
        }
        assertNotNull(NewFontInput.nameError("CON", NONE));
        assertNull(NewFontInput.nameError("com0", NONE));
        assertNull(NewFontInput.nameError("com10", NONE));
        assertNull(NewFontInput.nameError("console", NONE));
    }

    @Test
    void squareHeightsAreMultiplesOfEightUpToSixtyFour() {
        for (int height = 8; height <= 64; height += 8) {
            assertNull(NewFontInput.sizeError(height, Shape.SQUARE), "height " + height);
        }
        assertNotNull(NewFontInput.sizeError(0, Shape.SQUARE));
        assertNotNull(NewFontInput.sizeError(-8, Shape.SQUARE));
        assertNotNull(NewFontInput.sizeError(12, Shape.SQUARE));
        assertNotNull(NewFontInput.sizeError(72, Shape.SQUARE));
    }

    @Test
    void tallHeightsAreSixteenThirtyTwoFortyEightSixtyFour() {
        for (int height : new int[] {16, 32, 48, 64}) {
            assertNull(NewFontInput.sizeError(height, Shape.TALL), "height " + height);
        }
        assertNotNull(NewFontInput.sizeError(8, Shape.TALL));
        assertNotNull(NewFontInput.sizeError(24, Shape.TALL));
        assertNotNull(NewFontInput.sizeError(0, Shape.TALL));
        assertNotNull(NewFontInput.sizeError(80, Shape.TALL));
    }

    @Test
    void cellWidthFollowsShape() {
        assertEquals(8, NewFontInput.cellWidth(8, Shape.SQUARE));
        assertEquals(24, NewFontInput.cellWidth(24, Shape.SQUARE));
        assertEquals(8, NewFontInput.cellWidth(16, Shape.TALL));
        assertEquals(32, NewFontInput.cellWidth(64, Shape.TALL));
    }

    @Test
    void shapeIsDerivedFromCellSize() {
        assertEquals(Shape.SQUARE, NewFontInput.shapeOf(8, 8));
        assertEquals(Shape.TALL, NewFontInput.shapeOf(8, 16));
        assertEquals(Shape.TALL, NewFontInput.shapeOf(16, 32));
    }
}
