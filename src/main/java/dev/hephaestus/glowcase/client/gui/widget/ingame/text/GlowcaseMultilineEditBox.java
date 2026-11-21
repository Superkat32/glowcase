package dev.hephaestus.glowcase.client.gui.widget.ingame.text;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.gui.widget.ingame.IconButtonWidget;
import dev.hephaestus.glowcase.client.util.ColorUtil;
import dev.hephaestus.glowcase.client.util.GuiGraphicsUtil;
import dev.hephaestus.glowcase.util.TextJustify;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.TextCursorUtils;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;


/**
 * A multiline edit box for Glowcase Screens to use. Includes support for vertical scrolling with multiple lines, horizontal scrolling if a line overflows, text alignment, QuickText formatting, and formatting hotkeys (e.g. Ctrl+B for a bold tag). <br><br>
 * This class contains the widget side of things, including rendering and some mouse logic. {@link FormattableMultilineTextField} contains the text logic side of things, including text selection, cursor interactions, parsing into QuickText, and hotkey handling.
 * @see FormattableMultilineTextField
 * @see dev.hephaestus.glowcase.client.gui.screen.ingame.TextEditorScreen
 * @author Superkat32
 */
public class GlowcaseMultilineEditBox extends MultiLineEditBox {
	// FIXME - Ctrl+right on final word of line moves cursor to beginning of next line, instead of end of that line
	//  (This is actually a vanilla bug/thing with MultilineTextField)
	private static final Identifier UNDO_ICON = Glowcase.id("text_editor/undo");
	private static final Identifier UNDO_SHADOWLESS_ICON = Glowcase.id("text_editor/undo_shadowless");
	private static final Identifier REDO_ICON = Glowcase.id("text_editor/redo");
	private static final Identifier REDO_SHADOWLESS_ICON = Glowcase.id("text_editor/redo_shadowless");
	private static final Component ARROW_LEFT_SYMBOL = Component.literal("«");
	private static final Component ARROW_RIGHT_SYMBOL = Component.literal("»");

	public final FormattableMultilineTextField formatTextField;
	public final IconButtonWidget undoButton;
	public final IconButtonWidget redoButton;

	public final Consumer<List<Component>> parsedUpdateListener;
	public final Font font;
	public int sideAlignmentPadding;
	public int textTopPadding;
	public int undoRedoLeftPadding;
	public int lineHeight;
	public int maxLines;
	public boolean truncateText;
	public int overflowArrowColor;
	public int insetCursorColor;
	public int appendCursorColor;

	public int textColor = ColorUtil.WHITE;
	public boolean textShadow = true;
	public int undoRedoActiveColor;
	public int undoRedoUnactiveColor;

	public long focusedTime;

	public static Builder builder(Font font, List<Component> lines, int x, int y, int width, int height, Consumer<List<Component>> parsedUpdateListener) {
		return new Builder(font, lines, x, y, width, height, parsedUpdateListener);
	}

