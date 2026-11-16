package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CellResolverTest {

    private static final int GRID_FOREGROUND = 0x112233FF;
    private static final int GRID_BACKGROUND = 0x445566CC;
    private static final int TARGET = 0xAABBCCFF;

    private final CellHandle handle = new CellHandle(1, 1);

    @Test
    void noHandleGivesTheGridValues() {
        assertEquals('a', CellResolver.codePoint('a', null));
        assertEquals(GRID_FOREGROUND, CellResolver.foreground(GRID_FOREGROUND, null));
        assertEquals(GRID_BACKGROUND, CellResolver.background(GRID_BACKGROUND, null));
    }

    @Test
    void handleWithoutOverridesGivesTheGridValues() {
        assertEquals('a', CellResolver.codePoint('a', handle));
        assertEquals(GRID_FOREGROUND, CellResolver.foreground(GRID_FOREGROUND, handle));
        assertEquals(GRID_BACKGROUND, CellResolver.background(GRID_BACKGROUND, handle));
    }

    @Test
    void foregroundAndBackgroundMixesApplyIndependently() {
        handle.setForegroundMix(TARGET, 1f);

        assertEquals((TARGET & 0xFFFFFF00) | (GRID_FOREGROUND & 0xFF),
                CellResolver.foreground(GRID_FOREGROUND, handle));
        assertEquals(GRID_BACKGROUND, CellResolver.background(GRID_BACKGROUND, handle));

        handle.setBackgroundMix(TARGET, 1f);

        assertEquals((TARGET & 0xFFFFFF00) | (GRID_BACKGROUND & 0xFF),
                CellResolver.background(GRID_BACKGROUND, handle));
    }

    @Test
    void codePointOverrideReplacesTheGridGlyphUntilCleared() {
        handle.setCodePoint('x');
        assertEquals('x', CellResolver.codePoint('a', handle));

        handle.setCodePoint(CellHandle.NO_CODE_POINT);
        assertEquals('a', CellResolver.codePoint('a', handle));
    }

    @Test
    void codePointOverrideCanBeASpace() {
        handle.setCodePoint(' ');

        assertEquals(' ', CellResolver.codePoint('a', handle));
    }

    @Test
    void detachedHandleIsIgnored() {
        handle.setForegroundMix(TARGET, 1f);
        handle.setBackgroundMix(TARGET, 1f);
        handle.setCodePoint('x');
        handle.detach();

        assertEquals('a', CellResolver.codePoint('a', handle));
        assertEquals(GRID_FOREGROUND, CellResolver.foreground(GRID_FOREGROUND, handle));
        assertEquals(GRID_BACKGROUND, CellResolver.background(GRID_BACKGROUND, handle));
    }
}
