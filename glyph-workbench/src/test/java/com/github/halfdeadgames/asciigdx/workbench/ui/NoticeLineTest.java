package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NoticeLineTest {

    @Test
    void shortTextStaysUnchanged() {
        assertEquals("CANNOT SAVE", NoticeLine.fit("CANNOT SAVE", 20));
    }

    @Test
    void exactFitStaysUnchanged() {
        assertEquals("ABCDEF", NoticeLine.fit("ABCDEF", 6));
    }

    @Test
    void oneTooLongEndsWithTwoDotsAndFillsTheRoom() {
        String fitted = NoticeLine.fit("ABCDEFG", 6);
        assertEquals("ABCD..", fitted);
        assertEquals(6, fitted.length());
    }

    @Test
    void roomOfOneOrTwoIsHardCut() {
        assertEquals("A", NoticeLine.fit("ABCDEFG", 1));
        assertEquals("AB", NoticeLine.fit("ABCDEFG", 2));
    }
}
