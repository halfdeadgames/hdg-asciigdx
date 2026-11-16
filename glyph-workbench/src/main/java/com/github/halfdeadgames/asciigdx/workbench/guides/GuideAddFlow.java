package com.github.halfdeadgames.asciigdx.workbench.guides;

import java.util.function.Consumer;

/**
 * Adding a guide to the list in two steps: pick the kind, then type its label. Nothing is kept
 * until the label is accepted.
 */
final class GuideAddFlow {

    /** The kinds the picker offers, in button order. */
    enum Kind {
        SOLID_ACROSS("SOLID ACROSS", GuideListModel::addRow),
        SOLID_DOWN("SOLID DOWN", GuideListModel::addColumn),
        DOTTED_ACROSS("DOTTED ACROSS", GuideListModel::addBar),
        DOTTED_DOWN("DOTTED DOWN", GuideListModel::addColumnBar);

        private final String label;
        private final Consumer<GuideListModel> adder;

        Kind(String label, Consumer<GuideListModel> adder) {
            this.label = label;
            this.adder = adder;
        }

        String label() {
            return label;
        }
    }

    enum State {CLOSED, PICKING, NAMING}

    private final GuideListModel model;
    private State state = State.CLOSED;

    GuideAddFlow(GuideListModel model) {
        this.model = model;
    }

    State state() {
        return state;
    }

    boolean isPicking() {
        return state == State.PICKING;
    }

    boolean isNaming() {
        return state == State.NAMING;
    }

    /** Opens the picker; ignored while an add is already in progress. */
    void start() {
        if (state == State.CLOSED && !model.isRenaming()) {
            state = State.PICKING;
        }
    }

    /** Adds the chosen kind and starts its label; a call outside the picker does nothing. */
    void pick(Kind kind) {
        if (state != State.PICKING) {
            return;
        }
        model.beginAdding(() -> kind.adder.accept(model));
        state = State.NAMING;
    }

    /** Closes the picker, or drops the guide being named. */
    void cancel() {
        if (state == State.NAMING) {
            model.cancelRename();
        }
        state = State.CLOSED;
    }

    boolean canAccept() {
        return state == State.NAMING && model.canAcceptRename();
    }

    /** Keeps the new guide with the typed label; an empty label is refused and naming goes on. */
    void accept() {
        if (!canAccept()) {
            return;
        }
        model.acceptRename();
        state = State.CLOSED;
    }
}
