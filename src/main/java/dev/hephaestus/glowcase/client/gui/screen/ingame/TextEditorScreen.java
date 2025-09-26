package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.client.gui.screen.ingame.interfaces.ColorPickerIncludedScreen;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.ColorPickerWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public abstract class TextEditorScreen extends GlowcaseScreen implements ColorPickerIncludedScreen {
	private ButtonWidget colorText;
	private ButtonWidget[] widgets = new ButtonWidget[0];

	protected void addFormattingButtons(int x, int y, int innerPadding, int buttonSize, int buttonPadding) {
		int buttonX = x + innerPadding * 2;
		int buttonY = y + innerPadding; // reduce the times this is calculated
		ButtonWidget boldText = ButtonWidget.builder(Text.literal("B").formatted(Formatting.BOLD), action -> {
			this.insertBold();
		}).dimensions(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding;
		ButtonWidget italicizeText = ButtonWidget.builder(Text.literal("I").formatted(Formatting.ITALIC), action -> {
			this.insertItalic();
		}).dimensions(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding;
		ButtonWidget strikeText = ButtonWidget.builder(Text.literal("S").formatted(Formatting.STRIKETHROUGH), action -> {
			this.insertStrikethrough();
		}).dimensions(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding;
		ButtonWidget underlineText = ButtonWidget.builder(Text.literal("U").formatted(Formatting.UNDERLINE), action -> {
			this.insertUnderline();
		}).dimensions(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding;
		// not using the actual obfuscated formatting here because the movement can be annoying
		ButtonWidget obfuscateText = ButtonWidget.builder(Text.literal("@"), action -> {
			this.insertObfuscated();
		}).dimensions(buttonX, buttonY, buttonSize, buttonSize).build();

		buttonX += buttonSize + buttonPadding; // + 4? (only works on padding of 2)
		this.colorText = ButtonWidget.builder(Text.literal("\uD83D\uDD8C"), action -> {
			ColorPickerWidget colorPickerWidget = getColorPicker();
			colorPickerWidget.targetWidget(
				this.colorText, colorPickerWidget.getCurrentColor(),
				this.colorText.getX() + this.colorText.getWidth() - ColorPickerWidget.DEFAULT_WIDTH,
				this.colorText.getY() + this.colorText.getHeight(),
				ColorPickerWidget.DEFAULT_WIDTH, ColorPickerWidget.DEFAULT_HEIGHT
			);
		}).dimensions(buttonX, buttonY, buttonSize, buttonSize).build();

		widgets = new ButtonWidget[]{
			boldText, italicizeText, strikeText, underlineText, obfuscateText, colorText
		};

		this.addDrawableChild(boldText);
		this.addDrawableChild(italicizeText);
		this.addDrawableChild(strikeText);
		this.addDrawableChild(underlineText);
		this.addDrawableChild(obfuscateText);
		this.addDrawableChild(colorText);
	}

	public void toggleWidgets(boolean active) {
		for (ButtonWidget widget : widgets)
			widget.active = active;
	}
}
