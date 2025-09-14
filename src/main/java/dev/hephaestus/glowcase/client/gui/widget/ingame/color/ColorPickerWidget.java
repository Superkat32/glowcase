package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ColorPickerIncludedScreen;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Main color picker widget for choosing a color with hue/saturation/light, and optionally alpha or from a preset.<br><br>
 *
 * Each "area" (e.g. color preview, hue slider, sat/light picker, etc.) is split up into its own {@link ColorPickerComponent}, which contains the bounds of that area(used for rendering and checking if the mouse clicked it).<br><br>
 * Also contains a {@link ColorPresetsContainerWidget} for displaying a list of color presets too (if enabled).
 *
 * @see ColorPickerComponent
 * @see ColorPresetsContainerWidget
 * @see ColorPresetWidget
 */
public class ColorPickerWidget extends PressableWidget {
	private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_menu_list_background.png");
	private static final Identifier CONFIRM_TEXTURE = Identifier.ofVanilla("pending_invite/accept");
	private static final Identifier CONFIRM_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("pending_invite/accept_highlighted");
	private static final Identifier CANCEL_TEXTURE = Identifier.ofVanilla("pending_invite/reject");
	private static final Identifier CANCEL_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("pending_invite/reject_highlighted");
	private static final Identifier ALPHA_TEXTURE = Glowcase.id("alpha");
	private static final Identifier ALPHA_BIG_TEXTURE = Glowcase.id("alpha_big");

	public final ColorPickerIncludedScreen screen;
	public Element targetElement;
	protected int color = ColorUtil.RED; // cached color int for rendering & getting the color as int
	protected int colorNoAlpha = ColorUtil.RED; // cached color int with no transparency for rendering
	protected IconButtonWidget confirmButton;
	protected IconButtonWidget cancelButton;

	private boolean mouseDown = false;
	public boolean anyWidgetDown = false;
	private ColorPickerComponent currentComponent = null;
	private final List<ColorPickerComponent> components;
	private final ColorPickerComponent previewComponent = new ColorPickerComponent();
	private final ColorPickerComponent hueComponent = new ColorPickerComponent(this::setHue);
	private final ColorPickerComponent satLightComponent = new ColorPickerComponent(this::setSatLat);
	private final ColorPickerComponent alphaComponent = new ColorPickerComponent(this::setAlpha);
	private final ColorPickerComponent presetComponent = new ColorPickerComponent();

	public boolean includePresets = true;
	protected ColorPresetsContainerWidget presetsContainerWidget;

	public ArrayList<ColorPresetWidget> presetWidgets = Lists.newArrayList();
	public boolean allowTransparency = false;
	private Consumer<Integer> changeListener;
	private BiConsumer<Integer, @Nullable Formatting> presetListener;
	private Consumer<ColorPickerWidget> onAccept;
	private Consumer<ColorPickerWidget> onCancel;

	private float hue;
	private float saturation;
	private float light;
	private float alpha;

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

		this.presetsContainerWidget = new ColorPresetsContainerWidget(0, 0, 0, 0, this);

		this.components = List.of( // order isn't used for now, but assume click priority order I suppose
			this.previewComponent, this.satLightComponent, this.hueComponent, this.alphaComponent, this.presetComponent
		);

		this.update();
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if (!visible) return;
		this.updateHSLA();
		this.updatePositions();

		Matrix3x2fStack matrices = context.getMatrices();
		context.createNewRootLayer();
		matrices.pushMatrix();

		int x = this.getX();
		int y = this.getY();
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
		if(this.allowTransparency) {
			drawAlphaBar(context, this.alphaComponent);
		}
		if (this.includePresets) {
			this.presetsContainerWidget.renderWidget(context, mouseX, mouseY, delta);
		}

		// draw buttons
		this.confirmButton.renderWidget(context, mouseX, mouseY, delta);
		this.cancelButton.renderWidget(context, mouseX, mouseY, delta);

