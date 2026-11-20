/**
 * Text widgets drawn into a {@link com.github.halfdeadgames.asciigdx.ui.UiSurface}, which mixes a
 * square font and a tall font.
 *
 * <p>A {@link com.github.halfdeadgames.asciigdx.ui.Widget} has a position and a size in UI cells.
 * One UI column is one square cell wide, and one UI row is one square cell high. A tall glyph
 * covers 1 column and 2 rows. Columns grow to the right and rows grow downward (y-down). A widget's
 * position is relative to the top-left corner of its parent
 * {@link com.github.halfdeadgames.asciigdx.ui.WidgetGroup}; a widget with no parent is placed
 * relative to the top-left corner of the target surface. Widgets draw only when {@code draw} is
 * called, from the render thread, and do not allocate while drawing.
 *
 * <p>Input: a {@link com.github.halfdeadgames.asciigdx.ui.UiRoot} owns the widget tree of a
 * surface, redraws it when something changed, and routes pointer and key events to widgets, with
 * hover, press capture, and keyboard focus. {@link com.github.halfdeadgames.asciigdx.ui.UiInput}
 * feeds it from libGDX input, and {@link com.github.halfdeadgames.asciigdx.ui.TextButton} is the
 * first interactive widget. A {@link com.github.halfdeadgames.asciigdx.ui.Tooltip} shows a hint
 * widget in the popup layer when the pointer rests on another widget.
 *
 * <p>Keyboard navigation: Tab and Shift+Tab move focus through the visible, focusable widgets in
 * tree order, and the arrow keys move it to the nearest one in that direction, unless a widget
 * handles the key. Menu bars and hidden widgets are skipped. Games can do the same with
 * {@link com.github.halfdeadgames.asciigdx.ui.UiRoot#moveFocus(
 * com.github.halfdeadgames.asciigdx.ui.FocusDirection)}. A focused button shows its focused look
 * while the last input was keyboard input, and not after a pointer press. A widget hidden with
 * {@link com.github.halfdeadgames.asciigdx.ui.Widget#setVisible(boolean)} keeps its layout space
 * but is not drawn, hit, or focused.
 *
 * <p>{@link com.github.halfdeadgames.asciigdx.ui.MenuBar} is a row of titles whose
 * {@link com.github.halfdeadgames.asciigdx.ui.Menu} drop-downs open in the root's popup layer and
 * close on Escape or an outside press. Its look comes from a
 * {@link com.github.halfdeadgames.asciigdx.ui.MenuBarStyle}.
 *
 * <p>{@link com.github.halfdeadgames.asciigdx.ui.CheckBox} and
 * {@link com.github.halfdeadgames.asciigdx.ui.RadioButton} are widgets with a checked state that
 * report changes made by the user to a {@link com.github.halfdeadgames.asciigdx.ui.ChangeListener}.
 * A {@link com.github.halfdeadgames.asciigdx.ui.ButtonGroup} keeps at most one of its radio
 * buttons checked. Their look comes from a {@link com.github.halfdeadgames.asciigdx.ui.ToggleStyle}.
 *
 * <p>{@link com.github.halfdeadgames.asciigdx.ui.TextField} is a one-line text input with a caret,
 * scrolling with ellipses, a character filter, a length limit, and placeholder text. Its look and
 * caret come from a {@link com.github.halfdeadgames.asciigdx.ui.TextFieldStyle}. A root can focus
 * the first text field by itself, see
 * {@link com.github.halfdeadgames.asciigdx.ui.UiRoot#setAutoFocusTextFields(boolean)}, and
 * {@link com.github.halfdeadgames.asciigdx.ui.UiInput} shows the on-screen keyboard while a text
 * field has focus.
 *
 * <p>{@link com.github.halfdeadgames.asciigdx.ui.ProgressBar} is a display-only bar drawn with
 * eighth blocks, and {@link com.github.halfdeadgames.asciigdx.ui.Slider} lets the user pick a value
 * with the arrow keys, Home, End, or the pointer. Both run horizontally or vertically (see
 * {@link com.github.halfdeadgames.asciigdx.ui.BarOrientation}) over a
 * {@link com.github.halfdeadgames.asciigdx.ui.ValueRange}; their looks come from a
 * {@link com.github.halfdeadgames.asciigdx.ui.ProgressBarStyle} and a
 * {@link com.github.halfdeadgames.asciigdx.ui.SliderStyle}.
 *
 * <p>Creating widgets: a widget with one form is created with a public constructor, as with
 * {@link com.github.halfdeadgames.asciigdx.ui.TextButton},
 * {@link com.github.halfdeadgames.asciigdx.ui.CheckBox},
 * {@link com.github.halfdeadgames.asciigdx.ui.RadioButton}, and
 * {@link com.github.halfdeadgames.asciigdx.ui.TextField}. Static factories are used only where one
 * class has several distinct forms, such as a bar that is either horizontal or vertical, which is
 * created by a factory named for each form.
 *
 * <p>Example with a button:
 * <pre>{@code
 * UiSurface surface = UiSurface.ofSize(40, 12);
 * surface.addTo(layers, tallGlyphs, squareGlyphs);
 * UiRoot ui = UiRoot.of(surface);
 * LabelStyle look = LabelStyle.of(UiFont.TALL, 0xFFFFFFFF, 0x303030FF);
 * TextButton start = new TextButton("Start", ButtonStyle.of(look));
 * start.setPosition(2, 2);
 * start.addClickListener(button -> startGame());
 * ui.getRoot().addChild(start);
 * // a CellPicker turns screen pixels into cells; the UI goes first in the multiplexer
 * Gdx.input.setInputProcessor(
 *         new InputMultiplexer(UiInput.forRoot(ui, picker), gameInput));
 * // every frame, before drawing the layers:
 * ui.update(deltaSeconds);
 * ui.draw();
 * }</pre>
 *
 * <p>Example without input:
 * <pre>{@code
 * UiSurface surface = UiSurface.ofSize(40, 12);
 * surface.addTo(layers, tallGlyphs, squareGlyphs);
 * WidgetGroup root = new WidgetGroup();
 * root.setPosition(2, 2);
 * Label title = new Label("Hello", LabelStyle.of(UiFont.TALL, 0xFFFFFFFF, 0x000000FF));
 * title.setPosition(1, 0);
 * root.addChild(title);
 * root.draw(surface);
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.ui;