	public GlowcaseMultilineEditBox(
		Font font, List<Component> parsedLines,
		int x, int y, int width, int height,
		int sideAlignmentPadding, int textTopPadding, int undoRedoLeftPadding,
		int lineHeight, int maxLines, boolean truncateText,
		boolean showBackground, boolean showDecorations,
		int overflowArrowColor, int appendCursorColor, int insetCursorColor,
		int undoRedoActiveColor, int undoRedoUnactiveColor, boolean shadowlessUndoRedoIcons,
		Consumer<List<Component>> parsedUpdateListener
	) {
		super(font, x, y, width, height, CommonComponents.EMPTY, CommonComponents.EMPTY, ColorUtil.WHITE, true, -3092272, showBackground, showDecorations);
		this.font = font;
		this.sideAlignmentPadding = sideAlignmentPadding;
		this.textTopPadding = textTopPadding;
		this.undoRedoLeftPadding = undoRedoLeftPadding;
		this.lineHeight = lineHeight;
		this.maxLines = maxLines;
		this.truncateText = truncateText;
		this.parsedUpdateListener = parsedUpdateListener;
		this.overflowArrowColor = overflowArrowColor;
		this.appendCursorColor = appendCursorColor;
		this.insetCursorColor = insetCursorColor;
		this.undoRedoActiveColor = undoRedoActiveColor;
		this.undoRedoUnactiveColor = undoRedoUnactiveColor;

		this.focusedTime = Util.getMillis();

		this.formatTextField = new FormattableMultilineTextField(this.font, parsedLines, x + 1, width - 2, sideAlignmentPadding, parsedUpdateListener);
		this.formatTextField.setLineLimit(this.maxLines);
		this.formatTextField.truncateText = truncateText;
		this.formatTextField.setCursorListener(this::scrollToCursor);
		this.textField = this.formatTextField;

		this.undoButton = IconButtonWidget
			.builder(shadowlessUndoRedoIcons ? UNDO_SHADOWLESS_ICON : UNDO_ICON, button -> this.undo())
			.dimensions(0, 0, 12, 12, 8, 8)
			.renderBackground(false)
			.build();
		this.undoButton.setTooltip(
			Tooltip.create(
				Component.literal("Text Undo (Ctrl+Z)")
			)
		);

		this.redoButton = IconButtonWidget
			.builder(shadowlessUndoRedoIcons ? REDO_SHADOWLESS_ICON : REDO_ICON, button -> this.redo())
			.dimensions(0, 0, 12, 12, 8, 8)
			.renderBackground(false)
			.build();
		this.redoButton.setTooltip(
			Tooltip.create(
				Component.literal("Text Redo (Ctrl+Y, Ctrl+Shift+Z)")
			)
		);

		this.positionUndoRedoButtons();
		this.updateUndoRedoButtons();
	}

	public void updateSettings(int textColor, boolean textShadow, TextJustify textAlignment) {
		this.textColor = textColor;
		this.textShadow = textShadow;
		this.formatTextField.setTextAlignment(textAlignment);
	}

	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}

	public void setTextShadow(boolean textShadow) {
		this.textShadow = textShadow;
	}

	public void setTextAlignment(TextJustify textAlignment) {
		this.formatTextField.setTextAlignment(textAlignment);
	}

	@Override
	protected void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		// Debug - View edit regions depending on text alignment (red is center, green is left & right alignments)
