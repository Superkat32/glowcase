package dev.hephaestus.glowcase.client.gui.widget.ingame.text;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class UndoRedoButtonsWidget extends AbstractContainerWidget {
	private static final Identifier UNDO_ICON = Glowcase.id("text_editor/undo");
	private static final Identifier UNDO_SHADOWLESS_ICON = Glowcase.id("text_editor/undo_shadowless");
	private static final Identifier REDO_ICON = Glowcase.id("text_editor/redo");
	private static final Identifier REDO_SHADOWLESS_ICON = Glowcase.id("text_editor/redo_shadowless");

	public final IconButtonWidget undoButton;
	public final IconButtonWidget redoButton;
	public final GlowcaseMultilineEditBox glowcaseEditBox;

	public int buttonScales;
	public int paddingX;
	public boolean renderBackgrounds;
	public boolean renderShadows;
	public int activeColor;
	public int unactiveColor;

	public static UndoRedoButtonsWidget.Builder builder(GlowcaseMultilineEditBox glowcaseEditBox) {
		return new UndoRedoButtonsWidget.Builder(glowcaseEditBox);
	}

	public UndoRedoButtonsWidget(
		GlowcaseMultilineEditBox editBox,
		int x, int y, int width, int height,
		int buttonScales, int paddingX,
		boolean renderBackgrounds, boolean renderShadows,
		int activeColor, int unactiveColor
	) {
		super(x, y, width, height, Component.empty());
		this.glowcaseEditBox = editBox;

		this.buttonScales = buttonScales;
		this.paddingX = paddingX;
		this.renderBackgrounds = renderBackgrounds;
		this.renderShadows = renderShadows;
		this.activeColor = activeColor;
		this.unactiveColor = unactiveColor;

		this.undoButton = IconButtonWidget
			.builder(this.renderShadows ? UNDO_ICON : UNDO_SHADOWLESS_ICON, button -> this.glowcaseEditBox.undo())
			.dimensions(0, 0, this.buttonScales, this.buttonScales, 8, 8)
			.renderBackground(this.renderBackgrounds)
			.build();
		this.undoButton.setTooltip(
			Tooltip.create(
				Component.literal("Text Undo (Ctrl+Z)")
			)
		);

		this.redoButton = IconButtonWidget
			.builder(this.renderShadows ? REDO_ICON : REDO_SHADOWLESS_ICON, button -> this.glowcaseEditBox.redo())
			.dimensions(0, 0, this.buttonScales, this.buttonScales, 8, 8)
			.renderBackground(this.renderBackgrounds)
			.build();
		this.redoButton.setTooltip(
			Tooltip.create(
				Component.literal("Text Redo (Ctrl+Y, Ctrl+Shift+Z)")
			)
		);

		this.positionButtons();
	}

	@Override
	protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.updateButtons();
		this.undoButton.extractRenderState(graphics, mouseX, mouseY, a);
		this.redoButton.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
		this.undoButton.updateWidgetNarration(output);
		this.redoButton.updateWidgetNarration(output);
	}

	public void positionButtons() {
		this.undoButton.setX(this.getX());
		this.undoButton.setY(this.getY());
		this.redoButton.setX(this.undoButton.getRight() + this.paddingX);
		this.redoButton.setY(this.getY());
	}

	public void updateButtons() {
		this.undoButton.active = this.glowcaseEditBox.canUndo();
		this.undoButton.setColor(this.glowcaseEditBox.canUndo() ? this.activeColor : this.unactiveColor);
		this.redoButton.active = this.glowcaseEditBox.canRedo();
		this.redoButton.setColor(this.glowcaseEditBox.canRedo() ? this.activeColor : this.unactiveColor);
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		this.positionButtons();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		this.positionButtons();
	}

	@Override
	protected int contentHeight() {
		return this.buttonScales;
	}

	@Override
	public @NonNull List<? extends GuiEventListener> children() {
		return List.of(this.undoButton, this.redoButton);
	}

	public static class Builder {
		private final GlowcaseMultilineEditBox editBox;

		private int x, y, width, height;

		private int buttonScales = 20;
		private int paddingX = 2;
		private boolean renderBackgrounds = true;
		private boolean renderShadows = true;
		private int activeColor = ColorUtil.WHITE;
		private int unactiveColor = CommonColors.GRAY;

		public Builder(GlowcaseMultilineEditBox editBox) {
			this.editBox = editBox;
			this.updateAutoDims();
		}

		public Builder setPos(int x, int y) {
			this.setX(x);
			this.setY(y);
			return this;
		}

		public Builder setX(int x) {
			this.x = x;
			return this;
		}

		public Builder setY(int y) {
			this.y = y;
			return this;
		}

		public Builder setWidth(int width) {
			this.width = width;
			return this;
		}

		public Builder setHeight(int height) {
			this.height = height;
			return this;
		}

		public Builder setButtonScales(int buttonScales) {
			this.buttonScales = buttonScales;
			this.updateAutoDims();
			return this;
		}

		public Builder setPaddingX(int paddingX) {
			this.paddingX = paddingX;
			this.updateAutoDims();
			return this;
		}

		public Builder setRenderBackgrounds(boolean renderBackgrounds) {
			this.renderBackgrounds = renderBackgrounds;
			return this;
		}

		public Builder setRenderShadows(boolean renderShadows) {
			this.renderShadows = renderShadows;
			return this;
		}

		public Builder setActiveColor(int activeColor) {
			this.activeColor = activeColor;
			return this;
		}

		public Builder setUnactiveColor(int unactiveColor) {
			this.unactiveColor = unactiveColor;
			return this;
		}

		private void updateAutoDims() {
			this.width = this.buttonScales * 2 + this.paddingX;
			this.height = this.buttonScales;
		}

		public UndoRedoButtonsWidget build() {
			return new UndoRedoButtonsWidget(
				this.editBox,
				this.x, this.y, this.width, this.height,
				this.buttonScales, this.paddingX,
				this.renderBackgrounds, this.renderShadows,
				this.activeColor, this.unactiveColor
			);
		}
	}
}
