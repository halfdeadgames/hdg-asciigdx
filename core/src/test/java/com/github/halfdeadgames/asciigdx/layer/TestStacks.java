package com.github.halfdeadgames.asciigdx.layer;

/**
 * Builds stacks for tests with fake renderers and the real GL backends, which stay idle without a
 * GL context.
 */
final class TestStacks {

    private TestStacks() {
    }

    static LayerStack withRenderers(LayerRendererFactory rendererFactory) {
        return withRenderers(rendererFactory, new GlClipBackend());
    }

    static LayerStack withRenderers(LayerRendererFactory rendererFactory, ClipBackend clipBackend) {
        return new LayerStack(rendererFactory, clipBackend, new GlOffscreenBackend());
    }
}
