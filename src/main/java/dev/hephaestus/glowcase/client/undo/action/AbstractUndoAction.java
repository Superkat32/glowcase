package dev.hephaestus.glowcase.client.undo.action;

import net.minecraft.network.chat.Component;

public abstract class AbstractUndoAction {
	public final Component name;

	public AbstractUndoAction(Component name) {
		this.name = name;
	}

	public abstract void init();

	public abstract void undo();

	public abstract void redo();

	public Component getName() {
		return this.name;
	}
}
