package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.Layer;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs;
import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs.Shade;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.TransparencyMode;
import com.github.halfdeadgames.asciigdx.sandbox.review.ReviewCellSize;
import com.github.halfdeadgames.asciigdx.sandbox.review.ReviewList;
import com.github.halfdeadgames.asciigdx.sandbox.review.ReviewSession;
import com.github.halfdeadgames.asciigdx.sandbox.review.ReviewSession.FailReason;
import com.github.halfdeadgames.asciigdx.sandbox.review.ReviewSession.Verdict;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * Judges glyphs one at a time: enlarged copies of the glyph, then the glyph at 1x among
 * neighbors in several color pairs. Answers are saved after every change and resumed on open.
 */
final class GlyphReviewScene extends GridScene {

    private static final int[] REVIEW_LIST = {
        0xE0D0, 0xE0D1, 0xE0D2, 0xE0D3, 0xE0E0, 0xE0E4, 0x2B05, 0x2764, 0x26A1, 0x2654,
    };

    private static final int COLUMNS = 40;
    private static final int ROWS = 23;

    private static final int PROGRESS_ROW = 0;
    private static final int PREVIEW_TOP_ROW = 2;
    private static final int PREVIEW_ROWS = 10;
    private static final int CONTEXT_FIRST_ROW = PREVIEW_TOP_ROW + PREVIEW_ROWS + 1;
    private static final int LAST_ANSWER_ROW = CONTEXT_FIRST_ROW + 5;
    private static final int ERROR_ROW = LAST_ANSWER_ROW + 1;
    private static final int HELP_ROW = ERROR_ROW + 1;
    private static final int CONTEXT_WIDTH = 20;

    private static final int MARGIN_COLUMNS = 1;
    private static final int MENU_COLUMN = 21;
    private static final int MENU_TITLE_ROW = PREVIEW_TOP_ROW;
    private static final int MENU_FIRST_ITEM_ROW = PREVIEW_TOP_ROW + 2;
    private static final int REASON_MENU_ITEMS = FailReason.values().length + 1;

    private static final int COMPARE_LABEL_ROW = 1;
    private static final int COMPARE_MENU_LAST_TITLE_ROW = HELP_ROW - REASON_MENU_ITEMS - 1;
    private static final int[] COMPARE_TARGET_HEIGHTS = {16, 32, 64};
    private static final int PAIR_GAP = 4;
    private static final int PAIR_SPACING = 12;
    private static final String MISSING_MARKER = "new";
    private static final int CONTEXT_PATTERN_LENGTH = 17;

    private static final int PREVIEW_PADDING = 8;
    private static final int PREVIEW_WIDTH = 152;
    private static final int[] PREVIEW_SCALES = {2, 4, 8};
    private static final int[] PREVIEW_X = {8, 32, 80};

    private static final int LABEL = 0x9090C0FF;
    private static final int ERROR = 0xFF6060FF;
    private static final int PREVIEW_FOREGROUND = 0xE8E8E8FF;
    private static final int PREVIEW_BACKGROUND = 0x101018FF;

    private static final int[][] COLOR_PAIRS = {
        {0xE8E8E8FF, 0x101018FF},
        {0x101010FF, 0xE8E8E8FF},
        {0xFFD700FF, 0x102040FF},
        {0x40E040FF, 0x000000FF},
    };

    private static final int BOX_VERTICAL = 0x2502;
    private static final int MEDIUM_SHADE = 0x2592;

    private static final String RESULTS_FILE = "glyph-review/results.txt";
    private static final String DESKTOP_RESULTS_FILE = "../build/glyph-review/results.txt";

    static final String KEY_REVIEW_CHARS = "reviewChars";

    static final String KEY_REVIEW_SHEET = "reviewSheet";
    static final String KEY_REVIEW_CELL = "reviewCell";

    private final GlyphSource glyphs;
    private final String reviewCharsPath;
    private final String reviewSheetPath;
    private final String reviewCell;
    private final Color drawColor = new Color();

