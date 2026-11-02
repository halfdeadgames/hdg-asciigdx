package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GlyphAnswerTest {

    @Test
    void emptyNoteIsPerfect() {
        assertEquals(GlyphAnswer.PERFECT, GlyphAnswer.verdictFor("hand", ""));
    }

    @Test
    void anyNoteNeedsWork() {
        assertEquals(GlyphAnswer.NEEDS_WORK, GlyphAnswer.verdictFor("nearest", "too thin"));
    }

    @Test
    void nonePickNeedsHandDrawnWhateverTheNote() {
        assertEquals(GlyphAnswer.NEEDS_HAND_DRAWN, GlyphAnswer.verdictFor(GlyphAnswer.NONE_PICK, ""));
        assertEquals(GlyphAnswer.NEEDS_HAND_DRAWN, GlyphAnswer.verdictFor(GlyphAnswer.NONE_PICK, "x"));
    }
}
