package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class ColorPresetWidget extends PressableWidget {
	// Use our own array instead of Formatting.values() so we can have specific order
	public static final Formatting[] FORMATTING_COLORS = new Formatting[] {
		Formatting.DARK_RED, Formatting.RED, Formatting.GOLD, Formatting.YELLOW,
		Formatting.GREEN, Formatting.DARK_GREEN, Formatting.AQUA, Formatting.DARK_AQUA,
		Formatting.BLUE, Formatting.DARK_BLUE, Formatting.LIGHT_PURPLE, Formatting.DARK_PURPLE,
		Formatting.WHITE, Formatting.GRAY, Formatting.DARK_GRAY, Formatting.BLACK
	};

	public final ColorPickerWidget colorPickerWidget;
	public final Color color;
	@Nullable
	public Formatting formatting = null;
	public int z = 0;

	public static List<ColorPresetWidget> createDefaultPresets(ColorPickerWidget colorPickerWidget) {
		// my goodness I'm smart
		return Arrays.stream(FORMATTING_COLORS)
			.map(format -> fromFormatting(colorPickerWidget, format))
			.toList();
	}

	public ColorPresetWidget(ColorPickerWidget colorPicker, int x, int y, int width, int height, Color color) {
		super(x, y, width, height, Text.of(""));
		this.colorPickerWidget = colorPicker;
		this.color = color;
	}

	public void setPosition(int x, int y, int z, int size) {
		this.setX(x);
		this.setY(y);
		this.setDimensions(size, size);
		this.z = z;
	}

	// I really don't know why these static methods are down here, but it felt wrong putting them above the constructor ??
	public static ColorPresetWidget fromFormatting(ColorPickerWidget colorPicker, Formatting formatting) {
		if(formatting.isColor()) {
			//noinspection DataFlowIssue
			ColorPresetWidget presetWidget = new ColorPresetWidget(colorPicker,0, 0, 0, 0, new Color(formatting.getColorValue()));
			presetWidget.formatting = formatting;
			return presetWidget;
		}
		return new ColorPresetWidget(colorPicker, 0, 0, 0, 0, Color.white); // fallback
	}

	public static ColorPresetWidget fromColor(ColorPickerWidget colorPicker, Color color) {
		return new ColorPresetWidget(colorPicker, 0, 0, 0, 0, color);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.color.getRGB());
		if(isMouseOver(mouseX, mouseY)) {
			drawOutline(context, this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2, this.z + 1);
		}
	}

	private void drawOutline(DrawContext context, int x, int y, int width, int height, int z) {
		int color = Color.white.getRGB();
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width, y, x + width - 1, y + height, color);
		context.fill(x, y + height, x + width, y + height - 1, color);
	}

	@Override
	public void onPress() {
		BiConsumer<Color, Formatting> presetListener = this.colorPickerWidget.getPresetListener();
		if(presetListener != null) {
			presetListener.accept(this.color, this.formatting != null && this.formatting.isColor() ? this.formatting : null);
		} else {
			if(this.formatting != null && formatting.isColor()) {
				this.colorPickerWidget.color = this.color;
				this.colorPickerWidget.toggle(false);
			} else {
				this.colorPickerWidget.setColor(this.color);
			}
		}
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {

	}
}
