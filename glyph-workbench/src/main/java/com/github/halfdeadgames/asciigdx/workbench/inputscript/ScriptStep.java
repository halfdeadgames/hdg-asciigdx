package com.github.halfdeadgames.asciigdx.workbench.inputscript;

public sealed interface ScriptStep {

  record WaitSeconds(float seconds) implements ScriptStep {}

  record WaitFrames(int frames) implements ScriptStep {}

  record Press(int keyCode) implements ScriptStep {}

  record Hold(int keyCode) implements ScriptStep {}

  record Release(int keyCode) implements ScriptStep {}

  record TypeText(String text) implements ScriptStep {}

  record MoveMouse(int x, int y) implements ScriptStep {}

  record Click(int x, int y, int button) implements ScriptStep {}

  record MouseDown(int button) implements ScriptStep {}

  record MouseUp(int button) implements ScriptStep {}

  record Wheel(int ticks) implements ScriptStep {}

  record Capture(String name) implements ScriptStep {}

  record Quit() implements ScriptStep {}
}
