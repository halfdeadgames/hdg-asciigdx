# Font Rules

Rules shared by every font in this project. Each font adds its own files:

- `<size>.rules.json`: rules the workbench checks as you draw, per size.
- `notes.md`: the font's style and design notes, never checked.

The format of both is described in `glyph-workbench/src/README.md`.

## Cells and sizes
- Two cell shapes: square (maps and gameplay) and tall (long text). Games may mix both.
- Cell sizes are 8, 16, 32, or 64 pixels wide. No in-between sizes.
- Each size is judged as its own design at that size. An upscaled start still gets reworked until it reads well.
- The app never assumes a gap between glyphs. A font that wants one declares it in its rules file as columns with no ink.
- Equal cells give the monospace look, so ink widths may vary by letter shape.
- All glyphs are 1-bit: each pixel is on or off.

## Drawing
- Fix the metrics first: baseline, cap height, x-height, ascender, and descender. Every glyph follows them.
- Draw glyphs in groups. Each group starts from key glyphs: n and o for lowercase, H and O for capitals. The rest of the group reuses their stems, bowls, and bars.
- Diagonals step evenly (1:1 or 2:1). Never mix step lengths in one line.
- Curves step in runs that shrink steadily, such as 3, 2, 1, 1.
- No doubled pixels on a stroke, and no orphan pixels touching only at a corner.
- Confusable glyphs need a feature that tells them apart: 0 O, 1 l I, 5 S, 8 B, rn m, comma and period, quote and backtick.
- Narrow letters get small slabs to fill the cell. Text should look even, with no blotchy words.
- Judge glyphs at 1x, in words, code, and maps, on dark and light backgrounds.

## Matching glyphs
- A glyph that is an inverse, rotation, mirror, or repeat of another glyph is derived from it, never redrawn.
- Letters are the exception: draw every letter by hand, even when it mirrors another (b d p q), so each one can be tuned.
- Accented lowercase letters are an overlay of a base letter and the accent mark, so every accent keeps one shape.
- Rotations and mirrors turn the glyph about the center of the cell.
- Edit the source glyph. Derived glyphs update on export.

## Tiling glyphs
- Box drawing, blocks, and shades fill the whole cell, with no blank space around them.
- Lines and edges must join across neighboring cells at every size.
- Single lines sit on the same row and column in every box-drawing glyph. Double lines sit symmetrically around that line.
- Half blocks split the cell exactly in half.
- Judge tiling glyphs in repeated rows and columns, never alone.

## Sheet order
1. CP437 first, in its standard 16 x 16 layout.
2. All other glyphs by code point. Each Unicode block starts on a new row.
3. Private Use Area glyphs last, by code point.
The export builds this order. Nobody orders the sheet by hand.

## Coverage tiers
1. CP437.
2. Drawing and pictorial glyphs: the rest of box drawing, block elements, geometric shapes, arrows, and symbols. Each set is picked by hand, and a glyph that would look the same as another at the font's size is left out. Braille and quadrant glyphs are left out too.
3. Built-in fonts stop there. Accented letters and other scripts beyond CP437 belong in custom fonts.
