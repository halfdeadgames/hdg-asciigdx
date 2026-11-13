# Style
Terminal feel with some nostalgia, but none of the old fonts' frustrations. Readability comes first. Shapes are mostly boxy with a little roundness.

- Square cells are for maps and gameplay. Tall cells are for long text.
- Boxy where the shape can't be confused with another glyph. Diagonal where boxy would blur two glyphs: v and u, M and m, R and A.

# Strokes
- Letters, digits, and punctuation: 2 px verticals, 1 px horizontals.
- Box drawing, arrows, and UI glyphs: 2 px both ways, on center rows and columns 7-8.
- Round shapes get soft corners: cut 1 px off the outer corner and fill 1 px inside it. Straight letters keep sharp corners.
- Flat stroke ends. Diagonals step 1:1 or 2:1. Use 1 px ink traps sparingly.

# Metrics (16x16)
- Ascender row 1, capitals rows 2-11, x-height rows 6-11, descenders rows 12-14.
- Accented capitals shrink to rows 4-11 to leave room for the accent.
- Ink widths: capitals 10, lowercase 8, digits 8, wide letters (M W X m w) 12.
- Letters, digits, and most punctuation center across only and share the baseline. Symbols and UI glyphs center both ways.

# Metrics (16x32)
Trial values. They may change once the first drafts are drawn.
- Same widths, strokes, and letter shapes as 16x16. Only the heights change.
- Ascender row 6, capitals rows 8-23, x-height rows 13-23, descenders rows 24-29. Rows 0 and 31 stay blank as line spacing.
- Capitals center on rows 15-16, the middle of the cell, so symbols centered in the cell line up with text.
- Accented capitals keep full height. Accent marks are 3 rows tall: rows 3-5 over capitals, rows 8-10 over lowercase.
- Other row numbers in this file (the j hook, ª and º, and small glyphs) are for 16x16. Their 16x32 rows get set during drawing.

# Glyph classes (16x32)
- Text: letters, digits, punctuation, accents, and symbols used inside a line of text (currency, math, Greek, fractions, « » ¿ ¡ ¶ § … × ≠ —). Drafts start from the 16x16 glyph, with straight stem rows repeated and columns unchanged. Diagonal and round glyphs that would stretch badly (k v w x y z, 2 4 7, and similar) get drawn by hand instead.
- Connecting: box lines, stubs, diagonals, blocks, eighths, shades, checkers, and Legacy Computing fills. Redrawn for tiling, never stretched: lines on columns 7-8 and rows 15-16, eighths 4 rows, halves 16 rows.
- Pictorial: faces, suits, icons, pictographs, chess, dice, shapes, and standalone arrows. Copied from 16x16 and centered in the cell.

# Letter families
- Build h m r u from n. Build b d p q c e from o.
- Build E F L T I from H. Build C G Q D from O.
- a is two-story. f has a flat top arm and a foot slab. j hooks left on row 14.
- w and y have flat bottoms, for a more retro look.

# Small glyphs
- Small digits and letters (fractions, ª º, ² ⁿ) are 5 rows tall and 6 columns wide, with the same 2 px verticals and 1 px horizontals as full-size letters.
- Fractions use a diagonal layout: numerator top left, denominator bottom right, and a 2 px slash between them.
- ª and º sit on rows 2-6 with a 1 px underline on row 8.

# Telling glyphs apart
- 0 has a dot, 1 has a flag, l has a tail, I has slabs.
- Dots are 2x2. Comma tails reach into the descender rows.
