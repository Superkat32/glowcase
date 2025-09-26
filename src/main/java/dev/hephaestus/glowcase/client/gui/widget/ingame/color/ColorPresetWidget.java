package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.client.util.WidgetRenderUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
import java.util.function.Supplier;

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
		0x00000000, 0x40000000, 0x55000000, 0x77000000, 0x99000000, 0xAA000000, 0xCC000000, 0xFF000000
	};

	public final ColorPresetsContainerWidget presetsContainerWidget;
	public int color;
	public float colorAlpha; // cache the alpha value in constructor
	@Nullable
	public Formatting formatting;

	@Nullable
	public final Supplier<Integer> colorCopyReferenceSupplier;
	public boolean copyRgb;
	public boolean copyAlpha;

	public static void addDefaultWidgets(ColorPresetsContainerWidget presetsContainerWidget, List<ColorPresetWidget> presetsList) {
		// my goodness I'm smart
		presetsList.addAll(Arrays.stream(FORMATTING_COLORS)
			.map(format -> fromFormatting(presetsContainerWidget, format))
			.toList()
		);
	}

	public static void addDefaultTransparentWidgets(ColorPresetsContainerWidget presetsContainerWidget, List<ColorPresetWidget> presetsList) {
		presetsList.addAll(Arrays.stream(DEFAULT_TRANSPARENCIES)
			.map(colorInt -> fromColor(presetsContainerWidget, colorInt))
			.toList()
		);
	}

	public ColorPresetWidget(
		ColorPresetsContainerWidget presetsContainerWidget,
		int x, int y, int width, int height,
		int color, Formatting formatting,
		Supplier<Integer> colorCopySupplier, boolean copyRgb, boolean copyAlpha
	) {
		super(x, y, width, height, Text.of(""));
		this.presetsContainerWidget = presetsContainerWidget;
		this.color = color;
		this.formatting = formatting;
		this.colorCopyReferenceSupplier = colorCopySupplier;
		this.copyRgb = copyRgb;
		this.copyAlpha = copyAlpha;
		this.colorAlpha = ColorHelper.getAlphaFloat(color);
	}

	public void setPosition(int x, int y, int size) {
		this.setX(x);
		this.setY(y);
		this.setDimensions(size, size);
	}

	// I really don't know why these static methods are down here, but it felt wrong putting them above the constructor ??
	public static ColorPresetWidget fromFormatting(ColorPresetsContainerWidget presetsContainerWidget, Formatting formatting) {
		if(formatting.isColor()) {
			return ColorPresetWidget.Builder.createFromFormatting(presetsContainerWidget, formatting).build();
		}

		return ColorPresetWidget.Builder.createFromColor(presetsContainerWidget, ColorUtil.WHITE).build(); // fallback
	}

	public static ColorPresetWidget fromColor(ColorPresetsContainerWidget presetsContainerWidget, int color) {
		return ColorPresetWidget.Builder.createFromColor(presetsContainerWidget, color).build();
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if(this.colorAlpha < 1f || this.shouldCopyColor()) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ALPHA_TEXTURE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
		}
		context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.getCurrentColor());
		if(isMouseOver(mouseX, mouseY)) {
			WidgetRenderUtil.drawOutline(context, this.getX() - 1, this.getY() - 1, this.getWidth() + 2, this.getHeight() + 2, ColorUtil.WHITE);
		}
	}

	public int getCurrentColor() {
		if(this.shouldCopyColor()) {
			//noinspection DataFlowIssue
			int copyOverColor = colorCopyReferenceSupplier.get();
			if(this.copyRgb) return ColorHelper.withAlpha(this.colorAlpha, copyOverColor);
			else if (this.copyAlpha) return ColorUtil.transferAlpha(copyOverColor, this.color);
		}

		return this.color;
	}

	public boolean shouldCopyColor() {
		return Screen.hasShiftDown() && this.colorCopyReferenceSupplier != null;
	}

	@Override
	public void onPress() {
		BiConsumer<Integer, Formatting> presetListener = this.presetsContainerWidget.getPresetListener();
		if(presetListener != null) {
			presetListener.accept(this.getCurrentColor(),
				this.formatting != null && this.formatting.isColor() ?
					this.formatting : null
			);
		}
	}

	public void updateAlpha(float colorAlpha) {
		this.colorAlpha = colorAlpha;
		this.color = ColorHelper.withAlpha(colorAlpha, this.color);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

	@Environment(EnvType.CLIENT)
	public static class Builder {
		private final ColorPresetsContainerWidget presetsContainerWidget;
		private int x, y, width, height;
		private int color;
		private Formatting formatting = null;

		private Supplier<Integer> colorCopySupplier = null;
		private boolean copyRgb = false;
		private boolean copyAlpha = false;

		public Builder(ColorPresetsContainerWidget presetsContainerWidget) {
			this.presetsContainerWidget = presetsContainerWidget;
		}

		public static Builder create(ColorPresetsContainerWidget presetsContainerWidget) {
			return new Builder(presetsContainerWidget);
		}

		public static Builder createFromFormatting(ColorPresetsContainerWidget presetsContainerWidget, Formatting formatting) {
			return new Builder(presetsContainerWidget)
				.setColor(formatting)
				.setColorCopyReference(presetsContainerWidget::getCopyColor)
				.setCopyAlpha(true);
		}

		public static Builder createFromColor(ColorPresetsContainerWidget presetsContainerWidget, int color) {
			return new Builder(presetsContainerWidget)
				.setColor(color)
				.setColorCopyReference(presetsContainerWidget::getCopyColor)
				.setCopyRgb(true);
		}

		public final Builder setPos(int x, int y, int width, int height) {
			return this.setPos(x, y).setDims(width, height);
		}

		public final Builder setPos(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public final Builder setDims(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public final Builder setColor(int color) {
			this.color = color;
			return this;
		}

		public final Builder setColor(Formatting formatting) {
			this.formatting = formatting;
			//noinspection DataFlowIssue
			this.color = ColorHelper.withAlpha(1f, formatting.getColorValue());
			return this;
		}

		public final Builder setColorCopyReference(Supplier<Integer> colorCopySupplier) {
			this.colorCopySupplier = colorCopySupplier;
			return this;
		}

		public final Builder setCopyRgb(boolean copy) {
			this.copyRgb = copy;
			return this;
		}

		public final Builder setCopyAlpha(boolean copy) {
			this.copyAlpha = copy;
			return this;
		}

		public ColorPresetWidget build() {
			return new ColorPresetWidget(
				this.presetsContainerWidget,
				this.x, this.y, this.width, this.height,
				this.color, this.formatting,
				this.colorCopySupplier, this.copyRgb, this.copyAlpha
			);
		}
	}
}
