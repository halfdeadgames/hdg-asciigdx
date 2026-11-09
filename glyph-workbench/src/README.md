# Font rules files

The workbench checks each glyph against rules that the font declares in its own files. No rule lives in the app code.

Status: work in progress. The workbench checks the selected glyph against the rules file as you draw and lists the rules in the rules panel (F1). FILE > RELOAD FONT FILES (F5) reads the rules, guides, and notes again after you edit them outside the workbench.

## Files

Each font folder holds:

| File | Purpose |
| --- | --- |
| `<width>x<height>.txt` | The glyphs, for example `16x16.txt`. The name must match the `cell` line inside the file. |
| `<width>x<height>.guides.txt` | Guide lines shown on the paint grid. |
| `<width>x<height>.rules.json` | Rules for this size, checked as you draw. |
| `glyphs.txt` | Optional. The glyph slots planned for every size of the font. |
| `notes.md` | Design notes for every size of the font. Never checked. |

Rules are per size because row and column numbers differ between sizes. Notes and the glyph list are per font because they apply to every size.

## glyphs.txt

One entry per line. Blank lines are skipped.

```text
cp437
U+2588
U+2190..U+2193
```

- `cp437` reserves the 256 Code Page 437 slots, in their standard positions, in the editor and in exported sheets.
- A code point or a range adds empty slots in the editor. They are exported once drawn.
- Without this file, the font has slots only for the glyphs drawn so far.
- A drawn glyph always has a slot, even if the list leaves it out.

GLYPH > NEW GLYPH adds its code point to this file as soon as you confirm, creating the file if needed. A font with no slots at all opens that prompt first. The file is read when a font opens, so reopen the font after editing it by hand.

## The rules panel

F1 opens the rules panel for the selected glyph. It lists, in order:

1. Rules that check this glyph. A rule the glyph breaks right now is shown in red.
2. Rules that skip this glyph through an exception, with the reason.
3. Rules that never check this glyph, under the heading `NOT FOR` and the glyph. Their text is grey, so they read as inactive.
4. The notes.

## notes.md

Markdown, shown whole at the bottom of the rules panel. The workbench never checks it. The notes are reminders for the designer.

The panel understands a small part of Markdown: a line starting with `#` is a heading, a line starting with `- ` or `* ` is a bullet, and a blank line separates paragraphs. Everything else, such as bold text or links, shows exactly as typed.

```markdown
# Style
Terminal feel, easy to read, boxy with a little roundness.

- Straight letters stay sharp; round shapes get soft corners.
- Judge glyphs at 1x, in words, code, and maps.
```

## The rules file

The file is JSON in lenient mode: keys and plain words need no quotes, and `//` and `/* */` comments are allowed. Always quote glyph tokens, since characters like `,`, `:`, and `-` break unquoted text.

```json
{
  groups: {
    capitals:   [ "A-Z" ],
    lowercase:  [ "a-z" ],
    digits:     [ "0-9" ],
    descenders: [ "g", "j", "p", "q", "y", ",", ";" ],
    tiling:     [ "U+2500-U+259F" ]
  },
  rules: [
    {
      name: "Blank ring",
      check: ring,
      size: 1,
      appliesTo: [ all ],
      except: [ { group: tiling, why: "joins its neighbours" } ]
    },
    {
      name: "Centered across",
      check: centeredX,
      appliesTo: [ capitals, lowercase, digits ]
    },
    {
      name: "Nothing below the baseline",
      check: noInkRows,
      rows: [ "12-15" ],
      appliesTo: [ capitals, lowercase, digits ],
      except: [ { group: descenders, why: "tails drop into rows 12-14" } ]
    }
  ]
}
```

### Groups

`groups` names lists of glyphs. Each list is an array of glyph tokens. Rules refer to groups by name. The name `all` is built in and means every glyph; a file cannot define its own `all`.

### Glyph tokens

| Token | Meaning |
| --- | --- |
| `"A"` | One character. `","`, `"-"`, and `" "` (space) work too. |
| `"U+0041"` | A Unicode code, 4 to 6 hex digits. `u+` works too. |
| `"A-Z"` | A range, first to last, both included. |
| `"U+2500-U+259F"` | A range written with codes. Ends may mix forms: `"A-U+005A"`. |

Tokens are not trimmed: `" "` is the space glyph, and `" A"` is an error.

Errors quote the token and say what is allowed, for example:
`"ZZ" is not a glyph: use one character like "A", a code like "U+0041", or a range like "A-Z"`.
A range whose first end is higher than its last is an error too.

### Rules

| Key | Required | Meaning |
| --- | --- | --- |
| `name` | yes | Short name shown in warnings and in the rules panel. Unique in the file. |
| `check` | yes | One of the checks below. |
| `appliesTo` | yes | Group names the rule covers. |
| `except` | no | Glyphs the rule skips. Each entry has a `group` name or a `glyphs` token list, plus a `why`. |
| `rows`, `columns`, `size` | per check | Settings for the check. |

Every exception needs a `why`. The rules panel shows it, so the reason for a break is never lost.

```json
except: [
  { group: descenders, why: "tails drop into rows 12-14" },
  { glyphs: [ "U+00B7" ], why: "middle dot is 1 px wide, so it cannot center exactly" }
]
```

Rows count from 0 at the top. Columns count from 0 at the left. `rows` and `columns` are arrays of numbers or `"first-last"` ranges: `[ 0, "12-15" ]`. A number may be quoted too, so `[ "0", "12-15" ]` means the same.

### Checks

| Check | Settings | Passes when |
| --- | --- | --- |
| `noInkRows` | `rows` | The listed rows have no ink. |
| `noInkColumns` | `columns` | The listed columns have no ink. A font that wants a letter gap declares it here. |
| `ring` | `size` | The outer `size` rows and columns on every side have no ink. |
| `centeredX` | none | The blank columns left of the ink equal the blank columns right of it. |
| `centeredBoth` | none | `centeredX` passes, and the blank rows above the ink equal the blank rows below it. |

An empty glyph passes every check.

Centering is exact. A glyph whose ink width cannot center in the cell (odd width in an even cell) needs an exception with a `why`.

### Errors

A file that cannot be read shows an error in the rules panel instead of the rules. A JSON syntax error names its line: `Line 3: the file is not valid JSON (...)`. Other errors name the group or rule and say what is wrong, such as a misspelled key, an unknown check, a group that is not defined, or a missing `why`:
`Rule "Blank ring": unknown key "apliesTo". Known keys: name, check, appliesTo, except, rows, columns, size`.
Unknown keys are errors, so a typo never turns a rule off without a warning.
A group defined twice is an error: `Group "capitals": defined twice`.
An empty `appliesTo` is an error: `Rule "x": appliesTo is empty, so the rule would never check anything`.
A setting the check does not use, or a missing one, is an error: `Rule "x": check noInkRows does not use "size"`.
A row or column outside the cell is an error: `Rule "x": row 16 is outside the 16x16 cell (rows 0-15)`.
A rule without a usable name is named by its place in the list, starting at 1: `Rule 3: missing "name"`.
