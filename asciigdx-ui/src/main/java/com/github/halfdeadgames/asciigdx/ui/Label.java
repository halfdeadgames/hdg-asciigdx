package com.github.halfdeadgames.asciigdx.ui;

import com.github.halfdeadgames.asciigdx.text.ColorNames;
import com.github.halfdeadgames.asciigdx.text.EffectNames;
import com.github.halfdeadgames.asciigdx.text.EffectRun;
import com.github.halfdeadgames.asciigdx.text.StyledText;
import java.util.List;
import java.util.Objects;

/**
 * A single line of text on a solid background, in the text font. The text takes one text column
 * (half a UI column) per code point, does not wrap, and is cut off at the label's width.
 * {@code '\n'} does not start a new line; it is drawn like any other code point.
 *
 * <p>Drawing a label erases every glyph in its rectangle, so cells past the end of the text are
 * blank. When the style's background has alpha 0, the label keeps each cell's existing
 * background, so it shows whatever was drawn there before, such as a panel. Any other
 * background, including a partly transparent one, replaces the cell's background.
 *
 * <p>The text is written along the label's top row. By default it starts at the label's left
 * edge; see {@link #setTextAlignment} for other places when the label is wider than its text. The
 * label's width is in whole UI cells and rounds up from the text, so a text of an odd length
 * leaves a blank half cell, and a label cannot be sized in half cells.
 *
 * <p>A new label is packed: its size fits its text. Changing the text keeps the current size, so
 * call {@link #pack()} afterward to fit the new text.
 *
 * <p>Markup: while {@link #isMarkupEnabled()} is {@code true} (the default), the text may hold
 * inline color tags such as {@code The [color:red]quick[/color] fox}; see {@link StyledText} for
 * the tags. Tags take no width: the preferred width, the alignment, and the cut-off count visible
 * letters only. {@link #getText()} still returns the text with its tags. Color names come from
 * {@link #setColorNames}, for example {@code label.setColorNames(skin.getColorNames())}. Per-letter
 * foreground colors are drawn. Per-letter background ({@code [bg:...]}) is drawn too, with the
 * granularity of one text column, which is half a UI cell: the highlight covers exactly the
 * tagged letters, so it can start or end in the middle of a UI cell. A letter with a background
 * tag is drawn with {@link UiSurface#writeTextGlyphWithBackground}, so its background sits above
 * the label's own background; a letter without one keeps the cell's background. Use
 * {@link StyledText#escape} on text you do not trust.
 *
 * <p>Effect tags ({@code [effect:wave phase=columns:6]}) are read only after
 * {@link #setEffectNames}; until then they stay visible as text. A label draws no effects itself.
 * The game reads {@link #getEffectRuns()} and {@link #getAbsoluteTextColumnOfLetter(int)}, builds
 * one {@code EffectZone} per run on {@link UiSurface#getTextLayer()}, and applies it each frame
 * with its own time. Letters that are not shown return {@link #NOT_SHOWN}; the game skips them.
 *
 * <p>Like every widget, a label is for the game (GL) thread only.
 */
public class Label extends Widget {

    /**
     * The value that {@link #getTextColumnOfLetter(int)} and
     * {@link #getAbsoluteTextColumnOfLetter(int)} return for a letter that is not shown. It is
     * {@link Integer#MIN_VALUE}, not -1, because a shown letter can have the real column -1 when
     * its label sits partly left of the surface. Compare with this constant, never with -1.
     */
    public static final int NOT_SHOWN = Integer.MIN_VALUE;

    private static final int ALPHA_MASK = 0xFF;

    private String text;
    private LabelStyle style;
    private HorizontalAlignment textAlignment = HorizontalAlignment.LEFT;
    private ColorNames colorNames = ColorNames.NONE;
    private EffectNames effects = EffectNames.NONE;
    private boolean markupEnabled = true;
    private StyledText styledText;

    /**
     * Returns where the text sits when the label is wider than its text.
     *
     * @return the alignment, never {@code null}; {@link HorizontalAlignment#LEFT} for a new label
     */
    public HorizontalAlignment getTextAlignment() {
        return textAlignment;
    }

