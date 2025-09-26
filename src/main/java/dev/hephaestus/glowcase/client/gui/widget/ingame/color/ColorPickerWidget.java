package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.screen.ingame.interfaces.ColorPickerIncludedScreen;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.client.util.WidgetRenderUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

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
	public static final int DEFAULT_WIDTH = 182;
	public static final int DEFAULT_HEIGHT = 104; // presets, no alpha
	public static final int DEFAULT_HEIGHT_ALPHA = 134; // presets & alpha
	public static final int DEFAULT_HEIGHT_ALPHA_NO_PRESETS = 96; // no presets, with alpha
	public static final int DEFAULT_HEIGHT_NO_PRESETS_OR_ALPHA = 86; // no presets or alpha
	private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_menu_list_background.png");
	private static final Identifier CONFIRM_TEXTURE = Identifier.ofVanilla("pending_invite/accept");
	private static final Identifier CONFIRM_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("pending_invite/accept_highlighted");
	private static final Identifier CANCEL_TEXTURE = Identifier.ofVanilla("pending_invite/reject");
	private static final Identifier CANCEL_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("pending_invite/reject_highlighted");
	private static final Identifier ALPHA_TEXTURE = Glowcase.id("alpha");

	public final ColorPickerIncludedScreen screen;
	public Element targetElement;
	protected int color = ColorUtil.RED; // cached color int for rendering & getting the color as int
	protected int colorNoAlpha = ColorUtil.RED; // cached color int with no transparency for rendering
	protected int prevColorEntry = ColorUtil.RED; // prev color for undoing
	protected IconButtonWidget confirmButton;
	protected IconButtonWidget cancelButton;

	protected boolean mouseDown = false; // equal to mouseDragging
	protected boolean anyWidgetDown = false; // prevent widgets from being dragged after deactivated
	protected ColorPickerComponent currentComponent = null; // prevent components from being dragged after deactivated
	protected final List<ColorPickerComponent> components;
	protected final ColorPickerComponent previewComponent = new ColorPickerComponent();
	protected final ColorPickerComponent hueComponent = new ColorPickerComponent(this::setHue);
	protected final ColorPickerComponent satLightComponent = new ColorPickerComponent(this::setSatLat);
	protected final ColorPickerComponent alphaComponent = new ColorPickerComponent(this::setAlpha);

	public boolean includePresets = true;
	protected ColorPresetsContainerWidget presetsContainerWidget;
	public boolean allowAlpha = false;
	protected float minAlpha = 0f;

	private Consumer<Integer> changeListener;
	private BiConsumer<Integer, @Nullable Formatting> presetListener;
	private Consumer<ColorPickerWidget> onAccept;
	private Consumer<ColorPickerWidget> onCancel;

	private float hue;
	private float saturation;
	private float light;
	private float alpha;

	public ColorPickerWidget(ColorPickerIncludedScreen screen, int x, int y, int width, int height) {
		super(x, y, width, height, Text.empty());
		this.screen = screen;

		this.confirmButton = IconButtonWidget.builder(CONFIRM_TEXTURE, action -> this.confirmColor())
			.hoverIcon(CONFIRM_HIGHLIGHTED_TEXTURE).build();

		this.cancelButton = IconButtonWidget.builder(CANCEL_TEXTURE, action -> this.cancel())
			.hoverIcon(CANCEL_HIGHLIGHTED_TEXTURE).build();

		this.presetsContainerWidget = new ColorPresetsContainerWidget(0, 0, 0, 0, this::getCurrentColor);

		this.components = List.of( // order isn't used for now, but assume click priority order I suppose
			this.previewComponent, this.satLightComponent, this.hueComponent, this.alphaComponent
		);

		this.update();
		this.toggle(false); // start deactivated
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
			WidgetRenderUtil.drawOutline(context, x, y, width, height, ColorUtil.WHITE);
		}

		// draw components
		drawColorPreview(context, this.previewComponent, mouseX, mouseY);
		drawHueBar(context, this.hueComponent, mouseX, mouseY);
		drawSatLight(context, this.satLightComponent, mouseX, mouseY);
		if(this.allowAlpha) {
			drawAlphaBar(context, this.alphaComponent, mouseX, mouseY);
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

	// I(Superkat32) really can't be bothered to make this perfectly perfect, so this will have to do
	public void updatePositions() {
		int x = this.getX();
		int y = this.getY();
		int width = this.getWidth();
		int height = this.getHeight();

		int presetPadding = 2;
		int presetsPerLine = 10;
		int presetLines = MathHelper.ceil((float) this.presetsContainerWidget.getPresetSize() / presetsPerLine);
		int presetSize = ((width - presetPadding) / (presetsPerLine)) - presetPadding;

		int presetAreaHeight = this.includePresets ? (presetSize + presetPadding) * presetLines : presetSize;
		int mainAreaHeight = height - presetAreaHeight - presetPadding;

		int previewX = x + 2;
		int previewY = y + 2;
		int previewWidth = (int) (width / 3.5);
		int previewHeight = (int) (mainAreaHeight * (this.allowAlpha ? 0.667 : 0.788));

		int hueY = previewY + previewHeight + 2;
		int hueHeight = (int) (mainAreaHeight * (this.allowAlpha ? 0.129 : 0.152));
		int alphaY = hueY + (this.allowAlpha ? hueHeight + 2 : 0);

		int presetY = y + mainAreaHeight + presetPadding;

		this.previewComponent.updatePos(previewX, previewY, previewWidth, previewHeight);

		this.satLightComponent.updatePos(previewX + previewWidth + 2, y + 2,
			width - previewWidth - 6, previewHeight);

		this.hueComponent.updatePos(previewX, hueY, width - 4, hueHeight);

		this.alphaComponent.updatePos(previewX, alphaY, width - 4, hueHeight);

		this.presetsContainerWidget.setPosition(
			previewX, presetY, width, presetAreaHeight,
			presetSize, presetsPerLine
		);

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

	// Intended for text editing screens (e.g. the color picker formatting button from the text block)
	public void targetWidget(
		ClickableWidget widget, int color,
		int pickerX, int pickerY, int pickerWidth, int pickerHeight
	) {
		this.targetWidget(widget, color, false, 0f, true, null, pickerX, pickerY, pickerWidth, pickerHeight);
	}

	// Intended for text/color field widgets (e.g. the text color/background color from the text block)
	public void targetWidget(
		ClickableWidget widget, int color, boolean allowAlpha, float minAlpha, Consumer<Integer> onChange,
		int pickerX, int pickerY, int pickerWidth, int pickerHeight
	) {
		this.targetWidget(widget, color, allowAlpha, minAlpha, false, onChange, pickerX, pickerY, pickerWidth, pickerHeight);
	}

	public void targetWidget(
		ClickableWidget widget, int color,
		boolean allowAlpha, float minAlpha, boolean textEditorMode, Consumer<Integer> onChange,
		int pickerX, int pickerY, int pickerWidth, int pickerHeight
	) {
//		int pickerWidth = DEFAULT_WIDTH;
//		// disallow picker to go beyond screen limits
//		int pickerX = Math.min((widget.getX() + widget.getWidth()) - pickerWidth, screen.width);
//		int pickerY = widget.getY() + widget.getHeight();
//		int pickerHeight = allowAlpha ? DEFAULT_HEIGHT_ALPHA : DEFAULT_HEIGHT;
		this.setPosition(pickerX, pickerY);
		this.setDimensions(pickerWidth, pickerHeight);
		this.setAllowAlpha(allowAlpha);
		this.setMinAlpha(minAlpha);

		if(!allowAlpha) color = ColorUtil.transferAlpha(ColorUtil.WHITE, color); // remove alpha from color
		this.setTargetElement(widget);
		this.setColor(color);
		if(!this.activeAndVisible() && !this.isFocused()) {
			this.prevColorEntry = color;
		}

		if(textEditorMode) { // intended for screen editing
			this.setOnAccept(picker -> {
				this.screen.insertColorHexTag(picker.getCurrentColor());
				picker.toggle(false);
			});
			this.setOnCancel(picker -> picker.toggle(false));
			this.setChangeListener(null);
			this.setPresetListener((colorInt, formatting) -> {
				if(formatting != null) this.screen.insertFormattingTag(formatting);
				else this.screen.insertColorHexTag(colorInt);
				this.toggle(false);
			});
			this.toggle(!this.active);
		} else { // intended for text/color field widget editing
			this.setOnAccept(null);
			this.setOnCancel(picker -> {
				this.setColor(this.prevColorEntry);
			});
			this.setChangeListener(onChange);
			this.setPresetListener((colorInt, formatting) -> {
				this.setColor(colorInt);
			});
			this.toggle(true);
		}
	}

	private void drawColorPreview(DrawContext context, ColorPickerComponent component, int mouseX, int mouseY) {
		if(this.alpha < 1f) {
			// background tile
			WidgetRenderUtil.drawPreciseTile(context, ALPHA_TEXTURE,
				component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight(),
				4, 4
			);
		}

		// background
		context.fill(component.getMinX(), component.getMinY(), component.getMaxX(), component.getMaxY(), this.color);
	}

	private void drawSatLight(DrawContext context, ColorPickerComponent component, int mouseX, int mouseY) {
		// white to current color's hue, left to right
		WidgetRenderUtil.drawSidewaysGradient(context, component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight(), ColorUtil.WHITE, getRgbFromCurrentHue());

		// transparent to black, top to bottom
		context.fillGradient(component.getMinX(), component.getMinY(), component.getMaxX(), component.getMaxY(), 0x00000000, ColorUtil.BLACK);

		// thumb
		drawThumb(context, component, 8, 8, this.colorNoAlpha, false);
	}

	private void drawHueBar(DrawContext context, ColorPickerComponent component, int mouseX, int mouseY) {
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
			WidgetRenderUtil.drawSidewaysGradient(
				context,
				colorMinX, y,
				colorWidth, height,
				ColorUtil.RAINBOW_COLORS[color], ColorUtil.RAINBOW_COLORS[color + 1]
			);
		}

		// thumb
		drawThumb(context, component, 6, height + 1, getRgbFromCurrentHue());
	}

	private void drawAlphaBar(DrawContext context, ColorPickerComponent component, int mouseX, int mouseY) {
		// background tile
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ALPHA_TEXTURE, component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight());

		// transparent to current color
		int alphaColor = ColorHelper.withAlpha(this.minAlpha, this.colorNoAlpha);
		WidgetRenderUtil.drawSidewaysGradient(context, component.getMinX(), component.getMinY(), component.getWidth(), component.getHeight(), alphaColor, this.colorNoAlpha);

		// thumb
		drawThumb(context, component, 6, component.getHeight() + 1, this.color);
	}

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
		}

		context.fill(
			component.getThumbX() - halfWidth, initY - halfHeight,
			component.getThumbX() + halfWidth, initY + halfHeight,
			color
		);

		WidgetRenderUtil.drawOutline(context, component.getThumbX() - halfWidth, initY - halfHeight - 1, width + 1, height + 1, Colors.WHITE);
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

	public void update() {
		this.updatePositions();
		this.updateHSLA();
		this.updateThumbPositions();
	}

	public void updateThumbPositions() {
		this.satLightComponent.updateThumbXY(this.saturation, this.light);
		this.hueComponent.updateThumbX(this.hue);

		float alphaDelta;
		if(this.minAlpha != 0f) {
			alphaDelta = this.alpha - this.minAlpha + (this.alpha * this.minAlpha);
			if(alphaDelta < 0.5f) {
				// There's a weird thing with ColorHelper#withAlpha(0.11f, <color>), where the actual alpha is
				// equal to 0.10980392 instead, which causes an issue with moving the alpha slider to the very end
				// from presets with 0.11f minAlpha, so this counteracts that and clamps it to not go below 0
				alphaDelta -= 0.01f;
				alphaDelta = Math.max(alphaDelta, 0);
			}
		} else {
			alphaDelta = this.alpha;
		}
		this.alphaComponent.updateThumbX(alphaDelta);
	}

	public void updateHSLA() {
		float[] HSLA = getHSLA();
		this.hue = HSLA[0];
		this.saturation = HSLA[1];
		this.light = HSLA[2];
		this.alpha = this.allowAlpha ? HSLA[3] : 1f;
	}

	public void setHue(float hue) {
		this.hue = hue;
	}

	public void setSatLat(float saturation, float light) {
		this.saturation = saturation;
		this.light = light;
	}

	public void setAlpha(float alpha) {
		if(this.minAlpha != 0f) {
			this.alpha = (1f - this.minAlpha) * alpha + this.minAlpha;
		} else {
			this.alpha = alpha;
		}
	}

	public void setColor(int color) {
		this.color = color;
		this.colorNoAlpha = ColorHelper.withAlpha(1f, color);
		this.update();
	}

	public void setColorFromHSLA() {
		this.color = ColorUtil.HSBtoRGB(getHueFromThumb(), this.saturation, this.light);
		this.colorNoAlpha = this.color;
		if(this.allowAlpha) {
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

	public boolean activeAndVisible() {
		return this.active && this.visible;
	}

	//region Widget Related Getters & Setters
	public void setTargetElement(Element element) {
		this.targetElement = element;
	}

	public void setAllowAlpha(boolean allowAlpha) {
		this.allowAlpha = allowAlpha;
		this.presetsContainerWidget.setAllowAlpha(allowAlpha);
	}

	public boolean allowAlpha() {
		return this.allowAlpha;
	}

	public void setMinAlpha(float minAlpha) {
		this.minAlpha = minAlpha;
		this.presetsContainerWidget.setMinAlpha(minAlpha);
	}

	public void setIncludePresets(boolean shouldInclude) {
		this.includePresets = shouldInclude;
	}

	public void setChangeListener(Consumer<Integer> changeListener) {
		this.changeListener = changeListener;
	}

	public void setPresetListener(BiConsumer<Integer, @Nullable Formatting> presetListener) {
		this.presetListener = presetListener;
		this.presetsContainerWidget.setPresetListener(presetListener);
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

	//endregion

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		this.appendDefaultNarrations(builder);
	}

	@Environment(EnvType.CLIENT)
	public static class Builder {
		private final ColorPickerIncludedScreen screen;

		public static Builder create(ColorPickerIncludedScreen screen) {
			return new Builder(screen);
		}

		public Builder(ColorPickerIncludedScreen screen) {
			this.screen = screen;
		}

		public ColorPickerWidget build() {
			return new ColorPickerWidget(
				this.screen, 0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT
			);
//			ColorPickerWidget colorPickerWidget = new ColorPickerWidget(this.screen, this.x, this.y, this.width, this.height, Text.of(""));
//			colorPickerWidget.setIncludePresets(this.includePresets);
//			if (this.includePresets) {
//				colorPickerWidget.setPresets(this.includeDefaultPresets, this.presetColors);
//			}
//			return colorPickerWidget;
		}
	}
}
