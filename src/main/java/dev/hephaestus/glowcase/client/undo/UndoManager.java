package dev.hephaestus.glowcase.client.undo;

import dev.hephaestus.glowcase.client.undo.action.AbstractUndoAction;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class UndoManager {
	private static final int MAX_UNDO_HISTORY = 32; // Arbitrarily chosen
	public final Screen screen;
	public boolean activelyChanging = false;

	public ArrayList<AbstractUndoAction> past = new ArrayList<>();
	@Nullable
	public AbstractUndoAction present = null;
	public ArrayList<AbstractUndoAction> future = new ArrayList<>();

	public UndoManager(Screen screen) {
		this.screen = screen;
	}

	public void addAction(AbstractUndoAction action) {
		if (this.activelyChanging) return;

		if (this.present != null) {
			this.past.add(this.present);
		}

		this.present = action;
		this.future.clear();
	}

	public void undo() {
		if (this.present == null) return;
		this.activelyChanging = true;

		this.present.undo();

		this.future.addFirst(this.present);
		if (!this.past.isEmpty()) {
			this.present = this.past.getLast();
			this.past.removeLast();
		} else {
			this.present = null;
		}

		this.activelyChanging = false;
	}

	public void redo() {
		if (this.future.isEmpty()) return;
		this.activelyChanging = true;

		if (this.present != null) this.past.add(this.present);
		this.present = this.future.getFirst();
		this.future.removeFirst();

		if (this.present != null) {
			this.present.redo();
		}

		this.activelyChanging = false;
	}
}