    /**
     * Sets where the text sits when the label is wider than its text. The leftover space is
     * counted in text columns (half UI cells): {@code LEFT} puts the text at the left edge,
     * {@code RIGHT} ends it at the right edge, and {@code CENTER} puts half of the leftover,
     * rounded down, before the text, so centered text can start on a half cell. Text that is
     * longer than the label is not affected: it starts at the left edge and is cut off at the
     * right edge. A change requests a redraw (not layout); an equal alignment changes nothing.
     *
     * @param alignment the new alignment; must not be {@code null}
     * @throws NullPointerException if {@code alignment} is {@code null}
     */
    public void setTextAlignment(HorizontalAlignment alignment) {
        Objects.requireNonNull(alignment, "alignment");
        if (alignment == textAlignment) {
            return;
        }
        textAlignment = alignment;
        requestRedraw();
    }

    /**
     * Creates a label sized to fit its text.
     *
     * @param text text to show; must not be {@code null}; may be empty
     * @param style colors to use; must not be {@code null}
     * @throws NullPointerException if {@code text} or {@code style} is {@code null}
     */
    public Label(String text, LabelStyle style) {
        this.text = Objects.requireNonNull(text, "text");
        this.style = Objects.requireNonNull(style, "style");
        parseMarkup();
        setSize(getPreferredWidth(), getPreferredHeight());
    }

    /**
     * Returns the color name lookup used for {@code [color:NAME]} and {@code [bg:NAME]} tags.
     *
     * @return the lookup, never {@code null}; {@link ColorNames#NONE} for a new label
     */
    public ColorNames getColorNames() {
        return colorNames;
    }

    /**
     * Sets the color name lookup for tags. The text is parsed again once, now, not on every draw,
     * so later changes inside the lookup are not noticed until the next call to this method or to
     * {@link #setText}. Every call re-reads the lookup, even with the same instance, so a game
     * whose lookup changes (for example after a theme change) calls this again to refresh the
     * label. Calls {@link #invalidate()}, because unknown names stay visible and change the width.
     * Has no effect on drawing while {@link #isMarkupEnabled()} is {@code false}.
     *
     * @param names the lookup; must not be {@code null}; use {@link ColorNames#NONE} for none
     * @throws NullPointerException if {@code names} is {@code null}
     */
    public void setColorNames(ColorNames names) {
        Objects.requireNonNull(names, "names");
        colorNames = names;
        parseMarkup();
        invalidate();
    }

    /**
     * Returns whether tags in the text are read.
     *
     * @return {@code true} if tags are read; {@code true} for a new label
     */
    public boolean isMarkupEnabled() {
        return markupEnabled;
    }

    /**
     * Sets whether tags in the text are read. When {@code false} the text is drawn literally,
     * tags included, and its full length counts. A change parses or drops the parsed text once and
     * calls {@link #invalidate()}; an equal value changes nothing.
     *
     * @param markupEnabled {@code true} to read tags, {@code false} to draw the text as it is
     */
    public void setMarkupEnabled(boolean markupEnabled) {
        if (markupEnabled == this.markupEnabled) {
            return;
        }
        this.markupEnabled = markupEnabled;
        parseMarkup();
        invalidate();
    }

    /**
     * Returns the lookup used for {@code [effect:NAME ...]} tags.
     *
     * @return the lookup, never {@code null}; {@link EffectNames#NONE} for a new label, which
     *         leaves effect tags visible as text
     */
    public EffectNames getEffectNames() {
        return effects;
    }

    /**
     * Sets the lookup for effect tags, for example
     * {@code label.setEffectNames(skin.getEffectRegistry())}.
     * With a lookup that knows a tag, the tag is removed from the visible text and its letters
     * become an effect run, see {@link #getEffectRuns()}. The label does not run effects: the
     * game builds an {@code EffectZone} for each run and applies it with its own time. The text is
     * parsed again once, now, not on every draw. Every call re-reads the lookup, even with the same
     * instance. Calls {@link #invalidate()}, because a removed tag changes the width. Has no effect
     * on drawing while {@link #isMarkupEnabled()} is {@code false}.
     *
     * <p>An effect registered in the lookup after this call is not seen by the label until this
     * method is called again. Call it from the game (GL) thread only, like every widget method.
     *
     * @param effects the lookup; must not be {@code null}; use {@link EffectNames#NONE} for none
     * @throws NullPointerException if {@code effects} is {@code null}
     */
    public void setEffectNames(EffectNames effects) {
        Objects.requireNonNull(effects, "effects");
        this.effects = effects;
        parseMarkup();
        invalidate();
    }

