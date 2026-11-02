package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontData;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataWriter;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphEntry;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphResolver;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The font data of the target size, which picked candidates are written into. */
final class UpscaleTarget {

    private FontData data;
    private Map<Integer, PixelGlyph> resolved;

    private UpscaleTarget(FontData data) {
        this.data = data;
        this.resolved = GlyphResolver.resolveAll(data);
    }

    /** A target with no glyphs yet, as created when the target size file does not exist. */
    static UpscaleTarget empty(int cellWidth, int cellHeight) {
        return new UpscaleTarget(new FontData(cellWidth, cellHeight, new HashMap<>()));
    }

    /** Reads an existing target file. */
    static UpscaleTarget parse(String text) {
        return new UpscaleTarget(FontDataParser.parse(text));
    }

    /** A saved state of the target, for undoing a {@link #put} whose file write failed. */
    record Snapshot(FontData data, Map<Integer, PixelGlyph> resolved) {
    }

    Snapshot snapshot() {
        return new Snapshot(data, resolved);
    }

    void restore(Snapshot snapshot) {
        data = snapshot.data();
        resolved = snapshot.resolved();
    }

    int cellWidth() {
        return data.cellWidth();
    }

    int cellHeight() {
        return data.cellHeight();
    }

    boolean hasGlyph(int codePoint) {
        return data.entries().containsKey(codePoint);
    }

    /** Whether the target already has a different glyph for the code point, so writing would replace it. */
    boolean wouldReplace(int codePoint, PixelGrid candidateCell) {
        PixelGlyph existing = resolved.get(codePoint);
        return existing != null && !existing.toCell(data.cellWidth()).equals(candidateCell);
    }

    /**
     * Stores a candidate as a pixel glyph, replacing any glyph or recipe for the code point. A normal
     * glyph keeps only its body (the gap columns are dropped); a full glyph keeps the whole cell.
     *
     * @throws IllegalArgumentException if the candidate cell does not have the target's cell size
     */
    void put(int codePoint, boolean full, PixelGrid candidateCell) {
        if (candidateCell.getWidth() != data.cellWidth() || candidateCell.getHeight() != data.cellHeight()) {
            throw new IllegalArgumentException("The target is " + data.cellWidth() + "x" + data.cellHeight()
                + " but the candidate is " + candidateCell.getWidth() + "x" + candidateCell.getHeight());
        }
        Map<Integer, GlyphEntry> entries = new HashMap<>(data.entries());
        entries.put(codePoint, new PixelGlyph(codePoint, full, full ? candidateCell : body(candidateCell)));
        FontData updated = new FontData(data.cellWidth(), data.cellHeight(), entries);
        resolved = GlyphResolver.resolveAll(updated);
        data = updated;
    }

    private PixelGrid body(PixelGrid cell) {
        int bodyWidth = data.bodyWidth();
        List<String> rows = new ArrayList<>();
        for (String row : cell.toRows()) {
            rows.add(row.substring(0, bodyWidth));
        }
        return PixelGrid.ofRows(rows);
    }

    /** The target file text. */
    String toText() {
        return FontDataWriter.write(data);
    }

    PixelGlyph glyph(int codePoint) {
        return resolved.get(codePoint);
    }
}
