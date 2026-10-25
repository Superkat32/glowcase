package dev.hephaestus.glowcase.client.undo.action.ingame;

import dev.hephaestus.glowcase.client.undo.action.SingleUndoAction;
import net.minecraft.network.chat.Component;

public class ValueUndoAction<T> extends SingleUndoAction {
	private final T newValue;
	private final T prevValue;
	private final ValueSetter<T> valueSetter;

	public ValueUndoAction(Component name, T newValue, T prevValue, ValueSetter<T> valueSetter) {
		super(name);
		this.newValue = newValue;
		this.prevValue = prevValue;
		this.valueSetter = valueSetter;
	}

	@Override
	public void undo() {
		this.valueSetter.setValue(this.prevValue);
	}

	@Override
	public void redo() {
		this.valueSetter.setValue(this.newValue);
	}

	public static interface ValueSetter<T> {
		void setValue(T value);
	}
}
