package com.github.halfdeadgames.asciigdx.grid;

@FunctionalInterface
public interface DirtyCellVisitor {

    void visit(int x, int y);
}
