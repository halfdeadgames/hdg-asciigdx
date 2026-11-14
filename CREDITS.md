# Credits

Third-party work used by HDG-AsciiGDX.

## Fonts

| Asset | Author | License | Location |
|---|---|---|---|
| Saiksistt | halfdeadgames | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | `core/src/main/resources/com/github/halfdeadgames/asciigdx/fonts/saiksistt/` |
| [dungeon.mode](https://datagoblin.itch.io/dungeonmode) | datagoblin | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | `core/src/test/resources/com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/` and `glyph-workbench/src/main/resources/com/github/halfdeadgames/asciigdx/workbench/ui/` |

Saiksistt is the bundled default font, drawn for this project. Its sources are `fonts/saiksistt/16x16.txt` and `fonts/saiksistt/16x32.txt`.

dungeon.mode is not shipped with the library. Its CP437 sheet and both TTFs (regular and inverted) are test fixtures for the TrueType baker tests. Its extended 8x8 sheet is the glyph workbench's interface font; the glyphs this project added to that sheet are also released under CC0 1.0. CC0 does not require credit, but it is given here with thanks.

## Libraries

Runtime dependencies of `core`. They are not bundled; Gradle downloads them.

| Library | Author | License | Used for |
|---|---|---|---|
| [libGDX](https://libgdx.com/) | libGDX contributors | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Rendering, textures, platform backends |
| [gdx-freetype](https://libgdx.com/wiki/extensions/gdx-freetype) | libGDX contributors | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0); bundled FreeType natives under the [FreeType License](https://freetype.org/license.html) | Rasterizing TTF fonts into glyph sheets |
| [colorful](https://github.com/tommyettinger/colorful-gdx) | Tommy Ettinger | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Color conversion |

## Data

| Data | Author | License | Used for |
|---|---|---|---|
| [Unicode Character Database](https://www.unicode.org/ucd/) | Unicode, Inc. | [Unicode License v3](https://www.unicode.org/license.txt) | Character names newer than the JDK's data, in the glyph workbench (`glyph-workbench/src/main/resources/unicode-names.txt`; the license text is kept in that file) |

## Algorithms

| Algorithm | Author | License | Used for |
|---|---|---|---|
| [Oklab](https://bottosson.github.io/posts/oklab/) | Björn Ottosson | Public domain, or [MIT](https://opensource.org/licenses/MIT) | Color mixing in `CellColor.mixOklab` (conversion matrices from the 2021-01-25 update) |

## Development tools

Used only by the `sandbox` demo modules and the glyph workbench (Checkstyle and the native folder dialog). Not part of any release artifact.

| Library | Author | License | Used for |
|---|---|---|---|
| [anim8-gdx](https://github.com/tommyettinger/anim8-gdx) | Tommy Ettinger | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Recording GIF captures of the demo |
| [sjInGameConsole](https://github.com/tommyettinger/sjInGameConsole) | StrongJoshua, Tommy Ettinger | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | In-game debug console |
| [Checkstyle](https://checkstyle.org/) | Checkstyle contributors | [LGPL 2.1 or later](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt) | Code size and nesting reports for the glyph workbench; runs at build time only |
| [LWJGL NFD bindings](https://www.lwjgl.org/) (`org.lwjgl:lwjgl-nfd`) | Ioannis Tsakpinis and LWJGL contributors; bundled native library [Native File Dialog Extended](https://github.com/btzy/nativefiledialog-extended) by Bernard Teo | [BSD 3-Clause](https://www.lwjgl.org/license); bundled native library under the [zlib License](https://github.com/btzy/nativefiledialog-extended/blob/master/LICENSE) | Folder dialog behind the glyph workbench's File > Export command |
