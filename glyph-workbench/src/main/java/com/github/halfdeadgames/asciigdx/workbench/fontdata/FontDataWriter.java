package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes {@link FontData} as font data text, CP437 glyphs first, then the rest by code point, and
 * derived glyphs as recipes.
 */
public final class FontDataWriter {

    private FontDataWriter() {
    }

    /**
     * Writes a font.
     *
     * @param data the font
     * @return text with LF line endings, which {@link FontDataParser#parse} reads back to an equal
     *     font
     */
    public static String write(FontData data) {
        List<String> blocks = new ArrayList<>();
        blocks.add("cell " + data.cellWidth() + "x" + data.cellHeight());
        for (int codePoint : fileOrder(data)) {
            blocks.add(writeEntry(data.entries().get(codePoint)));
        }
        return String.join("\n\n", blocks) + "\n";
    }

    // CP437 glyphs keep their fixed order, so toggling a font's glyph list never reorders the file;
    // every other glyph follows in ascending code point order.
    private static List<Integer> fileOrder(FontData data) {
        List<Integer> order = new ArrayList<>();
        for (int slot = 0; slot < GlyphOrder.CP437.getGlyphCount(); slot++) {
            int codePoint = GlyphOrder.CP437.getCodePoint(slot);
            if (data.entries().containsKey(codePoint)) {
                order.add(codePoint);
            }
        }
        for (int codePoint : data.entries().keySet()) {
            if (!GlyphOrder.CP437.contains(codePoint)) {
                order.add(codePoint);
            }
        }
        return order;
    }

    private static String writeEntry(GlyphEntry entry) {
        String header = codePointLabel(entry.codePoint());
        if (entry instanceof PixelGlyph pixelGlyph) {
            return header + "\n" + String.join("\n", pixelGlyph.pixels().toRows());
        }
        return header + " = " + writeOp(((DerivedGlyph) entry).op());
    }

    private static String writeOp(GlyphOp op) {
        List<String> sources = new ArrayList<>();
        for (GlyphOp.Source source : op.sources()) {
            String reference = CodePoints.label(source.codePoint());
            sources.add(op.kind() == GlyphOp.Kind.OVERLAY ? writeOverlayPart(reference, source)
                    : reference);
        }
        return op.kind().keyword() + " " + String.join(", ", sources);
    }

    private static String writeOverlayPart(String reference, GlyphOp.Source source) {
        String text = reference + " shift " + source.shift();
        return source.down() == 0 ? text : text + " down " + source.down();
    }

    private static String codePointLabel(int codePoint) {
        String character = codePoint == ' '
                ? "\\u{20}"
                : GlyphOrderText.escapeCodePoint(codePoint);
        return CodePoints.label(codePoint) + " " + character;
    }
}
