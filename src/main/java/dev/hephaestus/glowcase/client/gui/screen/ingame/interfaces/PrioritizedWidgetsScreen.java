package dev.hephaestus.glowcase.client.gui.screen.ingame.interfaces;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An interface for holding and manging a unique List of ClickableWidgets which should be given priority over all other children widgets in this Screen.<br><br>
 * You'll still need to call the click/render methods in their respective code locations for this to work. Ideally:
 * <ul>
 *     <li>{@link PrioritizedWidgetsScreen#renderPriorityWidgets(DrawContext, int, int, float)} is called at the end of the render method.</li>
 *     <li>{@link PrioritizedWidgetsScreen#mouseClickedPriorityWidgets(double, double, int)} is called at the start of the mouseClickedMethod, and returns the method if true(prevents further buttons from being clicked).</li>
 *     <li>Any other mouse/key related method(e.g. mouseDragged, mouseScrolled) is called at the start of its respective method.</li>
 * </ul>
 * @see PrioritizedWidgetsScreen#addPriorityWidget(ClickableWidget)
 * @see PrioritizedWidgetsScreen#renderPriorityWidgets(DrawContext, int, int, float)
 * @see PrioritizedWidgetsScreen#mouseClickedPriorityWidgets(double, double, int)
 * @author Superkat32
 */
public interface PrioritizedWidgetsScreen extends ParentElement {

	/**
	 * @return This Screen's List of ClickableWidgets which should be given click/render priority (e.g. clicked first, rendered last).
	 */
	List<ClickableWidget> getPriorityWidgets();

	/**
	 * Renders all the priority widgets (in reversed order so that the first added widget is rendered last, therefore given priority).
	 */
	default void renderPriorityWidgets(DrawContext context, int mouseX, int mouseY, float tickDelta) {
		// reversed to render first added widgets last
		// it should always be assumed the order of added widgets is the desired priority
		for (ClickableWidget widget : this.getPriorityWidgets().reversed()) {
			widget.render(context, mouseX, mouseY, tickDelta);
		}
	}

	/**
	 * @return The first priority widget the mouse is hovering over, or null if the mouse isn't hovering over any.
	 */
	@Nullable
	default ClickableWidget getHoveredPriorityWidget(double mouseX, double mouseY) {
		for (ClickableWidget widget : this.getPriorityWidgets()) {
			if(widget.isMouseOver(mouseX, mouseY)) return widget;
		}
		return null;
	}

	/**
	 * @return True if any priority widget was clicked, or false if none were.
	 */
	default boolean mouseClickedPriorityWidgets(double mouseX, double mouseY, int button) {
		return this.mouseClickedPriorityWidgets(mouseX, mouseY, button, true);
	}

	/**
	 * @param focusWidget If this screen's focused element should be set to the clicked widget (Workaround for SuggestionListWidget impl).
	 * @return True if any priority widget was clicked, or false if none were.
	 */
	default boolean mouseClickedPriorityWidgets(double mouseX, double mouseY, int button, boolean focusWidget) {
		if(this.getPriorityWidgets().isEmpty()) return false;

		ClickableWidget widget = getHoveredPriorityWidget(mouseX, mouseY);
		if(widget == null) return false;

		if(widget.mouseClicked(mouseX, mouseY, button)) {
			if(focusWidget) {
				this.setFocused(widget);
				if(button == 0) {
					this.setDragging(true);
				}
			}
		}
		return true;
	}

	default boolean mouseDraggedPriorityWidgets(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if(this.getPriorityWidgets().isEmpty()) return false;

		ClickableWidget widget = getHoveredPriorityWidget(mouseX, mouseY);
		if(widget == null) return false;

		return widget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	default boolean mouseScrolledPriorityWidgets(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if(this.getPriorityWidgets().isEmpty()) return false;

		ClickableWidget widget = getHoveredPriorityWidget(mouseX, mouseY);
		if(widget == null) return false;

		return widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	default boolean keyPressedPriorityWidgets(int keyCode, int scanCode, int modifiers) {
		if(this.getPriorityWidgets().isEmpty()) return false;

		Element widget = this.getFocused();
		if(widget == null) return false;

		return widget.keyPressed(keyCode, scanCode, modifiers);
	}

	/**
	 * Adds a ClickableWidget to this Screen's priority widgets.<br><br>
	 * It is safe to assume that the order of added widgets is the same order they will be given priority.
	 */
	default void addPriorityWidget(ClickableWidget widget) {
		this.getPriorityWidgets().add(widget);
	}

	default void removePriorityWidget(ClickableWidget widget) {
		this.getPriorityWidgets().remove(widget);
	}

}
