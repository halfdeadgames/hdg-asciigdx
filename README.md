# HDG-AsciiGDX

> **Work in progress.** This library is just getting started. The API changes often and nothing is released. It is not ready for anything beyond tinkering.

HDG-AsciiGDX is a display engine for [libGDX](https://libgdx.com/) games drawn with ASCII and other text glyphs, such as roguelikes. It only handles drawing: glyphs on a grid, layers and visual effects. Game logic, input and physics belong to your game.

It targets desktop (LWJGL3) and Android, with a Java 17 core.

## Current features

- Terminal grid: a grid of cells, each holding a glyph plus foreground and background colors. The grid tracks changed cells so a renderer can redraw only what changed.
- Glyph sheets: load a bitmap font sheet, describe its layout (cell size, margin, spacing) and map its slots to Unicode characters. Code Page 437 order is built in.
- Transparency conversion: sheets without an alpha channel are converted on load. The background color (by default the top-left pixel) becomes transparent and the glyphs become white so they can be tinted any color. Anti-aliased edges stay smooth.
- Fallback glyph: characters missing from the sheet draw as `?`, or another glyph you choose.
- Bundled font: [dungeon.mode](https://datagoblin.itch.io/dungeonmode) by datagoblin (CC0) ships as the default font.

## Planned features

- A fast grid renderer with integer scaling and letterboxing
- TrueType fonts rendered to bitmap glyphs
- Stacked layers with transparency, and sub-terminals with their own coordinates
- Smooth camera pan and zoom, and parallax layers
- Glyphs that move freely off the grid, with rotation, scaling, fading and screen shake
- Light and tint maps, palette swaps and color grading
- Box drawing that connects automatically, 9-slice panels and screen transitions
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

## Credits

See [CREDITS.md](CREDITS.md).

## License

MIT. See [LICENSE](LICENSE).
