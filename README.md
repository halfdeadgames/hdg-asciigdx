# HDG-AsciiGDX

> **Work in progress.** This library is just getting started. The API changes often and nothing is released. It is not ready for anything beyond tinkering.

HDG-AsciiGDX is a display engine for [libGDX](https://libgdx.com/) games drawn with ASCII and other text glyphs, such as roguelikes. It only handles drawing: glyphs on a grid, layers, and visual effects. Game logic, input, and physics belong to your game.

It targets desktop (LWJGL3) and Android, with a Java 17 core.

## Current features

- Terminal grid: a grid of cells, each holding a glyph plus foreground and background colors. The grid tracks changed cells, and each renderer keeps its own record of what it still has to redraw.
- Grid renderer: draws each cell as one quad, with the shader placing the glyph over the cell's background in a single pass. Cell data lives in GPU vertex buffers, and only changed cells are rewritten, so a mostly static screen costs almost nothing to redraw. Fully opaque layers are drawn without blending, which saves GPU work on phones.
- Layers: stack several grids and draw them bottom to top in one call. Layers can differ in size and font, and each one has its own offset in pixels or cells, visibility, tint, alpha, saturation, and a mix toward any color, for flashes or grayscale. A faded layer fades as one flat image. Moving, tinting, or fading a layer is cheap because none of it rewrites the layer's cells. Every visible layer is drawn every frame in stack order, whether or not its cells changed. A layer can be clipped to a rectangle or to the stack's area, and by default it may extend past the edges, which suits shakes and floating effects.
- Sprite glyphs: glyphs that leave the grid, with rotation, scaling, mirroring, and their own alpha. Free sprites are placed anywhere on a layer, for projectiles or floating text. A grid cell's glyph can also be lifted with `Layer.cell(column, row)`: give it an offset, rotation, or scale and it becomes a sprite, while its background stays in the grid; set everything back and it settles into the grid again. Sprites belong to their layer, so they move with it and get its tint, fade, saturation, mix, and clip. The engine decides when a glyph needs to be a sprite, and layers without sprites pay nothing extra.
- Integer scaling: a libGDX `Scaling` that scales the view by whole numbers only, so pixel fonts stay crisp. Leftover space becomes letterbox bars. The scale never goes below 1x, so a view too large for the window is centered and cropped. To show more on a small screen, use a smaller font.
- Flexible grids: `GridFit` finds the largest whole-number scale that still shows a minimum number of columns and rows, and how many cells fit at that scale, so a game can fill the window instead of showing wide bars. `TerminalGrid.resize` changes a grid's size in place and keeps its contents, with an optional shift to add space on any side or keep content centered. Renderers and layers pick up the new size on their next draw.
- Font ladder: `FontFit` takes several fonts in your order of preference and returns the first one that fits the minimum columns and rows at 1x or more, along with its scale and cell count. `Layer.setGlyphSource` swaps a layer's font; the grid keeps its contents, and the layer redraws at the new cell size.
- Glyph sheets: load a bitmap font sheet, describe its layout (cell size, margin, and spacing) and map its slots to Unicode characters. Code Page 437 order is built in.
- Character lists: a UTF-8 text file next to a sheet can list the character in each slot, one line per sheet row. Characters that are hard to type can be written as escapes such as `\u{E000}`.
- Transparency conversion: sheets without an alpha channel are converted on load. The background color (by default the top-left pixel) becomes transparent and the glyphs become white so they can be tinted any color. Anti-aliased edges stay smooth.
- Fallback glyph: characters missing from the sheet draw as `?`, or another glyph you choose.
- TrueType fonts: bake a TTF into a glyph sheet at load time with FreeType. Bitmap sheets are the default; TTF baking is there for sizes or characters a sheet doesn't cover. Pixel fonts stay sharp only at whole multiples of their design size.
- Bundled font: Saiksistt, a pixel font drawn for this library and released under CC0, is the default font. It comes in three cell sizes: small square 8x8 cells for compact maps and UI (`loadSaiksistt(SaiksisttSize.SQUARE_8X8)`), square 16x16 cells for maps and gameplay (`TextureGlyphSource.loadSaiksistt()`), and tall 16x32 cells for long text (`loadSaiksistt(SaiksisttSize.TALL_16X32)`). Each size covers all of Code Page 437 plus light, heavy, double, and rounded box drawing, block elements and shades, arrows, geometric shapes, card suits, chess pieces, dice, and pictographs. A UTF-8 text file next to each image lists the character in each slot, and you can write the same kind of file for your own sheets. `SaiksisttSize` also gives the path of each sheet and its character list, so you can load them with your own fallback glyph or transparency mode.

## Planned features

- Sub-terminals with their own coordinates
- Smooth camera pan and zoom, and parallax layers
- Zoom for single layers, smooth or in whole steps
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
| Sprites | Glyphs off the grid on one map layer: an orbiting ring, a spinning glyph at double size, a mirrored glyph, a stream of short-lived sprites, and monsters that bump or wiggle out of their cells and settle back. G grays the layer, F flashes it white, P toggles pixel snapping, A fades the layer to half, M slides it, K shrinks the grid, and R settles every lifted glyph. On Android, tap the left, middle, or right third for G, F, or P. |
| Sprites: faded layer | The same scene with the layer at half alpha, so sprites and cells fade as one image |
| Stress: full rewrite | A 240x135 grid rewritten every frame |
| Large grid: sparse updates | The same grid with a small share of cells changing each frame. L switches between 1, 2, and 4 layers. |
| Glyph sheet showcase 8x8, 16x16, and 16x32 | Every glyph of the bundled font in that cell size, plus cards, chess, dice, shades, and animated meters, some drawn in two colors across two layers. Scroll with the arrow keys, the mouse wheel, or by dragging. |

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

The `exportSheet` task bakes a TTF into a glyph sheet PNG and a matching character list, without opening a window:

```
./gradlew :sandbox-desktop:exportSheet -Pfont=path/to/font.ttf -PcellHeight=16
```

`-Pfont` (a TTF path) is required. The other options are `-PcellHeight` (default 8), `-Pchars` (`cp437` or a character list file), `-PsheetName` (default: the TTF file name), `-Pcolumns` (default 16), and `-Pscale` (default 1), which enlarges the baked sheet by that whole factor. Output goes to `build/exports/`.

The bundled Saiksistt sheets are exported from `fonts/saiksistt/` with `./gradlew :glyph-workbench:exportFont -PfontSize=16x16` (the default), `-PfontSize=8x8`, or `-PfontSize=16x32`. Copy the resulting `saiksistt-<size>` `.png` and `.txt` from `build/exports/` into `core/src/main/resources/com/github/halfdeadgames/asciigdx/fonts/saiksistt/`. A workbench test fails if any copy in core is out of date.

### Adding a scene

Implement `SandboxScene` and register it in `SandboxApp` with one line.

## Credits

See [CREDITS.md](CREDITS.md).

## License

MIT. See [LICENSE](LICENSE).
