package com.github.halfdeadgames.asciigdx.workbench.upscale;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.List;

/** Lists the distinct upscaled versions of one glyph, so the user can pick one. */
public final class GlyphCandidates {

    /**
     * One upscaled version of a glyph.
     *
     * @param label name of the method that made it
     * @param full whether the source glyph is full
     * @param cell the whole target cell, as wide as the target cell width
     */
    public record Candidate(String label, boolean full, PixelGrid cell) {
    }

    private GlyphCandidates() {
    }

    /**
     * Upscales a glyph with every method that exists for the factor. A result equal to an earlier one is
     * dropped, so the first method that produces it wins.
     *
     * @param source the glyph in the smaller font
     * @param factor whole upscale factor
     * @param targetCellWidth cell width after scaling
     * @return the distinct candidates in method order
     */
    public static List<Candidate> forGlyph(PixelGlyph source, int factor, int targetCellWidth) {
        List<Candidate> candidates = new ArrayList<>();
        for (Upscaler.Method method : Upscaler.methodsFor(factor)) {
            PixelGrid cell = Upscaler.upscale(source, factor, method, targetCellWidth);
            if (!isAlreadyListed(candidates, cell)) {
                candidates.add(new Candidate(labelFor(method, factor), source.full(), cell));
            }
        }
        return candidates;
    }

    private static boolean isAlreadyListed(List<Candidate> candidates, PixelGrid cell) {
        for (Candidate candidate : candidates) {
            if (candidate.cell().equals(cell)) {
                return true;
            }
        }
        return false;
    }

    private static String labelFor(Upscaler.Method method, int factor) {
        return method == Upscaler.Method.NEAREST ? method.label + " " + factor + "X" : method.label;
    }
}