    private boolean candidateLoadAttempted;
    private TextureGlyphSource candidate;
    private FileHandle candidateOrderFile;
    private String candidateError = "";

    private TerminalGrid candidateGrid;
    private Layer candidateLayer;
    private int[] compareHeights = new int[0];
    private int[] compareX = new int[0];
    private int comparePanelWidth;
    private int comparePanelRows;
    private int contextPairs;

    private int[] reviewList = REVIEW_LIST;
    private String listNotice = "";
    private ReviewSession session = new ReviewSession(REVIEW_LIST);
    private final Vector2 touchPoint = new Vector2();

    private boolean reasonMenuOpen;
    private int reasonMenuCursor;
    private String errorMessage = "";
    private boolean savingBlocked;

    /**
     * @param reviewCharsPath absolute path of a glyph order text file to review, or null for the sheet's own
     *        order (with a candidate sheet) or the demo list
     * @param reviewSheetPath absolute path of a candidate sheet PNG (its list is the same path ending in .txt),
     *        or null to review the default font
     * @param reviewCell cell size of the candidate sheet, {@code WxH} or a single number
     */
    GlyphReviewScene(GlyphSource glyphs, String reviewCharsPath, String reviewSheetPath, String reviewCell) {
        super(glyphs, COLUMNS, ROWS);
        this.glyphs = glyphs;
        this.reviewCharsPath = emptyToNull(reviewCharsPath);
        this.reviewSheetPath = emptyToNull(reviewSheetPath);
        this.reviewCell = emptyToNull(reviewCell);
    }

    private boolean comparing() {
        return candidateLayer != null;
    }

    private int menuTitleRow() {
        return comparing() ? Math.min(PREVIEW_TOP_ROW + comparePanelRows, COMPARE_MENU_LAST_TITLE_ROW)
            : MENU_TITLE_ROW;
    }

    private int menuFirstItemRow() {
        return comparing() ? menuTitleRow() + 1 : MENU_FIRST_ITEM_ROW;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (candidate != null) {
            candidate.dispose();
            candidate = null;
        }
    }

    @Override
    public String id() {
        return "glyph-review";
    }

    @Override
    public String title() {
        return "Glyph review";
    }

    @Override
    public void show() {
        reasonMenuOpen = false;
        loadResults();
        refresh();
        super.show();
    }

    @Override
    public String statusText() {
        if (listNotice.isEmpty() || errorMessage.isEmpty()) {
            return listNotice + errorMessage;
        }
        return listNotice + "  " + errorMessage;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (reasonMenuOpen) {
            return reasonMenuKeyDown(keycode);
        }
        int answeredBefore = session.answeredCount();
        switch (keycode) {
            case Input.Keys.LEFT -> session.fail();
            case Input.Keys.UP -> session.needsWork();
            case Input.Keys.RIGHT -> session.perfect();
            case Input.Keys.BACKSPACE -> session.undo();
            default -> {
                return false;
            }
        }
        finishAnswer(answeredBefore);
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        if (reasonMenuOpen) {
            int index = reasonMenuIndexAt(screenX, screenY);
            if (index >= 0) {
                chooseReason(index);
            }
            return true;
        }
        int answeredBefore = session.answeredCount();
        int third = screenX * 3 / Math.max(1, Gdx.graphics.getWidth());
        if (third <= 0) {
            session.fail();
        } else if (third == 1) {
            session.needsWork();
        } else {
            session.perfect();
        }
        finishAnswer(answeredBefore);
        return true;
    }

    private void finishAnswer(int answeredBefore) {
        boolean recorded = session.answeredCount() > answeredBefore;
        if (recorded && session.lastVerdict().orElse(Verdict.PERFECT) != Verdict.PERFECT) {
            reasonMenuOpen = true;
            reasonMenuCursor = 0;
        }
        saveResults();
        refresh();
    }

