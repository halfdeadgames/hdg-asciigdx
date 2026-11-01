package com.github.halfdeadgames.asciigdx.sandbox.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.sandbox.review.ReviewSession.FailReason;
import com.github.halfdeadgames.asciigdx.sandbox.review.ReviewSession.Verdict;

import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class ReviewSessionTest {
    private static ReviewSession sessionOf(int... codePoints) {
        return new ReviewSession(codePoints);
    }

    @Test
    void perfectAdvancesToNextGlyph() {
        ReviewSession session = sessionOf('A', 'B');
        assertEquals(OptionalInt.of('A'), session.current());
        session.perfect();
        assertEquals(OptionalInt.of('B'), session.current());
        assertEquals(1, session.answeredCount());
        assertEquals(2, session.totalCount());
    }

    @Test
    void finishesAfterLastAnswerAndIgnoresFurtherAnswers() {
        ReviewSession session = sessionOf('A');
        session.fail();
        assertTrue(session.isFinished());
        assertEquals(OptionalInt.empty(), session.current());
        session.perfect();
        assertEquals(1, session.answeredCount());
    }

    @Test
    void failWithReasonIsWritten() {
        ReviewSession session = sessionOf('A');
        session.fail();
        session.setReasonForLast(FailReason.OFF_CENTER);
        assertEquals("U+0041\tA\tfail\t2\n", session.toResultsText());
    }

    @Test
    void failWithoutReasonOmitsReasonField() {
        ReviewSession session = sessionOf('A');
        session.fail();
        assertEquals("U+0041\tA\tfail\n", session.toResultsText());
    }

    @Test
    void needsWorkWithReasonIsWritten() {
        ReviewSession session = sessionOf('A');
        session.needsWork();
        session.setReasonForLast(FailReason.TOO_SIMILAR);
        assertEquals("U+0041\tA\tneeds-work\t3\n", session.toResultsText());
        assertEquals(Optional.of(Verdict.NEEDS_WORK), session.lastVerdict());
        assertEquals(Optional.of(FailReason.TOO_SIMILAR), session.lastReason());
    }

    @Test
    void reasonIsIgnoredAfterPerfect() {
        ReviewSession session = sessionOf('A');
        session.perfect();
        session.setReasonForLast(FailReason.TOO_SIMILAR);
        assertEquals("U+0041\tA\tperfect\n", session.toResultsText());
        assertTrue(session.lastReason().isEmpty());
    }

    @Test
    void countsVerdicts() {
        ReviewSession session = sessionOf('A', 'B', 'C', 'D');
        session.perfect();
        session.needsWork();
        session.fail();
        session.perfect();
        assertEquals(2, session.count(Verdict.PERFECT));
        assertEquals(1, session.count(Verdict.NEEDS_WORK));
        assertEquals(1, session.count(Verdict.FAIL));
    }

    @Test
    void reasonWithNoAnswersIsIgnored() {
        ReviewSession session = sessionOf('A');
        session.setReasonForLast(FailReason.TOO_SIMILAR);
        assertEquals("", session.toResultsText());
    }

    @Test
    void lastCodePointFollowsLatestAnswer() {
        ReviewSession session = new ReviewSession(new int[] {'A', 'B'});
        assertEquals(OptionalInt.empty(), session.lastCodePoint());
        session.fail();
        assertEquals(OptionalInt.of('A'), session.lastCodePoint());
        session.perfect();
        assertEquals(OptionalInt.of('B'), session.lastCodePoint());
        session.undo();
        assertEquals(OptionalInt.of('A'), session.lastCodePoint());
    }

    @Test
    void undoRemovesLastAnswerAndReopensGlyph() {
        ReviewSession session = sessionOf('A', 'B');
        session.perfect();
        session.fail();
        session.undo();
        assertEquals(OptionalInt.of('B'), session.current());
        assertEquals(1, session.answeredCount());
        session.undo();
        session.undo();
        assertEquals(0, session.answeredCount());
        assertFalse(session.isFinished());
    }

    @Test
    void duplicateCodePointsKeepFirstOccurrence() {
        ReviewSession session = sessionOf('A', 'B', 'A');
        assertEquals(2, session.totalCount());
    }

    @Test
    void reasonNumbersLookUp() {
        assertEquals(FailReason.WRONG_ORIENTATION, FailReason.fromNumber(5).orElseThrow());
        assertTrue(FailReason.fromNumber(0).isEmpty());
        assertTrue(FailReason.fromNumber(6).isEmpty());
    }

    @Test
    void textRoundTrips() {
        ReviewSession session = sessionOf('A', 'B', 'C');
        session.perfect();
        session.needsWork();
        session.setReasonForLast(FailReason.UGLY_OR_WRONG_SHAPE);
        session.fail();
        ReviewSession restored = new ReviewSession(new int[] {'A', 'B', 'C'}, session.toResultsText());
        assertEquals(session.toResultsText(), restored.toResultsText());
        assertTrue(restored.isFinished());
    }

    @Test
    void resumeSkipsAnsweredGlyphs() {
        ReviewSession session = new ReviewSession(new int[] {'A', 'B', 'C'}, "U+0041\tA\tperfect\n");
        assertEquals(1, session.answeredCount());
        assertEquals(OptionalInt.of('B'), session.current());
    }

    @Test
    void resumeIgnoresCodePointsNotInList() {
        ReviewSession session = new ReviewSession(new int[] {'A'}, "U+005A\tZ\tperfect\n");
        assertEquals(0, session.answeredCount());
        assertEquals("", session.toResultsText());
    }

    @Test
    void resumeKeepsFirstLineForRepeatedCodePoint() {
        ReviewSession session = new ReviewSession(new int[] {'A'}, "U+0041\tA\tperfect\nU+0041\tA\tfail\n");
        assertEquals("U+0041\tA\tperfect\n", session.toResultsText());
    }

    @Test
    void resumeAcceptsCrlfAndBlankLines() {
        String text = "U+0041\tA\tperfect\r\n\r\n  \r\nU+0042\tB\tneeds-work\t3\r\n";
        ReviewSession session = new ReviewSession(new int[] {'A', 'B'}, text);
        assertTrue(session.isFinished());
        assertEquals("U+0041\tA\tperfect\nU+0042\tB\tneeds-work\t3\n", session.toResultsText());
    }

    @Test
    void malformedLineReportsLineNumber() {
        String text = "U+0041\tA\tperfect\nnonsense\n";
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> new ReviewSession(new int[] {'A'}, text));
        assertTrue(error.getMessage().contains("line 2"), error.getMessage());
    }

    @Test
    void badVerdictBadReasonAndReasonOnPerfectAreMalformed() {
        int[] list = {'A'};
        assertThrows(IllegalArgumentException.class, () -> new ReviewSession(list, "U+0041\tA\tmaybe\n"));
        assertThrows(IllegalArgumentException.class, () -> new ReviewSession(list, "U+0041\tA\tpass\n"));
        assertThrows(IllegalArgumentException.class, () -> new ReviewSession(list, "U+0041\tA\tfail\t9\n"));
        assertThrows(IllegalArgumentException.class, () -> new ReviewSession(list, "U+0041\tA\tneeds-work\tx\n"));
        assertThrows(IllegalArgumentException.class, () -> new ReviewSession(list, "U+0041\tA\tperfect\t1\n"));
        assertThrows(IllegalArgumentException.class, () -> new ReviewSession(list, "U+ZZZZ\tA\tperfect\n"));
    }

    @Test
    void privateUseCharacterIsEscaped() {
        ReviewSession session = sessionOf(0xE01F);
        session.perfect();
        assertEquals("U+E01F\t\\u{E01F}\tperfect\n", session.toResultsText());
    }

    @Test
    void largeCodePointUsesMoreThanFourDigits() {
        ReviewSession session = sessionOf(0x1F600);
        session.perfect();
        assertTrue(session.toResultsText().startsWith("U+1F600\t"));
    }
}
