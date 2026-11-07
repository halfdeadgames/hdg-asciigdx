# Saiksistt exceptions

Deliberate breaks and letter-specific details. The workbench rules panel does not show this file. Warnings still fire on these glyphs.

## Uppercase
- I has slabs at top and bottom.
- Q has a tail on row 7.

## Lowercase
- a has a small tail at the bottom right so it never reads as o.
- f is shorter and starts on row 1. Its bar sits on row 3, leaving a blank row under its hook.
- t has its bar on row 2, its top.
- l has a tail curving right and a slab on row 0, which is accepted.
- Accented ascender letters (such as ĥ) are drawn by hand: ascender, a blank row, then the accent.

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
- The ring (˚) is an open arch on rows 0-1. On å, the top of the letter closes it.

## Box drawing
- Single lines sit on column 3 and row 4, in line with |, +, -, and =. Each single-line glyph is an overlay of the four half-line stubs (╴ ╵ ╶ ╷), because mirroring or rotating in an 8-pixel cell moves the line by one pixel.
- Double-line corners and junctions (╔ ╗ ╚ ╝ ╠ ╣ ╦ ╩ ╬) are mirror images of each other but are each drawn by hand, for the same reason. Their lines sit on columns 2 and 4 and rows 3 and 5.
- Mixed single and double glyphs reuse those positions. Eight of them are drawn by hand (╒ ╕ ╘ ╛ ╤ ╧ ╟ ╢), because an overlay would ink the pixel between two double lines or push a stub out of the cell. In the corners, the single line meets the outer double line, so the corner closes.
