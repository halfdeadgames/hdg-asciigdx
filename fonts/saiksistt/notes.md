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
- Same widths, strokes, and letter shapes as 16x16. Only the heights change.
- Ascender row 6, capitals rows 8-23, x-height rows 13-23, descenders rows 24-29. Rows 0 and 31 stay blank as line spacing.
- Capitals center on rows 15-16, the middle of the cell, so symbols centered in the cell line up with text.
- Accented capitals keep full height. Accent marks sit on rows 3-5 over capitals and rows 8-10 over lowercase. Most marks are 3 rows tall; the diaeresis dots are 2.
- j has its dot on rows 9-10 and hooks left on row 29.
- Small digits and letters (² ⁿ) are 8 rows tall, on rows 8-15. ª and º sit on rows 8-15 with an underline on row 18. Fractions span rows 6-23.
- Row numbers in the other sections of this file are for 16x16.

# Glyph classes (16x32)
- Text: letters, digits, punctuation, accents, and symbols used inside a line of text (currency, math, Greek, fractions, « » ¿ ¡ ¶ § … × ≠ —). Drafts start from the 16x16 glyph, with straight stem rows repeated and columns unchanged. Diagonal and round glyphs that would stretch badly (k v w x y z, 2 4 7, and similar) get drawn by hand instead.
- Connecting: box lines, stubs, diagonals, blocks, eighths, shades, checkers, and Legacy Computing fills. Redrawn for tiling, never stretched: lines on columns 7-8 and rows 15-16, eighths 4 rows, halves 16 rows.
- Pictorial: faces, suits, icons, pictographs, chess, dice, shapes, and standalone arrows. Copied from 16x16 and moved to rows 8-23, centered in the cell. ◘ and ◙ fill the whole tall cell. Segmented digits stretch to rows 8-23 to match the text digits.

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

# Metrics (8x8)
- Ascender row 0, capitals rows 1-6, x-height rows 3-6, descenders row 7. Capitals sit in the middle of the cell.
- Letters are 6 columns wide, on columns 1-6. Columns 0 and 7 stay blank. M W m w æ Æ are 7 wide, with a 1 px middle stem.
- Same strokes as 16x16: 2 px verticals, 1 px horizontals. Round shapes cut 1 px off the outer corner, with no inner fill.
- Bars: capitals and digits on row 3 (B E H), except A P R on row 4. Lowercase bars on row 4.
- The glyph set matches 16x16, so one scene renders at any size. Lookalikes stay in, and some glyphs are identical at this size.
- Accent marks sit on rows 0-1 over lowercase, with row 2 blank. The diaeresis is two 2x1 dots on row 1, like the i dot. The ring is 3 rows and touches the letter in å and Å.
- Accented capitals use short capitals. A O U are 5 rows (rows 2-6), with the diaeresis moved up to row 0. E and N are 4 rows (rows 3-6), to leave a gap under the 2-row ´ and ˜, so É and Ñ are one row shorter than Ä Ö Ü.
- Row numbers in the other sections of this file are for 16x16.

# Glyph classes (8x8)
- Text: drawn by hand, keeping the 16x16 shapes where they fit.
  - Dots are 2x2 for . , : ; and 2x1 for i, j, and ?. The colon uses rows 2-3 and 5-6.
  - 0 has a slash, not a dot: a dot fills the 2 px counter and reads as 8.
  - 1 px strokes where 2 px would fill in: # @ ¶ × ∞ °, the small glyphs ª º ⁿ ² ½ ¼, the sides of φ and Φ, and the neck of Ω.
  - + and = have 6 px bars, wider than the 4 px -.
  - ? has a 2-row neck on rows 3-4. ¿ follows it.
  - π and Ω reach columns 0 and 7. The overhanging bar keeps π from reading as n.
- Connecting: single lines are 2 px on columns and rows 3-4. Doubles are 1 px at 2 and 5. Heavy lines are 4 px at 2-5, half the cell.
  - ╌ ╎ are dashes, 3 on and 1 off. ┈ ┊ are dotted, 1 on and 1 off.
  - Rounded corners cut 1 px. ╱ ╲ ╳ are 3 px wide, like 16x16. Box diagonals keep the 16x16 structure.
  - Blocks are exact. Shades keep the 16x16 1 px grain.
  - Checkers 🮕 🮖 use 2 px squares, since 1 px would match ▒. 🮘 🮙 use 2 px stripes every 4 px.
- Pictorial: drawn by hand, with the halved 16x16 glyph as reference only.
  - Small round symbols, notes, and faces use 1 px lines.
  - Full cell: faces, ♣ ♡ ★ ☆ ☠ ⚠ ⚔ ⛫ ◎ ☐ ☑ ☒, the crowns on ♛ ♕, 👑, and 🍎.
  - Single arrows are shortened to leave a gap to the next glyph.
  - Dice are 7x7, so the pips have 1 px gaps.
  - 🦴 lies flat. A diagonal bone read as 🍖. 🧪 has 1 px walls.
  - Stick figures reach columns 0 and 7. Segmented digits use rows 0-6.

# Metrics (8x16)
- Same 8 columns, letter widths, and strokes as 8x8. Only the heights change.
- Ascender row 3, capitals rows 4-11, x-height rows 7-11, descenders rows 12-14. Rows 0 and 15 stay blank as line spacing.
- Capitals center on rows 7-8, the middle of the cell, so symbols centered in the cell line up with text.
- Bars: B E F H on row 7, A P on row 8. Lowercase bars on row 9.
- Accented capitals keep full height. Accent marks sit on rows 1-2 over capitals and rows 4-5 over lowercase, like the i dot. The ring is 3 rows and touches the letter in Å.
- Row numbers in the other sections of this file are for 16x16.

# Glyph classes (8x16)
- Text: drafts start from the 8x8 glyph, with straight stem rows repeated. Diagonal and round glyphs that would stretch badly (k x z K M N Q and similar) get drawn by hand instead.
  - Choices driven by width stay from 8x8: 1 px strokes in # @, and 7-wide M W m w.
  - Choices driven by height are judged again. Dots are 2x2, including on i and j (rows 4-5). 0 keeps a slash, on rows 6-9: a dot still fills the 2 px counter.
  - Q has a 1-row tail on row 12.
- Connecting: rows mapped from 8x8. Single lines are 2 px on columns 3-4 and rows 7-8. Doubles are 1 px at columns 2 and 5 and rows 6 and 9. Heavy lines are columns 2-5 and rows 6-9. Eighths are 2 rows.
  - ╎ is 3 on and 1 off. ┊ is 1 on and 1 off, to match ╌ and ┈.
  - ╱ ╲ and the box diagonals are 2 px wide and step one column every 2 rows. Where a box diagonal ends at the middle of an edge, it fills both pixels of the box line it meets.
  - ◢ ◣ ◤ ◥ and the diagonal fills run corner to corner, 2 rows per column, the same as 16x32 at half size.
  - Shades, checkers, and the stripe fills 🮗 🮘 🮙 repeat the 8x8 pattern twice, so they tile like 8x8.
- Pictorial: copied from 8x8 to rows 4-11, centered in the cell.
  - ◘ and ◙ fill the whole tall cell.
  - Segmented digits span rows 4-11 to match the text digits. The middle bar is on row 7, so the upper segments are 2 rows and the lower ones 3.
