package dev.hephaestus.glowcase.client.undo.action;

import net.minecraft.network.chat.Component;
import java.util.List;

public class GroupedUndoAction extends AbstractUndoAction {
	public List<SingleUndoAction> actions;

	public GroupedUndoAction(Component name) {
		super(name);
	}

	@Override
	public void undo() {
		for (SingleUndoAction action : actions) {
			action.undo();
		}
	}

	@Override
	public void redo() {
		for (SingleUndoAction action : actions) {
			action.redo();
		}
	}
}
