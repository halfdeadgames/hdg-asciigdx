package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import org.junit.jupiter.api.Test;

class WindowStyleTest {

    private static final LabelStyle BORDER = LabelStyle.of(0x111111FF, 0x010101FF);
    private static final LabelStyle FOCUSED = LabelStyle.of(0x222222FF, 0x020202FF);
    private static final LabelStyle TITLE = LabelStyle.of(0x333333FF, 0x030303FF);
    private static final int BACKGROUND = 0x555555FF;
    private static final ButtonStyle CLOSE = ButtonStyle.of(BORDER);

    private final WindowStyle plain = WindowStyle.of(BoxStyle.SINGLE, BORDER, TITLE, BACKGROUND);

    @Test
    void ofSetsTheGivenPartsAndDefaults() {
        assertEquals(BoxStyle.SINGLE, plain.getBox());
        assertSame(BORDER, plain.getBorder());
        assertSame(TITLE, plain.getTitle());
        assertEquals(BACKGROUND, plain.getBackgroundRgba());
        assertEquals(BORDER, plain.getFocusedBorder());
        assertFalse(plain.hasCloseButton());
        assertEquals("x", plain.getCloseText());
    }

    @Test
    void withersReturnCopiesAndLeaveTheOriginalAlone() {
        WindowStyle changed = plain.withFocusedBorder(FOCUSED).withCloseButton(CLOSE)
                .withCloseText("close");

        assertEquals(FOCUSED, changed.getFocusedBorder());
        assertTrue(changed.hasCloseButton());
        assertSame(CLOSE, changed.getCloseButton());
        assertEquals("close", changed.getCloseText());
        assertEquals(BORDER, plain.getFocusedBorder());
        assertFalse(plain.hasCloseButton());
    }

    @Test
    void closeButtonGetterThrowsWhenAbsent() {
        assertThrows(IllegalStateException.class, plain::getCloseButton);
    }

    @Test
    void emptyCloseTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> plain.withCloseText(""));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class,
                () -> WindowStyle.of(null, BORDER, TITLE, BACKGROUND));
        assertThrows(NullPointerException.class,
                () -> WindowStyle.of(BoxStyle.SINGLE, null, TITLE, BACKGROUND));
        assertThrows(NullPointerException.class,
                () -> WindowStyle.of(BoxStyle.SINGLE, BORDER, null, BACKGROUND));
        assertThrows(NullPointerException.class, () -> plain.withFocusedBorder(null));
        assertThrows(NullPointerException.class, () -> plain.withCloseButton(null));
        assertThrows(NullPointerException.class, () -> plain.withCloseText(null));
    }

    @Test
    void equalsAndHashCodeCoverEveryPart() {
        WindowStyle same = WindowStyle.of(BoxStyle.SINGLE, BORDER, TITLE, BACKGROUND);

        assertEquals(plain, same);
        assertEquals(plain.hashCode(), same.hashCode());
        assertNotEquals(plain, WindowStyle.of(BoxStyle.DOUBLE, BORDER, TITLE, BACKGROUND));
        assertNotEquals(plain, plain.withFocusedBorder(FOCUSED));
        assertNotEquals(plain, plain.withCloseButton(CLOSE));
        assertNotEquals(plain, plain.withCloseText("c"));
        assertNotEquals(plain, WindowStyle.of(BoxStyle.SINGLE, BORDER, TITLE, 0));
    }

    @Test
    void closeFontDefaultsToSquareAndCanBeChanged() {
        assertEquals(MarkFont.SQUARE, plain.getCloseFont());

        WindowStyle text = plain.withCloseFont(MarkFont.TEXT);

        assertEquals(MarkFont.TEXT, text.getCloseFont());
        assertEquals(MarkFont.SQUARE, plain.getCloseFont());
        assertThrows(NullPointerException.class, () -> plain.withCloseFont(null));
    }

    @Test
    void closeFontTakesPartInEqualsHashCodeAndToString() {
        WindowStyle text = plain.withCloseFont(MarkFont.TEXT);

        assertNotEquals(plain, text);
        assertEquals(text, plain.withCloseFont(MarkFont.TEXT));
        assertEquals(text.hashCode(), plain.withCloseFont(MarkFont.TEXT).hashCode());
        assertTrue(text.toString().contains("closeFont=TEXT"));
    }

    @Test
    void otherWithersKeepTheCloseFont() {
        WindowStyle text = plain.withCloseFont(MarkFont.TEXT);

        assertEquals(MarkFont.TEXT, text.withFocusedBorder(FOCUSED).getCloseFont());
        assertEquals(MarkFont.TEXT, text.withCloseButton(CLOSE).getCloseFont());
        assertEquals(MarkFont.TEXT, text.withCloseText("c").getCloseFont());
    }

    @Test
    void toStringNamesTheParts() {
        String text = plain.withCloseButton(CLOSE).toString();

        assertTrue(text.startsWith("WindowStyle["));
        assertTrue(text.contains("box=SINGLE"));
        assertTrue(text.contains("closeText=x"));
        assertTrue(plain.toString().contains("closeButton=none"));
    }
}
