package dev.hephaestus.glowcase.client.gui.widget.ingame.text.undo.action;

import dev.hephaestus.glowcase.client.gui.widget.ingame.text.FormattableMultilineTextField;

public class InsertTextUndoAction implements TextUndoAction {
	public final String input;
	public final String selected;
	public final int position;

	public InsertTextUndoAction(String input, String selected, int position) {
		this.input = input;
		this.selected = selected;
		this.position = position;
	}

	@Override
	public void undo(FormattableMultilineTextField textField) {
		textField.deleteTextAt(this.input.length(), this.position);
		if (!this.selected.isEmpty()) textField.insertTextAt(this.selected, this.position);
	}

	@Override
	public void redo(FormattableMultilineTextField textField) {
		if (!this.selected.isEmpty()) textField.deleteTextAt(this.selected.length(), this.position);
		textField.insertTextAt(this.input, this.position);
	}
}
