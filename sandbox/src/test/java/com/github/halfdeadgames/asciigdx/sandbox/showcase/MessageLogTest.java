package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MessageLogTest {

    private static final int ROWS = 4;

    private static MessageLog fiveLines() {
        return new MessageLog(
            new TypedLine(1f, "a", 30f),
            new TypedLine(2f, "b", 30f),
            new TypedLine(3f, "c", 30f),
            new TypedLine(4f, "d", 30f),
            new TypedLine(5f, "e", 30f));
    }

    @Test
    void nothingStartedBeforeFirstLine() {
        MessageLog log = fiveLines();
        assertEquals(0, log.startedCount(0.5f));
        assertEquals(0, log.firstShownLine(0.5f, ROWS));
    }

    @Test
    void countsStartedLines() {
        MessageLog log = fiveLines();
        assertEquals(1, log.startedCount(1f));
        assertEquals(3, log.startedCount(3.5f));
        assertEquals(5, log.startedCount(100f));
    }

    @Test
    void doesNotScrollUntilRowsAreFull() {
        assertEquals(0, fiveLines().firstShownLine(4f, ROWS));
    }

    @Test
    void scrollsOldestLineOffWhenFifthStarts() {
        assertEquals(1, fiveLines().firstShownLine(5f, ROWS));
    }

    @Test
    void exposesLines() {
        MessageLog log = fiveLines();
        assertEquals(5, log.lineCount());
        assertEquals("c", log.line(2).text());
    }

    @Test
    void allowsEqualStartTimes() {
        TypedLine first = new TypedLine(1f, "a", 30f);
        MessageLog log = new MessageLog(first, new TypedLine(1f, "b", 30f));
        assertSame(first, log.line(0));
    }

    @Test
    void rejectsDecreasingStartTimes() {
        assertThrows(IllegalArgumentException.class,
            () -> new MessageLog(new TypedLine(2f, "a", 30f), new TypedLine(1f, "b", 30f)));
    }

    @Test
    void rejectsEmptyLog() {
        assertThrows(IllegalArgumentException.class, MessageLog::new);
    }
}
