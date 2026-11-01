package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import java.nio.IntBuffer;

final class GlClipBackend implements ClipBackend {

    private final IntBuffer viewportBuffer = BufferUtils.newIntBuffer(16);
    private final Rectangle logicalRect = new Rectangle();

    @Override
    public void flush(SpriteBatch batch) {
        batch.flush();
    }

    @Override
    public void getCombinedMatrix(SpriteBatch batch, Matrix4 out) {
        out.set(batch.getProjectionMatrix()).mul(batch.getTransformMatrix());
    }

    @Override
    public void getViewport(int[] out) {
        viewportBuffer.clear();
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, viewportBuffer);
        for (int i = 0; i < 4; i++) {
            out[i] = viewportBuffer.get(i);
        }
    }

    /**
     * ScissorStack works in logical coordinates and scales them to backbuffer pixels
     * itself. The rectangle arrives already rounded outward in backbuffer pixels, so
     * the min edge is truncated and the max edge is bumped up by one logical pixel
     * if truncation left it short.
     */
    @Override
    public boolean push(Rectangle windowRect) {
        int minX = (int) windowRect.x;
        int minY = (int) windowRect.y;
        int maxX = (int) (windowRect.x + windowRect.width);
        int maxY = (int) (windowRect.y + windowRect.height);
        int logicalMinX = HdpiUtils.toLogicalX(minX);
        int logicalMinY = HdpiUtils.toLogicalY(minY);
        int logicalMaxX = HdpiUtils.toLogicalX(maxX);
        int logicalMaxY = HdpiUtils.toLogicalY(maxY);
        if (HdpiUtils.toBackBufferX(logicalMaxX) < maxX) {
            logicalMaxX++;
        }
        if (HdpiUtils.toBackBufferY(logicalMaxY) < maxY) {
            logicalMaxY++;
        }
        logicalRect.set(logicalMinX, logicalMinY, logicalMaxX - logicalMinX, logicalMaxY - logicalMinY);
        return ScissorStack.pushScissors(logicalRect);
    }

    @Override
    public void pop() {
        ScissorStack.popScissors();
    }
}
