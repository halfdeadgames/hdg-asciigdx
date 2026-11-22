package com.github.halfdeadgames.asciigdx.skindesigner.files;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** The skin files the user can pick from: recent ones first, then the detected ones. */
public final class KnownSkins {
    private final RecentSkins recentSkins;
    private final SkinDetector detector;
    private final File workingFolder;

    /** Creates the list from the recent files and the skins found in the working folder. */
    public KnownSkins(RecentSkins recentSkins, SkinDetector detector, File workingFolder) {
        this.recentSkins = recentSkins;
        this.detector = detector;
        this.workingFolder = workingFolder;
    }

    /** Returns the recent files, then the detected files that are not recent; no duplicates. */
    public List<File> listAll() {
        List<File> all = new ArrayList<>(recentSkins.listExisting());
        for (File detected : detector.detect(workingFolder)) {
            if (!all.contains(detected)) {
                all.add(detected);
            }
        }
        return all;
    }

    /** Records a file as the most recently used. */
    public void remember(File file) {
        recentSkins.remember(file);
    }
}
