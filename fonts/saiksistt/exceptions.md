# Saiksistt exceptions

Deliberate breaks and letter-specific details. The workbench rules panel does not show this file. Warnings still fire on these glyphs.

## Uppercase
- I has slabs at top and bottom.
- Q has a tail on row 7.
- Đ and Ł move one column right, so their stroke can cross the stem.
- Ħ is narrowed to columns 1 to 5, so its stroke on row 2 reaches past both stems. Its crossbar moves down to row 4, because on row 3 it would merge with the stroke.

## Lowercase
- a has a small tail at the bottom right so it never reads as o.
- f is shorter and starts on row 1. Its bar sits on row 3, leaving a blank row under its hook.
- t has its bar on row 2, its top.
- l has a tail curving right and a slab on row 0, which is accepted.
- Accented ascender letters (such as ĥ) are drawn by hand. See Accents for where the accent goes.

## Digits
- 0 has a center dot.
- 1 has a flag at the top and a base slab.
- 4 keeps its bar low, on row 5.

## Punctuation
- Comma (,) and semicolon (;) put their tail on row 7, the descender row, so they never read as a period or colon.
- Parentheses, square brackets, curly braces, and the bar (|) span rows 1 to 7, so they enclose descenders like g and y.
- Underscore (_) is a full-width line on row 7, the descender row, so it stays clear of letters on the baseline.
- The dollar sign ($) extends its stem onto rows 0 and 7, above and below its S-shaped body, so it never reads as S.
- The at sign (@) spans rows 1 to 7, like the brackets, so its open bowl has room for the small a inside.

## Accents
- The backtick (`) sits on rows 0-1, one row above the quote ('), because it doubles as the grave accent.
- The ring (˚) is an open arch on rows 0-1. å is drawn by hand: its ring is a small diamond on rows 0-2, with the corners cut so it reads as round, resting on the short a.
- Accented letters are built on short base letters stored at U+E8xx, where xx is the last two hex digits of the plain letter (short a is U+E861, short ı is U+E831, and short A is U+E841). These are drawing helpers, not meant to be typed.
- Capitals 6 pixels wide (C, E, G, N, O, R, S, U, Z) are centered between columns 2 and 3, so an odd-width mark cannot sit centered over them. They take even-width marks stored at U+E8xx, where xx is the last two hex digits of the plain mark: diaeresis U+E8A8 (dots on columns 1 and 4), macron U+E8AF (columns 1 to 4), circumflex U+E8C6, caron U+E8C7, breve U+E8D8, and tilde U+E8DC (both full letter width). The double acute is already 4 pixels wide, so those capitals shift it one column left instead. The dot above stays a single pixel, half a pixel off center.
- Ÿ uses the short Y with a blank row under the dots, because on the full Y the dots merge into its arms.
- Å is the ring (˚) over the short A. The open arch and the top of the A close into a ring.
- Letters with a mark below, such as ç, keep the full-height letter.
- Marks below sit where most fonts put them. The cedilla is centered under the letter or under its stem, and the ogonek hangs from the bottom right, or from the stem on į.
- On letters with an ascender, the accent sits beside the ascender, because there is no room above it. ĥ puts its circumflex to the right of the ascender, over a short bowl, and ĺ puts its acute at the upper right.
- ď, ľ, and ť write the caron as a short apostrophe to the right of the stem, as Czech and Slovak do. ď moves one column left so the apostrophe stays out of the gap column.
- ĵ is a dotless short j under the circumflex.
- ģ has a turned comma, a small 6 shape, above the short g, as Latvian does.

## Box drawing
- Single lines sit on column 3 and row 4, in line with |, +, -, and =. Each single-line glyph is an overlay of the four half-line stubs (╴ ╵ ╶ ╷), because mirroring or rotating in an 8-pixel cell moves the line by one pixel.
- Double-line corners and junctions (╔ ╗ ╚ ╝ ╠ ╣ ╦ ╩ ╬) are mirror images of each other but are each drawn by hand, for the same reason. Their lines sit on columns 2 and 4 and rows 3 and 5.
- Mixed single and double glyphs reuse those positions. Eight of them are drawn by hand (╒ ╕ ╘ ╛ ╤ ╧ ╟ ╢), because an overlay would ink the pixel between two double lines or push a stub out of the cell. In the corners, the single line meets the outer double line, so the corner closes.
