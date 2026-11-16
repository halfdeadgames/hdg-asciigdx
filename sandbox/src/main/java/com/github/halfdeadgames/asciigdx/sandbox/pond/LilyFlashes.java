package com.github.halfdeadgames.asciigdx.sandbox.pond;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.layer.CellHandle;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.layer.MapCellTarget;
import com.github.halfdeadgames.asciigdx.layer.MapWindow;
import com.github.halfdeadgames.asciigdx.tween.Timeline;
import com.github.halfdeadgames.asciigdx.tween.Tween;

/**
 * Short white flashes on the lilies of game cells the player just stepped into. Each flash is a
 * timeline built once at the step and written to the lilies' lasting handles; it ends at mix 0, so
 * the handles need nothing more afterward. A cell that scrolled out of the window loses its flash.
 */
public final class LilyFlashes {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int SLOTS_PER_AXIS = 2;
    private static final float START_AMOUNT = 0.6f;
    private static final float DURATION_SECONDS = 0.5f;

    private final LilySlot[] slots = LilySlot.values();
    private final PondMap map;
    private final Layer layer;
    private final MapWindow window;
    private final Array<Flash> flashes = new Array<>();

    public LilyFlashes(PondMap map, Layer layer) {
        this.map = map;
        this.layer = layer;
        this.window = layer.getMapWindow();
    }

    /**
     * Starts a flash on the game cell; a cell without lilies is ignored, and one already flashing
     * restarts.
     */
    public void start(int gameColumn, int gameRow, float nowSeconds) {
        if (!hasLilies(gameColumn, gameRow)) {
            return;
        }
        removeFlashOn(gameColumn, gameRow);
        flashes.add(new Flash(gameColumn, gameRow, nowSeconds + DURATION_SECONDS,
                buildTimeline(gameColumn, gameRow, nowSeconds)));
    }

    /**
     * Writes every flash for the time; a finished one gets its last write, which leaves mix 0, and
     * is dropped.
     */
    public void apply(float nowSeconds) {
        boolean anyFinished = false;
        for (int i = flashes.size - 1; i >= 0; i--) {
            Flash flash = flashes.get(i);
            flash.timeline.apply(nowSeconds);
            if (nowSeconds < flash.endSeconds) {
                continue;
            }
            flashes.removeIndex(i);
            anyFinished = true;
        }
        if (anyFinished) {
            layer.releaseIdleCellHandles();
        }
    }

    /** Forgets every flash and resets the lasting handles they wrote. */
    public void clear() {
        flashes.clear();
        layer.resetCellHandles();
        layer.releaseIdleCellHandles();
    }

    private Timeline buildTimeline(int gameColumn, int gameRow, float startSeconds) {
        PondCell gameCell = map.cell(gameColumn, gameRow);
        Timeline timeline = new Timeline();
        for (int southBit = 0; southBit < SLOTS_PER_AXIS; southBit++) {
            for (int eastBit = 0; eastBit < SLOTS_PER_AXIS; eastBit++) {
                if (!gameCell.hasLily(slots[southBit * SLOTS_PER_AXIS + eastBit])) {
                    continue;
                }
                int mapColumn = gameColumn * SLOTS_PER_AXIS + eastBit;
                int mapRow = gameRow * SLOTS_PER_AXIS + southBit;
                Tween fade = new Tween(startSeconds, DURATION_SECONDS, START_AMOUNT, 0f,
                        Interpolation.pow2Out);
                MapCellTarget target = window.createCellTarget(mapColumn, mapRow);
                timeline.bind(fade,
                        amount -> target.writeIfShown(LilyFlashes::setWhiteMix, amount));
            }
        }
        return timeline;
    }

    private static void setWhiteMix(CellHandle cell, float amount) {
        cell.setForegroundMix(WHITE, amount);
    }

    private boolean hasLilies(int gameColumn, int gameRow) {
        PondCell gameCell = map.cell(gameColumn, gameRow);
        for (LilySlot slot : slots) {
            if (gameCell.hasLily(slot)) {
                return true;
            }
        }
        return false;
    }

    private void removeFlashOn(int gameColumn, int gameRow) {
        for (int i = flashes.size - 1; i >= 0; i--) {
            Flash flash = flashes.get(i);
            if (flash.gameColumn == gameColumn && flash.gameRow == gameRow) {
                flashes.removeIndex(i);
            }
        }
    }

    private static final class Flash {
        final int gameColumn;
        final int gameRow;
        final float endSeconds;
        final Timeline timeline;

        Flash(int gameColumn, int gameRow, float endSeconds, Timeline timeline) {
            this.gameColumn = gameColumn;
            this.gameRow = gameRow;
            this.endSeconds = endSeconds;
            this.timeline = timeline;
        }
    }
}
