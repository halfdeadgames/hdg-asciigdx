package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GlyphCandidatesTest {

    private static PixelGlyph glyphOf(int codePoint, boolean full, String litRow, String otherRow) {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            rows.add(y == 3 ? litRow : otherRow);
        }
        return new PixelGlyph(codePoint, full, PixelGrid.ofRows(rows));
    }

    private static PixelGlyph bodyGlyph() {
        return glyphOf('A', false, "#######", "......#");
    }

    private static PixelGlyph blankBodyGlyph() {
        return glyphOf('A', false, ".......", ".......");
    }

    private static PixelGlyph fullGlyph() {
        return glyphOf(0x2588, true, "########", "#.......");
    }

    @Test
    void factorTwoListsNearestFirst() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(bodyGlyph(), 2, 16);
        assertEquals("NEAREST 2X", candidates.get(0).label());
        assertEquals(Upscaler.upscale(bodyGlyph(), 2, Upscaler.Method.NEAREST, 16), candidates.get(0).cell());
    }

    @Test
    void candidatesNeverRepeatACell() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(bodyGlyph(), 2, 16);
        for (int first = 0; first < candidates.size(); first++) {
            for (int second = first + 1; second < candidates.size(); second++) {
                assertFalse(candidates.get(first).cell().equals(candidates.get(second).cell()));
            }
        }
    }

    @Test
    void methodsThatAgreeCollapseToTheFirstOne() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(blankBodyGlyph(), 2, 16);
        assertEquals(1, candidates.size());
        assertEquals("NEAREST 2X", candidates.get(0).label());
    }

    @Test
    void factorThreeHasOnlyNearest() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(bodyGlyph(), 3, 24);
        assertEquals(1, candidates.size());
        assertEquals("NEAREST 3X", candidates.get(0).label());
    }

    @Test
    void everyCellHasTheTargetWidth() {
        for (GlyphCandidates.Candidate candidate : GlyphCandidates.forGlyph(bodyGlyph(), 2, 16)) {
            assertEquals(16, candidate.cell().getWidth(), candidate.label());
            assertFalse(candidate.full());
        }
    }

    @Test
    void fullGlyphKeepsTheFullFlagAndWholeWidth() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(fullGlyph(), 2, 16);
        for (GlyphCandidates.Candidate candidate : candidates) {
            assertTrue(candidate.full(), candidate.label());
            assertEquals(16, candidate.cell().getWidth(), candidate.label());
        }
    }
}
