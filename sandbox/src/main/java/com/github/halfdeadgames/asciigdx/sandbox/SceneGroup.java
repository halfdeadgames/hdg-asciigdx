package com.github.halfdeadgames.asciigdx.sandbox;

/** Menu sections that scenes are listed under. Enum order is menu order. */
enum SceneGroup {
    BENCHMARKS("Benchmarks"),
    GLYPHS("Glyphs"),
    GRIDS_LAYERS_AND_SPRITES("Grids, layers, and sprites"),
    UI("UI"),
    DEMOS("Demos"),
    OTHER("Other");

    private final String title;

    SceneGroup(String title) {
        this.title = title;
    }

    String title() {
        return title;
    }
}
