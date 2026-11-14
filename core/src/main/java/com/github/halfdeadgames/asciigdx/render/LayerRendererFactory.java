package com.github.halfdeadgames.asciigdx.render;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;
import java.util.function.Supplier;

/** Creates the renderer of a new {@link Layer}. */
interface LayerRendererFactory {

    /**
     * Creates a renderer for a layer. The supplier creates the stack's shared resources the
     * first time it is called; factories that do not need them can skip it.
     */
    LayerRenderer create(TerminalView grid, GlyphSource glyphSource, Supplier<GridRenderResources> sharedResources);

    /** Creates {@link GridRenderer}s that borrow the stack's shared resources. */
    LayerRendererFactory SHARED_GRID_RENDERERS =
        (grid, source, shared) -> new GridLayerRenderer(new GridRenderer(grid, source, shared.get()));
}
