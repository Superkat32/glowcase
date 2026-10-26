package dev.hephaestus.glowcase.client.undo.action;

import net.minecraft.network.chat.Component;
import java.util.List;

public class GroupedUndoAction extends AbstractUndoAction {
	public final List<AbstractUndoAction> actions;

	public GroupedUndoAction(Component name, List<AbstractUndoAction> actions) {
		super(name);
		this.actions = actions;
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
