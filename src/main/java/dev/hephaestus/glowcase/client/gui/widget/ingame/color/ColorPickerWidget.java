package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ColorPickerIncludedScreen;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ColorPickerWidget extends PressableWidget {
	private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_menu_list_background.png");
	private static final Identifier CONFIRM_TEXTURE = Identifier.ofVanilla("pending_invite/accept");
	private static final Identifier CONFIRM_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("pending_invite/accept_highlighted");
	private static final Identifier CANCEL_TEXTURE = Identifier.ofVanilla("pending_invite/reject");
	private static final Identifier CANCEL_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("pending_invite/reject_highlighted");

	public final ColorPickerIncludedScreen screen;
	public Element targetElement;
	public Color color = Color.RED; // TODO - possibly replace this with ints to prevent *possible* mac crashes
	public boolean includePresets = true;
	public ArrayList<ColorPresetWidget> presetWidgets = Lists.newArrayList();
	public IconButtonWidget confirmButton;
	public IconButtonWidget cancelButton;
	private Consumer<Color> changeListener;
	private BiConsumer<Color, @Nullable Formatting> presetListener;
	private Consumer<ColorPickerWidget> onAccept;
	private Consumer<ColorPickerWidget> onCancel;

	private boolean mouseDown = false;
	public boolean anyWidgetDown = false;
	private ColorPickerComponent currentComponent = null;
	private List<ColorPickerComponent> components;
	private ColorPickerComponent previewComponent = new ColorPickerComponent();
	private ColorPickerComponent hueComponent = new ColorPickerComponent(this::setHue);
	private ColorPickerComponent satLightComponent = new ColorPickerComponent(this::setSatLat);
	private ColorPickerComponent presetComponent = new ColorPickerComponent();

	private float[] HSL;
	private float hue;
	private float saturation;
	private float light;

	public static ColorPickerWidget.Builder builder(ColorPickerIncludedScreen screen, int x, int y) {
		return new ColorPickerWidget.Builder(screen, x, y);
	}

	public ColorPickerWidget(ColorPickerIncludedScreen screen, int x, int y, int width, int height, Text message) {
		super(x, y, width, height, message);
		this.screen = screen;

		this.confirmButton = IconButtonWidget.builder(CONFIRM_TEXTURE, action -> this.confirmColor())
			.hoverIcon(CONFIRM_HIGHLIGHTED_TEXTURE).build();

		this.cancelButton = IconButtonWidget.builder(CANCEL_TEXTURE, action -> this.cancel())
			.hoverIcon(CANCEL_HIGHLIGHTED_TEXTURE).build();

		this.components = List.of(
			this.previewComponent, this.hueComponent, this.satLightComponent, this.presetComponent
		);

		this.update();
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if (!visible) return;
		this.updateHSL();
		this.updatePositions();

		//context.setShaderColor(1f, 1f, 1f, this.alpha);
		/*RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();*/
		Matrix3x2fStack matrices = context.getMatrices();
		// context.applyBlur();

		context.createNewRootLayer();
		matrices.pushMatrix();

		int x = this.getX();
		int y = this.getY();
		int z = 1;
		int width = this.getWidth();
		int height = this.getHeight();

		// draw entire color picker's background
		context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, x, y, 0, 0, width, height, 32, 32);
		if (this.isSelected()) {
			// draw entire color picker's outline
			drawOutline(context, x, y, width, height, ColorUtil.WHITE);
		}

		// draw components
		drawColorPreview(context, this.previewComponent);
		drawSatLight(context, this.satLightComponent);
		drawHueBar(context, this.hueComponent);
		if (this.includePresets) {
			drawPresets(context, this.presetComponent, mouseX, mouseY, delta, z + 1);
		}

		// draw buttons
		this.confirmButton.renderWidget(context, mouseX, mouseY, delta);
		this.cancelButton.renderWidget(context, mouseX, mouseY, delta);

		matrices.popMatrix();
		// context.setShaderColor(1f, 1f, 1f, 1f);
	}

	public void updatePositions() {
		int x = this.getX();
		int y = this.getY();
		int z = 1; // TODO - remove z i think it's useless now
		int width = this.getWidth();
		int height = this.getHeight();

		// Any numbers which are used to determine the position of another component should be its own variable
		int presetSize = (int) (height / 6.5); // same number redone in drawPresets - if changed, change there too
		int presetPadding = 2;
		int presetHeight = this.includePresets ? presetSize * 2 + presetPadding * 2 : 0;

		int previewX = x + 2;
		int previewY = y + 2;
		int previewWidth = width / 3;
		int previewHeight = height - 16 - presetHeight;

		int hueY = previewY + previewHeight + 2;
		int hueHeight = height - previewHeight - 6 - presetHeight;
		int presetY = hueY + hueHeight + presetPadding;

		this.previewComponent.updatePos(previewX, previewY, previewWidth, previewHeight);

		this.hueComponent.updatePos(previewX, hueY,
			width - 4, hueHeight
		);

		this.satLightComponent.updatePos(previewX + previewWidth + 2, y + 2,
			width - previewWidth - 6, previewHeight);

		this.presetComponent.updatePos(previewX, presetY,
			width, y + height - presetY);

		this.confirmButton.setPosition(
			x + width - presetSize - presetPadding,
			y + height - presetSize - 2,
			z + 1, presetSize, presetSize + 2
		);

		this.cancelButton.setPosition(
			x + width - presetSize * 2 - presetPadding * 2 - 1,
			y + height - presetSize - 2,
			z + 1, presetSize, presetSize + 2
		);
	}

	private void drawColorPreview(DrawContext context, ColorPickerComponent component) {
		context.fill(component.getMinX(), component.getMinY(), component.getMaxX(), component.getMaxY(), this.color.getRGB());
	}

	private void drawSatLight(DrawContext context, ColorPickerComponent component) {
		// white to current color's hue, left to right
		sidewaysGradient(context, component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight(), ColorUtil.WHITE, getRgbFromCurrentHue());

		// transparent to black, top to bottom
		context.fillGradient(component.getMinX(), component.getMinY(), component.getMaxX(), component.getMaxY(), 0x00000000, ColorUtil.BLACK);

		// thumb
		drawThumb(context, component, 8, 8, this.color.getRGB(), false);
	}

	private void drawHueBar(DrawContext context, ColorPickerComponent component) {
		int x = component.getMinX();
		int maxX = component.getMaxX();
		int y = component.getMinY();
		int width = component.getWidth();
		int height = component.getHeight();

		// rainbow gradient
		int maxColors = ColorUtil.RAINBOW_COLORS.length - 1;
		int widthPerColor = width / maxColors;
		for (int color = 0; color < maxColors; color++) {
			int colorMinX = x + (widthPerColor * color);
			int colorWidth = color == maxColors - 1 ? maxX - colorMinX : widthPerColor;
			sidewaysGradient(
				context,
				colorMinX, y,
				colorWidth, height,
				ColorUtil.RAINBOW_COLORS[color], ColorUtil.RAINBOW_COLORS[color + 1]
			);
		}

		// thumb
		drawThumb(context, component, 6, height + 2, getRgbFromCurrentHue());
	}

	private void drawPresets(DrawContext context, ColorPickerComponent component, int mouseX, int mouseY, float delta, int z) {
		int presetSize = (int) (this.getHeight() / 6.5);
		int paddedSize = presetSize + 2; // presetPadding = 2
		// Mostly dynamic, but probably not perfect - am not too worried about it though
		int presetsPerLine = component.getWidth() / (paddedSize);

		// Render each preset widget in a grid, left to right, top to bottom
		int presetX = component.getMinX();
		int presetY = component.getMinY();
		int renderedPresets = 0;
		for (ColorPresetWidget preset : this.presetWidgets) {
			preset.setPosition(presetX, presetY, z, presetSize);
			preset.renderWidget(context, mouseX, mouseY, delta);

			presetX += paddedSize;
			renderedPresets++;
			if(renderedPresets % presetsPerLine != 0) continue;

			presetY += paddedSize;
			if(presetY > component.getMaxY()) return; // prevent overflow
			presetX = component.getMinX();
		}
	}

	private void drawThumb(DrawContext context, ColorPickerComponent component, int width, int height, int color) {
		this.drawThumb(context, component, width, height, color, true);
	}

	private void drawThumb(DrawContext context, ColorPickerComponent component, int width, int height, int color, boolean centered) {
		int halfWidth = width / 2;
		int halfHeight = height / 2;
		int initY = centered ? component.getMinY() + (component.getHeight() / 2) : component.getThumbY();

		context.fill(
			component.getThumbX() - halfWidth, initY - halfHeight,
			component.getThumbX() + halfWidth, initY + halfHeight,
			color
		);

		drawOutline(context, component.getThumbX() - halfWidth, initY - halfHeight, width + 1, height + 1, Colors.WHITE);
	}

	private void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width, y, x + width - 1, y + height, color);
		context.fill(x, y + height, x + width, y + height - 1, color);
	}

	private void sidewaysGradient(DrawContext context, int x, int y, int width, int height, int startColor, int endColor) {
		context.state.addSimpleElement(new SimpleGuiElementRenderState() {

			@Override
			public ScreenRect bounds() {
				return new ScreenRect(x, y, width, height).transformEachVertex(context.getMatrices());
			}

			@Override
			public void setupVertices(VertexConsumer vertices, float depth) {
				Matrix3x2fStack matrix = context.getMatrices();
				vertices.vertex(matrix, x, y, depth).color(startColor);
				vertices.vertex(matrix, x, y + height, depth).color(startColor);
				vertices.vertex(matrix, x + width, y + height, depth).color(endColor);
				vertices.vertex(matrix, x + width, y, depth).color(endColor);
			}

			@Override
			public RenderPipeline pipeline() {
				return RenderPipelines.GUI;
			}

			@Override
			public TextureSetup textureSetup() {
				return TextureSetup.empty();
			}

			@Override
			public @Nullable ScreenRect scissorArea() {
				return null;
			}
		});
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.mouseDown = true;
		this.currentComponent = null;
		this.anyWidgetDown = false;

		this.components.forEach(ColorPickerComponent::resetMouseDown);
		setColorFromMouse(mouseX, mouseY);
	}

	public void setColorFromMouse(double mouseX, double mouseY) {
		this.setColor(mouseX, mouseY);
		if (this.changeListener != null) {
			this.changeListener.accept(this.color);
		}
	}

	// Exclusively its own method so I can reduce nested if statements with return statements
	private void setColor(double mouseX, double mouseY) {
		// Click priority order:
		// Sat/light, hue, (transparency), confirm, cancel, presets
		boolean clickedComponent = tryClickComponent(this.satLightComponent, mouseX, mouseY)
			|| tryClickComponent(this.hueComponent, mouseX, mouseY);
		if(clickedComponent) return;

		boolean clickedButton = tryClickWidget(this.confirmButton, mouseX, mouseY)
			|| tryClickWidget(this.cancelButton, mouseX, mouseY);
		if(clickedButton) return;

		tryClickPresets(mouseX, mouseY);
	}

	public boolean tryClickComponent(ColorPickerComponent component, double mouseX, double mouseY) {
		if(!allowComponentClick(component)) return false;
		boolean clicked = component.clicked(mouseX, mouseY);
		if(clicked) {
			this.currentComponent = component;
			component.setValuesFromMouse(mouseX, mouseY);
			this.setColorFromHSL();
		}

		return clicked;
	}

	public boolean tryClickWidget(PressableWidget button, double mouseX, double mouseY) {
		if(!button.isMouseOver(mouseX, mouseY)) return false;
		if(!this.allowWidgetClick()) return false;

		button.onClick(mouseX, mouseY);
		this.anyWidgetDown = true;
		return true;
	}

	public boolean tryClickPresets(double mouseX, double mouseY) {
		if(!this.allowWidgetClick()) return false;

		// just checks for each preset here, and also sets here so it doesn't have to check again
		for (ColorPresetWidget preset : this.presetWidgets) {
			if (preset.isMouseOver(mouseX, mouseY)) {
				preset.onClick(mouseX, mouseY);
				// even though the preset almost always closes the color picker,
				// this is added to prevent spamming tags when holding down the mouse button
				this.anyWidgetDown = true;
			}
		}
		return this.anyWidgetDown;
	}

	// It's possible for the user to continue clicking widgets/components after the color picker has closed
	// if they hold down the click button, so these checks prevents that from happening
	private boolean allowComponentClick(ColorPickerComponent component) {
		return (this.currentComponent == null && !this.anyWidgetDown) || this.currentComponent == component;
	}

	private boolean allowWidgetClick() {
		return this.currentComponent == null && !this.anyWidgetDown;
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
		if (mouseDown || isMouseOver(mouseX, mouseY)) {
			setColorFromMouse(mouseX, mouseY);
		}
	}

	@Override
	public void onRelease(double mouseX, double mouseY) {
		this.mouseDown = false;
	}

	@Override
	public void onPress() {}

	public void confirmColor() {
		if (this.onAccept != null) {
			this.onAccept.accept(this);
		} else {
			this.toggle(false);
		}
	}

	public void cancel() {
		if (this.onCancel != null) {
			this.onCancel.accept(this);
		} else {
			this.toggle(false);
		}
	}

	public void toggle(boolean active) {
		this.active = active;
		this.visible = active;
		if (this.active) {
			this.update();
		}
	}

	public void insertColor(Color color) {
		String hex = getHexCode(color);
		this.screen.insertHexTag(hex);
	}

	public void update() {
		this.updatePositions();
		this.updateHSL();
		this.updateThumbPositions();
	}

	public void updateThumbPositions() {
		this.satLightComponent.updateThumbXY(this.saturation, this.light);
		this.hueComponent.updateThumbX(this.hue);
	}

	public void updateHSL() {
		this.HSL = getHSL();
		this.hue = HSL[0];
		this.saturation = HSL[1];
		this.light = HSL[2];
	}

	public void setHue(float hue) {
		this.hue = hue;
	}

	public void setSatLat(float saturation, float light) {
		this.saturation = saturation;
		this.light = light;
	}

	public void setColor(Color color) {
		this.color = color;
		this.update();
	}

	public void setColorFromHSL() {
		this.color = Color.getHSBColor(getHueFromThumb(), this.saturation, this.light);
	}

	public Color getCurrentColor() {
		return this.color;
	}

	public int getRgbFromCurrentHue() {
		return Color.HSBtoRGB(getHueFromThumb(), 1, 1);
	}

	public float getHueFromThumb() {
		return (float) (this.hueComponent.getThumbX() - this.hueComponent.getMinX()) / this.hueComponent.getWidth();
	}

	protected float[] getHSL() {
		return Color.RGBtoHSB(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), null);
	}

	public void setTargetElement(Element element) {
		this.targetElement = element;
	}

	public void setPresets(boolean includeDefaultPresets, List<Color> addedPresets) {
		if (includeDefaultPresets) {
			this.presetWidgets.addAll(ColorPresetWidget.createDefaultPresets(this));
		}
		if (!addedPresets.isEmpty()) {
			for (Color preset : addedPresets) {
				this.presetWidgets.add(ColorPresetWidget.fromColor(this, preset));
			}
		}
	}

	public void setIncludePresets(boolean shouldInclude) {
		this.includePresets = shouldInclude;
	}

	public void setChangeListener(Consumer<Color> changeListener) {
		this.changeListener = changeListener;
	}

	public void setPresetListener(BiConsumer<Color, @Nullable Formatting> presetListener) {
		this.presetListener = presetListener;
	}

	public void setOnAccept(Consumer<ColorPickerWidget> onAccept) {
		this.onAccept = onAccept;
	}

	public void setOnCancel(Consumer<ColorPickerWidget> onCancel) {
		this.onCancel = onCancel;
	}

	public BiConsumer<Color, @Nullable Formatting> getPresetListener() {
		return presetListener;
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		this.appendDefaultNarrations(builder);
	}

	public static String getHexCode(Color color) {
		return "#" + String.format("%1$06X", color.getRGB() & 0x00FFFFFF);
	}

	@Environment(EnvType.CLIENT)
	public static class Builder {
		private final ColorPickerIncludedScreen screen;
		private final int x;
		private final int y;
		private int width = 150;
		private int height = 200;
		private boolean includePresets = true;
		private boolean includeDefaultPresets = true;
		private final List<Color> presets = Lists.newArrayList();

		public Builder(ColorPickerIncludedScreen screen, int x, int y) {
			this.screen = screen;
			this.x = x;
			this.y = y;
		}

		public ColorPickerWidget.Builder size(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public ColorPickerWidget.Builder includePresets(boolean shouldInclude) {
			this.includePresets = shouldInclude;
			return this;
		}

		public ColorPickerWidget.Builder withPreset(boolean includeDefault, Color... presets) {
			this.includeDefaultPresets = includeDefault;
			this.presets.addAll(Arrays.asList(presets));
			return this;
		}

		public ColorPickerWidget build() {
			ColorPickerWidget colorPickerWidget = new ColorPickerWidget(this.screen, this.x, this.y, this.width, this.height, Text.of(""));
			colorPickerWidget.setIncludePresets(this.includePresets);
			if (this.includePresets) {
				colorPickerWidget.setPresets(this.includeDefaultPresets, this.presets);
			}
			return colorPickerWidget;
		}
	}
}
