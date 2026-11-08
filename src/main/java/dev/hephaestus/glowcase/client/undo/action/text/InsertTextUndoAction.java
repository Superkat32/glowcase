package dev.hephaestus.glowcase.client.undo.action.text;

import dev.hephaestus.glowcase.client.gui.widget.ingame.text.FormattableMultilineTextField;
import dev.hephaestus.glowcase.client.undo.action.AbstractUndoAction;
import net.minecraft.network.chat.Component;

public class InsertTextUndoAction extends AbstractUndoAction {
	public final FormattableMultilineTextField textField;
	public final String input;
	public final int position;
	public InsertTextUndoAction(FormattableMultilineTextField textField, String input, int position) {
		super(
			Component.literal("Insert '").append(input).append("'")
		);
		this.textField = textField;
		this.input = input;
		this.position = position;
	}

	@Override
	public void init() {
		// NO-OP
	}

	@Override
	public void undo() {
		this.textField.deleteTextAt(this.input.length(), this.position);
	}

	@Override
	public void redo() {
		this.textField.insertTextAt(this.input, this.position);
	}
}
