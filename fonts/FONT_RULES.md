# Font Rules

Rules shared by every font in this project. Each font adds its own rules in `fonts/<name>/rules.md`.

## Cells and sizes
- Two cell shapes: square (map and game glyphs) and tall (UI text). Games may mix both.
- Cell widths are multiples of 8: 8, 16, 24, 32, 40.
- Unit k = cell width / 8. Every measurement scales by k: stroke k, letter gap k, glyph body 7k.
- Glyphs with a center stroke or point (A T V X Y I, arrows, crosses) use a width that shares parity with the stroke (both odd or both even), so the center stays true.
- A monospace look comes from equal cells, not equal ink. Drawn widths may vary by letter shape.
- The letter gap sits in the rightmost k columns of the cell.

## Family consistency
- Design the smallest size first, with later upscales in mind.
- A larger size starts from the smaller one scaled up, then adds detail: smoother curves, tapered ends, ink traps, and finer punctuation.
- Upscale only from a smaller size that divides the target evenly (16 from 8, 24 from 8, 32 from 16 or 8). Never scale by a fraction.
- A larger size never adds new features. If it isn't in the small size, it doesn't belong in the large one.
- All glyphs are 1-bit: each pixel is on or off.

## Drawing
- Fix the metrics first: baseline, cap height, x-height, ascender, descender, and gap. Every glyph follows them.
- Draw glyphs in groups. Each group starts from key glyphs: n and o for lowercase, H and O for capitals. The rest of the group reuses their stems, bowls, and bars.
- Base h m r u on n, b d p q c e on o, E F L T I on H, and C G Q D on O.
- Diagonals step evenly (1:1 or 2:1). Never mix step lengths in one line.
- Curves step in runs that shrink steadily, such as 3, 2, 1, 1.
- No doubled pixels on a stroke, and no orphan pixels touching only at a corner.
- Confusable glyphs need a feature that survives the smallest size: 0 O, 1 l I, 5 S, 8 B, rn m, comma and period, quote and backtick.
- Narrow letters get small slabs to fill the cell. Wide letters are compressed. Text should look even, with no blotchy words.
- Judge glyphs at 1x, in words, code, and maps, on dark and light backgrounds.

## Matching glyphs
- A glyph that is an inverse, rotation, mirror, or repeat of another glyph is derived from it, never redrawn.
- Letters are the exception: draw every letter by hand, even when it mirrors another (b d p q), so each one can be tuned.
- Rotations and mirrors turn the glyph body only. The letter gap stays on the right.
- Edit the source glyph. Derived glyphs update on export.

## Tiling glyphs
- Box drawing, blocks, and shades fill the whole cell with no gap.
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
1. CP437 and WGL4.
2. Project extras (meters, game pieces, symbols), arrows, math, and geometric shapes.
3. More Latin Extended and symbol blocks, as needed.
