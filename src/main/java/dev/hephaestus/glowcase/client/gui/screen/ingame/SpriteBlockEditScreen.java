package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.screen.ingame.interfaces.ColorPickerIncludedScreen;
import dev.hephaestus.glowcase.client.gui.widget.ingame.GlowcaseTextFieldWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.SuggestionListWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.ColorFieldWidget;
import dev.hephaestus.glowcase.client.gui.widget.ingame.color.ColorPickerWidget;
import dev.hephaestus.glowcase.packet.C2SEditSpriteBlock;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SpriteBlockEditScreen extends GlowcaseScreen implements ColorPickerIncludedScreen {
	private final SpriteBlockEntity spriteBlockEntity;

	private TextFieldWidget spriteWidget;
	private ButtonWidget spriteWidgetHelpButton;
	private ButtonWidget rotationWidget;
	private ButtonWidget zOffsetToggle;
	private ColorFieldWidget colorEntryWidget;
	private TextFieldWidget scaleEntryWidget;

	private ColorPickerWidget colorPickerWidget;

	private List<OrderedText> spriteHelpTooltipText;

	private SuggestionListWidget<String> suggestionWidget;
    private List<String> validSprites = new ArrayList<>();

	public SpriteBlockEditScreen(SpriteBlockEntity spriteBlockEntity) {
		this.spriteBlockEntity = spriteBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.client == null) return;

		this.spriteWidget = new GlowcaseTextFieldWidget(this.client.textRenderer, width / 2 - 90, height / 2 - 55, 180, 20, Text.empty());
		this.spriteWidget.setMaxLength(255);
		this.spriteWidget.setText(spriteBlockEntity.getSprite());
		this.spriteWidget.setChangedListener(string -> {
			this.spriteBlockEntity.setSprite(this.spriteWidget.getText());
		});

		Tooltip spriteHelpTooltip =  Tooltip.of(Text.translatable("gui.glowcase.screen.sprite_edit.sprite"));

		this.spriteWidgetHelpButton = ButtonWidget.builder(Text.literal("?"), action -> {})
			.dimensions(spriteWidget.getX() + spriteWidget.getWidth() + 4, spriteWidget.getY(), spriteWidget.getHeight(), spriteWidget.getHeight())
			.tooltip(spriteHelpTooltip)
			.build();

		this.rotationWidget = ButtonWidget.builder(Text.translatable("gui.glowcase.rotate"), (action) -> {
			this.spriteBlockEntity.rotation = (this.spriteBlockEntity.rotation + 45) % 360;
		}).dimensions(width / 2 - 90, height / 2 - 25, 180, 20).build();

		this.zOffsetToggle = ButtonWidget.builder(Text.literal(this.spriteBlockEntity.zOffset.name()), action -> {
			switch (spriteBlockEntity.zOffset) {
				case FRONT -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.CENTER;
				case CENTER -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.BACK;
				case BACK -> spriteBlockEntity.zOffset = TextBlockEntity.ZOffset.FRONT;
			}

			this.zOffsetToggle.setMessage(Text.literal(this.spriteBlockEntity.zOffset.name()));
		}).dimensions(width / 2 - 90, height / 2 + 5, 180, 20).build();

		this.colorPickerWidget = createColorPicker();

//		this.colorPickerWidget = ColorPickerWidget.builder(this, 0, 0).build();
//		this.colorPickerWidget.toggle(false);

		this.colorEntryWidget = ColorFieldWidget.Builder
			.create(this, width / 2 - 90, height / 2 + 35, this.spriteBlockEntity::getColor, this.spriteBlockEntity::setColor)
			.size(180, ColorFieldWidget.DEFAULT_HEIGHT)
			.colorPicker(this.colorPickerWidget)
			.offsetColorPicker(1,
				-ColorFieldWidget.DEFAULT_HEIGHT - ColorPickerWidget.DEFAULT_HEIGHT)
			.build();

//		this.colorEntryWidget = new TextFieldWidget(this.client.textRenderer, width / 2 - 90, height / 2 + 35, 180, 20, Text.empty());
//		this.colorEntryWidget.setText("#" + String.format("%1$06X", this.spriteBlockEntity.color & 0x00FFFFFF));
//		this.colorEntryWidget.setChangedListener(string -> {
//			TextColor.parse(this.colorEntryWidget.getText()).ifSuccess(color -> {
//				this.spriteBlockEntity.color = color == null ? 0xFFFFFFFF : color.getRgb() | 0xFF000000;
//			});
//		});

		this.scaleEntryWidget = new TextFieldWidget(this.client.textRenderer, width / 2 - 90, height / 2 + 65, 180, 20, Text.empty());
		this.scaleEntryWidget.setText(String.valueOf(this.spriteBlockEntity.scale));
		this.scaleEntryWidget.setChangedListener(string -> {
			 try {
				 this.spriteBlockEntity.scale = Float.parseFloat(string);
			 } catch (NumberFormatException ignored) {}
		});

		this.addDrawableChild(this.spriteWidget);
		this.addDrawableChild(this.spriteWidgetHelpButton);
		this.addDrawableChild(this.rotationWidget);
		this.addDrawableChild(this.zOffsetToggle);
		this.addDrawableChild(this.colorEntryWidget);
		this.addDrawableChild(this.scaleEntryWidget);

		ResourceManager resourceManager = this.client.getResourceManager();
		validSprites = allValidSprites(resourceManager);

		suggestionWidget = SuggestionListWidget.forTextFieldWithStaticSuggestions(spriteWidget, client.textRenderer, validSprites, Function.identity(), this);
		this.addPriorityWidget(this.suggestionWidget);
		this.addPriorityWidget(this.colorPickerWidget);
	}

	/**
	 * A list of all valid entries for {@link #spriteWidget}. Used for suggestions.
	 */
	public static List<String> allValidSprites(ResourceManager resourceManager) {
		var validSprites = new ArrayList<String>();

		// Add all sprites inside /textures/sprite, these are explicitly meant for the sprite block
		// and can be used with just their filename. As these are intended to be used here, we'll list them first
		resourceManager.findResources("textures/sprite", id -> id.getPath().endsWith(".png")).forEach((sprite, res) -> {
			validSprites.add(sprite.getPath().substring("textures/sprite/".length(), sprite.getPath().length() - 4));
		});

		// You can use any texture. Technically I think you can also use ones outside of texture
		// But findResources requires us to filter
		resourceManager.findResources("textures", id -> id.getPath().endsWith(".png")).forEach((sprite, res) -> {
			validSprites.add(sprite.toString());
		});

		// You can also display any item
		Registries.ITEM.stream()
			.map(Registries.ITEM::getId)
			.map(Identifier::toString)
			.forEach(validSprites::add);

		// And you can use any modid to display its icon
		FabricLoader.getInstance().getAllMods().forEach(mod -> {
			validSprites.add("mod:"+mod.getMetadata().getId());
		});

		return validSprites;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		// Tooltip is handled this way, since setting the tooltip directly on the help button widget causes the tooltip
		// to clip off-screen at higher GUI scales.
		/*if (this.spriteWidgetHelpButton.isHovered() || (this.spriteWidgetHelpButton.isFocused() && this.client.getNavigationType().isKeyboard())) {
			setTooltip(this.spriteHelpTooltipText);
		}*/

		// render priority widgets over everything
		this.renderPriorityWidgets(context, mouseX, mouseY, delta);
	}

	@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// Do it this way instead of mouseClickedPriorityWidgets because
		// the suggestion widget shouldn't be focused when clicked, otherwise it resets the scroll
		ClickableWidget hoveredPriorityWidget = this.getHoveredPriorityWidget(mouseX, mouseY);
		if(hoveredPriorityWidget != null) {
			if(hoveredPriorityWidget == this.suggestionWidget
				&& this.spriteWidget.isFocused() && this.suggestionWidget.mouseClicked(mouseX, mouseY, button)) {
				return true;
			} else if(hoveredPriorityWidget == this.colorPickerWidget && colorPickerWidget.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused(this.colorPickerWidget);
				return true;
			}
		} else {
			tryClosingColorPicker(mouseX, mouseY);
			suggestionWidget.updateSuggestions(new ArrayList<>(), "", this);
		}

        return super.mouseClicked(mouseX, mouseY, button);
    }

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (suggestionWidget.draggingScrollbar && this.suggestionWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
			return true;
		} else if(this.colorPickerWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// only scroll suggestion list if spriteWidget is focused
        if (suggestionWidget.isMouseOver(mouseX, mouseY) && spriteWidget.isFocused()) {
            suggestionWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(this.keyPressedPriorityWidgets(keyCode, scanCode, modifiers)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void close() {
		spriteBlockEntity.setSprite(spriteWidget.getText());
		spriteBlockEntity.markDirty();
		C2SEditSpriteBlock.of(spriteBlockEntity).send();
		super.close();
	}

	@Override
	public ColorPickerWidget getColorPicker() {
		return this.colorPickerWidget;
	}

	@Override
	public SelectionManager getSelectionManager() {
		return null;
	}
}