		matrices.popMatrix();
	}

	// Height values determined using these base numbers:
	// - Normal height (with presets, no alpha) = 104 (pre-color-picker-rewrite height numbers)
	// - Height with alpha and presets = 116 (104 + 10 + 2 -> normal height + hueHeight + presetPadding)
	// - presetSize (normal height) = 16
	// - previewHeight (normal height) = 52
	// - hueHeight (normal height) = 10
	// - presetLines (no alpha) = 2
	// - presetLines (with alpha) = 3

	// - Color picker height (with presets, no alpha) = 104 (pre-color-picker-rewrite numbers)
	// - Color picker height (with presets & alpha) = 116 (normal height + hueHeight + presetPadding)
	// - mainAreaHeight (with presets, no alpha) = 66 (2px top padding, no bottom padding) = 0.635 of total height
	// - presetAreaHeight (2 lines, no alpha) = 38 (2px top & bottom padding) = 0.365 of total height
	// - mainAreaHeight (with presets & alpha) = 78

	// These should be pretty consistent despite alpha or no alpha, but assume the same width of 182
	// - previewHeight = 52
	// - hueHeight = 10
	// - alphaHeight = 10
	// - presetSize = 16
	// - presetLines = 2

	// I really can't be bothered to make this perfectly perfect, so this will have to do
	public void updatePositions() {
		int x = this.getX();
		int y = this.getY();
		int width = this.getWidth();
		int height = this.getHeight();
		this.allowTransparency = true;
		this.includePresets = true;
//		this.setHeight(86); // no presets or alpha
//		this.setHeight(96); // no presets, with alpha
//		this.setHeight(116); // presets, no alpha
		this.setHeight(134); // presets & alpha

		int presetPadding = 2;
		int presetsPerLine = 10;
//		int presetLines = this.presetWidgets.isEmpty() ? 2 : MathHelper.ceil((float) this.presetWidgets.size() / presetsPerLine);
		int presetLines = MathHelper.ceil((float) this.presetsContainerWidget.getPresetSize() / presetsPerLine);
		int presetSize = ((width - presetPadding) / (presetsPerLine)) - presetPadding;

		int presetAreaHeight = this.includePresets ? (presetSize + presetPadding) * presetLines : presetSize;
		int mainAreaHeight = height - presetAreaHeight - presetPadding;

		int previewX = x + 2;
		int previewY = y + 2;
		int previewWidth = (int) (width / 3.5);
		int previewHeight = (int) (mainAreaHeight * (this.allowTransparency ? 0.667 : 0.788));

		int hueY = previewY + previewHeight + 2;
		int hueHeight = (int) (mainAreaHeight * (this.allowTransparency ? 0.129 : 0.152));
		int alphaY = hueY + (this.allowTransparency ? hueHeight + 2 : 0);

		int presetY = y + mainAreaHeight + presetPadding;
		int presetComponentHeight = presetAreaHeight;

		// old stuff
//		int presetPadding = 2;
//		int presetSize = (int) (height * (allowTransparency ? 0.136 : 0.154));
//
//		// Any numbers which are used to determine the position of another component should be its own variable
//		int previewX = x + 2;
//		int previewY = y + 2;
//		int previewWidth = (int) (width / 3.5);
//		int previewHeight = MathHelper.ceil(height * (allowTransparency ? 0.44 : 0.5));
//
//		int hueY = previewY + previewHeight + 2;
//		int hueHeight = MathHelper.ceil(height * (allowTransparency ? 0.085 : 0.096));
//
//		int alphaY = hueY + (this.allowTransparency ? hueHeight + 2 : 0);
//		int presetY = alphaY + hueHeight + presetPadding;
//
//		int presetComponentHeight = y + height - presetY; // allocate rest of height to presets

		this.previewComponent.updatePos(previewX, previewY, previewWidth, previewHeight);

		this.satLightComponent.updatePos(previewX + previewWidth + 2, y + 2,
			width - previewWidth - 6, previewHeight);

		this.hueComponent.updatePos(previewX, hueY, width - 4, hueHeight);

		this.alphaComponent.updatePos(previewX, alphaY, width - 4, hueHeight);

		this.presetComponent.updatePos(previewX, presetY, width, presetComponentHeight); // 36 / 104 = ~0.35

		this.presetsContainerWidget.setPosition(previewX, presetY, width, presetComponentHeight, presetSize, presetsPerLine);

		this.confirmButton.setPosition(
			x + width - presetSize - presetPadding - 2,
			y + height - presetSize - 3,
			presetSize, presetSize + 3
		);

		this.cancelButton.setPosition(
			x + width - presetSize * 2 - presetPadding * 2 - 3,
			y + height - presetSize - 3,
			presetSize, presetSize + 3
		);
	}