    /**
     * Returns the effects of the current text as ranges of letters. Letter indexes count the
     * visible letters (code points without tags), from 0, the same numbers that
     * {@link #getTextColumnOfLetter(int)} takes. Runs are ordered, do not overlap, and are never
     * empty; see {@link StyledText#getEffectRuns()}. Valid at any time and cheap: it returns the
     * result of the last parse, which is made when the text, the color names, the effects, or the
     * markup flag change.
     *
     * @return an unmodifiable list, never {@code null}; empty while {@link #isMarkupEnabled()} is
     *         {@code false} or when the text has no effects
     */
    public List<EffectRun> getEffectRuns() {
        return styledText == null ? List.of() : styledText.getEffectRuns();
    }

    /**
     * Returns where a visible letter is drawn, relative to the label's left edge, in text columns
     * (half a UI cell each). Column 0 is the left half of the label's first UI cell. Games
     * normally call {@link #getAbsoluteTextColumnOfLetter(int)} instead, which adds the label's
     * position and also checks visibility and scroll panes. The row is the label's top row,
     * because a label has one line. Alignment and the cut-off at the label's width are applied.
     * Valid after the label has its final size, so after {@link #validate()} or {@link #pack()};
     * it reads the current size, alignment, and text, and needs no draw. It does not know the
     * surface the label is drawn into.
     *
     * @param letterIndex index of a visible letter, as used by {@link EffectRun}
     * @return the text column of the letter, 0 or greater; {@link #NOT_SHOWN} when the index is
     *         negative or not below the number of visible letters, or when the letter is cut off
     *         by the label's width
     */
    public int getTextColumnOfLetter(int letterIndex) {
        String displayedText = getDisplayedText();
        int letters = displayedText.codePointCount(0, displayedText.length());
        if (letterIndex < 0 || letterIndex >= letters) {
            return NOT_SHOWN;
        }
        int textColumnLimit = UiSurface.TEXT_COLUMNS_PER_CELL * getWidth();
        int textColumn = alignmentOffset(textColumnLimit, letters) + letterIndex;
        return textColumn < textColumnLimit ? textColumn : NOT_SHOWN;
    }

    /**
     * Returns the column in the text grid of {@link UiSurface#getTextGrid()} where a visible
     * letter is shown: {@code 2 * getAbsoluteX() + getTextColumnOfLetter(letterIndex)}. The row of
     * every letter is {@link #getAbsoluteY()}. Use it to place an effect zone on the letters of an
     * {@link EffectRun}.
     *
     * <p>Returns {@link #NOT_SHOWN} for each letter that is not shown: an index out of range, a
     * letter cut off by the label's width (see {@link #getTextColumnOfLetter(int)}), a letter of
     * a label that is hidden or inside a hidden ancestor ({@link #isVisible()} is {@code false}
     * anywhere up the chain), and a letter whose UI cell lies outside the view of a
     * {@link ScrollPane} ancestor.
     * A UI cell is two text columns, so each half of a cell at the edge of the view counts as
     * inside or outside together.
     *
     * <p>Windows, popups, and other widgets drawn over the label are not detected. The result is
     * valid only when the label has its final size and position, so after layout; call it again
     * after anything moves, scrolls, or hides the label.
     *
     * @param letterIndex index of a visible letter, as used by {@link EffectRun}
     * @return the absolute text column, which is -1 or less for a shown letter left of the surface;
     *         {@link #NOT_SHOWN} when the letter is not shown
     */
    public int getAbsoluteTextColumnOfLetter(int letterIndex) {
        int relativeColumn = getTextColumnOfLetter(letterIndex);
        if (relativeColumn == NOT_SHOWN || isHiddenInChain()) {
            return NOT_SHOWN;
        }
        int absoluteColumn = UiSurface.TEXT_COLUMNS_PER_CELL * getAbsoluteX() + relativeColumn;
        return isOutsideScrollPaneViews(absoluteColumn) ? NOT_SHOWN : absoluteColumn;
    }

