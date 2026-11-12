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

# Letter families
- Build h m r u from n. Build b d p q c e from o.
- Build E F L T I from H. Build C G Q D from O.
- a is two-story. f has a flat top arm and a foot slab. j hooks left on row 14.
- w and y have flat bottoms, for a more retro look.

# Small glyphs
- Small digits and letters (fractions, ª º, later ² ⁿ) are 5 rows tall and 6 columns wide, with the same 2 px verticals and 1 px horizontals as full-size letters.
- Fractions use a diagonal layout: numerator top left, denominator bottom right, and a 2 px slash between them.
- ª and º sit on rows 2-6 with a 1 px underline on row 8.

# Telling glyphs apart
- 0 has a dot, 1 has a flag, l has a tail, I has slabs.
- Dots are 2x2. Comma tails reach into the descender rows.
