package com.github.halfdeadgames.asciigdx.skindesigner.files;

/** Asks the user whether the bundled default skin's file may be overwritten. */
@FunctionalInterface
public interface BundledOverwriteConfirmation {

    /**
     * Asks the question, possibly showing a dialog, and later calls exactly one of the callbacks.
     *
     * @param confirmed called when the user agrees to overwrite
     * @param declined called when the user does not agree
     */
    void ask(Runnable confirmed, Runnable declined);
}
