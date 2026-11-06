package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpscaleCandidatePickTest {

    private static GlyphCandidates.Candidate candidate(String label) {
        return new GlyphCandidates.Candidate(label, false, PixelGrid.blank(2, 2));
    }

    private static UpscaleCandidatePick threeCandidates() {
        return new UpscaleCandidatePick(List.of(candidate("A"), candidate("B"), candidate("C")));
    }

    @Test
    void startsOnTheFirstCandidate() {
        UpscaleCandidatePick pick = threeCandidates();
        assertEquals(0, pick.selectedIndex());
        assertEquals("A", pick.selected().label());
    }

    @Test
    void listsEveryLabelInOrder() {
        assertEquals(List.of("A", "B", "C"), threeCandidates().labels());
    }

    @Test
    void selectNumberIsOneBased() {
        UpscaleCandidatePick pick = threeCandidates();
        pick.selectNumber(3);
        assertEquals(2, pick.selectedIndex());
        assertEquals("C", pick.selected().label());
    }

    @Test
    void selectNumberOutOfRangeChangesNothing() {
        UpscaleCandidatePick pick = threeCandidates();
        pick.selectNumber(2);
        pick.selectNumber(0);
        pick.selectNumber(4);
        pick.selectNumber(-1);
        assertEquals(1, pick.selectedIndex());
    }

    @Test
    void moveStepsAndStopsAtBothEnds() {
        UpscaleCandidatePick pick = threeCandidates();
        pick.move(-1);
        assertEquals(0, pick.selectedIndex());
        pick.move(1);
        pick.move(1);
        pick.move(1);
        assertEquals(2, pick.selectedIndex());
        pick.move(-1);
        assertEquals(1, pick.selectedIndex());
    }

    @Test
    void refusesAnEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> new UpscaleCandidatePick(List.of()));
    }
}
