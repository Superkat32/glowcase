package dev.hephaestus.glowcase.client.undo.action;

import net.minecraft.network.chat.Component;

public abstract class SingleUndoAction extends AbstractUndoAction {
	public SingleUndoAction(Component name) {
		super(name);
	}
}
