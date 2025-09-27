package dev.hephaestus.glowcase.client.gui.widget.ingame.color.picker;

import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * A class for containing the bounds of a {@link ColorPickerWidget} "area" (e.g. color preview, hue slider, sat/light picker, alpha slider, etc.).<br><br>
 * This is equivalent to a 2d box with some extra variables/methods for holding thumb positions and mouse clicking checks/handling. However, no rendering is actually done from here.
 *
 * @see ColorPickerWidget
 */
public class ColorPickerComponent {
	private boolean mouseDown = false;
	private int x, y, width, height;
	private int thumbX, thumbY;

	@Nullable
	private final ComponentSetter lerpFunction;
	@Nullable
	private final ComponentDoubleSetter doubleLerpFunction;
	private final boolean clickableVertically;

	public ColorPickerComponent() {
		this.lerpFunction = null;
		this.doubleLerpFunction = null;
		this.clickableVertically = false;
	}

	public ColorPickerComponent(@Nullable ComponentSetter mouseClickXLerpFunction) {
		this.lerpFunction = mouseClickXLerpFunction;
		this.doubleLerpFunction = null;
		this.clickableVertically = false;
	}

	public ColorPickerComponent(@Nullable ComponentDoubleSetter mouseClickXYLerpFunction) {
		this.lerpFunction = null;
		this.doubleLerpFunction = mouseClickXYLerpFunction;
		this.clickableVertically = true;
	}

	public void updatePos(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public boolean clicked(double mouseX, double mouseY) {
		if(this.hovered(mouseX, mouseY)) {
			this.mouseDown = true;
		}

		if(this.mouseDown) {
			this.thumbX = (int) MathHelper.clamp(mouseX, this.getMinX(), this.getMaxX());

			if(this.clickableVertically)
				this.thumbY = (int) MathHelper.clamp(mouseY, this.getMinY(), this.getMaxY());
		}

		return mouseDown;
	}

	public boolean hovered(double mouseX, double mouseY) {
		return (mouseY >= this.getMinY() && mouseY <= this.getMaxY()
			&& mouseX >= this.getMinX() && mouseX <= this.getMaxX());
	}

	public void setValuesFromMouse(double mouseX, double mouseY) {
		float mouseXValue = clampMouseValues(mouseX, this.x, this.width);
		if(this.lerpFunction != null) {
			this.lerpFunction.apply(mouseXValue);
			return; // y values should never be needed when using this function
		}

		float mouseYValue = clampMouseValues(mouseY, this.y, this.height);
		if (this.clickableVertically && this.doubleLerpFunction != null) {
			this.doubleLerpFunction.apply(mouseXValue, 1f - mouseYValue); // mouseY seems to be inverted
		}

	}

	private float clampMouseValues(double mouse, int min, int length) {
		if(mouse < min) return 0f;
		if(mouse > min + length) return 1f;

		float value = (float) ((mouse - min) / length);
		return MathHelper.clamp(value, 0f, 1f);
	}

	public void updateThumbX(float value) {
		this.thumbX = lerpPosition(this.x, this.width, value);
	}

	public void updateThumbXY(float valueX, float valueY) {
		this.updateThumbX(valueX);
		this.thumbY = lerpPosition(this.y, this.height, 1f - valueY);
	}

	private int lerpPosition(int min, int length, float lerp) {
		int max = min + length;
		return MathHelper.lerp(lerp, min, max);
	}

	public void resetMouseDown() {
		this.mouseDown = false;
	}

	// region Getters

	public int getMinX() {
		return this.x;
	}

	public int getMaxX() {
		return this.x + this.width;
	}

	public int getWidth() {
		return this.width;
	}

	public int getMinY() {
		return this.y;
	}

	public int getMaxY() {
		return this.y + this.height;
	}

	public int getHeight() {
		return this.height;
	}

	public int getThumbX() {
		return this.thumbX;
	}

	public int getThumbY() {
		return this.thumbY;
	}

	public boolean isMouseDown() {
		return this.mouseDown;
	}

	// endregion

	/**
	 * Setter method for applying a float from 0f-1f based on where a mouse clicked on this component.<br><br>
	 * Applied based on the horizontal distance along this component.
	 */
	@FunctionalInterface
	public interface ComponentSetter {
		void apply(float mouseHorizontalLerp);
	}

	/**
	 * Setter method for applying a float from 0f-1f based on where a mouse clicked on this component.<br><br>
	 * Applied based on the horizontal and vertical distance along this component.
	 */
	@FunctionalInterface
	public interface ComponentDoubleSetter {
		void apply(float mouseHorizontalLerp, float mouseVerticalLerp);
	}
}
