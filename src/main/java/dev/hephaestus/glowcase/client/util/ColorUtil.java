package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;

/**
 * @author Ampflower
 **/
public final class ColorUtil {
	// Custom variables here instead of minecraft.util.Colors because Minecraft's doesn't always use the
	// most saturated/lit up values (e.g. cyan is 0xFF57FFE1 instead of 0xFF00FFFF, and magenta doesn't exist)
	// All values that minecraft.util.Colors does cover are still created and preferred here for consistency
	public static final int RED = 0xFFFF0000;
	public static final int YELLOW = 0xFFFFFF00;
	public static final int GREEN = 0xFF00FF00;
	public static final int CYAN = 0xFF00FFFF;
	public static final int BLUE = 0xFF0000FF;
	public static final int MAGENTA = 0xFFFF00FF;

	public static final int WHITE = 0xFFFFFFFF;
	public static final int BLACK = 0xFF000000;
	public static final int TRANSPARENT = 0x00000000;
	public static final int ALPHA_MASK = 0xFF000000;
	public static final int COLOR_MASK = 0x00FFFFFF;

	public static final int[] RAINBOW_COLORS = new int[] { // loops back to red for hue bar in color picker
		RED, YELLOW, GREEN, CYAN, BLUE, MAGENTA, RED
	};

	public static int transferAlpha(int oldColor, int newColor) {
		return (oldColor & ALPHA_MASK) | (newColor & COLOR_MASK);
	}

	public static String getHexCode(int color) {
		return "#" + String.format("%1$06X", color & 0x00FFFFFF);
	}

	public static DataResult<Integer> parse(String string, int reference) {
		if (string.startsWith("#")) {
			try {
				final int color = Integer.parseUnsignedInt(string, 1, string.length(), 16);

				return switch (string.length()) {
					case 4 -> {
						int rgb = upcast(color);
						int a = reference & ALPHA_MASK;

						yield DataResult.success(a | rgb);
					}
					case 5 -> DataResult.success(upcast(color));
					case 7 -> {
						int a = reference & ALPHA_MASK;

						yield DataResult.success(a | color);
					}
					case 9 -> DataResult.success(color);
					default -> DataResult.error(() -> "Unexpected value: " + string);
				};
			} catch (NumberFormatException ignored) {
				return DataResult.error(() -> "Not a number: " + string);
			}
		} else {
			final Formatting formatting = Formatting.byName(string);
			if (formatting == null || !formatting.isColor()) {
				return DataResult.error(() -> "Unknown color: " + string);
			}

			int rgb = formatting.getColorValue() & COLOR_MASK;
			int a = reference & ALPHA_MASK;

			return DataResult.success(a | rgb);
		}
	}

	public static String toAlphaHex(int color) {
		return String.format("#%1$08X", color);
	}

	private static int upcast(int color) {
		int r = (color & 0x000F) * 0x00000011;
		int g = (color & 0x00F0) * 0x00000110;
		int b = (color & 0x0F00) * 0x00001100;
		int a = (color & 0xF000) * 0x00011000;
		return r | g | b | a;
	}

	public static float[] intToHSLA(int color) {
		int red = ColorHelper.getRed(color);
		int green = ColorHelper.getGreen(color);
		int blue = ColorHelper.getBlue(color);
		float[] HSL = new float[4];
		RGBtoHSB(red, green, blue, HSL);

		HSL[3] = ColorHelper.getAlphaFloat(color);
		return HSL;
	}


	/**
	 * Converts RGB to HSB(hue, saturation, brightness/light/value.<br><br>
	 *
	 * Custom method here used in favor of java.awt.Color's to prevent possible crashes on Mac.
	 * @see java.awt.Color#RGBtoHSB(int, int, int, float[])
	 */
	public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
		float hue, saturation, brightness;
		if (hsbvals == null) {
			hsbvals = new float[3];
		}
		int cmax = (r > g) ? r : g;
		if (b > cmax) cmax = b;
		int cmin = (r < g) ? r : g;
		if (b < cmin) cmin = b;

		brightness = ((float) cmax) / 255.0f;
		if (cmax != 0)
			saturation = ((float) (cmax - cmin)) / ((float) cmax);
		else
			saturation = 0;
		if (saturation == 0)
			hue = 0;
		else {
			float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
			float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
			float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
			if (r == cmax)
				hue = bluec - greenc;
			else if (g == cmax)
				hue = 2.0f + redc - bluec;
			else
				hue = 4.0f + greenc - redc;
			hue = hue / 6.0f;
			if (hue < 0)
				hue = hue + 1.0f;
		}
		hsbvals[0] = hue;
		hsbvals[1] = saturation;
		hsbvals[2] = brightness;
		return hsbvals;
	}

	public static int HSBtoRGBA(float hue, float saturation, float brightness, float alpha) {
		return ColorHelper.withAlpha(alpha, HSBtoRGB(hue, saturation,brightness));
	}

	/**
	 * Converts HSB(hue, saturation, brightness/light/value) to RGB<br><br>
	 *
	 * Custom method here used in favor of java.awt.Color's to prevent possible crashes on Mac.
	 * @see java.awt.Color#HSBtoRGB(float, float, float)
	 */
	public static int HSBtoRGB(float hue, float saturation, float brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			float h = (hue - (float)Math.floor(hue)) * 6.0f;
			float f = h - (float)java.lang.Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch ((int) h) {
				case 0:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 1:
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 2:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (t * 255.0f + 0.5f);
					break;
				case 3:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 4:
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 5:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (q * 255.0f + 0.5f);
					break;
			}
		}
		return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
	}
}
