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
- Capitals with a center stroke or point use an odd width, 3, 5, or 7, so the center stays true.
- Wide capitals use the full 7 pixels.
- Capital bars sit on row 3, slightly above center.
- Bowls close on the capital bar.
- Capitals use row 7 only for details they need.

## Lowercase
- Must differ clearly from capitals in height AND width.
- Round and symmetric lowercase letters are 5 pixels wide, centered in the body.
- Wide lowercase letters use the full 7 pixels.
- Lowercase ascenders start on row 0, so they stand taller than capitals.
- Lowercase letters put at most one pixel on row 0. Row 0 ink can touch descenders on the line above; games that want space between lines add line spacing.
- Lowercase bars sit on row 4, the middle of the x-height.
- a and g are one-story.

## Digits
- 5 pixels wide, centered in the body.
- Digit middles sit on row 3, on the capital bar.

## Punctuation
- Must stay readable at 1x. Comma and period must differ.

## At 16 and above
- Slightly more rounding on curves.
- 1-pixel ink traps where strokes meet.
- Finer, rounder punctuation where it helps.
- No new features.
