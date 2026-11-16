package com.github.halfdeadgames.asciigdx.render;

import com.github.halfdeadgames.asciigdx.color.CellColor;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/**
 * Holds everything temporary about one cell, for bump attacks, wiggles, hit pops, and flashes.
 * Handles come from {@link Layer#cell(int, int)}. The grid data is never modified.
 *
 * <p>Motion lifts the glyph off the grid so it can move, rotate, scale, and fade on its own. A
 * handle with default motion does nothing: the glyph stays in the grid. As soon as one motion
 * value differs (see {@link #isLifted()}), the glyph is drawn as a sprite instead. The cell's
 * background always stays in the grid. The lifted glyph shows the cell's current glyph and
 * foreground color every frame, so game changes to the cell follow it.
 *
 * <p>Temporary colors and a temporary glyph never lift a cell and are not considered by
 * {@link #isLifted()}. Flat and lifted cells show them the same way. The grid's current glyph and
 * colors are still followed underneath: a mix color is blended on top of whatever the grid holds
 * now, and a temporary glyph replaces the grid's glyph only while it is set. The order is the grid
 * value, then the handle overrides, then the layer effects (tint, mix, saturation, invert, alpha).
 *
 * <p>A detached handle keeps its getters working, but it draws nothing: no motion and no overrides.
 *
 * <p>There are two kinds of handle. A lasting handle ({@link Layer#cell}) stays until the game
 * releases it. A frame handle ({@link Layer#frameCell}) is for effects applied again every frame:
 * {@link LayerStack#beginCellEffects} resets it each frame and releases it after it stays unused for
 * a while. A cell can have both; they are separate objects and never change each other's values. A
 * frame handle has no pivot of its own; it uses the lasting handle's pivot.
 *
 * <p>When a cell has both, the cell is drawn with their effects together: offsets and rotations add,
 * scales and alpha multiply, a frame glyph wins over a lasting glyph, and the frame's
 * color mix is applied on top of the lasting one. Two mixes at once can differ from applying them
 * one after the other by 1 in a color channel, because they are combined into a single mix.
 *
 * <p>Handles are keyed by cell position. If {@link com.github.halfdeadgames.asciigdx.grid.TerminalGrid}
 * is resized with a content shift, handles do not move: the transform applies to whatever is now at
 * that position. {@link Layer#scroll} instead moves the handles together with the content.
 * Lifting and settling take effect at the next draw. Lifted cells draw before the
 * layer's free sprites, in the order their handles were created, and are positioned relative to the
 * layer like sprites.
 *
 * <p>Offsets are in virtual pixels with y growing downward, like layer offsets. Rotation is in
 * degrees, clockwise on screen. Values are not clamped, except alpha. Once the handle is
 * detached (the layer was removed, or the grid shrank so the cell no longer exists), getters keep
 * working but every mutator throws {@link IllegalStateException}; ask the layer for a new handle.
 */
public final class CellHandle implements SpriteTransform {

    private static final float FULL_TURN_DEGREES = 360f;

    /**
     * Marks "no temporary glyph". It is {@link Integer#MIN_VALUE} rather than -1 because grid
     * writes accept any int, so -1 could appear as real grid data.
     */
    public static final int NO_CODE_POINT = Integer.MIN_VALUE;

    private int column;
    private int row;
    private boolean detached;
    private boolean glyphHidden;
    private float offsetX;
    private float offsetY;
    private float rotation;
    private float scaleX = 1f;
    private float scaleY = 1f;
    private float pivotX = 0.5f;
    private float pivotY = 0.5f;
    private float alpha = 1f;
    private int foregroundMixColor;
    private float foregroundMixAmount;
    private int backgroundMixColor;
    private float backgroundMixAmount;
    private int overrideCodePoint = NO_CODE_POINT;
    private boolean overridesChanged;
    private float unusedSeconds;

    private final Layer owner;
    private final boolean frameHandle;

    CellHandle(int column, int row) {
        this(null, column, row, false);
    }

    CellHandle(Layer owner, int column, int row) {
        this(owner, column, row, false);
    }

    CellHandle(Layer owner, int column, int row, boolean frameHandle) {
        this.owner = owner;
        this.column = column;
        this.row = row;
        this.frameHandle = frameHandle;
    }

    /**
     * Whether this is a frame handle, from {@link Layer#frameCell(int, int)}, rather than a lasting
     * handle from {@link Layer#cell(int, int)}.
     */
    public boolean isFrameHandle() {
        return frameHandle;
    }

    void detach() {
        detached = true;
    }

    /** Seconds this handle has stayed idle, counted by {@link FrameHandleSweep}. */
    float getUnusedSeconds() {
        return unusedSeconds;
    }

