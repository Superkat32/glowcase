package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hephaestus.glowcase.client.undo.UndoManager;
import dev.hephaestus.glowcase.client.undo.action.AbstractUndoAction;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;

public interface UndoHistoryIncludedScreen {
	Identifier UNDO_BUTTON_TEXTURE = Identifier.withDefaultNamespace("spectator/scroll_left");
	Identifier REDO_BUTTON_TEXTURE = Identifier.withDefaultNamespace("spectator/scroll_right");

	void updateWidgetsFromValues();

	UndoManager getUndoManager();

	default void runUndoableAction(AbstractUndoAction action) {
		action.init();
		this.getUndoManager().addAction(action);
	}

	default void undo() {
		this.getUndoManager().undo();
		this.updateWidgetsFromValues();
	}

	default void redo() {
		this.getUndoManager().redo();
		this.updateWidgetsFromValues();
	}

//	default void initUndoRedoButtons(int x, int y, int padding) {
//		this.initUndoRedoButtons(x, y, padding, 20);
//	}
//
//	default void initUndoRedoButtons(int x, int y, int padding, int buttonSize) {
//		int buttonX = x;
//		int buttonY = y;
//		int shiftX = buttonSize + padding;
//
//		IconButtonWidget undoButton = IconButtonWidget.builder(UNDO_BUTTON_TEXTURE, button -> this.undo())
//			.dimensions(buttonX, buttonY, buttonSize, buttonSize, 16, 16).build();
//
//		IconButtonWidget redoButton = IconButtonWidget.builder(REDO_BUTTON_TEXTURE, button -> this.undo())
//			.dimensions(buttonX, buttonY, buttonSize, buttonSize, 16, 16).build();
//	}

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
