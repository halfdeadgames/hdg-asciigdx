# HDG-AsciiGDX

> **Work in progress.** This library is just getting started. The API changes often and nothing is released. It is not ready for anything beyond tinkering.

HDG-AsciiGDX is a display engine for [libGDX](https://libgdx.com/) games drawn with ASCII and other text glyphs, such as roguelikes. It only handles drawing: glyphs on a grid, layers, and visual effects. Game logic, input, and physics belong to your game.

It targets desktop (LWJGL3) and Android, with a Java 17 core.

## Current features

- Terminal grid: a grid of cells, each holding a glyph plus foreground and background colors. The grid tracks changed cells, and each renderer keeps its own record of what it still has to redraw.
- Grid renderer: draws a grid in two passes, backgrounds first and then glyphs. Cell data lives in GPU vertex buffers, and only changed cells are rewritten, so a mostly static screen costs almost nothing to redraw. Background and glyph passes are skipped when they have nothing to draw, and fully opaque backgrounds are drawn without blending, which saves GPU work on phones.
- Layers: stack several grids and draw them bottom to top in one call. Layers can differ in size and font, and each one has its own offset in pixels or cells, visibility, tint, and alpha. Moving, tinting, or fading a layer is cheap because none of it rewrites the layer's cells. Every visible layer is drawn every frame in stack order, whether or not its cells changed. A layer can be clipped to a rectangle or to the stack's area, and by default it may extend past the edges, which suits shakes and floating effects.
- Integer scaling: a libGDX `Scaling` that scales the view by whole numbers only, so pixel fonts stay crisp. Leftover space becomes letterbox bars. The scale never goes below 1x, so a view too large for the window is centered and cropped. To show more on a small screen, use a smaller font.
- Flexible grids: `GridFit` finds the largest whole-number scale that still shows a minimum number of columns and rows, and how many cells fit at that scale, so a game can fill the window instead of showing wide bars. `TerminalGrid.resize` changes a grid's size in place and keeps its contents, with an optional shift to add space on any side or keep content centered. Renderers and layers pick up the new size on their next draw.
- Glyph sheets: load a bitmap font sheet, describe its layout (cell size, margin, and spacing) and map its slots to Unicode characters. Code Page 437 order is built in.
- Character lists: a UTF-8 text file next to a sheet can list the character in each slot, one line per sheet row. Characters that are hard to type can be written as escapes such as `\u{E000}`.
- Transparency conversion: sheets without an alpha channel are converted on load. The background color (by default the top-left pixel) becomes transparent and the glyphs become white so they can be tinted any color. Anti-aliased edges stay smooth.
- Fallback glyph: characters missing from the sheet draw as `?`, or another glyph you choose.
- TrueType fonts: bake a TTF into a glyph sheet at load time with FreeType. Bitmap sheets are the default; TTF baking is there for sizes or characters a sheet doesn't cover. Pixel fonts stay sharp only at whole multiples of their design size.
- Bundled font: [dungeon.mode](https://datagoblin.itch.io/dungeonmode) by datagoblin (CC0) is the default font. It is extended with about 360 extra glyphs: quadrant blocks in several shade patterns, box-drawing pieces, arrows, geometric shapes, cards, chess pieces, dice, and meters for durability, health, and time. A UTF-8 text file next to the image lists the character in each slot, and you can write the same kind of file for your own sheets.
- Minimal example sheet: the original Code Page 437 sheet is also bundled (`TextureGlyphSource.loadDungeonMode437()`). It is a plain 16x16 image on a black background with no character list and no alpha channel. Use it as a reference for the least a hand-made sheet needs.

## Planned features

- Sub-terminals with their own coordinates
- Smooth camera pan and zoom, and parallax layers
- Glyphs that move freely off the grid, with rotation, scaling, fading, and screen shake
- Light and tint maps, palette swaps, and color grading
- Box drawing that connects automatically, 9-slice panels, and screen transitions
- Post-processing shaders, such as CRT and scanline effects
- Conversion from screen coordinates to grid coordinates for mouse and touch
- An optional Kotlin module with DSL builders and operator shortcuts

## Building

```
./gradlew build              # build everything
./gradlew :core:test         # run the unit tests
./gradlew :sandbox-desktop:run   # run the desktop demo
```

On Windows use `gradlew.bat`. The Android demo needs the Android SDK path set in `local.properties`.

## Sandbox

The sandbox is a demo app for checking features by eye. It opens to a menu of test scenes, and each scene exercises one area:

| Scene | What it shows |
| --- | --- |
| Renderer basics | Grid orientation, colors, transparent backgrounds, and box drawing |
| Layers | A pacing boss shakes the map under a frame, rain fades in and out, a popup slides in, and a character hops. R changes the layer order, V hides the rain, and P toggles pixel snapping. On Android, tap the left, middle, or right third. |
| Stress: full rewrite | A 240x135 grid rewritten every frame |
| Large grid: sparse updates | The same grid with a small share of cells changing each frame. L switches between 1, 2, and 4 layers. |
| Font comparison: PNG vs TTF | The bundled sheet and the baked TTF, swapped back and forth |
| Glyph sheet showcase | The whole extended sheet, plus cards, chess, dice, shades, and animated meters, some drawn in two colors across two layers |

```
./gradlew :sandbox-desktop:run                                 # desktop
./gradlew :sandbox-android:installDebug :sandbox-android:run   # Android
```

Desktop controls:

| Key | Action |
| --- | --- |
| Up / Down | Select a scene |
| Enter | Open the selected scene |
| Esc or Backspace | Return to the menu |
| F12 | Save a PNG screenshot |
| G | Start or stop a GIF recording (up to 120 frames) |

Screenshots and GIFs are saved to `assets/captures/`.

On Android, tap a scene to open it. Back returns to the menu, and Back on the menu exits.

While a scene is open, a small overlay shows FPS, frame time, and cell count.

### Capture mode

Capture mode runs the sandbox unattended with vsync off:

```
./gradlew :sandbox-desktop:run -Pcapture
```

It saves `menu.png`, one PNG per scene, GIFs of the animated scenes, and `perf.txt` (render time per scene as median, min, and max) to `build/captures/`, then exits. Options:

| Property | Effect |
| --- | --- |
| `-Pscenes=a,b` | Capture only these scene ids |
| `-Psamples=n` | Number of timing samples per scene (default 5) |
| `-PsampleSeconds=s` | Length of each sample in seconds (default 0.5) |
| `-Pdirty=0,25,50` | Sweep the sparse-update scene through these changed-cell percentages |
| `-Players=1,2,4` | Sweep the sparse-update scene through these layer counts |

In PowerShell, quote comma lists: `"-Pscenes=renderer-basics,glyph-showcase"`.

### Exporting glyph sheets

The `exportSheet` task bakes a font into a PNG sheet and a matching character list, without opening a window:

```
./gradlew :sandbox-desktop:exportSheet -Pfont=dungeon-mode -PcellHeight=16
```

Options are `-Pfont` (`dungeon-mode`, `dungeon-mode-inverted`, or a TTF path), `-PcellHeight`, `-Pchars` (`cp437` or a character list file), `-Pcolumns`, and `-PsheetName`. Output goes to `build/exports/`.

`-Passemble=true` rebuilds the bundled extended sheet. Copy the resulting `dungeon-extended.png` and `.txt` into `core/src/main/resources/com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/` by hand.

### Adding a scene

Implement `SandboxScene` and register it in `SandboxApp` with one line.

## Credits

See [CREDITS.md](CREDITS.md).

## License

MIT. See [LICENSE](LICENSE).
