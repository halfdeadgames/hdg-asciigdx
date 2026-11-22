/**
 * Layered drawing of terminal grids: a {@link com.github.halfdeadgames.asciigdx.layer.LayerStack}
 * holds ordered {@link com.github.halfdeadgames.asciigdx.layer.Layer}s, and each layer draws one
 * {@link com.github.halfdeadgames.asciigdx.grid.TerminalGrid} with one
 * {@link com.github.halfdeadgames.asciigdx.glyph.GlyphSource}. The package draws only; the game
 * owns the grid contents.
 *
 * <p>Main types:
 * <ul>
 *   <li>{@link com.github.halfdeadgames.asciigdx.layer.LayerStack}: the entry point. It orders
 *       layers (index 0 is the bottom), draws them into a caller-managed {@code SpriteBatch}, owns
 *       the renderers, and maps screen points back to cells.</li>
 *   <li>{@link com.github.halfdeadgames.asciigdx.layer.Layer}: one grid with its own offset, zoom,
 *       rotation, parallax, clip, tint, and visibility.</li>
 *   <li>{@link com.github.halfdeadgames.asciigdx.layer.Panel}: a pixel rectangle that clips its
 *       member layers and gives them their own camera.</li>
 *   <li>{@link com.github.halfdeadgames.asciigdx.layer.LayerGroup}: a set of layers that share
 *       {@link com.github.halfdeadgames.asciigdx.layer.ColorEffects}, such as a common tint or
 *       fade. Groups can nest.</li>
 *   <li>{@link com.github.halfdeadgames.asciigdx.layer.CellHandle}: per-cell motion and color
 *       overrides, such as an offset, rotation, or flash. A lasting handle (from
 *       {@code Layer.obtainCellHandle}) stays until released and suits display-owned animation. A
 *       frame handle (from {@code Layer.obtainFrameCellHandle}) is released automatically when it
 *       goes unused, and suits effects the game reapplies every update after
 *       {@link com.github.halfdeadgames.asciigdx.layer.LayerStack#beginCellEffects}.</li>
 *   <li>{@link com.github.halfdeadgames.asciigdx.layer.MapWindow}: shows a large game map through
 *       a smaller grid. It converts map coordinates to grid coordinates and reports which cells
 *       scrolled into view so the game fills only those.
 *       {@link com.github.halfdeadgames.asciigdx.layer.MapCellTarget} keeps tweens writing a map
 *       cell while it scrolls out of and back into the window.</li>
 *   <li>{@link com.github.halfdeadgames.asciigdx.layer.GridRenderer}: draws one grid into a batch
 *       without a stack. Most games use the stack instead.</li>
 * </ul>
 *
 * <p>Coordinates: layer, panel, and camera positions are in virtual pixels measured from the
 * stack's top-left corner with y growing downward, like grid rows. The position passed to
 * {@code LayerStack.draw} is a screen position with y growing upward. Rotation is in degrees,
 * positive clockwise. Map coordinates (used by a map window) and grid coordinates (cells of the
 * layer's grid) differ by the window's origin.
 *
 * <p>Camera zoom: {@code LayerStack.setCameraZoom} magnifies the scene about a focus point, and
 * each layer follows it by its zoom rate ({@code Layer.setZoomRate}, which by default is the
 * layer's horizontal parallax factor): the layer is scaled by {@code cameraZoom ^ rate}. Near
 * layers grow faster than far ones, together with the pan parallax. Layers in a {@code Panel} are
 * not zoomed. For example, {@code stack.setCameraZoom(2f)} then
 * {@code stack.setCameraZoomFocus(160f, 100f)} (stack pixels, y down).
 *
 * <p>Time: nothing here reads a clock. Durations and deltas are in seconds, passed in by the game.
 *
 * <p>Ownership: the stack disposes the renderers it creates. Grids and glyph sources belong to the
 * caller and are never disposed here.
 *
 * <p>Thread rule: nothing in this package is thread-safe. Draw and dispose on the GL (render)
 * thread.
 *
 * <p>Example:
 * <pre>{@code
 * // create (GL thread)
 * TerminalGrid grid = new TerminalGrid(80, 25);
 * TextureGlyphSource glyphs = TextureGlyphSource.loadSaiksistt();
 * LayerStack stack = new LayerStack();
 * Layer layer = stack.add(grid, glyphs);
 * grid.set(0, 0, '@', CellColor.rgb(0xFFFFFF), TerminalGrid.TRANSPARENT_RGBA);
 *
 * // each frame, with the batch begun
 * stack.draw(batch, 0, viewport.getWorldHeight());
 *
 * // on shutdown
 * stack.dispose();
 * glyphs.dispose();
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.layer;
