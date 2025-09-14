package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import dev.hephaestus.glowcase.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Single widget for containing and handling a bunch of {@link ColorPresetWidget}. Intended for use in the {@link ColorPickerWidget}, but reusable elsewhere too.
 *
 * @see ColorPresetWidget
 * @see ColorPickerWidget
 */
public class ColorPresetsContainerWidget extends PressableWidget {
	public final ColorPickerWidget colorPickerWidget; // TODO - make this independent of color picker
	public final List<ColorPresetWidget> presets = new ArrayList<>();
	public final List<ColorPresetWidget> transparentPresets = new ArrayList<>();

	public int presetSize = 16;
	public int presetPadding = 2;
	public int presetsPerLine = 10;
	private BiConsumer<Integer, @Nullable Formatting> presetListener;

	public ColorPresetsContainerWidget(int x, int y, int width, int height, ColorPickerWidget colorPickerWidget) {
		super(x, y, width, height, Text.of(""));
		this.colorPickerWidget = colorPickerWidget;
	}

	public void createPresets(boolean includeDefaultPresets, List<Integer> addedPresets) {
		if(includeDefaultPresets) {
			ColorPresetWidget.addDefaultWidgets(this.presets, this.colorPickerWidget);

			if(this.colorPickerWidget.allowTransparency)
				ColorPresetWidget.addDefaultTransparentWidgets(this.transparentPresets, this.colorPickerWidget);
		}

		if(addedPresets.isEmpty()) return;
		for (int color : addedPresets) {
			this.presets.add(ColorPresetWidget.fromColor(this.colorPickerWidget, color));
		}
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		renderPresetList(context, this.presets, this.getY(), mouseX, mouseY, delta);
		renderPresetList(context, this.transparentPresets, this.getY() + (this.presetSize + this.presetPadding) * 2, mouseX, mouseY, delta);
	}

	protected void renderPresetList(DrawContext context, List<ColorPresetWidget> presets, int yOffset, int mouseX, int mouseY, float delta) {
		int x = this.getX();
		int maxY = this.getY() + this.getHeight();
		renderPresetList(context, presets, x, yOffset, maxY, this.presetSize, this.presetPadding, this.presetsPerLine, mouseX, mouseY, delta);
	}

	// Render each preset widget in a grid, left to right, top to bottom
	protected void renderPresetList(
		DrawContext context, List<ColorPresetWidget> presets, int x, int y, int maxY,
		int presetSize, int presetPadding, int presetsPerLine,
		int mouseX, int mouseY, float delta
	) {
		if(presets.isEmpty()) return;

		int paddedSize = presetSize + presetPadding;
		int presetX = x;
		int presetY = y;
		int renderedPresets = 0;
		for (ColorPresetWidget preset : presets) {
			preset.setPosition(presetX, presetY, presetSize);
			preset.renderWidget(context, mouseX, mouseY, delta);

			presetX += paddedSize;
			renderedPresets++;
			if(renderedPresets % presetsPerLine != 0) continue;

			presetY += paddedSize;
			int currentScaleY = presetY + paddedSize;
			if(currentScaleY > maxY) return; // prevent overflow
			presetX = x;
		}
	}

	public boolean tryClickingPresets(double mouseX, double mouseY) {
		for (ColorPresetWidget preset : this.presets) {
			if (preset.isMouseOver(mouseX, mouseY)) {
				preset.onClick(mouseX, mouseY);
				return true;
			}
		}

		for (ColorPresetWidget preset : this.transparentPresets) {
			if (preset.isMouseOver(mouseX, mouseY)) {
				preset.onClick(mouseX, mouseY);
				return true;
			}
		}
		return false;
	}

	public int getPresetSize() {
		return this.presets.size() + this.transparentPresets.size();
	}

	public void setPosition(int x, int y, int width, int height, int presetScale, int presetsPerLine) {
		this.setX(x);
		this.setY(y);
		this.setDimensions(width, height);
		this.setPresetSize(presetScale);
		this.setPresetsPerLine(presetsPerLine);
	}

	public void setPresetSize(int presetSize) {
		this.presetSize = presetSize;
	}

	public void setPresetPadding(int presetPadding) {
		this.presetPadding = presetPadding;
	}

	public void setPresetsPerLine(int presetsPerLine) {
		this.presetsPerLine = presetsPerLine;
	}

	@Override
	public void onPress() {}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}
