package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * A modal {@link Window} that asks the user something and reports the answer as a
 * {@link DialogChoice}. While a dialog is shown, input reaches only the dialog (and popups): a
 * press anywhere else is swallowed, widgets outside it are never hovered, Tab and the arrow keys
 * stay inside it, and menu shortcuts do not work. Everything under it can be dimmed (see
 * {@link DialogStyle#withDim(int, float)}). Use {@link Dialogs} for the common dialogs, or build
 * one by hand.
 *
 * <p>Layout: the interior holds the content area on top and a centered row of buttons below it.
 * {@link #setContent(Widget)}, {@link #getContent()}, {@link #hasContent()}, and
 * {@link #clearContent()} work on the content area only, so the buttons stay. Buttons come from
 * {@link #addButton(String, DialogChoice)} and {@link #addCustomButton(TextButton)}, in
 * left-to-right order. Add content and buttons before {@link #show(UiRoot)}; the dialog is sized
 * when it is shown.
 *
 * <p>Styles: {@link #getStyle()} returns the window part of the style, and
 * {@link #getDialogStyle()} returns the whole dialog style.
 *
 * <p>Choosing: a click on a button made with {@code addButton(String, DialogChoice)}, and
 * {@link #choose(DialogChoice)}, report a choice. Escape (unless {@link #setCloseOnEscape} turned
 * it off) and the window's close button report the {@linkplain #getBackOutChoice() back-out
 * choice}. Enter on a focused button clicks it, as for any button. On a choice, the dialog first
 * closes (unless {@link #setCloseOnChoice(boolean)} turned that off): the modal state ends and the
 * focus from before {@code show} is restored. Then the {@link ChoiceListener}s run, in the order
 * they were added.
 *
 * <p>Escape: unlike a plain {@link Window}, a dialog reacts to Escape by default. See
 * {@link #setCloseOnEscape(boolean)}.
 *
 * <p>Focus: {@link #show(UiRoot)} focuses the first button, or the widget chosen with
 * {@link #setInitialFocus(Widget)}.
 *
 * <p>Calling {@link #close()} while the dialog is showing is the same as choosing the back-out
 * choice. To hide a dialog without reporting anything, call {@link #dismiss()}.
 *
 * <p>Example:
 * <pre>{@code
 * Dialog dialog = new Dialog("Quit", style, DialogChoice.NO);
 * dialog.setContent(new Label("Really quit?", messageLook));
 * dialog.addButton("Yes", DialogChoice.YES);
 * dialog.addButton("No", DialogChoice.NO);
 * dialog.addChoiceListener((answered, choice) -> {
 *     if (choice == DialogChoice.YES) {
 *         game.quit();
 *     }
 * });
 * dialog.show(ui);
 * }</pre>
 */
public class Dialog extends Window {

    private static final ChoiceListener[] NO_CHOICE_LISTENERS = new ChoiceListener[0];
    private static final int NO_DIM_RGBA = 0x000000FF;
    private static final int GAP_BELOW_CONTENT = 1;
    private static final int GAP_BETWEEN_BUTTONS = 1;

    private final DialogStyle dialogStyle;
    private final DialogChoice backOutChoice;
    private final Table layoutTable = new Table();
    private final Table buttonRow = new Table();
    private final ArrayList<TextButton> buttons = new ArrayList<>();
    private Widget content;
    private Widget initialFocus;
    private ChoiceListener[] choiceListeners = NO_CHOICE_LISTENERS;
    private ChoiceListener finalChoiceListener;
    private boolean closeOnChoice = true;
    private boolean closeOnEscape = true;
    private UiRoot shownRoot;

    /**
     * Creates a dialog with no content and no buttons, at position (0, 0), not shown. The dialog
     * reports {@code backOutChoice} for Escape (see {@link #setCloseOnEscape(boolean)}, on by
     * default) and for the window's close button.
     *
     * @param title text for the top border; must not be {@code null}; may be empty
     * @param style looks of the dialog; must not be {@code null}
     * @param backOutChoice the choice reported when the user presses Escape or uses the window's
     *         close button; must not be {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public Dialog(String title, DialogStyle style, DialogChoice backOutChoice) {
        super(title, Objects.requireNonNull(style, "style").getWindow());
        this.dialogStyle = style;
        this.backOutChoice = Objects.requireNonNull(backOutChoice, "backOutChoice");
        super.setContent(layoutTable);
        rebuildLayout();
    }

    /**
     * Returns the looks of the dialog. {@link #getStyle()} returns only its window part.
     *
     * @return the dialog style, never {@code null}
     */
    public DialogStyle getDialogStyle() {
        return dialogStyle;
    }

    /**
     * Returns the choice reported for Escape and for the window's close button.
     *
     * @return the back-out choice, never {@code null}
     */
    public DialogChoice getBackOutChoice() {
        return backOutChoice;
    }

    /**
     * Returns whether the content area has a widget. The buttons are not part of the content
     * area.
     *
     * @return {@code true} after {@link #setContent} and before {@link #clearContent()}
     */
    @Override
    public boolean hasContent() {
        return content != null;
    }

    /**
     * Returns the widget in the content area, above the buttons.
     *
     * @return the content, never {@code null}
     * @throws IllegalStateException if the content area is empty; check {@link #hasContent()}
     */
    @Override
    public Widget getContent() {
        if (content == null) {
            throw new IllegalStateException("The dialog has no content");
        }
        return content;
    }

    /**
     * Sets the widget of the content area above the buttons, which replaces the previous one.
     * The previous content loses its parent. Call it before {@link #show(UiRoot)}.
     *
     * @param content widget to show above the buttons; must not be {@code null}, and must have
     *         no parent
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code content} already has a parent, or is this
     *         dialog or one of its ancestors; the dialog is unchanged then
     */
    @Override
    public void setContent(Widget content) {
        Objects.requireNonNull(content, "content");
        if (content == this.content) {
            return;
        }
        requireAttachable(content);
        this.content = content;
        rebuildLayout();
    }

    /**
     * Removes the widget of the content area, if any. It loses its parent. The buttons stay.
     * Does nothing when the content area is empty.
     */
    @Override
    public void clearContent() {
        if (content == null) {
            return;
        }
        content = null;
        rebuildLayout();
    }

    private void requireAttachable(Widget candidate) {
        if (candidate.getParent() != null) {
            throw new IllegalArgumentException("Widget already has a parent");
        }
        for (Widget current = this; current != null; current = current.getParent()) {
            if (current == candidate) {
                throw new IllegalArgumentException(
                        "A group cannot contain itself or its ancestors");
            }
        }
    }

    // The table holds the content row (if any) and the button row, rebuilt in that order.
    private void rebuildLayout() {
        while (layoutTable.getChildCount() > 0) {
            layoutTable.removeChild(layoutTable.getChild(0));
        }
        if (content != null) {
            layoutTable.add(content).expandX().padBottom(GAP_BELOW_CONTENT);
            layoutTable.row();
        }
        layoutTable.add(buttonRow).expandX().alignX(HorizontalAlignment.CENTER);
    }

    /**
     * Adds a button with the dialog's button style at the right end of the button row. Clicking
     * it, or pressing Enter while it has focus, calls {@link #choose(DialogChoice)} with
     * {@code choice}. Call it before {@link #show(UiRoot)}.
     *
     * @param text text of the button; must not be {@code null}
     * @param choice the choice the button reports; must not be {@code null}
     * @return the new button, for example to disable it; it belongs to the dialog
     * @throws NullPointerException if an argument is {@code null}
     */
    public TextButton addButton(String text, DialogChoice choice) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(choice, "choice");
        TextButton button = new TextButton(text, dialogStyle.getButtons());
        button.addClickListener(clicked -> choose(choice));
        addCustomButton(button);
        return button;
    }

    /**
     * Adds a custom button at the right end of the button row. Clicking it does nothing by
     * itself: add your own click listener, which can call {@link #choose(DialogChoice)} or
     * {@link #dismiss()}. Call it before {@link #show(UiRoot)}.
     *
     * @param button button to add; must not be {@code null}, and must have no parent
     * @throws NullPointerException if {@code button} is {@code null}
     * @throws IllegalArgumentException if {@code button} already has a parent
     */
    public void addCustomButton(TextButton button) {
        Objects.requireNonNull(button, "button");
        TableCell cell = buttonRow.add(button);
        if (!buttons.isEmpty()) {
            cell.padLeft(GAP_BETWEEN_BUTTONS);
        }
        buttons.add(button);
    }

    /**
     * Adds a listener that is called after every choice, in the order the listeners were added.
     * Safe to call from inside a listener; the change applies from the next choice.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addChoiceListener(ChoiceListener listener) {
        Objects.requireNonNull(listener, "listener");
        ChoiceListener[] grown = Arrays.copyOf(choiceListeners, choiceListeners.length + 1);
        grown[choiceListeners.length] = listener;
        choiceListeners = grown;
    }

    /**
     * Removes the first occurrence of a choice listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeChoiceListener(ChoiceListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(choiceListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        ChoiceListener[] shrunk = new ChoiceListener[choiceListeners.length - 1];
        System.arraycopy(choiceListeners, 0, shrunk, 0, index);
        System.arraycopy(choiceListeners, index + 1, shrunk, index, shrunk.length - index);
        choiceListeners = shrunk.length == 0 ? NO_CHOICE_LISTENERS : shrunk;
    }

    /**
     * Returns whether a choice closes the dialog.
     *
     * @return {@code true} when it does; {@code true} for a new dialog
     */
    public boolean isCloseOnChoice() {
        return closeOnChoice;
    }

    /**
     * Sets whether a choice closes the dialog before the listeners run. Turn it off for a dialog
     * that checks the answer, such as a text input that rejects empty text: the listener then
     * calls {@link #dismiss()} itself when it accepts. Escape and the close button also report a
     * choice and also keep the dialog open then.
     *
     * @param closeOnChoice {@code true} to close on every choice
     */
    public void setCloseOnChoice(boolean closeOnChoice) {
        this.closeOnChoice = closeOnChoice;
    }

    /**
     * Sets the widget that gets keyboard focus when the dialog is shown. {@code null}, the
     * default, means the first button. A widget that is not inside the dialog when
     * {@link #show(UiRoot)} runs also falls back to the first button.
     *
     * @param widgetOrNull the widget to focus, or {@code null} for the first button
     */
    public void setInitialFocus(Widget widgetOrNull) {
        this.initialFocus = widgetOrNull;
    }

    /**
     * Returns whether the dialog is currently shown through {@link #show(UiRoot)}. It turns
     * {@code false} again when the dialog closes in any way, also when it is removed from the
     * window layer by hand.
     *
     * @return {@code true} while the dialog is shown
     */
    public boolean isShowing() {
        return shownRoot != null && getParent() == shownRoot.getWindowLayer();
    }

    /**
     * Shows the dialog as a modal on a root. The dialog is sized to fit its content, buttons, and
     * title, and centered on the surface, but not above the top or past the bottom. The user can
     * still drag it afterwards. Then the root
     * makes it modal (see "Modal dialogs" in the {@link UiRoot} description), dims what is under
     * it if the style has a dim, and moves keyboard focus to the widget from
     * {@link #setInitialFocus(Widget)}, or to the first button when there is none or it is not
     * inside the dialog. When the dialog closes, focus returns to the widget that had it before,
     * if that is still shown.
     *
     * <p>A dialog that has closed may be shown again, on the same or another root; it is packed
     * and centered again each time. Changing the content or the buttons while the dialog is
     * showing is not supported: the dialog keeps its size and position until it is shown again.
     *
     * <p>Allowed from inside a listener. Dialogs can be nested: showing a second dialog while one
     * is showing puts it on top, and closing it returns to the first.
     *
     * @param root root to show the dialog on; must not be {@code null}
     * @throws NullPointerException if {@code root} is {@code null}
     * @throws IllegalStateException if the dialog is already showing (on any root), or has no
     *         button; without a button the user could not answer or leave it
     */
    public void show(UiRoot root) {
        Objects.requireNonNull(root, "root");
        if (isShowing()) {
            throw new IllegalStateException("The dialog is already showing");
        }
        if (buttons.isEmpty()) {
            throw new IllegalStateException("The dialog has no button; add one before showing it");
        }
        pack();
        placeCentered(root);
        boolean focusInside = initialFocus != null && isInside(initialFocus);
        Widget focus = focusInside ? initialFocus : buttons.get(0);
        if (dialogStyle.hasDim()) {
            root.showModal(this, focus, dialogStyle.getDimRgba(), dialogStyle.getDimAmount());
        } else {
            root.showModal(this, focus, NO_DIM_RGBA, 0f);
        }
        shownRoot = root;
    }

    private boolean isInside(Widget widget) {
        for (Widget current = widget; current != null; current = current.getParent()) {
            if (current == this) {
                return true;
            }
        }
        return false;
    }

    private void placeCentered(UiRoot root) {
        WidgetGroup layer = root.getWindowLayer();
        int column = Math.max(0, (root.getSurfaceColumns() - getWidth()) / 2);
        int row = Math.max(0, (root.getSurfaceRows() - getHeight()) / 2);
        setPosition(column - layer.getX(), row - layer.getY());
    }

    /**
     * Reports a choice as if its button had been clicked: closes the dialog if
     * {@link #isCloseOnChoice()}, then calls the choice listeners in the order they were added.
     * It may be called from a click listener of one of the dialog's buttons. With
     * {@link #isCloseOnChoice()} {@code true} the dialog is already closed while the choice
     * listeners run, so calling this from one of them throws. An exception thrown by a listener
     * stops the remaining listeners and propagates to the caller.
     *
     * @param choice the choice to report; must not be {@code null}
     * @throws NullPointerException if {@code choice} is {@code null}
     * @throws IllegalStateException if the dialog is not showing
     */
    public void choose(DialogChoice choice) {
        Objects.requireNonNull(choice, "choice");
        if (!isShowing()) {
            throw new IllegalStateException("The dialog is not showing");
        }
        if (closeOnChoice) {
            hide();
        }
        ChoiceListener[] snapshot = choiceListeners;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].chosen(this, choice);
        }
        if (finalChoiceListener != null) {
            finalChoiceListener.chosen(this, choice);
        }
    }

    // For Dialogs: runs after every listener added through the public API, so the text of a text
    // input dialog is reported after the choice itself.
    void setFinalChoiceListener(ChoiceListener listener) {
        this.finalChoiceListener = listener;
    }

    /**
     * Hides the dialog without reporting a choice: the modal state ends and the focus from before
     * {@link #show(UiRoot)} is restored. The {@linkplain #addCloseListener window close
     * listeners} are called. Does nothing when the dialog is not showing.
     */
    public void dismiss() {
        if (isShowing()) {
            hide();
        }
    }

    private void hide() {
        UiRoot root = shownRoot;
        shownRoot = null;
        root.hideModal(this);
        super.close();
    }

    /**
     * Returns whether Escape reports the back-out choice.
     *
     * @return {@code true} when it does; {@code true} for a new dialog, unlike a plain window
     */
    @Override
    public boolean isCloseOnEscape() {
        return closeOnEscape;
    }

    /**
     * Sets whether the Escape key reports the {@linkplain #getBackOutChoice() back-out choice}
     * (which also closes the dialog if {@link #isCloseOnChoice()}). On by default. When off,
     * Escape does nothing in the dialog. Escape only reaches the dialog while keyboard focus is
     * on one of its descendants, and a descendant that consumes Escape keeps it. The window's
     * close button is not affected.
     *
     * @param closeOnEscape {@code true} to report the back-out choice on Escape
     */
    @Override
    public void setCloseOnEscape(boolean closeOnEscape) {
        this.closeOnEscape = closeOnEscape;
    }

    /**
     * Closes the dialog. While the dialog is showing this reports the
     * {@linkplain #getBackOutChoice() back-out choice}, exactly as {@link #choose(DialogChoice)}
     * does, so the window's close button and a code call both reach the choice listeners. When
     * the dialog is not showing, it behaves as {@link Window#close()}.
     */
    @Override
    public void close() {
        if (isShowing()) {
            choose(backOutChoice);
            return;
        }
        super.close();
    }

    /**
     * Makes the content area and the button row fill the interior, so the buttons are centered
     * in the dialog's whole width.
     *
     * @param left column of the area's left edge, in window cells
     * @param top row of the area's top edge, in window cells
     * @param width width of the area inside the border, in UI cells, zero or greater
     * @param height height of the area inside the border, in UI cells, zero or greater
     */
    @Override
    protected void layoutContent(int left, int top, int width, int height) {
        layoutTable.setPosition(left, top);
        layoutTable.setSize(width, height);
    }

    /**
     * Reports the back-out choice for Escape key-downs while the dialog is showing and
     * {@link #isCloseOnEscape()} is on, and leaves
     * every other event to {@link Window#handleEvent(UiEvent)}.
     *
     * @param event event being delivered; never {@code null}
     * @return {@code true} if the event was consumed
     */
    @Override
    protected boolean handleEvent(UiEvent event) {
        boolean escape = event.getType() == UiEventType.KEY_DOWN
                && event.getKeycode() == Input.Keys.ESCAPE;
        if (escape && closeOnEscape && isShowing()) {
            choose(backOutChoice);
            return true;
        }
        return super.handleEvent(event);
    }
}
