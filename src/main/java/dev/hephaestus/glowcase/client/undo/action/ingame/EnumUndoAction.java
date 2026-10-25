package dev.hephaestus.glowcase.client.undo.action.ingame;

import dev.hephaestus.glowcase.client.undo.action.SingleUndoAction;
import net.minecraft.network.chat.Component;

public class EnumUndoAction<T> extends SingleUndoAction {
	private final T newValue;
	private final T prevValue;
	private final EnumSetter<T> setter;
	public EnumUndoAction(Component name, T newValue, T prevValue, EnumSetter<T> setter) {
		super(name);
		this.newValue = newValue;
		this.prevValue = prevValue;
		this.setter = setter;
	}

	@Override
	public void undo() {
		this.setter.setEnum(this.prevValue);
	}

	@Override
	public void redo() {
		this.setter.setEnum(this.newValue);
	}

	public static interface EnumSetter<T> {
		void setEnum(T value);
	}
}
