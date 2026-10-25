package dev.hephaestus.glowcase.client.undo.action.ingame;

import dev.hephaestus.glowcase.client.undo.action.SingleUndoAction;
import net.minecraft.network.chat.Component;

public class BooleanUndoAction extends SingleUndoAction {
	private final boolean value;
	private final BooleanSetter setter;
	public BooleanUndoAction(Component name, boolean value, BooleanSetter setter) {
		super(name);
		this.value = value;
		this.setter = setter;
	}

	@Override
	public void undo() {
		this.setter.setBoolean(!this.value);
	}

	@Override
	public void redo() {
		this.setter.setBoolean(this.value);
	}

	public static interface BooleanSetter {
		void setBoolean(boolean value);
	}
}
