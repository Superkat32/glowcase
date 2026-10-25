package dev.hephaestus.glowcase.client.undo.action;

import net.minecraft.network.chat.Component;

public class SingleUndoAction<T> extends AbstractUndoAction {
	private final T newValue;
	private final T prevValue;
	private final ValueSetter<T> valueSetter;

	public SingleUndoAction(Component name, T newValue, T prevValue, ValueSetter<T> valueSetter) {
		super(name);
		this.newValue = newValue;
		this.prevValue = prevValue;
		this.valueSetter = valueSetter;
	}

	@Override
	public void init() {
		this.redo();
	}

	@Override
	public void undo() {
		this.valueSetter.setValue(this.prevValue);
	}

	@Override
	public void redo() {
		this.valueSetter.setValue(this.newValue);
	}

	public interface ValueSetter<T> {
		void setValue(T value);
	}
}
