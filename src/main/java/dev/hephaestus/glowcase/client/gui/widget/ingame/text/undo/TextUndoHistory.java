package dev.hephaestus.glowcase.client.gui.widget.ingame.text.undo;

import dev.hephaestus.glowcase.client.gui.widget.ingame.text.FormattableMultilineTextField;
import dev.hephaestus.glowcase.client.gui.widget.ingame.text.undo.action.TextUndoAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Manages undo history specifically for {@link FormattableMultilineTextField}.
 */
public class TextUndoHistory {
	private static final int MAX_UNDO_HISTORY = 32; // Arbitrarily chosen

	public ArrayList<TextUndoAction> past = new ArrayList<>();
	@Nullable
	public TextUndoAction present = null;
	public ArrayList<TextUndoAction> future = new ArrayList<>();

	public void addAction(TextUndoAction action) {
		if (this.present != null) {
			this.past.add(this.present);
		}

		this.present = action;
		this.future.clear();
	}

	public void undo(FormattableMultilineTextField textField) {
		if (this.present == null) return;

		this.present.undo(textField);

		this.future.addFirst(this.present);
		if (!this.past.isEmpty()) {
			this.present = this.past.getLast();
			this.past.removeLast();
		} else {
			this.present = null;
		}
	}

	public void redo(FormattableMultilineTextField textField) {
		if (this.future.isEmpty()) return;

		if (this.present != null) this.past.add(this.present);
		this.present = this.future.getFirst();
		this.future.removeFirst();

		if (this.present != null) {
			this.present.redo(textField);
		}
	}
}
