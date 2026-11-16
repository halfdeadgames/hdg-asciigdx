package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GlyphCandidatesTest {

    private static PixelGlyph glyphOf(int codePoint, String litRow, String otherRow) {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            rows.add(y == 3 ? litRow : otherRow);
        }
        return new PixelGlyph(codePoint, PixelGrid.ofRows(rows));
    }

    private static PixelGlyph edgeGlyph() {
        return glyphOf('A', "########", ".......#");
    }

    private static PixelGlyph blankGlyph() {
        return glyphOf('A', "........", "........");
    }

    @Test
    void factorTwoListsNearestFirst() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(edgeGlyph(), 2, 2);
        assertEquals("NEAREST 2X", candidates.get(0).label());
        assertEquals(Upscaler.upscale(edgeGlyph(), 2, 2, Upscaler.Method.NEAREST),
                candidates.get(0).cell());
    }

    @Test
    void candidatesNeverRepeatACell() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(edgeGlyph(), 2, 2);
        for (int first = 0; first < candidates.size(); first++) {
            for (int second = first + 1; second < candidates.size(); second++) {
                assertFalse(candidates.get(first).cell().equals(candidates.get(second).cell()));
            }
        }
    }

    @Test
    void methodsThatAgreeCollapseToTheFirstOne() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(blankGlyph(), 2, 2);
        assertEquals(1, candidates.size());
        assertEquals("NEAREST 2X", candidates.get(0).label());
    }

    @Test
    void factorThreeHasOnlyNearest() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(edgeGlyph(), 3, 3);
        assertEquals(1, candidates.size());
        assertEquals("NEAREST 3X", candidates.get(0).label());
    }

    @Test
    void equalFactorsLabelOneFactor() {
        assertEquals("NEAREST 3X", GlyphCandidates.forGlyph(edgeGlyph(), 3, 3).get(0).label());
    }

    @Test
    void differentFactorsLabelBothAxes() {
        List<GlyphCandidates.Candidate> candidates = GlyphCandidates.forGlyph(edgeGlyph(), 2, 1);
        assertEquals(1, candidates.size());
        assertEquals("NEAREST 2X/1X", candidates.get(0).label());
    }

    @Test
    void everyCellIsTheSourceCellScaled() {
        for (GlyphCandidates.Candidate candidate : GlyphCandidates.forGlyph(edgeGlyph(), 2, 2)) {
            assertEquals(16, candidate.cell().getWidth(), candidate.label());
            assertEquals(16, candidate.cell().getHeight(), candidate.label());
        }
    }
}