//	private int getPresetSize() {
//		int presetPadding = 2;
//		int presetsPerLine = 10;
//		return ((this.getWidth() - presetPadding) / (presetsPerLine)) - presetPadding;
//		return 16;
//		if(!this.presetWidgets.isEmpty()) {
//			int presetPadding = 2;
//			int presetsPerLine = 10;
//			int presetLines = this.presetWidgets.size() % presetsPerLine;
//			return (this.presetComponent.getHeight() - (presetPadding * presetLines)) / presetLines;
//		} else {
//			return 16;
//		}
//	}

	private void drawColorPreview(DrawContext context, ColorPickerComponent component) {
		if(this.alpha < 1f) {
			// background tile
//			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ALPHA_BIG_TEXTURE, component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight());
			drawTile(context, ALPHA_BIG_TEXTURE,
				component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight(),
				4, 4
			);
		}

		context.fill(component.getMinX(), component.getMinY(), component.getMaxX(), component.getMaxY(), this.color);
	}

	private void drawSatLight(DrawContext context, ColorPickerComponent component) {
		// white to current color's hue, left to right
		sidewaysGradient(context, component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight(), ColorUtil.WHITE, getRgbFromCurrentHue());

		// transparent to black, top to bottom
		context.fillGradient(component.getMinX(), component.getMinY(), component.getMaxX(), component.getMaxY(), 0x00000000, ColorUtil.BLACK);

		// thumb
		drawThumb(context, component, 8, 8, this.colorNoAlpha, false);
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
		drawThumb(context, component, 6, height + 1, getRgbFromCurrentHue());
	}

	private void drawAlphaBar(DrawContext context, ColorPickerComponent component) {
		// background tile
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ALPHA_TEXTURE, component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight());

		// current color to transparent
		sidewaysGradient(context, component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight(), ColorUtil.TRANSPARENT, this.colorNoAlpha);

		// thumb
		drawThumb(context, component, 6, component.getHeight() + 1, this.color);
	}

//	private void drawPresets(DrawContext context, ColorPickerComponent component, int mouseX, int mouseY, float delta) {
//		int maxY = this.getY() + this.getHeight();
////		int presetSize = (int) (this.getHeight() / 6.5);
////		int presetSize = (int) (height * (allowTransparency ? 0.138 : 0.154));
//		int presetSize = this.getPresetSize();
//		int paddedSize = presetSize + 2; // presetPadding = 2
//		// Mostly dynamic, but probably not perfect - am not too worried about it though
//		int presetsPerLine = component.getWidth() / (paddedSize);
//
//		// Render each preset widget in a grid, left to right, top to bottom
//		int presetX = component.getMinX();
//		int presetY = component.getMinY();
//		int renderedPresets = 0;
//		for (ColorPresetWidget preset : this.presetWidgets) {
//			preset.setPosition(presetX, presetY, presetSize);
//			preset.renderWidget(context, mouseX, mouseY, delta);
//
//			presetX += paddedSize;
//			renderedPresets++;
//			if(renderedPresets % presetsPerLine != 0) continue;
//
//			presetY += paddedSize;
//			int currentScaleY = presetY + paddedSize;
//			if(currentScaleY > component.getMaxY() || currentScaleY > maxY) return; // prevent overflow
//			presetX = component.getMinX();
//		}
//	}

	private void drawThumb(DrawContext context, ColorPickerComponent component, int width, int height, int color) {
		this.drawThumb(context, component, width, height, color, true);
	}

	private void drawThumb(DrawContext context, ColorPickerComponent component, int width, int height, int color, boolean centered) {
		int halfWidth = width / 2;
		int halfHeight = height / 2;
		int initY = centered ? component.getMinY() + (component.getHeight() / 2) : component.getThumbY();

		if(ColorHelper.getAlphaFloat(color) < 1f) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ALPHA_TEXTURE,
				component.getThumbX() - halfWidth + 1, initY - halfHeight,
				width - 1, height - 1
			);