    void addUnusedSeconds(float seconds) {
        unusedSeconds += seconds;
    }

    void clearUnusedSeconds() {
        unusedSeconds = 0f;
    }

    /** Moves the handle's position; owned by {@link Layer#scroll}, which keeps the layer's index in step. */
    void moveBy(int columns, int rows) {
        column += columns;
        row += rows;
    }

    /** Whether the renderer currently hides this cell's glyph; owned by the layer's sync. */
    boolean isGlyphHidden() {
        return glyphHidden;
    }

    void setGlyphHidden(boolean glyphHidden) {
        this.glyphHidden = glyphHidden;
    }

    private void requireAttached() {
        if (detached) {
            throw new IllegalStateException("Cell handle was detached from its layer");
        }
    }

    private void requireLastingHandleForPivot() {
        if (frameHandle) {
            throw new IllegalStateException(
                    "Frame handles use the lasting handle's pivot; set it on Layer.cell(column, row)");
        }
    }

    /** The cell's column. */
    public int getColumn() {
        return column;
    }

    /** The cell's row. */
    public int getRow() {
        return row;
    }

    /** Horizontal offset from the cell's own position, in virtual pixels. */
    public float getOffsetX() {
        return offsetX;
    }

    /** Vertical offset from the cell's own position, in virtual pixels, growing downward. */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Sets the offset from the cell's own position.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setOffset(float x, float y) {
        requireAttached();
        offsetX = x;
        offsetY = y;
    }

    /**
     * Moves the glyph relative to its current offset.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void offsetBy(float dx, float dy) {
        setOffset(offsetX + dx, offsetY + dy);
    }

    /** Rotation in degrees; positive is clockwise on screen. */
    @Override
    public float getRotation() {
        return rotation;
    }

    /**
     * Sets the rotation in degrees; positive is clockwise on screen.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setRotation(float degrees) {
        requireAttached();
        rotation = degrees;
    }

    /**
     * Adds to the rotation, in degrees.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void rotateBy(float degrees) {
        setRotation(rotation + degrees);
    }

    /** Horizontal scale; 1 is unscaled, negative mirrors. */
    @Override
    public float getScaleX() {
        return scaleX;
    }

    /** Vertical scale; 1 is unscaled, negative mirrors. */
    @Override
    public float getScaleY() {
        return scaleY;
    }

    /**
     * Sets the same scale on both axes.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setScale(float scale) {
        setScale(scale, scale);
    }

    /**
     * Sets the scale per axis; negative values mirror.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setScale(float scaleX, float scaleY) {
        requireAttached();
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Multiplies the scale on both axes by {@code factor}, so several effects can stack.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void scaleBy(float factor) {
        scaleBy(factor, factor);
    }

    /**
     * Multiplies the scale on each axis by its own factor; negative factors mirror.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void scaleBy(float factorX, float factorY) {
        setScale(scaleX * factorX, scaleY * factorY);
    }

    /**
     * Horizontal pivot as a fraction of the cell box: 0 is left, 1 is right. A frame handle has no
     * pivot of its own and always returns 0.5; the effective pivot is the lasting handle's.
     */
    @Override
    public float getPivotX() {
        return pivotX;
    }

    /**
     * Vertical pivot as a fraction of the cell box: 0 is top, 1 is bottom. A frame handle has no
     * pivot of its own and always returns 0.5; the effective pivot is the lasting handle's.
     */
    @Override
    public float getPivotY() {
        return pivotY;
    }

    /**
     * Sets the pivot for rotation and scale, as fractions of the cell box. Defaults to the center.
     * The pivot alone does not lift the glyph.
     *
     * @throws IllegalStateException if the handle was detached, or is a frame handle
     */
    public void setPivot(float pivotX, float pivotY) {
        requireAttached();
        requireLastingHandleForPivot();
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    /**
     * Moves the pivot and adjusts the offset so the glyph stays where it is drawn now. Placement is
     * kept for the rotation and scale set at the time of the call; later changes to either happen
     * about the new pivot. {@link #setPivot} alone moves the glyph when the cell is rotated or
     * scaled. Like the pivot, this does not lift the glyph: with default rotation and scale the
     * offset stays as it is.
     *
     * @param pivotX horizontal pivot as a fraction of the cell box
     * @param pivotY vertical pivot as a fraction of the cell box
     * @throws IllegalStateException if the handle was detached, or is a frame handle
     */
    public void setPivotKeepingPlacement(float pivotX, float pivotY) {
        requireAttached();
        requireLastingHandleForPivot();
        GlyphSource glyphSource = owner.getGlyphSource();
        float shiftX = (pivotX - this.pivotX) * glyphSource.getCellWidth();
        float shiftY = (pivotY - this.pivotY) * glyphSource.getCellHeight();
        float newOffsetX = SpritePlacement.xKeepingPlacement(offsetX, shiftX, shiftY, scaleX, scaleY, rotation);
        float newOffsetY = SpritePlacement.yKeepingPlacement(offsetY, shiftX, shiftY, scaleX, scaleY, rotation);
        setPivot(pivotX, pivotY);
        setOffset(newOffsetX, newOffsetY);
    }

    /** Multiplier on the glyph's alpha, from 0 to 1. */
    public float getAlphaMultiplier() {
        return alpha;
    }

    /**
     * Sets the multiplier applied to the glyph's own alpha, clamped to 0..1. Default 1.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void setAlphaMultiplier(float alpha) {
        requireAttached();
        this.alpha = Math.max(0f, Math.min(1f, alpha));
    }

    /**
     * Sets everything back to its default: the motion values ({@link #resetTransform()}) and the
     * temporary colors and glyph ({@link #clearOverrides()}).
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void reset() {
        resetTransform();
        clearOverrides();
    }

    /**
     * Sets the motion values back to their default: no offset, no rotation, scale 1, pivot at the
     * center, alpha 1. The glyph settles back into the grid. Temporary colors and glyph are kept.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void resetTransform() {
        requireAttached();
        offsetX = 0f;
        offsetY = 0f;
        rotation = 0f;
        scaleX = 1f;
        scaleY = 1f;
        pivotX = 0.5f;
        pivotY = 0.5f;
        alpha = 1f;
    }

    /**
     * Mixes the cell's foreground color toward {@code rgba}. The cell's alpha is kept. Does not
     * lift the glyph. The stored color is kept, and the getters return it, even when the amount is
     * 0.
     *
     * @param rgba target color as an RGBA8888 int, e.g. {@code 0xFF6E8AFF}; its alpha is ignored
     * @param amount 0 shows the cell's own color, 1 shows {@code rgba}; clamped to 0..1, NaN counts
     *     as 0
     * @throws IllegalStateException if the handle was detached
     */
    public void setForegroundMix(int rgba, float amount) {
        requireAttached();
        float clamped = clampAmount(amount);
        if (foregroundMixColor == rgba && foregroundMixAmount == clamped) {
            return;
        }
        foregroundMixColor = rgba;
        foregroundMixAmount = clamped;
        overridesChanged = true;
    }

    /**
     * Mixes the cell's background color toward {@code rgba}. The cell's alpha is kept. See
     * {@link #setForegroundMix(int, float)} for the color and amount rules.
     *
     * @param rgba target color as an RGBA8888 int, e.g. {@code 0xFF6E8AFF}; its alpha is ignored
     * @param amount 0 shows the cell's own color, 1 shows {@code rgba}; clamped to 0..1, NaN counts
     *     as 0
     * @throws IllegalStateException if the handle was detached
     */
    public void setBackgroundMix(int rgba, float amount) {
        requireAttached();
        float clamped = clampAmount(amount);
        if (backgroundMixColor == rgba && backgroundMixAmount == clamped) {
            return;
        }
        backgroundMixColor = rgba;
        backgroundMixAmount = clamped;
        overridesChanged = true;
    }

    /**
     * Mixes both the foreground and the background toward the same color by the same amount. The
     * color is an RGBA8888 int, e.g. {@code 0xFF6E8AFF}, unlike {@link ColorEffects#setMix(float, float,
     * float, float)}, which takes float channels or a {@code Color}.
     *
     * @throws IllegalStateException if the handle was detached
     * @see #setForegroundMix(int, float)
     * @see ColorEffects#setMix(float, float, float, float) the layer-level mix, which applies to the whole
     *     layer after cell-level overrides
     */
    public void setMix(int rgba, float amount) {
        setMix(rgba, amount, rgba, amount);
    }

    /**
     * Mixes the foreground and the background toward their own colors by their own amounts. Colors
     * are RGBA8888 ints, e.g. {@code 0xFF6E8AFF}, unlike {@link ColorEffects#setMix(float, float, float,
     * float)}, which takes float channels or a {@code Color}.
     *
     * @throws IllegalStateException if the handle was detached
     * @see #setForegroundMix(int, float)
     * @see ColorEffects#setMix(float, float, float, float) the layer-level mix, which applies to the whole
     *     layer after cell-level overrides
     */
    public void setMix(int foregroundRgba, float foregroundAmount,
            int backgroundRgba, float backgroundAmount) {
        setForegroundMix(foregroundRgba, foregroundAmount);
        setBackgroundMix(backgroundRgba, backgroundAmount);
    }

    /**
     * Sets both mix amounts to 0 and both mix colors to 0, so the cell shows its own colors.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void clearMix() {
        setMix(0, 0f);
    }

    /** Target color of the foreground mix, as RGBA8888; only visible while the amount is above 0. */
    public int getForegroundMixColor() {
        return foregroundMixColor;
    }

    /** Foreground mix amount, from 0 (none) to 1. */
    public float getForegroundMixAmount() {
        return foregroundMixAmount;
    }

    /** Target color of the background mix, as RGBA8888; only visible while the amount is above 0. */
    public int getBackgroundMixColor() {
        return backgroundMixColor;
    }

    /** Background mix amount, from 0 (none) to 1. */
    public float getBackgroundMixAmount() {
        return backgroundMixAmount;
    }

    /**
     * Shows {@code codePoint} instead of the cell's own glyph until {@link #clearCodePoint()}.
     * Does not lift the glyph. Like a grid write, any int is accepted; a value the font lacks shows
     * the glyph source's fallback. Passing {@link #NO_CODE_POINT} is the same as
     * {@link #clearCodePoint()}.
     *
     * @param codePoint glyph as a Unicode code point
     * @throws IllegalStateException if the handle was detached
     */
    public void setCodePoint(int codePoint) {
        requireAttached();
        if (codePoint == NO_CODE_POINT) {
            clearCodePoint();
            return;
        }
        if (overrideCodePoint == codePoint) {
            return;
        }
        overrideCodePoint = codePoint;
        overridesChanged = true;
    }

    /**
     * Goes back to showing the cell's own glyph.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void clearCodePoint() {
        requireAttached();
        if (overrideCodePoint == NO_CODE_POINT) {
            return;
        }
        overrideCodePoint = NO_CODE_POINT;
        overridesChanged = true;
    }

    /** Returns whether a temporary glyph is set. */
    public boolean hasCodePoint() {
        return overrideCodePoint != NO_CODE_POINT;
    }

    /** Returns the temporary glyph, or {@link #NO_CODE_POINT} if none is set. */
    public int getCodePoint() {
        return overrideCodePoint;
    }

    /**
     * Clears the temporary colors and glyph. Same as {@link #clearMix()} plus
     * {@link #clearCodePoint()}. Motion is kept.
     *
     * @throws IllegalStateException if the handle was detached
     */
    public void clearOverrides() {
        clearMix();
        clearCodePoint();
    }

    /** Returns whether any temporary color or glyph is set. */
    boolean hasOverrides() {
        return foregroundMixAmount > 0f || backgroundMixAmount > 0f || hasCodePoint();
    }

    /** The foreground to draw for a cell whose grid foreground is {@code cellForegroundRgba}. */
    int foregroundFor(int cellForegroundRgba) {
        if (foregroundMixAmount <= 0f) {
            return cellForegroundRgba;
        }
        return CellColor.mixRgbKeepingAlpha(cellForegroundRgba, foregroundMixColor, foregroundMixAmount);
    }

    /** The background to draw for a cell whose grid background is {@code cellBackgroundRgba}. */
    int backgroundFor(int cellBackgroundRgba) {
        if (backgroundMixAmount <= 0f) {
            return cellBackgroundRgba;
        }
        return CellColor.mixRgbKeepingAlpha(cellBackgroundRgba, backgroundMixColor, backgroundMixAmount);
    }

    /** The glyph to draw for a cell whose grid glyph is {@code cellCodePoint}. */
    int codePointFor(int cellCodePoint) {
        return hasCodePoint() ? overrideCodePoint : cellCodePoint;
    }

    /** Whether an override value changed since {@link #clearOverridesChanged()}. */
    boolean areOverridesChanged() {
        return overridesChanged;
    }

    void clearOverridesChanged() {
        overridesChanged = false;
    }

    /** Asks the layer's sync to rewrite this cell even though no override value changed. */
    void markOverridesChanged() {
        overridesChanged = true;
    }

    private static float clampAmount(float amount) {
        if (!(amount > 0f)) {
            return 0f;
        }
        return Math.min(1f, amount);
    }

    /**
     * Returns whether the glyph is drawn off the grid: any of offset, rotation (other than a
     * whole number of turns), scale, or alpha differs from its default. Temporary colors and
     * glyph are not considered. A detached handle keeps its values but is never drawn.
     */
    public boolean isLifted() {
        return offsetX != 0f || offsetY != 0f
                || rotation % FULL_TURN_DEGREES != 0f
                || scaleX != 1f || scaleY != 1f
                || alpha != 1f;
    }

    /** A handle is idle when it is not lifted, has no temporary glyph, and both mix amounts are 0. */
    boolean isIdle() {
        return !isLifted()
                && !hasCodePoint()
                && foregroundMixAmount == 0f
                && backgroundMixAmount == 0f;
    }

    /** Returns whether the handle was detached from its layer. */
    public boolean isDetached() {
        return detached;
    }
}
