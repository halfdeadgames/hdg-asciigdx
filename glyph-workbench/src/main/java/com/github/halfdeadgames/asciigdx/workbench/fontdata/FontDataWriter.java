package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import java.util.ArrayList;
import java.util.List;

/** Writes {@link FontData} as font data text, glyphs in sheet order and derived glyphs as recipes. */
public final class FontDataWriter {

    private FontDataWriter() {
    }

    /**
     * Writes a font.
     *
     * @param data the font
     * @return text with LF line endings, which {@link FontDataParser#parse} reads back to an equal font
     */
    public static String write(FontData data) {
        List<String> blocks = new ArrayList<>();
        blocks.add("cell " + data.cellWidth() + "x" + data.cellHeight());
        for (int codePoint : SheetOrder.slotsFor(data.entries().keySet())) {
            GlyphEntry entry = data.entries().get(codePoint);
            if (entry != null) {
                blocks.add(writeEntry(entry));
            }
        }
        return String.join("\n\n", blocks) + "\n";
    }

    private static String writeEntry(GlyphEntry entry) {
        String header = codePointLabel(entry.codePoint());
        if (entry instanceof PixelGlyph pixelGlyph) {
            String title = pixelGlyph.full() ? header + " full" : header;
            return title + "\n" + String.join("\n", pixelGlyph.pixels().toRows());
        }
        return header + " = " + writeOp(((DerivedGlyph) entry).op());
    }

    private static String writeOp(GlyphOp op) {
        List<String> sources = new ArrayList<>();
        for (GlyphOp.Source source : op.sources()) {
            String reference = String.format("U+%04X", source.codePoint());
            sources.add(op.kind() == GlyphOp.Kind.OVERLAY ? reference + " shift " + source.shift() : reference);
        }
        return op.kind().keyword() + " " + String.join(", ", sources);
    }

    private static String codePointLabel(int codePoint) {
        String character = codePoint == ' '
            ? "\\u{20}"
            : GlyphOrderText.escapeCodePoint(codePoint);
        return String.format("U+%04X ", codePoint) + character;
    }
}
