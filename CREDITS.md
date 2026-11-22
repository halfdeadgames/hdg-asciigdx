# Credits

Third-party work used by HDG-AsciiGDX.

## Fonts

| Asset | Author | License | Location |
|---|---|---|---|
| Saiksistt | halfdeadgames | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | `core/src/main/resources/com/github/halfdeadgames/asciigdx/fonts/saiksistt/` |
| [dungeon.mode](https://datagoblin.itch.io/dungeonmode) | datagoblin | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | `core/src/test/resources/com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/` and `glyph-workbench/src/main/resources/com/github/halfdeadgames/asciigdx/workbench/ui/` |

Saiksistt is the bundled default font, drawn for this project. Its sources are `fonts/saiksistt/8x8.txt`, `fonts/saiksistt/16x16.txt`, `fonts/saiksistt/8x16.txt`, and `fonts/saiksistt/16x32.txt`.

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
| [Hyphenation patterns for American English](https://github.com/hyphenation/tex-hyphen) (`hyph-en-us.tex`) | Gerard D.C. Kuiken (copyright 1990, 2004, 2005) | "Copying and distribution of this file, with or without modification, are permitted in any medium without royalty provided the copyright notice and this notice are preserved." | Word hyphenation in `Hyphenator`. Shipped unchanged in the core jar (`core/src/main/resources/com/github/halfdeadgames/asciigdx/text/hyph-en-us.tex`) |
| [Moby Hyphenation List](https://www.gutenberg.org/ebooks/3204) | Grady Ward | Public domain, via Project Gutenberg | Test data only, never in the release jar. The full list is committed at `core/src/test/resources/com/github/halfdeadgames/asciigdx/text/mhyph.txt`. It measures where `Hyphenator` breaks words differently from a dictionary |

## Algorithms

| Algorithm | Author | License | Used for |
|---|---|---|---|
| [Oklab](https://bottosson.github.io/posts/oklab/) | Björn Ottosson | Public domain, or [MIT](https://opensource.org/licenses/MIT) | Color mixing in `CellColor.mixOklab` (conversion matrices from the 2021-01-25 update) |

## Source files

| File | Author | License | Used for |
|---|---|---|---|
| `StartupHelper.java`, based on a [java-gaming.org post](https://jvm-gaming.org/t/starting-jvm-on-mac-with-xstartonfirstthread-programmatically/57547) by kappa | damios (copyright 2020) | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Starting a new JVM when the desktop window needs one (macOS first thread, Windows user paths with non-ASCII characters, Linux with NVIDIA GPUs). Copies in `sandbox-desktop` and `skin-designer`; each file keeps its license notice |

## Development tools

Used only by the `sandbox` demo modules, the glyph workbench (the native folder dialog), the build (Checkstyle), the CPU benchmarks in `core/src/jmh` (JMH), and live code reload in the desktop sandbox and the skin designer (`:sandbox-desktop:runHotswap`, `:skin-designer:runHotswap`). Not part of any release artifact.

| Library | Author | License | Used for |
|---|---|---|---|
| [anim8-gdx](https://github.com/tommyettinger/anim8-gdx) | Tommy Ettinger | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Recording GIF captures of the demo |
| [sjInGameConsole](https://github.com/tommyettinger/sjInGameConsole) | StrongJoshua, Tommy Ettinger | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | In-game debug console |
| [Checkstyle](https://checkstyle.org/) | Checkstyle contributors | [LGPL 2.1 or later](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt) | Google Java Style reports for all modules (`styleCheck`) and code size and nesting reports for the glyph workbench; runs at build time only. `config/checkstyle/style.xml` is adapted from Checkstyle's bundled `google_checks.xml` (Checkstyle 10.21.4, LGPL 2.1 or later; authors Max Vetrenko, Mauryan Kansara, Ruslan Diachenko, and Roman Ivanov) |
| [JMH](https://github.com/openjdk/jmh) (Java Microbenchmark Harness) | Oracle and OpenJDK contributors | [GPL 2.0 with the Classpath Exception](https://openjdk.org/legal/gplv2+ce.html) | Timing CPU-side code (grid writes, dirty tracking, cell vertex building) in `core/src/jmh`; never on core's runtime classpath |
| [JMH Gradle plugin](https://github.com/melix/jmh-gradle-plugin) (`me.champeau.jmh`) | Cédric Champeau and contributors | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Building and running the JMH benchmarks (`:core:jmh`) |
| [LWJGL NFD bindings](https://www.lwjgl.org/) (`org.lwjgl:lwjgl-nfd`) | Ioannis Tsakpinis and LWJGL contributors; bundled native library [Native File Dialog Extended](https://github.com/btzy/nativefiledialog-extended) by Bernard Teo | [BSD 3-Clause](https://www.lwjgl.org/license); bundled native library under the [zlib License](https://github.com/btzy/nativefiledialog-extended/blob/master/LICENSE) | Folder dialog behind the glyph workbench's File > Export command |
| [LWJGL tinyfd bindings](https://www.lwjgl.org/) (`org.lwjgl:lwjgl-tinyfd`) | Ioannis Tsakpinis and LWJGL contributors; bundled native library [tiny file dialogs](https://sourceforge.net/projects/tinyfiledialogs/) by Guillaume Vareille | [BSD 3-Clause](https://www.lwjgl.org/license); bundled native library under the [zlib License](https://sourceforge.net/projects/tinyfiledialogs/) | Open and save file dialogs behind the skin designer's File menu |
| [JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime) | JetBrains and OpenJDK contributors | [GPL 2.0 with the Classpath Exception](https://openjdk.org/legal/gplv2+ce.html) | The JVM for `:sandbox-desktop:runHotswap` and `:skin-designer:runHotswap`; its enhanced class redefinition swaps in changed classes while the sandbox or the skin designer runs. Installed by each developer and set with the `hotswapJdk` Gradle property |
| [HotswapAgent](https://github.com/HotswapProjects/HotswapAgent) | HotswapProjects contributors | [GPL 2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html) | Watches the compiled class folders and reloads changed classes in `:sandbox-desktop:runHotswap` and `:skin-designer:runHotswap`; copied by each developer into the runtime's `lib/hotswap` folder |