//		graphics.outline(this.getX() + 1, this.getY() + 1, this.getWidth() - 2, this.getHeight() - 2, ColorUtil.RED);
//		graphics.outline(this.getX() + this.sideAlignmentPadding, this.getY() + 2, this.getWidth() - (this.sideAlignmentPadding * 2), this.getHeight() - 4, ColorUtil.GREEN);
//		graphics.horizontalLine(this.getX(), this.getX() + this.getWidth(), this.getY() + this.textTopPadding + 1, CommonColors.BLUE);

		// Render text lines
		boolean insetCursor = false;
		int cursorPos = this.textField.cursor();
		int cursorX = 0;
		int cursorY = 0;
		for (int i = 0; i < this.formatTextField.parsedLines.size(); i++) {
			int lineY = this.getY() + this.innerPadding() + i * this.lineHeight + this.textTopPadding;
			boolean lineWithinVisibleBounds = this.withinContentAreaTopBottom(lineY, lineY + this.lineHeight);
			if (!lineWithinVisibleBounds) continue;

			MultilineTextField.StringView view = this.textField.getLineView(i);
			MultilineTextField.StringView selectedView = this.textField.getSelected();
			boolean isCursorLine = this.formatTextField.isCursorLine(i);
			boolean isSelectedLine = this.formatTextField.isSelectedLine(i);

			FormattedText renderedLine = this.formatTextField.getLineForRender(i);

			int renderedLineWidth = this.font.width(renderedLine);
			boolean overflows = this.font.width(renderedLine) >= this.getWidth(); // Used for positioning & truncating
			boolean overflowsLeft = false; // Used for rendering overflow arrows
			boolean overflowsRight = false;

			int lineX = this.formatTextField.getLineX(i, renderedLineWidth);

			// Set cursor positions
			if (isCursorLine) {
				insetCursor = cursorPos < view.endIndex();

				String beforeCursor = this.textField.value().substring(view.beginIndex(), cursorPos);
				int beforeCursorWidth = this.font.width(beforeCursor);
				cursorX = lineX + beforeCursorWidth;
				cursorY = lineY;

				if (overflows) {
					overflowsLeft = lineX <= this.getX();
					overflowsRight = lineX + renderedLineWidth >= this.getX() + this.getWidth();
				}
			}

			// Render overflow indicators & fade text towards overflow
			if (overflows && isCursorLine) {
				int fadeWidth = 32;
				int nonFadedLineX = lineX;
				String nonFadedText = renderedLine.getString();
				int nonFadedLineWidth = this.font.width(nonFadedText);
				if (overflowsLeft) {
					int leftSideWidth = Mth.abs(nonFadedLineX - this.getX() - fadeWidth);
					String leftSideText = this.font.plainSubstrByWidth(nonFadedText, leftSideWidth);
					String fadeInText = this.font.plainSubstrByWidth(leftSideText, fadeWidth, true);
					String removedText = nonFadedText.substring(0, leftSideText.length() - fadeInText.length());
					int fadeInX = nonFadedLineX + this.font.width(removedText);
					GuiGraphicsUtil.extractFadingText(graphics, this.font, fadeInText, fadeInX, lineY, fadeWidth, this.textColor, this.textShadow, true);

					nonFadedText = nonFadedText.substring(leftSideText.length());
					nonFadedLineWidth = this.font.width(nonFadedText);
					nonFadedLineX += this.font.width(leftSideText);
				}
				if (overflowsRight) {
					int rightSideWidth = nonFadedLineX + nonFadedLineWidth - this.getX() - this.getWidth() + fadeWidth;
					String rightSideText = this.font.plainSubstrByWidth(nonFadedText, rightSideWidth, true);
					String fadeOutText = this.font.plainSubstrByWidth(rightSideText, fadeWidth);
					String removedText = nonFadedText.substring(rightSideText.length());

					nonFadedText = nonFadedText.substring(0, removedText.length());
					nonFadedLineWidth = this.font.width(nonFadedText);
					int fadeOutX = nonFadedLineX + nonFadedLineWidth;
					GuiGraphicsUtil.extractFadingText(graphics, this.font, fadeOutText, fadeOutX, lineY, fadeWidth, this.textColor, this.textShadow, false);
				}

				graphics.text(this.font, nonFadedText, nonFadedLineX, lineY, this.textColor, this.textShadow);
				if (overflowsLeft) graphics.text(this.font, ARROW_LEFT_SYMBOL, this.getX() + 2, lineY, this.overflowArrowColor);
				if (overflowsRight) graphics.text(this.font, ARROW_RIGHT_SYMBOL, this.getX() + this.getWidth() - this.font.width(ARROW_RIGHT_SYMBOL) - 2, lineY, this.overflowArrowColor);
			} else {
				graphics.text(this.font, Language.getInstance().getVisualOrder(renderedLine), lineX, lineY, this.textColor, this.textShadow);
			}

			// Render selection highlight
			if (isSelectedLine) {
				String startSelectWithinLine = this.textField.value().substring(view.beginIndex(), Math.max(selectedView.beginIndex(), view.beginIndex()));
				String endSelectWithinLine = this.textField.value().substring(view.beginIndex(), Math.min(view.endIndex(), selectedView.endIndex()));
				int highlightStartX = lineX + this.font.width(startSelectWithinLine);
				int highlightEndX = lineX + this.font.width(endSelectWithinLine);
				graphics.textHighlight(highlightStartX, lineY, highlightEndX, lineY + 9, true);
			}
		}

		// Render cursor
		boolean showCursor = this.isFocused() && TextCursorUtils.isCursorVisible(Util.getMillis() - this.focusedTime);
		if (showCursor) {
			if (insetCursor) TextCursorUtils.extractInsertCursor(graphics, cursorX, cursorY, this.insetCursorColor, 10);
			else TextCursorUtils.extractAppendCursor(graphics, this.font, cursorX, cursorY, this.appendCursorColor, this.textShadow);
		}

		if (this.isHovered()) {
			graphics.requestCursor(CursorTypes.IBEAM);
		}

