package dev.hephaestus.glowcase.client.undo.action;

import dev.hephaestus.glowcase.client.gui.widget.ingame.text.FormattableMultilineTextField;

public interface TextUndoAction {
	void undo(FormattableMultilineTextField textField);
	void redo(FormattableMultilineTextField textField);
}
