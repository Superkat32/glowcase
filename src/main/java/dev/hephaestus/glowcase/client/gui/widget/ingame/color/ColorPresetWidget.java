package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Single clickable widget for displaying a color preset - intended for use in {@link ColorPresetsContainerWidget} and {@link ColorPickerWidget}.
 *
 * @see ColorPresetsContainerWidget
 * @see ColorPickerWidget
 */
public class ColorPresetWidget extends PressableWidget {
	public static final Identifier ALPHA_TEXTURE = Glowcase.id("alpha_preset");
	// Use our own array instead of Formatting.values() so we can have specific order
	public static final Formatting[] FORMATTING_COLORS = new Formatting[] {
		Formatting.DARK_RED, Formatting.RED, Formatting.GOLD, Formatting.YELLOW,
		Formatting.GREEN, Formatting.DARK_GREEN, Formatting.AQUA, Formatting.DARK_AQUA,
		Formatting.BLUE, Formatting.DARK_BLUE, Formatting.LIGHT_PURPLE, Formatting.DARK_PURPLE,
		Formatting.WHITE, Formatting.GRAY, Formatting.DARK_GRAY, Formatting.BLACK
	};

	public static final Integer[] DEFAULT_TRANSPARENCIES = new Integer[] {
//		0xEE000000, 0xCC000000, 0xAA000000, 0x99000000, 0x77000000, 0x55000000, 0x33000000, 0x00000000,
		0x00000000, 0x33000000, 0x55000000, 0x77000000, 0x99000000, 0xAA000000, 0xCC000000, 0xEE000000
	};

	public final ColorPickerWidget colorPickerWidget;
	public final int color;
	public final float colorAlpha; // cache the alpha value in constructor
	@Nullable
	public Formatting formatting = null;

	public static void addDefaultWidgets(List<ColorPresetWidget> presetsList, ColorPickerWidget colorPickerWidget) {
		// my goodness I'm smart
		presetsList.addAll(Arrays.stream(FORMATTING_COLORS)
			.map(format -> fromFormatting(colorPickerWidget, format))
			.toList()
		);
	}

	public static void addDefaultTransparentWidgets(List<ColorPresetWidget> presetsList, ColorPickerWidget colorPickerWidget) {
		presetsList.addAll(Arrays.stream(DEFAULT_TRANSPARENCIES)
			.map(colorInt -> fromColor(colorPickerWidget, colorInt))
			.toList()
		);
	}


	public ColorPresetWidget(ColorPickerWidget colorPicker, int x, int y, int width, int height, int color) {
		super(x, y, width, height, Text.of(""));
		this.colorPickerWidget = colorPicker;
		this.color = color;
		this.colorAlpha = ColorHelper.getAlphaFloat(color);
	}

	public void setPosition(int x, int y, int size) {
		this.setX(x);
		this.setY(y);
		this.setDimensions(size, size);
	}

	// I really don't know why these static methods are down here, but it felt wrong putting them above the constructor ??
	public static ColorPresetWidget fromFormatting(ColorPickerWidget colorPicker, Formatting formatting) {
		if(formatting.isColor()) {
			//noinspection DataFlowIssue
			int color = ColorHelper.withAlpha(1f, formatting.getColorValue());
			ColorPresetWidget presetWidget = new ColorPresetWidget(colorPicker,0, 0, 0, 0, color);
			presetWidget.formatting = formatting;
			return presetWidget;
		}
		return new ColorPresetWidget(colorPicker, 0, 0, 0, 0, ColorUtil.WHITE); // fallback
	}

	public static ColorPresetWidget fromColor(ColorPickerWidget colorPicker, int color) {
		return new ColorPresetWidget(colorPicker, 0, 0, 0, 0, color);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if(this.colorAlpha < 1f) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ALPHA_TEXTURE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
		}
		context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.color);
		if(isMouseOver(mouseX, mouseY)) {
			drawOutline(context, this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2);
		}
	}

	private void drawOutline(DrawContext context, int x, int y, int width, int height) {
		int color = ColorUtil.WHITE;
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width, y, x + width - 1, y + height, color);
		context.fill(x, y + height, x + width, y + height - 1, color);
	}

	@Override
	public void onPress() {
		BiConsumer<Integer, Formatting> presetListener = this.colorPickerWidget.getPresetListener();
		if(presetListener != null) {
			presetListener.accept(this.color, this.formatting != null && this.formatting.isColor() ? this.formatting : null);
		} else {
			if(this.formatting != null && formatting.isColor()) {
				this.colorPickerWidget.setColor(this.color);
				this.colorPickerWidget.toggle(false);
			} else {
				this.colorPickerWidget.setColor(this.color);
			}
		}
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}
