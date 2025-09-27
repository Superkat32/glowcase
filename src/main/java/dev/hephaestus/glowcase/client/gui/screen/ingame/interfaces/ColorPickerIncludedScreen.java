package dev.hephaestus.glowcase.client.gui.screen.ingame.interfaces;

import dev.hephaestus.glowcase.client.gui.widget.ingame.color.ColorPickerWidget;

/**
 * A screen which has {@link ColorPickerWidget} support. <br><br>
 * Note that each screen only has a <b>single instance of a color picker!</b> That one color picker is moved/changed by other widgets as needed(e.g. {@link dev.hephaestus.glowcase.client.gui.widget.ingame.color.ColorFieldWidget}).<br><br>
 * The color picker may take advantage of methods from {@link TextFormattingScreen} (e.g. inserting color/formatting tags), which is why it is extended here to guarantee those methods are available. If you don't need a SelectionManager, simply return null on it.
 * @see ColorPickerWidget
 * @see TextFormattingScreen
 * @author Superkat32
 */
public interface ColorPickerIncludedScreen extends TextFormattingScreen {
	ColorPickerWidget getColorPicker();

	default void toggleColorPicker(boolean activate) {
		this.getColorPicker().toggle(activate);
	}

	default ColorPickerWidget createColorPicker() {
		if(this.getColorPicker() != null) {
			// fix bug where resizing/moving window may duplicate color pickers
			this.getColorPicker().toggle(false);
		}
		return ColorPickerWidget.Builder.create(this).build();
	}

	default void tryClosingColorPicker(double mouseX, double mouseY) {
		ColorPickerWidget colorPicker = this.getColorPicker();
		if(colorPicker.activeAndVisible() && !colorPicker.targetElement.isMouseOver(mouseX, mouseY)) {
			this.toggleColorPicker(false);
		}
//	} else if (this.colorPickerWidget.activeAndVisible() &&
//		!this.colorPickerWidget.targetElement.isMouseOver(mouseX, mouseY)
//		) { // don't disable color picker if its target element was clicked
//		this.toggleColorPicker(false);
//	}
	}

}
