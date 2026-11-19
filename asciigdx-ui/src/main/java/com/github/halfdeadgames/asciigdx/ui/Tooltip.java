package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * A hint that appears next to the pointer after it has rested on a widget for a short time. The
 * hint is any widget; it is shown in the {@link UiRoot#getPopupLayer() popup layer} of a
 * {@link UiRoot} and removed again when the pointer leaves.
 *
 * <p>Behavior, for pointer 0 (the mouse or the first finger):
 * <ul>
 *   <li>The delay starts when the pointer enters the target, including any of its descendants. It
 *       is counted only from the time passed to {@link UiRoot#update(float)}, so a game that does
 *       not call update never shows a hint. A hint is shown only from an update, never inside an
 *       input event.
 *   <li>After the delay the content is packed and placed one cell right of and one cell below the
 *       last pointer cell. When it would reach past the right or bottom edge of the surface it is
 *       placed left of or above the pointer instead, then moved inside the surface. The row is
 *       rounded to a multiple of the content's {@link Widget#getRowSnap()}, so tall text sits on
 *       an even row. Placement happens once, when the hint shows; later size changes of the
 *       content are not corrected.
 *   <li>The hint stays where it appeared; it does not follow the pointer.
 *   <li>The hint hides when the pointer leaves the target, when the target is not in the tree of
 *       the root, and on {@link #detach()}. If the target returns to the tree while the pointer
 *       is still over it, the delay runs again.
 *   <li>A press with pointer 0 while the hint is showing hides it, and it stays hidden until the
 *       pointer has left the target and entered it again. A press before the hint shows restarts
 *       the delay. On a touch screen a long press therefore shows the hint, and lifting the
 *       finger hides it.
 *   <li>The hint is never a hit target and is never hovered: pointer events pass through it to
 *       whatever lies under it, so a hint placed over the pointer does not steal hover from the
 *       target.
 *   <li>A disabled target still shows its hint, because the tooltip reacts to hover only.
 * </ul>
 *
 * <p>While attached, a tooltip registers three things: a listener on the target, an update
 * listener on the root, and a press observer on the root. The target listener only observes and
 * never consumes an event, so the order of listeners does not matter and the target's own
 * listeners and its parents' listeners see every event as usual. The tooltip never calls input
 * methods of the root.
 *
 * <p>Content ownership: each tooltip needs its own content widget. The tooltip adds the content to
 * a hidden wrapper in the popup layer when it shows, sets its position when it shows, and removes
 * it when it hides. The game must not add the content to a tree, change its parent, or move it
 * while it is attached. If the content has a parent when the hint is due, the update call throws
 * the {@link IllegalArgumentException} of {@link WidgetGroup#addChild(Widget)} and no hint
 * shows. After {@link #detach()} the content has no parent and can be used again; the library
 * never disposes it.
 *
 * <p>Lifetime: the root keeps an attached tooltip reachable until {@link #detach()}, so call it
 * when the target is discarded. Attaching before the target is in the tree works; the hint waits
 * for it. Attaching twice to one target is allowed and gives two independent hints, each with its
 * own content.
 *
 * <p>Not thread-safe; use it from the render thread, like {@link UiRoot}. Hovering does not
 * allocate.
 *
 * <p>Example:
 * <pre>{@code
 * LabelStyle hintStyle = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x303030FF);
 * Tooltip hint = Tooltip.attachText(ui, startButton, "Begins a new game", hintStyle);
 * hint.setDelaySeconds(0.8f);
 * // ui.update(deltaSeconds) each frame drives the delay
 * hint.detach(); // when the button goes away for good
 * }</pre>
 */
public final class Tooltip {

    /** Delay before a hint shows, in seconds, for a new tooltip. */
    public static final float DEFAULT_DELAY_SECONDS = 0.5f;

    private static final int PRIMARY_POINTER = 0;

    private final UiRoot root;
    private final Widget target;
    private final Widget content;
    private final HintLayer hintLayer = new HintLayer();
    private final UiListener targetListener = this::observe;
    private final UiUpdateListener updateListener = this::advance;
    private final UiRoot.PressObserver pressObserver = this::onPress;
    private float delaySeconds = DEFAULT_DELAY_SECONDS;
    private float waitedSeconds;
    private int pointerColumn;
    private int pointerRow;
    private boolean pointerOverTarget;
    private boolean showing;
    private boolean hiddenByPress;
    private boolean detached;

    private Tooltip(UiRoot root, Widget target, Widget content) {
        this.root = root;
        this.target = target;
        this.content = content;
    }

    /** Group that holds the hint in the popup layer and lets every pointer event pass through. */
    private static final class HintLayer extends WidgetGroup {
        @Override
        public Widget hit(int column, int row) {
            return null;
        }
    }

    /**
     * Makes a widget show another widget as a hint while the pointer rests on it, with a delay of
     * {@link #DEFAULT_DELAY_SECONDS}. Adds a listener to {@code target} and an update listener and
     * a press observer to {@code root}; see the class description for ownership and lifetime.
     *
     * @param root root whose popup layer shows the hint and whose update drives the delay; must not
     *         be {@code null}
     * @param target widget that triggers the hint; must not be {@code null}; it may be added to
     *         the tree of {@code root} later
     * @param content widget shown as the hint; must not be {@code null} and must not have a parent
     *         when the hint is due
     * @return the new tooltip, attached
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Tooltip attach(UiRoot root, Widget target, Widget content) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(content, "content");
        Tooltip tooltip = new Tooltip(root, target, content);
        target.addListener(tooltip.targetListener);
        root.addUpdateListener(tooltip.updateListener);
        root.addPressObserver(tooltip.pressObserver);
        return tooltip;
    }

    /**
     * Makes a widget show a one-line text as a hint. The content is a new {@link Label} made with
     * {@code text} and {@code style}, as given: the style is immutable and is not copied.
     * {@link #getContent()} returns that label, typed as {@link Widget}. A newline is drawn like
     * any other character, not as a line break. Text wider than the surface is placed at the left
     * edge and cut off at the right edge of the surface. Otherwise it behaves like
     * {@link #attach(UiRoot, Widget, Widget)}.
     *
     * @param root root whose popup layer shows the hint and whose update drives the delay; must not
     *         be {@code null}
     * @param target widget that triggers the hint; must not be {@code null}; it may be added to
     *         the tree of {@code root} later
     * @param text hint text; must not be {@code null}; may be empty
     * @param style font and colors of the hint; must not be {@code null}
     * @return the new tooltip, attached
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Tooltip attachText(UiRoot root, Widget target, String text, LabelStyle style) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
        return attach(root, target, new Label(text, style));
    }

    /**
     * Stops hinting this target and hides the hint if it is showing. Removes everything that
     * {@link #attach} registered. Calling it again does nothing. A detached tooltip cannot be
     * attached again; create a new one. After this call {@link #isShowing()} is {@code false}, the
     * getters keep returning their values, and {@link #setDelaySeconds(float)} is accepted but has
     * no effect. It may be called from any callback on the render thread, including a listener.
     */
    public void detach() {
        if (detached) {
            return;
        }
        detached = true;
        hide();
        pointerOverTarget = false;
        target.removeListener(targetListener);
        root.removeUpdateListener(updateListener);
        root.removePressObserver(pressObserver);
    }

    /**
     * Sets how long the pointer must rest on the target before the hint shows. The time already
     * waited is kept, so lowering the delay can show the hint on the next update. With 0 the hint
     * shows on the first update after the pointer enters the target, never inside the event
     * itself. A showing hint is not affected.
     *
     * @param seconds delay in seconds; zero or greater, finite or infinite (infinite never shows)
     * @throws IllegalArgumentException if {@code seconds} is negative or NaN
     */
    public void setDelaySeconds(float seconds) {
        if (!(seconds >= 0f)) {
            throw new IllegalArgumentException(
                    "Delay must not be negative or NaN, was " + seconds);
        }
        delaySeconds = seconds;
    }

    /**
     * Returns the delay before the hint shows.
     *
     * @return the delay in seconds, zero or greater
     */
    public float getDelaySeconds() {
        return delaySeconds;
    }

    /**
     * Returns whether the hint is currently in the popup layer.
     *
     * @return {@code true} while the hint shows
     */
    public boolean isShowing() {
        return showing;
    }

    /**
     * Returns the widget that triggers the hint.
     *
     * @return the target, never {@code null}
     */
    public Widget getTarget() {
        return target;
    }

    /**
     * Returns the widget shown as the hint.
     *
     * @return the content, never {@code null}
     */
    public Widget getContent() {
        return content;
    }

    private boolean observe(UiEvent event) {
        switch (event.getType()) {
            case POINTER_ENTERED -> enterTarget(event);
            case POINTER_EXITED -> leaveTarget();
            case POINTER_MOVED -> rememberPointer(event);
            default -> {
            }
        }
        return false;
    }

    private void enterTarget(UiEvent event) {
        pointerOverTarget = true;
        hiddenByPress = false;
        waitedSeconds = 0f;
        rememberPointer(event);
    }

    private void leaveTarget() {
        pointerOverTarget = false;
        hiddenByPress = false;
        waitedSeconds = 0f;
        hide();
    }

    private void rememberPointer(UiEvent event) {
        if (showing) {
            return;
        }
        pointerColumn = event.getSurfaceColumn();
        pointerRow = event.getSurfaceRow();
    }

    private void onPress(int pointer) {
        if (pointer != PRIMARY_POINTER || !pointerOverTarget) {
            return;
        }
        if (showing) {
            hide();
            hiddenByPress = true;
            return;
        }
        waitedSeconds = 0f;
    }

    private void advance(float deltaSeconds) {
        if (!pointerOverTarget) {
            return;
        }
        if (!isTargetInTree()) {
            hide();
            waitedSeconds = 0f;
            return;
        }
        if (showing || hiddenByPress) {
            return;
        }
        waitedSeconds += deltaSeconds;
        if (waitedSeconds >= delaySeconds) {
            show();
        }
    }

    private boolean isTargetInTree() {
        Widget top = target;
        while (top.getParent() != null) {
            top = top.getParent();
        }
        return top == root.getRoot() || top == root.getPopupLayer();
    }

    private void show() {
        hintLayer.addChild(content);
        root.getPopupLayer().addChild(hintLayer);
        content.pack();
        placeContent();
        showing = true;
    }

    private void hide() {
        if (!showing) {
            return;
        }
        showing = false;
        if (hintLayer.getParent() == root.getPopupLayer()) {
            root.getPopupLayer().removeChild(hintLayer);
        }
        if (content.getParent() == hintLayer) {
            hintLayer.removeChild(content);
        }
    }

    private void placeContent() {
        int surfaceColumns = root.getSurfaceColumns();
        int surfaceRows = root.getSurfaceRows();
        int width = content.getWidth();
        int height = content.getHeight();
        int snap = content.getRowSnap();

        int column = pointerColumn + 1;
        if (column + width > surfaceColumns) {
            column = pointerColumn - width;
        }
        column = Math.max(0, Math.min(column, surfaceColumns - width));

        int row = roundUpToMultiple(pointerRow + 1, snap);
        if (row + height > surfaceRows) {
            row = roundDownToMultiple(pointerRow - height, snap);
        }
        row = roundDownToMultiple(Math.min(row, surfaceRows - height), snap);
        row = Math.max(0, row);

        content.setPosition(column, row);
    }

    private static int roundUpToMultiple(int value, int multiple) {
        return -Math.floorDiv(-value, multiple) * multiple;
    }

    private static int roundDownToMultiple(int value, int multiple) {
        return Math.floorDiv(value, multiple) * multiple;
    }
}
