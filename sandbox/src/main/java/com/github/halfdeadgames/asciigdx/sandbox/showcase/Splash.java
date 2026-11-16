package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.layer.GlyphSprite;
import com.github.halfdeadgames.asciigdx.layer.Layer;

/** Drives the splash droplet sprites. */
public final class Splash {

    private static final int DROPLET_CODE_POINT = 0x00B7;
    private static final int DROPLET_COLOR = 0x8CB4E6FF;

    private final DropletSpray spray;
    private final GlyphSprite[] droplets;
    private final float centerX;
    private final float centerY;
    private final float cellWidth;
    private final float cellHeight;

    /** The center is the splash point in the droplet layer's pixels. */
    public Splash(Layer dropletLayer, float centerX, float centerY, float cellWidth,
            float cellHeight,
            DropletSpray spray) {
        this.spray = spray;
        this.centerX = centerX;
        this.centerY = centerY;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        droplets = new GlyphSprite[spray.count()];
        for (int index = 0; index < droplets.length; index++) {
            droplets[index] =
                    dropletLayer.addSprite(DROPLET_CODE_POINT, DROPLET_COLOR, centerX, centerY);
            droplets[index].setVisible(false);
        }
    }

    public void update(float timeSeconds) {
        boolean visible = spray.isVisibleAt(timeSeconds);
        float scale = spray.scaleAt(timeSeconds);
        float alpha = spray.alphaAt(timeSeconds);
        for (int index = 0; index < droplets.length; index++) {
            updateDroplet(droplets[index], index, timeSeconds, visible, scale, alpha);
        }
    }

    private void updateDroplet(GlyphSprite droplet, int index, float timeSeconds, boolean visible,
            float scale, float alpha) {
        droplet.setVisible(visible);
        if (!visible) {
            return;
        }
        float distance = spray.distanceCellsAt(index, timeSeconds);
        droplet.setPosition(
                centerX + spray.directionX(index) * distance * cellWidth - cellWidth / 2f,
                centerY + spray.directionY(index) * distance * cellHeight - cellHeight / 2f);
        droplet.setScale(scale);
        droplet.setAlpha(alpha);
    }
}
