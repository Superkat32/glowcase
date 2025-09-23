package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.client.gui.screen.ingame.interfaces.PrioritizedWidgetsScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public abstract class GlowcaseScreen extends Screen implements PrioritizedWidgetsScreen {
	protected final List<ClickableWidget> priorityWidgets = new ArrayList<>();
	protected GlowcaseScreen() {
		super(Text.empty());
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.renderInGameBackground(context);
		context.applyBlur();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public List<ClickableWidget> getPriorityWidgets() {
		return this.priorityWidgets;
	}
}
