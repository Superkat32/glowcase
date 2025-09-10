package dev.hephaestus.glowcase.client.util;

import com.mojang.serialization.DataResult;
import net.minecraft.util.Formatting;

/**
 * @author Ampflower
 **/
public final class ColorUtil {
	// Custom variables here instead of minecraft.util.Colors because Minecraft's doesn't always use the
	// most saturated/lit up values (e.g. cyan is 0xFF57FFE1, and magenta doesn't exist)
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
}
