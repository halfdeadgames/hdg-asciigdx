package com.github.halfdeadgames.asciigdx.render;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/** Creates the renderer of a new {@link Layer}. */
interface LayerRendererFactory {

    LayerRenderer create(TerminalView grid, GlyphSource glyphSource);

    /** Returns true if the factory wants shared GL resources, which the stack then creates lazily. */
    default boolean usesSharedResources() {
        return false;
    }

    /** Creates a renderer that uses the stack's shared resources. Called only if {@link #usesSharedResources()}. */
    default LayerRenderer create(GridRenderResources resources, TerminalView grid, GlyphSource glyphSource) {
        return create(grid, glyphSource);
    }

    /** Creates {@link GridRenderer}s that borrow the stack's shared resources. */
    LayerRendererFactory SHARED_GRID_RENDERERS = new LayerRendererFactory() {
        @Override
        public LayerRenderer create(TerminalView grid, GlyphSource glyphSource) {
            return new GridRenderer(grid, glyphSource);
        }

        @Override
        public boolean usesSharedResources() {
            return true;
        }

        @Override
        public LayerRenderer create(GridRenderResources resources, TerminalView grid, GlyphSource glyphSource) {
            return new GridRenderer(grid, glyphSource, resources);
        }
    };
}
