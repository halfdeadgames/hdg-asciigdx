package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import org.junit.jupiter.api.Test;

class DialogStyleTest {

    private static final LabelStyle LOOK = LabelStyle.of(0xFFFFFFFF, 0x111111FF);
    private static final WindowStyle WINDOW = WindowStyle.of(BoxStyle.SINGLE, LOOK, LOOK, 0);
    private static final ButtonStyle BUTTON = ButtonStyle.of(LOOK, LOOK, LOOK, LOOK);
    private static final TextFieldStyle FIELD = TextFieldStyle.of(LOOK);

    private static DialogStyle plain() {
        return DialogStyle.of(WINDOW, LOOK, BUTTON, FIELD);
    }

    @Test
    void ofKeepsTheFourPartsAndDoesNotDim() {
        DialogStyle style = plain();

        assertSame(WINDOW, style.getWindow());
        assertSame(LOOK, style.getMessage());
        assertSame(BUTTON, style.getButtons());
        assertSame(FIELD, style.getTextField());
        assertFalse(style.hasDim());
        assertThrows(IllegalStateException.class, style::getDimRgba);
        assertThrows(IllegalStateException.class, style::getDimAmount);
    }

    @Test
    void withDimStoresTheColorWithFullAlphaAndTheAmount() {
        DialogStyle style = plain().withDim(0x20406000, 0.5f);

        assertTrue(style.hasDim());
        assertEquals(0x204060FF, style.getDimRgba());
        assertEquals(0.5f, style.getDimAmount());
    }

    @Test
    void withDimDoesNotChangeTheOriginal() {
        DialogStyle original = plain();

        original.withDim(0xFF, 1f);

        assertFalse(original.hasDim());
    }

    @Test
    void anAmountOfZeroMeansNoDimAndEqualsAStyleWithoutOne() {
        DialogStyle style = plain().withDim(0x204060FF, 0f);

        assertFalse(style.hasDim());
        assertEquals(plain(), style);
    }

    @Test
    void anAmountOfOneIsAllowed() {
        assertEquals(1f, plain().withDim(0xFF, 1f).getDimAmount());
    }

    @Test
    void invalidAmountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> plain().withDim(0xFF, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> plain().withDim(0xFF, -0.01f));
        assertThrows(IllegalArgumentException.class, () -> plain().withDim(0xFF, 1.01f));
    }

    @Test
    void nullPartsAreRejected() {
        assertThrows(NullPointerException.class,
                () -> DialogStyle.of(null, LOOK, BUTTON, FIELD));
        assertThrows(NullPointerException.class,
                () -> DialogStyle.of(WINDOW, null, BUTTON, FIELD));
        assertThrows(NullPointerException.class,
                () -> DialogStyle.of(WINDOW, LOOK, null, FIELD));
        assertThrows(NullPointerException.class,
                () -> DialogStyle.of(WINDOW, LOOK, BUTTON, null));
    }

    @Test
    void equalityCoversTheDim() {
        assertEquals(plain().withDim(0x204060AA, 0.5f), plain().withDim(0x204060BB, 0.5f));
        assertEquals(plain().withDim(0x204060FF, 0.5f).hashCode(),
                plain().withDim(0x204060FF, 0.5f).hashCode());
        assertNotEquals(plain().withDim(0x204060FF, 0.5f), plain().withDim(0x204060FF, 0.6f));
        assertNotEquals(plain().withDim(0x204060FF, 0.5f), plain());
    }
}
