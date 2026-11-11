package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import com.github.halfdeadgames.asciigdx.workbench.ui.ArrowRepeats;
import com.github.halfdeadgames.asciigdx.workbench.ui.Blink;
import com.github.halfdeadgames.asciigdx.workbench.ui.BounceScroll;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.Keys;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.StatusBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.List;

/** The guide list panel of the paint screen: lists, edits, renames, and saves the guides of one font size. */
public final class PaintGuideListPanel {

    private static final String TAG = "Paint";
    private static final List<StatusBar.Key> LIST_KEYS = List.of(
        new StatusBar.Key("ENTER", "SAVE"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> RENAME_KEYS = List.of(
        new StatusBar.Key("ENTER", "OK"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> PICK_KIND_KEYS = List.of(
        new StatusBar.Key("1-4", "PICK"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> NEW_GUIDE_NAME_KEYS = List.of(
        new StatusBar.Key("ENTER", "ADD GUIDE"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> GROUP_LIST_KEYS = List.of(
        new StatusBar.Key("ENTER", "DONE"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<String[]> EDIT_LEGEND = List.of(
        new String[] {"ARROWS", "PICK"}, new String[] {"[ ]", "MOVE"},
        new String[] {"SH+[ ]", "SIZE"},
        new String[] {"A", "ADD"}, new String[] {"DEL", "REMOVE"},
        new String[] {"G", "GROUPS"}, new String[] {"L", "RENAME"});
    private static final List<String[]> GROUP_LEGEND = List.of(
        new String[] {"ARROWS", "PICK"}, new String[] {"SPACE", "TOGGLE"}, new String[] {"CLICK", "TOGGLE"},
        new String[] {"NONE = ALL GLYPHS"});
    private static final int LEGEND_ACTION_COLUMN = 8;
    private static final int LEGEND_MIN_LIST_ROWS = 8;
    private static final int GROUP_LEGEND_MIN_LIST_ROWS = 4;
    private static final String KEY_SELECTED_GROUPS = "guideGroups";
    private static final String NO_GROUPS_MESSAGE = "This font has no groups: guides show on all glyphs";
    private static final int[] KIND_KEYS = {Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4};
    private static final int[] KIND_NUMPAD_KEYS = {
        Input.Keys.NUMPAD_1, Input.Keys.NUMPAD_2, Input.Keys.NUMPAD_3, Input.Keys.NUMPAD_4};
    // The L that starts a rename can also arrive as a typed character a frame later; it must not enter the text.
    private static final int RENAME_KEY_SWALLOW_FRAMES = 2;

    private final Painter painter;
    private final MessageStrip messages;
    private PaintGuidesFile guidesFile;
    private final FontEditor editor;
    private final Host host;
    private final ArrowRepeats arrowRepeats;
    private final KeyRepeat repeatLabelBackspace = new KeyRepeat();
    private final Runnable backspaceLabel = this::backspaceInLabel;
    private boolean labelBackspaced;

    private GuideListModel guideList;
    private GuideAddFlow addFlow;
    private GuideGroupList groupList;
    private PaintGuides guidesBeforeGroups;
    private boolean open;
    private float seconds;
    private String title = "GUIDES";
    private int renameKeySwallowFrames;
    private final Blink cursorBlink = new Blink();
    private String labelBeforeRename;

    /** What the panel needs from the screen it is shown on. */
    public interface Host {

        /** The guides as shown now, which the list is rebuilt from when it closes. */
        PaintGuides currentGuides();

        /** Told about every guide change, so the grid and the rule warnings drop what they cached. */
        void showGuides(PaintGuides guides);

        /** Ends a pixel stroke in progress when the list opens. */
        void endStroke();

        void clearWheelTicks();

        /** Whether the pointer is over the area the list is drawn in. */
        boolean pointerOverList();

        /** The rules of the font being painted. A reload cannot happen while the panel is open, so it stays valid. */
        RulesFile currentRules();
    }

    public PaintGuideListPanel(Painter painter, MessageStrip messages, PaintGuidesFile guidesFile, FontEditor editor,
                               ArrowRepeats arrowRepeats, Host host) {
        this.painter = painter;
        this.messages = messages;
        this.guidesFile = guidesFile;
        this.editor = editor;
        this.arrowRepeats = arrowRepeats;
        this.host = host;
        this.guideList = newModel();
    }

    /** Saves to the given file from now on; call after the host shows the guides that file holds. */
    public void useFile(PaintGuidesFile reloadedFile) {
        guidesFile = reloadedFile;
        guideList = newModel();
    }

    // Keeps the backspace repeater tracking the real key without producing deletions.
    public void swallowInput() {
        repeatLabelBackspace.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), 0f);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isRenaming() {
        return open && guideList.isRenaming();
    }

    public PaintGuides.Listed highlightedGuide() {
        return open ? guideList.selectedGuideForDisplay() : null;
    }

    private boolean isPicking() {
        return open && addFlow.isPicking();
    }

    public List<StatusBar.Key> statusKeys() {
        if (groupList != null) {
            return GROUP_LIST_KEYS;
        }
        if (addFlow.isPicking()) {
            return PICK_KIND_KEYS;
        }
        if (addFlow.isNaming()) {
            return NEW_GUIDE_NAME_KEYS;
        }
        return guideList.isRenaming() ? RENAME_KEYS : LIST_KEYS;
    }

    /** Draws the guide kind picker over the screen while it is open. */
    public void drawPicker(int columns, int rows, CellRect area) {
        if (isPicking()) {
            GuideKindPicker.draw(painter, columns, rows, area);
        }
    }

    public void open() {
        messages.nextAction();
        host.endStroke();
        open = true;
        seconds = 0f;
        refreshInfo();
    }

    public void openForCapture(boolean deriveIdle) {
        int guideRequest = DevCapture.guidesRequest();
        if (guideRequest >= 0 && deriveIdle) {
            guideList.select(guideRequest);
            open();
        }
    }

    // Reads the typed character only while a label is being renamed; the caller decides whether the input is free.
    public void typeChar(char character) {
        boolean startingKey = character == 'l' || character == 'L' || (character >= '1' && character <= '4');
        if (renameKeySwallowFrames > 0 && startingKey) {
            return;
        }
        guideList.typeChar(character);
        showTypedLabel();
    }

    private void showTypedLabel() {
        if (!guideList.isRenaming()) {
            return;
        }
        cursorBlink.restart();
        host.showGuides(guideList.guidesForDisplay());
    }

    // Esc and F2 cancel the edits here, so Esc never reaches the unsaved-changes prompt.
    public void handle(float delta, int wheelTicks) {
        if (groupList != null) {
            handleGroups(delta, wheelTicks);
            return;
        }
        if (addFlow.isPicking()) {
            handlePicker();
            return;
        }
        if (guideList.isRenaming()) {
            handleRename(delta);
            return;
        }
        repeatLabelBackspace.update(false, 0f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            host.showGuides(guideList.original());
            close();
            return;
        }
        if (Keys.enterJustPressed()) {
            save();
            return;
        }
        moveSelection(selectionStep(delta, wheelTicks));
        if (handleEditKeys()) {
            messages.nextAction();
            host.showGuides(guideList.guides());
            refreshInfo();
        }
    }

    // Rows to move by this frame from the held arrow keys and the wheel over the list.
    private int selectionStep(float delta, int wheelTicks) {
        int step = arrowRepeats.down().update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta)
            - arrowRepeats.up().update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        if (wheelTicks != 0) {
            if (host.pointerOverList()) {
                step += wheelTicks;
            }
            host.clearWheelTicks();
        }
        return step;
    }

    // Returning right after Enter, G, or Esc keeps that key from acting twice in the same frame.
    private void handleGroups(float delta, int wheelTicks) {
        repeatLabelBackspace.update(false, 0f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            cancelGroups();
            return;
        }
        if (Keys.enterJustPressed() || (Gdx.input.isKeyJustPressed(Input.Keys.G) && !Keys.controlDown())) {
            closeGroups();
            return;
        }
        groupList.move(selectionStep(delta, wheelTicks));
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            toggleSelectedGroup();
        }
    }

    private void openGroups() {
        PaintGuides.Listed guide = guideList.selectedGuide();
        if (guide == null) {
            return;
        }
        messages.nextAction();
        List<String> definedGroups = host.currentRules().groupNames();
        if (!GuideGroupList.canOpen(definedGroups)) {
            messages.ok(NO_GROUPS_MESSAGE);
            return;
        }
        guidesBeforeGroups = guideList.guides();
        groupList = new GuideGroupList(definedGroups, guide.groups());
        refreshTitle();
    }

    private void toggleSelectedGroup() {
        guideList.toggleGroup(groupList.selectedName(), host.currentRules().groupNames());
        groupList.showGroups(guideList.selectedGuide().groups());
        messages.nextAction();
        host.showGuides(guideList.guides());
        refreshGroupsMessage();
    }

    private void cancelGroups() {
        guideList.restoreGuides(guidesBeforeGroups);
        host.showGuides(guideList.guides());
        closeGroups();
    }

    private void closeGroups() {
        groupList = null;
        guidesBeforeGroups = null;
        host.clearWheelTicks();
        refreshInfo();
    }

    // One row per text line; only the selected row bounces when it is too long, the others are cut.
    public void draw(CellRect area, int cellPixels) {
        Panel.draw(painter, area, title, true);
        seconds += Gdx.graphics.getDeltaTime();
        cursorBlink.advance(Gdx.graphics.getDeltaTime());
        int visible = visibleRows(area);
        int charsPerLine = Math.max(1, area.width() - 2 - 2 * Panel.TEXT_PADDING);
        if (legendShown(area) && !guideList.isRenaming()) {
            drawLegend(area, visible, cellPixels);
        }
        int first = firstVisibleRow(visible);
        int shown = Math.min(visible, rowCount() - first);
        for (int i = 0; i < shown; i++) {
            int index = first + i;
            String text = rowText(index);
            int top = (area.y() + 1 + i) * cellPixels;
            int textX = (area.x() + 1 + Panel.TEXT_PADDING) * cellPixels;
            if (index == selectedRow()) {
                painter.fill(UiPalette.KEYBG, (area.x() + 1) * cellPixels, top, (area.width() - 2) * cellPixels,
                    cellPixels);
                painter.text("►", (area.x() + 1) * cellPixels, top, UiPalette.ACCENT);
                painter.text(BounceScroll.window(text, charsPerLine, seconds), textX, top, UiPalette.ACCENT);
            } else {
                painter.text(text.length() > charsPerLine ? text.substring(0, charsPerLine) : text, textX, top, UiPalette.TEXT);
            }
        }
    }

    /**
     * Selects the guide whose row is at the clicked cell, like arrowing there; a click on the selected guide opens its
     * groups. While the group list is open a click on a group toggles it. Other clicks change nothing.
     */
    public void click(CellRect area, CellRect pickerArea, int cellX, int cellY) {
        if (!open) {
            return;
        }
        if (addFlow.isPicking()) {
            pickKind(GuideKindPicker.kindAt(pickerArea, cellX, cellY));
            return;
        }
        if (guideList.isRenaming()) {
            return;
        }
        int visible = visibleRows(area);
        int index = GuideListRows.indexAt(area, visible, firstVisibleRow(visible), rowCount(), cellX, cellY);
        if (groupList != null) {
            clickGroup(index);
            return;
        }
        clickGuide(index);
    }

    private void clickGroup(int index) {
        if (index == GuideListRows.NONE) {
            return;
        }
        groupList.select(index);
        toggleSelectedGroup();
    }

    private void clickGuide(int index) {
        switch (GuideListRows.clickOutcome(index, guideList.selectedIndex())) {
            case SELECT -> moveSelection(index - guideList.selectedIndex());
            case OPEN_GROUPS -> openGroups();
            case NONE -> { }
        }
    }

    private int rowCount() {
        return groupList != null ? groupList.size() : guideList.size();
    }

    private int selectedRow() {
        return groupList != null ? groupList.selectedIndex() : guideList.selectedIndex();
    }

    private int firstVisibleRow(int visible) {
        return groupList != null ? groupList.firstVisible(visible) : guideList.firstVisible(visible);
    }

    private String rowText(int index) {
        return groupList != null ? groupList.rowText(index) : guideList.row(index, cursorBlink.isOn()).text();
    }

    private List<String[]> legend() {
        return groupList != null ? GROUP_LEGEND : EDIT_LEGEND;
    }

    private boolean legendShown(CellRect area) {
        int minListRows = groupList != null ? GROUP_LEGEND_MIN_LIST_ROWS : LEGEND_MIN_LIST_ROWS;
        return GuideListRows.visibleRows(area, legend().size()) >= minListRows;
    }

    private int visibleRows(CellRect area) {
        return GuideListRows.visibleRows(area, legendShown(area) ? legend().size() : 0);
    }

    private void drawLegend(CellRect area, int rowsAbove, int cellPixels) {
        int textX = (area.x() + 1 + Panel.TEXT_PADDING) * cellPixels;
        List<String[]> entries = legend();
        for (int i = 0; i < entries.size(); i++) {
            String[] entry = entries.get(i);
            int top = (area.y() + 1 + rowsAbove + i) * cellPixels;
            if (entry.length == 1) {
                painter.text(entry[0], textX, top, UiPalette.TEXT);
                continue;
            }
            painter.text(entry[0], textX, top, UiPalette.TITLE);
            painter.text(entry[1], textX + LEGEND_ACTION_COLUMN * cellPixels, top, UiPalette.TEXT);
        }
    }

    private GuideListModel newModel() {
        GuideListModel model = new GuideListModel(host.currentGuides(), editor.cellWidth(), editor.cellHeight());
        addFlow = new GuideAddFlow(model);
        return model;
    }

    private void handlePicker() {
        host.clearWheelTicks();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            addFlow.cancel();
            return;
        }
        pickKind(GuideKindPicker.kindForNumber(pressedNumber()));
    }

    // 1-based number of the digit key (top row or numpad) just pressed, or 0.
    private static int pressedNumber() {
        for (int i = 0; i < KIND_KEYS.length; i++) {
            if (Gdx.input.isKeyJustPressed(KIND_KEYS[i]) || Gdx.input.isKeyJustPressed(KIND_NUMPAD_KEYS[i])) {
                return i + 1;
            }
        }
        return 0;
    }

    private void pickKind(GuideAddFlow.Kind kind) {
        if (kind == null) {
            return;
        }
        addFlow.pick(kind);
        messages.nextAction();
        renameKeySwallowFrames = RENAME_KEY_SWALLOW_FRAMES;
        cursorBlink.restart();
        host.showGuides(guideList.guidesForDisplay());
        refreshInfo();
    }

    private void refreshInfo() {
        refreshTitle();
        refreshGroupsMessage();
    }

    private void refreshGroupsMessage() {
        PaintGuides.Listed selectedGuide = guideList.selectedGuide();
        if (!open || selectedGuide == null) {
            messages.clearCondition(KEY_SELECTED_GROUPS);
            return;
        }
        messages.setCondition(KEY_SELECTED_GROUPS, false,
            GuideGroups.summary(selectedGuide.label(), selectedGuide.groups()));
    }

    private void refreshTitle() {
        seconds = 0f;
        if (groupList != null) {
            title = groupsTitle(guideList.selectedGuide().label());
            return;
        }
        boolean renamingExisting = guideList.isRenaming() && !addFlow.isNaming();
        title = titleFor(addFlow.isNaming(), renamingExisting ? labelBeforeRename : null,
            guideList.selectedIndex(), guideList.size());
    }

    static String groupsTitle(String guideLabel) {
        return "GROUPS " + guideLabel;
    }

    /** The title while typing a label wins over the list title; {@code renamedLabel} is null when no rename is going on. */
    static String titleFor(boolean namingNewGuide, String renamedLabel, int selectedIndex, int guideCount) {
        if (namingNewGuide) {
            return "NAME NEW GUIDE";
        }
        if (renamedLabel != null) {
            return "RENAME " + renamedLabel;
        }
        return titleFor(selectedIndex, guideCount);
    }

    static String titleFor(int selectedIndex, int guideCount) {
        if (guideCount == 0) {
            return "GUIDES (NONE)";
        }
        return "GUIDES " + (selectedIndex + 1) + "/" + guideCount;
    }

    private void close() {
        open = false;
        groupList = null;
        guidesBeforeGroups = null;
        messages.clearCondition(KEY_SELECTED_GROUPS);
        host.clearWheelTicks();
        guideList = newModel();
    }

    private void moveSelection(int step) {
        if (step == 0) {
            return;
        }
        int before = guideList.selectedIndex();
        guideList.move(step);
        if (guideList.selectedIndex() != before) {
            refreshInfo();
        }
    }

    // Writes nothing when the list is unchanged, so comments in the file survive opening and closing the list.
    private void save() {
        messages.nextAction();
        if (!guideList.isChanged()) {
            messages.ok("Guides unchanged");
            close();
            return;
        }
        if (guidesFile.isUnreadable()) {
            messages.warn("Not saved: " + guidesFile.fileName() + " could not be read and would be overwritten");
            return;
        }
        try {
            guidesFile.save(guideList.guides());
            messages.ok("Saved " + guidesFile.fileName());
            close();
        } catch (java.io.IOException e) {
            messages.warn("Save failed: see log");
            Gdx.app.error(TAG, "Cannot write " + guidesFile.displayPath() + ": " + e.getMessage());
        }
    }

    // Returns whether a key changed the guides.
    private boolean handleEditKeys() {
        if (Keys.controlDown()) {
            return false;
        }
        boolean edited = true;
        boolean shift = shiftDown();
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            edited = shift ? resizeSelected(-1) : moveSelected(-1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            edited = shift ? resizeSelected(1) : moveSelected(1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            addFlow.start();
            messages.nextAction();
            edited = false;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            guideList.deleteSelected();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            openGroups();
            edited = false;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            if (guideList.canRename()) {
                labelBeforeRename = guideList.selectedGuide().label();
                guideList.beginRename();
                messages.nextAction();
                renameKeySwallowFrames = RENAME_KEY_SWALLOW_FRAMES;
                cursorBlink.restart();
                refreshTitle();
            }
            edited = false;
        } else {
            edited = false;
        }
        return edited;
    }

    private static boolean shiftDown() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

    private boolean moveSelected(int delta) {
        guideList.adjust(delta);
        return true;
    }

    // Returns whether the size changed; a solid guide shows a warning, and a size limit changes nothing silently.
    private boolean resizeSelected(int delta) {
        if (!guideList.canResizeSelected()) {
            messages.warn("Only dotted guides have a size");
            return false;
        }
        PaintGuides before = guideList.guides();
        guideList.resizeSelected(delta);
        return guideList.guides() != before;
    }

    // Only Enter, Esc, and Backspace act here; typed characters arrive through keyTyped. Returning right after
    // Enter or Esc keeps that key from also saving or closing the list in the same frame.
    private void handleRename(float delta) {
        host.clearWheelTicks();
        renameKeySwallowFrames = Math.max(0, renameKeySwallowFrames - 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            cancelNaming();
            return;
        }
        if (Keys.enterJustPressed()) {
            acceptRename();
            return;
        }
        Keys.repeatBackspace(repeatLabelBackspace, delta, backspaceLabel);
        if (labelBackspaced) {
            labelBackspaced = false;
            showTypedLabel();
        }
    }

    private void backspaceInLabel() {
        guideList.backspace();
        labelBackspaced = true;
    }

    private void cancelNaming() {
        boolean wasAdding = addFlow.isNaming();
        addFlow.cancel();
        if (!wasAdding) {
            guideList.cancelRename();
        }
        host.showGuides(guideList.guides());
        refreshInfo();
    }

    private void acceptRename() {
        if (!guideList.canAcceptRename()) {
            messages.warn("A label needs at least one character");
            return;
        }
        if (addFlow.isNaming()) {
            addFlow.accept();
        } else {
            guideList.acceptRename();
        }
        messages.nextAction();
        host.showGuides(guideList.guides());
        refreshInfo();
    }
}
