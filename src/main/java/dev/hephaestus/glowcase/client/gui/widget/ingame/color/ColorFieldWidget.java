package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker.ColorPickerWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Text field widget which stores a color and displays it in string form(hex code). Has direct support for color pickers.<br><br>
 */
public class ColorFieldWidget extends TextFieldWidget {
	public static final int DEFAULT_WIDTH = 50;
	public static final int DEFAULT_TRANSPARENT_WIDTH = 64;
	public static final int DEFAULT_HEIGHT = 20;

	public final Screen screen;
	public final ColorGetter colorGetter;
	public final ColorSetter colorSetter;

	@Nullable
	public final ColorPickerWidget colorPickerWidget;
	public int pickerX, pickerY, pickerWidth, pickerHeight;
	public boolean alphaEnabled;
	public float minAlpha;

	public int color;

	public ColorFieldWidget(
		Screen screen, int x, int y, int width, int height, Tooltip tooltip,
		ColorGetter colorGetter, ColorSetter colorSetter,
		boolean alphaEnabled, float minAlpha,
		@Nullable ColorPickerWidget colorPickerWidget,
		int pickerX, int pickerY, int pickerWidth, int pickerHeight
	) {
		super(MinecraftClient.getInstance().textRenderer, x, y, width, height, Text.empty());
		this.screen = screen;
		this.setTooltip(tooltip);
		this.colorGetter = colorGetter;
		this.colorSetter = colorSetter;

		this.alphaEnabled = alphaEnabled;
		this.minAlpha = minAlpha;

		this.colorPickerWidget = colorPickerWidget;
		this.pickerX = pickerX;
		this.pickerY = pickerY;
		this.pickerWidth = pickerWidth;
		this.pickerHeight = pickerHeight;

		this.color = this.colorGetter.getCurrentColor();

		this.setChangedListener(this::onChange);
		this.updateColorText();
	}

	// Use our own method because TextFieldWidget#onChanged() is final
	public void onChange(String text) {
		this.parseCurrentText();
	}

	public void updateColorText() {
		this.setText(this.getColorAsString());
	}

	public void parseCurrentText() {
		ColorUtil.parse(this.getText(), this.getColor()).ifSuccess(colorInt -> {
			setColor(colorInt);
			if(this.isFocused() && this.colorPickerWidget != null && this.colorPickerWidget.activeAndVisible()) {
				this.updateColorPicker();
			}
		});
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if(this.isFocused()) {
			this.updateColorPicker();
		}
	}

	public void updateColorPicker() {
		if(this.colorPickerWidget == null) return;
		this.colorPickerWidget.targetWidget(
			this, this.color, this.alphaEnabled, this.minAlpha, this::onColorPickerChange,
			this.pickerX, pickerY, pickerWidth, pickerHeight
		);

	}

	public void onColorPickerChange(int color) {
		this.setColor(color);
		this.updateColorText();
	}

	public String getColorAsString() {
		if(this.alphaEnabled) return ColorUtil.toAlphaHex(this.getColor());
		return ColorUtil.getHexCode(this.getColor());
	}

	public void setColor(int color) {
		this.color = color;
		this.colorSetter.setColor(color);
	}

	public int getColor() {
		return this.color;
	}

	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final Screen screen;
		private ColorGetter colorGetter;
		private ColorSetter colorSetter;
		private int x, y;

		private int width = DEFAULT_WIDTH;
		private int height = DEFAULT_HEIGHT;
		private Tooltip tooltip = Tooltip.of(Text.empty());

		private ColorPickerWidget colorPickerWidget = null;
		private int pickerX, pickerY, pickerWidth, pickerHeight;

		private boolean allowAlpha = false;
		private float minAlpha = 0f;

		public static Builder create(Screen screen, int x, int y, ColorGetter colorGetter, ColorSetter colorSetter) {
			return new Builder(screen).pos(x, y).getter(colorGetter).setter(colorSetter);
		}

		public Builder(Screen screen) {
			this.screen = screen;
		}

		public Builder pos(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public Builder size(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder getter(ColorGetter colorGetter) {
			this.colorGetter = colorGetter;
			return this;
		}
		public Builder setter(ColorSetter colorSetter) {
			this.colorSetter = colorSetter;
			return this;
		}

		public Builder tooltip(Tooltip tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		public Builder tooltip(Text text) {
			this.tooltip = Tooltip.of(text);
			return this;
		}

		/**
		 * If the width has not been changed, automatically adjusts the width to the default transparent width.
		 */
		public Builder transparency(boolean enabled) {
			return this.transparency(enabled, 0f);
		}

		public Builder transparency(boolean enabled, float minAlpha) {
			this.allowAlpha = enabled;
			this.minAlpha = minAlpha;
			if(enabled && this.width == DEFAULT_WIDTH) {
				this.width = DEFAULT_TRANSPARENT_WIDTH;
			}
			return this;
		}

		public Builder colorPicker(ColorPickerWidget colorPickerWidget) {
			int pickerWidth = ColorPickerWidget.DEFAULT_WIDTH;
			// disallow picker to go beyond screen limits
			int pickerX = Math.min((this.x + this.width) - pickerWidth, this.screen.width);
//			int pickerX = this.x + this.width - pickerWidth;
			int pickerY = this.y + this.height;
			return this.colorPicker(colorPickerWidget, pickerX, pickerY);
		}

		public Builder colorPicker(ColorPickerWidget colorPickerWidget, int pickerX, int pickerY) {
			int pickerWidth = ColorPickerWidget.DEFAULT_WIDTH;
			int pickerHeight = this.allowAlpha ? ColorPickerWidget.DEFAULT_HEIGHT_ALPHA : ColorPickerWidget.DEFAULT_HEIGHT;
			return this.colorPicker(colorPickerWidget, pickerX, pickerY, pickerWidth, pickerHeight);
		}

		public Builder colorPicker(ColorPickerWidget colorPickerWidget, int pickerX, int pickerY, int pickerWidth, int pickerHeight) {
			this.colorPickerWidget = colorPickerWidget;
			this.pickerX = pickerX;
			this.pickerY = pickerY;
			this.pickerWidth = pickerWidth;
			this.pickerHeight = pickerHeight;
			return this;
		}

		public Builder offsetColorPicker(int pickerXOffset, int pickerYOffset) {
			this.pickerX += pickerXOffset;
			this.pickerY += pickerYOffset;
			return this;
		}

		public ColorFieldWidget build() {
			return new ColorFieldWidget(
				this.screen, this.x, this.y, this.width, this.height, this.tooltip,
				this.colorGetter, this.colorSetter,
				this.allowAlpha, minAlpha,
				this.colorPickerWidget, this.pickerX, pickerY, pickerWidth, pickerHeight
			);
		}
	}

	// Use our own functional interfaces for custom method/variable names for clarity
	@FunctionalInterface
	public static interface ColorGetter {
		int getCurrentColor();
	}

	@FunctionalInterface
	public static interface ColorSetter {
		void setColor(int newColor);
	}
}