    private boolean reasonMenuKeyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.UP -> moveReasonCursor(-1);
            case Input.Keys.DOWN -> moveReasonCursor(1);
            case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER, Input.Keys.SPACE -> chooseReason(reasonMenuCursor);
            case Input.Keys.ESCAPE -> chooseReason(0);
            case Input.Keys.BACKSPACE -> {
                session.undo();
                reasonMenuOpen = false;
                saveResults();
                refresh();
            }
            case Input.Keys.LEFT, Input.Keys.RIGHT -> {
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void moveReasonCursor(int step) {
        reasonMenuCursor = Math.floorMod(reasonMenuCursor + step, REASON_MENU_ITEMS);
        refresh();
    }

    /** Index 0 leaves the reason unset; 1 and up pick the matching {@link FailReason}. */
    private void chooseReason(int index) {
        reasonMenuOpen = false;
        if (index > 0) {
            session.setReasonForLast(FailReason.values()[index - 1]);
        }
        saveResults();
        refresh();
    }

    /** Menu item under the given screen position, or -1 when it is not on an item. */
    private int reasonMenuIndexAt(int screenX, int screenY) {
        touchPoint.set(screenX, screenY);
        viewport.unproject(touchPoint);
        int column = (int) (touchPoint.x / glyphs.getCellWidth());
        int row = (int) ((viewport.getWorldHeight() - touchPoint.y) / glyphs.getCellHeight());
        int index = row - menuFirstItemRow();
        boolean inColumns = column >= MENU_COLUMN && column < grid.getWidth();
        return inColumns && index >= 0 && index < REASON_MENU_ITEMS ? index : -1;
    }

    private static String reasonName(FailReason reason) {
        return switch (reason) {
            case UNREADABLE_AT_1X -> "Unreadable at 1x";
            case OFF_CENTER -> "Off center";
            case TOO_SIMILAR -> "Too similar";
            case UGLY_OR_WRONG_SHAPE -> "Ugly/wrong shape";
            case WRONG_ORIENTATION -> "Wrong orientation";
        };
    }

    private void drawReasonMenu() {
        int menuColumn = MENU_COLUMN;
        writeText(menuColumn + 1, menuTitleRow(), "Reason", GOLD, CLEAR);
        FailReason[] reasons = FailReason.values();
        for (int i = 0; i < REASON_MENU_ITEMS; i++) {
            boolean selected = i == reasonMenuCursor;
            int foreground = selected ? BLACK : WHITE;
            int background = selected ? WHITE : CLEAR;
            int row = menuFirstItemRow() + i;
            for (int column = menuColumn; column < grid.getWidth(); column++) {
                grid.set(column, row, ' ', foreground, background);
            }
            writeText(menuColumn + 1, row, i == 0 ? "No reason" : reasonName(reasons[i - 1]), foreground, background);
        }
    }

    @Override
    protected void drawOverGrid(SpriteBatch batch) {
        OptionalInt current = displayedCodePoint();
        if (current.isEmpty()) {
            return;
        }
        if (comparing()) {
            drawComparePreviews(batch, current.getAsInt());
            return;
        }
        int cellWidth = glyphs.getCellWidth();
        int cellHeight = glyphs.getCellHeight();
        int panelTop = Math.round(viewport.getWorldHeight()) - PREVIEW_TOP_ROW * cellHeight;
        int panelBottom = panelTop - PREVIEW_ROWS * cellHeight;
        TextureRegion solid = glyphs.glyph(QuadrantGlyphs.codePoint(Shade.SOLID, QuadrantGlyphs.ALL));
        int left = MARGIN_COLUMNS * cellWidth;
        setDrawColor(batch, PREVIEW_BACKGROUND);
        batch.draw(solid, left, panelBottom, PREVIEW_WIDTH, panelTop - panelBottom);
        setDrawColor(batch, PREVIEW_FOREGROUND);
        TextureRegion region = (candidate != null ? candidate : glyphs).glyph(current.getAsInt());
        for (int i = 0; i < PREVIEW_SCALES.length; i++) {
            batch.draw(region, left + PREVIEW_X[i], panelBottom + PREVIEW_PADDING,
                cellWidth * PREVIEW_SCALES[i], cellHeight * PREVIEW_SCALES[i]);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawComparePreviews(SpriteBatch batch, int codePoint) {
        TextureRegion solid = glyphs.glyph(QuadrantGlyphs.codePoint(Shade.SOLID, QuadrantGlyphs.ALL));
        int cellHeight = glyphs.getCellHeight();
        int top = Math.round(viewport.getWorldHeight()) - PREVIEW_TOP_ROW * cellHeight;
        int bottom = top - comparePanelRows * cellHeight;
        int left = MARGIN_COLUMNS * glyphs.getCellWidth();
        setDrawColor(batch, PREVIEW_BACKGROUND);
        batch.draw(solid, left, bottom, comparePanelWidth, top - bottom);
        setDrawColor(batch, PREVIEW_FOREGROUND);
        for (int i = 0; i < compareHeights.length; i++) {
            int candidateScale = compareHeights[i] / candidate.getCellHeight();
            int x = left + compareX[i];
            drawPreviewGlyph(batch, candidate, codePoint, x, bottom + PREVIEW_PADDING, candidateScale);
            x += candidate.getCellWidth() * candidateScale + PAIR_GAP;
            drawPreviewGlyph(batch, glyphs, codePoint, x, bottom + PREVIEW_PADDING,
                compareHeights[i] / glyphs.getCellHeight());
        }
        batch.setColor(Color.WHITE);
    }

    private void drawPreviewGlyph(SpriteBatch batch, GlyphSource source, int codePoint, int x, int y, int scale) {
        if (source.hasGlyph(codePoint)) {
            batch.draw(source.glyph(codePoint), x, y, source.getCellWidth() * scale, source.getCellHeight() * scale);
            return;
        }
        int markerScale = Math.max(1, scale / MISSING_MARKER.length());
        for (int i = 0; i < MISSING_MARKER.length(); i++) {
            batch.draw(source.glyph(MISSING_MARKER.charAt(i)), x + i * source.getCellWidth() * markerScale, y,
                source.getCellWidth() * markerScale, source.getCellHeight() * markerScale);
        }
    }

    /** Heights both sources reach with whole-number scales, so the two rows line up. */
    private void layoutCompare() {
        int candidateHeight = candidate.getCellHeight();
        int defaultHeight = glyphs.getCellHeight();
        int common = candidateHeight / gcd(candidateHeight, defaultHeight) * defaultHeight;
        IntArray heights = new IntArray();
        for (int target : COMPARE_TARGET_HEIGHTS) {
            if (target % common == 0) {
                heights.add(target);
            }
        }
        if (heights.isEmpty()) {
            heights.add(common);
        }

        int maxWidth = (COLUMNS - 2 * MARGIN_COLUMNS) * glyphs.getCellWidth();
        int used = heights.size;
        while (used > 1 && panelWidth(heights, used) > maxWidth) {
            used--;
        }
        compareHeights = new int[used];
        compareX = new int[used];
        int cursor = PREVIEW_PADDING;
        for (int i = 0; i < used; i++) {
            compareHeights[i] = heights.get(i);
            compareX[i] = cursor;
            cursor += pairWidth(heights.get(i)) + PAIR_SPACING;
        }
        comparePanelWidth = panelWidth(heights, used);

        int cellHeight = glyphs.getCellHeight();
        comparePanelRows = (compareHeights[used - 1] + 2 * PREVIEW_PADDING + cellHeight - 1) / cellHeight;
        int contextTopRow = PREVIEW_TOP_ROW + comparePanelRows;
        int availablePixels = (LAST_ANSWER_ROW - contextTopRow) * cellHeight;
        contextPairs = Math.max(1, Math.min(COLOR_PAIRS.length, availablePixels / candidateHeight));
    }

    private int pairWidth(int height) {
        return candidate.getCellWidth() * (height / candidate.getCellHeight()) + PAIR_GAP
            + glyphs.getCellWidth() * (height / glyphs.getCellHeight());
    }

    private int panelWidth(IntArray heights, int count) {
        int width = 2 * PREVIEW_PADDING - PAIR_SPACING;
        for (int i = 0; i < count; i++) {
            width += pairWidth(heights.get(i)) + PAIR_SPACING;
        }
        return width;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private void setDrawColor(SpriteBatch batch, int rgba) {
        Color.rgba8888ToColor(drawColor, rgba);
        batch.setColor(drawColor);
    }

    private void refresh() {
        grid.clear();
        if (comparing()) {
            candidateGrid.clear();
        }
        OptionalInt current = displayedCodePoint();
        if (current.isPresent()) {
            int position = reasonMenuOpen ? session.answeredCount() : session.answeredCount() + 1;
            writeLine(PROGRESS_ROW, String.format(Locale.ROOT, "%d / %d   U+%04X",
                position, session.totalCount(), current.getAsInt()), WHITE);
            if (comparing()) {
                writeCompareLabels();
                writeCandidateContext(current.getAsInt());
            } else {
                writeContextRows(current.getAsInt());
            }
        } else {
            writeLine(PROGRESS_ROW, String.format(Locale.ROOT, "Done: %d perfect, %d needs work, %d fail",
                session.count(Verdict.PERFECT), session.count(Verdict.NEEDS_WORK), session.count(Verdict.FAIL)), GOLD);
        }
        writeLine(LAST_ANSWER_ROW, lastAnswerText(), CYAN);
        writeLine(ERROR_ROW, statusText(), ERROR);
        if (reasonMenuOpen) {
            drawReasonMenu();
            writeLine(HELP_ROW, "Up/Down move  Enter pick  Esc none", LABEL);
            writeLine(HELP_ROW + 1, "Backspace undo", LABEL);
        } else {
            writeLine(HELP_ROW, "Left fail  Up needs work", LABEL);
            writeLine(HELP_ROW + 1, "Right perfect  Backspace undo", LABEL);
        }
        if (comparing()) {
            candidateLayer.setVisible(!reasonMenuOpen);
        }
    }

    private void writeCompareLabels() {
        writeLine(COMPARE_LABEL_ROW, "left: new " + candidate.getCellWidth() + "x" + candidate.getCellHeight()
            + "  right: old " + glyphs.getCellWidth() + "x" + glyphs.getCellHeight(), LABEL);
    }

    private void writeCandidateContext(int codePoint) {
        int[] pattern = {
            'a', '#', codePoint, '#', 'b', ' ',
            BOX_VERTICAL, codePoint, BOX_VERTICAL, ' ',
            MEDIUM_SHADE, codePoint, MEDIUM_SHADE, ' ',
            '0', codePoint, 'O',
        };
        for (int pair = 0; pair < contextPairs; pair++) {
            for (int column = 0; column < candidateGrid.getWidth(); column++) {
                int glyph = pattern[column];
                boolean usable = glyph == codePoint || candidate.hasGlyph(glyph);
                candidateGrid.set(column, pair, usable ? glyph : ' ', COLOR_PAIRS[pair][0], COLOR_PAIRS[pair][1]);
            }
        }
    }

    private OptionalInt displayedCodePoint() {
        return reasonMenuOpen ? session.lastCodePoint() : session.current();
    }

    private String lastAnswerText() {
        if (session.lastVerdict().isEmpty()) {
            return "";
        }
        Verdict verdict = session.lastVerdict().get();
        StringBuilder text = new StringBuilder("Last: ").append(verdict.name());
        if (session.lastReason().isPresent()) {
            text.append("  ").append(reasonName(session.lastReason().get()));
        }
        return text.toString();
    }

    private void writeLine(int row, String text, int foreground) {
        for (int column = 0; column < grid.getWidth(); column++) {
            int index = column - MARGIN_COLUMNS;
            grid.set(column, row, index >= 0 && index < text.length() ? text.charAt(index) : ' ', foreground, CLEAR);
        }
    }

    private void writeContextRows(int codePoint) {
        int[] row = {
            'a', '#', codePoint, '#', 'b', ' ',
            BOX_VERTICAL, codePoint, BOX_VERTICAL, ' ',
            MEDIUM_SHADE, codePoint, MEDIUM_SHADE, ' ',
            '0', codePoint, 'O',
        };
        for (int pair = 0; pair < COLOR_PAIRS.length; pair++) {
            int foreground = COLOR_PAIRS[pair][0];
            int background = COLOR_PAIRS[pair][1];
            int y = CONTEXT_FIRST_ROW + pair;
            for (int column = 0; column < CONTEXT_WIDTH; column++) {
                int index = column - MARGIN_COLUMNS;
                boolean inMargin = index < 0;
                grid.set(column, y, !inMargin && index < row.length ? row[index] : ' ', foreground,
                        inMargin ? CLEAR : background);
            }
        }
    }

    private FileHandle resultsFile() {
        return switch (Gdx.app.getType()) {
            case Android -> Gdx.files.external(RESULTS_FILE);
            default -> Gdx.files.local(DESKTOP_RESULTS_FILE);
        };
    }

    private void loadCandidate() {
        if (candidateLoadAttempted) {
            return;
        }
        candidateLoadAttempted = true;
        if (reviewSheetPath == null) {
            return;
        }
        try {
            ReviewCellSize cell = ReviewCellSize.parse(reviewCell);
            FileHandle image = Gdx.files.absolute(reviewSheetPath);
            FileHandle order = Gdx.files.absolute(image.pathWithoutExtension() + ".txt");
            candidate = TextureGlyphSource.loadSheet(image, order, cell.width(), cell.height(), TransparencyMode.AUTO);
            candidateOrderFile = order;
            layoutCompare();
            candidateGrid = new TerminalGrid(CONTEXT_PATTERN_LENGTH, contextPairs);
            candidateLayer = layers.add(candidateGrid, candidate);
            candidateLayer.setOffset(MARGIN_COLUMNS * glyphs.getCellWidth(),
                (PREVIEW_TOP_ROW + comparePanelRows) * glyphs.getCellHeight());
        } catch (GdxRuntimeException | IllegalArgumentException e) {
            candidateError = "Sheet failed, using default font: " + e.getMessage();
        }
    }

    private void loadReviewList() {
        loadCandidate();
        reviewList = REVIEW_LIST;
        listNotice = candidateError;
        FileHandle listFile = reviewCharsPath != null ? Gdx.files.absolute(reviewCharsPath) : candidateOrderFile;
        if (listFile == null) {
            return;
        }
        GlyphSource reviewed = candidate != null ? candidate : glyphs;
        try {
            ReviewList list = ReviewList.fromText(listFile.readString("UTF-8"), reviewed::hasGlyph);
            reviewList = list.codePoints();
            if (list.missingCount() > 0) {
                listNotice = list.missingCount() + " not in sheet";
            }
        } catch (GdxRuntimeException | IllegalArgumentException e) {
            listNotice = "Review list failed, using demo: " + e.getMessage();
        }
    }

    private void loadResults() {
        errorMessage = "";
        savingBlocked = false;
        loadReviewList();
        session = new ReviewSession(reviewList);
        try {
            FileHandle file = resultsFile();
            if (file.exists()) {
                session = new ReviewSession(reviewList, file.readString("UTF-8"));
            }
        } catch (GdxRuntimeException | IllegalArgumentException e) {
            savingBlocked = true;
            errorMessage = "Load failed, not saving: " + e.getMessage();
        }
    }

    private void saveResults() {
        if (savingBlocked) {
            return;
        }
        try {
            resultsFile().writeString(session.toResultsText(), false, "UTF-8");
            errorMessage = "";
        } catch (GdxRuntimeException e) {
            errorMessage = "Save failed: " + e.getMessage();
        }
    }
}