//		this.undoButton.extractRenderState(graphics, mouseX, mouseY, a);
//		this.redoButton.extractRenderState(graphics, mouseX, mouseY, a);
		this.updateUndoRedoButtons();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.undoButton.mouseClicked(event, doubleClick)
			|| this.redoButton.mouseClicked(event, doubleClick)) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClick(@NonNull MouseButtonEvent event, boolean doubleClick) {
		// Allow what is practically a triple click to select the whole line (mirrors modern document editors)
		if (doubleClick && this.formatTextField.hasSelection()) {
			this.formatTextField.selectLineAtCursor();
		} else {
			super.onClick(event, doubleClick);
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (this.undoButton.isMouseOver(event.x(), event.y())
			|| this.redoButton.isMouseOver(event.x(), event.y())) return true;
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (Minecraft.getInstance().hasShiftDown()) {
			int cursorLine = this.formatTextField.getLineAtCursor();
			MultilineTextField.StringView lineView = this.formatTextField.getLineView(cursorLine);
			int lineWidth = this.font.width(this.formatTextField.value().substring(lineView.beginIndex(), lineView.endIndex()));
			if (lineWidth >= this.getWidth()) {
				this.formatTextField.setCursorOverflowX(
					this.formatTextField.cursorOverflowX + Mth.floor(scrollY * 5)
				);
				return true;
			}
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	@Override
	public boolean charTyped(@NonNull CharacterEvent event) {
		if (super.charTyped(event)) {
			// Handle text overflow adjustment
			int cursorLineIndex = this.formatTextField.getLineAtCursor();
			MultilineTextField.StringView cursorLineView = this.formatTextField.getLineView(cursorLineIndex);
			int cursorLineWidth = this.font.width(this.formatTextField.value().substring(cursorLineView.beginIndex(), cursorLineView.endIndex()));

			if (cursorLineWidth >= this.width) {
				this.formatTextField.cursorOverflowX -= this.font.width(event.codepointAsString());
			}
			return true;
		}
		return false;
	}

	public void insertText(String text) {
		this.formatTextField.insertText(text);
	}

	public void insertTag(String tagName) {
		this.formatTextField.insertTag(tagName);
	}

	public void insertTextTag(TextTag tag) {
		this.insertTextTag(tag, true);
	}

	public void insertTextTag(TextTag tag, boolean findShortestAlias) {
		this.formatTextField.insertTextTag(tag, findShortestAlias);
	}

	@Override
	protected void scrollToCursor() {
		double scrollAmount = this.scrollAmount();
		MultilineTextField.StringView firstFullyVisibleLine = this.textField.getLineView((int)(scrollAmount / this.lineHeight));
		if (this.textField.cursor() <= firstFullyVisibleLine.beginIndex()) {
			scrollAmount = this.textField.getLineAtCursor() * this.lineHeight;
		} else {
			MultilineTextField.StringView lastFullyVisibleLine = this.textField.getLineView((int)((scrollAmount + this.height) / this.lineHeight) - 1);
			if (this.textField.cursor() > lastFullyVisibleLine.endIndex()) {
				scrollAmount = this.textField.getLineAtCursor() * this.lineHeight - this.height + this.lineHeight + this.totalInnerPadding();
			}
		}

		this.setScrollAmount(scrollAmount);
	}

	@Override
	protected void seekCursorScreen(double mouseX, double mouseY) {
		double relativeY = mouseY - this.getY() - (this.innerPadding() / 2.0) - this.textTopPadding + this.scrollAmount();
		int top = Mth.floor(relativeY / this.lineHeight);
		int lineIndex = Mth.clamp(top, 0, this.textField.getLineCount() - 1);
		MultilineTextField.StringView clickedLineView = this.textField.getLineView(lineIndex);

		String rawLine = this.textField.value().substring(clickedLineView.beginIndex(), clickedLineView.endIndex());
		int lineX = this.formatTextField.getLineX(lineIndex, this.font.width(rawLine));

		double relativeX = mouseX - lineX + 2; // Adding 2 because it feels better for some reason

		int left = Mth.floor(relativeX);
		int clickedColumn = this.font.plainSubstrByWidth(this.textField.value().substring(clickedLineView.beginIndex(), clickedLineView.endIndex()), left).length();
		this.textField.seekCursor(Whence.ABSOLUTE, clickedLineView.beginIndex() + clickedColumn);
	}

	@Override
	public int getInnerHeight() {
		return this.textField.getLineCount() * this.lineHeight;
	}

	public void tick() {
		this.formatTextField.tick();
	}

	public void updateUndoRedoButtons() {
		this.undoButton.active = this.canUndo();
		this.undoButton.setColor(this.canUndo() ? this.undoRedoActiveColor : this.undoRedoUnactiveColor);
		this.redoButton.active = this.canRedo();
		this.redoButton.setColor(this.canRedo() ? this.undoRedoActiveColor : this.undoRedoUnactiveColor);
	}

	public void positionUndoRedoButtons() {
		this.undoButton.setX(this.getX() + this.undoRedoLeftPadding);
		this.undoButton.setY(this.getY() + 2);
		this.redoButton.setX(this.undoButton.getRight());
		this.redoButton.setY(this.undoButton.getY());
	}

	public void undo() {
		this.formatTextField.undo();
	}

	public boolean canUndo() {
		return this.formatTextField.canUndo();
	}

	public void redo() {
		this.formatTextField.redo();
	}

	public boolean canRedo() {
		return this.formatTextField.canRedo();
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		this.positionUndoRedoButtons();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		this.positionUndoRedoButtons();
	}

	public static class Builder {
		private final Font font;
		private final List<Component> lines;
		private final int x, y, width, height;
		private final Consumer<List<Component>> parsedUpdateListener;

		private int sideAlignmentPadding;
		private int textTopPadding = 0;
		public int undoRedoLeftPadding = 1;
		private int lineHeight = 12;
		private int maxLines = Integer.MAX_VALUE;
		private boolean truncateText = false;
		private boolean showBackground = false;
		private boolean showDecorations = true;
		private int overflowArrowColor = ColorUtil.WHITE;
		private int appendCursorColor = 0xFFFFFFFF;
		private int insetCursorColor = 0xCCFFFFFF;
		private int undoRedoActiveColor = ColorUtil.WHITE;
		private int undoRedoUnactiveColor = CommonColors.GRAY;
		private boolean shadowlessUndoRedoIcons = false;

		public Builder(Font font, List<Component> lines, int x, int y, int width, int height, Consumer<List<Component>> parsedUpdateListener) {
			this.font = font;
			this.lines = lines;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.parsedUpdateListener = parsedUpdateListener;

			this.sideAlignmentPadding = this.width / 10;
		}

		public Builder setSideAlignmentPadding(int sideAlignmentPadding) {
			this.sideAlignmentPadding = sideAlignmentPadding;
			return this;
		}

		public Builder setTextTopPadding(int textTopPadding) {
			this.textTopPadding = textTopPadding;
			return this;
		}

		public Builder setUndoRedoLeftPadding(int undoRedoLeftPadding) {
			this.undoRedoLeftPadding = undoRedoLeftPadding;
			return this;
		}

		public Builder setLineHeight(int lineHeight) {
			this.lineHeight = lineHeight;
			return this;
		}

		public Builder setMaxLines(int maxLines) {
			this.maxLines = maxLines;
			return this;
		}

		public Builder setTruncateText(boolean truncateText) {
			this.truncateText = truncateText;
			return this;
		}

		public Builder showBackground(boolean showBackground) {
			this.showBackground = showBackground;
			return this;
		}

		public Builder showDecorations(boolean showDecorations) {
			this.showDecorations = showDecorations;
			return this;
		}

		public Builder setOverflowArrowColor(int overflowArrowColor) {
			this.overflowArrowColor = overflowArrowColor;
			return this;
		}

		public Builder setAppendCursorColor(int appendCursorColor) {
			this.appendCursorColor = appendCursorColor;
			return this;
		}

		public Builder setInsetCursorColor(int insetCursorColor) {
			this.insetCursorColor = insetCursorColor;
			return this;
		}

		public Builder setUndoRedoColors(int activeColor, int unactiveColor, boolean shadowlessIcons) {
			this.undoRedoActiveColor = activeColor;
			this.undoRedoUnactiveColor = unactiveColor;
			this.shadowlessUndoRedoIcons = shadowlessIcons;
			return this;
		}

		public GlowcaseMultilineEditBox build() {
			return new GlowcaseMultilineEditBox(
				this.font, this.lines,
				this.x, this.y, this.width, this.height,
				this.sideAlignmentPadding, this.textTopPadding, this.undoRedoLeftPadding,
				this.lineHeight, this.maxLines, this.truncateText,
				this.showBackground, this.showDecorations,
				this.overflowArrowColor, this.appendCursorColor, this.insetCursorColor,
				this.undoRedoActiveColor, this.undoRedoUnactiveColor, this.shadowlessUndoRedoIcons,
				this.parsedUpdateListener
			);
		}
	}
}
