package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ManagedTextureDataTest {

    @BeforeAll
    static void loadNatives() {
        new SharedLibraryLoader().load("gdx");
    }

    @Test
    void textureDataIsManagedSoItCanBeReuploadedAfterContextLoss() {
        Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        try {
            PixmapTextureData data = TextureGlyphSource.createManagedTextureData(pixmap);

            assertTrue(data.isManaged());
        } finally {
            pixmap.dispose();
        }
    }

    @Test
    void textureDataLeavesThePixmapAliveAfterUpload() {
        Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        try {
            PixmapTextureData data = TextureGlyphSource.createManagedTextureData(pixmap);

            assertFalse(data.disposePixmap());
        } finally {
            pixmap.dispose();
        }
    }

    @Test
    void textureDataHandsBackTheSamePixmap() {
        Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        try {
            PixmapTextureData data = TextureGlyphSource.createManagedTextureData(pixmap);

            assertSame(pixmap, data.consumePixmap());
        } finally {
            pixmap.dispose();
        }
    }
}