//			drawTile(context, ALPHA_TEXTURE, component.getThumbX() - halfWidth + 1, initY - halfHeight, width - 1, height - 1, 1, 2);
		}

		context.fill(
			component.getThumbX() - halfWidth, initY - halfHeight,
			component.getThumbX() + halfWidth, initY + halfHeight,
			color
		);

		drawOutline(context, component.getThumbX() - halfWidth, initY - halfHeight - 1, width + 1, height + 1, Colors.WHITE);
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

	private void drawTile(DrawContext context, Identifier texture, int x, int y, int width, int height, float textureWidth, float textureHeight) {
		Sprite sprite = MinecraftClient.getInstance().getGuiAtlasManager().getSprite(texture);
		float tileWidth = width / textureWidth;
		float tileHeight = height / textureHeight;
		float u1 = sprite.getMinU();
		float v1 = sprite.getMinV();
		float u2 = sprite.getMaxU();
		float v2 = sprite.getMaxV();
		for (int iX = 0; iX < textureWidth; iX++) {
			float tileX = x + (tileWidth * iX);
			for (int iY = 0; iY < textureHeight; iY++) {
				float tileY = y + (tileHeight * iY);
				drawTextureWithPrecisionAndAccuracyEvenDonkeyKongWouldBeProudOf(context, sprite.getAtlasId(), tileX, tileY, tileWidth, tileHeight, u1, v1, u2, v2, ColorUtil.WHITE);
			}
		}
	}

	// am i mad?
	private void drawTextureWithPrecisionAndAccuracyEvenDonkeyKongWouldBeProudOf(DrawContext context, Identifier sprite, float x, float y, float width, float height, float u1, float v1, float u2, float v2, int color) {
		context.state.addSimpleElement(new SimpleGuiElementRenderState() {
			@Override
			public ScreenRect bounds() {
				return new ScreenRect((int) x, (int) y, (int) width, (int) height).transformEachVertex(context.getMatrices());
			}

			@Override
			public void setupVertices(VertexConsumer vertices, float depth) {
				Matrix3x2fStack matrix = context.getMatrices();
				vertices.vertex(matrix, x, y, depth).texture(u1, v1).color(color);
				vertices.vertex(matrix, x, y + height, depth).texture(u1, v2).color(color);
				vertices.vertex(matrix, x + width, y + height, depth).texture(u2, v2).color(color);
				vertices.vertex(matrix, x + width, y, depth).texture(u2, v1).color(color);
			}

			@Override
			public RenderPipeline pipeline() {
				return RenderPipelines.GUI_TEXTURED;
			}

			@Override
			public TextureSetup textureSetup() {
				return TextureSetup.withoutGlTexture(MinecraftClient.getInstance().getTextureManager().getTexture(sprite).getGlTextureView());
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
		// Sat/light, hue, transparency, confirm, cancel, presets
		boolean clickedComponent = tryClickComponent(this.satLightComponent, mouseX, mouseY)
			|| tryClickComponent(this.hueComponent, mouseX, mouseY)
			|| tryClickComponent(this.alphaComponent, mouseX, mouseY);
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
			this.setColorFromHSLA();
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

	public void tryClickPresets(double mouseX, double mouseY) {
		if(!this.allowWidgetClick()) return;

		this.anyWidgetDown = this.presetsContainerWidget.tryClickingPresets(mouseX, mouseY);
//		if(this.presetsContainerWidget.tryClickingPresets(mouseX, mouseY)) {
//			this.anyWidgetDown = true;
//		}
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

	public void insertColor(int color) {
		String hex = ColorUtil.getHexCode(color);
		this.screen.insertHexTag(hex);
	}

	public void update() {
		this.updatePositions();
		this.updateHSLA();
		this.updateThumbPositions();
	}

	public void updateThumbPositions() {
		this.satLightComponent.updateThumbXY(this.saturation, this.light);
		this.hueComponent.updateThumbX(this.hue);
		this.alphaComponent.updateThumbX(this.alpha);
	}

	public void updateHSLA() {
		float[] HSLA = getHSLA();
		this.hue = HSLA[0];
		this.saturation = HSLA[1];
		this.light = HSLA[2];
		this.alpha = this.allowTransparency ? HSLA[3] : 1f;
	}

	public void setHue(float hue) {
		this.hue = hue;
	}

	public void setSatLat(float saturation, float light) {
		this.saturation = saturation;
		this.light = light;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public void setColor(int color) {
		this.color = color;
		this.colorNoAlpha = ColorHelper.withAlpha(1f, color);
		this.update();
	}

	public void setColorFromHSLA() {
		this.color = ColorUtil.HSBtoRGB(getHueFromThumb(), this.saturation, this.light);
		this.colorNoAlpha = this.color;
		if(this.allowTransparency) {
			this.color = ColorHelper.withAlpha(this.alpha, this.color);
		}
	}

	public int getCurrentColor() {
		return this.color;
	}

	public int getCurrentColorWithoutAlpha() {
		return this.colorNoAlpha;
	}

	public int getRgbFromCurrentHue() {
		return ColorUtil.HSBtoRGB(getHueFromThumb(), 1, 1);
	}

	public float getHueFromThumb() {
		return (float) (this.hueComponent.getThumbX() - this.hueComponent.getMinX()) / this.hueComponent.getWidth();
	}

	protected float[] getHSLA() {
		return ColorUtil.intToHSLA(this.color);
	}

	public void setTargetElement(Element element) {
		this.targetElement = element;
	}

	public void setPresets(boolean includeDefaultPresets, List<Integer> addedPresets) {
		this.presetsContainerWidget.createPresets(includeDefaultPresets, addedPresets);

		if (includeDefaultPresets) {
//			this.presetWidgets.addAll(ColorPresetWidget.createDefaultPresets(this));
//			if(this.allowTransparency) {
//				this.presetWidgets.add(ColorPresetWidget.fromColor(this, 0x00000000));
//				this.presetWidgets.add(ColorPresetWidget.fromColor(this, 0x33000000));
//				this.presetWidgets.add(ColorPresetWidget.fromColor(this, 0x55000000));
//				this.presetWidgets.add(ColorPresetWidget.fromColor(this, 0x77000000));
//				this.presetWidgets.add(ColorPresetWidget.fromColor(this, 0x99000000));
//				this.presetWidgets.add(ColorPresetWidget.fromColor(this, 0xAA000000));
//				this.presetWidgets.add(ColorPresetWidget.fromColor(this, 0xCC000000));
//			}
		}

		if (addedPresets.isEmpty()) return;
		for (int preset : addedPresets) {
//			this.presetWidgets.add(ColorPresetWidget.fromColor(this, preset));
		}
	}

	// TODO - move into builder
	public void setIncludePresets(boolean shouldInclude) {
		this.includePresets = shouldInclude;
	}

	public void setChangeListener(Consumer<Integer> changeListener) {
		this.changeListener = changeListener;
	}

	public void setPresetListener(BiConsumer<Integer, @Nullable Formatting> presetListener) {
		this.presetListener = presetListener;
	}

	public void setOnAccept(Consumer<ColorPickerWidget> onAccept) {
		this.onAccept = onAccept;
	}

	public void setOnCancel(Consumer<ColorPickerWidget> onCancel) {
		this.onCancel = onCancel;
	}

	public BiConsumer<Integer, @Nullable Formatting> getPresetListener() {
		return presetListener;
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		this.appendDefaultNarrations(builder);
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
		private final List<Integer> presetColors = Lists.newArrayList();

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

		public ColorPickerWidget.Builder withPreset(boolean includeDefault, Integer... presets) {
			this.includeDefaultPresets = includeDefault;
			this.presetColors.addAll(Arrays.asList(presets));
			return this;
		}

		public ColorPickerWidget build() {
			ColorPickerWidget colorPickerWidget = new ColorPickerWidget(this.screen, this.x, this.y, this.width, this.height, Text.of(""));
			colorPickerWidget.setIncludePresets(this.includePresets);
			if (this.includePresets) {
				colorPickerWidget.setPresets(this.includeDefaultPresets, this.presetColors);
			}
			return colorPickerWidget;
		}
	}
}
