package dev.hephaestus.glowcase.client.undo.action;

import net.minecraft.network.chat.Component;
import java.util.List;

public class GroupedUndoAction extends AbstractUndoAction {
	public List<AbstractUndoAction> actions;

	public GroupedUndoAction(Component name) {
		super(name);
	}

	@Override
	public void init() {
		this.redo();
	}

	@Override
	public void undo() {
		for (AbstractUndoAction action : actions) {
			action.undo();
		}
	}

	@Override
	public void redo() {
		for (AbstractUndoAction action : actions) {
			action.redo();
		}
	}
}
