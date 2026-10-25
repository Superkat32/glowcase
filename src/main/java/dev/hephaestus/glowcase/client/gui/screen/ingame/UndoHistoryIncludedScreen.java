package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hephaestus.glowcase.client.undo.UndoManager;
import dev.hephaestus.glowcase.client.undo.action.AbstractUndoAction;
import net.minecraft.client.input.KeyEvent;

public interface UndoHistoryIncludedScreen {

	UndoManager getUndoManager();

	default void addUndoAction(AbstractUndoAction action) {
		this.getUndoManager().addAction(action);
	}

	default void undo() {
		this.getUndoManager().undo();
	}

	default void redo() {
		this.getUndoManager().redo();
	}

	default boolean keyPressedUndoHistory(KeyEvent event) {
		if (!event.hasControlDown()) return false;

		int keyCode = event.key();
		switch (keyCode) {
			case InputConstants.KEY_Z -> {
				if (event.hasShiftDown()) {
					this.redo(); // Allow ctrl+shift+z for redo
				} else {
					this.undo(); // Use ctrl+z for undo
				}
				return true;
			}
			case InputConstants.KEY_R -> {
				this.redo(); // Also use ctrl+r for redo
				return true;
			}
		}

		return false;
	}

}
