package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntUnaryOperator;

/** Turns every glyph of a {@link FontData}, including derived ones, into drawn pixels. */
public final class GlyphResolver {

    private final FontData data;
    private final IntUnaryOperator lineOfCodePoint;
    private final Map<Integer, PixelGlyph> resolved = new HashMap<>();
    private final List<Integer> resolving = new ArrayList<>();

    private GlyphResolver(FontData data, IntUnaryOperator lineOfCodePoint) {
        this.data = data;
        this.lineOfCodePoint = lineOfCodePoint;
    }

    /**
     * Resolves every glyph.
     *
     * @param data the font
     * @return pixel glyphs by code point
     * @throws FontDataException if a reference is unknown, a derivation is circular, or an operation is not allowed
     */
    public static Map<Integer, PixelGlyph> resolveAll(FontData data) {
        return resolveAll(data, codePoint -> 0);
    }

    /**
     * Resolves every glyph and reports problems with the line where the glyph was defined.
     *
     * @param data the font
     * @param lineOfCodePoint gives the 1-based definition line of a code point, or 0
     * @return pixel glyphs by code point
     * @throws FontDataException if a reference is unknown, a derivation is circular, or an operation is not allowed
     */
    public static Map<Integer, PixelGlyph> resolveAll(FontData data, IntUnaryOperator lineOfCodePoint) {
        GlyphResolver resolver = new GlyphResolver(data, lineOfCodePoint);
        Map<Integer, PixelGlyph> all = new TreeMap<>();
        for (int codePoint : data.entries().keySet()) {
            all.put(codePoint, resolver.resolve(codePoint, 0));
        }
        return all;
    }

    private PixelGlyph resolve(int codePoint, int referrerLine) {
        GlyphEntry entry = data.entries().get(codePoint);
        if (entry == null) {
            throw new FontDataException(referrerLine, "unknown code point " + name(codePoint));
        }
        if (entry instanceof PixelGlyph pixelGlyph) {
            return pixelGlyph;
        }
        PixelGlyph known = resolved.get(codePoint);
        if (known != null) {
            return known;
        }
        int line = lineOfCodePoint.applyAsInt(codePoint);
        if (resolving.contains(codePoint)) {
            throw new FontDataException(line, "derivation cycle: " + cyclePath(codePoint));
        }
        resolving.add(codePoint);
        PixelGlyph result;
        try {
            result = apply((DerivedGlyph) entry, line);
        } catch (IllegalArgumentException e) {
            throw new FontDataException(line, name(codePoint) + ": " + e.getMessage());
        }
        resolving.remove(resolving.size() - 1);
        resolved.put(codePoint, result);
        return result;
    }

    private PixelGlyph apply(DerivedGlyph derived, int line) {
        GlyphOp op = derived.op();
        int codePoint = derived.codePoint();
        PixelGlyph source = resolve(op.sources().get(0).codePoint(), line);
        PixelGrid pixels = source.pixels();
        return switch (op.kind()) {
            case INVERT -> new PixelGlyph(codePoint, true, source.toCell(data.cellWidth()).inverted());
            case ROTATE90 -> new PixelGlyph(codePoint, source.full() || hasSquareCell(), rotate(source, 1));
            case ROTATE180 -> new PixelGlyph(codePoint, source.full(), pixels.rotated180());
            case ROTATE270 -> new PixelGlyph(codePoint, source.full() || hasSquareCell(), rotate(source, 3));
            case MIRROR -> new PixelGlyph(codePoint, source.full(), pixels.mirrored());
            case FLIP -> new PixelGlyph(codePoint, source.full(), pixels.flipped());
            case OVERLAY -> overlay(codePoint, op, line);
        };
    }

    private boolean hasSquareCell() {
        return data.cellWidth() == data.cellHeight();
    }

    // In a square cell the gap column turns into a row, so the whole cell is rotated.
    private PixelGrid rotate(PixelGlyph source, int clockwiseQuarterTurns) {
        PixelGrid pixels = hasSquareCell() ? source.toCell(data.cellWidth()) : source.pixels();
        if (pixels.getWidth() != pixels.getHeight()) {
            throw new IllegalArgumentException("rotate90 and rotate270 need a square glyph, but "
                + name(source.codePoint()) + " is " + pixels.getWidth() + "x" + pixels.getHeight()
                + "; use a full glyph or a font whose cell height equals its body width");
        }
        for (int turn = 0; turn < clockwiseQuarterTurns; turn++) {
            pixels = pixels.rotatedClockwise();
        }
        return pixels;
    }

    private PixelGlyph overlay(int codePoint, GlyphOp op, int line) {
        PixelGlyph first = resolve(op.sources().get(0).codePoint(), line);
        PixelGrid combined = PixelGrid.blank(first.pixels().getWidth(), first.pixels().getHeight());
        for (GlyphOp.Source part : op.sources()) {
            PixelGlyph source = resolve(part.codePoint(), line);
            if (source.full() != first.full()) {
                throw new IllegalArgumentException("overlay cannot mix full glyphs and body glyphs: "
                    + name(first.codePoint()) + " and " + name(source.codePoint()));
            }
            combined = combined.withOverlay(source.pixels(), part.shift());
        }
        return new PixelGlyph(codePoint, first.full(), combined);
    }

    private String cyclePath(int codePoint) {
        StringBuilder path = new StringBuilder();
        for (int index = resolving.indexOf(codePoint); index < resolving.size(); index++) {
            path.append(name(resolving.get(index))).append(" -> ");
        }
        return path.append(name(codePoint)).toString();
    }

    private static String name(int codePoint) {
        return String.format("U+%04X", codePoint);
    }
}