    private boolean isHiddenInChain() {
        for (Widget current = this; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return true;
            }
        }
        return false;
    }

    private boolean isOutsideScrollPaneViews(int absoluteTextColumn) {
        int cell = Math.floorDiv(absoluteTextColumn, UiSurface.TEXT_COLUMNS_PER_CELL);
        int row = getAbsoluteY();
        for (Widget current = getParent(); current != null; current = current.getParent()) {
            if (current instanceof ScrollPane pane && !isInsideView(pane, cell, row)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideView(ScrollPane pane, int cell, int row) {
        int left = pane.getAbsoluteX();
        int top = pane.getAbsoluteY();
        return cell >= left && cell < left + pane.getViewColumns()
                && row >= top && row < top + pane.getViewRows();
    }

    private void parseMarkup() {
        styledText = markupEnabled ? StyledText.parse(text, colorNames, effects) : null;
    }

    private String getDisplayedText() {
        return styledText == null ? text : styledText.getVisibleText();
    }

    private int getForegroundRgbaAt(int letterIndex) {
        if (styledText == null || !styledText.hasForegroundAt(letterIndex)) {
            return style.getForegroundRgba();
        }
        return styledText.getForegroundRgbaAt(letterIndex);
    }

    /**
     * Returns the text, exactly as it was given, including any tags.
     *
     * @return the text, never {@code null}
     */
    public String getText() {
        return text;
    }

    /**
     * Replaces the text. The size stays as it is; call {@link #pack()} to fit the new text. When
     * the text differs from the current one, calls {@link #invalidate()}; equal text changes no
     * marks. Layout is marked even though the size is kept, because the preferred size changes
     * and {@link #pack()} and parent layouts rely on that mark.
     *
     * @param text new text; must not be {@code null}; may be empty
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public void setText(String text) {
        Objects.requireNonNull(text, "text");
        if (text.equals(this.text)) {
            return;
        }
        this.text = text;
        parseMarkup();
        invalidate();
    }

    /**
     * Returns the colors.
     *
     * @return the style, never {@code null}
     */
    public LabelStyle getStyle() {
        return style;
    }

    /**
     * Replaces the colors. The size is unaffected. When the style differs from the current one,
     * the label is marked by {@link #requestRedraw()}. An equal style changes no marks.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(LabelStyle style) {
        Objects.requireNonNull(style, "style");
        if (style.equals(this.style)) {
            return;
        }
        this.style = style;
        requestRedraw();
    }

    /**
     * Returns the width of the text, rounded up to whole UI cells. The text takes one text column
     * per code point, so a character above U+FFFF counts once, and two code points fit in one UI
     * cell. With markup on, only visible letters count; tags take no width.
     * The width always rounds up to whole UI cells, so a text of odd length leaves a blank half
     * cell at its end, and a label cannot be sized in half cells.
     *
     * @return the width in UI cells (not text columns), zero or greater
     */
    @Override
    public int getPreferredWidth() {
        String displayedText = getDisplayedText();
        int codePoints = displayedText.codePointCount(0, displayedText.length());
        return (codePoints + UiSurface.TEXT_COLUMNS_PER_CELL - 1)
                / UiSurface.TEXT_COLUMNS_PER_CELL;
    }

    /**
     * Returns the height of one line of text.
     *
     * @return 1 UI cell
     */
    @Override
    public int getPreferredHeight() {
        return 1;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        target.eraseGlyphs(left, top, width, height);
        int background = style.getBackgroundRgba();
        if ((background & ALPHA_MASK) != 0) {
            target.fillBackground(left, top, width, height, background);
        }
        writeClippedText(target, left, top, width);
    }

    // Text that fills or overflows the label always starts at the left edge.
    private int alignmentOffset(int textColumnLimit, int letters) {
        int spare = textColumnLimit - letters;
        if (spare <= 0) {
            return 0;
        }
        return switch (textAlignment) {
            case LEFT -> 0;
            case CENTER -> spare / 2;
            case RIGHT -> spare;
        };
    }

    private void writeLetter(UiSurface target, int textColumn, int top, int codePoint,
            int letterIndex) {
        int foreground = getForegroundRgbaAt(letterIndex);
        if (styledText == null || !styledText.hasBackgroundAt(letterIndex)) {
            target.writeTextGlyph(textColumn, top, codePoint, foreground);
            return;
        }
        target.writeTextGlyphWithBackground(textColumn, top, codePoint, foreground,
                styledText.getBackgroundRgbaAt(letterIndex));
    }

    private void writeClippedText(UiSurface target, int left, int top, int width) {
        String displayedText = getDisplayedText();
        int length = displayedText.length();
        int textColumnLimit = UiSurface.TEXT_COLUMNS_PER_CELL * width;
        int leftTextColumn = UiSurface.TEXT_COLUMNS_PER_CELL * left;
        int index = 0;
        int letterIndex = 0;
        int textOffset = alignmentOffset(textColumnLimit,
                displayedText.codePointCount(0, length));
        while (index < length && textOffset < textColumnLimit
                && leftTextColumn + textOffset < target.getTextColumns()) {
            int codePoint = displayedText.codePointAt(index);
            writeLetter(target, leftTextColumn + textOffset, top, codePoint, letterIndex);
            index += Character.charCount(codePoint);
            letterIndex++;
            textOffset++;
        }
    }
}
