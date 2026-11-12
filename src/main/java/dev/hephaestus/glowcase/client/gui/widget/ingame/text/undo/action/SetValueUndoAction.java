package dev.hephaestus.glowcase.client.gui.widget.ingame.text.undo.action;

import dev.hephaestus.glowcase.client.gui.widget.ingame.text.FormattableMultilineTextField;

public class SetValueUndoAction implements TextUndoAction {
	public final String newValue;
	public final String oldValue;

	public SetValueUndoAction(String newValue, String oldValue) {
		this.newValue = newValue;
		this.oldValue = oldValue;
	}

	@Override
	public void undo(FormattableMultilineTextField textField) {
		textField.setTrackingInputs(false);
		textField.setValue(this.oldValue);
		textField.setTrackingInputs(true);
	}

	@Override
	public void redo(FormattableMultilineTextField textField) {
		textField.setTrackingInputs(false);
		textField.setValue(this.newValue);
		textField.setTrackingInputs(true);
	}
}
