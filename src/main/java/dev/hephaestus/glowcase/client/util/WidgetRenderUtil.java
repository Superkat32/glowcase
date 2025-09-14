package dev.hephaestus.glowcase.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

/**
 * General widget render utils, including outlines, sideways gradients, and precise texture drawing.
 *
 * @author Superkat32
 */
public class WidgetRenderUtil {

	/**
	 * Draws an outline around an area.
	 */
	public static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width, y, x + width - 1, y + height, color);
		context.fill(x, y + height, x + width, y + height - 1, color);
	}

	/**
	 * Renders a gradient from left to right.
	 */
	public static void drawSidewaysGradient(DrawContext context, int x, int y, int width, int height, int startColor, int endColor) {
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

	/**
	 * Tiles a texture across the area with float precision, instead of being constrained to integer precision.
	 */
	public static void drawPreciseTile(DrawContext context, Identifier texture, int x, int y, int width, int height, float textureWidth, float textureHeight) {
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
				drawPreciseTexture(context, sprite.getAtlasId(), tileX, tileY, tileWidth, tileHeight, u1, v1, u2, v2, ColorUtil.WHITE);
			}
		}
	}

	/**
	 * Draws a textures with float precision instead of integer precision - it's actually so precise that Donkey Kong would be proud of it... why Donkey Kong I have no clue I'm losing my mind
	 */
	public static void drawPreciseTexture(DrawContext context, Identifier sprite, float x, float y, float width, float height, float u1, float v1, float u2, float v2, int color) {
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
}
