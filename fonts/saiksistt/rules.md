# Saiksistt

Saiksistt is the Blackfoot word for "clear to the vision or understanding." The goal: readable at every size.

Square cells. Clinical, modern terminal style. Shared rules: `fonts/FONT_RULES.md`.

## Core
- Sizes: 8 first, then 16. Later 24, 32, 40.
- Metrics at 8 (rows 0-7 top to bottom, columns 0-7 left to right):
  - Row 0: accents, the dots of i and j, and lowercase ascenders.
  - Capitals: rows 1-6 (rows 2-6 when accented).
  - Lowercase x-height: rows 2-6.
  - Baseline: bottom of row 6.
  - Descenders: row 7.
  - Body: columns 0-6. Gap: column 7.
- Sans serif. Small slabs only where a narrow letter needs them.
- Accents sit on row 0, on capitals and lowercase alike. Accented capitals shrink to rows 2-6, so row 1 stays blank under the accent. Each accent keeps one shape across the font.
- Round shapes have their corners cut by 1 pixel.
- No stroke contrast: all strokes are 1 unit.

## Uppercase
- 6 pixels wide by default (columns 0-5). Round capitals look wider than square ones, so 7 is too wide.
- Capitals with a center stroke or point (A T V X Y I) use an odd width, 3, 5, or 7, so the center stays true.
- Wide capitals (M W) use the full 7 pixels.
- Capital bars sit on row 3, slightly above center (A E F H, and any capital with a middle bar).
- Bowls of B, P, and R close on row 3, on the capital bar.
- I has slabs at top and bottom.
- Capitals use row 7 only for details they need, such as the tail of Q.

## Lowercase
- Must differ clearly from capitals in height AND width.
- Round and symmetric letters (c o s u v x z, and similar) are 5 pixels wide, centered in the body.
- Wide letters (m w) use the full 7 pixels.
- Lowercase ascenders (b d h k l) start on row 0, so they stand taller than capitals. f is shorter and starts on row 1.
- Lowercase letters put at most one pixel on row 0. The slab on l is an accepted exception. Row 0 ink can touch descenders on the line above; games that want space between lines add line spacing.
- Accented ascender letters (such as ĥ) are drawn by hand: ascender, a blank row, then the accent.
- Lowercase bars sit on row 4, the middle of the x-height (e f t).
- a: one-story, with a small tail at the bottom right so it never reads as o.
- g: one-story.
- l has a tail curving right.

## Digits
- 5 pixels wide, centered in the body.
- 0 has a center dot.
- 1 has a flag at the top and a base slab.
- Middles of 3, 5, 6, 8, and 9 sit on row 3, on the capital bar.
- 4 keeps its bar low, on row 5.

## Punctuation
- Must stay readable at 1x. Comma and period must differ.

## At 16 and above
- Slightly more rounding on curves.
- 1-pixel ink traps where strokes meet.
- Finer, rounder punctuation where it helps.
- No new features.
