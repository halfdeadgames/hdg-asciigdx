package com.github.halfdeadgames.asciigdx.workbench.inputscript;

public interface ScriptListener {

  void captureRequested(String name);

  void quitRequested();
}
